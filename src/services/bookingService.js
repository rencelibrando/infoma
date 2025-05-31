import { db, auth } from '../firebase';
import { 
  collection, 
  addDoc, 
  getDocs, 
  getDoc, 
  updateDoc, 
  deleteDoc, 
  doc, 
  query, 
  where, 
  orderBy, 
  serverTimestamp,
  Timestamp,
  collectionGroup,
  setDoc
} from "firebase/firestore";

// Helper function to check authentication
const checkAuth = () => {
  const user = auth.currentUser;
  if (!user) {
    throw new Error('User not authenticated. Please log in to access this resource.');
  }
  return user;
};

// Get all bookings from all collections and subcollections
export const getAllBookingsFromAllCollections = async () => {
  try {
    checkAuth();
    
    // Get bookings from main collection
    const mainBookingsCollection = collection(db, "bookings");
    const mainSnapshot = await getDocs(mainBookingsCollection);
    
    const mainBookings = mainSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startDate: doc.data().startDate?.toDate ? doc.data().startDate?.toDate() : doc.data().startDate,
      endDate: doc.data().endDate?.toDate ? doc.data().endDate?.toDate() : doc.data().endDate,
      createdAt: doc.data().createdAt,
      source: 'main'
    }));

    // Get bookings from all subcollections using collectionGroup
    const bookingsGroupRef = collectionGroup(db, "bookings");
    const subcollectionSnapshot = await getDocs(bookingsGroupRef);
    
    const subcollectionBookings = subcollectionSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startDate: doc.data().startDate?.toDate ? doc.data().startDate?.toDate() : doc.data().startDate,
      endDate: doc.data().endDate?.toDate ? doc.data().endDate?.toDate() : doc.data().endDate,
      createdAt: doc.data().createdAt,
      source: 'subcollection'
    }));

    // Merge all bookings, avoiding duplicates
    const mergedBookings = [...mainBookings];
    
    // Add subcollection bookings that don't already exist in the main collection
    subcollectionBookings.forEach(subcollectionBooking => {
      if (!mergedBookings.some(booking => booking.id === subcollectionBooking.id)) {
        mergedBookings.push(subcollectionBooking);
      }
    });

    return mergedBookings;
  } catch (error) {
    throw error;
  }
};

// Get all bookings
export const getAllBookings = async () => {
  try {
    checkAuth();
    
    // Use the new function that fetches from all collections
    return await getAllBookingsFromAllCollections();
  } catch (error) {
    // Fallback to main collection only if collectionGroup fails
    try {
      const bookingsCollection = collection(db, "bookings");
      const snapshot = await getDocs(bookingsCollection);
      const bookings = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
        startDate: doc.data().startDate?.toDate(),
        endDate: doc.data().endDate?.toDate(),
        createdAt: doc.data().createdAt
      }));
      return bookings;
    } catch (fallbackError) {
      throw fallbackError;
    }
  }
};

// Get bookings by user
export const getBookingsByUser = async (userId) => {
  try {
    // Check authentication first
    checkAuth();
    
    const bookingsCollection = collection(db, "bookings");
    const q = query(
      bookingsCollection, 
      where("userId", "==", userId),
      orderBy("startDate", "desc")
    );
    const snapshot = await getDocs(q);
    const bookings = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startDate: doc.data().startDate?.toDate(),
      endDate: doc.data().endDate?.toDate(),
      createdAt: doc.data().createdAt
    }));
    return bookings;
  } catch (error) {
    throw error;
  }
};

