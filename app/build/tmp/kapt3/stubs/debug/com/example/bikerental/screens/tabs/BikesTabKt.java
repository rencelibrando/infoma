package com.example.bikerental.screens.tabs;

import androidx.compose.foundation.layout.*;
import androidx.compose.foundation.lazy.grid.GridCells;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextOverflow;
import androidx.navigation.NavController;
import coil.request.ImageRequest;
import com.example.bikerental.models.Bike;
import com.example.bikerental.utils.ColorUtils;
import com.example.bikerental.utils.LocationManager;
import com.example.bikerental.viewmodels.BikeViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.model.LatLng;
import kotlin.math.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000D\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0005\u001a\\\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\b\u0010\u0004\u001a\u0004\u0018\u00010\u00052\u0014\b\u0002\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u00072\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\n2\b\b\u0002\u0010\u000b\u001a\u00020\fH\u0007\u001a2\u0010\r\u001a\u00020\u00012\b\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00112\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\u0012\u001a\u00020\u0013H\u0007\u001a(\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00152\u0006\u0010\u0017\u001a\u00020\u00152\u0006\u0010\u0018\u001a\u00020\u00152\u0006\u0010\u0019\u001a\u00020\u0015H\u0002\u00a8\u0006\u001a"}, d2 = {"BikeCard", "", "bike", "Lcom/example/bikerental/models/Bike;", "currentLocation", "Lcom/google/android/gms/maps/model/LatLng;", "onBikeClick", "Lkotlin/Function1;", "onBook", "onCompleteProfile", "Lkotlin/Function0;", "modifier", "Landroidx/compose/ui/Modifier;", "BikesTab", "fusedLocationProviderClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "navController", "Landroidx/navigation/NavController;", "bikeViewModel", "Lcom/example/bikerental/viewmodels/BikeViewModel;", "calculateDistance", "", "lat1", "lon1", "lat2", "lon2", "app_debug"})
public final class BikesTabKt {
    
    @androidx.compose.runtime.Composable()
    public static final void BikesTab(@org.jetbrains.annotations.Nullable()
    com.google.android.gms.location.FusedLocationProviderClient fusedLocationProviderClient, @org.jetbrains.annotations.Nullable()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.NotNull()
    com.example.bikerental.viewmodels.BikeViewModel bikeViewModel) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void BikeCard(@org.jetbrains.annotations.NotNull()
    com.example.bikerental.models.Bike bike, @org.jetbrains.annotations.Nullable()
    com.google.android.gms.maps.model.LatLng currentLocation, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.bikerental.models.Bike, kotlin.Unit> onBikeClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.bikerental.models.Bike, kotlin.Unit> onBook, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onCompleteProfile, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    private static final double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        return 0.0;
    }
}