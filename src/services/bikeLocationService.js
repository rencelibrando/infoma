import { db, realtimeDb } from '../firebase';
import { collection, addDoc, updateDoc, doc, getDoc, query, where, getDocs, serverTimestamp } from "firebase/firestore";
import { ref, onValue, off, push, set, update, remove } from "firebase/database";

// Real-time location listeners
const locationListeners = new Map();

/**
 * Start listening to real-time location updates for a specific user
 * @param {string} userId - User ID to track
 * @param {function} onLocationUpdate - Callback function for location updates
 * @returns {function} Unsubscribe function
 */
export const startLocationTracking = (userId, onLocationUpdate) => {
  try {
    const locationRef = ref(realtimeDb, `liveLocation/${userId}`);
    
    const unsubscribe = onValue(locationRef, (snapshot) => {
      const locationData = snapshot.val();
      if (locationData && onLocationUpdate) {
        onLocationUpdate(locationData);
      }
    });
    
    // Store the unsubscribe function
    locationListeners.set(userId, unsubscribe);
    
    console.log(`Started tracking location for user: ${userId}`);
    
    return () => {
      stopLocationTracking(userId);
    };
  } catch (error) {
    console.error('Error starting location tracking:', error);
    throw error;
  }
};

/**
 * Stop listening to location updates for a specific user
 * @param {string} userId - User ID to stop tracking
 */
export const stopLocationTracking = (userId) => {
  try {
    const unsubscribe = locationListeners.get(userId);
    if (unsubscribe) {
      unsubscribe();
      locationListeners.delete(userId);
      console.log(`Stopped tracking location for user: ${userId}`);
    }
  } catch (error) {
    console.error('Error stopping location tracking:', error);
  }
};

/**
 * Listen to all active rides in real-time
 * @param {function} onActiveRidesUpdate - Callback function for active rides updates
 * @returns {function} Unsubscribe function
 */
export const listenToActiveRides = (onActiveRidesUpdate) => {
  try {
    const activeRidesRef = ref(realtimeDb, 'activeRides');
    
    const unsubscribe = onValue(activeRidesRef, (snapshot) => {
      const activeRidesData = snapshot.val() || {};
      const activeRides = Object.entries(activeRidesData).map(([userId, rideData]) => ({
        ...rideData,
        userId: userId,
        id: rideData.rideId || rideData.id
      }));
      
      if (onActiveRidesUpdate) {
        onActiveRidesUpdate(activeRides);
      }
    });
    
    console.log('Started listening to active rides');
    
    return unsubscribe;
  } catch (error) {
    console.error('Error listening to active rides:', error);
    throw error;
  }
};

/**
 * Listen to all live locations in real-time
 * @param {function} onLiveLocationsUpdate - Callback function for live locations updates
 * @returns {function} Unsubscribe function
 */
export const listenToLiveLocations = (onLiveLocationsUpdate) => {
  try {
    const liveLocationRef = ref(realtimeDb, 'liveLocation');
    
    const unsubscribe = onValue(liveLocationRef, (snapshot) => {
      const liveLocationData = snapshot.val() || {};
      
      if (onLiveLocationsUpdate) {
        onLiveLocationsUpdate(liveLocationData);
      }
    });
    
    console.log('Started listening to live locations');
    
    return unsubscribe;
  } catch (error) {
    console.error('Error listening to live locations:', error);
    throw error;
  }
};

/**
 * Get ride location history from Realtime Database
 * @param {string} rideId - Ride ID to get history for
 * @returns {Promise<Array>} Array of location points
 */
export const getRideLocationHistory = async (rideId) => {
  try {
    const historyRef = ref(realtimeDb, `rideLocationHistory/${rideId}`);
    
    return new Promise((resolve, reject) => {
      onValue(historyRef, (snapshot) => {
        const historyData = snapshot.val() || {};
        const locationHistory = Object.values(historyData).sort((a, b) => a.timestamp - b.timestamp);
        resolve(locationHistory);
      }, { onlyOnce: true });
    });
  } catch (error) {
    console.error('Error getting ride location history:', error);
    throw error;
  }
};

