package com.demo.domain.domain.entities

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class BehaviourInfo(
    val isSleep: Boolean? = false,
    val isLookAway: Boolean? = false,
    val emotion: String? = null
){
//    @Exclude
//    fun toMap(): Map<String, Any?> {
//        return mapOf(
//            "isSleep" to isSleep,
//            "isLookAway" to isLookAway,
//            "emotion" to emotion
//        )
//    }
}
