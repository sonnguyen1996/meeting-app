package com.demo.domain.domain.usecase

import com.demo.domain.domain.repository.IMeetingRepository
import com.demo.domain.domain.usecase.meeting_interface.IMeetingUseCase
import javax.inject.Inject

class MeetingUseCase @Inject constructor(private val meetingRepository: IMeetingRepository):
    IMeetingUseCase {
    override fun joinMeeting(meetingID: String) = meetingRepository.jointMeeting(meetingID)

    override fun fetchMeetingTime(meetingID: String) = meetingRepository.fetchMeetingTime(meetingID)
}