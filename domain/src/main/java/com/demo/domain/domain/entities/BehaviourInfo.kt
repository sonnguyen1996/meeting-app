package com.demo.domain.domain.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

open class BehaviourInfo : Serializable {
    @SerializedName("userID")
    var userID: String = ""

    @SerializedName("isSleep")
    var isSleep: Boolean = false

    @SerializedName("isLookAway")
    var isLookAway: Boolean = false

    @SerializedName("emotion")
    var emotion: String = ""
}