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

    private var meetingSeconds = 0

    private val meetingTimeResponse: MutableLiveData<SessionResponse> =
        MutableLiveData()


    val executeCaptureImage: MutableLiveData<Boolean> =
        MutableLiveData()

    val updateTimeMeeting: MutableLiveData<String> =
        MutableLiveData()

    fun joinMeetingRoom(roomID: String) = coroutineScope.launch {
        val response = useCase.joinMeeting(roomID)
        roomResponse.postValue(response)
    }

    fun fetchMeetingTime(roomID: String) = coroutineScope.launch {
        val response = useCase.fetchMeetingTime(roomID)
        meetingTimeResponse.postValue(response)
    }

    fun startObserver(initialTime: Int) = heavyTaskScope.launch {
        meetingSeconds = initialTime
        while (isActive) {
            val hours = meetingSeconds / 3600
            val minutes = (meetingSeconds % 3600) / 60
            val secs = meetingSeconds % 60

            // Format the seconds into minutes,seconds.
            val time = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d", hours,
                minutes, secs
            )
            updateTimeMeeting.postValue(time)
            if (secs % 5 == 0) {
              executeCaptureImage.postValue(true)
            }
            delay(1000)
        }
    }

    fun getJoinRoomResponse() = roomResponse
    fun getMeetingTime() = meetingTimeResponse

}