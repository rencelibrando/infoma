// src/services/bikeService.js
import { db, storage, auth } from '../firebase';
import { collection, getDocs, doc, setDoc, deleteDoc, updateDoc, getDoc, onSnapshot, query, where, addDoc, serverTimestamp } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { v4 as uuidv4 } from 'uuid';

// Install uuid first: npm install uuid

// Secure characters for QR code generation (excluding confusing characters)
const SECURE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

/**
 * Generate a cryptographically secure QR code
 * Format: BIKE-XXXXXXXX where X is a secure random character
 */
const generateSecureQRCode = async () => {
  let attempts = 0;
  const maxAttempts = 10;
  
  while (attempts < maxAttempts) {
    // Generate a secure 8-character code
    const code = Array.from({ length: 8 }, () => 
      SECURE_CHARS[Math.floor(Math.random() * SECURE_CHARS.length)]
    ).join('');
    
    const qrCode = `BIKE-${code}`;
    
    // Check for collisions in Firestore
    const isUnique = await checkQRCodeUnique(qrCode);
    if (isUnique) {
      return qrCode;
    }
    
    attempts++;
    console.warn(`QR code collision detected: ${qrCode}, attempt ${attempts}`);
  }
  
  // Fallback to UUID-based generation if too many collisions
  const fallbackCode = uuidv4().replace(/-/g, '').substring(0, 8).toUpperCase();
  return `BIKE-${fallbackCode}`;
};

/**
 * Check if a QR code is unique across all bikes
 * @deprecated Use exported isQRCodeUnique instead
 */
const checkQRCodeUnique = async (qrCode, excludeBikeId = null) => {
  try {
    // Check qrCode field
    const qrCodeQuery = query(
      collection(db, "bikes"), 
      where("qrCode", "==", qrCode),
      ...(excludeBikeId ? [where("id", "!=", excludeBikeId)] : [])
    );
    const qrCodeSnapshot = await getDocs(qrCodeQuery);
    
    // Check hardwareId field for backward compatibility
    const hardwareIdQuery = query(
      collection(db, "bikes"), 
      where("hardwareId", "==", qrCode),
      ...(excludeBikeId ? [where("id", "!=", excludeBikeId)] : [])
    );
    const hardwareIdSnapshot = await getDocs(hardwareIdQuery);
    
    return qrCodeSnapshot.empty && hardwareIdSnapshot.empty;
  } catch (error) {
    console.error('Error checking QR code uniqueness:', error);
    return false;
  }
};

/**
 * Generate hardware ID for backward compatibility
 * @deprecated Use generateSecureQRCode instead
 */
const generateHardwareId = async () => {
  console.warn('generateHardwareId is deprecated. Use generateSecureQRCode instead.');
  return await generateSecureQRCode();
};

// Helper function to check authentication with better error handling
const checkAuth = () => {
  const user = auth.currentUser;
  if (!user) {
    // Don't throw an error immediately, log it for debugging
    console.warn('User not authenticated in bikeService checkAuth()');
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

// Get all bikes
export const getAllBikes = async () => {
  try {
    // Check authentication - but don't fail immediately
    const user = checkAuth();
    if (!user) {
      console.warn('Attempting to get bikes without authentication');
      // Try to proceed anyway for admin dashboard
    }
    
    const bikesRef = collection(db, 'bikes');
    const snapshot = await getDocs(bikesRef);
    return snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));
  } catch (error) {
    console.error('Error fetching bikes:', error);
    throw error;
  }
};

// Get a single bike by ID
export const getBikeById = async (id) => {
  try {
    // Check authentication - but don't fail immediately
    const user = checkAuth();
    if (!user) {
      console.warn('Attempting to get bike by ID without authentication');
      // Try to proceed anyway for public bike data
    }
    
    const bikeRef = doc(db, 'bikes', id);
    const bikeDoc = await getDoc(bikeRef);
    if (bikeDoc.exists()) {
      return { id: bikeDoc.id, ...bikeDoc.data() };
    } else {
      throw new Error('Bike not found');
    }
  } catch (error) {
    console.error('Error fetching bike:', error);
    throw error;
  }
};

