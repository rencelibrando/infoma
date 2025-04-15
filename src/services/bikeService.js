// src/services/bikeService.js
import { db, storage } from '../firebase';
import { collection, getDocs, doc, setDoc, deleteDoc, updateDoc } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { v4 as uuidv4 } from 'uuid';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';

// Install uuid first: npm install uuid

// Get all bikes
export const getBikes = async () => {
  const bikesCollection = collection(db, "bikes");
  const snapshot = await getDocs(bikesCollection);
  return snapshot.docs.map(doc => ({
    id: doc.id,
    ...doc.data()
  }));
};

// Upload a bike
export const uploadBike = async (bike, imageFile) => {
  const id = uuidv4();
  
  // Upload image to Firebase Storage
  const storageRef = ref(storage, `bikes/${id}.jpg`);
  await uploadBytes(storageRef, imageFile);
  const imageUrl = await getDownloadURL(storageRef);
  
  // Create bike object (matching your Android structure)
  const bikeData = {
    id,
    name: bike.name,
    type: bike.type,
    price: `₱${bike.price}/hr`,
    priceValue: parseFloat(bike.price),
    imageUrl,
    latitude: bike.latitude,
    longitude: bike.longitude,
    description: bike.description,
    isAvailable: true,
    isInUse: false
  };
  
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
    
    // Prepare update data
    const updatedBike = {
      name: bikeData.name,
      type: bikeData.type,
      priceValue: parseFloat(bikeData.price),
      price: `₱${bikeData.price}/hr`,
      description: bikeData.description,
      isAvailable: bikeData.isAvailable,
      updatedAt: new Date(),
      latitude: bikeData.latitude,
      longitude: bikeData.longitude
    };

    // Upload new image if provided
    if (imageFile) {
      const storageRef = ref(storage, `bikes/${bikeId}_${Date.now()}`);
      await uploadBytes(storageRef, imageFile);
      const imageUrl = await getDownloadURL(storageRef);
      updatedBike.imageUrl = imageUrl;
    }

    // Update Firestore document
    await updateDoc(bikeRef, updatedBike);
    return { id: bikeId, ...updatedBike };
  } catch (error) {
    console.error('Error updating bike:', error);
    throw error;
  }
};