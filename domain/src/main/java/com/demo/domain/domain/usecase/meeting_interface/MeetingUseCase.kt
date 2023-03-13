package com.demo.domain.domain.usecase.meeting_interface

import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse


interface MeetingUseCase {
    suspend fun joinMeeting(meetingID: String): RoomResponse
    suspend fun fetchMeetingTime(meetingID: String): SessionResponse
}