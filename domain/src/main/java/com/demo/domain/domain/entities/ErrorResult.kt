package com.demo.domain.domain.entities

data class ErrorResult(
    val errorMessage: String?,
    val apiName: String? = "",
    val email: String? = "",
    val errorCode: Int = 500
)