// Get bookings by bike
export const getBookingsByBike = async (bikeId) => {
  try {
    // Check authentication first
    checkAuth();
    
    // Get bookings from main collection
    const bookingsCollection = collection(db, "bookings");
    const mainQuery = query(
      bookingsCollection, 
      where("bikeId", "==", bikeId),
      orderBy("startDate", "desc")
    );
    const mainSnapshot = await getDocs(mainQuery);
    
    const mainBookings = mainSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startDate: doc.data().startDate?.toDate(),
      endDate: doc.data().endDate?.toDate(),
      createdAt: doc.data().createdAt,
      source: 'main'
    }));
    
    // Get bookings from all subcollections using collectionGroup
    const bookingsGroupRef = collectionGroup(db, "bookings");
    const subcollectionQuery = query(
      bookingsGroupRef,
      where("bikeId", "==", bikeId),
      orderBy("startDate", "desc")
    );
    const subcollectionSnapshot = await getDocs(subcollectionQuery);
    
    const subcollectionBookings = subcollectionSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startDate: doc.data().startDate?.toDate ? doc.data().startDate?.toDate() : doc.data().startDate,
      endDate: doc.data().endDate?.toDate ? doc.data().endDate?.toDate() : doc.data().endDate,
      createdAt: doc.data().createdAt,
      source: 'subcollection'
    }));
    
    // Merge and deduplicate bookings
    const allBookings = [...mainBookings];
    
    subcollectionBookings.forEach(subcollectionBooking => {
      if (!allBookings.some(booking => booking.id === subcollectionBooking.id)) {
        allBookings.push(subcollectionBooking);
      }
    });
    
    return allBookings;
  } catch (error) {
    throw error;
  }
};

// Get bookings within a date range
export const getBookingsByDateRange = async (startDate, endDate) => {
  try {
    // Check authentication first
    checkAuth();
    
    const startTimestamp = startDate instanceof Date ? Timestamp.fromDate(startDate) : startDate;
    const endTimestamp = endDate instanceof Date ? Timestamp.fromDate(endDate) : endDate;
    
    const bookingsCollection = collection(db, "bookings");
    const q = query(
      bookingsCollection,
      where("startDate", ">=", startTimestamp),
      where("startDate", "<=", endTimestamp),
      orderBy("startDate", "desc")
    );
    
    const snapshot = await getDocs(q);
    const bookings = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startDate: doc.data().startDate?.toDate(),
      endDate: doc.data().endDate?.toDate(),
      createdAt: doc.data().createdAt
    }));
    
    return bookings;
  } catch (error) {
    throw error;
  }
};

// Check if a booking format is valid and convert it if needed
// This helps handle bookings from mobile app
export const validateBookingFormat = (booking) => {
  // Create a copy to avoid modifying the original
  const validatedBooking = { ...booking };
  
  // Handle different date formats
  if (validatedBooking.startDate) {
    // Convert string dates to Date objects
    if (typeof validatedBooking.startDate === 'string') {
      validatedBooking.startDate = Timestamp.fromDate(new Date(validatedBooking.startDate));
    }
    // Convert numeric timestamps to Date objects
    else if (typeof validatedBooking.startDate === 'number') {
      validatedBooking.startDate = Timestamp.fromDate(new Date(validatedBooking.startDate));
    }
  }
  
  if (validatedBooking.endDate) {
    // Convert string dates to Date objects
    if (typeof validatedBooking.endDate === 'string') {
      validatedBooking.endDate = Timestamp.fromDate(new Date(validatedBooking.endDate));
    }
    // Convert numeric timestamps to Date objects
    else if (typeof validatedBooking.endDate === 'number') {
      validatedBooking.endDate = Timestamp.fromDate(new Date(validatedBooking.endDate));
    }
  }
  
  // Ensure required fields are present
  if (!validatedBooking.status) {
    validatedBooking.status = 'PENDING';
  }
  
  if (!validatedBooking.isHourly && validatedBooking.isHourly !== false) {
    validatedBooking.isHourly = true;
  }
  
  if (!validatedBooking.createdAt) {
    validatedBooking.createdAt = Date.now();
  }
  
  return validatedBooking;
};

