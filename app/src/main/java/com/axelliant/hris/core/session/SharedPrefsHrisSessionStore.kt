package com.axelliant.hris.core.session

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.axelliant.hris.core.contracts.session.AppSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsHrisSessionStore @Inject constructor(
    @ApplicationContext context: Context
) : HrisSessionStore {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = createEncryptedPreferences(appContext)

    init {
        migrateLegacySessionIfNeeded()
    }

    override fun hasValidSession(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    override fun currentSession(): AppSession? {
        if (!hasValidSession()) return null
        return AppSession(
            email = prefs.getString(KEY_EMAIL, null),
            displayName = prefs.getString(KEY_DISPLAY_NAME, null),
            accessToken = rawToken()
        )
    }

    override fun saveMicrosoftSession(email: String?, accessToken: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USERNAME, email)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
    }

    override fun rawToken(): String {
        return prefs.getString(KEY_ACCESS_TOKEN, null).orEmpty()
    }

    override fun saveToken(token: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    override fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun migrateLegacySessionIfNeeded() {
        if (prefs.contains(KEY_IS_LOGGED_IN) || prefs.contains(KEY_ACCESS_TOKEN)) return

        val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE)
        if (!legacyPrefs.getBoolean(LEGACY_KEY_IS_LOGGED_IN, false)) return

        val token = legacyPrefs.getString(LEGACY_KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
            ?: return

        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_EMAIL, legacyPrefs.getString(LEGACY_KEY_EMAIL, null))
            .putString(KEY_DISPLAY_NAME, legacyPrefs.getString(LEGACY_KEY_FIRST_NAME, null))
            .putString(KEY_USERNAME, legacyPrefs.getString(LEGACY_KEY_USERNAME, null))
            .apply()
    }

    private companion object {
        const val PREF_NAME = "hris_session"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_USERNAME = "username"

        const val LEGACY_PREF_NAME = "UserPref"
        const val LEGACY_KEY_IS_LOGGED_IN = "IsLoggedIn"
        const val LEGACY_KEY_TOKEN = "token"
        const val LEGACY_KEY_EMAIL = "email"
        const val LEGACY_KEY_FIRST_NAME = "firstname"
        const val LEGACY_KEY_USERNAME = "username"

        @SuppressLint("ApplySharedPref")
        fun createEncryptedPreferences(context: Context): SharedPreferences {
            return try {
                encryptedPreferences(context)
            } catch (exception: Exception) {
                if (!exception.isRecoverableEncryptedPrefsFailure()) throw exception

                // This recovery path must clear synchronously before retrying encrypted prefs.
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
                encryptedPreferences(context)
            }
        }

        fun encryptedPreferences(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        fun Exception.isRecoverableEncryptedPrefsFailure(): Boolean {
            var cause: Throwable? = this
            while (cause != null) {
                if (cause is AEADBadTagException) return true
                cause = cause.cause
            }
            return false
        }
    }
}
