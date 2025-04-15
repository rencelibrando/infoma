package com.example.bikerental.utils;

import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\b"}, d2 = {"Lcom/example/bikerental/utils/ConnectionUtils;", "", "()V", "isGpsEnabled", "", "context", "Landroid/content/Context;", "isInternetAvailable", "app_debug"})
public final class ConnectionUtils {
    @org.jetbrains.annotations.NotNull()
    public static final com.example.bikerental.utils.ConnectionUtils INSTANCE = null;
    
    private ConnectionUtils() {
        super();
    }
    
    public final boolean isInternetAvailable(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    public final boolean isGpsEnabled(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
}