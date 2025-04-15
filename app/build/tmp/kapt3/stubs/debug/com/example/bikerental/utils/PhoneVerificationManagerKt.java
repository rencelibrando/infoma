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

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\b\n\u0000\n\u0002\u0010\u000e\n\u0000\"\u000e\u0010\u0000\u001a\u00020\u0001X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0002"}, d2 = {"TAG", "", "app_debug"})
public final class PhoneVerificationManagerKt {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "PhoneVerification";
}