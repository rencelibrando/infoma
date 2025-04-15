package com.example.bikerental;

import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.compose.runtime.*;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import com.example.bikerental.viewmodels.AuthViewModel;
import com.example.bikerental.viewmodels.PhoneAuthViewModel;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.example.bikerental.navigation.Screen;
import androidx.lifecycle.ViewModelProvider;
import android.util.Log;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.navigation.NavHostController;
import com.example.bikerental.models.AuthState;
import java.util.logging.Level;
import java.util.logging.Logger;
import androidx.compose.ui.text.style.TextAlign;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.GoogleAuthProvider;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u001a\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a\b\u0010\u0000\u001a\u00020\u0001H\u0003\u001a$\u0010\u0002\u001a\u00020\u00012\u0012\u0010\u0003\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00010\u00042\u0006\u0010\u0006\u001a\u00020\u0007H\u0007\u00a8\u0006\b"}, d2 = {"LoadingScreen", "", "MainNavigation", "onLoginScreenChange", "Lkotlin/Function1;", "", "fusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "app_debug"})
public final class MainActivityKt {
    
    @androidx.compose.runtime.Composable()
    private static final void LoadingScreen() {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void MainNavigation(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onLoginScreenChange, @org.jetbrains.annotations.NotNull()
    com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient) {
    }
}