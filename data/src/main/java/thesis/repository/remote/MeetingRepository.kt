package thesis.repository.remote

import com.demo.domain.domain.repository.IMeetingRepository
import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse
import thesis.datasource.network.api.RemoteAPI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepository @Inject constructor(
    private val apiService: RemoteAPI,
) : IMeetingRepository {
    override suspend fun jointMeeting(id: String) = apiService.joinMeeting(id)
    override suspend fun fetchMeetingTime(id: String) = apiService.fetchMeetingTime(id)

}