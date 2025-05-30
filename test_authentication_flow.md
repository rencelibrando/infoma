# Authentication Flow Debug Test

## Problem Analysis

Based on your logs, there are two main issues:
1. **ANR (Application Not Responsive)**: 8+ second startup blocking
2. **Authentication State Issue**: Logged-in users still see login page

## Debug Commands

### 1. Clear Logcat and Start Fresh Test
```bash
# Clear all logs
adb logcat -c

# Start monitoring ALL relevant logs
adb logcat | grep -E "(PerformanceMonitor|AuthViewModel|MainActivity|BikeRentalApp|ANR_LOG)"
```

### 2. Test Logged-In User Flow
```bash
# In a separate terminal, force stop and restart the app
adb shell am force-stop com.example.bikerental
adb shell am start -n com.example.bikerental/.MainActivity
```

## Expected Debug Output

### ✅ **Successful Authentication Flow**
You should see this sequence in the logs:
```
BikeRentalApp: Application onCreate - starting initialization
BikeRentalApp: Firebase app initialized
AuthViewModel: AuthViewModel init - Firebase user: [user_id]
AuthViewModel: Found existing user ([user_id]), checking authentication status...
PerformanceMonitor: Started timer for: auth_initialization
AuthViewModel: initializeExistingUser: Starting for user [user_id]
AuthViewModel: initializeExistingUser: Reloading Firebase user...
AuthViewModel: initializeExistingUser: Firebase user reloaded successfully
AuthViewModel: initializeExistingUser: Fetching user document from Firestore...
AuthViewModel: initializeExistingUser: User document exists: true
AuthViewModel: initializeExistingUser: Parsed user data: [user_id]
AuthViewModel: initializeExistingUser: Email verified - Firebase: true, Google: [true/false], User doc: true
AuthViewModel: User authenticated successfully: [user_id]
PerformanceMonitor: Completed auth_initialization in XXXms
MainActivity: Navigation check - showSplash: false, authState: Authenticated([user])
MainActivity: User authenticated, navigating to Home
```

### ⚠️ **Problem Indicators**

**If you see ANR:**
```
ANR_LOG: Blocked msg = { ... } , cost = XXXX ms
```

**If authentication fails:**
```
AuthViewModel: Authentication initialization timed out after 5s
AuthViewModel: Setting auth state to Initial due to timeout
MainActivity: User not authenticated (state: Initial), navigating to initial login
```

**If stuck in Loading state:**
```
AuthViewModel: Found existing user ([user_id]), checking authentication status...
MainActivity: Auth state is Loading, waiting for resolution...
(No further auth updates after this)
```

## Diagnostic Steps

### Step 1: Check if Firebase User Exists
Look for this line:
```
AuthViewModel: AuthViewModel init - Firebase user: [user_id or null]
```

- **If null**: User is not logged in to Firebase (normal to see login page)
- **If user_id**: User should be authenticated, investigate further

### Step 2: Check Authentication Timeout
Look for:
```
AuthViewModel: Authentication initialization timed out after 5s
```

- **If present**: Firestore is taking too long or network issues
- **If absent**: Authentication is processing normally

### Step 3: Check Navigation Logic
Look for:
```
MainActivity: Navigation check - showSplash: false, authState: [state]
```

- **If authState is Loading**: Authentication is still processing
- **If authState is Initial**: Authentication failed or timed out
- **If authState is Authenticated**: Should navigate to Home

### Step 4: Check for ANR Patterns
Look for:
```
ANR_LOG: Blocked msg = { when=-8s226ms what=110 ...
```

- **what=110**: Application binding blocked
- **cost > 5000ms**: Indicates serious blocking issue

## Quick Fixes to Try

### Fix 1: If Authentication Times Out
The user document fetch might be slow. Try:
1. Check your internet connection
2. Check Firebase Firestore rules
3. Try signing out and back in: Firebase Auth → Sign Out → Sign In again

### Fix 2: If ANR Persists
The Firebase Session Lifecycle Service might be blocking. Try:
1. Force close the app completely
2. Clear app data: `adb shell pm clear com.example.bikerental`
3. Restart the app with monitoring

### Fix 3: If Still Stuck in Loading
```bash
# Check if there are any Firebase errors
adb logcat | grep -E "(Firebase|Firestore|AUTH)" | grep -i error
```

## Advanced Debugging

### Enable Firebase Debug Logging
Add this to test Firebase issues:
```bash
adb shell setprop log.tag.FirebaseAuth DEBUG
adb shell setprop log.tag.FirebaseFirestore DEBUG
```

### Monitor Memory Usage
```bash
# Check if memory issues are causing ANR
adb shell dumpsys meminfo com.example.bikerental
```

### Monitor Network Activity
```bash
# Check if network calls are blocking startup
adb logcat | grep -E "(HTTP|Network|Connection)"
```

## Test Scenarios

### Scenario A: First Launch After Install
1. Install app fresh
2. Create account and verify email
3. Force close app
4. Restart app
5. **Expected**: Should go directly to Home screen

### Scenario B: Existing User
1. User already logged in and verified
2. Force close app
3. Restart app
4. **Expected**: Should go to Home screen within 3 seconds

### Scenario C: Network Issues
1. Turn off WiFi/data
2. Launch app
3. **Expected**: Should timeout gracefully and show login screen

## Success Criteria

✅ **Authentication working correctly:**
- Firebase user detected immediately
- Firestore document retrieved within 2 seconds
- Navigation to Home screen within 3 seconds total
- No ANR logs
- PerformanceMonitor shows EXCELLENT/GOOD ratings

❌ **Authentication failing:**
- Timeouts after 5 seconds
- ANR logs with 8+ second blocking
- Stuck in Loading state
- Always navigating to login screen for logged-in users

## Emergency Workaround

If authentication is completely broken, you can temporarily bypass it by adding this to AuthViewModel init:

```kotlin
// TEMPORARY DEBUG: Force authentication for testing
if (currentFirebaseUser != null) {
    viewModelScope.launch {
        delay(1000) // Brief delay
        _authState.value = AuthState.Authenticated(
            User(
                id = currentFirebaseUser.uid,
                email = currentFirebaseUser.email ?: "",
                fullName = currentFirebaseUser.displayName ?: "",
                isEmailVerified = true
            )
        )
    }
}
```

This will help determine if the issue is with navigation logic vs. authentication logic. 