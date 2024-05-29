package com.axelliant.hrms.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.axelliant.hrms.base.BaseFragment
import com.axelliant.hrms.databinding.FragmentLoginBinding
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.viewmodel.LoginViewModel
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import org.koin.android.ext.android.inject
import java.util.Arrays

class LoginFragment : BaseFragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding
    private val loginViewModel: LoginViewModel by inject()
    /* Azure AD Variables */
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private var mAccount: IAccount? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLoginBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding?.btnMicLogin?.setOnClickListener {
//            AppNavigator.navigateToHome()
//
//        }
        binding?.btnMicLogin?.setOnClickListener(View.OnClickListener {
            if (mSingleAccountApp == null) {
                return@OnClickListener
            }
            val signInParameters: SignInParameters = SignInParameters.builder()
                .withActivity(requireActivity())
                .withLoginHint(null)
//                .withScopes(Arrays.asList(*scopes))
                .withCallback(authInteractiveCallback)
                .build()
            mSingleAccountApp!!.signIn(signInParameters)
        })
        binding?.btnLogin?.setOnClickListener(View.OnClickListener {
            if (mSingleAccountApp == null) {
                return@OnClickListener
            }

            /*
                     * Removes the signed-in account and cached tokens from this app (or device, if the device is in shared mode).
                     */mSingleAccountApp!!.signOut(object :
            ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                mAccount = null
                updateUI()
                val signOutText = "Signed Out."
               requireContext().showErrorMsg(signOutText)
            }

            override fun onError(exception: MsalException) {
                displayError(exception)
            }
        })
        })
    }
    private val authInteractiveCallback: AuthenticationCallback
        private get() = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                Log.d(TAG, "Successfully authenticated")
                Log.d(TAG, "ID Token: " + authenticationResult.account.claims!!["id_token"])

                /* Update account */mAccount = authenticationResult.account
                updateUI()

            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                Log.d(
                    TAG,
                    "Authentication failed: $exception"
                )
                displayError(exception)
                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }

            override fun onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.")
            }
        }
    /**
     * Display the error message
     */
    private fun displayError(exception: Exception) {
        requireContext().showErrorMsg(exception.toString())
    }
    companion object {
        private val TAG = LoginFragment::class.java.simpleName
    }
    /**
     * Updates UI based on the current account.
     */
    private fun updateUI() {
        if (mAccount != null) {
            binding?.btnMicLogin!!.isEnabled = false
            binding?.btnLogin!!.isEnabled = true
        } else {
            binding?.btnMicLogin!!!!.isEnabled = true
            binding?.btnLogin!!.isEnabled = false
        }

    }
}