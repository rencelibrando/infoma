package com.example.bikerental.screens.admin;

import android.net.Uri;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.foundation.layout.*;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.navigation.NavController;
import com.example.bikerental.utils.LocationManager;
import com.example.bikerental.viewmodels.BikeViewModel;
import com.google.android.gms.maps.model.LatLng;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000.\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a\u001a\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u0007\u001a4\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\t2\b\u0010\f\u001a\u0004\u0018\u00010\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0002\u00a8\u0006\u0010"}, d2 = {"BikeUploadScreen", "", "navController", "Landroidx/navigation/NavController;", "bikeViewModel", "Lcom/example/bikerental/viewmodels/BikeViewModel;", "validateInputs", "", "name", "", "type", "price", "imageUri", "Landroid/net/Uri;", "location", "Lcom/google/android/gms/maps/model/LatLng;", "app_debug"})
public final class BikeUploadScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void BikeUploadScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.bikerental.viewmodels.BikeViewModel bikeViewModel) {
    }
    
    private static final boolean validateInputs(java.lang.String name, java.lang.String type, java.lang.String price, android.net.Uri imageUri, com.google.android.gms.maps.model.LatLng location) {
        return false;
    }
}