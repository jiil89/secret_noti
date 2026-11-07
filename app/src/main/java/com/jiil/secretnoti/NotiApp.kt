package com.jiil.secretnoti

import android.app.Application
import com.jiil.secretnoti.data.AppDatabase
import com.jiil.secretnoti.settings.Prefs

class NotiApp : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.init(this)
        db = AppDatabase.create(this)
    }

    companion object {
        lateinit var instance: NotiApp
            private set
    }
}
