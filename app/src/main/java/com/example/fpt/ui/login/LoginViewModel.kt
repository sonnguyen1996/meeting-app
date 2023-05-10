package com.example.fpt.ui.login


import com.demo.domain.domain.usecase.meeting_interface.MeetingUseCase
import com.example.fpt.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor (
    private val useCase: MeetingUseCase,
) : BaseViewModel() {}