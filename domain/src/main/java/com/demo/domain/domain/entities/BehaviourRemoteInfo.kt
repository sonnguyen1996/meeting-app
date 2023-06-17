package com.demo.domain.domain.entities

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class BehaviourRemoteInfo(
    var studentId: String? = null,
    val isSleep: Boolean? = false,
    val isFocus: Boolean? = false,
    val emotion: String? = null,
    val engagementState: String? = null,
    val timeStamp: String? = null

) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "studentId" to studentId,
            "isSleep" to isSleep,
            "isFocus" to isFocus,
            "emotion" to emotion,
            "engagementState" to engagementState,
            "timeStamp" to timeStamp,
            )
    }

    override fun toString(): String {
        return "studentId:${studentId},isSleep :$isSleep,isFocus: $isFocus,emotion:$emotion,engagementState:$engagementState, timeStamp:$timeStamp"
    }
}
