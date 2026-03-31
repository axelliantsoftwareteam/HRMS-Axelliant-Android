package com.axelliant.hris.screens

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentLoginBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.SessionManager
import com.axelliant.hris.viewmodel.LoginViewModel
import com.google.android.material.textview.MaterialTextView
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import org.koin.android.ext.android.inject

class LoginFragment : BaseFragment() {

    private val sessionManager: SessionManager by inject()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val loginViewModel: LoginViewModel by inject()

    /* Azure AD Variables */
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private val scopes = arrayOf("User.Read")
    private var mAccount: IAccount? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvAppVersion.text = appVersion()
        binding.btnMicLogin.setOnClickListener(View.OnClickListener {

            if (mSingleAccountApp == null) {
                requireContext().showErrorMsg("SDK initialise error")
                return@OnClickListener
            }
            showDialog()
            val signInParameters: SignInParameters = SignInParameters.builder()
                .withActivity(requireActivity())
                .withLoginHint(null)
                .withScopes(listOf(*scopes))
                .withCallback(authInteractiveCallback)
                .build()
            mSingleAccountApp!!.signIn(signInParameters)
        })


        loginViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        binding.tvViewDetail.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_leave_pend)
        setTextBold(binding.tvEmploy)
        setTextNormal(binding.tvVendor)
        binding.tvViewDetail.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction {
                // Scale down the other view
                binding.tvDelete.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
            }
            .start()

        binding.tvViewDetail.setOnClickListener {
            // Change the background drawable for the clicked view
            binding.tvViewDetail.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_leave_pend)
            binding.tvDelete.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_bgg)

            setTextBold(binding.tvEmploy)
            setTextNormal(binding.tvVendor)

            // Scale up the clicked view
            binding.tvViewDetail.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
                .withEndAction {
                    // Scale down the other view
                    binding.tvDelete.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }

        binding.tvDelete.setOnClickListener {
            // Change the background drawable for the clicked view
            binding.tvDelete.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_leave_pend)
            binding.tvViewDetail.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_bgg)
            setTextBold(binding.tvVendor)
            setTextNormal(binding.tvEmploy)
            // Scale up the clicked view
            binding.tvDelete.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
                .withEndAction {
                    // Scale down the other view
                    binding.tvViewDetail.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }


        binding.btnLogin.setOnClickListener {
//            AppNavigator.navigateToHome()
            requireContext().showSuccessMsg()
        }
        PublicClientApplication.createSingleAccountPublicClientApplication(
            requireContext(),
            R.raw.auth_config_ciam_auth,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    // You can now use mSingleAccountApp to interact with the SDK
                    loadAccount()
                }

                override fun onError(exception: MsalException) {
                    // Handle the exception
                    displayError(exception)
                    Log.d(TAG, exception.toString())

                }
            })
    }

    private fun setTextNormal(tvEmploy: MaterialTextView) {
        tvEmploy.apply {
            setTypeface(null,Typeface.NORMAL)
        }
    }

    private fun setTextBold(tvEmploy: MaterialTextView) {
        tvEmploy.apply {
            setTypeface(null,Typeface.BOLD)
        }
    }

    private val authInteractiveCallback: AuthenticationCallback
        get() = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                // Successfully got a token, use it to call a protected resource - MSGraph
                Log.d(TAG, "Successfully authenticated")
                Log.d(TAG, "ID Token: " + authenticationResult.account.idToken)

                micLogin(authenticationResult.account.idToken)
                // Update account
                mAccount = authenticationResult.account
                updateUI()
            }

            override fun onError(exception: MsalException) {
                hideDialog()
                // Failed to acquireToken
                Log.d(TAG, "Authentication failed: $exception")
                displayError(exception)

            }

            override fun onCancel() {
                hideDialog()
                // User canceled the authentication
                Log.d(TAG, "User cancelled login.")
            }
        }

    private fun displayError(exception: Exception) {
        requireContext().showErrorMsg(exception.toString())
    }

    private fun updateUI() {
        binding.btnMicLogin.isEnabled = mAccount == null
    }

    private fun loadAccount() {
        mSingleAccountApp?.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                mAccount = activeAccount
                updateUI()
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                mAccount = currentAccount
                updateUI()
            }

            override fun onError(exception: MsalException) {
                displayError(exception)
            }
        })
    }


    private fun micLogin(token: String?) {
        if (token != null) {
            loginViewModel.postMicToken(
                token
            )
        }
        loginViewModel.userLoginResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response!=null && response.meta.status==true)
                {
                    // success
                    sessionManager.saveUserEmail(response.access_token?.email)
                    sessionManager.saveToken(response.access_token?.api_key.plus(":").plus(response.access_token?.api_sec))
                    sessionManager.createLoginSession(
                        username = response.access_token?.email,
                        userPass = null,
                        accessToken = response.access_token?.api_key.plus(":").plus(response.access_token?.api_sec),
                        lastRemember = true
                    )
                    AppConst.TOKEN =response.access_token?.api_key.plus(":").plus(response.access_token?.api_sec)
                    AppNavigator.navigateToHome()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                    /*
                     * Removes the signed-in account and cached tokens from this app (or device, if the device is in shared mode).
                   */
                    mSingleAccountApp!!.signOut(object :
                        ISingleAccountPublicClientApplication.SignOutCallback {
                        override fun onSignOut() {
                            mAccount = null
                        }
                        override fun onError(exception: MsalException) {
                            requireContext().showErrorMsg(exception.toString())
                        }
                    })

                }

            })


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val TAG = LoginFragment::class.java.simpleName
    }
}
