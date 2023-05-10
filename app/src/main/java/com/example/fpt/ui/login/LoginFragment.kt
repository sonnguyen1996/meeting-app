package com.example.fpt.ui.login

import com.demo.domain.domain.entities.ErrorResult
import com.example.demothesisfpteduvn.R
import com.example.demothesisfpteduvn.databinding.FragmentLoginBinding
import com.example.fpt.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment<LoginViewModel, FragmentLoginBinding>() {

    override fun isNeedHideBottomBar() = true

    override fun onRequestError(errorResponse: ErrorResult) {
    }

    override fun initView() {
    }

    override fun initData() {
    }

    override fun initActions() {
        binding.testButton.setOnClickListener {
            binding.testButton.startAnimation()
            navigate(R.id.action_loginFragment_to_engagementVisuallyFragment)
        }
    }

    override fun provideViewModelClass() = LoginViewModel::class.java

    override fun provideLayoutId() = R.layout.fragment_login

}