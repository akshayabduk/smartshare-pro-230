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
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}
