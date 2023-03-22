package com.example.fpt.classifer

import com.google.mlkit.vision.face.Face

fun Face.convertToBehaviour(): Boolean{
    val isLookAway = this.headEulerAngleY <= -36 ||this.headEulerAngleY >= 36 || this.
    return x<10
}