package com.example.fpt.ui.metting

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.demo.domain.domain.entities.ErrorResult
import com.example.demothesisfpteduvn.R
import com.example.demothesisfpteduvn.databinding.FragmentCreateMeetingBinding
import com.example.fpt.ui.base.BaseFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import live.videosdk.rtc.android.lib.PeerConnectionUtils
import org.webrtc.*

class CreateMeetingFragment : BaseFragment<MeetingViewModel, FragmentCreateMeetingBinding>() {
    var isMicEnabled = true

    var isWebcamEnabled = true

    private var videoTrack: VideoTrack? = null

    private var videoCapturer: VideoCapturer? = null

    private var initializationOptions: PeerConnectionFactory.InitializationOptions? = null

    private var peerConnectionFactory: PeerConnectionFactory? = null

    private var videoSource: VideoSource? = null

    override fun initView() {
        changeFloatingActionButtonLayout(binding.btnWebcam, isMicEnabled)
        changeFloatingActionButtonLayout(binding.btnMic, isMicEnabled)
        updateCameraView()
    }

    override fun initData() {
    }

    override fun initActions() {
        binding.btnMic.setOnClickListener { toggleMic() }
        binding.btnWebcam.setOnClickListener { toggleWebcam() }
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

    override fun onRequestError(errorResponse: ErrorResult) {
    }

    private fun updateCameraView() {
        if (isWebcamEnabled) {
            // create PeerConnectionFactory
            initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(baseContext)
                    .createInitializationOptions()
            PeerConnectionFactory.initialize(initializationOptions)
            peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
            binding.joiningView.setMirror(true)
            val surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", PeerConnectionUtils.getEglContext())

            // create VideoCapturer
            videoCapturer = createCameraCapturer()
            videoSource = peerConnectionFactory!!.createVideoSource(videoCapturer!!.isScreencast)
            videoCapturer!!.initialize(
                surfaceTextureHelper,
                baseContext,
                videoSource!!.capturerObserver
            )
            videoCapturer!!.startCapture(480, 640, 30)

            // create VideoTrack
            videoTrack = peerConnectionFactory!!.createVideoTrack("100", videoSource)

            // display in localView
            binding.joiningView.addTrack(videoTrack)
        } else {
            binding.joiningView.removeTrack()
            binding.joiningView.releaseSurfaceViewRenderer()
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
        binding.joiningView.removeTrack()
        binding.joiningView.releaseSurfaceViewRenderer()
        closeCapturer()
        super.onDestroy()
    }

    override fun onPause() {
        binding.joiningView.removeTrack()
        binding.joiningView.releaseSurfaceViewRenderer()
        closeCapturer()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        updateCameraView()
    }

    private fun closeCapturer() {
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
            peerConnectionFactory!!.stopAecDump()
            peerConnectionFactory!!.dispose()
            peerConnectionFactory = null
        }
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }

    override fun provideLayoutId() = R.layout.fragment_create_meeting

    override fun isNeedHideBottomBar() = false

    override fun provideViewModelClass() = MeetingViewModel::class.java
}