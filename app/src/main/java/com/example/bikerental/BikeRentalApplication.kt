package com.example.bikerental

import android.app.Application
import com.google.firebase.FirebaseApp

class BikeRentalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
} 