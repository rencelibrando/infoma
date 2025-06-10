import { db, auth } from '../firebase';
import { collection, getDocs, doc, updateDoc, onSnapshot, query, deleteDoc, getDoc } from 'firebase/firestore';
import { httpsCallable, getFunctions } from 'firebase/functions';

// Helper function to check authentication with better error handling
const checkAuth = () => {
  const user = auth.currentUser;
  if (!user) {
    // Don't throw an error immediately, log it for debugging
    console.warn('User not authenticated in userService checkAuth()');
    return null;
  }
  return user;
};

// Helper function for operations that require authentication
const requireAuth = () => {
  const user = auth.currentUser;
  if (!user) {
    throw new Error('User not authenticated. Please log in to access this resource.');
  }
  return user;
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
    requireAuth();
    
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, { 
      role: newRole,
      lastUpdated: new Date()
    });
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
    
    // Get current user's role to verify admin permissions
    const currentUserRef = doc(db, 'users', currentUser.uid);
    const currentUserDoc = await getDoc(currentUserRef);
    const currentUserData = currentUserDoc.data();
    
    if (!currentUserDoc.exists()) {
      throw new Error('User document not found. Please ensure your account is properly set up.');
    }
    
    // More flexible admin check - case insensitive and multiple fields
    const isAdmin = currentUserData && (
      currentUserData.role?.toLowerCase() === 'admin' ||
      currentUserData.isAdmin === true ||
      currentUserData.isAdmin === 'true' ||
      currentUserData.role?.toLowerCase() === 'administrator'
    );
    
    if (!isAdmin) {
      throw new Error(`Only administrators can block/unblock users. Your current role: ${currentUserData?.role || 'None'}. Contact an administrator to get admin permissions.`);
    }
    
    // Update the user's block status in Firestore
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, { 
      isBlocked: isBlocked,
      lastUpdated: new Date(),
      blockedBy: isBlocked ? currentUser.uid : null,
      blockedAt: isBlocked ? new Date() : null
    });
    
    console.log(`User ${userId} ${isBlocked ? 'blocked' : 'unblocked'} successfully`);
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
    
    // Get current user's role to verify admin permissions
    const currentUserRef = doc(db, 'users', currentUser.uid);
    const currentUserDoc = await getDoc(currentUserRef);
    const currentUserData = currentUserDoc.data();
    
    if (!currentUserDoc.exists()) {
      throw new Error('User document not found. Please ensure your account is properly set up.');
    }
    
    // More flexible admin check - case insensitive and multiple fields
    const isAdmin = currentUserData && (
      currentUserData.role?.toLowerCase() === 'admin' ||
      currentUserData.isAdmin === true ||
      currentUserData.isAdmin === 'true' ||
      currentUserData.role?.toLowerCase() === 'administrator'
    );
    
    if (!isAdmin) {
      throw new Error(`Only administrators can delete users. Your current role: ${currentUserData?.role || 'None'}. Contact an administrator to get admin permissions.`);
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