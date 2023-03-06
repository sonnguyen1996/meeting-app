package com.example.fpt.ui.metting

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse
import com.example.fpt.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import mobiletv.repository.remote.MeetingRepository
import java.util.LinkedHashMap


class MeetingViewModel(application: Application) : BaseViewModel(application){

    private val roomResponse: MutableLiveData<RoomResponse> =
        MutableLiveData()


    private val meetingTimeResponse: MutableLiveData<SessionResponse> =
        MutableLiveData()

    private val meetingRepository = MeetingRepository()

    fun joinMeetingRoom(roomID : String) = coroutineScope.launch {
        val response = meetingRepository.joinMeeting(roomID)
            ?: return@launch apiFailResponse.postValue(true)
        roomResponse.postValue(response)
    }

    fun fetchMeetingTime(roomID : String) = coroutineScope.launch {
        val response = meetingRepository.joinMeeting(roomID)
            ?: return@launch apiFailResponse.postValue(true)
        meetingTimeResponse.postValue(response)
    }

    fun getJoinRoomResponse() = roomResponse
    fun getMeetingTime() = meetingTimeResponse

}