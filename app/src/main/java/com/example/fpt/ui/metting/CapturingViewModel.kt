package com.example.fpt.ui.metting

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.demo.domain.domain.entities.BehaviourInfo
import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse
import com.example.fpt.ui.base.BaseViewModel
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import live.videosdk.rtc.android.Meeting
import java.util.*

class CapturingViewModel : BaseViewModel() {

    var detector: FaceDetector? = null

    var listDetection: List<BehaviourInfo>? = null

    private var database: DatabaseReference

    var isCollectDone = false

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        detector = FaceDetection.getClient(options)

        database = Firebase.database.reference
    }

    fun processImage(image: Bitmap, meetingInfo: Meeting) {
        val firebaseVisionImage = InputImage.fromBitmap(image, 0)

        detector?.process(firebaseVisionImage)
            ?.addOnCompleteListener {
                if (it.isComplete) {
                    if (!it.result.isNullOrEmpty()) {
                        processFace(it.result)
                    } else {
                        val behaviourInfo = BehaviourInfo(
                            isSleep = false,
                            isLookAway = true,
                            emotion = "Unidentified"
                        )
                        updateValue(meetingInfo, behaviourInfo)
                    }
                }
            }
            ?.addOnFailureListener {
                Log.d("xxx", "exception $it")
            }

    }

    private fun processFace(faces: List<Face>) {
        val listBehaviourInfo  = faces.map {
            return BehaviourInfo().toMap()
        }
        for (face in faces) {

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
        database.child("roomSession").child(meeting?.meetingId ?: "")
            .child(meeting?.localParticipant?.id ?: "")
            .updateChildren(behaviourInfo.toMap())
            .addOnSuccessListener {
                isCollectDone = true
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
            if (secs % 20 == 0) {
                executeCaptureImage.postValue(true)
            }
            meetingSeconds++
            delay(1000)
        }
    }
}