package com.example.fpt.classifer.model


data class ProcessingData(
    val imageCapture: ByteArray? = null,
    val isSleepy: Boolean,
    val attentionScore: Float,
)
