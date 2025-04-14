// src/services/bikeService.js
import { db, storage } from '../firebase';
import { collection, getDocs, doc, setDoc, deleteDoc } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { v4 as uuidv4 } from 'uuid';

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