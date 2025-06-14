# Resolving Firebase App Check Issues

## Common Error Messages

### Error 1: Firebase: Error (auth/firebase-app-check-token-is-invalid.)
This error occurs when App Check is enabled but no valid token is present.

### Error 2: AppCheck: Fetch server returned an HTTP error status. HTTP status: 403. (appCheck/fetch-status-error)
This error occurs when your App Check token is being rejected by the server.

## Solutions

### Step 1: Verify reCAPTCHA Site Key

1. Go to Firebase Console > Project Settings > App Check
2. Check the reCAPTCHA v3 site key that's configured
3. Make sure it matches the one in your code: `6LfqjBsrAAAAAMs93cei_7rFTn2hXKLPvL-sEKFr`
4. If different, update your code to match the site key in Firebase Console

### Step 2: Register a Debug Token (For Development)

1. **Use the validation tool**
   - Open `validate-app-check.html` in your browser
   - Click "Generate Debug Token"
   - Look in your browser console (F12) for the debug token

2. **Register the debug token in Firebase Console**
   - Go to Firebase Console > Project Settings > App Check
   - Scroll down to "Debug tokens" section
   - Enter the debug token for your web app (from your browser console)
   - Make sure to select the correct app: "BambikeAdmin" (1:862099405823:web:6a4c5698adf458d62545ca)

3. **Test validation**
   - After registering the token, click "Validate App Check" in the validation tool
   - If successful, your app should now work with App Check

### Step 3: Debug the Admin Page

If you continue to experience issues with admin login, try the following:

1. **Verify App Check Initialization**
   - Make sure the App Check setup runs BEFORE any Firebase service is used
   - The updated `firebase.js` now handles this correctly

2. **Clear Browser Cache**
   - Clear browser cache and cookies
   - Try logging in again

3. **Temporarily Disable "Enforce All"**
   - As a last resort for debugging, temporarily disable "Enforce All" in Firebase Console > App Check
   - Test login functionality
   - Re-enable after confirming it works

## Using the App Check Validator Tool

We've added a special tool to help diagnose App Check issues:

1. Open `validate-app-check.html` in your browser
2. Use the buttons to:
   - Generate debug tokens for development
   - Validate your App Check configuration

This tool will help verify that your App Check setup is working correctly.

## Additional Resources
- [Firebase App Check Documentation](https://firebase.google.com/docs/app-check)
- [App Check Web Debug Provider](https://firebase.google.com/docs/app-check/web/debug-provider) 