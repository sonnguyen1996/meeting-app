package com.demo.domain.domain.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

open class MeetingInfo : Serializable {
    @SerializedName("meetingId")
    var meetingId: String = ""

    @SerializedName("localParticipantName")
    var localParticipantName: String = ""
}