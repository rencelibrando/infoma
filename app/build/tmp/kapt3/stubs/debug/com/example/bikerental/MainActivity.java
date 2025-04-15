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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\r\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u0000 &2\u00020\u0001:\u0001&B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010!\u001a\u00020\"2\b\u0010#\u001a\u0004\u0018\u00010$H\u0014J\b\u0010%\u001a\u00020\"H\u0014R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R+\u0010\t\u001a\u00020\b2\u0006\u0010\u0007\u001a\u00020\b8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\r\u0010\u000e\u001a\u0004\b\t\u0010\n\"\u0004\b\u000b\u0010\fR+\u0010\u000f\u001a\u00020\b2\u0006\u0010\u0007\u001a\u00020\b8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0011\u0010\u000e\u001a\u0004\b\u000f\u0010\n\"\u0004\b\u0010\u0010\fR+\u0010\u0012\u001a\u00020\b2\u0006\u0010\u0007\u001a\u00020\b8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0014\u0010\u000e\u001a\u0004\b\u0012\u0010\n\"\u0004\b\u0013\u0010\fR\u000e\u0010\u0015\u001a\u00020\u0016X\u0082.\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0017\u001a\u00020\u00188BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001b\u0010\u001c\u001a\u0004\b\u0019\u0010\u001aR+\u0010\u001d\u001a\u00020\b2\u0006\u0010\u0007\u001a\u00020\b8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b \u0010\u000e\u001a\u0004\b\u001e\u0010\n\"\u0004\b\u001f\u0010\f\u00a8\u0006\'"}, d2 = {"Lcom/example/bikerental/MainActivity;", "Landroidx/activity/ComponentActivity;", "()V", "authStateListener", "Lcom/google/firebase/auth/FirebaseAuth$AuthStateListener;", "fusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "<set-?>", "", "isLoading", "()Z", "setLoading", "(Z)V", "isLoading$delegate", "Landroidx/compose/runtime/MutableState;", "isLoggedIn", "setLoggedIn", "isLoggedIn$delegate", "isLoginScreenActive", "setLoginScreenActive", "isLoginScreenActive$delegate", "locationCallback", "Lcom/google/android/gms/location/LocationCallback;", "phoneAuthViewModel", "Lcom/example/bikerental/viewmodels/PhoneAuthViewModel;", "getPhoneAuthViewModel", "()Lcom/example/bikerental/viewmodels/PhoneAuthViewModel;", "phoneAuthViewModel$delegate", "Lkotlin/Lazy;", "showSplash", "getShowSplash", "setShowSplash", "showSplash$delegate", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "Companion", "app_debug"})
@kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
public final class MainActivity extends androidx.activity.ComponentActivity {
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    private com.google.android.gms.location.LocationCallback locationCallback;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState isLoading$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy phoneAuthViewModel$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState isLoggedIn$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState isLoginScreenActive$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState showSplash$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private com.google.firebase.auth.FirebaseAuth.AuthStateListener authStateListener;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "MainActivity";
    @org.jetbrains.annotations.NotNull()
    public static final com.example.bikerental.MainActivity.Companion Companion = null;
    
    public MainActivity() {
        super();
    }
    
    private final boolean isLoading() {
        return false;
    }
    
    private final void setLoading(boolean p0) {
    }
    
    private final com.example.bikerental.viewmodels.PhoneAuthViewModel getPhoneAuthViewModel() {
        return null;
    }
    
    private final boolean isLoggedIn() {
        return false;
    }
    
    private final void setLoggedIn(boolean p0) {
    }
    
    private final boolean isLoginScreenActive() {
        return false;
    }
    
    private final void setLoginScreenActive(boolean p0) {
    }
    
    private final boolean getShowSplash() {
        return false;
    }
    
    private final void setShowSplash(boolean p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/example/bikerental/MainActivity$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}