package com.example.fpt.ui.base

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import com.demo.domain.domain.entities.ErrorResult


abstract class BaseFragment<T : BaseViewModel, Y : ViewDataBinding> : Fragment() {

    private val TAG = this.javaClass.name

    protected var hostActivity: AppCompatActivity? = null

    protected lateinit var viewModel: T

    protected lateinit var binding: Y

    protected var navController: NavController? = null

    protected lateinit var baseContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(provideViewModelClass())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, provideLayoutId(), container, false)
        try {
            navController = NavHostFragment.findNavController(this)
        } catch (e: Exception) {
            Log.d("xxx", "${e.message}")
        }
        initView()
        return binding.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initActions()
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        baseContext = context
        hostActivity = context as AppCompatActivity
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearDisposable()
    }

    protected open fun navigate(@IdRes action: Int) {
        if (navController != null) {
            val currentDestination = navController?.currentDestination
            if (currentDestination?.getAction(action) != null) {
                navController?.navigate(action)
            }
        }
    }

    protected open fun navigate(@IdRes action: Int, bundle: Bundle?) {
        if (navController != null) {
            val currentDestination = navController?.currentDestination
            if (currentDestination?.getAction(action) != null) {
                navController?.navigate(action, bundle)
            }
        }
    }

    protected open fun popBackStack(@IdRes desId: Int) {
        if (navController != null) {
            val currentDestination = navController?.currentDestination
            if (currentDestination != null && desId != currentDestination.id) {
                navController?.popBackStack(desId, false)
            }
        }
    }

    protected open fun popBackParent() {
        if (navController != null) {
            val currentDestination =
                navController?.currentDestination as FragmentNavigator.Destination?
            if (currentDestination != null) {
                currentDestination.parent?.startDestinationId?.let {
                    navController?.popBackStack(
                        it,
                        true
                    )
                }
            }
        }
    }

    protected open fun popBackStack() {
        if (navController != null) {
            val thisDestinationName = this.javaClass.name
            val currentDestination =
                navController?.currentDestination as FragmentNavigator.Destination?
            if (currentDestination != null) {
                val currentDestinationName = currentDestination.className
                if (thisDestinationName == currentDestinationName) {
                    navController?.popBackStack(currentDestination.id, true)
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
    }

    protected abstract fun isNeedHideBottomBar(): Boolean

    @LayoutRes
    protected abstract fun provideLayoutId(): Int

    protected abstract fun onRequestError(errorResponse: ErrorResult)

    protected abstract fun provideViewModelClass(): Class<T>

    protected abstract fun initActions()

    protected abstract fun initData()

    protected abstract fun initView()
}