package com.example.fpt.ui.metting

import android.app.Application
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.demo.domain.domain.entities.BehaviourInfo
import com.demo.domain.domain.entities.ErrorResult
import com.demo.domain.domain.response.SessionResponse
import com.example.fpt.classifer.ClassifierResult
import com.example.fpt.classifer.EmotionTfLiteClassifier
import com.example.fpt.classifer.model.BehaviourData
import com.example.fpt.mtcnn.Box
import com.example.fpt.mtcnn.MTCNN
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import kotlinx.coroutines.*
import live.videosdk.rtc.android.Meeting
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.max
import kotlin.math.min


class CapturingViewModel(application: Application) : AndroidViewModel(application) {

    var detector: FaceDetector? = null

    var listCaptureImage = mutableListOf<Bitmap>()

    private var database: DatabaseReference

    var recentAttentionScore: Int = 0

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
//        val options = FaceDetectorOptions.Builder()
//            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
//            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
//            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
//            .setMinFaceSize(0.15f)
//            .enableTracking()
//            .build()
//        detector = FaceDetection.getClient(options)
        mtcnnFaceDetector = MTCNN(context = application.baseContext)
        emotionClassifierTfLite = EmotionTfLiteClassifier(application.baseContext)

        database = Firebase.database.reference
    }

    fun processImage() {
        val processList = listCaptureImage.takeLast(10)
        processList.forEach { captureImage ->
//            mtcnnDetectionAndAttributesRecognition(captureImage)
        }
        listCaptureImage.clear()
    }

    fun processDetectFace(bitmap: ByteArray, score: Float): Bitmap? {
        val captureBitmap = convertBitmap(bitmap)
        return mtcnnDetectionAndAttributesRecognition(captureBitmap, score)
    }

    private fun convertBitmap(bitmap: ByteArray): Bitmap {
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

    private fun mtcnnDetectionAndAttributesRecognition(bitmap: Bitmap, score: Float): Bitmap? {
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
                val emotionCategory = emotionClassifierTfLite?.classifyFrame(resultBitmap).toString()
                val concentrationIndex =  mappingEngagement(emotionCategory)
//                val engagementLevel = getEngamentState()
                Log.d("xxx",emotionCategory)
                val detectResult = BehaviourData(emotionState = emotionCategory, engagementState ="100.0")
                emotionResult.postValue(detectResult)
                c.drawText(
                    emotionCategory,
                    max(0, bbox.left).toFloat(),
                    max(0, bbox.top - 20).toFloat(),
                    p_text
                )
            }
        }
        return tempBmp
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

    private fun insertSessionUser(meeting: Meeting?) {
        val behaviourInfo = BehaviourInfo(
            isSleep = false,
            isLookAway = false,
            emotion = "Unidentified"
        )
        database.child("roomSession").child(meeting?.meetingId ?: "")
            .child(meeting?.localParticipant?.id ?: "")
            .setValue(behaviourInfo)
            .addOnSuccessListener {
                Log.d("xxx", "database addOnSuccessListener")
            }
            .addOnFailureListener {
                Log.d("xxx", "database addOnFailureListener")
            }
    }

    private fun updateValue(meeting: Meeting?, behaviourInfo: BehaviourInfo) {
//        database.child("roomSession").child(meeting?.meetingId ?: "")
//            .child(meeting?.localParticipant?.id ?: "")
////            .updateChildren(behaviourInfo.toMap())
//            .addOnSuccessListener {
//                isCollectDone = true
//            }
//            .addOnFailureListener {
//                Log.d("xxx", "database addOnFailureListener")
//            }
    }

    private var meetingSeconds = 0

    val executeCaptureImage: MutableLiveData<Boolean> =
        MutableLiveData()

    val updateTimeMeeting: MutableLiveData<String> =
        MutableLiveData()

    fun startObserver(initialTime: Int, meetingInfo: Meeting?) = heavyTaskScope.launch {
        meetingSeconds = initialTime
        insertSessionUser(meetingInfo)
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