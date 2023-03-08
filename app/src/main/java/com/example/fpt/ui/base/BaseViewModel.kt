package com.example.fpt.ui.base

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.demo.domain.domain.entities.ErrorResult
import kotlinx.coroutines.*
import java.util.LinkedHashMap

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    private var ioJob: Job = Job()

    private var heavyJob: Job = Job()

    private var mainJob: Job = Job()

    val coroutineScope by lazy { CoroutineScope(ioJob + Dispatchers.IO + exceptionHandler) }

    val heavyTaskScope by lazy { CoroutineScope(heavyJob + Dispatchers.IO + exceptionHandler) }

    val coroutineMainScope = CoroutineScope(mainJob + Dispatchers.Main)



    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, error ->
        val errorResult =
            ErrorResult(errorMessage = error.message)
        apiErrorResponse.postValue(errorResult)
        coroutineContext.cancel()
    }

    val apiErrorResponse: MutableLiveData<ErrorResult> =
        MutableLiveData()

    protected val apiFailResponse =
        MutableLiveData<Boolean>()

    suspend fun executeApi(requestFunction: suspend () -> Unit) {
        try {
            requestFunction()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    open fun clearDisposable() {
        ioJob.cancel()
        mainJob.cancel()
        heavyJob.cancel()
    }

    open fun clearObserverTask() {
        heavyJob.cancel()
    }
}