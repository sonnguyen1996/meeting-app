package com.example.fpt.ui.login

import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import camp.visual.gazetracker.callback.*
import camp.visual.gazetracker.gaze.GazeInfo
import com.demo.domain.domain.entities.ErrorResult
import com.example.demothesisfpteduvn.R
import com.example.demothesisfpteduvn.databinding.FragmentEngagementVisuallyBinding
import com.example.fpt.classifer.GazeTrackerManager
import com.example.fpt.classifer.SeeSoInitializeState
import com.example.fpt.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class EngagementVisuallyFragment :
    BaseFragment<LoginViewModel, FragmentEngagementVisuallyBinding>() {

    private var gazeTrackerManager: GazeTrackerManager? = null

    var isGazeTrackingStarting = false

    var gazeTrackerFPS: Int = 30

    // SeeSo UserStatus

    var blinked = false

    var isSleepy = false

    private val backgroundThread: HandlerThread = HandlerThread("background")

    private var backgroundHandler: Handler? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun initView() {
        gazeTrackerManager = GazeTrackerManager.makeNewInstance(baseContext)
        initHandler()
        gazeTrackerManager?.setGazeTrackerCallbacks(
            gazeCallback,
            calibrationCallback,
            userStatusCallback,
            imageCallBack
        )
    }

    override fun initData() {

    }

    private fun initGazeTracker() {
        gazeTrackerManager?.initGazeTracker(initializationCallback, true)

        // You can also set FPS of gazeTracker if you want
        gazeTrackerManager?.setGazeTrackingFps(gazeTrackerFPS)
    }

    private val initializationCallback = // Note: for understanding, left as function here
        // you can change this to lambda
        InitializationCallback { gazeTracker, error ->
             Log.d("xxx",error.toString())
            runBlocking(Dispatchers.Main) {
                binding.startCapture.revertAnimation {
                    binding.startCapture.text = "Stop"
                }
            }
            startTracking()
            updateViewState()
        }

    private fun startTracking() {
        backgroundHandler?.post {
            gazeTrackerManager?.startGazeTracking()
        }
        isGazeTrackingStarting = true
    }

    // Thread control
    private fun initHandler() {
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun releaseHandler() {
        backgroundThread.quitSafely()
    }

    override fun initActions() {
        binding.startCapture.setOnClickListener {
            binding.startCapture.startAnimation()
            initGazeTracker()
        }
    }

    private fun updateViewState() {

        // ------ tracking ------ //
        binding.gazePointView.apply {
            pivotX = (width / 2).toFloat()
            pivotY = (height / 2).toFloat()
        }

        binding.attentionScore.text = ""
        binding.blinkState.text = ""
        binding.sleepyState.text = ""
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
    private val userStatusCallback = object : UserStatusCallback {
        override fun onAttention(timestampBegin: Long, timestampEnd: Long, score: Float) {
            binding.attentionScore.text = "${(score * 100).toInt()}%"
        }

        override fun onBlink(
            timestamp: Long,
            isBlinkLeft: Boolean,
            isBlinkRight: Boolean,
            isBlink: Boolean,
            eyeOpenness: Float
        ) {
            binding.blinkState.text = if (isBlink) "X_X" else "0_0"
        }

        override fun onDrowsiness(timestamp: Long, isDrowsiness: Boolean) {

            binding.sleepyState.text = if (isDrowsiness) "yes.." else "NO!"
        }
    }

    private val imageCallBack = ImageCallback { p, p1 ->
        runBlocking(Dispatchers.Main) {
            val out = ByteArrayOutputStream()
            val yuvImage = YuvImage(p1, ImageFormat.NV21, 640, 480, null)
            yuvImage.compressToJpeg(Rect(0, 0, 640, 480), 100, out)
            val imageBytes: ByteArray = out.toByteArray()
            val image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            val width: Int = image.width
            val height: Int = image.height
            val matrix = Matrix()
            matrix.postRotate((-90).toFloat())
            matrix.postScale(-1f, 1f, width / 2f, height / 2f)
            val resizedBitmap = Bitmap.createBitmap(image, 0, 0, width, height, matrix, true)
            binding.imageView.setImageBitmap(resizedBitmap)
        }
    }

    private val gazeCallback = GazeCallback { gazeInfo ->
        if (isGazeTrackingStarting) {
            isGazeTrackingStarting = false
            updateViewState()
        }
        if (gazeTrackerManager?.isCalibrating() == false) {
            runBlocking(Dispatchers.Main) {
                binding.gazePointView.apply {
                    x = gazeInfo.x
                    y = gazeInfo.y
                }
            }
        }
    }

    override fun onRequestError(errorResponse: ErrorResult) {
    }

    override fun provideLayoutId() = R.layout.fragment_engagement_visually

    override fun isNeedHideBottomBar() = true

    override fun provideViewModelClass() = LoginViewModel::class.java
}