// Create a new booking
export const createBooking = async (bookingData) => {
  try {
    // Check authentication first
    checkAuth();
    
    // Validate and standardize the booking data format
    const validatedBookingData = validateBookingFormat(bookingData);
    
    // Convert Date objects to Firestore Timestamps
    const startDate = validatedBookingData.startDate instanceof Date 
      ? Timestamp.fromDate(validatedBookingData.startDate) 
      : validatedBookingData.startDate;
    
    const endDate = validatedBookingData.endDate instanceof Date 
      ? Timestamp.fromDate(validatedBookingData.endDate) 
      : validatedBookingData.endDate;
    
    const booking = {
      bikeId: validatedBookingData.bikeId,
      userId: validatedBookingData.userId,
      userName: validatedBookingData.userName,
      fullName: validatedBookingData.fullName || validatedBookingData.userName,
      bikeName: validatedBookingData.bikeName || '',
      bikeType: validatedBookingData.bikeType || '',
      bikeImageUrl: validatedBookingData.bikeImageUrl || '',
      startDate,
      endDate,
      totalPrice: validatedBookingData.totalPrice,
      status: validatedBookingData.status || 'PENDING', // PENDING, CONFIRMED, COMPLETED, CANCELLED
      isHourly: validatedBookingData.isHourly !== undefined ? validatedBookingData.isHourly : true,
      createdAt: validatedBookingData.createdAt || Date.now()
    };
    
    // 1. Save to main bookings collection
    const docRef = await addDoc(collection(db, "bookings"), booking);
    
    // 2. Also save to user's bookings subcollection for quicker access
    if (booking.userId) {
      const userBookingRef = doc(db, `users/${booking.userId}/bookings`, docRef.id);
      await setDoc(userBookingRef, booking);
    }
    
    return {
      id: docRef.id,
      ...booking,
      startDate: booking.startDate?.toDate(),
      endDate: booking.endDate?.toDate()
    };
  } catch (error) {
    throw error;
  }
};

// Update a booking
export const updateBooking = async (bookingId, updatedData) => {
  try {
    // Check authentication first
    checkAuth();
    
    let bookingRef = null;
    let currentBooking = null;
    let foundLocation = null;
    
    // First, try to find the booking in the main collection
    const mainBookingRef = doc(db, "bookings", bookingId);
    const mainBookingDoc = await getDoc(mainBookingRef);
    
    if (mainBookingDoc.exists()) {
      bookingRef = mainBookingRef;
      currentBooking = mainBookingDoc.data();
      foundLocation = 'main';
    } else {
      // If not found in main collection, use collectionGroup to find it in subcollections
      const bookingsGroupRef = collectionGroup(db, "bookings");
      const groupSnapshot = await getDocs(bookingsGroupRef);
      
      // Find the specific booking document
      const foundDoc = groupSnapshot.docs.find(doc => doc.id === bookingId);
      
      if (foundDoc) {
        bookingRef = foundDoc.ref;
        currentBooking = foundDoc.data();
        foundLocation = foundDoc.ref.path;
      } else {
        throw new Error(`Booking with ID ${bookingId} not found in any collection`);
      }
    }
    
    // Convert Date objects to Firestore Timestamps if present
    const updates = { ...updatedData };
    
    if (updates.startDate instanceof Date) {
      updates.startDate = Timestamp.fromDate(updates.startDate);
    }
    
    if (updates.endDate instanceof Date) {
      updates.endDate = Timestamp.fromDate(updates.endDate);
    }
    
    // Add updatedAt timestamp
    updates.updatedAt = serverTimestamp();
    
    // Update the booking in its found location
    await updateDoc(bookingRef, updates);
    
    // If booking was found in a subcollection, also try to update/sync with other locations
    if (foundLocation !== 'main') {
      const userId = currentBooking.userId;
      const bikeId = currentBooking.bikeId;
      
      // Try to update in main collection if it exists there
      try {
        const mainDoc = await getDoc(mainBookingRef);
        if (mainDoc.exists()) {
          await updateDoc(mainBookingRef, updates);
        }
      } catch (error) {
        // Silent error handling
      }
      
      // Try to update in user subcollection if it exists and we have userId
      if (userId) {
        try {
          const userBookingRef = doc(db, `users/${userId}/bookings`, bookingId);
          const userBookingDoc = await getDoc(userBookingRef);
          if (userBookingDoc.exists()) {
            await updateDoc(userBookingRef, updates);
          }
        } catch (error) {
          // Silent error handling
        }
      }
      
      // Try to update in bike subcollection if it exists and we have bikeId
      if (bikeId) {
        try {
          const bikeBookingRef = doc(db, `bikes/${bikeId}/bookings`, bookingId);
          const bikeBookingDoc = await getDoc(bikeBookingRef);
          if (bikeBookingDoc.exists()) {
            await updateDoc(bikeBookingRef, updates);
          }
        } catch (error) {
          // Silent error handling
        }
      }
    } else {
      // If booking was found in main collection, also update subcollections
      const userId = currentBooking.userId;
      const bikeId = currentBooking.bikeId;
      
      // Update in user subcollection if it exists
      if (userId) {
        try {
          const userBookingRef = doc(db, `users/${userId}/bookings`, bookingId);
          const userBookingDoc = await getDoc(userBookingRef);
          if (userBookingDoc.exists()) {
            await updateDoc(userBookingRef, updates);
          }
        } catch (error) {
          // Silent error handling
        }
      }
      
      // Update in bike subcollection if it exists
      if (bikeId) {
        try {
          const bikeBookingRef = doc(db, `bikes/${bikeId}/bookings`, bookingId);
          const bikeBookingDoc = await getDoc(bikeBookingRef);
          if (bikeBookingDoc.exists()) {
            await updateDoc(bikeBookingRef, updates);
          }
        } catch (error) {
          // Silent error handling
        }
      }
    }
    
    // Get the updated document from the original location
    const updatedDoc = await getDoc(bookingRef);
    const data = updatedDoc.data();
    
    return {
      id: bookingId,
      ...data,
      startDate: data.startDate?.toDate ? data.startDate.toDate() : data.startDate,
      endDate: data.endDate?.toDate ? data.endDate.toDate() : data.endDate,
      createdAt: data.createdAt?.toDate ? data.createdAt.toDate() : data.createdAt,
      updatedAt: data.updatedAt?.toDate ? data.updatedAt.toDate() : data.updatedAt
    };
  } catch (error) {
    throw error;
  }
};

