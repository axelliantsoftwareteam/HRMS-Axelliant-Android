package com.axelliant.hris.features.dashboard.presentation

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
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
    private var originalSoftInputMode: Int? = null
    private var thinkingRow: View? = null
    private val thinkingAnimators = mutableListOf<ValueAnimator>()

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
        setupKeyboardResize()
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

    override fun onResume() {
        super.onResume()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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
                    updateThinkingBubble(state.isAwaitingAssistantReply)
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

    private fun setupKeyboardResize() {
        originalSoftInputMode = requireActivity().window.attributes.softInputMode
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun updateSendButtonState() {
        val hasMessage = !binding.messageInput.text?.toString().isNullOrBlank()
        val canSend = hasMessage && !viewModel.uiState.value.isAwaitingAssistantReply
        binding.sendButton.isEnabled = canSend
        binding.sendButton.alpha = if (canSend) 1f else 0.45f
        binding.sendIcon.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                if (canSend) R.color.ds_neutral_white else R.color.ds_text_muted
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
        updateThinkingBubble(false)
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

    private fun updateThinkingBubble(shouldShow: Boolean) {
        if (shouldShow) {
            if (thinkingRow == null) {
                thinkingRow = createThinkingRow().also { row ->
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
            }
        } else {
            thinkingAnimators.forEach { it.cancel() }
            thinkingAnimators.clear()
            thinkingRow?.let { binding.messageContainer.removeView(it) }
            thinkingRow = null
        }
    }

    private fun createThinkingRow(): View {
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

        val bubble = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.bg_agent_bubble_incoming)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.ds_space_16),
                resources.getDimensionPixelSize(R.dimen.ds_space_12),
                resources.getDimensionPixelSize(R.dimen.ds_space_16),
                resources.getDimensionPixelSize(R.dimen.ds_space_12)
            )
        }

        repeat(THINKING_DOT_COUNT) { index ->
            val dot = View(requireContext()).apply {
                alpha = THINKING_DOT_MIN_ALPHA
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(requireContext(), R.color.ds_outline_strong))
                }
            }
            bubble.addView(
                dot,
                LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.ds_space_8),
                    resources.getDimensionPixelSize(R.dimen.ds_space_8)
                ).apply {
                    if (index > 0) {
                        leftMargin = resources.getDimensionPixelSize(R.dimen.ds_space_6)
                    }
                }
            )
            startThinkingDotAnimation(dot, index)
        }

        row.addView(
            bubble,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(R.dimen.ds_space_32)
            ).apply {
                leftMargin = resources.getDimensionPixelSize(R.dimen.ds_space_14)
            }
        )

        return row
    }

    private fun startThinkingDotAnimation(dot: View, index: Int) {
        val animator = ValueAnimator.ofFloat(
            THINKING_DOT_MIN_ALPHA,
            THINKING_DOT_MAX_ALPHA,
            THINKING_DOT_MIN_ALPHA
        ).apply {
            duration = THINKING_DOT_DURATION_MS
            startDelay = index.toLong() * THINKING_DOT_DELAY_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                dot.alpha = value
                dot.scaleX = THINKING_DOT_MIN_SCALE + (value * THINKING_DOT_SCALE_RANGE)
                dot.scaleY = THINKING_DOT_MIN_SCALE + (value * THINKING_DOT_SCALE_RANGE)
            }
            start()
        }
        thinkingAnimators.add(animator)
    }

    override fun onDestroyView() {
        updateThinkingBubble(false)
        originalSoftInputMode?.let { requireActivity().window.setSoftInputMode(it) }
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "AgentConsoleFragment"
        const val THINKING_DOT_COUNT = 3
        const val THINKING_DOT_MIN_ALPHA = 0.35f
        const val THINKING_DOT_MAX_ALPHA = 1f
        const val THINKING_DOT_MIN_SCALE = 0.85f
        const val THINKING_DOT_SCALE_RANGE = 0.15f
        const val THINKING_DOT_DURATION_MS = 900L
        const val THINKING_DOT_DELAY_MS = 150L
    }
}
