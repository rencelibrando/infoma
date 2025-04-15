package com.example.bikerental.navigation;

import android.util.Log;
import androidx.navigation.NavController;
import androidx.navigation.NavOptionsBuilder;
import androidx.navigation.NavHostController;
import androidx.compose.runtime.Composable;

/**
 * Utility class for centralized navigation handling throughout the app.
 * This provides consistent navigation patterns and eliminates hardcoded route strings.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u000f\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J3\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u001b\b\u0002\u0010\t\u001a\u0015\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u0004\u0018\u00010\n\u00a2\u0006\u0002\b\fJ\u0016\u0010\r\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\u000fJ\u0016\u0010\u0010\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u000fJ\u000e\u0010\u0012\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u0013\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u0014\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u001a\u0010\u0015\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\n\b\u0002\u0010\u0016\u001a\u0004\u0018\u00010\u000fJ\u001a\u0010\u0017\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\n\b\u0002\u0010\u0016\u001a\u0004\u0018\u00010\u000fJ\u000e\u0010\u0018\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u0019\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u001a\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u001b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u001a\u0010\u001c\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\n\b\u0002\u0010\u0016\u001a\u0004\u0018\u00010\u000fJ\u001a\u0010\u001d\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\n\b\u0002\u0010\u0016\u001a\u0004\u0018\u00010\u000f\u00a8\u0006\u001e"}, d2 = {"Lcom/example/bikerental/navigation/NavigationUtils;", "", "()V", "navigateTo", "", "navController", "Landroidx/navigation/NavController;", "screen", "Lcom/example/bikerental/navigation/Screen;", "builder", "Lkotlin/Function1;", "Landroidx/navigation/NavOptionsBuilder;", "Lkotlin/ExtensionFunctionType;", "navigateToBikeDetails", "bikeId", "", "navigateToBookingDetails", "bookingId", "navigateToBookings", "navigateToChangePassword", "navigateToEditProfile", "navigateToEmailVerification", "popUpRoute", "navigateToGoogleVerification", "navigateToHelp", "navigateToHome", "navigateToLogin", "navigateToProfile", "navigateToSignIn", "navigateToSignUp", "app_debug"})
public final class NavigationUtils {
    @org.jetbrains.annotations.NotNull()
    public static final com.example.bikerental.navigation.NavigationUtils INSTANCE = null;
    
    private NavigationUtils() {
        super();
    }
    
    /**
     * Navigate to a screen with options
     */
    public final void navigateTo(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.bikerental.navigation.Screen screen, @org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function1<? super androidx.navigation.NavOptionsBuilder, kotlin.Unit> builder) {
    }
    
    /**
     * Navigate to home screen and clear back stack
     */
    public final void navigateToHome(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController) {
    }
    
    /**
     * Navigate to the initial login screen and clear back stack
     */
    public final void navigateToLogin(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController) {
    }
    
    /**
     * Navigate to sign in screen with options
     */
    public final void navigateToSignIn(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.Nullable()
    java.lang.String popUpRoute) {
    }
    
    /**
     * Navigate to sign up screen with options
     */
    public final void navigateToSignUp(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.Nullable()
    java.lang.String popUpRoute) {
    }
    
    /**
     * Navigate to profile screen
     */
    public final void navigateToProfile(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController) {
    }
    
    /**
     * Navigate to bike details with the specified bike ID
     */
    public final void navigateToBikeDetails(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    java.lang.String bikeId) {
    }
    
    /**
     * Navigate to edit profile screen
     */
    public final void navigateToEditProfile(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController) {
    }
    
    /**
     * Navigate to change password screen
     */
    public final void navigateToChangePassword(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController) {
    }
    
    /**
     * Navigate to help screen
     */
    public final void navigateToHelp(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController) {
    }
    
    /**
     * Navigate to bookings screen
     */
    public final void navigateToBookings(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController) {
    }
    
    /**
     * Navigate to booking details with the specified booking ID
     */
    public final void navigateToBookingDetails(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    java.lang.String bookingId) {
    }
    
    /**
     * Navigate to Google verification screen
     */
    public final void navigateToGoogleVerification(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.Nullable()
    java.lang.String popUpRoute) {
    }
    
    /**
     * Navigate to Email verification screen
     */
    public final void navigateToEmailVerification(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.Nullable()
    java.lang.String popUpRoute) {
    }
}