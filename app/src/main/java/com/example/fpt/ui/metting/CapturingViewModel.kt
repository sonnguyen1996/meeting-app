package com.example.fpt.ui.metting

import android.app.Application
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.demo.domain.domain.entities.BehaviourRemoteInfo
import com.demo.domain.domain.entities.ErrorResult
import com.example.fpt.classifer.EmotionTfLiteClassifier
import com.example.fpt.classifer.model.BehaviourData
import com.example.fpt.classifer.model.ProcessingData
import com.example.fpt.mtcnn.Box
import com.example.fpt.mtcnn.MTCNN
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.face.FaceDetector
import kotlinx.coroutines.*
import live.videosdk.rtc.android.Meeting
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.max
import kotlin.math.min


class CapturingViewModel(application: Application) : AndroidViewModel(application) {

    var listCaptureImage = mutableListOf<ProcessingData>()

    private var database: DatabaseReference

    private var emotionClassifierTfLite: EmotionTfLiteClassifier? = null

    private var mtcnnFaceDetector: MTCNN? = null

    private val minFaceSize = 32

    private var heavyJob: Job = Job()

    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, error ->
        val errorResult =
            ErrorResult(errorMessage = error.message)
        coroutineContext.cancel()
    }

    val emotionResult: MutableLiveData<BehaviourData> =
        MutableLiveData()

    val heavyTaskScope by lazy { CoroutineScope(heavyJob + Dispatchers.Main + exceptionHandler) }

    init {
        mtcnnFaceDetector = MTCNN(context = application.baseContext)
        emotionClassifierTfLite = EmotionTfLiteClassifier(application.baseContext)

        database = Firebase.database.reference
    }

    fun processImage(): BehaviourRemoteInfo {
        val sleepyPercent = (listCaptureImage.filter { it.isSleepy }
            .map { it.isSleepy }.size / listCaptureImage.size) * 100
        val attentionScore = listCaptureImage.map { it.attentionScore }.average()
        var engagementState = "Undefined"
        var emotionState = "Undefined"
        if (attentionScore >= 50) {
            val processingList =
                listCaptureImage.map { it.imageCapture?.let { it1 -> detectEmotionRealTime(it1) } }
                    .toMutableList()
            processingList.removeIf { it == null }
            if (processingList.isNotEmpty()) {
                val ciScore = processingList.map { it!!.engagementValue }.average()
                emotionState = processingList.map { it?.emotionState }.groupingBy { it }.eachCount()
                    .maxBy { it.value }.key.toString()
                engagementState = convertEngagementLevel(ciScore)
            }
        }

        listCaptureImage.clear()
        return BehaviourRemoteInfo(
            isSleep = sleepyPercent >= 50,
            isFocus = attentionScore >= 50,
            emotion = emotionState,
            engagementState = engagementState
        )
    }

    fun processDetectFace(bitmap: ByteArray): Bitmap? {
        val captureBitmap = convertBitmap(bitmap)
        return mtcnnDetectionAndAttributesRecognition(captureBitmap)
    }

    fun convertBitmap(bitmap: ByteArray): Bitmap {
        val out = ByteArrayOutputStream()
        val yuvImage = YuvImage(bitmap, ImageFormat.NV21, 640, 480, null)
        yuvImage.compressToJpeg(Rect(0, 0, 640, 480), 100, out)
        val imageBytes: ByteArray = out.toByteArray()
        val image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val width: Int = image.width
        val height: Int = image.height
        val matrix = Matrix()
        matrix.postRotate((-90).toFloat())
        matrix.postScale(-1f, 1f, width / 2f, height / 2f)
        return Bitmap.createBitmap(image, 0, 0, width, height, matrix, true)
    }

    fun mtcnnDetectionAndAttributesRecognition(bitmap: Bitmap): Bitmap? {
        var resizedBitmap = bitmap
        val minSize = 600.0
        val scale = min(bitmap.width, bitmap.height) / minSize
        if (scale > 1.0) {
            resizedBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width / scale).toInt(),
                (bitmap.height / scale).toInt(),
                false
            )
            //bmp=resizedBitmap;
        }

        val startTime = SystemClock.uptimeMillis()
        val bboxes: Vector<Box> = mtcnnFaceDetector!!.detectFaces(
            resizedBitmap,
            minFaceSize
        )

        Log.i(
            "xxx",
            "Timecost to run mtcnn: " + (SystemClock.uptimeMillis() - startTime).toString()
        )
        val tempBmp =
            Bitmap.createBitmap(resizedBitmap.width, resizedBitmap.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(tempBmp)
        val p = Paint()
        p.style = Paint.Style.STROKE
        p.isAntiAlias = true
        p.isFilterBitmap = true
        p.isDither = true
        p.color = Color.BLUE
        p.strokeWidth = 5f
        val p_text = Paint()
        p_text.color = Color.WHITE
        p_text.style = Paint.Style.FILL
        p_text.color = Color.BLUE
        p_text.textSize = 24f
        c.drawBitmap(resizedBitmap, 0f, 0f, null)
        for (box in bboxes) {
            p.color = Color.RED
            val bbox: Rect =
                box.transform2Rect() //new android.graphics.Rect(Math.max(0,box.left()),Math.max(0,box.top()),box.right(),box.bottom());
            c.drawRect(bbox, p)
            if (emotionClassifierTfLite != null && bbox.width() > 0 && bbox.height() > 0) {
                val w = bitmap.width
                val h = bitmap.height
                val bboxOrig = Rect(
                    max(0, w * bbox.left / resizedBitmap.width),
                    max(0, h * bbox.top / resizedBitmap.height),
                    min(w, w * bbox.right / resizedBitmap.width),
                    min(h, h * bbox.bottom / resizedBitmap.height)
                )
                val faceBitmap = Bitmap.createBitmap(
                    bitmap,
                    bboxOrig.left,
                    bboxOrig.top,
                    bboxOrig.width(),
                    bboxOrig.height()
                )
                val resultBitmap = Bitmap.createScaledBitmap(
                    faceBitmap,
                    emotionClassifierTfLite!!.getImageSizeX(),
                    emotionClassifierTfLite!!.getImageSizeY(),
                    false
                )
                val emotionCategory =
                    emotionClassifierTfLite?.classifyFrame(resultBitmap).toString().split("#")
                val emotionType = emotionCategory.first()
                val emotionPercent = emotionCategory.last()
                val emotionWeight = mappingEngagement(emotionType)
                val engagementValue = (emotionWeight * emotionPercent.toFloat())
                val engagementState = convertEngagementLevel(engagementValue)
                val detectResult = BehaviourData(
                    emotionState = emotionType,
                    engagementState = engagementState,
                    emotionPercent = (emotionPercent.toDouble() * 100),
                    engagementValue = engagementValue
                )
                emotionResult.postValue(detectResult)
                c.drawText(
                    emotionType,
                    max(0, bbox.left).toFloat(),
                    max(0, bbox.top - 20).toFloat(),
                    p_text
                )
            }
        }
        return tempBmp
    }

    fun detectEmotionRealTime(bitmap: ByteArray): BehaviourData? {
        val captureBitmap = convertBitmap(bitmap)
        var resizedBitmap = captureBitmap
        val minSize = 600.0
        val scale = min(captureBitmap.width, captureBitmap.height) / minSize
        if (scale > 1.0) {
            resizedBitmap = Bitmap.createScaledBitmap(
                captureBitmap,
                (captureBitmap.width / scale).toInt(),
                (captureBitmap.height / scale).toInt(),
                false
            )
        }

        val startTime = SystemClock.uptimeMillis()
        val bboxes: Vector<Box> = mtcnnFaceDetector!!.detectFaces(
            resizedBitmap,
            minFaceSize
        )

        Log.i(
            "xxx",
            "Timecost to run mtcnn: " + (SystemClock.uptimeMillis() - startTime).toString()
        )
        for (box in bboxes) {
            val bbox: Rect =
                box.transform2Rect()
            if (emotionClassifierTfLite != null && bbox.width() > 0 && bbox.height() > 0) {
                val w = captureBitmap.width
                val h = captureBitmap.height
                val bboxOrig = Rect(
                    max(0, w * bbox.left / resizedBitmap.width),
                    max(0, h * bbox.top / resizedBitmap.height),
                    min(w, w * bbox.right / resizedBitmap.width),
                    min(h, h * bbox.bottom / resizedBitmap.height)
                )
                val faceBitmap = Bitmap.createBitmap(
                    captureBitmap,
                    bboxOrig.left,
                    bboxOrig.top,
                    bboxOrig.width(),
                    bboxOrig.height()
                )
                val resultBitmap = Bitmap.createScaledBitmap(
                    faceBitmap,
                    emotionClassifierTfLite!!.getImageSizeX(),
                    emotionClassifierTfLite!!.getImageSizeY(),
                    false
                )
                val emotionCategory =
                    emotionClassifierTfLite?.classifyFrame(resultBitmap).toString().split("#")
                val emotionType = emotionCategory.first()
                val emotionPercent = emotionCategory.last()
                val emotionWeight = mappingEngagement(emotionType)
                val engagementValue = (emotionWeight * emotionPercent.toFloat())
                val engagementState = convertEngagementLevel(engagementValue)
                return BehaviourData(
                    emotionState = emotionType,
                    engagementState = engagementState,
                    emotionPercent = (emotionPercent.toDouble() * 100),
                    engagementValue = engagementValue
                )
            }
        }
        return null
    }

    private fun mappingEngagement(emotion: String): Double {
        return when (emotion) {
            "Anger" -> {
                0.25
            }
            "Disgust" -> {
                0.2
            }
            "Fear" -> {
                0.3
            }
            "Happiness" -> {
                0.6
            }
            "Neutral" -> {
                0.9
            }
            "Sadness" -> {
                0.3
            }
            "Surprise" -> {
                0.6
            }
            else -> {
                0.0
            }
        }
    }

    private fun convertEngagementLevel(emotion: Double): String {
        return if (emotion >= 0.5) {
            return "High Engagement"
        } else if (0.2 < emotion && emotion < 0.5) {
            return "Normally Engagement"
        } else {
            return "Distracted"
        }
    }

    fun insertSessionUser(
        studentID: String,
        meetingID: String,
        behaviourRemoteInfo: BehaviourRemoteInfo
    ) {
        behaviourRemoteInfo.studentId = studentID
        database.child("roomSession").child(meetingID)
            .child(studentID)
            .setValue(behaviourRemoteInfo)
            .addOnSuccessListener {
                listCaptureImage.clear()
                Log.d("xxx", "database addOnSuccessListener")
            }
            .addOnFailureListener {
                Log.d("xxx", "database addOnFailureListener")
            }
    }

    fun updateRealtimeDatabase(
        studentID: String,
        meetingID: String,
        behaviourRemoteInfo: BehaviourRemoteInfo
    ) {
        database.child("roomSession").child(meetingID)
            .child(studentID)
            .updateChildren(behaviourRemoteInfo.toMap())
            .addOnSuccessListener {
                listCaptureImage.clear()
                Log.d("xxx", "database addOnSuccessListener")
            }
            .addOnFailureListener {
                Log.d("xxx", "database addOnFailureListener")
            }
    }

    private var meetingSeconds = 0

    val executeCaptureImage: MutableLiveData<Boolean> =
        MutableLiveData()

    val updateTimeMeeting: MutableLiveData<String> =
        MutableLiveData()

    fun startObserver(initialTime: Int) = heavyTaskScope.launch {
        meetingSeconds = initialTime
        while (isActive) {
            val hours = meetingSeconds / 3600
            val minutes = (meetingSeconds % 3600) / 60
            val secs = meetingSeconds % 60

            // Format the seconds into minutes,seconds.
            val time = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d", hours,
                minutes, secs
            )
            updateTimeMeeting.postValue(time)
            meetingSeconds++
            delay(1000)
        }
    }
}