// Delete a booking
export const deleteBooking = async (bookingId) => {
  try {
    // Check authentication first
    checkAuth();
    
    let bookingRef = null;
    let currentBooking = null;
    let foundLocation = null;
    
    // First, try to find the booking in the main collection
    const mainBookingRef = doc(db, "bookings", bookingId);
    const mainBookingDoc = await getDoc(mainBookingRef);
    
    if (mainBookingDoc.exists()) {
      bookingRef = mainBookingRef;
      currentBooking = mainBookingDoc.data();
      foundLocation = 'main';
    } else {
      // If not found in main collection, use collectionGroup to find it in subcollections
      const bookingsGroupRef = collectionGroup(db, "bookings");
      const groupSnapshot = await getDocs(bookingsGroupRef);
      
      // Find the specific booking document
      const foundDoc = groupSnapshot.docs.find(doc => doc.id === bookingId);
      
      if (foundDoc) {
        bookingRef = foundDoc.ref;
        currentBooking = foundDoc.data();
        foundLocation = foundDoc.ref.path;
      } else {
        return { id: bookingId, deleted: false, error: 'Booking not found' };
      }
    }
    
    const userId = currentBooking.userId;
    const bikeId = currentBooking.bikeId;
    
    // Delete the booking from its found location
    await deleteDoc(bookingRef);
    
    // Also try to delete from other locations if they exist
    
    // Try to delete from main collection if booking was found elsewhere
    if (foundLocation !== 'main') {
      try {
        const mainDoc = await getDoc(mainBookingRef);
        if (mainDoc.exists()) {
          await deleteDoc(mainBookingRef);
        }
      } catch (error) {
        // Silent error handling
      }
    }
    
    // Try to delete from user subcollection if it exists
    if (userId) {
      try {
        const userBookingRef = doc(db, `users/${userId}/bookings`, bookingId);
        const userBookingDoc = await getDoc(userBookingRef);
        if (userBookingDoc.exists()) {
          await deleteDoc(userBookingRef);
        }
      } catch (error) {
        // Silent error handling
      }
    }
    
    // Try to delete from bike subcollection if it exists
    if (bikeId) {
      try {
        const bikeBookingRef = doc(db, `bikes/${bikeId}/bookings`, bookingId);
        const bikeBookingDoc = await getDoc(bikeBookingRef);
        if (bikeBookingDoc.exists()) {
          await deleteDoc(bikeBookingRef);
        }
      } catch (error) {
        // Silent error handling
      }
    }
    
    return { id: bookingId, deleted: true };
  } catch (error) {
    throw error;
  }
};

