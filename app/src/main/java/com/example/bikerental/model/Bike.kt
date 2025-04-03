package com.example.bikerental.model

import com.google.android.gms.maps.model.LatLng

data class Bike(
    val id: String,
    val name: String,
    val type: String,
    val price: Double,
    val imageUrl: String,
    val location: LatLng
) 