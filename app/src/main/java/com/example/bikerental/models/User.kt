package com.example.bikerental.models

data class User(
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = 0,
    val profilePictureUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val givenName: String? = null,
    val familyName: String? = null,
    val displayName: String? = null,
    val lastSignInTime: Long = 0,
    val provider: String = "email", // "email" or "google"
    val googleId: String? = null
) 