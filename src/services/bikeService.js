// src/services/bikeService.js
import { db, storage } from '../firebase';
import { collection, getDocs, doc, setDoc, deleteDoc, updateDoc, getDoc, onSnapshot, query } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { v4 as uuidv4 } from 'uuid';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';

// Install uuid first: npm install uuid

// Generate a hardware ID format: BIKE-XXXX where X is alphanumeric
const generateHardwareId = () => {
  // Generate a 4-character alphanumeric string
  const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let result = 'BIKE-';
  for (let i = 0; i < 4; i++) {
    result += characters.charAt(Math.floor(Math.random() * characters.length));
  }
  return result;
};

// Get all bikes
export const getBikes = async () => {
  try {
    const bikesCollection = collection(db, "bikes");
    const snapshot = await getDocs(bikesCollection);
    const bikes = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));
    
    // Log each bike's status to help with debugging
    bikes.forEach(bike => {
      console.log(`Bike ${bike.id} (${bike.name}) status:`, {
        isLocked: bike.isLocked,
        isAvailable: bike.isAvailable,
        isInUse: bike.isInUse
      });
    });
    
    return bikes;
  } catch (error) {
    console.error('Error getting bikes:', error);
    throw error;
  }
};

// Get a single bike by ID
export const getBikeById = async (bikeId) => {
  const bikeRef = doc(db, "bikes", bikeId);
  const bikeDoc = await getDoc(bikeRef);
  
  if (bikeDoc.exists()) {
    return {
      id: bikeDoc.id,
      ...bikeDoc.data()
    };
  }
  
  return null;
};

// Upload a bike
export const uploadBike = async (bike, imageFile) => {
  const id = uuidv4();
  const hardwareId = generateHardwareId();
  
  // Upload image to Firebase Storage
  const storageRef = ref(storage, `bikes/${id}.jpg`);
  await uploadBytes(storageRef, imageFile);
  const imageUrl = await getDownloadURL(storageRef);
  
  // Set the initial lock state
  const isLocked = true; // All new bikes start as locked
  const isInUse = false; // Not in use initially
  const isAvailable = isLocked && !isInUse; // Available if locked and not in use
  
  // Create bike object (matching your Android structure)
  const bikeData = {
    id,
    hardwareId,
    name: bike.name,
    type: bike.type,
    price: `₱${bike.price}/hr`,
    priceValue: parseFloat(bike.price),
    imageUrl,
    latitude: bike.latitude,
    longitude: bike.longitude,
    description: bike.description,
    isAvailable: isAvailable,
    isInUse: isInUse,
    isLocked: isLocked,
    createdAt: new Date()
  };
  
  console.log('Creating new bike with status:', {
    isLocked: bikeData.isLocked,
    isAvailable: bikeData.isAvailable,
    isInUse: bikeData.isInUse
  });
  
  // Save to Firestore
  await setDoc(doc(db, "bikes", id), bikeData);
  
  return bikeData;
};

// Delete a bike
export const deleteBike = async (bikeId) => {
  await deleteDoc(doc(db, "bikes", bikeId));
};

