import { db, auth } from '../firebase';
import { collection, getDocs, doc, updateDoc, onSnapshot, query, deleteDoc } from 'firebase/firestore';
import { httpsCallable, getFunctions } from 'firebase/functions';

// Helper function to check authentication
const checkAuth = () => {
  const user = auth.currentUser;
  if (!user) {
    throw new Error('User not authenticated. Please log in to access this resource.');
  }
  return user;
};

// Fetch all users
export const getUsers = async () => {
  try {
    // Check authentication first
    checkAuth();
    
    const usersCollection = collection(db, 'users');
    const usersSnapshot = await getDocs(usersCollection);
    
    return usersSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      role: doc.data().role || (doc.data().isAdmin ? 'Admin' : 'User')
    }));
  } catch (error) {
    console.error('Error getting users:', error);
    throw error;
  }
};

// Update user role
export const updateUserRole = async (userId, newRole) => {
  try {
    // Check authentication first
    checkAuth();
    
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
    // Check authentication first
    const user = checkAuth();
    
    // Create a query against the users collection
    const usersQuery = query(collection(db, 'users'));
    
    // Set up a real-time listener
    const unsubscribe = onSnapshot(usersQuery, (querySnapshot) => {
      const users = querySnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
        role: doc.data().role || (doc.data().isAdmin ? 'Admin' : 'User')
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
    // Check authentication first
    checkAuth();
    
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, { 
      isBlocked: isBlocked,
      lastUpdated: new Date()
    });
    return true;
  } catch (error) {
    console.error('Error updating user block status:', error);
    throw error;
  }
};

// Delete a user
export const deleteUser = async (userId) => {
  try {
    // Check authentication first
    checkAuth();
    
    // 1. Delete the user document from Firestore
    const userRef = doc(db, 'users', userId);
    await deleteDoc(userRef);
    
    try {
      // 2. Call Cloud Function to delete the user from Firebase Authentication
      const functions = getFunctions();
      const deleteFirebaseUser = httpsCallable(functions, 'deleteUser');
      await deleteFirebaseUser({ userId });
    } catch (authError) {
      console.error('Error deleting user from Authentication:', authError);
      // Continue execution - at least the Firestore document was deleted
    }
    
    return true;
  } catch (error) {
    console.error('Error deleting user:', error);
    throw error;
  }
}; 