package com.demo.domain.domain.usecase.meeting_interface

import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse


interface IMeetingUseCase {
    fun joinMeeting(meetingID: String):  RoomResponse
    fun fetchMeetingTime(meetingID: String): SessionResponse
}