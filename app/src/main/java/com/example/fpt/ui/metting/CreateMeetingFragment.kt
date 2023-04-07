package com.example.fpt.ui.metting

import android.R.attr.x
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.Toast
import camp.visual.gazetracker.callback.*
import camp.visual.gazetracker.constant.StatusErrorType
import com.demo.domain.domain.entities.ErrorResult
import com.demo.domain.domain.response.MeetingInfo
import com.example.demothesisfpteduvn.R
import com.example.demothesisfpteduvn.databinding.FragmentCreateMeetingBinding
import com.example.fpt.classifer.GazeTrackerManager
import com.example.fpt.ui.base.BaseFragment
import com.example.fpt.ui.metting.ultils.Constant
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.webrtc.*
import java.io.ByteArrayOutputStream


@AndroidEntryPoint
class CreateMeetingFragment : BaseFragment<MeetingViewModel, FragmentCreateMeetingBinding>() {
    private var isMicEnabled = true

    private var isWebcamEnabled = true

    private var videoTrack: VideoTrack? = null

    private var videoCapturer: VideoCapturer? = null

    private var initializationOptions: PeerConnectionFactory.InitializationOptions? = null

    private var peerConnectionFactory: PeerConnectionFactory? = null

    private var videoSource: VideoSource? = null

    private var gazeTrackerManager: GazeTrackerManager? = null

    var isGazeTrackingStarting = false

    var gazeTrackerFPS: Int = 30

    var recentAttentionScore: Int = 0

    // Thread control
    private val backgroundThread: HandlerThread = HandlerThread("background")
    private var backgroundHandler: Handler? = null
    private fun initHandler() {
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    override fun initView() {
        initHandler()
        gazeTrackerManager = GazeTrackerManager.makeNewInstance(baseContext)
        changeFloatingActionButtonLayout(binding.btnWebcam, isMicEnabled)
        changeFloatingActionButtonLayout(binding.btnMic, isMicEnabled)
        updateCameraView()
        gazeTrackerManager?.setGazeTrackerCallbacks(
            gazeCallback,
            calibrationCallback,
            statusCallback,
            userStatusCallback,
            imageCallBack
        )
        initGazeTracker()

    }
    private val gazeCallback = GazeCallback { gazeInfo ->
        if (isGazeTrackingStarting) {
            isGazeTrackingStarting = false
        }
        if (gazeTrackerManager?.isCalibrating() == false) {
            Log.d("xxx", "gazeInfo.x ${gazeInfo.x} ${gazeInfo.y}")

            runBlocking(Dispatchers.Main) {
                binding.gazePointView.apply {
                    x = gazeInfo.x
                    y = gazeInfo.y
                }
            }
        }
    }

    private val calibrationCallback = object : CalibrationCallback {
        override fun onCalibrationProgress(progress: Float) {

        }

        override fun onCalibrationNextPoint(p0: Float, p1: Float) {
            TODO("Not yet implemented")
        }


        override fun onCalibrationFinished(calibrationData: DoubleArray?) {
        }
    }
    private val imageCallBack = ImageCallback { _, p1 ->
        runBlocking (Dispatchers.Main) {
            val out = ByteArrayOutputStream()
            val yuvImage = YuvImage(p1, ImageFormat.NV21, 640, 480, null)
            yuvImage.compressToJpeg(Rect(0, 0, 640, 480), 100, out)
            val imageBytes: ByteArray = out.toByteArray()
            val image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)


            val width: Int = image.width
            val height: Int = image.height
            // calculate the scale - in this case = 0.4f
            // calculate the scale - in this case = 0.4f
            val scaleWidth = 480.toFloat() / width
            val scaleHeight = 640.toFloat() / height
            val matrix = Matrix()
            matrix.postScale(scaleWidth, scaleHeight)
            matrix.postRotate((-90).toFloat())

            val resizedBitmap = Bitmap.createBitmap(image, 0, 0, width, height, matrix, true)


            binding.joiningView.setImageBitmap(resizedBitmap);
        }
    }

    private val userStatusCallback = object : UserStatusCallback {
        override fun onAttention(timestampBegin: Long, timestampEnd: Long, score: Float) {
            recentAttentionScore = (score * 100).toInt()
            Log.d("xxx", "recentAttentionScore $recentAttentionScore")

        }

        override fun onBlink(
            timestamp: Long,
            isBlinkLeft: Boolean,
            isBlinkRight: Boolean,
            isBlink: Boolean,
            eyeOpenness: Float
        ) {
        }

        override fun onDrowsiness(timestamp: Long, isDrowsiness: Boolean) {

        }
    }

    private val statusCallback = object : StatusCallback {
        override fun onStarted() {
            // will be called after gaze tracking started
        }

        override fun onStopped(error: StatusErrorType?) {
            // Check https://docs.seeso.io/docs/api/android-api-docs#statuserrortype
            when (error) {
                StatusErrorType.ERROR_NONE -> {}
                StatusErrorType.ERROR_CAMERA_START -> {}
                StatusErrorType.ERROR_CAMERA_INTERRUPT -> {}
                else -> {}
            }
        }
    }
    override fun initData() {
        viewModel.getJoinRoomResponse().observe(
            viewLifecycleOwner
        ) { roomResponse ->
            val roomBundle = Bundle()
            var meetingInfo = MeetingInfo()
            meetingInfo.meetingId = roomResponse.roomId
            meetingInfo.localParticipantName = binding.etName.text.toString().trim()
            roomBundle.putSerializable(Constant.BundleKey.MEETING_INFO, meetingInfo)
            navigate(R.id.action_createMeetingFragment_to_meetingCallFragment, roomBundle)
        }

        viewModel.apiErrorResponse.observe(
            viewLifecycleOwner
        ) {
            Toast.makeText(baseContext, "Error Occurred", Toast.LENGTH_LONG)
        }

    }
    private fun initGazeTracker() {
        gazeTrackerManager?.initGazeTracker(initializationCallback, true)

        // You can also set FPS of gazeTracker if you want
        gazeTrackerManager?.setGazeTrackingFps(gazeTrackerFPS)
    }

