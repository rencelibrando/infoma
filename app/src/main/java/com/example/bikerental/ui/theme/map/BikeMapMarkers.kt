package com.example.bikerental.ui.theme.map
import com.google.android.gms.maps.model.LatLng
import com.example.bikerental.R
data class BikeMapMarker(
    val id: String,
    val name: String,
    val type: String,
    val price: String,
    val position: LatLng,
    val imageRes: Int,
    var distance: String = ""
)

data class BikeStation(
    val id: String,
    val name: String,
    val position: LatLng,
    val availableBikes: Int
)
val intramurosAvailableBikes = listOf(
    BikeMapMarker(
        id = "bike1",
        name = "Mountain Explorer",
        type = "Mountain Bike",
        price = "₱12/hr",
        position = LatLng(14.5895, 120.9742),
        imageRes = R.drawable.bambike
    ),
    BikeMapMarker(
        id = "bike2",
        name = "Road Master",
        type = "Road Bike",
        price = "₱10/hr",
        position = LatLng(14.5902, 120.9763),
        imageRes = R.drawable.bambike
    ),
    BikeMapMarker(
        id = "bike3",
        name = "City Cruiser",
        type = "Hybrid Bike",
        price = "₱8/hr",
        position = LatLng(14.5880, 120.9758),
        imageRes = R.drawable.bambike
    ),
    BikeMapMarker(
        id = "bike4",
        name = "Urban Commuter",
        type = "City Bike",
        price = "₱9/hr",
        position = LatLng(14.5910, 120.9732),
        imageRes = R.drawable.bambike
    )
)

// Define bike stations
val intramurosStations = listOf(
    BikeStation(
        id = "station1",
        name = "Fort Santiago Station",
        position = LatLng(14.5944, 120.9735),
        availableBikes = 8
    ),
    BikeStation(
        id = "station2",
        name = "Manila Cathedral Station",
        position = LatLng(14.5917, 120.9754),
        availableBikes = 5
    ),
    BikeStation(
        id = "station3",
        name = "San Agustin Church Station",
        position = LatLng(14.5892, 120.9769),
        availableBikes = 7
    )
)

// Define outline points for Intramuros area
val intramurosOutlinePoints = listOf(
    LatLng(14.5977, 120.9745),
    LatLng(14.5951, 120.9801),
    LatLng(14.5882, 120.9824),
    LatLng(14.5844, 120.9778),
    LatLng(14.5855, 120.9719),
    LatLng(14.5908, 120.9696),
    LatLng(14.5977, 120.9745)
)