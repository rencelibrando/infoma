package com.example.bikerental.screens.tabs;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresPermission;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.core.content.ContextCompat;
import com.example.bikerental.ui.theme.BikeLocation;
import com.example.bikerental.ui.theme.RouteInfo;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.compose.*;
import kotlinx.coroutines.Dispatchers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONObject;
import com.google.android.gms.maps.model.Dot;
import androidx.compose.ui.layout.ContentScale;
import com.example.bikerental.utils.ColorUtils;
import com.google.android.gms.maps.model.Gap;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000(\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\u001a\"\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u0006\u0010\u0007\u001a\u0012\u0010\b\u001a\u00020\u00012\b\u0010\t\u001a\u0004\u0018\u00010\nH\u0007\u001a&\u0010\u000b\u001a\u00020\u00012\b\u0010\t\u001a\u0004\u0018\u00010\n2\u0012\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u00010\rH\u0003\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u000f"}, d2 = {"MapLegendItem", "", "color", "Landroidx/compose/ui/graphics/Color;", "label", "", "MapLegendItem-DxMtmZc", "(JLjava/lang/String;)V", "MapTab", "fusedLocationProviderClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "getCurrentLocation", "onLocationReceived", "Lkotlin/Function1;", "Lcom/google/android/gms/maps/model/LatLng;", "app_debug"})
public final class MapTabKt {
    
    @androidx.compose.runtime.Composable()
    public static final void MapTab(@org.jetbrains.annotations.Nullable()
    com.google.android.gms.location.FusedLocationProviderClient fusedLocationProviderClient) {
    }
    
    @androidx.annotation.RequiresPermission(allOf = {"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"})
    private static final void getCurrentLocation(com.google.android.gms.location.FusedLocationProviderClient fusedLocationProviderClient, kotlin.jvm.functions.Function1<? super com.google.android.gms.maps.model.LatLng, kotlin.Unit> onLocationReceived) {
    }
}