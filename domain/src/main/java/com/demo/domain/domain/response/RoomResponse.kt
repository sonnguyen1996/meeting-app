package com.demo.domain.domain.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

open class RoomResponse : Serializable {
    @SerializedName("roomId")
    val roomId: String = ""

    @SerializedName("customRoomId")
    val customRoomId: String = ""

    @SerializedName("userId")
    val userId: String = ""

    @SerializedName("disabled")
    val disabled: Boolean = false

    @SerializedName("createdAt")
    val createdAt: String = ""

    @SerializedName("updatedAt")
    val updatedAt: String = ""

    @SerializedName("id")
    val id: String = ""

    @SerializedName("links")
    val links: LinkResponse? = null

    @SerializedName("statusCode")
    val statusCode: String? = null

    @SerializedName("error")
    val error: String? = null
}