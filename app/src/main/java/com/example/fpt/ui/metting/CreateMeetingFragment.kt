package com.example.fpt.ui.metting

import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.widget.Toast
import com.demo.domain.domain.entities.ErrorResult
import com.demo.domain.domain.response.MeetingInfo
import com.example.demothesisfpteduvn.R
import com.example.demothesisfpteduvn.databinding.FragmentCreateMeetingBinding
import com.example.fpt.ui.base.BaseFragment
import com.example.fpt.ui.metting.ultils.Constant
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import live.videosdk.rtc.android.lib.PeerConnectionUtils
import org.webrtc.*

@AndroidEntryPoint
class CreateMeetingFragment : BaseFragment<MeetingViewModel, FragmentCreateMeetingBinding>() {
    private var isMicEnabled = true

    private var isWebcamEnabled = true

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
        viewModel.getJoinRoomResponse().observe(
            viewLifecycleOwner
        ) { roomResponse ->
            binding.btnJoinMeeting.revertAnimation {
                binding.btnJoinMeeting.text = "Stop"
            }
            val roomBundle = Bundle()
            val meetingInfo = MeetingInfo()
            meetingInfo.meetingId = roomResponse.roomId
            meetingInfo.localParticipantName = binding.etName.text.toString().trim()
            roomBundle.putSerializable(Constant.BundleKey.MEETING_INFO, meetingInfo)
            navigate(R.id.action_createMeetingFragment_to_meetingCallFragment, roomBundle)
        }

        viewModel.apiErrorResponse.observe(
            viewLifecycleOwner
        ) {
            Toast.makeText(baseContext, "Error Occurred", Toast.LENGTH_LONG).show()
        }

    }


    override fun initActions() {
        binding.btnMic.setOnClickListener { toggleMic() }
        binding.btnWebcam.setOnClickListener { toggleWebcam() }

        binding.btnJoinMeeting.setOnClickListener {
            binding.btnJoinMeeting.startAnimation()
            val meetingId = binding.etMeetingId.text.toString().trim { it <= ' ' }
            val pattern = Regex("\\w{4}-\\w{4}-\\w{4}")
            if ("" == meetingId) {
                Toast.makeText(
                    context, "Please enter meeting ID",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (!pattern.matches(meetingId)) {
                Toast.makeText(
                    context, "Please enter valid meeting ID",
                    Toast.LENGTH_SHORT
                ).show()
            } else if ("" == binding.etMeetingId.text.toString()) {
                Toast.makeText(context, "Please Enter Name", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.joinMeetingRoom(meetingId)
            }
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


    override fun onRequestError(errorResponse: ErrorResult) {
    }

    private fun updateCameraView() {
        if (isWebcamEnabled) {
//             create PeerConnectionFactory
            initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(baseContext)
                    .createInitializationOptions()
            initializationOptions
            PeerConnectionFactory.initialize(initializationOptions)
            peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
            val surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", PeerConnectionUtils.getEglContext())

            // create VideoCapturer
            videoCapturer = createCameraCapturer()
            videoSource = peerConnectionFactory!!.createVideoSource(videoCapturer!!.isScreencast)
            videoCapturer?.initialize(
                surfaceTextureHelper,
                baseContext,
                videoSource?.capturerObserver
            )
            videoCapturer?.startCapture(480, 640, 30)
            // create VideoTrack
            videoTrack = peerConnectionFactory!!.createVideoTrack("100", videoSource)
            //display in localView
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
        closeCapture()
        super.onDestroy()
    }

    override fun onPause() {
        binding.joiningView.removeTrack()
        binding.joiningView.releaseSurfaceViewRenderer()
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