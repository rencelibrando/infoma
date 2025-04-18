package com.example.bikerental.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Manages map state persistence across app sessions using DataStore
 */
class MapPreferencesManager(private val context: Context) {

    // DataStore keys
    private object PreferencesKeys {
        val LAT = doublePreferencesKey("map_latitude")
        val LNG = doublePreferencesKey("map_longitude")
        val ZOOM = floatPreferencesKey("map_zoom")
        val TILT = floatPreferencesKey("map_tilt")
        val BEARING = floatPreferencesKey("map_bearing")
    }

    // Get saved camera position as flow
    fun getCameraPositionFlow(): Flow<CameraPosition?> = context.mapPrefs.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val lat = preferences[PreferencesKeys.LAT]
            val lng = preferences[PreferencesKeys.LNG]
            val zoom = preferences[PreferencesKeys.ZOOM]
            
            if (lat != null && lng != null && zoom != null) {
                val tilt = preferences[PreferencesKeys.TILT] ?: 0f
                val bearing = preferences[PreferencesKeys.BEARING] ?: 0f
                
                CameraPosition.builder()
                    .target(LatLng(lat, lng))
                    .zoom(zoom)
                    .tilt(tilt)
                    .bearing(bearing)
                    .build()
            } else {
                null
            }
        }

    // Save camera position
    suspend fun saveCameraPosition(position: CameraPosition) {
        context.mapPrefs.edit { preferences ->
            position.target?.let { target ->
                preferences[PreferencesKeys.LAT] = target.latitude
                preferences[PreferencesKeys.LNG] = target.longitude
            }
            preferences[PreferencesKeys.ZOOM] = position.zoom
            preferences[PreferencesKeys.TILT] = position.tilt
            preferences[PreferencesKeys.BEARING] = position.bearing
        }
    }

    companion object {
        @Volatile private var instance: MapPreferencesManager? = null
        
        fun getInstance(context: Context): MapPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: MapPreferencesManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
}

// Extension property for DataStore
private val Context.mapPrefs: DataStore<Preferences> by preferencesDataStore(name = "map_preferences") 