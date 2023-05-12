package com.example.fpt.classifer.model

import com.example.fpt.classifer.ClassifierResult
import java.io.Serializable


class EmotionData(emotionScores: FloatArray) : ClassifierResult, Serializable {
    var emotionScores: FloatArray? = null

    init {
        this.emotionScores = FloatArray(emotionScores.size)
        this.emotionScores?.let { System.arraycopy(emotionScores, 0, it, 0, emotionScores.size) }
    }

    override fun toString(): String {
        return getEmotion(emotionScores)
    }

    companion object {
        private val emotions =
            arrayOf("", "Anger", "Disgust", "Fear", "Happiness", "Neutral", "Sadness", "Surprise")
        fun getEmotion(emotionScores: FloatArray?): String {
            var bestInd = -1
            var maxEmotionScore = 0f
            if (emotionScores != null) {
                for (i in emotionScores.indices) {
                    if (maxEmotionScore < emotionScores[i]) {
                        maxEmotionScore = emotionScores[i]
                        bestInd = i
                    }
                }
            }
            return "${emotions[bestInd + 1]}#$maxEmotionScore"
        }
    }
}