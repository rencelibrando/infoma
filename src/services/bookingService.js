import { db } from '../firebase';
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

// Get all bookings
export const getAllBookings = async () => {
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
  } catch (error) {
    console.error('Error getting bookings:', error);
    throw error;
  }
};

// Get bookings by user
export const getBookingsByUser = async (userId) => {
  try {
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
    console.error('Error getting user bookings:', error);
    throw error;
  }
};

// Get bookings by bike
export const getBookingsByBike = async (bikeId) => {
  try {
    const bookingsCollection = collection(db, "bookings");
    const q = query(
      bookingsCollection, 
      where("bikeId", "==", bikeId),
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
    console.error('Error getting bike bookings:', error);
    throw error;
  }
};

// Get bookings within a date range
export const getBookingsByDateRange = async (startDate, endDate) => {
  try {
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
    console.error('Error getting bookings by date range:', error);
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
    
    // Logging for debugging
    console.log("Creating new booking:", booking);
    
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
    console.error('Error creating booking:', error);
    throw error;
  }
};

// Update a booking
export const updateBooking = async (bookingId, updatedData) => {
  try {
    const bookingRef = doc(db, "bookings", bookingId);
    
    // Get the current booking to find the user ID
    const currentBookingDoc = await getDoc(bookingRef);
    if (!currentBookingDoc.exists()) {
      throw new Error(`Booking with ID ${bookingId} not found`);
    }
    
    const currentBooking = currentBookingDoc.data();
    const userId = currentBooking.userId;
    
    // Convert Date objects to Firestore Timestamps if present
    const updates = { ...updatedData };
    
    if (updates.startDate instanceof Date) {
      updates.startDate = Timestamp.fromDate(updates.startDate);
    }
    
    if (updates.endDate instanceof Date) {
      updates.endDate = Timestamp.fromDate(updates.endDate);
    }
    
    // 1. Update in main bookings collection
    await updateDoc(bookingRef, updates);
    
    // 2. Also update in user's bookings subcollection if user ID exists
    if (userId) {
      const userBookingRef = doc(db, `users/${userId}/bookings`, bookingId);
      
      // Check if the document exists in the user's subcollection
      const userBookingDoc = await getDoc(userBookingRef);
      
      if (userBookingDoc.exists()) {
        await updateDoc(userBookingRef, updates);
      } else {
        // If it doesn't exist in the subcollection, create it
        const mergedBooking = { ...currentBooking, ...updates };
        await setDoc(userBookingRef, mergedBooking);
      }
    }
    
    // Get the updated document
    const updatedDoc = await getDoc(bookingRef);
    const data = updatedDoc.data();
    
    return {
      id: bookingId,
      ...data,
      startDate: data.startDate?.toDate(),
      endDate: data.endDate?.toDate(),
      createdAt: data.createdAt
    };
  } catch (error) {
    console.error('Error updating booking:', error);
    throw error;
  }
};

// Delete a booking
export const deleteBooking = async (bookingId) => {
  try {
    // Get the booking to find the user ID
    const bookingRef = doc(db, "bookings", bookingId);
    const bookingDoc = await getDoc(bookingRef);
    
    if (bookingDoc.exists()) {
      const bookingData = bookingDoc.data();
      const userId = bookingData.userId;
      
      // Delete from main collection
      await deleteDoc(bookingRef);
      
      // Delete from user's subcollection if user ID exists
      if (userId) {
        const userBookingRef = doc(db, `users/${userId}/bookings`, bookingId);
        
        // Check if it exists in the subcollection before deleting
        const userBookingDoc = await getDoc(userBookingRef);
        if (userBookingDoc.exists()) {
          await deleteDoc(userBookingRef);
        }
      }
    } else {
      console.warn(`Booking with ID ${bookingId} not found when attempting to delete`);
    }
    
    return { id: bookingId, deleted: true };
  } catch (error) {
    console.error('Error deleting booking:', error);
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
    
    // Get all bookings from start date
    const bookings = await getBookingsByDateRange(Timestamp.fromDate(startDate), Timestamp.fromDate(now));
    
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
    console.error(`Error getting revenue for ${period}:`, error);
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
    // Convert to timestamp if needed
    const startTimestamp = startDate instanceof Date ? Timestamp.fromDate(startDate) : startDate;
    const endTimestamp = endDate instanceof Date ? Timestamp.fromDate(endDate) : endDate;
    
    const bookingsCollection = collection(db, "bookings");
    
    // Query to find any conflicting bookings for this bike
    // We need to check for bookings where:
    // 1. The booking starts during our requested period
    // 2. The booking ends during our requested period
    // 3. The booking spans our entire requested period
    const q = query(
      bookingsCollection,
      where("bikeId", "==", bikeId),
      where("status", "in", ["PENDING", "CONFIRMED"]),
      where("startDate", "<=", endTimestamp)
    );
    
    const snapshot = await getDocs(q);
    
    // Filter out bookings that end before our start time
    const conflictingBookings = snapshot.docs.filter(doc => {
      const bookingData = doc.data();
      return bookingData.endDate >= startTimestamp;
    });
    
    return {
      available: conflictingBookings.length === 0,
      conflictingBookings: conflictingBookings.map(doc => ({
        id: doc.id,
        ...doc.data(),
        startDate: doc.data().startDate?.toDate(),
        endDate: doc.data().endDate?.toDate()
      }))
    };
  } catch (error) {
    console.error('Error checking bike availability:', error);
    throw error;
  }
};

// Get bookings based on user role (admin sees all, regular users see only their own)
export const getBookingsByUserRole = async (userId, isAdmin) => {
  try {
    if (isAdmin) {
      // Admins see all bookings from main collection
      return await getAllBookings();
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
      
      // Merge and deduplicate bookings
      const allUserBookings = [...mainBookings];
      userBookings.forEach(userBooking => {
        if (!allUserBookings.some(booking => booking.id === userBooking.id)) {
          allUserBookings.push(userBooking);
        }
      });
      
      return allUserBookings;
    }
  } catch (error) {
    console.error('Error getting bookings based on user role:', error);
    throw error;
  }
}; 