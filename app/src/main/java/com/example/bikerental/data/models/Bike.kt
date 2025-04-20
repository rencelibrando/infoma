package com.example.bikerental.data.models

data class Bike(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val hourlyRate: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isAvailable: Boolean = true,
    val imageUrl: String = ""
) 