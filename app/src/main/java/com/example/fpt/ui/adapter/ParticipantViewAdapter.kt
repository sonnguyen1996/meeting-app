package com.example.fpt.ui.adapter

import android.text.TextUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fpt.ui.metting.ParticipantViewFragment
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.listeners.MeetingEventListener

class ParticipantViewAdapter(
    fragmentManager: FragmentManager,
    val lifecycle: Lifecycle,
    var meeting: Meeting
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    var participantListSize = 4

    init {
        meeting.addEventListener(object : MeetingEventListener() {
            override fun onPresenterChanged(participantId: String?) {
                super.onPresenterChanged(participantId)
                participantListSize = if (!TextUtils.isEmpty(participantId)) {
                    2
                } else {
                    4
                }
                notifyDataSetChanged()
            }
        })
    }

    override fun createFragment(position: Int): Fragment {
        return ParticipantViewFragment(meeting, position)
    }

    override fun getItemCount(): Int {
        return meeting.participants.size / participantListSize + 1
    }
}
