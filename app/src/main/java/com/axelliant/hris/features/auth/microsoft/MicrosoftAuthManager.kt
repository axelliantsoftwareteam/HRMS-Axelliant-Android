package com.axelliant.hris.features.auth.microsoft

import android.app.Activity
import android.content.Context
import android.util.Log
import com.axelliant.hris.R
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class MicrosoftAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cachedApplication: ISingleAccountPublicClientApplication? = null

    suspend fun signIn(activity: Activity): MicrosoftAuthResult {
        val application = createApplication()
        return suspendCancellableCoroutine { continuation ->
            application.signIn(
                activity,
                null,
                MICROSOFT_SCOPES,
                object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        val idToken = authenticationResult.account.idToken
                        val accessToken = authenticationResult.accessToken
                        if (continuation.isActive) {
                            continuation.resume(
                                if (idToken.isNullOrBlank()) {
                                    MicrosoftAuthResult.Error("Microsoft sign-in did not return an ID token.")
                                } else {
                                    MicrosoftAuthResult.Success(
                                        idToken = idToken,
                                        accessToken = accessToken
                                    )
                                }
                            )
                        }
                    }

                    override fun onError(exception: MsalException) {
                        Log.e(TAG, "Microsoft sign-in failed.", exception)
                        if (continuation.isActive) {
                            continuation.resume(MicrosoftAuthResult.Error(exception.userSafeMessage()))
                        }
                    }

                    override fun onCancel() {
                        if (continuation.isActive) {
                            continuation.resume(MicrosoftAuthResult.Cancelled)
                        }
                    }
                }
            )
        }
    }

    suspend fun acquireGraphAccessToken(): String? {
        val application = createApplication()
        val account = loadCurrentAccount(application) ?: return null
        return suspendCancellableCoroutine { continuation ->
            application.acquireTokenSilentAsync(
                MICROSOFT_SCOPES,
                account.authority,
                object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        if (continuation.isActive) {
                            continuation.resume(authenticationResult.accessToken)
                        }
                    }

                    override fun onError(exception: MsalException) {
                        Log.e(TAG, "Microsoft silent token failed.", exception)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            )
        }
    }

    private suspend fun loadCurrentAccount(
        application: ISingleAccountPublicClientApplication
    ): IAccount? {
        return suspendCancellableCoroutine { continuation ->
            application.getCurrentAccountAsync(
                object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                    override fun onAccountLoaded(activeAccount: IAccount?) {
                        if (continuation.isActive) {
                            continuation.resume(activeAccount)
                        }
                    }

                    override fun onAccountChanged(
                        priorAccount: IAccount?,
                        currentAccount: IAccount?
                    ) = Unit

                    override fun onError(exception: MsalException) {
                        Log.e(TAG, "Microsoft current account load failed.", exception)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            )
        }
    }

    private suspend fun createApplication(): ISingleAccountPublicClientApplication {
        cachedApplication?.let { return it }
        return suspendCancellableCoroutine { continuation ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.auth_config_ciam_auth,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        cachedApplication = application
                        if (continuation.isActive) {
                            continuation.resume(application)
                        }
                    }

                    override fun onError(exception: MsalException) {
                        Log.e(TAG, "Microsoft auth application creation failed.", exception)
                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                    }
                }
            )
        }
    }

    private companion object {
        const val TAG = "MicrosoftAuthManager"
        val MICROSOFT_SCOPES = arrayOf("User.Read")
    }
}

sealed interface MicrosoftAuthResult {
    data class Success(
        val idToken: String,
        val accessToken: String?
    ) : MicrosoftAuthResult
    data object Cancelled : MicrosoftAuthResult
    data class Error(val message: String? = null) : MicrosoftAuthResult
}

private fun MsalException.userSafeMessage(): String {
    val details = listOfNotNull(
        errorCode,
        message,
        cause?.message
    ).joinToString(separator = " - ")

    return details.ifBlank { "Microsoft sign-in failed." }
}
