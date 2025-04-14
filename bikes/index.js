const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Secure function to get all bikes (admin only)
exports.getAllBikes = functions.https.onCall(async (data, context) => {
  // Check if user is authenticated and admin
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be logged in');
  }
  
  // Admin check logic here
  const userDoc = await admin.firestore().collection('users').doc(context.auth.uid).get();
  if (!userDoc.exists || !userDoc.data().isAdmin) {
    throw new functions.https.HttpsError('permission-denied', 'Must be an admin user');
  }
  
  // Get all bikes
  const bikesSnapshot = await admin.firestore().collection('bikes').get();
  return { bikes: bikesSnapshot.docs.map(doc => ({id: doc.id, ...doc.data()})) };
});