// Calculate booking duration in hours or days
export const calculateBookingDuration = (booking) => {
  if (!booking.startDate || !booking.endDate) {
    return 'N/A';
  }
  
  const start = booking.startDate instanceof Date ? booking.startDate : booking.startDate.toDate();
  const end = booking.endDate instanceof Date ? booking.endDate : booking.endDate.toDate();
  const diffMs = end.getTime() - start.getTime();
  const diffHours = diffMs / (1000 * 60 * 60);
  
  if (booking.isHourly) {
    // For hourly bookings, show hours and minutes
    const hours = Math.floor(diffHours);
    const minutes = Math.floor((diffHours - hours) * 60);
    return `${hours}h ${minutes}m`;
  } else {
    // For daily bookings, show days
    const days = Math.ceil(diffHours / 24);
    return `${days} day${days !== 1 ? 's' : ''}`;
  }
};

// Get revenue by period (day, week, month)
export const getRevenueByPeriod = async (period) => {
  try {
    // Check authentication first
    checkAuth();
    
    const now = new Date();
    let startDate;
    
    // Calculate start date based on period
    if (period === 'day') {
      startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    } else if (period === 'week') {
      const day = now.getDay(); // 0 is Sunday
      startDate = new Date(now);
      startDate.setDate(now.getDate() - day);
    } else if (period === 'month') {
      startDate = new Date(now.getFullYear(), now.getMonth(), 1);
    } else {
      throw new Error('Invalid period. Use "day", "week", or "month"');
    }
    
    // Get all bookings from all collections and subcollections
    const allBookings = await getAllBookingsFromAllCollections();
    
    // Filter bookings by date range
    const bookings = allBookings.filter(booking => {
      const bookingDate = booking.startDate instanceof Date ? booking.startDate : new Date(booking.startDate);
      return bookingDate >= startDate && bookingDate <= now;
    });
    
    // Calculate total revenue from completed bookings
    // Include all COMPLETED bookings regardless of payment status
    const completedBookings = bookings.filter(booking => booking.status === 'COMPLETED');
    const totalRevenue = completedBookings.reduce((sum, booking) => {
      // Convert booking.totalPrice to a number if it's a string or default to 0
      const price = booking.totalPrice ? 
        (typeof booking.totalPrice === 'string' ? parseFloat(booking.totalPrice) : booking.totalPrice) : 0;
      return sum + price;
    }, 0);
    
    return {
      period,
      totalRevenue: totalRevenue || 0,
      bookings: bookings.length || 0,
      completedBookings: completedBookings.length || 0,
      startDate,
      endDate: now
    };
  } catch (error) {
    // Return default values in case of error
    return {
      period,
      totalRevenue: 0,
      bookings: 0,
      completedBookings: 0,
      error: error.message
    };
  }
};

