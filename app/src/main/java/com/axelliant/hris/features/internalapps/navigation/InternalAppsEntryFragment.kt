package com.axelliant.hris.features.internalapps.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InternalAppsEntryFragment : Fragment() {

    @Inject
    lateinit var workspaceSessionProvider: WorkspaceSessionProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FrameLayout(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val destination = if (workspaceSessionProvider.hasValidSession(WorkspaceKey.INTERNAL_APPS)) {
            R.id.iaDashboardFragment
        } else {
            R.id.commonLoginFragment
        }
        findNavController().navigate(
            destination,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.iaInternalAppsEntryFragment, true)
                .build()
        )
    }
}
