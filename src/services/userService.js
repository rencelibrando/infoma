import { db } from '../firebase';
import { collection, getDocs, doc, updateDoc, onSnapshot, query } from 'firebase/firestore';

// Fetch all users
export const getUsers = async () => {
  try {
    const querySnapshot = await getDocs(collection(db, 'users'));
    return querySnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      role: doc.data().role || (doc.data().isAdmin ? 'Admin' : 'User')
    }));
  } catch (error) {
    console.error('Error fetching users:', error);
    throw error;
  }
};

// Update user role
export const updateUserRole = async (userId, newRole) => {
  try {
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, { 
      role: newRole,
      isAdmin: newRole === 'Admin',
      updatedAt: new Date()
    });
  } catch (error) {
    console.error('Error updating user role:', error);
    throw error;
  }
};

// Update user verification status
export const updateUserVerificationStatus = async (userId, newStatus) => {
  try {
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, { 
      idVerificationStatus: newStatus,
      lastUpdated: new Date()
    });
  } catch (error) {
    console.error('Error updating verification status:', error);
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