package com.example.fpt.ui.login

import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import camp.visual.gazetracker.callback.*
import com.demo.domain.domain.entities.ErrorResult
import com.example.demothesisfpteduvn.R
import com.example.demothesisfpteduvn.databinding.FragmentEngagementVisuallyBinding
import com.example.fpt.classifer.EmotionTfLiteClassifier
import com.example.fpt.classifer.GazeTrackerManager
import com.example.fpt.mtcnn.MTCNN
import com.example.fpt.ui.base.BaseFragment
import com.example.fpt.ui.metting.CapturingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlinx.dataframe.api.*
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class EngagementVisuallyFragment :
    BaseFragment<LoginViewModel, FragmentEngagementVisuallyBinding>() {

    private var emotionClassifierTfLite: EmotionTfLiteClassifier? = null

    private var gazeTrackerManager: GazeTrackerManager? = null

    var isGazeTrackingStarting = false

    var gazeTrackerFPS: Int = 30

    private val captureViewModel: CapturingViewModel by activityViewModels()


    private val backgroundThread: HandlerThread = HandlerThread("background")

    private var backgroundHandler: Handler? = null

    private var mtcnnFaceDetector: MTCNN? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emotionClassifierTfLite = EmotionTfLiteClassifier(baseContext)
        mtcnnFaceDetector = MTCNN(baseContext)

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
        InitializationCallback { _, error ->
            Log.d("xxx", error.toString())
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
        }


        override fun onCalibrationFinished(calibrationData: DoubleArray?) {
        }
    }
    private val userStatusCallback = object : UserStatusCallback {
        override fun onAttention(timestampBegin: Long, timestampEnd: Long, score: Float) {
            binding.attentionScore.text = "${(score * 100).toInt()}%"
            Log.d("xxx", "end ${timestampEnd}")
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
            Log.d("xxx", "onDrowsiness ${timestamp}")

            binding.sleepyState.text = if (isDrowsiness) "yes.." else "NO!"
        }
    }

    private val imageCallBack = ImageCallback { p, captureImage ->
        Log.d("xxx", "ImageCallback ${p}")

        val bitmap = captureViewModel.processDetectFace(captureImage)
//        binding.imageView.setImageBitmap(bitmap)
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