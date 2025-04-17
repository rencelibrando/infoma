import { db } from '../firebase';
import { collection, addDoc, updateDoc, doc, getDoc, query, where, getDocs, serverTimestamp } from "firebase/firestore";

// Update a bike's location
export const updateBikeLocation = async (bikeId, latitude, longitude) => {
  try {
    const bikeRef = doc(db, "bikes", bikeId);
    const bikeDoc = await getDoc(bikeRef);
    
    if (!bikeDoc.exists()) {
      throw new Error(`Bike with ID ${bikeId} not found`);
    }
    
    // Update the bike's location
    await updateDoc(bikeRef, {
      latitude,
      longitude,
      lastLocationUpdate: serverTimestamp()
    });
    
    // Also log to location history
    await addDoc(collection(db, "bikeLocationHistory"), {
      bikeId,
      latitude,
      longitude,
      timestamp: serverTimestamp()
    });
    
    return {
      id: bikeId,
      latitude,
      longitude,
      lastLocationUpdate: new Date()
    };
  } catch (error) {
    console.error('Error updating bike location:', error);
    throw error;
  }
};

// Start a bike ride - assign bike to user
export const startBikeRide = async (bikeId, userId) => {
  try {
    const bikeRef = doc(db, "bikes", bikeId);
    const bikeDoc = await getDoc(bikeRef);
    
    if (!bikeDoc.exists()) {
      throw new Error(`Bike with ID ${bikeId} not found`);
    }
    
    const bikeData = bikeDoc.data();
    
    // Check if bike is available and locked (per our business rule)
    if (!bikeData.isAvailable) {
      throw new Error("This bike is not available for use");
    }
    
    // Bikes should be locked when available, and we should unlock them to start a ride
    if (!bikeData.isLocked) {
      throw new Error("This bike appears to be unlocked already. Please contact support.");
    }
    
    console.log(`Starting ride for bike ${bikeId}:`, {
      isLocked: bikeData.isLocked,
      isAvailable: bikeData.isAvailable,
      isInUse: bikeData.isInUse
    });
    
    // Create a new ride record
    const rideData = {
      bikeId,
      userId,
      startTime: serverTimestamp(),
      isActive: true,
      startLatitude: bikeData.latitude,
      startLongitude: bikeData.longitude
    };
    
    const rideRef = await addDoc(collection(db, "rides"), rideData);
    
    // Update bike status
    // Unlock the bike as part of starting the ride
    await updateDoc(bikeRef, {
      isLocked: false,      // Unlock the bike for use
      isAvailable: false,   // Not available for others
      isInUse: true,        // Mark as in use
      currentUserId: userId,
      currentRideId: rideRef.id,
      lastUpdated: serverTimestamp()
    });
    
    return {
      rideId: rideRef.id,
      ...rideData,
      startTime: new Date()
    };
  } catch (error) {
    console.error('Error starting bike ride:', error);
    throw error;
  }
};

// End a bike ride
export const endBikeRide = async (rideId, latitude, longitude) => {
  try {
    const rideRef = doc(db, "rides", rideId);
    const rideDoc = await getDoc(rideRef);
    
    if (!rideDoc.exists()) {
      throw new Error(`Ride with ID ${rideId} not found`);
    }
    
    const rideData = rideDoc.data();
    
    if (!rideData.isActive) {
      throw new Error("This ride is already completed");
    }
    
    const bikeId = rideData.bikeId;
    const bikeRef = doc(db, "bikes", bikeId);
    
    // Update ride status
    await updateDoc(rideRef, {
      endTime: serverTimestamp(),
      isActive: false,
      endLatitude: latitude, 
      endLongitude: longitude
    });
    
    console.log(`Ending ride for bike ${bikeId}, locking bike and marking as available.`);
    
    // Update bike status - lock the bike and mark as available
    await updateDoc(bikeRef, {
      isLocked: true,         // Lock the bike when ride ends
      isAvailable: true,      // Make available for next user
      isInUse: false,         // No longer in use
      currentUserId: null,
      currentRideId: null,
      latitude: latitude,
      longitude: longitude,
      lastUpdated: serverTimestamp()
    });
    
    return {
      rideId,
      bikeId,
      endTime: new Date(),
      endLatitude: latitude,
      endLongitude: longitude
    };
  } catch (error) {
    console.error('Error ending bike ride:', error);
    throw error;
  }
};

// Get active rides
export const getActiveRides = async () => {
  try {
    const ridesCollection = collection(db, "rides");
    const q = query(ridesCollection, where("isActive", "==", true));
    const snapshot = await getDocs(q);
    
    return snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startTime: doc.data().startTime?.toDate() || null
    }));
  } catch (error) {
    console.error('Error getting active rides:', error);
    throw error;
  }
};

// Get a user's ride history
export const getUserRideHistory = async (userId) => {
  try {
    const ridesCollection = collection(db, "rides");
    const q = query(ridesCollection, where("userId", "==", userId));
    const snapshot = await getDocs(q);
    
    return snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startTime: doc.data().startTime?.toDate() || null,
      endTime: doc.data().endTime?.toDate() || null
    }));
  } catch (error) {
    console.error('Error getting user ride history:', error);
    throw error;
  }
};

// Get a bike's location history
export const getBikeLocationHistory = async (bikeId) => {
  try {
    const locationCollection = collection(db, "bikeLocationHistory");
    const q = query(locationCollection, where("bikeId", "==", bikeId));
    const snapshot = await getDocs(q);
    
    return snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      timestamp: doc.data().timestamp?.toDate() || null
    }));
  } catch (error) {
    console.error('Error getting bike location history:', error);
    throw error;
  }
}; 