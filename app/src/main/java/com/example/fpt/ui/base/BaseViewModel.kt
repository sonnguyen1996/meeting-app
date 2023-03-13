package com.example.fpt.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.demo.domain.domain.entities.ErrorResult
import kotlinx.coroutines.*

open class BaseViewModel : ViewModel() {

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