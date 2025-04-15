package com.example.bikerental.components;

import android.content.Intent;
import android.provider.Settings;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material3.ButtonDefaults;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import com.example.bikerental.utils.ConnectionUtils;
import androidx.compose.material.icons.Icons;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000,\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\t\n\u0002\u0018\u0002\n\u0000\u001a\u0016\u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0007\u001a\u0016\u0010\u0004\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0007\u001a&\u0010\u0005\u001a\u00020\u00012\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a4\u0010\u000b\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a&\u0010\u0011\u001a\u00020\u00012\u0006\u0010\u0012\u001a\u00020\u00072\u0006\u0010\u0013\u001a\u00020\u00072\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0007\u001a\u001b\u0010\u0015\u001a\u00020\u00012\u0011\u0010\u0016\u001a\r\u0012\u0004\u0012\u00020\u00010\u0003\u00a2\u0006\u0002\b\u0017H\u0007\u00a8\u0006\u0018"}, d2 = {"NoGpsAlert", "", "onDismiss", "Lkotlin/Function0;", "NoInternetAlert", "RequirementButton", "text", "", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "onClick", "RequirementsBlocker", "hasInternet", "", "hasGps", "onInternetClick", "onGpsClick", "RequirementsNotMetBanner", "message", "actionLabel", "onActionClick", "RequirementsWrapper", "content", "Landroidx/compose/runtime/Composable;", "app_debug"})
public final class RequirementAlertsKt {
    
    @androidx.compose.runtime.Composable()
    public static final void RequirementsWrapper(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> content) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void RequirementsBlocker(boolean hasInternet, boolean hasGps, kotlin.jvm.functions.Function0<kotlin.Unit> onInternetClick, kotlin.jvm.functions.Function0<kotlin.Unit> onGpsClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void RequirementButton(java.lang.String text, androidx.compose.ui.graphics.vector.ImageVector icon, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void NoInternetAlert(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void NoGpsAlert(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void RequirementsNotMetBanner(@org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.NotNull()
    java.lang.String actionLabel, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onActionClick) {
    }
}