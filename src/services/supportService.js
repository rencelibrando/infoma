import { db } from '../firebase';
import { collection, addDoc, getDocs, doc, updateDoc, deleteDoc, query, orderBy, Timestamp, writeBatch, getDoc, setDoc } from 'firebase/firestore';
import { getStorage, ref as storageRef, uploadBytes, getDownloadURL, listAll, deleteObject } from 'firebase/storage';

// Support messages collection
const messagesCollection = collection(db, 'supportMessages');
const faqsCollection = collection(db, 'faqs');
const storage = getStorage();

// Operating Hours
const operatingHoursCollection = collection(db, 'operating_hours');

export const getOperatingHours = async () => {
  try {
    const querySnapshot = await getDocs(operatingHoursCollection);
    if (querySnapshot.empty) {
      console.log('No operating hours found in Firestore, returning default.');
      return null;
    }
    const hours = [];
    querySnapshot.forEach(doc => {
      hours.push({ ...doc.data() });
    });
    
    const dayOrder = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
    hours.sort((a, b) => dayOrder.indexOf(a.day) - dayOrder.indexOf(b.day));
    
    return hours;
  } catch (error) {
    console.error('Error getting operating hours:', error);
    throw error;
  }
};

export const saveOperatingHours = async (hours) => {
  try {
    const savePromises = hours.map(day => {
      const docRef = doc(operatingHoursCollection, day.day.toLowerCase());
      return setDoc(docRef, day);
    });
    await Promise.all(savePromises);
    return true;
  } catch (error) {
    console.error('Error saving operating hours:', error);
    throw error;
  }
};

// Location
const configCollection = collection(db, 'config');

export const getLocation = async () => {
    try {
        const locationRef = doc(configCollection, 'location');
        const docSnap = await getDoc(locationRef);
        if (docSnap.exists()) {
            return docSnap.data();
        } else {
            console.log('No location data found in Firestore.');
            return {
              name: 'Bambike Ecotours Intramuros',
              address: 'Real St. corner General Luna St.\nIntramuros, Manila 1002\nPhilippines'
            };
        }
    } catch (error) {
        console.error('Error getting location:', error);
        throw error;
    }
};

export const saveLocation = async (locationData) => {
    try {
        const locationRef = doc(configCollection, 'location');
        await setDoc(locationRef, locationData);
        return true;
    } catch (error) {
        console.error('Error saving location:', error);
        throw error;
    }
};

// Get all support messages
export const getMessages = async () => {
  try {
    const q = query(messagesCollection, orderBy('dateCreated', 'desc'));
    const querySnapshot = await getDocs(q);
    
    return querySnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      dateCreated: doc.data().dateCreated?.toDate() || new Date()
    }));
  } catch (error) {
    console.error('Error getting messages:', error);
    throw error;
  }
};

// Update message status
export const updateMessageStatus = async (messageId, newStatus) => {
  try {
    const messageRef = doc(messagesCollection, messageId);
    await updateDoc(messageRef, { 
      status: newStatus,
      lastUpdated: Timestamp.now()
    });
    return true;
  } catch (error) {
    console.error('Error updating message status:', error);
    throw error;
  }
};

// Send a response to a message
export const sendResponse = async (messageId, responseText) => {
  try {
    const messageRef = doc(messagesCollection, messageId);
    await updateDoc(messageRef, {
      response: responseText,
      status: 'resolved',
      respondedAt: Timestamp.now()
    });
    return true;
  } catch (error) {
    console.error('Error sending response:', error);
    throw error;
  }
};

// FAQs
export const getFaqs = async () => {
  try {
    const q = query(faqsCollection, orderBy('order'));
    const querySnapshot = await getDocs(q);
    
    return querySnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));
  } catch (error) {
    console.error('Error getting FAQs:', error);
    throw error;
  }
};

export const addFaq = async (faqData) => {
  try {
    // Get count of existing FAQs for default ordering
    const faqs = await getFaqs();
    const newFaq = {
      ...faqData,
      order: faqs.length + 1,
      createdAt: Timestamp.now()
    };
    
    const docRef = await addDoc(faqsCollection, newFaq);
    return {
      id: docRef.id,
      ...newFaq
    };
  } catch (error) {
    console.error('Error adding FAQ:', error);
    throw error;
  }
};

