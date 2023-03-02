package com.example.fpt.mtcnn

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MTCNN(context: Context) {

    private val factor = 0.709f

    private val pNetThreshold = 0.6f

    private val rNetThreshold = 0.7f

    private val oNetThreshold = 0.7f

    private val pInterpreter: Interpreter

    private val rInterpreter: Interpreter

    private val oInterpreter: Interpreter

    init {
        val options = Interpreter.Options()

        options.numThreads = 4

        pInterpreter = Interpreter(FileUtil.loadMappedFile(context, MODEL_FILE_PNET), options)

        rInterpreter = Interpreter(FileUtil.loadMappedFile(context, MODEL_FILE_RNET), options)

        oInterpreter = Interpreter(FileUtil.loadMappedFile(context, MODEL_FILE_ONET), options)
    }

    fun detectFaces(bitmap: Bitmap, minFaceSize: Int): Vector<Box> {
        var boxes: Vector<Box>
        try {
            //【1】PNet generate candidate boxes
            boxes = pNet(bitmap, minFaceSize)
            square_limit(boxes, bitmap.width, bitmap.height)

            //【2】RNet
            boxes = rNet(bitmap, boxes)
            square_limit(boxes, bitmap.width, bitmap.height)

            //【3】ONet
            boxes = oNet(bitmap, boxes)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            boxes = Vector()
        }
        return boxes
    }

    private fun square_limit(boxes: Vector<Box>, w: Int, h: Int) {
        // square
        for (i in boxes.indices) {
            boxes[i].toSquareShape()
            boxes[i].limitSquare(w, h)
        }
    }


    private fun pNet(bitmap: Bitmap, minSize: Int): Vector<Box> {
        val whMin = min(bitmap.width, bitmap.height)
        var currentFaceSize = minSize.toFloat()
        val totalBoxes = Vector<Box>()

        while (currentFaceSize <= whMin) {
            val scale = 12.0f / currentFaceSize

            // (1)Image Resize
            val bm = bitmapResize(bitmap, scale)
            val w = bm.width
            val h = bm.height

            // (2)RUN CNN
            val outW = (ceil(w * 0.5 - 5) + 0.5).toInt()
            val outH = (ceil(h * 0.5 - 5) + 0.5).toInt()
            var prob1 = Array(1) { Array(outW) { Array(outH) { FloatArray(2) } } }
            var conv4_2_BiasAdd = Array(1) { Array(outW) { Array(outH) { FloatArray(4) } } }
            pNetForward(bm, prob1, conv4_2_BiasAdd)
            prob1 = transposeBatch(prob1)
            conv4_2_BiasAdd = transposeBatch(conv4_2_BiasAdd)

            // (3)数据解析
            val curBoxes = Vector<Box>()
            generateBoxes(prob1, conv4_2_BiasAdd, scale, curBoxes)

            // (4)nms 0.5
            nms(curBoxes, 0.5f, "Union")

            // (5)add to totalBoxes
            for (i in curBoxes.indices) if (!curBoxes[i].deleted) totalBoxes.addElement(curBoxes[i])

            // Face Size等比递增
            currentFaceSize /= factor
        }

        // NMS 0.7
        nms(totalBoxes, 0.7f, "Union")

        // BBR
        BoundingBoxReggression(totalBoxes)
        return updateBoxes(totalBoxes)
    }

    private fun pNetForward(
        bitmap: Bitmap,
        prob1: Array<Array<Array<FloatArray>>>,
        conv4_2_BiasAdd: Array<Array<Array<FloatArray>>>
    ) {
        val img = normalizeImage(bitmap)
//        var pNetIn = arrayOfNulls<Array<Array<FloatArray>>>(1)
        var pNetIn = arrayOfNulls<Array<Array<FloatArray>>>(1)
        pNetIn[0] = img
        pNetIn = transposeBatch1(pNetIn)
        val outputs: MutableMap<Int, Any> = HashMap()
        outputs[pInterpreter.getOutputIndex("pnet/prob1")] = prob1
        outputs[pInterpreter.getOutputIndex("pnet/conv4-2/BiasAdd")] = conv4_2_BiasAdd

        pInterpreter.runForMultipleInputsOutputs(arrayOf<Any>(pNetIn), outputs)
    }


    private fun generateBoxes(
        prob1: Array<Array<Array<FloatArray>>>,
        conv4_2_BiasAdd: Array<Array<Array<FloatArray>>>,
        scale: Float,
        boxes: Vector<Box>
    ): Int {
        val h = prob1[0].size
        val w = prob1[0][0].size
        for (y in 0 until h) {
            for (x in 0 until w) {
                val score = prob1[0][y][x][1]
                // only accept prob >threadshold(0.6 here)
                if (score > pNetThreshold) {
                    val box = Box()
                    // core
                    box.score = score
                    // box
                    box.box[0] = (x * 2 / scale).roundToInt()
                    box.box[1] = (y * 2 / scale).roundToInt()
                    box.box[2] = ((x * 2 + 11) / scale).roundToInt()
                    box.box[3] = ((y * 2 + 11) / scale).roundToInt()
                    // bbr
                    for (i in 0..3) {
                        box.bbr[i] = conv4_2_BiasAdd[0][y][x][i]
                    }
                    // add
                    boxes.addElement(box)
                }
            }
        }
        return 0
    }

    private fun nms(boxes: Vector<Box>, threshold: Float, method: String) {
        for (i in boxes.indices) {
            val box = boxes[i]
            if (!box.deleted) {
                for (j in i + 1 until boxes.size) {
                    val box2 = boxes[j]
                    if (!box2.deleted) {
                        val x1 = max(box.box[0], box2.box[0])
                        val y1 = max(box.box[1], box2.box[1])
                        val x2 = min(box.box[2], box2.box[2])
                        val y2 = min(box.box[3], box2.box[3])
                        if (x2 < x1 || y2 < y1) continue
                        val areaIoU = (x2 - x1 + 1) * (y2 - y1 + 1)
                        var iou = 0f
                        if (method == "Union") iou =
                            1.0f * areaIoU / (box.area() + box2.area() - areaIoU) else if (method == "Min") iou =
                            1.0f * areaIoU / min(box.area(), box2.area())
                        if (iou >= threshold) { // 删除prob小的那个框
                            if (box.score > box2.score) box2.deleted = true else box.deleted = true
                        }
                    }
                }
            }
        }
    }

    private fun BoundingBoxReggression(boxes: Vector<Box>) {
        for (i in boxes.indices) boxes[i].calibrate()
    }

    private fun rNet(bitmap: Bitmap, boxes: Vector<Box>): Vector<Box> {
        // RNet Input Init
        val num = boxes.size
        val rNetIn = Array(num) { Array(24) { Array(24) { FloatArray(3) } } }
        for (i in 0 until num) {
            var curCrop = cropAndResize(bitmap, boxes[i], 24)
            curCrop = transposeImage(curCrop)
            rNetIn[i] = curCrop
        }

        // Run RNet
        rNetForward(rNetIn, boxes)

        // RNetThreshold
        for (i in 0 until num) {
            if (boxes[i].score < rNetThreshold) {
                boxes[i].deleted = true
            }
        }

        // Nms
        nms(boxes, 0.7f, "Union")
        BoundingBoxReggression(boxes)
        return updateBoxes(boxes)
    }

    /**
     * RNET跑神经网络，将score和bias写入boxes
     * @param rNetIn
     * @param boxes
     */
    private fun rNetForward(rNetIn: Array<Array<Array<FloatArray>>>, boxes: Vector<Box>) {
        val num = rNetIn.size
        val prob1 = Array(num) { FloatArray(2) }
        val conv5_2_conv5_2 = Array(num) { FloatArray(4) }
        val outputs: MutableMap<Int, Any> = HashMap()
        outputs[rInterpreter.getOutputIndex("rnet/prob1")] = prob1
        outputs[rInterpreter.getOutputIndex("rnet/conv5-2/conv5-2")] = conv5_2_conv5_2
        rInterpreter.runForMultipleInputsOutputs(arrayOf<Any>(rNetIn), outputs)

        // 转换
        for (i in 0 until num) {
            boxes[i].score = prob1[i][1]
            for (j in 0..3) {
                boxes[i].bbr[j] = conv5_2_conv5_2[i][j]
            }
        }
    }

    /**
     * ONet
     * @param bitmap
     * @param boxes
     * @return
     */
    private fun oNet(bitmap: Bitmap, boxes: Vector<Box>): Vector<Box> {
        // ONet Input Init
        val num = boxes.size
        val oNetIn = Array(num) { Array(48) { Array(48) { FloatArray(3) } } }
        for (i in 0 until num) {
            var curCrop = cropAndResize(bitmap, boxes[i], 48)
            curCrop = transposeImage(curCrop)
            oNetIn[i] = curCrop
        }

        // Run ONet
        oNetForward(oNetIn, boxes)
        // ONetThreshold
        for (i in 0 until num) {
            if (boxes[i].score < oNetThreshold) {
                boxes[i].deleted = true
            }
        }
        BoundingBoxReggression(boxes)
        // Nms
        nms(boxes, 0.7f, "Min")
        return updateBoxes(boxes)
    }

    private fun oNetForward(oNetIn: Array<Array<Array<FloatArray>>>, boxes: Vector<Box>) {
        val num = oNetIn.size
        val prob1 = Array(num) { FloatArray(2) }
        val conv6_2_conv6_2 = Array(num) { FloatArray(4) }
        val conv6_3_conv6_3 = Array(num) { FloatArray(10) }
        val outputs: MutableMap<Int, Any> = HashMap()
        outputs[oInterpreter.getOutputIndex("onet/prob1")] = prob1
        outputs[oInterpreter.getOutputIndex("onet/conv6-2/conv6-2")] = conv6_2_conv6_2
        outputs[oInterpreter.getOutputIndex("onet/conv6-3/conv6-3")] = conv6_3_conv6_3
        oInterpreter.runForMultipleInputsOutputs(arrayOf<Any>(oNetIn), outputs)

        // 转换
        for (i in 0 until num) {
            // prob
            boxes[i].score = prob1[i][1]
            // bias
            for (j in 0..3) {
                boxes[i].bbr[j] = conv6_2_conv6_2[i][j]
            }
            // landmark
            for (j in 0..4) {
                val x = (boxes[i].left() + conv6_3_conv6_3[i][j] * boxes[i].width()).roundToInt()
                val y =
                    (boxes[i].top() + conv6_3_conv6_3[i][j + 5] * boxes[i].height()).roundToInt()
                boxes[i].landmark[j] = Point(x, y)
            }
        }
    }

    companion object {
        private const val MODEL_FILE_PNET = "pnet.tflite"
        private const val MODEL_FILE_RNET = "rnet.tflite"
        private const val MODEL_FILE_ONET = "onet.tflite"

        fun updateBoxes(boxes: Vector<Box>): Vector<Box> {
            val b = Vector<Box>()
            for (i in boxes.indices) {
                if (!boxes[i].deleted) {
                    b.addElement(boxes[i])
                }
            }
            return b
        }

        fun normalizeImage(bitmap: Bitmap): Array<Array<FloatArray>> {
            val h = bitmap.height
            val w = bitmap.width
            val floatValues = Array(h) { Array(w) { FloatArray(3) } }
            val imageMean = 127.5f
            val imageStd = 128f
            val pixels = IntArray(h * w)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, w, h)
            for (i in 0 until h) { //
                for (j in 0 until w) {
                    val value = pixels[i * w + j]
                    val r = ((value shr 16 and 0xFF) - imageMean) / imageStd
                    val g = ((value shr 8 and 0xFF) - imageMean) / imageStd
                    val b = ((value and 0xFF) - imageMean) / imageStd
                    val arr = floatArrayOf(r, g, b)
                    floatValues[i][j] = arr
                }
            }
            return floatValues
        }

        fun bitmapResize(bitmap: Bitmap, scale: Float): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            val matrix = Matrix()
            matrix.postScale(scale, scale)
            return Bitmap.createBitmap(
                bitmap, 0, 0, width, height, matrix, true
            )
        }

        fun transposeImage(input: Array<Array<FloatArray>>): Array<Array<FloatArray>> {
            val h = input.size
            val w = input[0].size
            val channel = input[0][0].size
            val out = Array(w) { Array(h) { FloatArray(channel) } }
            for (i in 0 until h) {
                for (j in 0 until w) {
                    out[j][i] = input[i][j]
                }
            }
            return out
        }

        fun transposeBatch(input: Array<Array<Array<FloatArray>>>): Array<Array<Array<FloatArray>>> {
            val batch = input.size
            val h = input[0].size

            val w = input[0][0].size
            val channel = input[0][0].size
            val out = Array(batch) { Array(w) { Array(h) { FloatArray(channel) } } }
            for (i in 0 until batch) {
                for (j in 0 until h) {
                    for (k in 0 until w) {
                        out[i][k][j] = input[i][j][k]
                    }
                }
            }
            return out
        }

        fun transposeBatch1(input: Array<Array<Array<FloatArray>>?>): Array<Array<Array<FloatArray>>?> {
            val batch = input.size
            val h = input[0]?.size

            val w = input[0]?.get(0)?.size
            val channel = input[0]?.get(0)?.size
            var out = Array(batch) { w?.let { it1 -> Array(it1) { Array(h!!) { FloatArray(channel!!) } } } }
            for (i in 0 until batch) {
                for (j in 0 until h!!) {
                    for (k in 0 until w!!) {
                        out[i]?.get(k)?.set(j, input[i]?.get(j)?.get(k)!!)
                    }
                }
            }
            return out
        }

        fun cropAndResize(bitmap: Bitmap?, box: Box, size: Int): Array<Array<FloatArray>> {
            // crop and resize
            val matrix = Matrix()
            val scaleW = 1.0f * size / box.width()
            val scaleH = 1.0f * size / box.height()
            matrix.postScale(scaleW, scaleH)
            val rect = box.transform2Rect()
            val croped = Bitmap.createBitmap(
                bitmap!!, rect.left, rect.top, box.width(), box.height(), matrix, true
            )
            return normalizeImage(croped)
        }
    }
}