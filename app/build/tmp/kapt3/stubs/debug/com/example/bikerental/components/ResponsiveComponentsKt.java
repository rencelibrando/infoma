package com.example.bikerental.components;

import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.drawscope.Fill;
import androidx.compose.ui.graphics.drawscope.Stroke;
import androidx.compose.ui.text.input.VisualTransformation;
import androidx.compose.ui.text.font.FontWeight;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000>\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u001a\u0012\u0010\u0000\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u0007\u001a*\u0010\u0004\u001a\u00020\u00012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0007\u001a\u00020\bH\u0007\u001a<\u0010\t\u001a\u00020\u00012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\u0006\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\f\u001a\u00020\bH\u0007\u001a\u009b\u0001\u0010\r\u001a\u00020\u00012\u0006\u0010\u000e\u001a\u00020\u000b2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00010\u00102\u0006\u0010\u0011\u001a\u00020\u000b2\b\b\u0002\u0010\u0002\u001a\u00020\u00032\u0015\b\u0002\u0010\u0012\u001a\u000f\u0012\u0004\u0012\u00020\u0001\u0018\u00010\u0006\u00a2\u0006\u0002\b\u00132\u0015\b\u0002\u0010\u0014\u001a\u000f\u0012\u0004\u0012\u00020\u0001\u0018\u00010\u0006\u00a2\u0006\u0002\b\u00132\b\b\u0002\u0010\u0015\u001a\u00020\u00162\b\b\u0002\u0010\u0017\u001a\u00020\b2\n\b\u0002\u0010\u0018\u001a\u0004\u0018\u00010\u000b2\u0015\b\u0002\u0010\u0019\u001a\u000f\u0012\u0004\u0012\u00020\u0001\u0018\u00010\u0006\u00a2\u0006\u0002\b\u0013H\u0007\u00a8\u0006\u001a"}, d2 = {"GoogleLogo", "", "modifier", "Landroidx/compose/ui/Modifier;", "GoogleSignInButton", "onClick", "Lkotlin/Function0;", "isLoading", "", "ResponsiveButton", "text", "", "enabled", "ResponsiveTextField", "value", "onValueChange", "Lkotlin/Function1;", "label", "leadingIcon", "Landroidx/compose/runtime/Composable;", "trailingIcon", "visualTransformation", "Landroidx/compose/ui/text/input/VisualTransformation;", "isError", "errorMessage", "supportingText", "app_debug"})
public final class ResponsiveComponentsKt {
    
    /**
     * A responsive text field with consistent styling across the app
     */
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void ResponsiveTextField(@org.jetbrains.annotations.NotNull()
    java.lang.String value, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onValueChange, @org.jetbrains.annotations.NotNull()
    java.lang.String label, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function0<kotlin.Unit> leadingIcon, @org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function0<kotlin.Unit> trailingIcon, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.text.input.VisualTransformation visualTransformation, boolean isError, @org.jetbrains.annotations.Nullable()
    java.lang.String errorMessage, @org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function0<kotlin.Unit> supportingText) {
    }
    
    /**
     * A responsive button with consistent styling and loading state
     */
    @androidx.compose.runtime.Composable()
    public static final void ResponsiveButton(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick, @org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, boolean isLoading, boolean enabled) {
    }
    
    /**
     * Google logo composable that renders the multicolor "G" logo
     */
    @androidx.compose.runtime.Composable()
    public static final void GoogleLogo(@org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    /**
     * A Google sign-in button with consistent styling
     */
    @androidx.compose.runtime.Composable()
    public static final void GoogleSignInButton(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, boolean isLoading) {
    }
}