/**
 * Update ride status (for emergency alerts, etc.)
 * @param {string} rideId - Ride ID to update
 * @param {string} status - New status
 * @returns {Promise<void>}
 */
export const updateRideStatus = async (rideId, status) => {
  try {
    // Update in Realtime Database
    const rideRef = ref(realtimeDb, `rides/${rideId}`);
    await update(rideRef, { status });
    
    // Also update in Firestore for persistence
    await updateDoc(doc(db, 'rides', rideId), {
      status,
      updatedAt: serverTimestamp()
    });
    
    console.log(`Updated ride ${rideId} status to: ${status}`);
  } catch (error) {
    console.error('Error updating ride status:', error);
    throw error;
  }
};

/**
 * Send emergency alert for a ride
 * @param {string} rideId - Ride ID
 * @param {string} userId - User ID
 * @param {object} location - Current location
 * @returns {Promise<void>}
 */
export const sendEmergencyAlert = async (rideId, userId, location) => {
  try {
    const emergencyData = {
      rideId,
      userId,
      location,
      timestamp: Date.now(),
      status: 'emergency',
      responded: false
    };
    
    // Save to Realtime Database for immediate alerts
    const emergencyRef = ref(realtimeDb, `emergencyAlerts/${rideId}`);
    await set(emergencyRef, emergencyData);
    
    // Update ride status
    await updateRideStatus(rideId, 'emergency');
    
    console.log(`Emergency alert sent for ride: ${rideId}`);
  } catch (error) {
    console.error('Error sending emergency alert:', error);
    throw error;
  }
};

/**
 * Respond to emergency alert
 * @param {string} rideId - Ride ID
 * @returns {Promise<void>}
 */
export const respondToEmergency = async (rideId) => {
  try {
    const emergencyRef = ref(realtimeDb, `emergencyAlerts/${rideId}`);
    await update(emergencyRef, {
      responded: true,
      responseTime: Date.now()
    });
    
    console.log(`Responded to emergency for ride: ${rideId}`);
  } catch (error) {
    console.error('Error responding to emergency:', error);
    throw error;
  }
};

/**
 * Clean up all location listeners
 */
export const cleanupLocationListeners = () => {
  try {
    locationListeners.forEach((unsubscribe, userId) => {
      unsubscribe();
      console.log(`Cleaned up location listener for user: ${userId}`);
    });
    locationListeners.clear();
    console.log('All location listeners cleaned up');
  } catch (error) {
    console.error('Error cleaning up location listeners:', error);
  }
};

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
    
    const currentTimestamp = Date.now();
    
    // Create a new ride record with proper structure
    const rideData = {
      bikeId,
      userId,
      startTime: currentTimestamp,
      startLocation: {
        accuracy: 0,
        latitude: bikeData.latitude,
        longitude: bikeData.longitude,
        position: {
          latitude: bikeData.latitude,
          longitude: bikeData.longitude
        },
        speed: 0,
        timestamp: currentTimestamp
      },
      status: "active",
      cost: 0,
      distanceTraveled: 0,
      path: [{
        accuracy: 0,
        latitude: bikeData.latitude,
        longitude: bikeData.longitude,
        position: {
          latitude: bikeData.latitude,
          longitude: bikeData.longitude
        },
        speed: 0,
        timestamp: currentTimestamp
      }],
      // Keep legacy fields for backward compatibility
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
      startTime: new Date(currentTimestamp)
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
    
    if (rideData.status === "completed" || !rideData.isActive) {
      throw new Error("This ride is already completed");
    }
    
    const bikeId = rideData.bikeId;
    const bikeRef = doc(db, "bikes", bikeId);
    const currentTimestamp = Date.now();
    
    // Calculate ride cost and distance
    const startTime = rideData.startTime;
    const duration = (currentTimestamp - startTime) / 1000 / 3600; // hours
    const bikeDoc = await getDoc(bikeRef);
    const bikeInfo = bikeDoc.data();
    const hourlyRate = bikeInfo.priceValue || 50; // Default rate if not found
    const calculatedCost = duration * hourlyRate;
    
    // Calculate distance from path if available
    let totalDistance = 0;
    if (rideData.path && rideData.path.length > 1) {
      for (let i = 1; i < rideData.path.length; i++) {
        const prev = rideData.path[i - 1];
        const curr = rideData.path[i];
        totalDistance += getDistanceBetweenPoints(prev, curr);
      }
    }
    
    // Update ride status with proper structure
    await updateDoc(rideRef, {
      endTime: currentTimestamp,
      endLocation: {
        accuracy: 0,
        latitude: latitude,
        longitude: longitude,
        position: {
          latitude: latitude,
          longitude: longitude
        },
        speed: 0,
        timestamp: currentTimestamp
      },
      status: "completed",
      cost: calculatedCost,
      distanceTraveled: totalDistance,
      // Keep legacy fields for backward compatibility
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
      endTime: new Date(currentTimestamp),
      endLatitude: latitude,
      endLongitude: longitude,
      cost: calculatedCost,
      distanceTraveled: totalDistance
    };
  } catch (error) {
    console.error('Error ending bike ride:', error);
    throw error;
  }
};

