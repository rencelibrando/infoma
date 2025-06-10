/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onCall} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK
if (!admin.apps.length) {
  admin.initializeApp();
}

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

// Function to delete a user from Firebase Authentication
exports.deleteUser = onCall({cors: true}, async (request) => {
  const {data, auth} = request;
  
  // Check if the user is authenticated
  if (!auth) {
    throw new Error("User must be authenticated");
  }

  // Check if the user has admin privileges
  const db = admin.firestore();
  const userDoc = await db.collection('users').doc(auth.uid).get();
  const userData = userDoc.data();
  
  // More flexible admin check - case insensitive and multiple fields
  const isAdmin = userData && (
    userData.role?.toLowerCase() === 'admin' ||
    userData.isAdmin === true ||
    userData.isAdmin === 'true' ||
    userData.role?.toLowerCase() === 'administrator'
  );
  
  if (!isAdmin) {
    throw new Error("Only admins can delete users");
  }

  const {userId} = data;
  
  if (!userId) {
    throw new Error("User ID is required");
  }

  try {
    // Delete user from Firebase Authentication
    await admin.auth().deleteUser(userId);
    
    logger.info(`User ${userId} deleted from Authentication by admin ${auth.uid}`);
    
    return {success: true, message: "User deleted successfully"};
  } catch (error) {
    logger.error("Error deleting user from Authentication:", error);
    
    if (error.code === 'auth/user-not-found') {
      // User doesn't exist in Authentication, but that's okay
      logger.info(`User ${userId} not found in Authentication, continuing...`);
      return {success: true, message: "User deleted successfully"};
    }
    
    throw new Error(`Failed to delete user: ${error.message || 'Unknown error'}`);
  }
});

// Function to update user block status
exports.updateUserBlockStatus = onCall({cors: true}, async (request) => {
  const {data, auth} = request;
  
  if (!auth) {
    throw new Error("User must be authenticated");
  }

  const db = admin.firestore();
  const userDoc = await db.collection('users').doc(auth.uid).get();
  const userData = userDoc.data();
  
  // More flexible admin check - case insensitive and multiple fields
  const isAdmin = userData && (
    userData.role?.toLowerCase() === 'admin' ||
    userData.isAdmin === true ||
    userData.isAdmin === 'true' ||
    userData.role?.toLowerCase() === 'administrator'
  );
  
  if (!isAdmin) {
    throw new Error("Only admins can block/unblock users");
  }

  const {userId, isBlocked} = data;
  
  if (!userId || typeof isBlocked !== 'boolean') {
    throw new Error("User ID and block status are required");
  }

  try {
    // Update user's custom claims in Authentication
    await admin.auth().setCustomUserClaims(userId, {
      blocked: isBlocked
    });
    
    logger.info(`User ${userId} ${isBlocked ? 'blocked' : 'unblocked'} by admin ${auth.uid}`);
    
    return {success: true, message: `User ${isBlocked ? 'blocked' : 'unblocked'} successfully`};
  } catch (error) {
    logger.error("Error updating user block status:", error);
    
    if (error.code === 'auth/user-not-found') {
      throw new Error("User not found in Authentication");
    }
    
    throw new Error(`Failed to update user status: ${error.message || 'Unknown error'}`);
  }
});
