package com.axelliant.hris.features.profiles.data.local

import android.content.Context
import com.axelliant.hris.features.profiles.data.remote.dto.MicrosoftProfileResponse
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileCacheStore @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun save(profile: MicrosoftProfileResponse) {
        prefs.edit()
            .putString(KEY_PROFILE, gson.toJson(profile))
            .apply()
    }

    fun get(): MicrosoftProfileResponse? {
        val cachedProfile = prefs.getString(KEY_PROFILE, null).orEmpty()
        if (cachedProfile.isBlank()) return null
        return runCatching {
            gson.fromJson(cachedProfile, MicrosoftProfileResponse::class.java)
        }.getOrNull()
    }

    private companion object {
        const val PREF_NAME = "profile_cache"
        const val KEY_PROFILE = "microsoft_profile"
    }
}
