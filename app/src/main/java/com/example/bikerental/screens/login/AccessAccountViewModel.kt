package com.example.bikerental.screens.login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AccessAccountViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    val currentUser = auth.currentUser

    fun signOut() {
        auth.signOut()
    }
} 