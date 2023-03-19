package com.example.fpt.ui.metting

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse
import com.demo.domain.domain.usecase.meeting_interface.MeetingUseCase
import com.example.fpt.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MeetingViewModel @Inject constructor (
    private val useCase: MeetingUseCase,
) : BaseViewModel() {

    private val roomResponse: MutableLiveData<RoomResponse> =
        MutableLiveData()

    private val meetingTimeResponse: MutableLiveData<SessionResponse> =
        MutableLiveData()

    fun joinMeetingRoom(roomID: String) = coroutineScope.launch {
        val response = useCase.joinMeeting(roomID)
        roomResponse.postValue(response)
    }

    fun fetchMeetingTime(roomID: String) = coroutineScope.launch {
        val response = useCase.fetchMeetingTime(roomID)
        meetingTimeResponse.postValue(response)
    }

    fun getJoinRoomResponse() = roomResponse
    fun getMeetingTime() = meetingTimeResponse

}