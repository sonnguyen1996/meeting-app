package com.example.fpt.ui.metting

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnTouchListener
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Transformations.map
import androidx.viewpager2.widget.ViewPager2
import com.demo.domain.domain.entities.ErrorResult
import com.example.demothesisfpteduvn.R
import com.example.demothesisfpteduvn.databinding.FragmentParticipantViewBinding
import com.example.fpt.ui.base.BaseFragment
import com.example.fpt.ui.metting.listener.ParticipantChangeListener
import com.example.fpt.ui.metting.listener.ParticipantStreamChangeListener
import com.example.fpt.ui.metting.ultils.HelperClass
import com.example.fpt.ui.metting.ultils.ParticipantState
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.Stream
import live.videosdk.rtc.android.VideoView
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import org.webrtc.VideoTrack
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min


@AndroidEntryPoint
class ParticipantViewFragment(
    var meeting: Meeting?,
    var position: Int,
    var listener: OnTouchListener?
) : BaseFragment<MeetingViewModel, FragmentParticipantViewBinding>() {

    private var participantChangeListener: ParticipantChangeListener? = null

    private var participantState: ParticipantState? = null

    private var participants: List<Participant>? = null

    private var participantListArr: List<List<Participant>>? = null

    private var tabLayoutMediator: TabLayoutMediator? = null

    private var viewPager2: ViewPager2? = null

    private var tabLayout: TabLayout? = null

    private var popupwindow_obj: PopupWindow? = null

    private var participantsInGrid: MutableMap<String, Participant>? = null

    private val participantsView: MutableMap<String, View> = HashMap()

    private val captureViewModel: CapturingViewModel by activityViewModels()

    private var localVideo: VideoView? = null

    var participantStreamChangeListener: ParticipantStreamChangeListener? = null

    private fun changeLayout(
        participantList: List<List<Participant>>,
        activeSpeaker: Participant?
    ) {
        participantListArr = participantList
        if (position < participantList.size) {
            participants = participantList[position]
            if (popupwindow_obj != null && popupwindow_obj!!.isShowing) popupwindow_obj?.dismiss()
            showInGUI(activeSpeaker)
            tabLayoutMediator = TabLayoutMediator(
                tabLayout!!, viewPager2!!, true
            ) { _: TabLayout.Tab?, _: Int ->
                Log.d(
                    "TAG",
                    "onCreate: "
                )
            }
            if (tabLayoutMediator?.isAttached == true) {
                tabLayoutMediator?.detach()
            }
            tabLayoutMediator?.attach()
            if (participantList.size == 1) {
                tabLayout?.visibility = View.GONE
            } else {
                tabLayout?.visibility = View.VISIBLE
            }
        }
    }


    override fun onResume() {
        if (position < participantListArr!!.size) {
            val currentParticipants = participantListArr!![position]
            for (i in currentParticipants.indices) {
                val participant = currentParticipants[i]
                if (!participant.isLocal) {
                    for ((_, stream) in participant.streams) {
                        if (stream.kind.equals("video", ignoreCase = true)) stream.resume()
                    }
                }
            }
        }
        super.onResume()
    }

    override fun isNeedHideBottomBar() = true

    override fun provideLayoutId() = R.layout.fragment_participant_view
    override fun onRequestError(errorResponse: ErrorResult) {
    }

    override fun provideViewModelClass() = MeetingViewModel::class.java

    override fun initActions() {
        participantState = meeting?.let { ParticipantState.getInstance(it) }
        participantState?.addParticipantChangeListener(participantChangeListener as ParticipantChangeListener)
    }

    private fun initMeetingEvent() {
        participantChangeListener = object : ParticipantChangeListener {
            override fun onChangeParticipant(participantList: List<List<Participant>>) {
                changeLayout(participantList, null)
            }

            override fun onPresenterChanged(screenShare: Boolean) {
                showInGUI(null)
                updateGridLayout(screenShare)
            }

            override fun onSpeakerChanged(
                participantList: List<List<Participant>>?,
                activeSpeaker: Participant?
            ) {
                participantList?.let { changeLayout(it, activeSpeaker!!) } ?: activeSpeakerLayout(
                    activeSpeaker
                )
            }
        }
    }


    override fun initData() {
        captureViewModel.executeCaptureImage.observe(
            viewLifecycleOwner
        ) { _ ->
            collectData()
            localVideo?.addFrameListener({
                runBlocking (Dispatchers.Main) {
                    captureViewModel.processImage(it)
                }
            }, 1f)
        }
    }

    private fun collectData() {
    }

    override fun initView() {
        viewPager2 = requireActivity().findViewById(R.id.view_pager_video_grid)
        tabLayout = requireActivity().findViewById(R.id.tab_layout_dots)
        initMeetingEvent()
        binding.participantGridLayout.setOnTouchListener(listener)
    }

    override fun onPause() {
        if (position < participantListArr!!.size) {
            var otherParticipants: List<Participant> = ArrayList()
            for (i in participantListArr!!.indices) {
                if (position == i) {
                    continue
                }
                otherParticipants = participantListArr!![i]
            }
            for (i in otherParticipants.indices) {
                val participant = otherParticipants[i]
                if (!participant.isLocal) {
                    for ((_, stream) in participant.streams) {
                        if (stream.kind.equals("video", ignoreCase = true)) {
                            stream.pause()
                        }
                    }
                }
            }
        }
        super.onPause()
    }

    // Call where View ready.
    @SuppressLint("MissingInflatedId")
    private fun showInGUI(activeSpeaker: Participant?) {
        for (i in participants!!.indices) {
            val participant = participants!![i]
            if (participantsInGrid != null) {
                for ((_, key) in participantsInGrid!!) {
                    if (!participants!!.contains(key)) {
                        participantsInGrid!!.remove(key.id)
                        val participantVideoView =
                            participantsView[key.id]!!.findViewById<VideoView>(R.id.participantVideoView)
                        participantVideoView.releaseSurfaceViewRenderer()
                        binding.participantGridLayout.removeView(participantsView[key.id])
                        participantsView.remove(key.id)
                        updateGridLayout(false)
                    }
                }
            }
            if (participantsInGrid == null || !participantsInGrid!!.containsKey(participant.id)) {
                if (participantsInGrid == null) participantsInGrid = ConcurrentHashMap()
                participantsInGrid!![participant.id] = participant
                val participantView: View = LayoutInflater.from(context)
                    .inflate(R.layout.item_participant, binding.participantGridLayout, false)
                participantsView[participant.id] = participantView
                val participantCard = participantView.findViewById<CardView>(R.id.ParticipantCard)
                val ivMicStatus = participantView.findViewById<ImageView>(R.id.ivMicStatus)
                if (activeSpeaker == null) {
                    participantCard.foreground = null
                } else {
                    if (participant.id == activeSpeaker.id) {
                        participantCard.foreground =
                            AppCompatResources.getDrawable(baseContext, R.drawable.layout_bg)
                    } else {
                        participantCard.foreground = null
                    }
                }
                val ivNetwork = participantView.findViewById<ImageView>(R.id.ivNetwork)
                participantStreamChangeListener = object : ParticipantStreamChangeListener {
                    override fun onStreamChanged() {
                        if (participant.streams.isEmpty()) {
                            ivNetwork.visibility = View.GONE
                        } else {
                            ivNetwork.visibility = View.VISIBLE
                        }
                    }
                }
                ivNetwork.setOnClickListener {
                    popupwindow_obj = HelperClass().callStatsPopupDisplay(
                        participant, ivNetwork,
                        requireContext(), false
                    )
                    popupwindow_obj?.showAsDropDown(ivNetwork, -350, -85)
                }
                val tvName = participantView.findViewById<TextView>(R.id.tvName)
                val txtParticipantName =
                    participantView.findViewById<TextView>(R.id.txtParticipantName)
                val participantVideoView =
                    participantView.findViewById<VideoView>(R.id.participantVideoView)
                if (participant.id == meeting!!.localParticipant.id) {
                    tvName.text = "You"
                } else {
                    tvName.text = participant.displayName
                }
                txtParticipantName.text = participant.displayName.substring(0, 1)
                for ((_, stream) in participant.streams) {
                    if (stream.kind.equals("video", ignoreCase = true)) {
                        participantVideoView.visibility = View.VISIBLE
                        val videoTrack = stream.track as VideoTrack
                        participantVideoView.addTrack(videoTrack)
                        participantStreamChangeListener?.onStreamChanged()
                        break
                    } else if (stream.kind.equals("audio", ignoreCase = true)) {
                        participantStreamChangeListener?.onStreamChanged()
                        ivMicStatus.setImageResource(R.drawable.ic_audio_on)
                    }
                }
                participant.addEventListener(object : ParticipantEventListener() {
                    override fun onStreamEnabled(stream: Stream) {
                        if (stream.kind.equals("video", ignoreCase = true)) {
                            participantVideoView.visibility = View.VISIBLE
                            val videoTrack = stream.track as VideoTrack
                            participantVideoView.addTrack(videoTrack)
                            if (participant.id == meeting!!.localParticipant.id) {
                                localVideo = participantVideoView
                            }
                            participantStreamChangeListener?.onStreamChanged()
                        } else if (stream.kind.equals("audio", ignoreCase = true)) {
                            participantStreamChangeListener?.onStreamChanged()
                            ivMicStatus.setImageResource(R.drawable.ic_audio_on)
                        }
                    }

                    override fun onStreamDisabled(stream: Stream) {
                        if (stream.kind.equals("video", ignoreCase = true)) {
                            val track = stream.track as VideoTrack
                            participantVideoView.removeTrack()
                            participantVideoView.visibility = View.GONE
                        } else if (stream.kind.equals("audio", ignoreCase = true)) {
                            ivMicStatus.setImageResource(R.drawable.ic_audio_off)
                        }
                    }
                })
                binding.participantGridLayout.addView(participantView)
                updateGridLayout(false)
            }
        }
    }

    fun activeSpeakerLayout(activeSpeaker: Participant?) {
        for (j in 0 until binding.participantGridLayout.childCount) {
            val participant = participants!![j]
            val participantView = binding.participantGridLayout.getChildAt(j)
            val participantCard = participantView.findViewById<CardView>(R.id.ParticipantCard)
            if (activeSpeaker == null) {
                participantCard.foreground = null
            } else {
                if (participant.id == activeSpeaker.id) {
                    participantCard.foreground =
                        AppCompatResources.getDrawable(baseContext, R.drawable.layout_bg)
                } else {
                    participantCard.foreground = null
                }
            }
        }
    }

    override fun onDestroy() {
        if (participantChangeListener != null) {
            participantState!!.removeParticipantChangeListener(participantChangeListener!!)
        }
        for (i in 0 until binding.participantGridLayout.childCount) {
            val view = binding.participantGridLayout.getChildAt(i)
            val videoView = view.findViewById<VideoView>(R.id.participantVideoView)
            if (videoView != null) {
                videoView.visibility = View.GONE
                videoView.releaseSurfaceViewRenderer()
            }
        }
        binding.participantGridLayout.removeAllViews()
        participantsInGrid = null
        super.onDestroy()
    }

    fun updateGridLayout(screenShareFlag: Boolean) {
        if (screenShareFlag) {
            var col = 0
            var row = 0
            for (i in 0 until binding.participantGridLayout.childCount) {
                val params =
                    binding.participantGridLayout.getChildAt(i).layoutParams as GridLayout.LayoutParams
                params.columnSpec = GridLayout.spec(col, 1, 1f)
                params.rowSpec = GridLayout.spec(row, 1, 1f)
                if (col + 1 == 2) {
                    col = 0
                    row++
                } else {
                    col++
                }
            }
            binding.participantGridLayout.requestLayout()
        } else {
            var col = 0
            var row = 0
            for (i in 0 until binding.participantGridLayout.childCount) {
                val params =
                    binding.participantGridLayout.getChildAt(i).layoutParams as GridLayout.LayoutParams
                params.columnSpec = GridLayout.spec(col, 1, 1f)
                params.rowSpec = GridLayout.spec(row, 1, 1f)
                if (col + 1 == normalLayoutColumnCount) {
                    col = 0
                    row++
                } else {
                    col++
                }
            }
            binding.participantGridLayout.requestLayout()
        }
    }

    private val normalLayoutRowCount = min(max(1, participantsView.size), 2)
    private val normalLayoutColumnCount: Int
        get() {
            val maxColumns = 2
            val result = max(
                1,
                (participantsView.size + normalLayoutRowCount - 1) / normalLayoutRowCount
            )
            check(result <= maxColumns) { "\${result} videos not allowed." }
            return result
        }

    private fun setQuality(quality: String) {
        val participants: Iterator<Participant> = meeting!!.participants.values.iterator()
        for (i in 0 until meeting!!.participants.size) {
            val participant = participants.next()
            participant.quality = quality
        }
    }
}