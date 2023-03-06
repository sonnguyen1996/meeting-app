package mobiletv.datasource.network.api

import com.demo.domain.domain.response.RoomResponse
import com.demo.domain.domain.response.SessionResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface RemoteAPI {
    @GET("rooms/validate/{id}")
    suspend fun joinMeeting(
        @Path("id") id: String
    ): RoomResponse

    @GET("https://api.videosdk.live/v2/sessions/?roomId={meetingId}")
    suspend fun fetchMeetingTime(
        @Path("meetingId") meetingId: String
    ): SessionResponse

}
