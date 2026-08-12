package com.axelliant.hris.features.dashboard.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentAgentConsoleBinding
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.features.agent.presentation.AgentConsoleViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AgentConsoleFragment : Fragment() {
    private var _binding: FragmentAgentConsoleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AgentConsoleViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgentConsoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.sendButton.setOnClickListener {
            viewModel.sendMessage(binding.messageInput.text?.toString().orEmpty())
        }
        binding.moreButton.setOnClickListener {
            viewModel.clearSession()
            binding.messageInput.text?.clear()
        }

        observeAgentSession()
        viewModel.prepareSession()
    }

    private fun observeAgentSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.sendButton.isEnabled = !state.isLoading
                    binding.sendButton.alpha = if (state.isLoading) 0.45f else 1f
                    binding.agentStatusText.setText(
                        if (state.isLoading) {
                            R.string.agent_status_waiting
                        } else {
                            R.string.agent_status_ready
                        }
                    )
                    state.pendingMessage?.let { message ->
                        appendUserMessage(message)
                        binding.messageInput.text?.clear()
                        viewModel.consumePendingMessage()
                    }
                    state.error?.let { error ->
                        requireContext().showErrorMsg(error)
                    }
                }
            }
        }
    }

    private fun appendUserMessage(message: String) {
        val bubble = TextView(requireContext()).apply {
            text = message
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_neutral_white))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.ds_text_body))
            setBackgroundResource(R.drawable.bg_agent_bubble_outgoing)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.ds_space_16),
                resources.getDimensionPixelSize(R.dimen.ds_space_16),
                resources.getDimensionPixelSize(R.dimen.ds_space_16),
                resources.getDimensionPixelSize(R.dimen.ds_space_16)
            )
            maxWidth = (resources.displayMetrics.widthPixels * 0.72f).toInt()
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.END
            topMargin = resources.getDimensionPixelSize(R.dimen.ds_space_16)
        }

        binding.messageContainer.addView(bubble, params)
        binding.agentConversationScroll.post {
            binding.agentConversationScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
