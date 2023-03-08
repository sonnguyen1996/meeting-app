package com.example.fpt.ui.metting

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.demo.domain.domain.entities.ErrorResult
import com.demo.domain.domain.response.MeetingInfo
import com.example.demothesisfpteduvn.BuildConfig
import com.example.demothesisfpteduvn.R
import com.example.demothesisfpteduvn.databinding.FragmentMeetingCallBinding
import com.example.fpt.ui.base.BaseFragment
import com.example.fpt.ui.metting.ultils.Constant
import com.example.fpt.ui.metting.ultils.HelperClass
import com.example.fpt.ui.metting.ultils.MeetingMenuItem
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import live.videosdk.rtc.android.*
import com.example.fpt.ui.adapter.AudioDeviceListAdapter
import com.example.fpt.ui.adapter.LeaveOptionListAdapter
import com.example.fpt.ui.adapter.MoreOptionsListAdapter
import dagger.hilt.android.AndroidEntryPoint
import live.videosdk.rtc.android.lib.AppRTCAudioManager
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.MicRequestListener
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import live.videosdk.rtc.android.listeners.WebcamRequestListener
import live.videosdk.rtc.android.model.PubSubPublishOptions
import org.json.JSONObject
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@AndroidEntryPoint
class MeetingCallFragment : BaseFragment<MeetingViewModel, FragmentMeetingCallBinding>() {
    private var meeting: Meeting? = null

    private var micEnabled = true

    private var webcamEnabled = true

    private var recording = false

    private var localScreenShare = false

    private var recordingStatusSnackbar: Snackbar? = null

    private val handler: Looper = Looper.getMainLooper()

    private var screenshareEnabled = false

    private var screenShareParticipantNameSnackbar: Snackbar? = null

    private var selectedAudioDeviceName: String? = null

    var onTouchListener: View.OnTouchListener? = null

    private var bottomSheetDialog: BottomSheetDialog? = null

    private val CAPTURE_PERMISSION_REQUEST_CODE = 1

