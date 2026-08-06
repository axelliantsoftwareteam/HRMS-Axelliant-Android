package com.axelliant.hris.core.appentry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.databinding.FragmentAppEntryBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

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
            openWorkspace(WorkspaceKey.HRIS)
        }
        binding.internalAppsButton.setOnClickListener {
            openWorkspace(WorkspaceKey.INTERNAL_APPS)
        }
    }

    private fun openWorkspace(workspace: WorkspaceKey) {
        setButtonsEnabled(false)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = appEntryNavigator.navigate(workspace, findNavController())
            if (!isAdded || _binding == null) return@launch
            when (result) {
                AppEntryNavigationResult.Success -> Unit
                AppEntryNavigationResult.Blocked -> {
                    setButtonsEnabled(true)
                    Toast.makeText(requireContext(), "Workspace is not available.", Toast.LENGTH_SHORT).show()
                }

                is AppEntryNavigationResult.Error -> {
                    setButtonsEnabled(true)
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setButtonsEnabled(isEnabled: Boolean) {
        binding.hrisButton.isEnabled = isEnabled && appEntryNavigator.isEnabled(WorkspaceKey.HRIS)
        binding.internalAppsButton.isEnabled =
            isEnabled && appEntryNavigator.isEnabled(WorkspaceKey.INTERNAL_APPS)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
