package com.demo.domain.domain.repository

import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse

interface IMeetingRepository {
    fun jointMeeting(id: String): RoomResponse
    fun fetchMeetingTime(id: String): SessionResponse
}