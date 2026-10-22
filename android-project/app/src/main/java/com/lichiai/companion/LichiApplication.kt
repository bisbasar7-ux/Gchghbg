package com.lichiai.companion

import android.app.Application
import com.lichiai.companion.data.PreferencesManager

/**
 * LichiApplication initializes app-wide singletons and logging policies.
 * No sensitive API keys or user credentials are stored in the client code.
 */
class LichiApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
    }

    companion object {
        lateinit var instance: LichiApplication
            private set
    }
}