// Function to update an existing bike
export const updateBike = async (bikeId, bikeData, imageFile) => {
  try {
    const db = getFirestore();
    const storage = getStorage();
    const bikesRef = collection(db, 'bikes');
    const bikeRef = doc(bikesRef, bikeId);
    
    // Get current bike data to check if it already has a hardware ID
    const currentBikeDoc = await getDoc(bikeRef);
    const currentBike = currentBikeDoc.exists() ? currentBikeDoc.data() : {};
    
    // Enforce rule: if bike is available it must be locked
    const isLocked = bikeData.isLocked !== undefined ? bikeData.isLocked : currentBike.isLocked;
    const isInUse = currentBike.isInUse || false;
    let isAvailable = bikeData.isAvailable;
    
    // Rule: All bikes in use must be unlocked
    if (isInUse && isLocked) {
      console.warn(`Bike ${bikeId} is in use but was being set to locked. Forcing unlock.`);
      bikeData.isLocked = false;
    }
    
    // If trying to make a bike available but it's not locked, force lock it
    if (isAvailable && !isLocked) {
      console.warn(`Bike ${bikeId} is being marked as available but wasn't locked. Forcing lock state.`);
      bikeData.isLocked = true;
    }
    
    // If bike is not locked, it cannot be available
    if (!bikeData.isLocked && isAvailable) {
      isAvailable = false;
      console.warn(`Bike ${bikeId} cannot be available when unlocked. Setting to unavailable.`);
    }
    
    // Prepare update data
    const updatedBike = {
      name: bikeData.name,
      type: bikeData.type,
      priceValue: parseFloat(bikeData.price),
      price: `₱${bikeData.price}/hr`,
      description: bikeData.description,
      isAvailable: isAvailable,
      isLocked: bikeData.isLocked !== undefined ? bikeData.isLocked : currentBike.isLocked,
      updatedAt: new Date(),
      latitude: bikeData.latitude,
      longitude: bikeData.longitude
    };

    // Generate hardware ID if not already present
    if (!currentBike.hardwareId) {
      updatedBike.hardwareId = generateHardwareId();
    }

    // Upload new image if provided
    if (imageFile) {
      const storageRef = ref(storage, `bikes/${bikeId}_${Date.now()}`);
      await uploadBytes(storageRef, imageFile);
      const imageUrl = await getDownloadURL(storageRef);
      updatedBike.imageUrl = imageUrl;
    }

    // Update Firestore document
    await updateDoc(bikeRef, updatedBike);
    return { 
      id: bikeId, 
      ...currentBike,
      ...updatedBike 
    };
  } catch (error) {
    console.error('Error updating bike:', error);
    throw error;
  }
};

// Function to update or generate hardware IDs for existing bikes
export const updateBikesWithHardwareIds = async () => {
  try {
    const bikes = await getBikes();
    const updates = [];
    
    for (const bike of bikes) {
      const bikeRef = doc(db, "bikes", bike.id);
      const updateData = {};
      
      // Check for missing hardware ID
      if (!bike.hardwareId) {
        updateData.hardwareId = generateHardwareId();
      }
      
      // Check for missing isLocked field - add it based on isAvailable
      if (bike.isLocked === undefined) {
        // If a bike is available, it should be locked (business rule)
        updateData.isLocked = bike.isAvailable || false;
        console.log(`Adding missing isLocked field to bike ${bike.id}, setting to ${updateData.isLocked}`);
      }
      
      // Only update if we have changes
      if (Object.keys(updateData).length > 0) {
        updates.push(updateDoc(bikeRef, updateData));
      }
    }
    
    await Promise.all(updates);
    return await getBikes();
  } catch (error) {
    console.error('Error updating bikes with hardware IDs:', error);
    throw error;
  }
};

// Function to toggle the lock state of a bike
export const toggleBikeLock = async (bikeId, lockState) => {
  try {
    const bikeRef = doc(db, "bikes", bikeId);
    const bikeDoc = await getDoc(bikeRef);
    
    if (!bikeDoc.exists()) {
      throw new Error(`Bike with ID ${bikeId} not found`);
    }
    
    const bikeData = bikeDoc.data();
    
    // Handle case where isLocked field doesn't exist yet
    const currentLockState = bikeData.isLocked !== undefined ? bikeData.isLocked : bikeData.isAvailable;
    
    // Check if bike is in use and trying to lock it
    if (bikeData.isInUse && lockState) {
      throw new Error("Cannot lock a bike that is currently in use");
    }
    
    // Display the state change
    console.log(`Changing bike ${bikeId} lock state from ${currentLockState} to ${lockState}`);
    
    // Update availability based on lock state and in-use status
    // A bike is available if it is locked and not in use
    const isAvailable = lockState && !bikeData.isInUse;
    
    console.log(`Setting bike ${bikeId} to:`, {
      isLocked: lockState,
      isAvailable: isAvailable,
      isInUse: bikeData.isInUse
    });
    
    // Warning: if unlocking a bike, it will be marked as unavailable (unless in use)
    if (!lockState && !bikeData.isInUse) {
      console.warn(`Warning: Bike ${bikeId} is being unlocked which will mark it as unavailable.`);
    }
    
    // Update the bike document in Firestore
    await updateDoc(bikeRef, {
      isLocked: lockState,
      isAvailable: isAvailable,
      lastUpdated: new Date()
    });
    
    // Get the updated document to ensure we return the correct state
    const updatedBikeDoc = await getDoc(bikeRef);
    const updatedBikeData = updatedBikeDoc.data();
    
    return {
      id: bikeId,
      ...updatedBikeData
    };
  } catch (error) {
    console.error('Error toggling bike lock state:', error);
    throw error;
  }
};

