package com.demo.domain.domain.usecase

import com.demo.domain.domain.repository.IMeetingRepository
import com.demo.domain.domain.usecase.meeting_interface.MeetingUseCase
import javax.inject.Inject

class MeetingInteraction @Inject constructor(private val meetingRepository: IMeetingRepository):
    MeetingUseCase {
    override suspend fun joinMeeting(meetingID: String) = meetingRepository.jointMeeting(meetingID)

    override suspend fun fetchMeetingTime(meetingID: String) = meetingRepository.fetchMeetingTime(meetingID)
}