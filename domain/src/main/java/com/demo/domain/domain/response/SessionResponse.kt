package com.demo.domain.domain.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

open class SessionResponse : Serializable {
    @SerializedName("data")
    val data: List<SessionData>? = null
}

class SessionData : Serializable {
    @SerializedName("start")
    val start: String = ""
}
