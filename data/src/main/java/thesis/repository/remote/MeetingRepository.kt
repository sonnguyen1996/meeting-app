package thesis.repository.remote

import com.demo.domain.domain.repository.IMeetingRepository
import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse
import thesis.datasource.network.api.RemoteAPI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepository @Inject constructor(
    val apiService: RemoteAPI,
) : IMeetingRepository {
    override fun jointMeeting(id: String): RoomResponse {
        TODO("Not yet implemented")
    }

    override fun fetchMeetingTime(id: String): SessionResponse {
        TODO("Not yet implemented")
    }

}