// Upload a bike
export const uploadBike = async (bike, imageFile) => {
  const id = uuidv4();
  
  // Handle QR code (primary field)
  let qrCode;
  if (bike.qrCode && bike.qrCode.trim()) {
    // User provided QR code
    qrCode = bike.qrCode.trim();
    
    // Validate uniqueness
    const isUnique = await checkQRCodeUnique(qrCode);
    if (!isUnique) {
      throw new Error(`QR code "${qrCode}" is already in use by another bike`);
    }
  } else {
    // Generate secure QR code
    qrCode = await generateSecureQRCode();
  }
  
  // Handle hardware ID (backward compatibility)
  let hardwareId;
  if (bike.hardwareId && bike.hardwareId.trim()) {
    // User provided hardware ID
    hardwareId = bike.hardwareId.trim();
    
    // Validate uniqueness
    const isUnique = await checkQRCodeUnique(hardwareId);
    if (!isUnique) {
      throw new Error(`Hardware ID "${hardwareId}" is already in use by another bike`);
    }
  } else {
    // Generate hardware ID for backward compatibility
    hardwareId = await generateHardwareId();
  }
  
  // Validation: Ensure at least one identifier exists
  if (!qrCode && !hardwareId) {
    throw new Error('At least one identifier (QR Code or Hardware ID) is required');
  }
  
  // Upload image to Firebase Storage
  const storageRef = ref(storage, `bikes/${id}.jpg`);
  await uploadBytes(storageRef, imageFile);
  const imageUrl = await getDownloadURL(storageRef);
  
  // Set the initial lock state
  const isLocked = true; // All new bikes start as locked
  const isInUse = false; // Not in use initially
  const isAvailable = isLocked && !isInUse; // Available if locked and not in use
  
  // Create bike object (enhanced with both qrCode and hardwareId)
  const bikeData = {
    id,
    qrCode,              // Primary QR code field
    hardwareId,          // Backward compatibility
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
    batteryLevel: 100,   // Default battery level
    rating: 0,           // Default rating
    totalRides: 0,       // Initialize ride counter
    createdAt: new Date(),
    lastUpdated: new Date()
  };
  
  console.log('Creating new bike with enhanced data:', {
    id: bikeData.id,
    qrCode: bikeData.qrCode,
    hardwareId: bikeData.hardwareId,
    isLocked: bikeData.isLocked,
    isAvailable: bikeData.isAvailable,
    isInUse: bikeData.isInUse
  });
  
  // Save to Firestore
  await setDoc(doc(db, "bikes", id), bikeData);
  
  return bikeData;
};

// Delete a bike
export const deleteBike = async (id) => {
  try {
    // Check authentication first
    const user = requireAuth();
    
    const bikeRef = doc(db, 'bikes', id);
    await deleteDoc(bikeRef);
    console.log('Bike deleted successfully');
  } catch (error) {
    console.error('Error deleting bike:', error);
    throw error;
  }
};

