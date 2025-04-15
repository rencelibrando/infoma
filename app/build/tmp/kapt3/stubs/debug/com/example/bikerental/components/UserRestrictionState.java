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

/**
 * Interface for user restriction state
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010$\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001R\u0012\u0010\u0002\u001a\u00020\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0002\u0010\u0004R\u0012\u0010\u0005\u001a\u00020\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0005\u0010\u0004R\u0012\u0010\u0006\u001a\u00020\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0004R\u0012\u0010\u0007\u001a\u00020\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\u0004R\u001e\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00030\tX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\u000bR\u0018\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\rX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000f\u0010\u0010R\u001e\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\n0\tX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\u000bR \u0010\u0013\u001a\u0010\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u0001\u0018\u00010\u0014X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0015\u0010\u0016\u00a8\u0006\u0017"}, d2 = {"Lcom/example/bikerental/components/UserRestrictionState;", "", "isEmailVerified", "", "()Z", "isLoading", "isPhoneVerified", "isProfileComplete", "isRestricted", "Lkotlin/Function1;", "", "()Lkotlin/jvm/functions/Function1;", "refreshUserData", "Lkotlin/Function0;", "", "getRefreshUserData", "()Lkotlin/jvm/functions/Function0;", "restrictionMessage", "getRestrictionMessage", "userData", "", "getUserData", "()Ljava/util/Map;", "app_debug"})
public abstract interface UserRestrictionState {
    
    public abstract boolean isLoading();
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.util.Map<java.lang.String, java.lang.Object> getUserData();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlin.jvm.functions.Function0<kotlin.Unit> getRefreshUserData();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlin.jvm.functions.Function1<java.lang.String, java.lang.Boolean> isRestricted();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlin.jvm.functions.Function1<java.lang.String, java.lang.String> getRestrictionMessage();
    
    public abstract boolean isProfileComplete();
    
    public abstract boolean isPhoneVerified();
    
    public abstract boolean isEmailVerified();
}