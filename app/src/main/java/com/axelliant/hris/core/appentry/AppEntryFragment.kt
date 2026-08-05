package com.axelliant.hris.core.appentry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.databinding.FragmentAppEntryBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppEntryFragment : Fragment() {

    @Inject
    lateinit var appEntryNavigator: AppEntryNavigator

    private var _binding: FragmentAppEntryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindWorkspaceButtons()
    }

    private fun bindWorkspaceButtons() {
        binding.hrisButton.isEnabled = appEntryNavigator.isEnabled(WorkspaceKey.HRIS)
        binding.internalAppsButton.isEnabled = appEntryNavigator.isEnabled(WorkspaceKey.INTERNAL_APPS)

        binding.hrisButton.setOnClickListener {
            appEntryNavigator.navigate(WorkspaceKey.HRIS, findNavController())
        }
        binding.internalAppsButton.setOnClickListener {
            appEntryNavigator.navigate(WorkspaceKey.INTERNAL_APPS, findNavController())
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
