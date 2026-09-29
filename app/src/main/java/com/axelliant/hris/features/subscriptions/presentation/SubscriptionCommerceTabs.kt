package com.axelliant.hris.features.subscriptions.presentation

import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.extention.showSuccessMsg
import com.google.android.material.tabs.TabLayout

object SubscriptionCommerceTabs {
    const val SUBSCRIPTIONS = 0
    const val PLANS = 1
    const val INVOICES = 2
    const val RECONCILIATION = 3

    fun bind(fragment: Fragment, tabs: TabLayout, selectedIndex: Int) {
        var restoringSelection = false
        tabs.clearOnTabSelectedListeners()
        tabs.getTabAt(selectedIndex)?.select()
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (restoringSelection || tab.position == selectedIndex) return
                when (tab.position) {
                    SUBSCRIPTIONS -> navigateAfterIndicator(fragment, tabs, R.id.iaSubscriptionsFragment)
                    PLANS -> navigateAfterIndicator(fragment, tabs, R.id.iaSubscriptionPlansFragment)
                    INVOICES, RECONCILIATION -> {
                        fragment.requireContext().showSuccessMsg(
                            fragment.getString(R.string.subscriptions_coming_soon)
                        )
                        tabs.postDelayed({
                            restoringSelection = true
                            tabs.getTabAt(selectedIndex)?.select()
                            restoringSelection = false
                        }, INDICATOR_ANIMATION_DELAY_MS)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun navigateAfterIndicator(fragment: Fragment, tabs: TabLayout, destinationId: Int) {
        tabs.postDelayed({
            if (!fragment.isAdded) return@postDelayed
            val controller = fragment.findNavController()
            if (controller.currentDestination?.id == destinationId) return@postDelayed
            if (destinationId == R.id.iaSubscriptionsFragment &&
                controller.popBackStack(R.id.iaSubscriptionsFragment, false)
            ) {
                return@postDelayed
            }
            controller.navigate(
                destinationId,
                null,
                NavOptions.Builder()
                    .setEnterAnim(android.R.anim.fade_in)
                    .setExitAnim(android.R.anim.fade_out)
                    .setPopEnterAnim(android.R.anim.fade_in)
                    .setPopExitAnim(android.R.anim.fade_out)
                    .build()
            )
        }, INDICATOR_ANIMATION_DELAY_MS)
    }

    private const val INDICATOR_ANIMATION_DELAY_MS = 120L
}
