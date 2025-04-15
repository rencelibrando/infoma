package com.example.bikerental.utils;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;
import com.example.bikerental.models.PhoneAuthState;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.firestore.FirebaseFirestore;
import kotlinx.coroutines.Dispatchers;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u0013\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\u0002\u0010\u0005J\u0010\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u0002J\u0010\u0010\u0018\u001a\u00020\u000b2\u0006\u0010\u0019\u001a\u00020\u000bH\u0002J\u0006\u0010\u001a\u001a\u00020\u0015J(\u0010\u001b\u001a\u00020\u00152\u0006\u0010\u0019\u001a\u00020\u000b2\u0006\u0010\u0016\u001a\u00020\u00172\b\b\u0002\u0010\u001c\u001a\u00020\u000bH\u0086@\u00a2\u0006\u0002\u0010\u001dJ\u0010\u0010\u001e\u001a\u00020\u00152\u0006\u0010\u001f\u001a\u00020 H\u0002J\b\u0010!\u001a\u00020\u0015H\u0002J\u0010\u0010\"\u001a\u00020\u00132\u0006\u0010\u0019\u001a\u00020\u000bH\u0002J\u000e\u0010#\u001a\u00020\u00152\u0006\u0010$\u001a\u00020\u000bR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0011\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006%"}, d2 = {"Lcom/example/bikerental/utils/PhoneVerificationManager;", "", "uiStateFlow", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/example/bikerental/models/PhoneAuthState;", "(Lkotlinx/coroutines/flow/MutableStateFlow;)V", "LOCAL_RETRY_COOLDOWN_MS", "", "auth", "Lcom/google/firebase/auth/FirebaseAuth;", "browserInfo", "", "callbacks", "Lcom/google/firebase/auth/PhoneAuthProvider$OnVerificationStateChangedCallbacks;", "lastAttemptTimestamp", "resendToken", "Lcom/google/firebase/auth/PhoneAuthProvider$ForceResendingToken;", "storedVerificationId", "verificationInProgress", "", "detectBrowserInfo", "", "activity", "Landroid/app/Activity;", "formatPhoneNumber", "phoneNumber", "reset", "startVerification", "senderName", "(Ljava/lang/String;Landroid/app/Activity;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updatePhoneCredential", "credential", "Lcom/google/firebase/auth/PhoneAuthCredential;", "updateUserProfile", "validatePhoneNumber", "verifyCode", "code", "app_debug"})
public final class PhoneVerificationManager {
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.bikerental.models.PhoneAuthState> uiStateFlow = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.auth.FirebaseAuth auth = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String storedVerificationId;
    @org.jetbrains.annotations.Nullable()
    private com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken resendToken;
    private boolean verificationInProgress = false;
    private long lastAttemptTimestamp = 0L;
    private final long LOCAL_RETRY_COOLDOWN_MS = 5000L;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String browserInfo = "Unknown";
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks = null;
    
    public PhoneVerificationManager(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.flow.MutableStateFlow<com.example.bikerental.models.PhoneAuthState> uiStateFlow) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object startVerification(@org.jetbrains.annotations.NotNull()
    java.lang.String phoneNumber, @org.jetbrains.annotations.NotNull()
    android.app.Activity activity, @org.jetbrains.annotations.NotNull()
    java.lang.String senderName, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void detectBrowserInfo(android.app.Activity activity) {
    }
    
    private final java.lang.String formatPhoneNumber(java.lang.String phoneNumber) {
        return null;
    }
    
    public final void verifyCode(@org.jetbrains.annotations.NotNull()
    java.lang.String code) {
    }
    
    private final void updatePhoneCredential(com.google.firebase.auth.PhoneAuthCredential credential) {
    }
    
    private final void updateUserProfile() {
    }
    
    private final boolean validatePhoneNumber(java.lang.String phoneNumber) {
        return false;
    }
    
    public final void reset() {
    }
}