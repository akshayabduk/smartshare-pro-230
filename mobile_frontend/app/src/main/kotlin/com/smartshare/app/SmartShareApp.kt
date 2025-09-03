package com.smartshare.app

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * PUBLIC_INTERFACE
 * SmartShareApp is the Application class that initializes global services.
 * - Initializes Firebase SDK.
 */
class SmartShareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase safely; app should still open if Firebase isn't configured.
        try {
            val app = FirebaseApp.initializeApp(this)
            if (app == null) {
                android.util.Log.w("SmartShareApp", "Firebase not configured (google-services.json missing). Continuing without Firebase.")
            }
        } catch (t: Throwable) {
            android.util.Log.e("SmartShareApp", "Firebase init failed: ${t.message}", t)
        }
    }
}
