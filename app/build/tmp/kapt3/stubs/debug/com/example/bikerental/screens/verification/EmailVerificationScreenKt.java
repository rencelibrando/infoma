package com.example.bikerental.screens.verification;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.navigation.NavController;
import com.example.bikerental.models.AuthState;
import com.example.bikerental.navigation.NavigationUtils;
import com.example.bikerental.viewmodels.AuthViewModel;
import android.widget.Toast;
import android.util.Log;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import com.example.bikerental.BikeRentalApplication;
import com.example.bikerental.navigation.Screen;
import com.google.firebase.auth.FirebaseAuth;
import androidx.compose.ui.text.input.KeyboardType;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a@\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001aK\u0010\t\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\b\u0010\u0011\u001a\u0004\u0018\u00010\rH\u0003\u00a2\u0006\u0002\u0010\u0012\u001a\u001a\u0010\u0013\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\u0014\u001a\u00020\u0015H\u0007\u001a\u0010\u0010\u0016\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000bH\u0003\u00a8\u0006\u0017"}, d2 = {"EmailInputDialog", "", "emailInput", "", "onEmailChange", "Lkotlin/Function1;", "onDismiss", "Lkotlin/Function0;", "onConfirm", "EmailVerificationControls", "navController", "Landroidx/navigation/NavController;", "isLoading", "", "canResendEmail", "onCheckVerification", "onResendEmail", "emailVerified", "(Landroidx/navigation/NavController;ZZLkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Ljava/lang/Boolean;)V", "EmailVerificationScreen", "viewModel", "Lcom/example/bikerental/viewmodels/AuthViewModel;", "VerifiedSuccessCard", "app_debug"})
public final class EmailVerificationScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void EmailVerificationScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.bikerental.viewmodels.AuthViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void EmailVerificationControls(androidx.navigation.NavController navController, boolean isLoading, boolean canResendEmail, kotlin.jvm.functions.Function0<kotlin.Unit> onCheckVerification, kotlin.jvm.functions.Function0<kotlin.Unit> onResendEmail, java.lang.Boolean emailVerified) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void VerifiedSuccessCard(androidx.navigation.NavController navController) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void EmailInputDialog(java.lang.String emailInput, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onEmailChange, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function0<kotlin.Unit> onConfirm) {
    }
}