import { db } from '../firebase';
import { collection, addDoc, getDocs, doc, updateDoc, deleteDoc, query, orderBy, Timestamp } from 'firebase/firestore';

// Support messages collection
const messagesCollection = collection(db, 'supportMessages');
const faqsCollection = collection(db, 'faqs');

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