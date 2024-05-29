package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import com.axelliant.android_erp.R
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.config.AppConst
import com.axelliant.android_erp.databinding.FragmentLoginBinding
import com.axelliant.android_erp.event.EventObserver
import com.axelliant.android_erp.extention.showErrorMsg
import com.axelliant.android_erp.navigation.AppNavigator
import com.axelliant.android_erp.utils.SessionManager
import com.axelliant.android_erp.viewmodel.LoginViewModel
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

        binding.btnMicLogin.setOnClickListener(View.OnClickListener {
            if (mSingleAccountApp == null) {
                return@OnClickListener
            }
            val signInParameters: SignInParameters = SignInParameters.builder()
                .withActivity(requireActivity())
                .withLoginHint(null)
                .withScopes(listOf(*scopes))
                .withCallback(authInteractiveCallback)
                .build()
            mSingleAccountApp!!.signIn(signInParameters)
        })
        binding.btnLogin.setOnClickListener {
            AppNavigator.navigateToHome()
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

    private val authInteractiveCallback: AuthenticationCallback
        get() = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                // Successfully got a token, use it to call a protected resource - MSGraph
                Log.d(TAG, "Successfully authenticated")
                Log.d(TAG, "ID Token: " + authenticationResult.account.idToken)

                sessionManager.saveToken(authenticationResult.account.idToken)
                sessionManager.createLoginSession(
                    username = null,
                    userPass = null,
                    accessToken = authenticationResult.account.idToken,
                    lastRemember = true
                )
                AppConst.TOKEN = authenticationResult.account.idToken

//                micLogin(authenticationResult.account.idToken)


                // Update account
                mAccount = authenticationResult.account
                updateUI()
                AppNavigator.navigateToHome()
            }

            override fun onError(exception: MsalException) {
                // Failed to acquireToken
                Log.d(TAG, "Authentication failed: $exception")
                displayError(exception)
            }

            override fun onCancel() {
                // User canceled the authentication
                Log.d(TAG, "User cancelled login.")
            }
        }

    private fun displayError(exception: Exception) {
        requireContext().showErrorMsg(exception.toString())
    }

    private fun updateUI() {
        if (mAccount != null)
        {
            binding.btnMicLogin.isEnabled = false
            binding.btnLogin.isEnabled = true
        } else {
            binding.btnMicLogin.isEnabled = true
            binding.btnLogin.isEnabled = false
        }
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

                if (response?.meta?.status == true) {
                    // success
                    sessionManager.saveToken(response.access_token)
                    sessionManager.createLoginSession(
                        username = null,
                        userPass = null,
                        accessToken = response.access_token,
                        lastRemember = true
                    )
                    AppConst.TOKEN = response.access_token
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val TAG = LoginFragment::class.java.simpleName
    }
}

