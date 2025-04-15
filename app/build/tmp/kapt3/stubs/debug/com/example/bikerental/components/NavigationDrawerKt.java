package com.example.bikerental.components;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.ButtonDefaults;
import androidx.compose.material3.DrawerState;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.material3.NavigationDrawerItemDefaults;
import androidx.compose.material3.TopAppBarDefaults;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.graphics.vector.ImageVector;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.navigation.NavController;
import coil.request.CachePolicy;
import coil.request.ImageRequest;
import com.example.bikerental.R;
import com.example.bikerental.utils.ColorUtils;
import com.example.bikerental.viewmodels.AuthViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000`\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0010\u000b\n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\b\u001a<\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\nH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u000b\u0010\f\u001ae\u0010\r\u001a\u00020\u00012\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00010\u00132\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u00152\u0011\u0010\u0016\u001a\r\u0012\u0004\u0012\u00020\u00010\u0015\u00a2\u0006\u0002\b\u00172\n\b\u0002\u0010\u0018\u001a\u0004\u0018\u00010\u00192\n\b\u0002\u0010\u001a\u001a\u0004\u0018\u00010\u001bH\u0007\u001a\u001a\u0010\u001c\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\bH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u001d\u0010\u001e\u001aH\u0010\u001f\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010 \u001a\u00020\u00052\u0006\u0010!\u001a\u00020\u00052\f\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00010\u00152\u0006\u0010#\u001a\u00020\b2\u0006\u0010$\u001a\u00020\bH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b%\u0010&\u001aT\u0010\'\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010 \u001a\u00020\u00052\u0006\u0010(\u001a\u00020)2\f\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00010\u00152\u0006\u0010$\u001a\u00020\b2\b\b\u0002\u0010*\u001a\u00020)2\b\b\u0002\u0010+\u001a\u00020\u0011H\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b,\u0010-\u001aH\u0010.\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010 \u001a\u00020\u00052\u0006\u0010/\u001a\u00020\u00052\f\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00010\u00152\u0006\u0010#\u001a\u00020\b2\u0006\u0010$\u001a\u00020\bH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b0\u0010&\u001a\"\u00101\u001a\u00020\u00012\u0006\u0010 \u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\bH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b2\u00103\u001aD\u00104\u001a\u00020\u00012\b\u00105\u001a\u0004\u0018\u0001062\u0006\u00107\u001a\u00020\u00052\u0006\u00108\u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\b2\u000e\b\u0002\u00109\u001a\b\u0012\u0004\u0012\u00020\u00010\u0015H\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b:\u0010;\u001a\u001a\u0010<\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\bH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b=\u0010\u001e\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006>"}, d2 = {"AnimatedStat", "", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "value", "", "label", "greenColor", "Landroidx/compose/ui/graphics/Color;", "modifier", "Landroidx/compose/ui/Modifier;", "AnimatedStat-42QJj7c", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;Ljava/lang/String;JLandroidx/compose/ui/Modifier;)V", "AppNavigationDrawer", "drawerState", "Landroidx/compose/material3/DrawerState;", "selectedItem", "", "onItemSelected", "Lkotlin/Function1;", "openDrawer", "Lkotlin/Function0;", "content", "Landroidx/compose/runtime/Composable;", "navController", "Landroidx/navigation/NavController;", "viewModel", "Lcom/example/bikerental/viewmodels/AuthViewModel;", "BrandFooter", "BrandFooter-8_81llA", "(J)V", "CompactProfileWarningCard", "title", "restrictionType", "onClick", "warningColor", "accentColor", "CompactProfileWarningCard-VT9Kpxs", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;JJ)V", "EnhancedDrawerMenuItem", "selected", "", "isSignOut", "badgeCount", "EnhancedDrawerMenuItem-qFjXxE8", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;JZI)V", "ProfileWarningCard", "message", "ProfileWarningCard-VT9Kpxs", "SectionHeader", "SectionHeader-4WTKRHQ", "(Ljava/lang/String;J)V", "UserProfileHeader", "profilePictureUrl", "Landroid/net/Uri;", "userName", "userEmail", "onManageAccountClick", "UserProfileHeader-42QJj7c", "(Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;JLkotlin/jvm/functions/Function0;)V", "UserStats", "UserStats-8_81llA", "app_debug"})
public final class NavigationDrawerKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void AppNavigationDrawer(@org.jetbrains.annotations.NotNull()
    androidx.compose.material3.DrawerState drawerState, int selectedItem, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> onItemSelected, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> openDrawer, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> content, @org.jetbrains.annotations.Nullable()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.Nullable()
    com.example.bikerental.viewmodels.AuthViewModel viewModel) {
    }
}