// Function to ensure data consistency across all bikes
export const ensureBikeDataConsistency = async () => {
  try {
    console.log("Running bike data consistency check...");
    const bikes = await getBikes();
    const updates = [];
    
    for (const bike of bikes) {
      const bikeRef = doc(db, "bikes", bike.id);
      const updateData = {};
      let needsUpdate = false;
      
      // If isLocked field doesn't exist, add it
      if (bike.isLocked === undefined) {
        updateData.isLocked = bike.isAvailable || false;
        needsUpdate = true;
      }
      
      // Rule 1: All available bikes must be locked
      if (bike.isAvailable && !bike.isLocked) {
        console.warn(`Inconsistency detected: Bike ${bike.id} is available but not locked. Fixing...`);
        updateData.isLocked = true;
        needsUpdate = true;
      }
      
      // Rule 2: All unlocked bikes must be unavailable (if not in use)
      if (!bike.isLocked && bike.isAvailable && !bike.isInUse) {
        console.warn(`Inconsistency detected: Bike ${bike.id} is unlocked but marked available. Fixing...`);
        updateData.isAvailable = false;
        needsUpdate = true;
      }
      
      // Rule 3: All bikes in use must be unlocked
      if (bike.isInUse && bike.isLocked) {
        console.warn(`Inconsistency detected: Bike ${bike.id} is in use but locked. Fixing...`);
        updateData.isLocked = false;
        needsUpdate = true;
      }
      
      // Only update if fixes are needed
      if (needsUpdate) {
        console.log(`Updating bike ${bike.id} with fixed data:`, updateData);
        updates.push(updateDoc(bikeRef, updateData));
      }
    }
    
    if (updates.length > 0) {
      await Promise.all(updates);
      console.log(`Fixed ${updates.length} bikes with data inconsistencies.`);
    } else {
      console.log("No data inconsistencies found.");
    }
    
    return await getBikes();
  } catch (error) {
    console.error('Error ensuring bike data consistency:', error);
    throw error;
  }
};

// Call this function when initializing to ensure all bikes are consistent
export const initializeBikesData = async () => {
  try {
    // First ensure all bikes have hardware IDs
    await updateBikesWithHardwareIds();
    
    // Then ensure data consistency
    return await ensureBikeDataConsistency();
  } catch (error) {
    console.error('Error initializing bikes data:', error);
    throw error;
  }
};

// Function to subscribe to real-time bike updates
export const subscribeToBikes = (callback) => {
  try {
    console.log("Setting up real-time listener for bikes...");
    const bikesCollection = collection(db, "bikes");
    const bikesQuery = query(bikesCollection);
    
    // Create a real-time listener for the bikes collection
    const unsubscribe = onSnapshot(bikesQuery, (snapshot) => {
      const bikes = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      
      console.log(`Real-time update received: ${bikes.length} bikes`);
      callback(bikes);
    }, (error) => {
      console.error("Error in real-time bike listener:", error);
    });
    
    // Return the unsubscribe function to clean up the listener when no longer needed
    return unsubscribe;
  } catch (error) {
    console.error("Error setting up bike subscription:", error);
    throw error;
  }
};