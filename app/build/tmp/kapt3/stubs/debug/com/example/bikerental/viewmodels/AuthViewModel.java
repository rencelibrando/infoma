package com.example.bikerental.viewmodels;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.example.bikerental.BikeRentalApplication;
import com.example.bikerental.R;
import com.example.bikerental.models.AuthState;
import com.example.bikerental.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import kotlinx.coroutines.flow.StateFlow;
import java.util.UUID;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000v\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0018\u0002\n\u0002\b\u0015\u0018\u00002\u00020\u0001B\u0011\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u0004J8\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020$2\u0006\u0010%\u001a\u00020$2\f\u0010&\u001a\b\u0012\u0004\u0012\u00020\"0\'2\u0012\u0010(\u001a\u000e\u0012\u0004\u0012\u00020$\u0012\u0004\u0012\u00020\"0)J\u0006\u0010*\u001a\u00020\"J&\u0010+\u001a\u00020\"2\u0006\u0010,\u001a\u00020$2\u0006\u0010-\u001a\u00020$2\u0006\u0010.\u001a\u00020$2\u0006\u0010/\u001a\u00020$J(\u00100\u001a\u00020\"2\f\u0010&\u001a\b\u0012\u0004\u0012\u00020\"0\'2\u0012\u0010(\u001a\u000e\u0012\u0004\u0012\u00020$\u0012\u0004\u0012\u00020\"0)J\u0010\u00101\u001a\u0004\u0018\u00010$H\u0086@\u00a2\u0006\u0002\u00102J\u000e\u00103\u001a\u00020\"2\u0006\u00104\u001a\u00020$J\u000e\u00105\u001a\u00020\u001e2\u0006\u00106\u001a\u000207J*\u00108\u001a\u00020\"2\u0006\u00104\u001a\u00020$2\b\u00109\u001a\u0004\u0018\u00010$2\b\u0010,\u001a\u0004\u0018\u00010$2\u0006\u00106\u001a\u000207J\u000e\u0010:\u001a\u00020\"2\u0006\u00106\u001a\u000207J\b\u0010;\u001a\u00020\"H\u0014J\u000e\u0010<\u001a\u00020\"H\u0082@\u00a2\u0006\u0002\u00102J\u0006\u0010=\u001a\u00020\"J\u000e\u0010>\u001a\u00020\"2\u0006\u0010,\u001a\u00020$J\u0016\u0010?\u001a\u00020\"2\u0006\u0010,\u001a\u00020$H\u0082@\u00a2\u0006\u0002\u0010@J\u000e\u0010A\u001a\u00020\"2\u0006\u0010B\u001a\u00020\tJ\u0016\u0010C\u001a\u00020\"2\u0006\u0010,\u001a\u00020$2\u0006\u0010-\u001a\u00020$J\u0006\u0010D\u001a\u00020\"J&\u0010E\u001a\u00020\"2\u0006\u0010,\u001a\u00020$2\u0006\u0010-\u001a\u00020$2\u0006\u0010.\u001a\u00020$2\u0006\u0010F\u001a\u00020$J\u000e\u0010G\u001a\u00020\"2\u0006\u0010H\u001a\u00020\tJ8\u0010I\u001a\u00020\"2\u0006\u0010.\u001a\u00020$2\u0006\u0010F\u001a\u00020$2\f\u0010&\u001a\b\u0012\u0004\u0012\u00020\"0\'2\u0012\u0010(\u001a\u000e\u0012\u0004\u0012\u00020$\u0012\u0004\u0012\u00020\"0)J\u000e\u0010J\u001a\u00020\"2\u0006\u0010K\u001a\u00020$R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\n\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00070\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0017\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\t0\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0014R\u0019\u0010\u0017\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0014R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u001b\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0014R\u0010\u0010\u001d\u001a\u0004\u0018\u00010\u001eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020 X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006L"}, d2 = {"Lcom/example/bikerental/viewmodels/AuthViewModel;", "Landroidx/lifecycle/ViewModel;", "application", "Landroid/app/Application;", "(Landroid/app/Application;)V", "_authState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/example/bikerental/models/AuthState;", "_bypassVerification", "", "_currentUser", "Lcom/example/bikerental/models/User;", "_emailVerified", "activeSignInJob", "Lkotlinx/coroutines/Job;", "auth", "Lcom/google/firebase/auth/FirebaseAuth;", "authState", "Lkotlinx/coroutines/flow/StateFlow;", "getAuthState", "()Lkotlinx/coroutines/flow/StateFlow;", "bypassVerification", "getBypassVerification", "currentUser", "getCurrentUser", "db", "Lcom/google/firebase/firestore/FirebaseFirestore;", "emailVerified", "getEmailVerified", "googleSignInClient", "Lcom/google/android/gms/auth/api/signin/GoogleSignInClient;", "viewModelJob", "Lkotlinx/coroutines/CompletableJob;", "changePassword", "", "currentPassword", "", "newPassword", "onSuccess", "Lkotlin/Function0;", "onError", "Lkotlin/Function1;", "checkEmailVerification", "createUserWithEmailPassword", "email", "password", "fullName", "phone", "deleteAccount", "ensureUserEmail", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "firebaseAuthWithGoogle", "idToken", "getGoogleSignInClient", "context", "Landroid/content/Context;", "handleGoogleSignInResult", "displayName", "initializeGoogleSignIn", "onCleared", "refreshCurrentUser", "resendEmailVerification", "resetPassword", "sendAppSpecificVerificationEmail", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setBypassVerification", "bypass", "signInWithEmailPassword", "signOut", "signUpWithEmailPassword", "phoneNumber", "updateEmailVerificationStatus", "isVerified", "updateUserProfile", "verifyEmail", "token", "app_debug"})
public final class AuthViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.auth.FirebaseAuth auth = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.firestore.FirebaseFirestore db = null;
    @org.jetbrains.annotations.Nullable()
    private com.google.android.gms.auth.api.signin.GoogleSignInClient googleSignInClient;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CompletableJob viewModelJob = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.bikerental.models.AuthState> _authState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.bikerental.models.AuthState> authState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.bikerental.models.User> _currentUser = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.bikerental.models.User> currentUser = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _emailVerified = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> emailVerified = null;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job activeSignInJob;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _bypassVerification = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> bypassVerification = null;
    
    public AuthViewModel(@org.jetbrains.annotations.Nullable()
    android.app.Application application) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.bikerental.models.AuthState> getAuthState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.bikerental.models.User> getCurrentUser() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getEmailVerified() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getBypassVerification() {
        return null;
    }
    
    /**
     * Initialize Google Sign-In
     */
    public final void initializeGoogleSignIn(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    /**
     * Get the Google Sign-In client for starting the sign-in flow
     */
    @org.jetbrains.annotations.NotNull()
    public final com.google.android.gms.auth.api.signin.GoogleSignInClient getGoogleSignInClient(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    /**
     * Handle Google Sign-In result with improved error handling and debugging
     */
    public final void handleGoogleSignInResult(@org.jetbrains.annotations.NotNull()
    java.lang.String idToken, @org.jetbrains.annotations.Nullable()
    java.lang.String displayName, @org.jetbrains.annotations.Nullable()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    /**
     * Send app-specific verification email (Placeholder - requires backend/functions)
     */
    private final java.lang.Object sendAppSpecificVerificationEmail(java.lang.String email, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Verify email with token (Placeholder - relies on token mechanism)
     */
    public final void verifyEmail(@org.jetbrains.annotations.NotNull()
    java.lang.String token) {
    }
    
    /**
     * Ensures user has an email in Firebase Auth and potentially Firestore.
     * Prioritizes Firebase Auth email. Updates Firebase Auth if only found locally.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object ensureUserEmail(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    /**
     * Check if the current user's email is verified in Firebase Auth.
     * Updates local state and Firestore if necessary.
     */
    public final void checkEmailVerification() {
    }
    
    /**
     * Manually update email verification status (e.g., for Google sign-in).
     */
    public final void updateEmailVerificationStatus(boolean isVerified) {
    }
    
    /**
     * Resend Firebase Auth email verification link.
     */
    public final void resendEmailVerification() {
    }
    
    /**
     * Sign in with email and password.
     */
    public final void signInWithEmailPassword(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
    }
    
    /**
     * Create a new user account with email/password. Sends verification email.
     * Sets state to NeedsEmailVerification.
     */
    public final void createUserWithEmailPassword(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    java.lang.String fullName, @org.jetbrains.annotations.NotNull()
    java.lang.String phone) {
    }
    
    /**
     * Sign out the current user from Firebase and Google.
     */
    public final void signOut() {
    }
    
    /**
     * Send a password reset email to the user.
     */
    public final void resetPassword(@org.jetbrains.annotations.NotNull()
    java.lang.String email) {
    }
    
    /**
     * Update user profile information in Firestore.
     */
    public final void updateUserProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String fullName, @org.jetbrains.annotations.NotNull()
    java.lang.String phoneNumber, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSuccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onError) {
    }
    
    /**
     * Change the user's password (for email/password auth only).
     */
    public final void changePassword(@org.jetbrains.annotations.NotNull()
    java.lang.String currentPassword, @org.jetbrains.annotations.NotNull()
    java.lang.String newPassword, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSuccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onError) {
    }
    
    /**
     * Delete the user's account from Firebase Auth and Firestore.
     */
    public final void deleteAccount(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSuccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onError) {
    }
    
    /**
     * Sign up with email/password (wrapper for createUserWithEmailPassword).
     * Kept for potential compatibility if called elsewhere.
     */
    public final void signUpWithEmailPassword(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    java.lang.String fullName, @org.jetbrains.annotations.NotNull()
    java.lang.String phoneNumber) {
    }
    
    /**
     * Set whether to bypass email verification (e.g., for testing).
     */
    public final void setBypassVerification(boolean bypass) {
    }
    
    /**
     * Refresh current user data from Firestore. Updates _currentUser.
     */
    private final java.lang.Object refreshCurrentUser(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Firebase authentication using a Google ID token (e.g., from one-tap sign-in).
     * This is similar to handleGoogleSignInResult but takes only the token.
     */
    public final void firebaseAuthWithGoogle(@org.jetbrains.annotations.NotNull()
    java.lang.String idToken) {
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
    
    public AuthViewModel() {
        super();
    }
}