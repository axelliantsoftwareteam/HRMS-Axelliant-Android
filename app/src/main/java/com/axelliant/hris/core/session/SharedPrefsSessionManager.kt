package com.axelliant.hris.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsSessionManager @Inject constructor(
    @ApplicationContext context: Context
) : SessionManager {
    private val prefs: SharedPreferences = createEncryptedPreferences(context.applicationContext)

    override fun saveSession(session: UserSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_EMAIL, session.email)
            .apply()
    }

    override fun saveMicrosoftGraphToken(token: String) {
        prefs.edit()
            .putString(KEY_MICROSOFT_GRAPH_TOKEN, token)
            .apply()
    }

    override fun clearMicrosoftGraphToken() {
        prefs.edit()
            .remove(KEY_MICROSOFT_GRAPH_TOKEN)
            .apply()
    }

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    override fun getMicrosoftGraphToken(): String? =
        prefs.getString(KEY_MICROSOFT_GRAPH_TOKEN, null)

    override fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    override fun clearSession() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREF_NAME = "internal_apps_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_EMAIL = "email"
        const val KEY_MICROSOFT_GRAPH_TOKEN = "microsoft_graph_token"

        fun createEncryptedPreferences(context: Context): SharedPreferences {
            return try {
                encryptedPreferences(context)
            } catch (exception: Exception) {
                if (!exception.isRecoverableEncryptedPrefsFailure()) throw exception

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
