package com.example.fpt.ui.metting

import android.app.Application
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.demo.domain.domain.entities.BehaviourInfo
import com.demo.domain.domain.entities.ErrorResult
import com.example.fpt.classifer.ClassifierResult
import com.example.fpt.classifer.EmotionTfLiteClassifier
import com.example.fpt.classifer.TfLiteClassifier
import com.example.fpt.mtcnn.Box
import com.example.fpt.mtcnn.MTCNN
import com.example.fpt.ui.base.BaseViewModel
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import kotlinx.coroutines.*
import live.videosdk.rtc.android.Meeting
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

    val heavyTaskScope by lazy { CoroutineScope(heavyJob + Dispatchers.IO + exceptionHandler) }

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
            mtcnnDetectionAndAttributesRecognition(captureImage)
        }
        listCaptureImage.clear()
    }

    private fun mtcnnDetectionAndAttributesRecognition(sampledImage: Bitmap) {
        val bmp: Bitmap = sampledImage
        var resizedBitmap = bmp
        val minSize = 600.0
        val scale = min(bmp.width, bmp.height) / minSize
        if (scale > 1.0) {
            resizedBitmap = Bitmap.createScaledBitmap(
                bmp,
                (bmp.width / scale).toInt(),
                (bmp.height / scale).toInt(),
                false
            )
            //bmp=resizedBitmap;
        }
        val startTime = SystemClock.uptimeMillis()
        val bboxes: Vector<Box> = mtcnnFaceDetector!!.detectFaces(
            resizedBitmap,
            minFaceSize
        ) //(int)(bmp.getWidth()*MIN_FACE_SIZE));
        Log.i(
            "xxx",
            "Timecost to run mtcnn: " + (SystemClock.uptimeMillis() - startTime).toString()
        )
        for (box in bboxes) {
            val bbox: Rect =
                box.transform2Rect() //new android.graphics.Rect(Math.max(0,box.left()),Math.max(0,box.top()),box.right(),box.bottom());
            if (emotionClassifierTfLite != null && bbox.width() > 0 && bbox.height() > 0) {
                val w = bmp.width
                val h = bmp.height
                val bboxOrig = Rect(
                    max(0, w * bbox.left / resizedBitmap.width),
                    max(0, h * bbox.top / resizedBitmap.height),
                    min(w, w * bbox.right / resizedBitmap.width),
                    min(h, h * bbox.bottom / resizedBitmap.height)
                )
                val faceBitmap = Bitmap.createBitmap(
                    bmp,
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
                val res = emotionClassifierTfLite?.classifyFrame(resultBitmap)
                Log.i("xxx", res.toString())
            }
        }
    }


    private fun processFace(faces: List<Face>) {

        Log.d("xxx", faces.size.toString())

        for (face in faces) {
            Log.d("xxx", face.boundingBox.flattenToString())
            Log.d("xxx", face.boundingBox.toString())

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