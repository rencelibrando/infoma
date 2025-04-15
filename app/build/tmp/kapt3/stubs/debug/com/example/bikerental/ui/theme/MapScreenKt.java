package com.example.bikerental.ui.theme;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import androidx.compose.foundation.layout.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Modifier;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.compose.*;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.example.bikerental.R;
import com.example.bikerental.models.TrackableBike;
import com.example.bikerental.ui.theme.map.BikeMapMarker;
import com.example.bikerental.viewmodels.BikeViewModel;
import java.util.Locale;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000$\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a<\u0010\u0000\u001a\u00020\u00012\b\u0010\u0002\u001a\u0004\u0018\u00010\u00032\u0014\b\u0002\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00052\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\nH\u0007\u00a8\u0006\u000b"}, d2 = {"BikeMap", "", "fusedLocationProviderClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "onBikeSelected", "Lkotlin/Function1;", "Lcom/example/bikerental/ui/theme/map/BikeMapMarker;", "requestLocationUpdate", "", "bikeViewModel", "Lcom/example/bikerental/viewmodels/BikeViewModel;", "app_debug"})
public final class MapScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void BikeMap(@org.jetbrains.annotations.Nullable()
    com.google.android.gms.location.FusedLocationProviderClient fusedLocationProviderClient, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.bikerental.ui.theme.map.BikeMapMarker, kotlin.Unit> onBikeSelected, boolean requestLocationUpdate, @org.jetbrains.annotations.NotNull()
    com.example.bikerental.viewmodels.BikeViewModel bikeViewModel) {
    }
}