// Helper function to calculate distance between two points
const getDistanceBetweenPoints = (point1, point2) => {
  const R = 6371; // Earth's radius in kilometers
  const dLat = (point2.latitude - point1.latitude) * Math.PI / 180;
  const dLon = (point2.longitude - point1.longitude) * Math.PI / 180;
  const a = 
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(point1.latitude * Math.PI / 180) * Math.cos(point2.latitude * Math.PI / 180) * 
    Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c; // Distance in kilometers
};

// Update ride location during active ride
export const updateRideLocation = async (rideId, latitude, longitude, speed = 0, accuracy = 0) => {
  try {
    const rideRef = doc(db, "rides", rideId);
    const rideDoc = await getDoc(rideRef);
    
    if (!rideDoc.exists()) {
      throw new Error(`Ride with ID ${rideId} not found`);
    }
    
    const rideData = rideDoc.data();
    
    if (rideData.status !== "active" && !rideData.isActive) {
      throw new Error("Cannot update location for inactive ride");
    }
    
    const currentTimestamp = Date.now();
    const newLocationPoint = {
      accuracy: accuracy,
      latitude: latitude,
      longitude: longitude,
      position: {
        latitude: latitude,
        longitude: longitude
      },
      speed: speed,
      timestamp: currentTimestamp
    };
    
    // Add to path array
    const currentPath = rideData.path || [];
    const updatedPath = [...currentPath, newLocationPoint];
    
    // Calculate updated distance
    let totalDistance = rideData.distanceTraveled || 0;
    if (currentPath.length > 0) {
      const lastPoint = currentPath[currentPath.length - 1];
      totalDistance += getDistanceBetweenPoints(lastPoint, newLocationPoint);
    }
    
    await updateDoc(rideRef, {
      path: updatedPath,
      distanceTraveled: totalDistance,
      // Update current location fields for real-time tracking
      currentLatitude: latitude,
      currentLongitude: longitude,
      currentSpeed: speed,
      locationAccuracy: accuracy,
      lastLocationUpdate: serverTimestamp()
    });
    
    return {
      rideId,
      latitude,
      longitude,
      speed,
      accuracy,
      distanceTraveled: totalDistance
    };
  } catch (error) {
    console.error('Error updating ride location:', error);
    throw error;
  }
};

// Get active rides
export const getActiveRides = async () => {
  try {
    const ridesCollection = collection(db, "rides");
    const q = query(ridesCollection, where("status", "==", "active"));
    const snapshot = await getDocs(q);
    
    return snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startTime: doc.data().startTime ? new Date(doc.data().startTime) : null,
      endTime: doc.data().endTime ? new Date(doc.data().endTime) : null
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
      startTime: doc.data().startTime ? new Date(doc.data().startTime) : null,
      endTime: doc.data().endTime ? new Date(doc.data().endTime) : null
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