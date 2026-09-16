package com.axelliant.hris.features.agent.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentChatSessionManager @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSession(session: AgentChatSession) {
        prefs.edit()
            .putString(KEY_SESSION, gson.toJson(session))
            .apply()
    }

    fun getSession(): AgentChatSession? {
        val rawSession = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching {
            gson.fromJson(rawSession, AgentChatSession::class.java)
        }.getOrNull()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREF_NAME = "agent_chat_session"
        const val KEY_SESSION = "session"
    }
}
