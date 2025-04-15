package com.example.bikerental.screens.profile;

import android.net.Uri;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.layout.ContentScale;
import androidx.navigation.NavController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.firestore.SetOptions;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.content.Context;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Build;
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions;
import com.example.bikerental.utils.ColorUtils;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000D\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a\u0010\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0007\u001a\u0010\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H\u0002\u001a\u0010\u0010\b\u001a\u00020\u00052\u0006\u0010\t\u001a\u00020\nH\u0002\u001a\u0086\u0001\u0010\u000b\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\r2\u0006\u0010\u0010\u001a\u00020\r2\b\u0010\u0011\u001a\u0004\u0018\u00010\r2\u0006\u0010\u0012\u001a\u00020\r2\u0006\u0010\u0013\u001a\u00020\r2\u0006\u0010\u0014\u001a\u00020\r2\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0002\u001a\u00020\u00032\u0014\b\u0002\u0010\u0019\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u00010\u001a2\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00010\u001cH\u0002\u00a8\u0006\u001d"}, d2 = {"EditProfileScreen", "", "navController", "Landroidx/navigation/NavController;", "hasRequiredPermissions", "", "context", "Landroid/content/Context;", "shouldShowPermissionRationale", "activity", "Landroid/app/Activity;", "updateUserProfile", "userId", "", "fullName", "phoneNumber", "email", "profilePictureUrl", "street", "city", "barangay", "db", "Lcom/google/firebase/firestore/FirebaseFirestore;", "currentUser", "Lcom/google/firebase/auth/FirebaseUser;", "onError", "Lkotlin/Function1;", "onComplete", "Lkotlin/Function0;", "app_debug"})
public final class EditProfileScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void EditProfileScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController) {
    }
    
    private static final void updateUserProfile(java.lang.String userId, java.lang.String fullName, java.lang.String phoneNumber, java.lang.String email, java.lang.String profilePictureUrl, java.lang.String street, java.lang.String city, java.lang.String barangay, com.google.firebase.firestore.FirebaseFirestore db, com.google.firebase.auth.FirebaseUser currentUser, androidx.navigation.NavController navController, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onError, kotlin.jvm.functions.Function0<kotlin.Unit> onComplete) {
    }
    
    private static final boolean hasRequiredPermissions(android.content.Context context) {
        return false;
    }
    
    private static final boolean shouldShowPermissionRationale(android.app.Activity activity) {
        return false;
    }
}