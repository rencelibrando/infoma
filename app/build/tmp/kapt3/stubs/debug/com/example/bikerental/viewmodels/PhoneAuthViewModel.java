package com.example.bikerental.viewmodels;

import android.app.Activity;
import androidx.lifecycle.ViewModel;
import com.example.bikerental.models.PhoneAuthState;
import com.example.bikerental.utils.PhoneVerificationManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import kotlinx.coroutines.flow.StateFlow;
import com.google.firebase.appcheck.FirebaseAppCheck;
import android.util.Log;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\f\u001a\u00020\rJ\u0006\u0010\u000e\u001a\u00020\rJ\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00120\u0010J\u0006\u0010\u0013\u001a\u00020\rJ\u001e\u0010\u0014\u001a\u00020\r2\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010\u0019J\u0016\u0010\u001a\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\u00122\u0006\u0010\u001c\u001a\u00020\u0016J \u0010\u001d\u001a\u00020\r2\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00182\b\b\u0002\u0010\u001e\u001a\u00020\u0016J\u0010\u0010\u001f\u001a\u00020\r2\b\u0010 \u001a\u0004\u0018\u00010!J\u000e\u0010\"\u001a\u00020\r2\u0006\u0010#\u001a\u00020\u0016R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006$"}, d2 = {"Lcom/example/bikerental/viewmodels/PhoneAuthViewModel;", "Landroidx/lifecycle/ViewModel;", "()V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/example/bikerental/models/PhoneAuthState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "verificationManager", "Lcom/example/bikerental/utils/PhoneVerificationManager;", "checkAndCleanupExpiredRateLimits", "", "checkIfDeviceBlockExpired", "checkRateLimitStatus", "Lkotlin/Pair;", "", "", "resetState", "retryWithoutRecaptcha", "phoneNumber", "", "activity", "Landroid/app/Activity;", "(Ljava/lang/String;Landroid/app/Activity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setRateLimited", "expiryTime", "displayDuration", "startPhoneNumberVerification", "senderName", "updateAuthState", "user", "Lcom/google/firebase/auth/FirebaseUser;", "verifyPhoneNumberWithCode", "code", "app_debug"})
public final class PhoneAuthViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.bikerental.models.PhoneAuthState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.bikerental.models.PhoneAuthState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.bikerental.utils.PhoneVerificationManager verificationManager = null;
    
    public PhoneAuthViewModel() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.bikerental.models.PhoneAuthState> getUiState() {
        return null;
    }
    
    public final void startPhoneNumberVerification(@org.jetbrains.annotations.NotNull()
    java.lang.String phoneNumber, @org.jetbrains.annotations.NotNull()
    android.app.Activity activity, @org.jetbrains.annotations.NotNull()
    java.lang.String senderName) {
    }
    
    public final void verifyPhoneNumberWithCode(@org.jetbrains.annotations.NotNull()
    java.lang.String code) {
    }
    
    public final void checkAndCleanupExpiredRateLimits() {
    }
    
    public final void checkIfDeviceBlockExpired() {
    }
    
    public final void setRateLimited(long expiryTime, @org.jetbrains.annotations.NotNull()
    java.lang.String displayDuration) {
    }
    
    /**
     * Checks if the user is currently rate limited for phone verification
     * @return Pair<Boolean, Long> where first is whether user is rate limited and second is expiry time
     */
    @org.jetbrains.annotations.NotNull()
    public final kotlin.Pair<java.lang.Boolean, java.lang.Long> checkRateLimitStatus() {
        return null;
    }
    
    /**
     * Resets the phone auth state to Initial
     */
    public final void resetState() {
    }
    
    public final void updateAuthState(@org.jetbrains.annotations.Nullable()
    com.google.firebase.auth.FirebaseUser user) {
    }
    
    /**
     * Retry verification without reCAPTCHA when the normal flow fails
     * @param phoneNumber The phone number to verify
     * @param activity The activity context
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object retryWithoutRecaptcha(@org.jetbrains.annotations.NotNull()
    java.lang.String phoneNumber, @org.jetbrains.annotations.NotNull()
    android.app.Activity activity, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}