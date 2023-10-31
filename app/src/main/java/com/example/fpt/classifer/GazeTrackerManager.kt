package com.example.fpt.classifer

import android.content.Context
import camp.visual.gazetracker.GazeTracker
import camp.visual.gazetracker.callback.*
import camp.visual.gazetracker.constant.*
import java.lang.ref.WeakReference

enum class SeeSoInitializeState {
    default, initializing, initialized
}

class GazeTrackerManager private constructor(context: Context) {

    private val initializationCallbacks: MutableList<InitializationCallback> = ArrayList()
    private val statusCallbacks: MutableList<StatusCallback> = ArrayList()
    private val gazeCallbacks: MutableList<GazeCallback> = ArrayList()
    private val calibrationCallbacks: MutableList<CalibrationCallback> = ArrayList()
    private val userStatusCallbacks: MutableList<UserStatusCallback> = ArrayList()
    private val imageCallbacks: MutableList<ImageCallback> = ArrayList()

    // state control
    var isInitWithUserOption = false
    var initializeState: SeeSoInitializeState = SeeSoInitializeState.default

    private val mContext: WeakReference<Context> = WeakReference(context)
    private var gazeTracker: GazeTracker? = null

    private val SEESO_LICENSE_KEY = "dev_tq1gzmz9wd01jx1uh31rr6r49cvaymatvv982ifl"


    companion object {

        private var instance: GazeTrackerManager? = null
        fun makeNewInstance(context: Context): GazeTrackerManager? {
            instance.also { it?.deInitGazeTracker() }
            instance = GazeTrackerManager(context)
            return instance
        }
    }
   
    fun isTracking(): Boolean {
        return gazeTracker?.isTracking ?: false
    }

    fun isCalibrating(): Boolean {
        return gazeTracker?.isCalibrating ?: false
    }

    fun initGazeTracker(callback: InitializationCallback, isInitWithUserOption: Boolean) {
        initializationCallbacks.add(callback)
        var userOption: UserStatusOption? = null
        if (isInitWithUserOption) {
            userOption = UserStatusOption()
            userOption.useAll()
            this.isInitWithUserOption = true
        }
        initializeState = SeeSoInitializeState.initializing

        GazeTracker.initGazeTracker(
            mContext.get(),
            SEESO_LICENSE_KEY,
            initializationCallback,
            userOption
        )
    }

    fun deInitGazeTracker() {
        gazeTracker?.also { GazeTracker.deinitGazeTracker(it) }
        gazeTracker = null
        isInitWithUserOption = false
        initializeState = SeeSoInitializeState.default
    }
    fun getAttentionScore() = gazeTracker?.attentionScore

    fun setGazeTrackerCallbacks(vararg callbacks: GazeTrackerCallback?) {
        for (callback in callbacks) {
            when (callback) {
                is GazeCallback -> gazeCallbacks.add(callback)
                is CalibrationCallback -> calibrationCallbacks.add(callback)
                is StatusCallback -> statusCallbacks.add(callback)
                is UserStatusCallback -> userStatusCallbacks.add(callback)
                is ImageCallback -> imageCallbacks.add(callback)
            }
        }
    }

    fun setGazeTrackingFps(fps: Int): Boolean {
        return gazeTracker?.setTrackingFPS(fps) ?: false
    }

    fun startGazeTracking(): Boolean {
        gazeTracker?.setAttentionInterval(10)
        gazeTracker?.also {
            it.startTracking()
            return true
        }
        return false
    }

    fun stopGazeTracking(): Boolean {
        gazeTracker?.also {
            it.stopTracking()
            return true
        }
        return false
    }

    // Start Calibration
    fun startCalibration(modeType: CalibrationModeType?, criteria: AccuracyCriteria?): Boolean {
        return if (isTracking()) {
            gazeTracker?.startCalibration(modeType, criteria)
            true
        } else {
            false
        }
    }

    // Start Collect calibration sample data
    fun startCollectionCalibrationSamples(): Boolean {
        return if (isCalibrating()) {
            gazeTracker?.startCollectSamples()
            true
        } else {
            false
        }
    }

    // inner callbacks
    private val initializationCallback: InitializationCallback =
        InitializationCallback { tracker, error ->
            initializeState = SeeSoInitializeState.initialized
            gazeTracker = tracker
            for (initializationCallback in initializationCallbacks) {
                initializationCallback.onInitialized(gazeTracker, error)
            }
            initializationCallbacks.clear()
            gazeTracker?.setCallbacks(
                gazeCallback,
                calibrationCallback,
                statusCallback,
                userStatusCallback,
                imageCallback
            )
        }
    private val gazeCallback = GazeCallback { gazeInfo ->
        for (gazeCallback in gazeCallbacks) {
            gazeCallback.onGaze(gazeInfo)
        }
    }


    private val imageCallback: ImageCallback =
        ImageCallback { p0, p1 ->
            for (imageCallback in imageCallbacks) {
                imageCallback.onImage(p0, p1)
            }
        }

    private val calibrationCallback: CalibrationCallback = object : CalibrationCallback {
        override fun onCalibrationProgress(progress: Float) {
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationProgress(progress)
            }
        }

        override fun onCalibrationNextPoint(x: Float, y: Float) {
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationNextPoint(x, y)
            }
        }

        override fun onCalibrationFinished(calibrationData: DoubleArray) {
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationFinished(calibrationData)
            }
        }
    }
    private val statusCallback: StatusCallback = object : StatusCallback {
        override fun onStarted() {
            for (statusCallback in statusCallbacks) {
                statusCallback.onStarted()
            }
        }

        override fun onStopped(statusErrorType: StatusErrorType) {
            for (statusCallback in statusCallbacks) {
                statusCallback.onStopped(statusErrorType)
            }
        }
    }
    private val userStatusCallback: UserStatusCallback = object : UserStatusCallback {
        override fun onAttention(timestampBegin: Long, timestampEnd: Long, score: Float) {
            for (userStatusCallback in userStatusCallbacks) {
                userStatusCallback.onAttention(timestampBegin, timestampEnd, score)
            }
        }

        override fun onBlink(
            timestamp: Long,
            isBlinkLeft: Boolean,
            isBlinkRight: Boolean,
            isBlink: Boolean,
            eyeOpenness: Float
        ) {
            for (userStatusCallback in userStatusCallbacks) {
                userStatusCallback.onBlink(
                    timestamp,
                    isBlinkLeft,
                    isBlinkRight,
                    isBlink,
                    eyeOpenness
                )
            }
        }

        override fun onDrowsiness(timestamp: Long, isDrowsiness: Boolean) {
            for (userStatusCallback in userStatusCallbacks) {
                userStatusCallback.onDrowsiness(timestamp, isDrowsiness)
            }
        }
    }
}