    private var meetingInfo: MeetingInfo? = null

    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            meetingInfo =
                arguments?.getSerializable(Constant.BundleKey.MEETING_INFO) as? MeetingInfo
        }
    }

    override fun initView() {
        initMeeting()

        binding.txtMeetingId.text = meetingInfo?.meetingId

        recordingStatusSnackbar?.view?.let { HelperClass.setSnackBarStyle(it, 0) }

        recordingStatusSnackbar?.isGestureInsetBottomIgnored = true

    }


    override fun initData() {
        viewModel.getMeetingTime().observe(
            viewLifecycleOwner
        ) { sessionInfo ->
            val startMeetingTime = sessionInfo?.data?.get(0)?.start
            var startMeetingDate: Date? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startMeetingDate = Date.from(Instant.parse(startMeetingTime))
            }
            val currentTime = Calendar.getInstance().time
            val difference = currentTime.time - startMeetingDate?.time!!
            Math.toIntExact(TimeUnit.MILLISECONDS.toSeconds(difference))
//
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initActions() {
        meeting?.addEventListener(meetingEventListener)

        setActionListeners()
        setAudioDeviceListeners()

        binding.btnAudioSelection.setOnClickListener { showAudioInputDialog() }
        binding.btnStopScreenShare.setOnClickListener {
            if (localScreenShare) {
                meeting!!.disableScreenShare()
            }
        }
        recordingStatusSnackbar = Snackbar.make(
            binding.mainLayout, "Recording will be started in few moments",
            Snackbar.LENGTH_INDEFINITE
        )

        binding.participantsLayout.setOnTouchListener(onTouchListener)

        binding.ivParticipantScreenShareNetwork.setOnClickListener {
            val participantList = getAllParticipants()
            val participant = participantList[0]
            val popupwindow_obj: PopupWindow? = HelperClass().callStatsPopupDisplay(
                participant,
                binding.ivParticipantScreenShareNetwork,
                baseContext,
                true
            )
            popupwindow_obj!!.showAsDropDown(
                binding.ivParticipantScreenShareNetwork,
                -350,
                -85
            )
        }

        binding.ivLocalScreenShareNetwork.setOnClickListener {
            val popupwindow_obj: PopupWindow? = HelperClass().callStatsPopupDisplay(
                meeting!!.localParticipant,
                binding.ivLocalScreenShareNetwork,
                baseContext,
                true
            )
            popupwindow_obj!!.showAsDropDown(
                binding.ivLocalScreenShareNetwork,
                -350,
                -85
            )
        }

    }

    private fun initMeeting() {
        VideoSDK.config(BuildConfig.AUTH_TOKEN)
        val customTracks: MutableMap<String, CustomStreamTrack> = HashMap()

        val videoCustomTrack = VideoSDK.createCameraVideoTrack(
            "h720p_w960p",
            "front",
            CustomStreamTrack.VideoMode.TEXT,
            context
        )
        customTracks["video"] = videoCustomTrack

        val audioCustomTrack = VideoSDK.createAudioTrack("high_quality", context)
        customTracks["mic"] = audioCustomTrack

        meeting = VideoSDK.initMeeting(
            baseContext, meetingInfo?.meetingId, meetingInfo?.localParticipantName,
            micEnabled, webcamEnabled, null, customTracks
        )
        activity?.window?.decorView?.rootView?.let { HelperClass.showProgress(it) }
        meeting?.join()
    }

    private fun setActionListeners() {
        // Toggle mic
        binding.micLayout.setOnClickListener { toggleMic() }
        binding.btnMic.setOnClickListener { toggleMic() }

        // Toggle webcam
        binding.btnWebcam.setOnClickListener { toggleWebCam() }

        // Leave meeting
        binding.btnLeave.setOnClickListener { showLeaveOrEndDialog() }
        binding.btnMore.setOnClickListener { showMoreOptionsDialog() }
        binding.btnSwitchCameraMode.setOnClickListener { meeting?.changeWebcam() }

        // Chat
        binding.btnChat.setOnClickListener {
            if (meeting != null) {
                openChat()
            }
        }
    }

    private fun showMoreOptionsDialog() {
        val participantSize = meeting!!.participants.size + 1
        val moreOptionsArrayList: ArrayList<MeetingMenuItem> = ArrayList<MeetingMenuItem>()
        val raised_hand = AppCompatResources.getDrawable(baseContext, R.drawable.raise_hand)?.let {
            MeetingMenuItem(
                "Raise Hand",
                it
            )
        }

        val start_screen_share =
            AppCompatResources.getDrawable(baseContext, R.drawable.ic_screen_share)?.let {
                MeetingMenuItem(
                    "Share screen",
                    it
                )
            }

        val stop_screen_share =
            AppCompatResources.getDrawable(baseContext, R.drawable.ic_screen_share)?.let {
                MeetingMenuItem(
                    "Stop screen share",
                    it
                )
            }

        val start_recording =
            AppCompatResources.getDrawable(baseContext, R.drawable.ic_recording)?.let {
                MeetingMenuItem(
                    "Start recording",
                    it
                )
            }
        val stop_recording =
            AppCompatResources.getDrawable(baseContext, R.drawable.ic_recording)?.let {
                MeetingMenuItem(
                    "Stop recording",
                    it
                )
            }
        val participant_list =
            AppCompatResources.getDrawable(baseContext, R.drawable.ic_people)?.let {
                MeetingMenuItem(
                    "Participants ($participantSize)",
                    it
                )
            }
        raised_hand?.let { moreOptionsArrayList.add(it) }
        if (localScreenShare) {
            stop_screen_share?.let { moreOptionsArrayList.add(it) }
        } else {
            start_screen_share?.let { moreOptionsArrayList.add(it) }
        }
        if (recording) {
            stop_recording?.let { moreOptionsArrayList.add(it) }
        } else {
            start_recording?.let { moreOptionsArrayList.add(it) }
        }
        participant_list?.let { moreOptionsArrayList.add(it) }
        val arrayAdapter: ArrayAdapter<*> = MoreOptionsListAdapter(
            baseContext,
            R.layout.more_options_list_layout,
            moreOptionsArrayList
        )
        val materialAlertDialogBuilder =
            MaterialAlertDialogBuilder(baseContext, R.style.AlertDialogCustom)
                .setAdapter(
                    arrayAdapter
                ) { _: DialogInterface?, which: Int ->
                    when (which) {
                        0 -> {
                            raisedHand()
                        }
                        1 -> {
                            toggleScreenSharing()
                        }
                        2 -> {
                            toggleRecording()
                        }
                        3 -> {
                            openParticipantList()
                        }
                    }
                }
        val alertDialog = materialAlertDialogBuilder.create()
        val listView = alertDialog.listView
        listView.divider =
            context?.let { ContextCompat.getColor(it, R.color.md_grey_200) }
                ?.let { ColorDrawable(it) } // set color
        listView.setFooterDividersEnabled(false)
        listView.addFooterView(View(context))
        listView.dividerHeight = 2
        val wmlp = alertDialog.window!!.attributes
        wmlp.gravity = Gravity.BOTTOM or Gravity.END
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(alertDialog.window!!.attributes)
        layoutParams.width = (getWindowWidth() * 0.8).roundToInt().toInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        alertDialog.window!!.attributes = layoutParams
        alertDialog.show()
    }

    private fun toggleRecording() {
        if (!recording) {
            recordingStatusSnackbar!!.show()
            meeting!!.startRecording(null)
        } else {
            meeting!!.stopRecording()
        }
    }

    private fun openParticipantList() {
    }

    private fun raisedHand() {
        meeting!!.pubSub.publish("RAISE_HAND", "Raise Hand by Me", PubSubPublishOptions())
    }

    private fun toggleScreenSharing() {
        if (!screenshareEnabled) {
            if (!localScreenShare) {
                askPermissionForScreenShare()
            }
            localScreenShare = !localScreenShare
        } else {
            if (localScreenShare) {
                meeting!!.disableScreenShare()
            } else {
                Toast.makeText(context, "You can't share your screen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun askPermissionForScreenShare() {
        val mediaProjectionManager = context?.applicationContext?.getSystemService(
            AppCompatActivity.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE
        )
    }

    private fun getAllParticipants(): ArrayList<Participant> {
        val participantList: ArrayList<Participant> = ArrayList<Participant>()
        val participants: Iterator<Participant> = meeting!!.participants.values.iterator()
        for (i in 0 until meeting!!.participants.size) {
            val participant = participants.next()
            participantList.add(participant)
        }
        return participantList
    }

    private fun getWindowWidth(): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun showLeaveOrEndDialog() {
        val optionsArrayList: ArrayList<MeetingMenuItem> = ArrayList<MeetingMenuItem>()
        val leaveMeeting =
            context?.let { context ->
                AppCompatResources.getDrawable(context, R.drawable.ic_leave)?.let { drawable ->
                    MeetingMenuItem(
                        "Leave",
                        "Only you will leave the call",
                        drawable
                    )
                }
            }
        val endMeeting =
            context?.let {
                AppCompatResources.getDrawable(it, R.drawable.ic_end_meeting)?.let {
                    MeetingMenuItem(
                        "End",
                        "End call for all the participants",
                        it
                    )
                }
            }

        optionsArrayList.add(leaveMeeting!!)
        optionsArrayList.add(endMeeting!!)
        val arrayAdapter: ArrayAdapter<*> = LeaveOptionListAdapter(
            baseContext,
            R.layout.leave_options_list_layout,
            optionsArrayList
        )
        val materialAlertDialogBuilder =
            MaterialAlertDialogBuilder(baseContext, R.style.AlertDialogCustom)
                .setAdapter(
                    arrayAdapter
                ) { _: DialogInterface?, which: Int ->
                    when (which) {
//                        0 -> {
//                            viewPager2!!.adapter = null
//                            ParticipantState.destroy()
//                            unSubscribeTopics()
//                            meeting!!.leave()
//                        }
//                        1 -> {
//                            viewPager2!!.adapter = null
//                            ParticipantState.destroy()
//                            unSubscribeTopics()
//                            meeting!!.end()
//                        }
                    }
                }
        val alertDialog = materialAlertDialogBuilder.create()
        val listView = alertDialog.listView
        listView.divider =
            ColorDrawable(ContextCompat.getColor(baseContext, R.color.md_grey_200)) // set color
        listView.setFooterDividersEnabled(false)
        listView.addFooterView(View(baseContext))
        listView.dividerHeight = 2
        val wmlp = alertDialog.window!!.attributes
        wmlp.gravity = Gravity.BOTTOM or Gravity.END
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(alertDialog.window!!.attributes)
        layoutParams.width = (getWindowWidth() * 0.8).roundToInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        alertDialog.window!!.attributes = layoutParams
        alertDialog.show()
    }

    fun openChat() {
//        val messageRcv: RecyclerView
//        val close: ImageView
//        bottomSheetDialog = BottomSheetDialog(this)
//        val v3 = LayoutInflater.from(baseContext)
//            .inflate(
//               R.layout.activity_chat,
//                findViewById(live.videosdk.rtc.android.R.id.layout_chat)
//            )
//        bottomSheetDialog!!.setContentView(v3)
//        messageRcv = v3.findViewById(live.videosdk.rtc.android.R.id.messageRcv)
//        messageRcv.layoutManager = LinearLayoutManager(applicationContext)
//        val lp = RelativeLayout.LayoutParams(
//            RelativeLayout.LayoutParams.MATCH_PARENT,
//            getWindowHeight() / 2
//        )
//        messageRcv.layoutParams = lp
//        val mBottomSheetCallback: BottomSheetBehavior.BottomSheetCallback =
//            object : BottomSheetBehavior.BottomSheetCallback() {
//                override fun onStateChanged(
//                    bottomSheet: View,
//                    @BottomSheetBehavior.State newState: Int
//                ) {
//                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
//                        val lp = RelativeLayout.LayoutParams(
//                            RelativeLayout.LayoutParams.MATCH_PARENT,
//                            getWindowHeight() / 2
//                        )
//                        messageRcv.layoutParams = lp
//                    } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
//                        val lp = RelativeLayout.LayoutParams(
//                            RelativeLayout.LayoutParams.MATCH_PARENT,
//                            RelativeLayout.LayoutParams.MATCH_PARENT
//                        )
//                        messageRcv.layoutParams = lp
//                    }
//                }
//
//                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
//            }
//        bottomSheetDialog!!.behavior.addBottomSheetCallback(mBottomSheetCallback)
//        etmessage = v3.findViewById(live.videosdk.rtc.android.R.id.etMessage)
//        etmessage!!.setOnTouchListener { view, event ->
//            if (view.id == live.videosdk.rtc.android.R.id.etMessage) {
//                view.parent.requestDisallowInterceptTouchEvent(true)
//                when (event.action and MotionEvent.ACTION_MASK) {
//                    MotionEvent.ACTION_UP -> view.parent.requestDisallowInterceptTouchEvent(false)
//                }
//            }
//            false
//        }
//        val btnSend = v3.findViewById<ImageButton>(live.videosdk.rtc.android.R.id.btnSend)
//        btnSend.isEnabled = false
//        etmessage!!.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
//            if (hasFocus) {
//                etmessage!!.hint = ""
//            }
//        }
//        etmessage!!.isVerticalScrollBarEnabled = true
//        etmessage!!.isScrollbarFadingEnabled = false
//        etmessage!!.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
//            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
//                if (etmessage!!.text.toString().trim { it <= ' ' }.isNotEmpty()) {
//                    btnSend.isEnabled = true
//                    btnSend.isSelected = true
//                } else {
//                    btnSend.isEnabled = false
//                    btnSend.isSelected = false
//                }
//            }
//
//            override fun afterTextChanged(editable: Editable) {}
//        })
//
//        //
//        pubSubMessageListener = PubSubMessageListener { message ->
//            messageAdapter!!.addItem(message)
//            messageRcv.scrollToPosition(messageAdapter!!.itemCount - 1)
//        }
//
//        // Subscribe for 'CHAT' topic
//        val pubSubMessageList = meeting!!.pubSub.subscribe("CHAT", pubSubMessageListener)
//
//        //
//        messageAdapter =
//            MessageAdapter(this, pubSubMessageList, meeting!!)
//        messageRcv.adapter = messageAdapter
//        messageRcv.addOnLayoutChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
//            messageRcv.scrollToPosition(
//                messageAdapter!!.itemCount - 1
//            )
//        }
//        v3.findViewById<View>(live.videosdk.rtc.android.R.id.btnSend).setOnClickListener {
//            val message: String = etmessage!!.text.toString()
//            if (message != "") {
//                val publishOptions = PubSubPublishOptions()
//                publishOptions.isPersist = true
//                meeting!!.pubSub.publish("CHAT", message, publishOptions)
//                etmessage!!.setText("")
//            } else {
//                Toast.makeText(
//                    this@GroupCallActivity, "Please Enter Message",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//        close = v3.findViewById(live.videosdk.rtc.android.R.id.ic_close)
//        bottomSheetDialog!!.show()
//        close.setOnClickListener { bottomSheetDialog!!.dismiss() }
//        bottomSheetDialog!!.setOnDismissListener {
//            meeting!!.pubSub.unsubscribe(
//                "CHAT",
//                pubSubMessageListener
//            )
//        }
    }

    private fun toggleMic() {
        if (micEnabled) {
            meeting!!.muteMic()
        } else {
            val audioCustomTrack = VideoSDK.createAudioTrack("high_quality", context)
            meeting!!.unmuteMic(audioCustomTrack)
        }
    }

    private fun toggleWebCam() {
        if (webcamEnabled) {
            meeting!!.disableWebcam()
        } else {
            val videoCustomTrack = VideoSDK.createCameraVideoTrack(
                "h720p_w960p",
                "front",
                CustomStreamTrack.VideoMode.DETAIL,
                baseContext
            )
            meeting!!.enableWebcam(videoCustomTrack)
        }
    }

    private fun showAudioInputDialog() {
        val mics = meeting!!.mics
        var audioDeviceListItem: MeetingMenuItem?
        val audioDeviceList: ArrayList<MeetingMenuItem?> = ArrayList<MeetingMenuItem?>()
        // Prepare list
        var item: String
        for (i in mics.indices) {
            item = mics.toTypedArray()[i].toString()
            var mic =
                item.substring(0, 1).uppercase(Locale.getDefault()) + item.substring(1).lowercase(
                    Locale.getDefault()
                )
            mic = mic.replace("_", " ")
            audioDeviceListItem = MeetingMenuItem(mic, null, (item == selectedAudioDeviceName))
            audioDeviceList.add(audioDeviceListItem)
        }
        val arrayAdapter: ArrayAdapter<*> = AudioDeviceListAdapter(
            baseContext,
            R.layout.audio_device_list_layout,
            audioDeviceList
        )
        val materialAlertDialogBuilder =
            MaterialAlertDialogBuilder(
                baseContext,
                R.style.AlertDialogCustom
            )
                .setAdapter(arrayAdapter) { _: DialogInterface?, which: Int ->
                    var audioDevice: AppRTCAudioManager.AudioDevice? = null
                    when (audioDeviceList[which]!!.itemName) {
                        "Bluetooth" -> audioDevice = AppRTCAudioManager.AudioDevice.BLUETOOTH
                        "Wired headset" -> audioDevice =
                            AppRTCAudioManager.AudioDevice.WIRED_HEADSET
                        "Speaker phone" -> audioDevice =
                            AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
                        "Earpiece" -> audioDevice = AppRTCAudioManager.AudioDevice.EARPIECE
                    }
                    meeting!!.changeMic(
                        audioDevice,
                        VideoSDK.createAudioTrack("high_quality", baseContext)
                    )
                }
        val alertDialog = materialAlertDialogBuilder.create()
        val listView = alertDialog.listView
        listView.divider =
            ColorDrawable(
                ContextCompat.getColor(
                    baseContext,
                    R.color.md_grey_200
                )
            ) // set color
        listView.setFooterDividersEnabled(false)
        listView.addFooterView(View(baseContext))
        listView.dividerHeight = 2
        val wmlp = alertDialog.window!!.attributes
        wmlp.gravity = Gravity.BOTTOM or Gravity.END
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(alertDialog.window!!.attributes)
        layoutParams.width = (getWindowWidth() * 0.6).roundToInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        alertDialog.window!!.attributes = layoutParams
        alertDialog.show()
    }

    private fun setAudioDeviceListeners() {
        meeting?.setAudioDeviceChangeListener { selectedAudioDevice, availableAudioDevices ->
            selectedAudioDeviceName = selectedAudioDevice.toString()
        }
    }

    private val meetingEventListener: MeetingEventListener = object : MeetingEventListener() {
        override fun onMeetingJoined() {
            if (meeting != null) {
                //hide progress when meetingJoined
                activity?.window?.decorView?.rootView?.let { HelperClass.hideProgress(it) }
                toggleMicIcon()
                toggleWebcamIcon()
                setLocalListeners()
                meetingInfo?.meetingId?.let { viewModel.fetchMeetingTime(it) }
            }
//                NetworkUtils(this@GroupCallActivity).fetchMeetingTime(
//                    meeting!!.meetingId,
//                    token,
//                    object : ResponseListener<Int> {
//                        override fun onResponse(meetingTime: Int?) {
//                            meetingSeconds = meetingTime!!
//                            showMeetingTime()
//                        }
//                    })
//                viewPager2!!.offscreenPageLimit = 1
//                viewPager2!!.adapter = viewAdapter
//                raiseHandListener =
//                    PubSubMessageListener { pubSubMessage ->
//                        val parentLayout = findViewById<View>(android.R.id.content)
//                        var snackbar: Snackbar
//                        if ((pubSubMessage.senderId == meeting!!.localParticipant.id)) {
//                            snackbar = Snackbar.make(
//                                parentLayout,
//                                "You raised hand",
//                                Snackbar.LENGTH_SHORT
//                            )
//                        } else {
//                            snackbar = Snackbar.make(
//                                parentLayout,
//                                pubSubMessage.senderName + " raised hand  ",
//                                Snackbar.LENGTH_LONG
//                            )
//                        }
//
//                        val snackbarLayout = snackbar.view
//                        val snackbarTextId = com.google.android.material.R.id.snackbar_text
//                        val textView = snackbarLayout.findViewById<View>(snackbarTextId) as TextView
//
//                        val drawable =
//                            resources.getDrawable(live.videosdk.rtc.android.R.drawable.ic_raise_hand)
//                        drawable.setBounds(0, 0, 50, 65)
//                        textView.setCompoundDrawablesRelative(drawable, null, null, null)
//                        textView.compoundDrawablePadding = 15
//                        HelperClass.setSnackBarStyle(snackbar.view, 0)
//                        snackbar.isGestureInsetBottomIgnored = true
//                        snackbar.view.setOnClickListener { snackbar.dismiss() }
//                        snackbar.show()
//                    }
//
//                // notify user for raise hand
//                meeting!!.pubSub.subscribe("RAISE_HAND", raiseHandListener)
//                chatListener = PubSubMessageListener { pubSubMessage ->
//                    if (pubSubMessage.senderId != meeting!!.localParticipant.id) {
//                        val parentLayout = findViewById<View>(android.R.id.content)
//                        val snackbar = Snackbar.make(
//                            parentLayout, (pubSubMessage.senderName + " says: " +
//                                    pubSubMessage.message), Snackbar.LENGTH_SHORT
//                        )
//                            .setDuration(2000)
//                        val snackbarView = snackbar.view
//                        HelperClass.setSnackBarStyle(snackbarView, 0)
//                        snackbar.view.setOnClickListener { snackbar.dismiss() }
//                        snackbar.show()
//                    }
//                }
//                // notify user of any new messages
//                meeting!!.pubSub.subscribe("CHAT", chatListener)
//
//
//            }
        }

        override fun onMeetingLeft() {
        }

        override fun onPresenterChanged(participantId: String?) {
            updatePresenter(participantId)
        }

        override fun onRecordingStarted() {
            recording = true
            recordingStatusSnackbar!!.dismiss()
            binding.recordingLottie.visibility =
                View.VISIBLE
            Toast.makeText(
                context, "Recording started",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onRecordingStopped() {
            recording = false
            binding.recordingLottie.visibility =
                View.GONE
            Toast.makeText(
                context, "Recording stopped",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onExternalCallStarted() {
            Toast.makeText(context, "onExternalCallStarted", Toast.LENGTH_SHORT)
                .show()
        }

        override fun onError(error: JSONObject) {
            try {
                val errorCodes = VideoSDK.getErrorCodes()
                val code = error.getInt("code")
                if (code == errorCodes.getInt("PREV_RECORDING_PROCESSING")) {
                    recordingStatusSnackbar!!.dismiss()
                }
                val snackbar = Snackbar.make(
                    binding.mainLayout,
                    error.getString("message"),
                    Snackbar.LENGTH_LONG
                )
                HelperClass.setSnackBarStyle(snackbar.view, 0)
                snackbar.view.setOnClickListener { snackbar.dismiss() }
                snackbar.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onSpeakerChanged(participantId: String?) {}
        override fun onMeetingStateChanged(state: String) {
//            if (state === "FAILED") {
//                val parentLayout = findViewById<View>(android.R.id.content)
//                val builderTextLeft = SpannableStringBuilder()
//                builderTextLeft.append("   Call disconnected. Reconnecting...")
//                builderTextLeft.setSpan(
//                    context?.let {
//                        ImageSpan(
//                            it,
//                            R.drawable.ic_call_disconnected
//                        )
//                    }, 0, 1, 0
//                )
//                val snackbar = Snackbar.make(binding.conte, builderTextLeft, Snackbar.LENGTH_LONG)
//                HelperClass.setSnackBarStyle(
//                    snackbar.view,
//                    resources.getColor(R.color.md_red_400)
//                )
//                snackbar.view.setOnClickListener { snackbar.dismiss() }
//                snackbar.show()
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    if (handler.hasCallbacks((runnable)!!)) handler.removeCallbacks((runnable)!!)
//                }
//            }
        }

        override fun onMicRequested(participantId: String, listener: MicRequestListener) {
            showMicRequestDialog(listener)
        }

        override fun onWebcamRequested(participantId: String, listener: WebcamRequestListener) {
            showWebcamRequestDialog(listener)
        }
    }

    private fun updatePresenter(participantId: String?) {
        if (participantId == null) {
            binding.shareView.visibility = View.GONE
            binding.shareLayout.visibility = View.GONE
            screenshareEnabled = false
            return
        } else {
            screenshareEnabled = true
        }

        // find participant
        val participant = meeting!!.participants[participantId] ?: return

        // find share stream in participant
        var shareStream: Stream? = null
        for (stream: Stream in participant.streams.values) {
            if ((stream.kind == "share")) {
                shareStream = stream
                break
            }
        }
        if (shareStream == null) return
        binding.tvScreenShareParticipantName.text =
            participant.displayName + " is presenting"
        binding.tvScreenShareParticipantName.visibility =
            View.VISIBLE
        binding.ivParticipantScreenShareNetwork.visibility =
            View.VISIBLE

        // display share video
        binding.shareLayout.visibility = View.VISIBLE
        binding.shareView.visibility = View.VISIBLE
        binding.shareView.setZOrderMediaOverlay(true)
        binding.shareView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        val videoTrack = shareStream.track as VideoTrack
        binding.shareView.addTrack(videoTrack)
        screenShareParticipantNameSnackbar = Snackbar.make(
            binding.mainLayout, participant.displayName + " started presenting",
            Snackbar.LENGTH_SHORT
        )
        HelperClass.setSnackBarStyle(screenShareParticipantNameSnackbar!!.view, 0)
        screenShareParticipantNameSnackbar!!.isGestureInsetBottomIgnored = true
        screenShareParticipantNameSnackbar!!.view.setOnClickListener { screenShareParticipantNameSnackbar!!.dismiss() }
        screenShareParticipantNameSnackbar!!.show()


        // listen for share stop event
        participant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamDisabled(stream: Stream) {
                if ((stream.kind == "share")) {
                    val track: VideoTrack = stream.track as VideoTrack
                    binding.shareView.removeTrack()
                    binding.shareView.visibility = View.GONE
                    View.GONE.also { binding.shareLayout!!.visibility = it }
                    binding.tvScreenShareParticipantName.visibility =
                        View.GONE
                    binding.ivParticipantScreenShareNetwork.visibility =
                        View.GONE

                    localScreenShare = false
                }
            }
        })
    }

    private fun setLocalListeners() {
        meeting!!.localParticipant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamEnabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    webcamEnabled = true
                    toggleWebcamIcon()
                } else if (stream.kind.equals("audio", ignoreCase = true)) {
                    micEnabled = true
                    toggleMicIcon()
                } else if (stream.kind.equals("share", ignoreCase = true)) {
                    binding.localScreenShareView.visibility =
                        View.VISIBLE
                    screenShareParticipantNameSnackbar = Snackbar.make(
                        binding.mainLayout,
                        "You started presenting",
                        Snackbar.LENGTH_SHORT
                    )
                    screenShareParticipantNameSnackbar?.view?.let {
                        HelperClass.setSnackBarStyle(
                            it,
                            0
                        )
                    }
                    screenShareParticipantNameSnackbar?.isGestureInsetBottomIgnored = true
                    screenShareParticipantNameSnackbar?.view?.setOnClickListener { screenShareParticipantNameSnackbar!!.dismiss() }
                    screenShareParticipantNameSnackbar?.show()
                    localScreenShare = true
                    screenshareEnabled = true
                }
            }

            override fun onStreamDisabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    webcamEnabled = false
                    toggleWebcamIcon()
                } else if (stream.kind.equals("audio", ignoreCase = true)) {
                    micEnabled = false
                    toggleMicIcon()
                } else if (stream.kind.equals("share", ignoreCase = true)) {
                    binding.localScreenShareView.visibility =
                        View.GONE
                    localScreenShare = false
                    screenshareEnabled = false
                }
            }
        })
    }

    private fun toggleMicIcon() {
        if (micEnabled) {
            binding.btnMic.setImageResource(R.drawable.ic_mic_on)
            binding.btnAudioSelection.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24)
            binding.micLayout.background =
                context?.let { ContextCompat.getDrawable(it, R.drawable.layout_selected) }
        } else {
            binding.btnMic.setImageResource(R.drawable.ic_mic_off_24)
            binding.btnAudioSelection.setImageResource(R.drawable.ic_baseline_arrow_drop_down)
            binding.micLayout.setBackgroundColor(Color.WHITE)
            binding.micLayout.background =
                context?.let { ContextCompat.getDrawable(it, R.drawable.layout_nonselected) }
        }
    }

    private fun showMicRequestDialog(listener: MicRequestListener) {
        val alertDialog =
            MaterialAlertDialogBuilder(
                baseContext,
                R.style.AlertDialogCustom
            ).create()
        alertDialog.setCanceledOnTouchOutside(false)
        val inflater = this.layoutInflater
        val dialogView =
            inflater.inflate(R.layout.alert_dialog_layout, null)
        alertDialog.setView(dialogView)
        val title = dialogView.findViewById<View>(live.videosdk.rtc.android.R.id.title) as TextView
        title.visibility = View.GONE
        val message =
            dialogView.findViewById<View>(live.videosdk.rtc.android.R.id.message) as TextView
        message.text = "Host is asking you to unmute your mic, do you want to allow ?"
        val positiveButton =
            dialogView.findViewById<Button>(R.id.positiveBtn)
        positiveButton.text = "Yes"
        positiveButton.setOnClickListener {
            listener.accept()
            alertDialog.dismiss()
        }
        val negativeButton =
            dialogView.findViewById<Button>(R.id.negativeBtn)
        negativeButton.text = "No"
        negativeButton.setOnClickListener {
            listener.reject()
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    private fun showWebcamRequestDialog(listener: WebcamRequestListener) {
        val alertDialog =
            context?.let {
                MaterialAlertDialogBuilder(
                    it,
                    R.style.AlertDialogCustom
                ).create()
            }
        alertDialog?.setCanceledOnTouchOutside(false)
        val inflater = this.layoutInflater
        val dialogView =
            inflater.inflate(R.layout.alert_dialog_layout, null)
        alertDialog?.setView(dialogView)
        val title = dialogView.findViewById<View>(live.videosdk.rtc.android.R.id.title) as TextView
        title.visibility = View.GONE
        val message =
            dialogView.findViewById<View>(live.videosdk.rtc.android.R.id.message) as TextView
        message.text = "Host is asking you to enable your webcam, do you want to allow ?"
        val positiveButton =
            dialogView.findViewById<Button>(R.id.positiveBtn)
        positiveButton.text = "Yes"
        positiveButton.setOnClickListener {
            listener.accept()
            alertDialog?.dismiss()
        }
        val negativeButton =
            dialogView.findViewById<Button>(R.id.negativeBtn)
        negativeButton.text = "No"
        negativeButton.setOnClickListener {
            listener.reject()
            alertDialog?.dismiss()
        }
        alertDialog?.show()
    }

    private fun toggleWebcamIcon() {
        if (webcamEnabled) {
            binding.btnWebcam.setImageResource(R.drawable.ic_video_camera)
            binding.btnWebcam.setColorFilter(Color.WHITE)
            var buttonDrawable = binding.btnWebcam.background
            buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
            //the color is a direct color int and not a color resource
            DrawableCompat.setTint(buttonDrawable, Color.TRANSPARENT)
            binding.btnWebcam.background = buttonDrawable
        } else {
            binding.btnWebcam.setImageResource(R.drawable.ic_video_camera_off)
            binding.btnWebcam.setColorFilter(Color.BLACK)
            var buttonDrawable = binding.btnWebcam.background
            buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
            //the color is a direct color int and not a color resource
            DrawableCompat.setTint(buttonDrawable, Color.WHITE)
            binding.btnWebcam.background = buttonDrawable
        }
    }

    override fun provideLayoutId() = R.layout.fragment_meeting_call

    override fun onRequestError(errorResponse: ErrorResult) {
    }

    override fun provideViewModelClass() = MeetingViewModel::class.java

    override fun isNeedHideBottomBar() = true

}