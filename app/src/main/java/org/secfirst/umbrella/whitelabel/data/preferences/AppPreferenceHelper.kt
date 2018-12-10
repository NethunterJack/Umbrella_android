package org.secfirst.umbrella.whitelabel.data.preferences

import android.content.Context
import android.content.SharedPreferences
import org.secfirst.umbrella.whitelabel.di.PreferenceInfo
import javax.inject.Inject

class AppPreferenceHelper @Inject constructor(context: Context, @PreferenceInfo private val prefFileName: String) : PreferenceHelper {

    private val prefs: SharedPreferences = context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)

    companion object {
        private const val EXTRA_REFRESH_INTERVAL = "refresh_interval"
        private const val EXTRA_SKIP_PASSWORD = "skip_password"
        private const val EXTRA_STORE_FEED_SOURCES = "feed_sources"
        private const val EXTRA_STORE_LOCATION = "location"
        const val PREF_NAME = "umbrella.preference"
        const val EXTRA_LOGGED_IN = "is_logged_in"
    }

    override fun setFeedSources(feedSources: String) = prefs.edit().putString(EXTRA_STORE_FEED_SOURCES, feedSources).commit()

    override fun getFeedSources() = prefs.getString(EXTRA_STORE_FEED_SOURCES, "")

    override fun setLocation(locationInfo: String) = prefs.edit().putString(EXTRA_STORE_LOCATION, locationInfo).commit()

    override fun getLocation() = prefs.getString(EXTRA_STORE_LOCATION, "")

    override fun isLoggedIn() = prefs.edit().putBoolean(EXTRA_LOGGED_IN, false).commit()

    override fun setIsLoggedIn(isLoggedIn: Boolean) = prefs.edit().putBoolean(EXTRA_LOGGED_IN, isLoggedIn).commit()

    override fun setSkipPassword(isSkip: Boolean) = prefs.edit().putBoolean(EXTRA_SKIP_PASSWORD, isSkip).commit()

    override fun getSkipPassword() = prefs.getBoolean(EXTRA_SKIP_PASSWORD, false)

    override fun setRefreshInterval(position: Int) = prefs.edit().putInt(EXTRA_REFRESH_INTERVAL, position).commit()

    override fun getRefreshInterval() = prefs.getInt(EXTRA_REFRESH_INTERVAL, 0)

}

interface PreferenceHelper {

    fun isLoggedIn(): Boolean

    fun setIsLoggedIn(isLoggedIn: Boolean): Boolean

    fun setSkipPassword(isSkip: Boolean): Boolean

    fun getSkipPassword(): Boolean

    fun setRefreshInterval(position: Int): Boolean

    fun getRefreshInterval(): Int

    fun setFeedSources(feedSources: String): Boolean

    fun getFeedSources(): String

    fun setLocation(locationInfo: String): Boolean

    fun getLocation(): String
}