import { db } from '../firebase';
import { collection, getDocs, doc, updateDoc, onSnapshot, query, deleteDoc } from 'firebase/firestore';
import { httpsCallable, getFunctions } from 'firebase/functions';

// Fetch all users
export const getUsers = async () => {
  try {
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
};

// Block or unblock a user
export const updateUserBlockStatus = async (userId, isBlocked) => {
  try {
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