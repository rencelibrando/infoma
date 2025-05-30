# Firebase Security Rules for Bike Rental App

## Firestore Security Rules

Save this as `firestore.rules`:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return request.auth.uid == userId;
    }
    
    function isAdmin() {
      return request.auth.token.admin == true;
    }
    
    function isValidUser() {
      return isAuthenticated() && 
             request.auth.token.email_verified == true;
    }
    
    // Bikes collection
    match /bikes/{bikeId} {
      // Anyone can read bike information
      allow read: if true;
      
      // Only admin can create, update, or delete bikes
      allow write: if isAdmin();
      
      // Reviews subcollection
      match /reviews/{reviewId} {
        // Anyone can read reviews
        allow read: if true;
        
        // Only authenticated users can create reviews
        allow create: if isValidUser() && 
                     isOwner(resource.data.userId) &&
                     request.auth.uid == request.auth.uid;
        
        // Only review author can update their own review
        allow update: if isValidUser() && 
                     isOwner(resource.data.userId);
        
        // Only review author or admin can delete
        allow delete: if isValidUser() && 
                     (isOwner(resource.data.userId) || isAdmin());
      }
    }
    
    // Users collection
    match /users/{userId} {
      // Users can read their own data, admin can read all
      allow read: if isOwner(userId) || isAdmin();
      
      // Users can create their own profile
      allow create: if isOwner(userId) && 
                   isValidUser() &&
                   request.resource.data.uid == request.auth.uid;
      
      // Users can update their own profile (except certain fields)
      allow update: if isOwner(userId) && 
                   isValidUser() &&
                   !('totalRides' in request.resource.data.diff(resource.data).affectedKeys()) &&
                   !('totalSpent' in request.resource.data.diff(resource.data).affectedKeys()) &&
                   !('membershipType' in request.resource.data.diff(resource.data).affectedKeys());
      
      // No deletion allowed (soft delete only)
      allow delete: if false;
      
      // Ride history subcollection
      match /rideHistory/{rideId} {
        // Users can read their own ride history, admin can read all
        allow read: if isOwner(userId) || isAdmin();
        
        // Only system/admin can write ride history
        allow write: if isAdmin();
      }
      
      // Payment methods subcollection
      match /paymentMethods/{paymentId} {
        // Users can read their own payment methods
        allow read: if isOwner(userId);
        
        // Users can create and update their own payment methods
        allow create, update: if isOwner(userId) && isValidUser();
        
        // Users can delete their own payment methods
        allow delete: if isOwner(userId) && isValidUser();
      }
    }
    
    // Bookings collection
    match /bookings/{bookingId} {
      // Users can read their own bookings, admin can read all
      allow read: if isValidUser() && 
                 (resource.data.userId == request.auth.uid || isAdmin());
      
      // Users can create their own bookings
      allow create: if isValidUser() && 
                   request.resource.data.userId == request.auth.uid &&
                   request.resource.data.keys().hasAll(['userId', 'bikeId', 'startTime', 'endTime', 'status']) &&
                   request.resource.data.status == 'pending';
      
      // Users can update their own bookings (limited fields)
      allow update: if isValidUser() && 
                   (resource.data.userId == request.auth.uid || isAdmin()) &&
                   (!request.resource.data.diff(resource.data).affectedKeys().hasAny(['userId', 'bikeId', 'createdAt']));
      
      // Only admin can delete bookings
      allow delete: if isAdmin();
    }
    
    // Stations collection
    match /stations/{stationId} {
      // Anyone can read station information
      allow read: if true;
      
      // Only admin can write station data
      allow write: if isAdmin();
    }
    
    // Admin collection
    match /admin/{document=**} {
      // Only admin can access admin data
      allow read, write: if isAdmin();
    }
    
    // Analytics collection (read-only for admin)
    match /analytics/{document=**} {
      allow read: if isAdmin();
      allow write: if false; // Updated by Cloud Functions only
    }
  }
}
```

## Realtime Database Security Rules

Save this as `database.rules.json`:

```json
{
  "rules": {
    ".read": false,
    ".write": false,
    
    "bikes": {
      "$bikeId": {
        ".read": true,
        ".write": "auth != null && (auth.token.admin == true || root.child('rides').orderByChild('bikeId').equalTo($bikeId).orderByChild('userId').equalTo(auth.uid).orderByChild('status').equalTo('active').exists())",
        ".validate": "newData.hasChildren(['latitude', 'longitude', 'isAvailable', 'isInUse'])",
        
        "latitude": {
          ".validate": "newData.isNumber() && newData.val() >= -90 && newData.val() <= 90"
        },
        "longitude": {
          ".validate": "newData.isNumber() && newData.val() >= -180 && newData.val() <= 180"
        },
        "isAvailable": {
          ".validate": "newData.isBoolean()"
        },
        "isInUse": {
          ".validate": "newData.isBoolean()"
        },
        "currentRider": {
          ".validate": "newData.isString()"
        },
        "batteryLevel": {
          ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 100"
        },
        "lastUpdated": {
          ".validate": "newData.isString()"
        }
      }
    },
    
    "rides": {
      "$rideId": {
        ".read": "auth != null && (auth.token.admin == true || data.child('userId').val() == auth.uid)",
        ".write": "auth != null && (auth.token.admin == true || data.child('userId').val() == auth.uid)",
        ".validate": "newData.hasChildren(['id', 'bikeId', 'userId', 'startTime', 'status'])",
        
        "id": {
          ".validate": "newData.isString() && newData.val() == $rideId"
        },
        "bikeId": {
          ".validate": "newData.isString()"
        },
        "userId": {
          ".validate": "newData.isString() && newData.val() == auth.uid"
        },
        "startTime": {
          ".validate": "newData.isNumber()"
        },
        "endTime": {
          ".validate": "newData.isNumber() || !newData.exists()"
        },
        "status": {
          ".validate": "newData.isString() && (newData.val() == 'active' || newData.val() == 'completed' || newData.val() == 'cancelled')"
        },
        "cost": {
          ".validate": "newData.isNumber() && newData.val() >= 0"
        },
        "startLocation": {
          ".validate": "newData.hasChildren(['latitude', 'longitude', 'timestamp'])",
          "latitude": {
            ".validate": "newData.isNumber() && newData.val() >= -90 && newData.val() <= 90"
          },
          "longitude": {
            ".validate": "newData.isNumber() && newData.val() >= -180 && newData.val() <= 180"
          },
          "timestamp": {
            ".validate": "newData.isNumber()"
          }
        },
        "endLocation": {
          ".validate": "newData.hasChildren(['latitude', 'longitude', 'timestamp']) || !newData.exists()",
          "latitude": {
            ".validate": "newData.isNumber() && newData.val() >= -90 && newData.val() <= 90"
          },
          "longitude": {
            ".validate": "newData.isNumber() && newData.val() >= -180 && newData.val() <= 180"
          },
          "timestamp": {
            ".validate": "newData.isNumber()"
          }
        },
        "path": {
          "$pathIndex": {
            ".validate": "newData.hasChildren(['latitude', 'longitude', 'timestamp'])",
            "latitude": {
              ".validate": "newData.isNumber() && newData.val() >= -90 && newData.val() <= 90"
            },
            "longitude": {
              ".validate": "newData.isNumber() && newData.val() >= -180 && newData.val() <= 180"
            },
            "timestamp": {
              ".validate": "newData.isNumber()"
            }
          }
        }
      }
    },
    
    "liveTracking": {
      "$rideId": {
        ".read": "auth != null && (auth.token.admin == true || root.child('rides').child($rideId).child('userId').val() == auth.uid)",
        ".write": "auth != null && (auth.token.admin == true || root.child('rides').child($rideId).child('userId').val() == auth.uid)",
        ".validate": "newData.hasChildren(['currentLocation', 'lastUpdate'])",
        
        "currentLocation": {
          ".validate": "newData.hasChildren(['latitude', 'longitude', 'timestamp'])",
          "latitude": {
            ".validate": "newData.isNumber() && newData.val() >= -90 && newData.val() <= 90"
          },
          "longitude": {
            ".validate": "newData.isNumber() && newData.val() >= -180 && newData.val() <= 180"
          },
          "timestamp": {
            ".validate": "newData.isNumber()"
          },
          "speed": {
            ".validate": "newData.isNumber() && newData.val() >= 0"
          },
          "heading": {
            ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() < 360"
          }
        },
        "lastUpdate": {
          ".validate": "newData.isNumber()"
        }
      }
    },
    
    "userPresence": {
      "$userId": {
        ".read": "auth != null && (auth.token.admin == true || auth.uid == $userId)",
        ".write": "auth != null && auth.uid == $userId",
        ".validate": "newData.hasChildren(['online', 'lastSeen'])",
        
        "online": {
          ".validate": "newData.isBoolean()"
        },
        "lastSeen": {
          ".validate": "newData.isNumber()"
        },
        "currentRide": {
          ".validate": "newData.isString() || !newData.exists()"
        }
      }
    }
  }
}
```

## Firebase Storage Security Rules

Save this as `storage.rules`:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return request.auth.uid == userId;
    }
    
    function isAdmin() {
      return request.auth.token.admin == true;
    }
    
    function isValidImageFile() {
      return request.resource.contentType.matches('image/.*') &&
             request.resource.size < 10 * 1024 * 1024; // 10MB limit
    }
    
    // Bike images
    match /bikes/{bikeId}/{allPaths=**} {
      // Anyone can read bike images
      allow read: if true;
      
      // Only admin can upload bike images
      allow write: if isAdmin() && isValidImageFile();
    }
    
    // User profile images
    match /users/{userId}/profile/{fileName} {
      // Anyone can read profile images
      allow read: if true;
      
      // Users can upload their own profile image
      allow write: if isAuthenticated() && 
                  isOwner(userId) && 
                  isValidImageFile();
    }
    
    // Ride photos/evidence
    match /rides/{rideId}/{fileName} {
      // Only ride participant and admin can read
      allow read: if isAuthenticated() && 
                 (isAdmin() || 
                  firestore.get(/databases/(default)/documents/users/$(request.auth.uid)/rideHistory/$(rideId)).data.userId == request.auth.uid);
      
      // Only ride participant can upload
      allow write: if isAuthenticated() && 
                  isValidImageFile() &&
                  firestore.get(/databases/(default)/documents/users/$(request.auth.uid)/rideHistory/$(rideId)).data.userId == request.auth.uid;
    }
    
    // Admin uploads
    match /admin/{allPaths=**} {
      allow read, write: if isAdmin();
    }
    
    // Default deny
    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

## Custom Claims Setup

To implement admin functionality, you'll need to set custom claims. Here's a Cloud Function example:

```javascript
// Cloud Function to set admin claims
const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.setAdminClaim = functions.https.onCall(async (data, context) => {
  // Check if request is made by an admin
  if (!context.auth || !context.auth.token.admin) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Only admins can set admin claims.'
    );
  }

  const { uid } = data;
  
  try {
    await admin.auth().setCustomUserClaims(uid, { admin: true });
    return { success: true, message: `Admin claim set for user ${uid}` };
  } catch (error) {
    throw new functions.https.HttpsError('internal', error.message);
  }
});

