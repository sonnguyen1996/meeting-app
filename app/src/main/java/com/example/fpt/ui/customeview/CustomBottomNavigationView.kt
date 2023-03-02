package com.example.fpt.ui.customeview
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

open class CustomBottomNavigationView : BottomNavigationView {

    private var selectedItemTag: String = ""

    private var isOnFirstFragment = false

    private var firstFragmentGraphId = 0

    constructor(context: Context) : super(context)
    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
    }

    fun setupWithNavController(
        navGraphIds: List<Int>,
        fragmentManager: FragmentManager,
        containerId: Int,
        intent: Intent
    ): LiveData<NavController> {
        val graphIdToTagMap =
            SparseArray<String>()
        val selectedNavController =
            MutableLiveData<NavController>()
        val selectedItemId = selectedItemId
        for (navGraphId in navGraphIds) {
            val index = navGraphIds.indexOf(navGraphId)
            val fragmentTag = getFragmentTag(index)
            val navHostFragment = obtainNavHostFragment(
                fragmentManager,
                fragmentTag,
                navGraphId,
                containerId
            )
            val graphId = navHostFragment.navController.graph.id
            if (index == 0) {
                firstFragmentGraphId = graphId
            }
            graphIdToTagMap.put(graphId, fragmentTag)
            if (selectedItemId == graphId) {
                selectedNavController.value = navHostFragment.navController
                attachNavHostFragment(fragmentManager, navHostFragment, index == 0)
            } else {
                detachNavHostFragment(fragmentManager, navHostFragment)
            }
        }
        selectedItemTag = graphIdToTagMap[selectedItemId]
        val firstFragmentTag = graphIdToTagMap[firstFragmentGraphId]
        isOnFirstFragment = true && selectedItemTag == firstFragmentTag
        setOnItemSelectedListener { item: MenuItem ->
            if (fragmentManager.isStateSaved) {
                return@setOnItemSelectedListener false
            } else {
                val newlySelectedItemTag = graphIdToTagMap[item.itemId]
                if (selectedItemTag != newlySelectedItemTag) {
                    fragmentManager.popBackStack(
                        firstFragmentTag,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    val selectedFragment =
                        fragmentManager.findFragmentByTag(newlySelectedItemTag) as NavHostFragment?
                    if (firstFragmentTag != newlySelectedItemTag) {
                        var transaction: FragmentTransaction? = null
                        if (selectedFragment != null) {
                            transaction = fragmentManager.beginTransaction()
                                .attach(selectedFragment)
                                .setPrimaryNavigationFragment(selectedFragment)
                        }
                        for (i in 0 until graphIdToTagMap.size()) {
                            val fragmentTag =
                                graphIdToTagMap[graphIdToTagMap.keyAt(i)]
                            if (fragmentTag != newlySelectedItemTag) {
                                if (transaction != null) {
                                    fragmentManager.findFragmentByTag(firstFragmentTag)
                                }
                            }
                        }
                        transaction?.addToBackStack(firstFragmentTag)?.setReorderingAllowed(true)
                            ?.commit()
                    }
                    selectedItemTag = newlySelectedItemTag
                    isOnFirstFragment = selectedItemTag == firstFragmentTag
                    if (selectedFragment != null) {
                        selectedNavController.value = selectedFragment.navController
                    }
                    return@setOnItemSelectedListener true
                } else {
                    return@setOnItemSelectedListener false
                }
            }
        }
        setupItemReselected(graphIdToTagMap, fragmentManager)
        setupDeepLinks(navGraphIds, fragmentManager, containerId, intent)
        fragmentManager.addOnBackStackChangedListener {
            if (!isOnFirstFragment && !isOnBackStack(firstFragmentTag, fragmentManager)) {
                setSelectedItemId(firstFragmentGraphId)
            }
            val controller = selectedNavController.value
            if (controller != null && controller.currentDestination == null) {
                controller.navigate(controller.graph.id)
            }
        }
        return selectedNavController
    }

    private fun obtainNavHostFragment(
        fragmentManager: FragmentManager,
        fragmentTag: String,
        navGraphId: Int,
        containerId: Int
    ): NavHostFragment {
        val existingFragment =
            fragmentManager.findFragmentByTag(fragmentTag)
        if (existingFragment is NavHostFragment) {
            return existingFragment
        }
        val navHostFragment = NavHostFragment.create(navGraphId)
        fragmentManager.beginTransaction().add(containerId, navHostFragment, fragmentTag)
            .commitNow()
        return navHostFragment
    }

    private fun attachNavHostFragment(
        fragmentManager: FragmentManager,
        navHostFragment: NavHostFragment,
        isPrimaryNavFragment: Boolean
    ) {
        val transaction =
            fragmentManager.beginTransaction().attach(navHostFragment)
        if (isPrimaryNavFragment) {
            transaction.setPrimaryNavigationFragment(navHostFragment)
        }
        transaction.commitNow()
    }

    private fun detachNavHostFragment(
        fragmentManager: FragmentManager,
        navHostFragment: NavHostFragment
    ) {
        fragmentManager.beginTransaction().detach(navHostFragment).commitNow()
    }

    private fun setupItemReselected(
        graphIdToTagMap: SparseArray<String>,
        fragmentManager: FragmentManager
    ) {
        setOnItemReselectedListener { item: MenuItem ->
            val newlySelectedItemTag = graphIdToTagMap[item.itemId]
            val selectedFragment =
                fragmentManager.findFragmentByTag(newlySelectedItemTag) as NavHostFragment?
            var navController: NavController? = null
            if (selectedFragment != null) {
                navController = selectedFragment.navController
            }
            navController?.popBackStack(navController.graph.startDestinationId, false)
        }
    }

    private fun setupDeepLinks(
        navGraphIds: List<Int>,
        fragmentManager: FragmentManager,
        containerId: Int,
        intent: Intent
    ) {
        for (navGraphId in navGraphIds) {
            val index = navGraphIds.indexOf(navGraphId)
            val fragmentTag = getFragmentTag(index)
            val navHostFragment = obtainNavHostFragment(
                fragmentManager,
                fragmentTag,
                navGraphId,
                containerId
            )
            if (navHostFragment.navController.handleDeepLink(intent)
                && selectedItemId != navHostFragment.navController.graph
                    .id
            ) {
                selectedItemId = navHostFragment.navController.graph.id
            }
        }
    }

    private fun isOnBackStack(
        backStackName: String,
        fragmentManager: FragmentManager
    ): Boolean {
        val backStackCount = fragmentManager.backStackEntryCount
        for (index in 0 until backStackCount) {
            if (fragmentManager.getBackStackEntryAt(index).name == backStackName) return true
        }
        return false
    }

    private fun getFragmentTag(index: Int): String {
        return "bottomNavigation#$index"
    }
}