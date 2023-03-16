package thesis.datasource.network.api

import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface RemoteAPI {
    @GET("rooms/validate/{id}")
    suspend fun joinMeeting(
        @Path("id") id: String
    ): RoomResponse

    @GET("sessions/?")
    suspend fun fetchMeetingTime(
        @Query("roomId") meetingId: String
    ): SessionResponse

}
