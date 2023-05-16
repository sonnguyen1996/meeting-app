package com.example.fpt.ui.login

import android.content.res.Resources
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
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

    var isCaptureByInterval = false

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
        captureViewModel.emotionResult.observe(
            viewLifecycleOwner
        ) {
            binding.emotionState.text =
                "${it.emotionState}(${String.format("%.2f", it.emotionPercent)}%)"
            binding.engagementState.text = "${it.engagementState}"
        }
    }

    private fun initGazeTracker() {
        gazeTrackerManager?.initGazeTracker(initializationCallback, true)

        // You can also set FPS of gazeTracker if you want
        gazeTrackerManager?.setGazeTrackingFps(gazeTrackerFPS)
    }

    private val initializationCallback = // Note: for understanding, left as function here
        // you can change this to lambda
        InitializationCallback { _, error ->
            runBlocking(Dispatchers.Main) {
                binding.startCapture.doneLoadingAnimation(R.color.md_red_400, defaultDoneImage())
                binding.startCapture.revertAnimation{
                    binding.startCapture.text ="Stop"
                }
            }
            startTracking()
            updateViewState()
        }


    private fun startTracking() {
        backgroundHandler?.post {
            gazeTrackerManager?.startGazeTracking()
        }
    }
    private fun defaultDoneImage() =
        BitmapFactory.decodeResource(resources, R.drawable.ic_done_white_48dp)

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
            if (!isGazeTrackingStarting) {
                isGazeTrackingStarting = true

                binding.startCapture.startAnimation()
                initGazeTracker()
            } else {
                binding.startCapture.doneLoadingAnimation(R.color.md_red_400, defaultDoneImage())
                binding.startCapture.revertAnimation {
                    binding.startCapture.text = "Start"
                }
                gazeTrackerManager?.stopGazeTracking()
                isGazeTrackingStarting = false

            }
        }

        binding.captureCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            isCaptureByInterval = isChecked
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
            var attensionScore = (score * 100)
            binding.attentionScore.text = "${attensionScore}%"
            if (isCaptureByInterval) {
                binding.imageView.invalidate()
                val drawable = binding.imageView.drawable
                val bitmap = drawable.toBitmap()
                val processedBitmap =
                    captureViewModel.mtcnnDetectionAndAttributesRecognition(bitmap)
                binding.imageView.setImageBitmap(processedBitmap)

            }
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

    private val imageCallBack = ImageCallback { p, captureImage ->
        var resultBitmap: Bitmap? = if (!isCaptureByInterval) {
            captureViewModel.processDetectFace(captureImage)
        } else {
            captureViewModel.convertBitmap(captureImage)
        }
        binding.imageView.setImageBitmap(resultBitmap)
    }

    private val gazeCallback = GazeCallback { gazeInfo ->
        if (isGazeTrackingStarting) {
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