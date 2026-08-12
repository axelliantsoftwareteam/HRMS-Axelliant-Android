package com.axelliant.hris.features.commonlogin.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.hideKeyboard
import com.axelliant.hris.core.extensions.hideKeyboardOnClick
import com.axelliant.hris.databinding.IaFragmentLoginBinding
import com.axelliant.hris.features.auth.microsoft.MicrosoftAuthManager
import com.axelliant.hris.features.auth.microsoft.MicrosoftAuthResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CommonLoginFragment : Fragment() {
    private var _binding: IaFragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CommonLoginViewModel by viewModels()
    private var hasNavigated = false

    @Inject
    lateinit var microsoftAuthManager: MicrosoftAuthManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = IaFragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindInputs()
        bindActions()
        bindKeyboardDismissal()
        observeState()
    }

    private fun bindInputs() {
        binding.emailInput.doAfterTextChanged { text ->
            viewModel.onEmailChanged(text?.toString().orEmpty())
        }
        binding.passwordInput.doAfterTextChanged { text ->
            viewModel.onPasswordChanged(text?.toString().orEmpty())
        }
        binding.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitLogin()
                true
            } else {
                false
            }
        }
    }

    private fun bindActions() {
        binding.loginButton.setOnClickListener { submitLogin() }
        binding.microsoftButton.setOnClickListener { signInWithMicrosoft() }
        binding.rememberCheckBox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onRememberMeChanged(isChecked)
        }
        binding.rememberMeText.setOnClickListener {
            binding.rememberCheckBox.isChecked = !binding.rememberCheckBox.isChecked
        }
        binding.forgotPasswordText.setOnClickListener {
            binding.errorText.text = getString(R.string.forgot_password_unavailable)
            binding.errorText.isVisible = true
        }
    }

    private fun bindKeyboardDismissal() = with(binding) {
        root.hideKeyboardOnClick(emailInput, passwordInput)
        loginScrollView.hideKeyboardOnClick(emailInput, passwordInput)
        loginContentLayout.hideKeyboardOnClick(emailInput, passwordInput)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: CommonLoginUiState) = with(binding) {
        emailLabel.text = getString(R.string.email_hint)
        passwordLabel.text = getString(R.string.password_hint)

        if (emailInput.text?.toString() != state.email) {
            emailInput.setText(state.email)
            emailInput.setSelection(state.email.length)
        }
        if (passwordInput.text?.toString() != state.password) {
            passwordInput.setText(state.password)
            passwordInput.setSelection(state.password.length)
        }
        if (rememberCheckBox.isChecked != state.rememberMe) {
            rememberCheckBox.isChecked = state.rememberMe
        }

        emailLayout.error = state.validation.emailErrorRes?.let(::getString)
        passwordLayout.error = state.validation.passwordErrorRes?.let(::getString)

        val isBusy = state.isLoading || state.isMicrosoftLoading
        val canLogin = state.isLoginEnabled && !isBusy
        emailInput.isEnabled = !isBusy
        passwordInput.isEnabled = !isBusy
        rememberCheckBox.isEnabled = !isBusy
        rememberMeText.isEnabled = !isBusy
        bindLoginButton(canLogin)
        microsoftButton.isEnabled = !isBusy
        loginProgress.isVisible = isBusy

        val authError = state.errorMessage ?: state.errorMessageRes?.let(::getString)
        errorText.text = authError.orEmpty()
        errorText.isVisible = !authError.isNullOrBlank()

        if (state.isAuthenticated) {
            openHomeDashboard()
        }
    }

    private fun IaFragmentLoginBinding.bindLoginButton(canLogin: Boolean) {
        val color = ContextCompat.getColor(
            requireContext(),
            if (canLogin) R.color.ia_white else R.color.ds_disabled_text
        )
        loginButton.isEnabled = canLogin
        loginButton.setText(R.string.ia_login_action)
        loginButton.setTextColor(color)
        loginButton.iconTint = ColorStateList.valueOf(color)
    }

    private fun submitLogin() {
        binding.root.hideKeyboard()
        viewModel.signInWithInternalAppsPassword()
    }

    private fun signInWithMicrosoft() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.onMicrosoftLoginStarted()
            try {
                when (val result = microsoftAuthManager.signIn(requireActivity())) {
                    is MicrosoftAuthResult.Success -> viewModel.acceptMicrosoftToken(
                        idToken = result.idToken,
                        graphAccessToken = result.accessToken
                    )

                    MicrosoftAuthResult.Cancelled -> viewModel.onMicrosoftLoginCancelled()
                    is MicrosoftAuthResult.Error -> viewModel.onMicrosoftLoginFailed(result.message)
                }
            } catch (exception: Exception) {
                viewModel.onMicrosoftLoginFailed(exception.message)
            }
        }
    }

    private fun openHomeDashboard() {
        if (hasNavigated || findNavController().currentDestination?.id != R.id.commonLoginFragment) return
        hasNavigated = true
        findNavController().navigate(
            R.id.homeFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.commonLoginFragment, true)
                .build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