// Check if a bike is available during a specific time period
export const checkBikeAvailability = async (bikeId, startDate, endDate) => {
  try {
    // Check authentication first
    checkAuth();
    
    // Convert to timestamp if needed
    const startTimestamp = startDate instanceof Date ? Timestamp.fromDate(startDate) : startDate;
    const endTimestamp = endDate instanceof Date ? Timestamp.fromDate(endDate) : endDate;
    
    // Check main bookings collection
    const bookingsCollection = collection(db, "bookings");
    const mainQuery = query(
      bookingsCollection,
      where("bikeId", "==", bikeId),
      where("status", "in", ["PENDING", "CONFIRMED"]),
      where("startDate", "<=", endTimestamp)
    );
    const mainSnapshot = await getDocs(mainQuery);
    
    // Check all subcollections using collectionGroup
    const bookingsGroupRef = collectionGroup(db, "bookings");
    const subcollectionQuery = query(
      bookingsGroupRef,
      where("bikeId", "==", bikeId),
      where("status", "in", ["PENDING", "CONFIRMED"]),
      where("startDate", "<=", endTimestamp)
    );
    const subcollectionSnapshot = await getDocs(subcollectionQuery);
    
    // Combine results and filter out bookings that end before our start time
    const allConflictingDocs = [...mainSnapshot.docs, ...subcollectionSnapshot.docs];
    
    // Remove duplicates based on document ID
    const uniqueConflictingDocs = allConflictingDocs.filter((doc, index, self) => 
      index === self.findIndex(d => d.id === doc.id)
    );
    
    const conflictingBookings = uniqueConflictingDocs.filter(doc => {
      const bookingData = doc.data();
      const bookingEndDate = bookingData.endDate instanceof Timestamp ? bookingData.endDate : Timestamp.fromDate(new Date(bookingData.endDate));
      return bookingEndDate >= startTimestamp;
    });
    
    return {
      available: conflictingBookings.length === 0,
      conflictingBookings: conflictingBookings.map(doc => ({
        id: doc.id,
        ...doc.data(),
        startDate: doc.data().startDate?.toDate ? doc.data().startDate?.toDate() : doc.data().startDate,
        endDate: doc.data().endDate?.toDate ? doc.data().endDate?.toDate() : doc.data().endDate
      }))
    };
  } catch (error) {
    throw error;
  }
};

// Get bookings based on user role (admin sees all, regular users see only their own)
export const getBookingsByUserRole = async (userId, isAdmin) => {
  try {
    // Check authentication first
    checkAuth();
    
    if (isAdmin) {
      // Admins see all bookings from all collections and subcollections
      return await getAllBookingsFromAllCollections();
    } else {
      // Regular users only see their own bookings
      // Check both the main bookings collection and user subcollection
      const mainBookings = await getBookingsByUser(userId);
      
      // Get bookings from user subcollection
      const userBookingsPath = `users/${userId}/bookings`;
      const userBookingsCollection = collection(db, userBookingsPath);
      const userSnapshot = await getDocs(userBookingsCollection);
      
      const userBookings = userSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
        startDate: doc.data().startDate?.toDate(),
        endDate: doc.data().endDate?.toDate(),
        createdAt: doc.data().createdAt
      }));
      
      // Also check for user bookings in other subcollections using collectionGroup
      const userBookingsGroupRef = collectionGroup(db, "bookings");
      const userBookingsQuery = query(userBookingsGroupRef, where("userId", "==", userId));
      const userBookingsSnapshot = await getDocs(userBookingsQuery);
      
      const userBookingsFromSubcollections = userBookingsSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
        startDate: doc.data().startDate?.toDate ? doc.data().startDate?.toDate() : doc.data().startDate,
        endDate: doc.data().endDate?.toDate ? doc.data().endDate?.toDate() : doc.data().endDate,
        createdAt: doc.data().createdAt
      }));
      
      // Merge and deduplicate bookings
      const allUserBookings = [...mainBookings];
      
      userBookings.forEach(userBooking => {
        if (!allUserBookings.some(booking => booking.id === userBooking.id)) {
          allUserBookings.push(userBooking);
        }
      });
      
      userBookingsFromSubcollections.forEach(userBooking => {
        if (!allUserBookings.some(booking => booking.id === userBooking.id)) {
          allUserBookings.push(userBooking);
        }
      });
      
      return allUserBookings;
    }
  } catch (error) {
    throw error;
  }
};
