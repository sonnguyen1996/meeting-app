package com.example.fpt.ui.metting.listener

import live.videosdk.rtc.android.Participant

interface ParticipantChangeListener {
    fun onChangeParticipant(participantList: List<List<Participant>>)
    fun onPresenterChanged(screenShare: Boolean)
    fun onSpeakerChanged(participantList: List<List<Participant>>?, activeSpeaker: Participant?)
}
