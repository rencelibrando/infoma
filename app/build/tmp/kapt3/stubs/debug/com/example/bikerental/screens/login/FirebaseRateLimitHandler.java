package com.example.bikerental.screens.login;

import android.app.Activity;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.PasswordVisualTransformation;
import androidx.compose.ui.text.input.VisualTransformation;
import androidx.navigation.NavController;
import com.example.bikerental.models.AuthState;
import com.example.bikerental.navigation.Screen;
import com.example.bikerental.viewmodels.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.api.ApiException;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.example.bikerental.navigation.NavigationUtils;
import com.google.firebase.FirebaseException;
import kotlinx.coroutines.Dispatchers;
import com.google.firebase.auth.FirebaseAuth;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J \u0010\t\u001a\u00020\n2\n\u0010\u000b\u001a\u00060\fj\u0002`\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\n0\u000fR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/example/bikerental/screens/login/FirebaseRateLimitHandler;", "", "()V", "TAG", "", "backoffMultiplier", "", "lastBackoffTime", "", "handleException", "", "e", "Ljava/lang/Exception;", "Lkotlin/Exception;", "onRetryCallback", "Lkotlin/Function0;", "app_debug"})
public final class FirebaseRateLimitHandler {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "FirebaseRateLimit";
    private static long lastBackoffTime = 0L;
    private static int backoffMultiplier = 1;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.bikerental.screens.login.FirebaseRateLimitHandler INSTANCE = null;
    
    private FirebaseRateLimitHandler() {
        super();
    }
    
    public final void handleException(@org.jetbrains.annotations.NotNull()
    java.lang.Exception e, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRetryCallback) {
    }
}