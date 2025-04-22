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
  Timestamp
} from "firebase/firestore";

// Get all bookings
export const getAllBookings = async () => {
  try {
    const bookingsCollection = collection(db, "bookings");
    const snapshot = await getDocs(bookingsCollection);
    const bookings = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startTime: doc.data().startTime?.toDate(),
      endTime: doc.data().endTime?.toDate(),
      createdAt: doc.data().createdAt?.toDate()
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
      orderBy("startTime", "desc")
    );
    const snapshot = await getDocs(q);
    const bookings = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startTime: doc.data().startTime?.toDate(),
      endTime: doc.data().endTime?.toDate(),
      createdAt: doc.data().createdAt?.toDate()
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
      orderBy("startTime", "desc")
    );
    const snapshot = await getDocs(q);
    const bookings = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startTime: doc.data().startTime?.toDate(),
      endTime: doc.data().endTime?.toDate(),
      createdAt: doc.data().createdAt?.toDate()
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
      where("startTime", ">=", startTimestamp),
      where("startTime", "<=", endTimestamp),
      orderBy("startTime", "desc")
    );
    
    const snapshot = await getDocs(q);
    const bookings = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      startTime: doc.data().startTime?.toDate(),
      endTime: doc.data().endTime?.toDate(),
      createdAt: doc.data().createdAt?.toDate()
    }));
    
    return bookings;
  } catch (error) {
    console.error('Error getting bookings by date range:', error);
    throw error;
  }
};

// Create a new booking
export const createBooking = async (bookingData) => {
  try {
    // Convert Date objects to Firestore Timestamps
    const startTime = bookingData.startTime instanceof Date 
      ? Timestamp.fromDate(bookingData.startTime) 
      : bookingData.startTime;
    
    const endTime = bookingData.endTime instanceof Date 
      ? Timestamp.fromDate(bookingData.endTime) 
      : bookingData.endTime;
    
    const booking = {
      bikeId: bookingData.bikeId,
      userId: bookingData.userId,
      userName: bookingData.userName,
      bikeName: bookingData.bikeName,
      bikeType: bookingData.bikeType || '',
      bikeImageUrl: bookingData.bikeImageUrl || '',
      location: bookingData.location || '',
      startTime,
      endTime,
      totalPrice: bookingData.totalPrice,
      status: bookingData.status || 'PENDING', // PENDING, CONFIRMED, COMPLETED, CANCELLED
      paymentStatus: bookingData.paymentStatus || 'unpaid', // unpaid, paid
      isHourly: bookingData.isHourly !== undefined ? bookingData.isHourly : true,
      notes: bookingData.notes || '',
      createdAt: serverTimestamp()
    };
    
    const docRef = await addDoc(collection(db, "bookings"), booking);
    return {
      id: docRef.id,
      ...booking,
      startTime: booking.startTime?.toDate(),
      endTime: booking.endTime?.toDate(),
      createdAt: new Date()
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
    
    // Convert Date objects to Firestore Timestamps if present
    const updates = { ...updatedData };
    
    if (updates.startTime instanceof Date) {
      updates.startTime = Timestamp.fromDate(updates.startTime);
    }
    
    if (updates.endTime instanceof Date) {
      updates.endTime = Timestamp.fromDate(updates.endTime);
    }
    
    // Add update timestamp
    updates.updatedAt = serverTimestamp();
    
    await updateDoc(bookingRef, updates);
    
    // Get the updated document
    const updatedDoc = await getDoc(bookingRef);
    const data = updatedDoc.data();
    
    return {
      id: bookingId,
      ...data,
      startTime: data.startTime?.toDate(),
      endTime: data.endTime?.toDate(),
      createdAt: data.createdAt?.toDate(),
      updatedAt: data.updatedAt?.toDate()
    };
  } catch (error) {
    console.error('Error updating booking:', error);
    throw error;
  }
};

// Delete a booking
export const deleteBooking = async (bookingId) => {
  try {
    const bookingRef = doc(db, "bookings", bookingId);
    await deleteDoc(bookingRef);
    return { id: bookingId, deleted: true };
  } catch (error) {
    console.error('Error deleting booking:', error);
    throw error;
  }
};

// Calculate booking duration in hours or days
export const calculateBookingDuration = (booking) => {
  if (!booking.startTime || !booking.endTime) {
    return 'N/A';
  }
  
  const start = booking.startTime instanceof Date ? booking.startTime : booking.startTime.toDate();
  const end = booking.endTime instanceof Date ? booking.endTime : booking.endTime.toDate();
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
    const totalRevenue = bookings
      .filter(booking => booking.status === 'COMPLETED' && booking.paymentStatus === 'paid')
      .reduce((sum, booking) => sum + (parseFloat(booking.totalPrice) || 0), 0);
    
    return {
      period,
      totalRevenue,
      bookings: bookings.length,
      startDate,
      endDate: now
    };
  } catch (error) {
    console.error(`Error getting revenue for ${period}:`, error);
    throw error;
  }
};

// Check if a bike is available during a specific time period
export const checkBikeAvailability = async (bikeId, startTime, endTime) => {
  try {
    // Convert to timestamp if needed
    const startTimestamp = startTime instanceof Date ? Timestamp.fromDate(startTime) : startTime;
    const endTimestamp = endTime instanceof Date ? Timestamp.fromDate(endTime) : endTime;
    
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
      where("startTime", "<=", endTimestamp)
    );
    
    const snapshot = await getDocs(q);
    
    // Filter out bookings that end before our start time
    const conflictingBookings = snapshot.docs.filter(doc => {
      const bookingData = doc.data();
      return bookingData.endTime >= startTimestamp;
    });
    
    return {
      available: conflictingBookings.length === 0,
      conflictingBookings: conflictingBookings.map(doc => ({
        id: doc.id,
        ...doc.data(),
        startTime: doc.data().startTime?.toDate(),
        endTime: doc.data().endTime?.toDate()
      }))
    };
  } catch (error) {
    console.error('Error checking bike availability:', error);
    throw error;
  }
}; 