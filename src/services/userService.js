import { db, auth } from '../firebase';
import { collection, getDocs, doc, updateDoc, onSnapshot, query, deleteDoc, getDoc } from 'firebase/firestore';
import { httpsCallable, getFunctions } from 'firebase/functions';

// Check for development environment
const isDev = process.env.NODE_ENV === 'development';

// Helper function to check authentication with better error handling
const checkAuth = () => {
  const user = auth.currentUser;
  if (!user) {
    // Don't throw an error immediately, log it for debugging
    console.warn('User not authenticated in userService checkAuth()');
    return null;
  }
  console.log("Auth check successful for:", user.uid);
  return user;
};

// Helper function for operations that require authentication
const requireAuth = () => {
  const user = auth.currentUser;
  if (!user) {
    console.error("No authenticated user found!");
    throw new Error('User not authenticated. Please log in to access this resource.');
  }
  console.log("Authenticated user:", user.uid, "Email:", user.email);
  return user;
};

// Helper for getting admin status - allows multiple paths to determine admin privilege
const checkAdminStatus = async (uid) => {
  let isAdmin = false;
  
  // First try - check Firestore document
  try {
    const userRef = doc(db, 'users', uid);
    const userDoc = await getDoc(userRef);
    
    if (userDoc.exists()) {
      const userData = userDoc.data();
      isAdmin = userData.role?.toLowerCase() === 'admin' || 
                userData.isAdmin === true || 
                userData.isAdmin === 'true';
      
      if (isAdmin) {
        console.log(`User ${uid} confirmed as admin via Firestore document`);
        return true;
      }
    } else {
      console.warn(`No user document found for ${uid} when checking admin status`);
    }
  } catch (error) {
    console.error(`Error checking Firestore for admin status: ${error.message}`);
  }
  
  // Second try - check user email if in development mode
  if (isDev) {
    try {
      // Get the current user's email
      const userEmail = auth.currentUser?.email;
      
      // List of emails that should be considered admins in development mode
      const devAdminEmails = [
        'admin@example.com',
        'dev@example.com',
        'test@example.com',
        'admin@test.com',
        'admin@bambike.com',
        'bambike.admin@gmail.com'
      ];
      
      if (userEmail && devAdminEmails.includes(userEmail.toLowerCase())) {
        console.log(`Dev environment: granting admin privileges to ${userEmail}`);
        return true;
      }
      
      // Always grant admin access in dev mode
      console.log("Development environment detected, granting admin privileges");
      return true;
    } catch (error) {
      console.error(`Error checking email for admin status: ${error.message}`);
    }
  }
  
  return false;
};

// Fetch all users
export const getUsers = async () => {
  try {
    // Check authentication - but don't fail immediately
    const user = checkAuth();
    if (!user) {
      console.warn('Attempting to get users without authentication');
      // Try to proceed anyway for admin dashboard
    }
    
    const usersCollection = collection(db, 'users');
    const usersSnapshot = await getDocs(usersCollection);
    
    return usersSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      role: doc.data().role || (doc.data().isAdmin ? 'admin' : 'user')
    }));
  } catch (error) {
    console.error('Error getting users:', error);
    throw error;
  }
};

// Update user role
export const updateUserRole = async (userId, newRole) => {
  try {
    // Require authentication for this operation
    const currentUser = requireAuth();
    
    // Check if current user is an admin
    const isAdmin = await checkAdminStatus(currentUser.uid);
    
    if (!isAdmin) {
      throw new Error('Only administrators can update user roles.');
    }
    
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, { 
      role: newRole,
      lastUpdated: new Date()
    });
    
    console.log(`User ${userId} role updated to ${newRole} by admin ${currentUser.uid}`);
  } catch (error) {
    console.error('Error updating user role:', error);
    throw error;
  }
};

// Set up real-time listener for users collection
export const subscribeToUsers = (callback) => {
  try {
    // Check authentication - but don't fail immediately
    const user = checkAuth();
    if (!user) {
      console.warn('Setting up users subscription without authentication');
    }
    
    // Create a query against the users collection
    const usersQuery = query(collection(db, 'users'));
    
    // Set up a real-time listener
    const unsubscribe = onSnapshot(usersQuery, (querySnapshot) => {
      const users = querySnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
        role: doc.data().role || (doc.data().isAdmin ? 'admin' : 'user')
      }));
      callback(users);
    }, (error) => {
      console.error('Error subscribing to users:', error);
    });
    
    // Return the unsubscribe function to clean up the listener
    return unsubscribe;
  } catch (error) {
    console.error('Error subscribing to users:', error);
    throw error;
  }
};

// Block or unblock a user
export const updateUserBlockStatus = async (userId, isBlocked) => {
  try {
    // Require authentication for this operation
    const currentUser = requireAuth();
    
    // Check if current user is an admin
    const isAdmin = await checkAdminStatus(currentUser.uid);
    
    if (!isAdmin) {
      throw new Error(`Only administrators can block/unblock users. Contact an administrator to get admin permissions.`);
    }
    
    // Update the user's block status in Firestore
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, { 
      isBlocked: isBlocked,
      lastUpdated: new Date(),
      blockedBy: isBlocked ? currentUser.uid : null,
      blockedAt: isBlocked ? new Date() : null
    });
    
    console.log(`User ${userId} ${isBlocked ? 'blocked' : 'unblocked'} successfully by ${currentUser.uid}`);
    return true;
  } catch (error) {
    console.error('Error updating user block status:', error);
    if (error.message.includes('administrators')) {
      throw error; // Re-throw permission errors as-is
    }
    throw new Error(`Failed to ${isBlocked ? 'block' : 'unblock'} user: ${error.message}`);
  }
};

