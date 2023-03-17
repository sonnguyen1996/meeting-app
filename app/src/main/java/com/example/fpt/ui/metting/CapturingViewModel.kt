package com.example.fpt.ui.metting

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse
import com.example.fpt.ui.base.BaseViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*

class CapturingViewModel  : BaseViewModel() {

    var detector : FaceDetector? = null
    init{
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        detector = FaceDetection.getClient(options)
    }

    fun processImage(image: Bitmap) {
        val firebaseVisionImage = InputImage.fromBitmap(image, 0)

        detector?.process(firebaseVisionImage)
            ?.addOnSuccessListener {
                processFace(it)
            }
            ?.addOnFailureListener {
            }

    }

    private fun processFace(faces: List<Face>) {
        for (face in faces) {
            val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
            val rotX = face.headEulerAngleX // Head is tilted sideways rotZ degrees

            val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees
            Log.d("xxx", " turn right $rotY")
            Log.d("xxx", " turn up $rotX")
            Log.d("xxx", " turn left $rotZ")
            Log.d("xxx", " eye right ${face.rightEyeOpenProbability}")
            Log.d("xxx", " eye left ${face.leftEyeOpenProbability}")
            Log.d("xxx", " smile ${face.smilingProbability}")
            Log.d("xxx", " ========================= ")

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
            if (secs % 5 == 0) {
              executeCaptureImage.postValue(true)
            }
            meetingSeconds++
            delay(1000)
        }
    }
}