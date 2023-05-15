package com.example.fpt.classifer.model


data class BehaviourData(
    val emotionState: String,
    val emotionPercent: Double,
    val engagementValue: Double,
    val engagementState: String,
){
    override fun toString(): String {
        return "(emotionState: ${emotionState},emotionPercent: ${emotionPercent},engagementValue:${engagementValue},engagementState:${engagementState})"
    }
}
