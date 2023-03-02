package com.example.fpt.classifer

import android.content.Context
import com.example.fpt.classifer.model.EmotionData

class EmotionTfLiteClassifier(context: Context?) : TfLiteClassifier(context, MODEL_FILE) {

    override fun addPixelValue(value: Int) {
        imgData?.putFloat((value and 0xFF) - 103.939f)
        imgData?.putFloat((value shr 8 and 0xFF) - 116.779f)
        imgData?.putFloat((value shr 16 and 0xFF) - 123.68f)
    }

    override fun getResults(outputs: Array<Array<FloatArray?>>?): ClassifierResult? {
        val emotionsScores = outputs?.get(0)?.get(0)
        return emotionsScores?.let { EmotionData(it) }
    }



    companion object {
        private const val TAG = "EmotionTfLite"
        private const val MODEL_FILE = "emotions_mobilenet.tflite"
    }
}