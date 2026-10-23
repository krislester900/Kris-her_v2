package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * ChatApplication:
 * Application class ensuring reliable and safe initialization of Firebase
 * and unhandled exception safety to prevent application launch crashes on mobile devices.
 */
class ChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Set up safe exception logging to trace any unexpected runtime crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ChatApplication", "Unhandled exception in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize Firebase safely
        try {
            FirebaseApp.initializeApp(this)
            Log.i("ChatApplication", "FirebaseApp initialized successfully")
        } catch (e: Exception) {
            Log.e("ChatApplication", "FirebaseApp initialization non-blocking warning: ${e.message}", e)
        }
    }
}
