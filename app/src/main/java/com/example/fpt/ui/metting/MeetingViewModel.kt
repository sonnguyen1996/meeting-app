package com.example.fpt.ui.metting

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse
import com.example.fpt.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import thesis.repository.remote.MeetingRepository
import java.util.*

@HiltViewModel
class MeetingViewModel(application: Application) : BaseViewModel(application) {

    private val roomResponse: MutableLiveData<RoomResponse> =
        MutableLiveData()

    private var meetingSeconds = 0

    private val meetingTimeResponse: MutableLiveData<SessionResponse> =
        MutableLiveData()


    val executeCaptureImage: MutableLiveData<Unit> =
        MutableLiveData()

    val updateTimeMeeting: MutableLiveData<String> =
        MutableLiveData()


    private val meetingRepository = MeetingRepository()

    fun joinMeetingRoom(roomID: String) = coroutineScope.launch {
        val response = meetingRepository.joinMeeting(roomID)
            ?: return@launch apiFailResponse.postValue(true)
        roomResponse.postValue(response)
    }

    fun fetchMeetingTime(roomID: String) = coroutineScope.launch {
        val response = meetingRepository.fetchMeetingTime(roomID)
            ?: return@launch apiFailResponse.postValue(true)
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
            if (secs % 5 == 0){
//              executeCaptureImage.postValue()
            }
            delay(1000)
        }
    }

    fun getJoinRoomResponse() = roomResponse
    fun getMeetingTime() = meetingTimeResponse

}