package com.example.bikerental.components;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.vector.ImageVector;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import com.example.bikerental.utils.ProfileRestrictionUtils;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00004\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\u001aH\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u000bH\u0007\u001aS\u0010\f\u001a\u00020\u00012\u0006\u0010\u0004\u001a\u00020\u00032\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\b\b\u0002\u0010\b\u001a\u00020\t2\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0011\u0010\u0010\u001a\r\u0012\u0004\u0012\u00020\u00010\u0006\u00a2\u0006\u0002\b\u0011H\u0007\u001a2\u0010\u0012\u001a\u00020\u00012\u0006\u0010\u0013\u001a\u00020\u00032\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\b\b\u0002\u0010\u0015\u001a\u00020\u00032\b\b\u0002\u0010\u0016\u001a\u00020\u000bH\u0007\u001a\u0010\u0010\u0017\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0007\u00a8\u0006\u0018"}, d2 = {"RestrictedButton", "", "text", "", "featureType", "onClick", "Lkotlin/Function0;", "onCompleteProfile", "modifier", "Landroidx/compose/ui/Modifier;", "enabled", "", "RestrictedFeature", "customMessage", "customIcon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "content", "Landroidx/compose/runtime/Composable;", "RestrictedFeatureMessage", "message", "onButtonClick", "buttonText", "isLoading", "VerificationBenefit", "app_debug"})
public final class RestrictedFeatureKt {
    
    /**
     * A wrapper component that conditionally displays content based on profile completion restrictions.
     * If the feature is restricted, it shows a message explaining why and a button to complete the profile.
     *
     * @param featureType The type of feature being restricted (booking, payment, rental, etc.)
     * @param onCompleteProfile Action to navigate to profile completion screen
     * @param content The content to display if the feature is not restricted
     */
    @androidx.compose.runtime.Composable()
    public static final void RestrictedFeature(@org.jetbrains.annotations.NotNull()
    java.lang.String featureType, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onCompleteProfile, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.Nullable()
    java.lang.String customMessage, @org.jetbrains.annotations.Nullable()
    androidx.compose.ui.graphics.vector.ImageVector customIcon, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> content) {
    }
    
    /**
     * A composable that renders a message and button when a feature is restricted
     * based on profile completion or verification status
     */
    @androidx.compose.runtime.Composable()
    public static final void RestrictedFeatureMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onButtonClick, @org.jetbrains.annotations.NotNull()
    java.lang.String buttonText, boolean isLoading) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void VerificationBenefit(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
    }
    
    /**
     * A simpler version that can be applied to individual UI elements like buttons
     */
    @androidx.compose.runtime.Composable()
    public static final void RestrictedButton(@org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    java.lang.String featureType, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onCompleteProfile, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, boolean enabled) {
    }
}