// Delete a user
export const deleteUser = async (userId) => {
  try {
    // Require authentication for this operation
    const currentUser = requireAuth();
    
    // Check if current user is an admin
    const isAdmin = await checkAdminStatus(currentUser.uid);
    
    if (!isAdmin) {
      throw new Error(`Only administrators can delete users. Contact an administrator to get admin permissions.`);
    }
    
    // Prevent self-deletion
    if (userId === currentUser.uid) {
      throw new Error('You cannot delete your own account');
    }
    
    // 1. Delete the user document from Firestore
    const userRef = doc(db, 'users', userId);
    const userDoc = await getDoc(userRef);
    
    if (!userDoc.exists()) {
      throw new Error('User not found');
    }
    
    // Store user data for logging
    const userData = userDoc.data();
    
    // Delete from Firestore
    await deleteDoc(userRef);
    console.log(`User ${userId} deleted from Firestore by admin ${currentUser.uid}`);
    
    try {
      // 2. Call Cloud Function to delete the user from Firebase Authentication
      const functions = getFunctions();
      const deleteFirebaseUser = httpsCallable(functions, 'deleteUser');
      const result = await deleteFirebaseUser({ userId });
      console.log('User deleted from Authentication:', result.data);
    } catch (authError) {
      console.error('Error deleting user from Authentication:', authError);
      // This is not fatal - the Firestore document is already deleted
      console.warn('User was deleted from Firestore but may still exist in Firebase Authentication');
    }
    
    return {
      success: true,
      message: `User ${userData.email || userData.displayName || userId} deleted successfully`
    };
  } catch (error) {
    console.error('Error deleting user:', error);
    if (error.message.includes('administrators') || error.message.includes('cannot delete')) {
      throw error; // Re-throw permission errors as-is
    }
    throw new Error(`Failed to delete user: ${error.message}`);
  }
};

/**
 * Submit ID for verification (sets status to 'pending')
 * This is a convenience function for users to submit their ID
 * 
 * @param {string} userId - The ID of the user submitting verification
 * @param {string} idImageUrl - The URL of the uploaded ID image
 * @returns {Promise<void>}
 */
export const submitIdVerification = async (userId, idImageUrl) => {
  try {
    const currentUser = requireAuth();
    
    // Users can only submit verification for their own account
    if (currentUser.uid !== userId) {
      throw new Error('You can only submit verification for your own account.');
    }
    
    // Build update object for submission
    const updateData = {
      idImageUrl: idImageUrl,
      idVerificationStatus: 'pending',
      isIdVerified: false,
      idVerificationNote: null, // Clear any previous rejection notes
      idSubmittedAt: new Date(),
      lastUpdated: new Date(),
    };
    
    // Update Firestore document
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, updateData);
    
    console.log(`User ${userId} submitted ID for verification`);
    return true;
  } catch (error) {
    console.error('Error submitting ID verification:', error);
    throw error;
  }
};

/**
 * Update the ID verification status of a user
 * 
 * @param {string} userId - The ID of the user to update
 * @param {string} status - The new verification status ('pending', 'verified', or 'rejected')
 * @param {string|null} note - Optional note to include (required for rejections)
 * @returns {Promise<void>}
 */
export const updateIdVerificationStatus = async (userId, status, note = null) => {
  try {
    // Require authentication for this operation
    const currentUser = requireAuth();
    console.log("Current authenticated user:", currentUser.uid);
    
    // Validate status first before checking permissions
    if (!['pending', 'verified', 'rejected'].includes(status)) {
      throw new Error(`Invalid verification status: ${status}`);
    }
    
    // If rejecting, note is required
    if (status === 'rejected' && !note) {
      throw new Error('A reason for rejection is required.');
    }

    // Check if current user is an admin
    const isAdmin = await checkAdminStatus(currentUser.uid);
    
    // Permission logic:
    // - Users can only set their own status to 'pending' (when submitting ID)
    // - Only admins can approve ('verified') or reject ('rejected') any user's ID
    if (status === 'pending') {
      // Users can set their own status to pending, or admins can set anyone's status to pending
      if (currentUser.uid !== userId && !isAdmin) {
        console.error(`User ${currentUser.uid} attempted to set pending status for another user ${userId}`);
        throw new Error('You can only submit verification for your own account.');
      }
    } else if (status === 'verified' || status === 'rejected') {
      // Only admins can approve or reject ID verification
      if (!isAdmin) {
        console.error(`User ${currentUser.uid} attempted to ${status} verification but is not an admin`);
        throw new Error('Only administrators can approve or reject ID verification.');
      }
    }
    
    console.log(`${isAdmin ? 'Admin' : 'User'} ${currentUser.uid} updating ID verification for user ${userId} to ${status}`);
    
    // Build update object
    const updateData = {
      idVerificationStatus: status,
      isIdVerified: status === 'verified',
      idVerifiedAt: status === 'verified' ? new Date() : null,
      idVerifiedBy: status === 'verified' ? currentUser.uid : null,
      lastUpdated: new Date(),
    };
    
    // Add note if provided (should be provided for rejections)
    if (note) {
      updateData.idVerificationNote = note;
    } else if (status === 'verified') {
      // Clear notes if verifying
      updateData.idVerificationNote = null;
    }
    
    // Update Firestore document
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, updateData);
    
    console.log(`User ${userId} ID verification status updated to "${status}" by ${isAdmin ? 'admin' : 'user'} ${currentUser.uid}`);
    return true;
  } catch (error) {
    console.error('Error updating ID verification status:', error);
    throw error;
  }
}; 