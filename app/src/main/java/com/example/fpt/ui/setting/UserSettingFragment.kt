package com.example.fpt.ui.setting

import com.demo.domain.domain.entities.ErrorResult
import com.example.demothesisfpteduvn.R
import com.example.demothesisfpteduvn.databinding.FragmentUserSettingBinding
import com.example.fpt.ui.base.BaseFragment

class UserSettingFragment : BaseFragment<SettingViewModel, FragmentUserSettingBinding>(){

    override fun initView() {
    }

    override fun initData() {
    }

    override fun initActions() {
    }

    override fun onRequestError(errorResponse: ErrorResult) {
    }

    override fun provideLayoutId()  = R.layout.fragment_user_setting

    override fun provideViewModelClass()  = SettingViewModel::class.java


    override fun isNeedHideBottomBar() = false

}