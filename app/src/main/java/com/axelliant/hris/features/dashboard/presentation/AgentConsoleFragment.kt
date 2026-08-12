package com.axelliant.hris.features.dashboard.presentation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
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
            val message = binding.messageInput.text?.toString().orEmpty()
            Log.d(TAG, "Send clicked. messageLength=${message.trim().length}")
            viewModel.sendMessage(message)
        }
        binding.messageInput.doAfterTextChanged {
            updateSendButtonState()
        }
        updateSendButtonState()
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
                    updateSendButtonState()
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
                    }
                    state.assistantReply?.takeIf { it.isNotBlank() }?.let { reply ->
                        appendAssistantMessage(reply)
                    }
                    if (state.pendingMessage != null || state.assistantReply != null) {
                        viewModel.consumeMessageEvents()
                    }
                    state.error?.let { error ->
                        requireContext().showErrorMsg(error)
                    }
                }
            }
        }
    }

    private fun updateSendButtonState() {
        val hasMessage = !binding.messageInput.text?.toString().isNullOrBlank()
        binding.sendButton.isEnabled = hasMessage
        binding.sendButton.alpha = if (hasMessage) 1f else 0.45f
        binding.sendIcon.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                if (hasMessage) R.color.ds_neutral_white else R.color.ds_text_muted
            )
        )
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

    private fun appendAssistantMessage(message: String) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.TOP
        }

        val avatar = FrameLayout(requireContext()).apply {
            setBackgroundResource(R.drawable.bg_agent_avatar)
            addView(
                androidx.appcompat.widget.AppCompatImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_ai_sparkles)
                    setColorFilter(ContextCompat.getColor(requireContext(), R.color.ds_neutral_white))
                },
                FrameLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.ds_space_12),
                    resources.getDimensionPixelSize(R.dimen.ds_space_12),
                    android.view.Gravity.CENTER
                )
            )
        }
        row.addView(
            avatar,
            LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.ds_space_24),
                resources.getDimensionPixelSize(R.dimen.ds_space_24)
            )
        )

        val bubble = TextView(requireContext()).apply {
            text = message
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.ds_text_body))
            setBackgroundResource(R.drawable.bg_agent_bubble_incoming)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.ds_space_16),
                resources.getDimensionPixelSize(R.dimen.ds_space_16),
                resources.getDimensionPixelSize(R.dimen.ds_space_16),
                resources.getDimensionPixelSize(R.dimen.ds_space_16)
            )
            maxWidth = (resources.displayMetrics.widthPixels * 0.72f).toInt()
        }
        row.addView(
            bubble,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = resources.getDimensionPixelSize(R.dimen.ds_space_14)
            }
        )

        binding.messageContainer.addView(
            row,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.ds_space_16)
            }
        )
        binding.agentConversationScroll.post {
            binding.agentConversationScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "AgentConsoleFragment"
    }
}
