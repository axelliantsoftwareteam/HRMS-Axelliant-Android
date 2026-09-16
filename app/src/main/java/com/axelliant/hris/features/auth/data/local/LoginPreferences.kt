package com.axelliant.hris.features.auth.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginPreferences @Inject constructor(
    @ApplicationContext context: Context
) : LoginCredentialStore {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun getRememberedEmail(): String = prefs.getString(KEY_EMAIL, null).orEmpty()

    override fun getRememberedPassword(): String = prefs.getString(KEY_PASSWORD, null).orEmpty()

    override fun isRememberMeEnabled(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    override fun saveRememberedCredentials(email: String, password: String) {
        prefs.edit()
            .putBoolean(KEY_REMEMBER_ME, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    override fun clearRememberedCredentials() {
        prefs.edit()
            .putBoolean(KEY_REMEMBER_ME, false)
            .remove(KEY_EMAIL)
            .remove(KEY_PASSWORD)
            .apply()
    }

    private companion object {
        const val PREF_NAME = "login_preferences"
        const val KEY_EMAIL = "remembered_email"
        const val KEY_PASSWORD = "remembered_password"
        const val KEY_REMEMBER_ME = "remember_me"
    }
}

interface LoginCredentialStore {
    fun getRememberedEmail(): String
    fun getRememberedPassword(): String
    fun isRememberMeEnabled(): Boolean
    fun saveRememberedCredentials(email: String, password: String)
    fun clearRememberedCredentials()
}
