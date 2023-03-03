package mobiletv.datasource.network.api

import com.demo.domain.domain.response.RoomResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface RemoteAPI {
    @GET("rooms/validate/{id}")
    suspend fun joinMeeting(
        @Path("id") id: String
    ): RoomResponse

}
