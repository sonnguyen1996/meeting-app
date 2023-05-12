package com.example.fpt.classifer

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class TfLiteClassifier(context: Context?, model_path: String?) {
    /** An instance of the driver class to run model inference with Tensorflow Lite.  */
    protected var tflite: Interpreter? = null

    /* Preallocated buffers for storing image data in. */
    private var intValues: IntArray? = null
    protected var imgData: ByteBuffer? = null

    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.  */
   private var imageSizeX = 224
    private var imageSizeY = 224
    private val outputs: Array<Array<FloatArray?>>
    var outputMap: MutableMap<Int, Any> = HashMap()

    init {
        val options = Interpreter.Options().setNumThreads(4)
        val tfliteModel = context?.let { model_path?.let { modelPath -> FileUtil.loadMappedFile(it, modelPath) } }
        if(tfliteModel != null){
            tflite = Interpreter(tfliteModel, options)
        }
        tflite?.allocateTensors()
        val inputShape = tflite?.getInputTensor(0)?.shape()
        if(inputShape != null){
            imageSizeX = inputShape[1]
            imageSizeY = inputShape[2]
            intValues = IntArray(imageSizeX * imageSizeY)
            imgData =
                ByteBuffer.allocateDirect(imageSizeX * imageSizeY * inputShape[3] * numBytesPerChannel())
        }
        imgData?.order(ByteOrder.nativeOrder())
        val outputCount = tflite?.outputTensorCount ?: 0
        outputs = Array(outputCount) { arrayOfNulls(1) }
        for (i in 0 until outputCount) {
            val shape = tflite?.getOutputTensor(i)?.shape()
            val numOFFeatures = shape?.get(1)
            Log.i(TAG, "Read output layer size is $numOFFeatures")
            if(numOFFeatures != null){
                outputs[i][0] = FloatArray(numOFFeatures)
                val ithOutput =
                    ByteBuffer.allocateDirect(numOFFeatures * numBytesPerChannel()) // Float tensor, shape 3x2x4
                ithOutput.order(ByteOrder.nativeOrder())
                outputMap[i] = ithOutput
            }
        }
    }

    protected abstract fun addPixelValue(value: Int)

    /** Classifies a frame from the preview stream.  */
    fun classifyFrame(bitmap: Bitmap): ClassifierResult? {
        val inputs = arrayOf<Any?>(null)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        if (imgData == null) {
            return null
        }
        imgData?.rewind()
        // Convert the image to floating point.
        var pixel = 0
        for (i in 0 until imageSizeX) {
            for (j in 0 until imageSizeY) {
                val value = intValues?.get(pixel++)
                value?.let { addPixelValue(it) }
            }
        }
        inputs[0] = imgData
        val startTime = SystemClock.uptimeMillis()
        tflite?.runForMultipleInputsOutputs(inputs, outputMap)
        for (i in outputs.indices) {
            val ithOutput = outputMap[i] as ByteBuffer?
            ithOutput?.rewind()
            val len = outputs[i][0]!!.size
            for (j in 0 until len) {
                ithOutput?.float?.let { outputs[i][0]?.set(j, it) }
            }
            ithOutput?.rewind()
        }
        val endTime = SystemClock.uptimeMillis()
        Log.i(
            TAG,
            "tf lite Timecost to run model inference: " + (endTime - startTime).toString()
        )
        return getResults(outputs)
    }

    fun close() {
        tflite?.close()
    }

    protected abstract fun getResults(outputs: Array<Array<FloatArray?>>?): ClassifierResult?

    fun getImageSizeX() = imageSizeX

     fun getImageSizeY() = imageSizeY

     fun numBytesPerChannel() = 4

    companion object {
        /** Tag for the [Log].  */
        private const val TAG = "TfLiteClassifier"
    }
}