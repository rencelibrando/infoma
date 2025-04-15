package com.example.bikerental.components;

import android.app.Activity;
import android.os.Build;
import android.webkit.WebView;
import androidx.compose.foundation.layout.*;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.KeyboardType;
import com.example.bikerental.R;
import com.example.bikerental.models.PhoneAuthState;
import com.example.bikerental.viewmodels.PhoneAuthViewModel;
import java.text.SimpleDateFormat;
import java.util.*;
import android.util.Log;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000B\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0003\u001a\u0010\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0003\u001aP\u0010\u0004\u001a\u00020\u00012\u0006\u0010\u0005\u001a\u00020\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u00072\u0006\u0010\b\u001a\u00020\u00032\u0006\u0010\t\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\f\u001a\u00020\r2\b\b\u0002\u0010\u000e\u001a\u00020\u000fH\u0007\u001a*\u0010\u0010\u001a\u00020\u00012\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00010\u00122\b\b\u0002\u0010\f\u001a\u00020\r2\b\b\u0002\u0010\u0013\u001a\u00020\u0014H\u0007\u001a \u0010\u0015\u001a\u00020\u00012\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u00122\b\b\u0002\u0010\u0017\u001a\u00020\u000fH\u0003\u001a@\u0010\u0018\u001a\u00020\u00012\u0006\u0010\u0019\u001a\u00020\u00032\u0006\u0010\u001a\u001a\u00020\u00032\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u000f2\f\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00010\u00122\b\b\u0002\u0010\f\u001a\u00020\rH\u0003\u00a8\u0006\u001f"}, d2 = {"BulletPoint", "", "text", "", "OtpInputField", "value", "onValueChange", "Lkotlin/Function1;", "label", "placeholder", "keyboardOptions", "Landroidx/compose/foundation/text/KeyboardOptions;", "modifier", "Landroidx/compose/ui/Modifier;", "singleLine", "", "PhoneVerification", "onVerificationComplete", "Lkotlin/Function0;", "viewModel", "Lcom/example/bikerental/viewmodels/PhoneAuthViewModel;", "RecaptchaTroubleshootingCard", "onRetry", "showFullOptions", "VerificationMethodOption", "title", "subtitle", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "selected", "onClick", "app_debug"})
public final class PhoneVerificationComponentKt {
    
    /**
     * A reusable phone verification component with robust reCAPTCHA handling
     * for different devices and browsers
     */
    @androidx.compose.runtime.Composable()
    public static final void PhoneVerification(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onVerificationComplete, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.NotNull()
    com.example.bikerental.viewmodels.PhoneAuthViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void VerificationMethodOption(java.lang.String title, java.lang.String subtitle, androidx.compose.ui.graphics.vector.ImageVector icon, boolean selected, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void BulletPoint(java.lang.String text) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void RecaptchaTroubleshootingCard(kotlin.jvm.functions.Function0<kotlin.Unit> onRetry, boolean showFullOptions) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void OtpInputField(@org.jetbrains.annotations.NotNull()
    java.lang.String value, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onValueChange, @org.jetbrains.annotations.NotNull()
    java.lang.String label, @org.jetbrains.annotations.NotNull()
    java.lang.String placeholder, @org.jetbrains.annotations.NotNull()
    androidx.compose.foundation.text.KeyboardOptions keyboardOptions, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, boolean singleLine) {
    }
}