// Automatically set email verification requirement
exports.onUserCreate = functions.auth.user().onCreate(async (user) => {
  // Set default claims for new users
  await admin.auth().setCustomUserClaims(user.uid, {
    admin: false,
    emailVerified: user.emailVerified
  });
  
  // Create user document in Firestore
  await admin.firestore().collection('users').doc(user.uid).set({
    uid: user.uid,
    email: user.email,
    displayName: user.displayName || '',
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    totalRides: 0,
    totalDistance: 0,
    totalSpent: 0,
    membershipType: 'standard'
  });
});
```

## Security Best Practices

1. **Always validate data on the server side**
2. **Use custom claims for role-based access control**
3. **Implement rate limiting for sensitive operations**
4. **Regularly audit and update security rules**
5. **Use Firebase App Check for additional security**
6. **Implement proper session management**
7. **Encrypt sensitive data before storing**
8. **Use HTTPS only for all communications**
9. **Implement proper error handling that doesn't leak information**
10. **Regular security testing and monitoring**

## Deployment Commands

```bash
# Deploy Firestore rules
firebase deploy --only firestore:rules

# Deploy Realtime Database rules
firebase deploy --only database

# Deploy Storage rules
firebase deploy --only storage

# Deploy all rules
firebase deploy --only firestore:rules,database,storage
```

## Testing Security Rules

Use the Firebase Emulator Suite to test your security rules:

```bash
# Start emulators
firebase emulators:start

# Run security rules tests
npm test
```

Create test files for your security rules to ensure they work correctly before deploying to production. 