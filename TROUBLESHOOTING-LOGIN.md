# Troubleshooting Admin Login Issues

If you're experiencing issues with admin login, follow these steps to resolve them:

## Quick Solutions

### 1. Disable App Check in Firebase Console (Recommended)

1. Log in to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (`bike-rental-bc5bd`)
3. Go to Project Settings > App Check
4. Uncheck "Enforce" for each service or disable "Enforce all"
5. Save your changes
6. Try logging in again

### 2. Use the bypass script

We created several utility scripts that can help bypass App Check issues:

1. Run the helper script:
```bash
node bypass-app-check.js
```

2. Follow the prompts to create an admin account
3. Try logging in with the provided credentials

### 3. Clear browser cache and cookies

1. Open your browser settings
2. Clear browsing data, including cookies and cache
3. Close and reopen your browser
4. Try logging in again

## Administrative Credentials

Use these credentials to log in:

- **Email:** admin@bambike.com
- **Password:** Admin123!

## Debug Information

If you're still experiencing issues, check:

1. **Browser console** (F12) for errors
2. **Firebase Authentication** console to verify the user exists
3. **Firestore Database** to ensure the user document has `role: "admin"` and there's a matching document in the `admins` collection

## App Check Configuration for Production

When you're ready to use App Check in production:

1. Go to Firebase Console > Project Settings > App Check
2. Set up the reCAPTCHA v3 provider
3. Register proper debug tokens for development
4. Update the code in `src/firebase.js` to use the correct approach
5. Test thoroughly before enabling "Enforce all"

## Need More Help?

Check these files for additional solutions:
- `README-APP-CHECK.md` - Detailed App Check setup
- `validate-app-check.html` - Tool to verify App Check configuration
- `get-debug-token.js` - Generate valid App Check debug tokens 