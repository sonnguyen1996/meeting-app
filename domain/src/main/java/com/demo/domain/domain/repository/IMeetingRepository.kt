package com.demo.domain.domain.repository

import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse

interface IMeetingRepository {
    suspend fun jointMeeting(id: String): RoomResponse
    suspend fun fetchMeetingTime(id: String): SessionResponse
}