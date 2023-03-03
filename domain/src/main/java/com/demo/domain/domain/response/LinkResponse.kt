package com.demo.domain.domain.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

open class LinkResponse : Serializable {
    @SerializedName("get_room")
    val get_room: String = ""

    @SerializedName("get_session")
    val customRget_sessionoomId: String = ""
}