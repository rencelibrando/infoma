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

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u0014\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a\u001a\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u0007\u00a8\u0006\u0006"}, d2 = {"AccessAccountScreen", "", "navController", "Landroidx/navigation/NavController;", "viewModel", "Lcom/example/bikerental/viewmodels/AuthViewModel;", "app_debug"})
public final class AccessAccountScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void AccessAccountScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.example.bikerental.viewmodels.AuthViewModel viewModel) {
    }
}