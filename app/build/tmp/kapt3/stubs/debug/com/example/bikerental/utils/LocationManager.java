package com.example.bikerental.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import androidx.compose.runtime.MutableState;
import androidx.core.app.ActivityCompat;
import com.example.bikerental.models.BikeLocation;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.model.LatLng;
import kotlinx.coroutines.flow.Flow;

/**
 * Centralized location manager for tracking user location across the app
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u0000 #2\u00020\u0001:\u0001#B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00072\u0006\u0010\u0015\u001a\u00020\u0007J\u0015\u0010\u0016\u001a\u0004\u0018\u00010\u00132\u0006\u0010\u0017\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\u0018J*\u0010\u0019\u001a\u00020\u001a2\u0012\u0010\u001b\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u001a0\u001c2\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001a0\u001eH\u0007J\u0006\u0010\u001f\u001a\u00020\tJ\u000e\u0010 \u001a\b\u0012\u0004\u0012\u00020\"0!H\u0007R\u0016\u0010\u0005\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\n\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\t0\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\fR\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006$"}, d2 = {"Lcom/example/bikerental/utils/LocationManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_currentLocation", "Landroidx/compose/runtime/MutableState;", "Lcom/google/android/gms/maps/model/LatLng;", "_isTracking", "", "currentLocation", "getCurrentLocation", "()Landroidx/compose/runtime/MutableState;", "fusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "isTracking", "locationRequest", "Lcom/google/android/gms/location/LocationRequest;", "calculateDistance", "", "start", "end", "distanceToCurrentLocation", "point", "(Lcom/google/android/gms/maps/model/LatLng;)Ljava/lang/Float;", "getLastLocation", "", "onSuccess", "Lkotlin/Function1;", "onFailure", "Lkotlin/Function0;", "hasLocationPermission", "locationFlow", "Lkotlinx/coroutines/flow/Flow;", "Lcom/example/bikerental/models/BikeLocation;", "Companion", "app_debug"})
public final class LocationManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.android.gms.location.LocationRequest locationRequest = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState<com.google.android.gms.maps.model.LatLng> _currentLocation = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState<com.google.android.gms.maps.model.LatLng> currentLocation = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState<java.lang.Boolean> _isTracking = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState<java.lang.Boolean> isTracking = null;
    public static final long UPDATE_INTERVAL = 5000L;
    public static final long FASTEST_INTERVAL = 2000L;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile com.example.bikerental.utils.LocationManager instance;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.bikerental.utils.LocationManager.Companion Companion = null;
    
    public LocationManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.MutableState<com.google.android.gms.maps.model.LatLng> getCurrentLocation() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.MutableState<java.lang.Boolean> isTracking() {
        return null;
    }
    
    /**
     * Gets the last known location if permissions are granted
     */
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    public final void getLastLocation(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.google.android.gms.maps.model.LatLng, kotlin.Unit> onSuccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onFailure) {
    }
    
    /**
     * Creates a flow of location updates
     */
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<com.example.bikerental.models.BikeLocation> locationFlow() {
        return null;
    }
    
    /**
     * Calculate distance between two points
     */
    public final float calculateDistance(@org.jetbrains.annotations.NotNull()
    com.google.android.gms.maps.model.LatLng start, @org.jetbrains.annotations.NotNull()
    com.google.android.gms.maps.model.LatLng end) {
        return 0.0F;
    }
    
    /**
     * Calculate distance between current location and a point
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Float distanceToCurrentLocation(@org.jetbrains.annotations.NotNull()
    com.google.android.gms.maps.model.LatLng point) {
        return null;
    }
    
    /**
     * Checks if location permissions are granted
     */
    public final boolean hasLocationPermission() {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\b\u001a\u00020\u00072\u0006\u0010\t\u001a\u00020\nR\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/example/bikerental/utils/LocationManager$Companion;", "", "()V", "FASTEST_INTERVAL", "", "UPDATE_INTERVAL", "instance", "Lcom/example/bikerental/utils/LocationManager;", "getInstance", "context", "Landroid/content/Context;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.bikerental.utils.LocationManager getInstance(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
    }
}