package com.myaunt.app.privacy

import android.content.Context

object PrivacyConsent {
    private const val PREFS_NAME = "privacy_consent"
    private const val KEY_ACCEPTED_VERSION = "accepted_policy_version"

    /** 更新隐私正文或条款时递增，已同意旧版的用户需重新确认 */
    const val CURRENT_POLICY_VERSION: Int = 1

    fun hasAccepted(context: Context): Boolean {
        val v = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_ACCEPTED_VERSION, 0)
        return v >= CURRENT_POLICY_VERSION
    }

    fun markAccepted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_ACCEPTED_VERSION, CURRENT_POLICY_VERSION)
            .apply()
    }
}