// Function to update an existing bike
export const updateBike = async (id, bikeData, imageFile) => {
  try {
    // Check authentication first
    const user = requireAuth();
    
    const bikeRef = doc(db, 'bikes', id);
    const currentBikeDoc = await getDoc(bikeRef);
    const currentBike = currentBikeDoc.exists() ? currentBikeDoc.data() : {};
    
    // Enforce rule: if bike is available it must be locked
    const isLocked = bikeData.isLocked !== undefined ? bikeData.isLocked : currentBike.isLocked;
    const isInUse = currentBike.isInUse || false;
    let isAvailable = bikeData.isAvailable;
    
    // Rule: All bikes in use must be unlocked
    if (isInUse && isLocked) {
      console.warn(`Bike ${id} is in use but was being set to locked. Forcing unlock.`);
      bikeData.isLocked = false;
    }
    
    // If trying to make a bike available but it's not locked, force lock it
    if (isAvailable && !isLocked) {
      console.warn(`Bike ${id} is being marked as available but wasn't locked. Forcing lock state.`);
      bikeData.isLocked = true;
    }
    
    // If bike is not locked, it cannot be available
    if (!bikeData.isLocked && isAvailable) {
      isAvailable = false;
      console.warn(`Bike ${id} cannot be available when unlocked. Setting to unavailable.`);
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
      lastUpdated: new Date(),
      latitude: bikeData.latitude,
      longitude: bikeData.longitude
    };

    // Handle QR code field (primary identifier)
    if (bikeData.qrCode !== undefined) {
      if (bikeData.qrCode.trim()) {
        // User provided a QR code
        updatedBike.qrCode = bikeData.qrCode.trim();
      } else {
        // User cleared the QR code - only allowed if hardwareId exists
        if (bikeData.hardwareId && bikeData.hardwareId.trim()) {
          updatedBike.qrCode = null;
        }
      }
    } else if (!currentBike.qrCode) {
      // Generate QR code if not present and not provided by user
      updatedBike.qrCode = await generateSecureQRCode();
      console.log(`Generated new QR code for bike ${id}: ${updatedBike.qrCode}`);
    }

    // Handle hardware ID field (legacy compatibility)
    if (bikeData.hardwareId !== undefined) {
      if (bikeData.hardwareId.trim()) {
        // User provided a hardware ID
        updatedBike.hardwareId = bikeData.hardwareId.trim();
      } else {
        // User cleared the hardware ID - only allowed if qrCode exists
        if ((bikeData.qrCode && bikeData.qrCode.trim()) || currentBike.qrCode) {
          updatedBike.hardwareId = null;
        }
      }
    } else if (!currentBike.hardwareId) {
      // Generate hardware ID if not present and not provided by user (backward compatibility)
      updatedBike.hardwareId = await generateHardwareId();
      console.log(`Generated new hardware ID for bike ${id}: ${updatedBike.hardwareId}`);
    }

    // Validation: Ensure at least one identifier exists
    const finalQrCode = updatedBike.qrCode !== undefined ? updatedBike.qrCode : currentBike.qrCode;
    const finalHardwareId = updatedBike.hardwareId !== undefined ? updatedBike.hardwareId : currentBike.hardwareId;
    
    if (!finalQrCode && !finalHardwareId) {
      throw new Error('At least one identifier (QR Code or Hardware ID) is required');
    }

    // Upload new image if provided
    if (imageFile) {
      const storageRef = ref(storage, `bikes/${id}_${Date.now()}`);
      await uploadBytes(storageRef, imageFile);
      const imageUrl = await getDownloadURL(storageRef);
      updatedBike.imageUrl = imageUrl;
    }

    // Update Firestore document
    await updateDoc(bikeRef, updatedBike);
    return { 
      id, 
      ...currentBike,
      ...updatedBike 
    };
  } catch (error) {
    console.error('Error updating bike:', error);
    throw error;
  }
};

// Function to update or generate QR codes and hardware IDs for existing bikes
export const updateBikesWithHardwareIds = async () => {
  try {
    // Check authentication first
    const user = requireAuth();
    
    const bikes = await getAllBikes();
    const updates = [];
    
    for (const bike of bikes) {
      const bikeRef = doc(db, "bikes", bike.id);
      const updateData = {};
      
      // Check for missing QR code (primary field)
      if (!bike.qrCode) {
        updateData.qrCode = await generateSecureQRCode();
        console.log(`Generated QR code for bike ${bike.id}: ${updateData.qrCode}`);
      }
      
      // Check for missing hardware ID (backward compatibility)
      if (!bike.hardwareId) {
        updateData.hardwareId = await generateHardwareId();
        console.log(`Generated hardware ID for bike ${bike.id}: ${updateData.hardwareId}`);
      }
      
      // Check for missing isLocked field - add it based on isAvailable
      if (bike.isLocked === undefined) {
        // If a bike is available, it should be locked (business rule)
        updateData.isLocked = bike.isAvailable || false;
        console.log(`Adding missing isLocked field to bike ${bike.id}, setting to ${updateData.isLocked}`);
      }
      
      // Add missing battery level
      if (bike.batteryLevel === undefined) {
        updateData.batteryLevel = 100;
      }
      
      // Add missing rating
      if (bike.rating === undefined) {
        updateData.rating = 0;
      }
      
      // Add missing total rides counter
      if (bike.totalRides === undefined) {
        updateData.totalRides = 0;
      }
      
      // Add last updated timestamp
      updateData.lastUpdated = new Date();
      
      // Only update if there are fields to update
      if (Object.keys(updateData).length > 1) { // > 1 because lastUpdated is always added
        await updateDoc(bikeRef, updateData);
        updates.push({
          id: bike.id,
          updates: updateData
        });
        console.log(`Updated bike ${bike.id} with:`, updateData);
      }
    }
    
    console.log(`Updated ${updates.length} bikes with missing fields`);
    return updates;
    
  } catch (error) {
    console.error('Error updating bikes with hardware IDs:', error);
    throw error;
  }
};