    private val initializationCallback = // Note: for understanding, left as function here
        // you can change this to lambda
        InitializationCallback { gazeTracker, error ->

            runBlocking (Dispatchers.Main) {
                Log.d("xxx","${gazeTracker !== null}")
            }

        }

    override fun initActions() {
        binding.btnMic.setOnClickListener { toggleMic() }
        binding.btnWebcam.setOnClickListener { toggleWebcam() }

        binding.btnJoinMeeting.setOnClickListener {
//            val meetingId = binding.etMeetingId.text.toString().trim { it <= ' ' }
//            val pattern = Regex("\\w{4}-\\w{4}-\\w{4}")
//            if ("" == meetingId) {
//                Toast.makeText(
//                    context, "Please enter meeting ID",
//                    Toast.LENGTH_SHORT
//                ).show()
//            } else if (!pattern.matches(meetingId)) {
//                Toast.makeText(
//                    context, "Please enter valid meeting ID",
//                    Toast.LENGTH_SHORT
//                ).show()
//            } else if ("" == binding.etMeetingId.text.toString()) {
//                Toast.makeText(context, "Please Enter Name", Toast.LENGTH_SHORT).show()
//            } else {
//                viewModel.joinMeetingRoom(meetingId)
//            }
            startTracking()
        }
    }

    private fun changeFloatingActionButtonLayout(btn: FloatingActionButton?, enabled: Boolean) {
        if (enabled) {
            btn?.setColorFilter(Color.BLACK)
            btn?.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.md_grey_300))
        } else {
            btn?.setColorFilter(Color.WHITE)
            btn?.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.md_red_500))
        }
    }

    private fun toggleMic() {
        isMicEnabled = !isMicEnabled
        if (isMicEnabled) {
            binding.btnMic.setImageResource(R.drawable.ic_mic_on)
        } else {
            binding.btnMic.setImageResource(R.drawable.ic_mic_off)
        }
        changeFloatingActionButtonLayout(binding.btnMic, isMicEnabled)
    }

    private fun toggleWebcam() {
        isWebcamEnabled = !isWebcamEnabled
        if (isWebcamEnabled) {
            binding.btnWebcam.setImageResource(R.drawable.ic_video_camera)
        } else {
            binding.btnWebcam.setImageResource(R.drawable.ic_video_camera_off)
        }
        updateCameraView()
        changeFloatingActionButtonLayout(binding.btnWebcam, isWebcamEnabled)
    }
    private fun startTracking() {
        backgroundHandler?.post {
            gazeTrackerManager?.startGazeTracking()
        }
        isGazeTrackingStarting = true
    }

    override fun onRequestError(errorResponse: ErrorResult) {
    }

    private fun updateCameraView() {
        if (isWebcamEnabled) {
            // create PeerConnectionFactory
//            initializationOptions =
//                PeerConnectionFactory.InitializationOptions.builder(baseContext)
//                    .createInitializationOptions()
//            initializationOptions
//            PeerConnectionFactory.initialize(initializationOptions)
//            peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
////            binding.joiningView.setMirror(true)
//            val surfaceTextureHelper =
//                SurfaceTextureHelper.create("CaptureThread", PeerConnectionUtils.getEglContext())
//
//            // create VideoCapturer
//            videoCapturer = createCameraCapturer()
//            videoSource = peerConnectionFactory!!.createVideoSource(videoCapturer!!.isScreencast)
//            videoCapturer?.initialize(
//                surfaceTextureHelper,
//                baseContext,
//                videoSource?.capturerObserver
//            )
//            gazeTrackerManager
////            videoCapturer?.startCapture(480, 640, 30)
//
//            // create VideoTrack
//            videoTrack = peerConnectionFactory!!.createVideoTrack("100", videoSource)
//
            // display in localView
//            binding.joiningView.addTrack(videoTrack)
        } else {
//            binding.joiningView.removeTrack()
//            binding.joiningView.releaseSurfaceViewRenderer()
        }
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera1Enumerator(false)
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    override fun onDestroy() {
//        binding.joiningView.removeTrack()
//        binding.joiningView.releaseSurfaceViewRenderer()
        closeCapture()
        super.onDestroy()
    }

    override fun onPause() {
//        binding.joiningView.removeTrack()
//        binding.joiningView.releaseSurfaceViewRenderer()
        closeCapture()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        updateCameraView()
    }

    private fun closeCapture() {
        if (videoCapturer != null) {
            try {
                videoCapturer!!.stopCapture()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            videoCapturer!!.dispose()
            videoCapturer = null
        }
        if (videoSource != null) {
            videoSource!!.dispose()
            videoSource = null
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory?.stopAecDump()
            peerConnectionFactory?.dispose()
            peerConnectionFactory = null
        }
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }

    override fun provideLayoutId() = R.layout.fragment_create_meeting

    override fun isNeedHideBottomBar() = true

    override fun provideViewModelClass() = MeetingViewModel::class.java
}