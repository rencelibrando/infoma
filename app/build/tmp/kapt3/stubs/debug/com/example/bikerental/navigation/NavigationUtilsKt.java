package com.example.bikerental.navigation;

import android.util.Log;
import androidx.navigation.NavController;
import androidx.navigation.NavOptionsBuilder;
import androidx.navigation.NavHostController;
import androidx.compose.runtime.Composable;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u0016\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u001a\"\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0010\b\u0002\u0010\u0004\u001a\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\u0005H\u0007\u001a\n\u0010\u0006\u001a\u00020\u0001*\u00020\u0003\u001a\n\u0010\u0007\u001a\u00020\u0001*\u00020\u0003\u00a8\u0006\b"}, d2 = {"ProfileBackHandler", "", "navController", "Landroidx/navigation/NavController;", "onBackPressed", "Lkotlin/Function0;", "popBackToProfileTab", "setReturnToProfileTab", "app_debug"})
public final class NavigationUtilsKt {
    
    /**
     * Sets a flag to return to the Profile tab when navigating back
     * @param navController The navigation controller
     */
    public static final void setReturnToProfileTab(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController $this$setReturnToProfileTab) {
    }
    
    /**
     * Navigates back to the previous screen with a flag to return to the Profile tab
     * @param navController The navigation controller
     */
    public static final void popBackToProfileTab(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController $this$popBackToProfileTab) {
    }
    
    /**
     * Composable function that handles back button presses to return to the Profile tab
     * @param navController The navigation controller
     * @param onBackPressed Optional callback to execute additional logic on back press
     */
    @androidx.compose.runtime.Composable()
    public static final void ProfileBackHandler(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBackPressed) {
    }
}