// Function to toggle the lock state of a bike
export const toggleBikeLock = async (bikeId, lockState) => {
  try {
    // Check authentication first
    const user = requireAuth();
    
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
    // Check authentication first
    const user = requireAuth();
    
    console.log("Running bike data consistency check...");
    const bikes = await getAllBikes();
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
    
    return await getAllBikes();
  } catch (error) {
    console.error('Error ensuring bike data consistency:', error);
    throw error;
  }
};

// Call this function when initializing to ensure all bikes are consistent
export const initializeBikesData = async () => {
  try {
    // Check authentication first
    const user = requireAuth();
    
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
    // Check authentication first
    const user = requireAuth();
    
    console.log("Setting up real-time listener for bikes...");
    const bikesCollection = collection(db, "bikes");
    const bikesQuery = query(bikesCollection);
    
    // Create a real-time listener for the bikes collection
    const unsubscribe = onSnapshot(bikesQuery, (snapshot) => {
      const bikes = snapshot.docs.map(doc => {
        const data = doc.data();
        
        // Process coordinates like in getAllBikes
        let latitude = data.latitude;
        let longitude = data.longitude;
        
        // Convert string coordinates to numbers
        if (typeof latitude === 'string') {
          latitude = parseFloat(latitude);
        }
        
        if (typeof longitude === 'string') {
          longitude = parseFloat(longitude);
        }
        
        // Ensure they're valid numbers
        if (isNaN(latitude)) latitude = null;
        if (isNaN(longitude)) longitude = null;
        
        return {
          id: doc.id,
          ...data,
          latitude,
          longitude
        };
      });
      
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

// Function to fix bikes with missing coordinates
export const fixBikeCoordinates = async () => {
  try {
    // Check authentication first
    const user = requireAuth();
    
    const bikes = await getAllBikes();
    const defaultCoords = { latitude: 14.554729, longitude: 121.0244 }; // Default Manila coords
    let fixedCount = 0;
    
    for (const bike of bikes) {
      if (!bike.latitude || !bike.longitude) {
        console.log(`Fixing coordinates for bike ${bike.id} (${bike.name || 'Unnamed'})`);
        
        // Spread bikes around the default location if needed
        const randomOffset = () => (Math.random() - 0.5) * 0.01; // Small offset
        
        const bikeRef = doc(db, "bikes", bike.id);
        await updateDoc(bikeRef, {
          latitude: defaultCoords.latitude + randomOffset(),
          longitude: defaultCoords.longitude + randomOffset()
        });
        
        fixedCount++;
      }
    }
    
    console.log(`Fixed coordinates for ${fixedCount} bikes`);
    return fixedCount;
  } catch (error) {
    console.error('Error fixing bike coordinates:', error);
    throw error;
  }
};

// Function to update bike status based on booking changes
export const updateBikeStatus = async (bikeId, statusUpdate) => {
  try {
    // Check authentication first
    const user = requireAuth();
    
    const bikeRef = doc(db, "bikes", bikeId);
    const bikeDoc = await getDoc(bikeRef);
    
    if (!bikeDoc.exists()) {
      console.error(`Bike with ID ${bikeId} not found`);
      return null;
    }
    
    const currentBike = bikeDoc.data();
    
    // Create update data with proper fallbacks for undefined values
    const updateData = {};
    
    // Only update fields that are explicitly provided and not undefined
    if (statusUpdate.isAvailable !== undefined) {
      updateData.isAvailable = Boolean(statusUpdate.isAvailable);
    }
    
    if (statusUpdate.isInUse !== undefined) {
      updateData.isInUse = Boolean(statusUpdate.isInUse);
    }
    
    // Determine lock state based on availability
    // Business rule: available bikes should be locked, bikes in use should be unlocked
    if (statusUpdate.isAvailable !== undefined || statusUpdate.isInUse !== undefined) {
      const finalIsAvailable = statusUpdate.isAvailable !== undefined ? 
        Boolean(statusUpdate.isAvailable) : 
        Boolean(currentBike.isAvailable);
      
      const finalIsInUse = statusUpdate.isInUse !== undefined ? 
        Boolean(statusUpdate.isInUse) : 
        Boolean(currentBike.isInUse);
      
      // Set lock state: available bikes are locked, bikes in use are unlocked
      if (finalIsAvailable && !finalIsInUse) {
        updateData.isLocked = true;  // Available bikes should be locked
      } else if (finalIsInUse) {
        updateData.isLocked = false; // Bikes in use should be unlocked
      } else {
        // Preserve current lock state if unclear
        updateData.isLocked = Boolean(currentBike.isLocked);
      }
    }
    
    // Always add timestamp
    updateData.updatedAt = new Date();
    
    console.log(`Updating bike ${bikeId} status:`, updateData);
    
    // Only proceed with update if we have data to update
    if (Object.keys(updateData).length > 1) { // More than just updatedAt
      await updateDoc(bikeRef, updateData);
      
      return {
        id: bikeId,
        ...currentBike,
        ...updateData
      };
    } else {
      console.log(`No valid updates provided for bike ${bikeId}`);
      return {
        id: bikeId,
        ...currentBike,
        updatedAt: updateData.updatedAt
      };
    }
  } catch (error) {
    console.error('Error updating bike status:', error);
    throw error;
  }
};

// QR Code and Hardware ID validation utilities
export const validateBikeIdentifiers = async (qrCode, hardwareId, excludeBikeId = null) => {
  try {
    // Check authentication first
    const user = requireAuth();
    
    const errors = [];
    
    // Ensure at least one identifier exists
    if (!qrCode && !hardwareId) {
      errors.push('At least one identifier (QR Code or Hardware ID) is required');
      return { isValid: false, errors };
    }
    
    // Validate QR code format and uniqueness
    if (qrCode) {
      const trimmedQRCode = qrCode.trim();
      
      // Check format (alphanumeric, 8-16 characters)
      if (!/^[A-Za-z0-9]{8,16}$/.test(trimmedQRCode)) {
        errors.push('QR Code must be 8-16 alphanumeric characters');
      }
      
      // Check uniqueness
      const isUnique = await isQRCodeUnique(trimmedQRCode, excludeBikeId);
      if (!isUnique) {
        errors.push(`QR code "${trimmedQRCode}" is already in use by another bike`);
      }
    }
    
    // Validate hardware ID format and uniqueness
    if (hardwareId) {
      const trimmedHardwareId = hardwareId.trim();
      
      // Check format (alphanumeric, 6-20 characters)
      if (!/^[A-Za-z0-9]{6,20}$/.test(trimmedHardwareId)) {
        errors.push('Hardware ID must be 6-20 alphanumeric characters');
      }
      
      // Check uniqueness
      const isUnique = await isQRCodeUnique(trimmedHardwareId, excludeBikeId);
      if (!isUnique) {
        errors.push(`Hardware ID "${trimmedHardwareId}" is already in use by another bike`);
      }
    }
    
    return {
      isValid: errors.length === 0,
      errors,
      normalizedQRCode: qrCode ? qrCode.trim() : null,
      normalizedHardwareId: hardwareId ? hardwareId.trim() : null
    };
  } catch (error) {
    console.error('Error validating bike identifiers:', error);
    throw error;
  }
};

// Enhanced uniqueness check that can exclude a specific bike ID
export const isQRCodeUnique = async (identifier, excludeBikeId = null) => {
  try {
    // Check authentication first
    const user = requireAuth();
    
    // Check in qrCode field
    const qrCodeQuery = query(
      collection(db, "bikes"),
      where("qrCode", "==", identifier),
      ...(excludeBikeId ? [where("id", "!=", excludeBikeId)] : [])
    );
    const qrCodeSnapshot = await getDocs(qrCodeQuery);
    
    // Check in hardwareId field for backward compatibility
    const hardwareIdQuery = query(
      collection(db, "bikes"),
      where("hardwareId", "==", identifier),
      ...(excludeBikeId ? [where("id", "!=", excludeBikeId)] : [])
    );
    const hardwareIdSnapshot = await getDocs(hardwareIdQuery);
    
    return qrCodeSnapshot.empty && hardwareIdSnapshot.empty;
  } catch (error) {
    console.error('Error checking identifier uniqueness:', error);
    return false; // Assume not unique to be safe
  }
};

// Update the subscribeToBikes function to normalize coordinates