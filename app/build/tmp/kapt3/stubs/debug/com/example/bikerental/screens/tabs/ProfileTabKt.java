package com.example.bikerental.screens.tabs;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontWeight;
import androidx.navigation.NavController;
import com.example.bikerental.R;
import com.example.bikerental.viewmodels.AuthViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.app.Activity;
import com.example.bikerental.viewmodels.PhoneAuthViewModel;
import androidx.compose.ui.text.input.OffsetMapping;
import androidx.compose.ui.text.input.ImeAction;
import com.example.bikerental.models.PhoneAuthState;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.compose.ui.text.input.VisualTransformation;
import androidx.compose.ui.text.input.TransformedText;
import androidx.compose.material.ExperimentalMaterialApi;
import com.example.bikerental.utils.ColorUtils;
import android.util.Log;
import androidx.compose.ui.text.style.TextAlign;
import kotlinx.coroutines.Dispatchers;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.ui.unit.Dp;
import com.example.bikerental.navigation.Screen;
import android.widget.Toast;
import androidx.compose.ui.text.style.TextOverflow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000l\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0012\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\u001a$\u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a\u001e\u0010\u0005\u001a\u00020\u00012\u0006\u0010\u0006\u001a\u00020\u00072\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a*\u0010\b\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\u00072\u0006\u0010\n\u001a\u00020\u00072\u0006\u0010\u000b\u001a\u00020\fH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\r\u0010\u000e\u001a\b\u0010\u000f\u001a\u00020\u0001H\u0003\u001a$\u0010\u0010\u001a\u00020\u00012\u0006\u0010\u0011\u001a\u00020\u00072\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00010\u0013H\u0003\u001aX\u0010\u0014\u001a\u00020\u00012\u0006\u0010\u0011\u001a\u00020\u00072\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00182\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001a2\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001a2\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001aH\u0003\u001a\u0018\u0010\u001e\u001a\u00020\u00012\u0006\u0010\u001f\u001a\u00020 2\u0006\u0010\u0015\u001a\u00020!H\u0007\u001a&\u0010\"\u001a\u00020\u00012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u0006\u0010#\u001a\u00020$2\u0006\u0010\u0015\u001a\u00020\u0016H\u0003\u001a\u0016\u0010%\u001a\u00020\u00012\f\u0010&\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a,\u0010\'\u001a\u00020\u00012\u0006\u0010\u0011\u001a\u00020\u00072\f\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\f\u0010)\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a\u001a\u0010*\u001a\u00020\u00012\u0006\u0010\u000b\u001a\u00020\fH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b+\u0010,\u001aB\u0010-\u001a\u00020\u00012\u0006\u0010.\u001a\u00020\u00072\u0006\u0010/\u001a\u00020\u00072\u0006\u00100\u001a\u00020\u00072\u0006\u00101\u001a\u00020\u00072\u0006\u00102\u001a\u00020\u00072\u0006\u0010\u000b\u001a\u00020\fH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b3\u00104\u001aB\u00105\u001a\u00020\u00012\u0006\u00106\u001a\u0002072\u0006\u00108\u001a\u00020\u00072\f\u00109\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u0006\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010:\u001a\u00020;H\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b<\u0010=\u001a2\u0010>\u001a\u00020\u00012\u0006\u0010\u001f\u001a\u00020 2\u0006\u0010\u0015\u001a\u00020!2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010?\u001a\u00020@H\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\bA\u0010B\u001a\b\u0010C\u001a\u00020\u0001H\u0003\u001a\u000e\u0010D\u001a\u00020\u00072\u0006\u0010\u0011\u001a\u00020\u0007\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006E"}, d2 = {"AppIdentifierErrorSection", "", "onRetry", "Lkotlin/Function0;", "onDismiss", "ErrorSection", "errorMessage", "", "InfoRow", "label", "value", "purple200", "Landroidx/compose/ui/graphics/Color;", "InfoRow-mxwnekA", "(Ljava/lang/String;Ljava/lang/String;J)V", "LoadingSection", "PhoneNumberInputSection", "phoneNumber", "onPhoneNumberChange", "Lkotlin/Function1;", "PhoneVerificationDialog", "viewModel", "Lcom/example/bikerental/viewmodels/PhoneAuthViewModel;", "activity", "Landroid/app/Activity;", "verificationAttemptedRef", "Landroidx/compose/runtime/MutableState;", "", "showVerifyPhoneDialogRef", "actuallyShowVerifyDialogRef", "ProfileScreen", "navController", "Landroidx/navigation/NavController;", "Lcom/example/bikerental/viewmodels/AuthViewModel;", "RateLimitedSection", "uiState", "Lcom/example/bikerental/models/PhoneAuthState$RateLimited;", "RecaptchaErrorSection", "onBypass", "RecaptchaInfoDialog", "onContinue", "onBack", "RideHistoryContent", "RideHistoryContent-8_81llA", "(J)V", "RideHistoryItem", "bikeName", "date", "duration", "cost", "status", "RideHistoryItem-kKL39v8", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V", "SettingsButton", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "text", "onClick", "elevation", "Landroidx/compose/ui/unit/Dp;", "SettingsButton-1Yev-eo", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;Lkotlin/jvm/functions/Function0;JF)V", "SettingsContent", "coroutineScope", "Lkotlinx/coroutines/CoroutineScope;", "SettingsContent-9LQNqLg", "(Landroidx/navigation/NavController;Lcom/example/bikerental/viewmodels/AuthViewModel;JLkotlinx/coroutines/CoroutineScope;)V", "SuccessSection", "formatPhilippinePhoneNumber", "app_debug"})
public final class ProfileTabKt {
    
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String formatPhilippinePhoneNumber(@org.jetbrains.annotations.NotNull()
    java.lang.String phoneNumber) {
        return null;
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class, androidx.compose.material.ExperimentalMaterialApi.class})
    @androidx.compose.runtime.Composable()
    public static final void ProfileScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.bikerental.viewmodels.AuthViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void RateLimitedSection(kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, com.example.bikerental.models.PhoneAuthState.RateLimited uiState, com.example.bikerental.viewmodels.PhoneAuthViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AppIdentifierErrorSection(kotlin.jvm.functions.Function0<kotlin.Unit> onRetry, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void RecaptchaInfoDialog(java.lang.String phoneNumber, kotlin.jvm.functions.Function0<kotlin.Unit> onContinue, kotlin.jvm.functions.Function0<kotlin.Unit> onBack) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    private static final void PhoneNumberInputSection(java.lang.String phoneNumber, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onPhoneNumberChange) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void LoadingSection() {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SuccessSection() {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ErrorSection(java.lang.String errorMessage, kotlin.jvm.functions.Function0<kotlin.Unit> onRetry) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void RecaptchaErrorSection(kotlin.jvm.functions.Function0<kotlin.Unit> onBypass) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PhoneVerificationDialog(java.lang.String phoneNumber, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, com.example.bikerental.viewmodels.PhoneAuthViewModel viewModel, android.app.Activity activity, androidx.compose.runtime.MutableState<java.lang.Boolean> verificationAttemptedRef, androidx.compose.runtime.MutableState<java.lang.Boolean> showVerifyPhoneDialogRef, androidx.compose.runtime.MutableState<java.lang.Boolean> actuallyShowVerifyDialogRef) {
    }
}