export const updateFaq = async (faqId, faqData) => {
  try {
    const faqRef = doc(faqsCollection, faqId);
    await updateDoc(faqRef, {
      ...faqData,
      updatedAt: Timestamp.now()
    });
    return true;
  } catch (error) {
    console.error('Error updating FAQ:', error);
    throw error;
  }
};

export const deleteFaq = async (faqId) => {
  try {
    const faqRef = doc(faqsCollection, faqId);
    await deleteDoc(faqRef);
    return true;
  } catch (error) {
    console.error('Error deleting FAQ:', error);
    throw error;
  }
};

// Upload an image to Firebase Storage
export const uploadSupportImage = async (messageId, file) => {
  try {
    const timestamp = Date.now();
    const path = `support_images/${messageId}/admin_${timestamp}_${file.name}`;
    const imageRef = storageRef(storage, path);
    
    // Upload the file
    await uploadBytes(imageRef, file);
    
    // Get the download URL
    const downloadURL = await getDownloadURL(imageRef);
    
    return downloadURL;
  } catch (error) {
    console.error('Error uploading image:', error);
    throw error;
  }
};

// Add a reply to a support message
export const addReply = async (messageId, replyData) => {
  try {
    const repliesCollection = collection(db, 'supportMessages', messageId, 'replies');
    const newReply = {
      ...replyData,
      createdAt: Timestamp.now()
    };
    const docRef = await addDoc(repliesCollection, newReply);
    return { id: docRef.id, ...newReply };
  } catch (error) {
    console.error('Error adding reply:', error);
    throw error;
  }
};

// Add a reply with an image attachment
export const addReplyWithImage = async (messageId, replyData, imageFile) => {
  try {
    let imageUrl = null;
    
    // Upload image if provided
    if (imageFile) {
      imageUrl = await uploadSupportImage(messageId, imageFile);
    }
    
    // Add the reply with the image URL
    const repliesCollection = collection(db, 'supportMessages', messageId, 'replies');
    const newReply = {
      ...replyData,
      imageUrl,
      createdAt: Timestamp.now()
    };
    
    const docRef = await addDoc(repliesCollection, newReply);
    
    // Update message status to in-progress if it was resolved
    const messageRef = doc(messagesCollection, messageId);
    const messageDoc = await messageRef.get();
    if (messageDoc.exists && messageDoc.data().status === 'resolved') {
      await updateDoc(messageRef, { status: 'in-progress' });
    }
    
    return { id: docRef.id, ...newReply };
  } catch (error) {
    console.error('Error adding reply with image:', error);
    throw error;
  }
};

// Get all replies for a support message
export const getReplies = async (messageId) => {
  try {
    const repliesCollection = collection(db, 'supportMessages', messageId, 'replies');
    const q = query(repliesCollection, orderBy('createdAt', 'asc'));
    const querySnapshot = await getDocs(q);
    return querySnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      createdAt: doc.data().createdAt?.toDate() || new Date()
    }));
  } catch (error) {
    console.error('Error getting replies:', error);
    throw error;
  }
};

// Delete a support message and all its replies and attachments
export const deleteMessage = async (messageId) => {
  try {
    const batch = writeBatch(db);
    
    // Delete all replies
    const repliesCollection = collection(db, 'supportMessages', messageId, 'replies');
    const repliesSnapshot = await getDocs(repliesCollection);
    
    repliesSnapshot.docs.forEach((replyDoc) => {
      batch.delete(doc(db, 'supportMessages', messageId, 'replies', replyDoc.id));
    });
    
    // Delete the message itself
    batch.delete(doc(db, 'supportMessages', messageId));
    
    // Commit the batch
    await batch.commit();
    
    // Delete any images associated with the message
    try {
      const imagesRef = storageRef(storage, `support_images/${messageId}`);
      const imagesList = await listAll(imagesRef);
      
      const deletePromises = imagesList.items.map(imageRef => {
        return deleteObject(imageRef);
      });
      
      await Promise.all(deletePromises);
    } catch (storageError) {
      console.log('No images found or error deleting images:', storageError);
      // Continue even if image deletion fails
    }
    
    return true;
  } catch (error) {
    console.error('Error deleting message:', error);
    throw error;
  }
}; 