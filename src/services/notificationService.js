import { 
  collection, 
  addDoc, 
  query, 
  orderBy, 
  getDocs, 
  doc, 
  updateDoc, 
  deleteDoc,
  where,
  serverTimestamp 
} from 'firebase/firestore';
import { db, auth } from '../firebase';

// Check authentication
const checkAuth = () => {
  return auth.currentUser;
};

// Create a new notification
export const createNotification = async (notificationData) => {
  try {
    const user = checkAuth();
    if (!user) {
      console.warn('Attempting to create notification without authentication');
      // Try to proceed anyway for admin dashboard
    }

    const notification = {
      ...notificationData,
      id: `notification_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
      read: false,
      createdBy: user ? user.uid : 'system',
      createdByEmail: user ? user.email : 'system'
    };

    // Add to notifications collection
    const docRef = await addDoc(collection(db, 'notifications'), notification);
    
    return {
      success: true,
      notificationId: docRef.id,
      notification: { ...notification, id: docRef.id }
    };
  } catch (error) {
    console.error('Error creating notification:', error);
    throw error;
  }
};

// Create a booking cancellation notification
export const createBookingCancellationNotification = async (bookingData, reason = '', cancelledBy = 'admin') => {
  try {
    const notificationData = {
      type: 'cancellation',
      title: 'Booking Cancelled',
      message: `Booking for ${bookingData.bikeModel || bookingData.bikeName || 'bike'} has been cancelled${reason ? `. Reason: ${reason}` : '.'}`,
      bookingId: bookingData.id,
      userId: bookingData.userId,
      userEmail: bookingData.userEmail || bookingData.userName,
      bikeId: bookingData.bikeId,
      bikeName: bookingData.bikeModel || bookingData.bikeName,
      cancelReason: reason,
      cancelledBy: cancelledBy,
      priority: 'high',
      category: 'booking',
      metadata: {
        bookingStartDate: bookingData.startDate,
        bookingEndDate: bookingData.endDate,
        bookingDate: bookingData.bookingDate || bookingData.date,
        isHourly: bookingData.isHourly,
        cost: bookingData.cost
      }
    };

    return await createNotification(notificationData);
  } catch (error) {
    console.error('Error creating booking cancellation notification:', error);
    throw error;
  }
};

// Create a booking confirmation notification
export const createBookingConfirmationNotification = async (bookingData) => {
  try {
    const notificationData = {
      type: 'confirmation',
      title: 'Booking Confirmed',
      message: `Booking for ${bookingData.bikeModel || bookingData.bikeName || 'bike'} has been confirmed.`,
      bookingId: bookingData.id,
      userId: bookingData.userId,
      userEmail: bookingData.userEmail || bookingData.userName,
      bikeId: bookingData.bikeId,
      bikeName: bookingData.bikeModel || bookingData.bikeName,
      priority: 'medium',
      category: 'booking',
      metadata: {
        bookingStartDate: bookingData.startDate,
        bookingEndDate: bookingData.endDate,
        bookingDate: bookingData.bookingDate || bookingData.date,
        isHourly: bookingData.isHourly,
        cost: bookingData.cost
      }
    };

    return await createNotification(notificationData);
  } catch (error) {
    console.error('Error creating booking confirmation notification:', error);
    throw error;
  }
};

// Create a booking completion notification
export const createBookingCompletionNotification = async (bookingData) => {
  try {
    const notificationData = {
      type: 'completion',
      title: 'Booking Completed',
      message: `Booking for ${bookingData.bikeModel || bookingData.bikeName || 'bike'} has been completed.`,
      bookingId: bookingData.id,
      userId: bookingData.userId,
      userEmail: bookingData.userEmail || bookingData.userName,
      bikeId: bookingData.bikeId,
      bikeName: bookingData.bikeModel || bookingData.bikeName,
      priority: 'low',
      category: 'booking',
      metadata: {
        bookingStartDate: bookingData.startDate,
        bookingEndDate: bookingData.endDate,
        bookingDate: bookingData.bookingDate || bookingData.date,
        isHourly: bookingData.isHourly,
        cost: bookingData.cost
      }
    };

    return await createNotification(notificationData);
  } catch (error) {
    console.error('Error creating booking completion notification:', error);
    throw error;
  }
};

// Get all notifications
export const getAllNotifications = async (limit = 50) => {
  try {
    const user = checkAuth();
    if (!user) {
      console.warn('Attempting to get notifications without authentication');
      // Try to proceed anyway for admin dashboard
    }

    const notificationsRef = collection(db, 'notifications');
    const q = query(
      notificationsRef,
      orderBy('createdAt', 'desc'),
      ...(limit ? [limit] : [])
    );

    const snapshot = await getDocs(q);
    const notifications = [];

    snapshot.forEach(doc => {
      notifications.push({
        id: doc.id,
        ...doc.data()
      });
    });

    return notifications;
  } catch (error) {
    console.error('Error getting notifications:', error);
    throw error;
  }
};

// Get notifications by type
export const getNotificationsByType = async (type, limit = 50) => {
  try {
    const user = checkAuth();
    if (!user) {
      console.warn('Attempting to get notifications without authentication');
    }

    const notificationsRef = collection(db, 'notifications');
    const q = query(
      notificationsRef,
      where('type', '==', type),
      orderBy('createdAt', 'desc'),
      ...(limit ? [limit] : [])
    );

    const snapshot = await getDocs(q);
    const notifications = [];

    snapshot.forEach(doc => {
      notifications.push({
        id: doc.id,
        ...doc.data()
      });
    });

    return notifications;
  } catch (error) {
    console.error('Error getting notifications by type:', error);
    throw error;
  }
};

// Mark notification as read
export const markNotificationAsRead = async (notificationId) => {
  try {
    const user = checkAuth();
    if (!user) {
      console.warn('Attempting to mark notification as read without authentication');
    }

    const notificationRef = doc(db, 'notifications', notificationId);
    await updateDoc(notificationRef, {
      read: true,
      readAt: serverTimestamp(),
      readBy: user ? user.uid : 'system'
    });

    return { success: true };
  } catch (error) {
    console.error('Error marking notification as read:', error);
    throw error;
  }
};

// Delete notification
export const deleteNotification = async (notificationId) => {
  try {
    const user = checkAuth();
    if (!user) {
      console.warn('Attempting to delete notification without authentication');
    }

    const notificationRef = doc(db, 'notifications', notificationId);
    await deleteDoc(notificationRef);

    return { success: true };
  } catch (error) {
    console.error('Error deleting notification:', error);
    throw error;
  }
};

// Clear all notifications
export const clearAllNotifications = async () => {
  try {
    const user = checkAuth();
    if (!user) {
      console.warn('Attempting to clear notifications without authentication');
    }

    const notifications = await getAllNotifications();
    
    // Delete all notifications
    const deletePromises = notifications.map(notification => 
      deleteNotification(notification.id)
    );

    await Promise.all(deletePromises);

    return { success: true, deletedCount: notifications.length };
  } catch (error) {
    console.error('Error clearing all notifications:', error);
    throw error;
  }
};

// Get unread notification count
export const getUnreadNotificationCount = async () => {
  try {
    const user = checkAuth();
    if (!user) {
      console.warn('Attempting to get unread count without authentication');
      return 0;
    }

    const notificationsRef = collection(db, 'notifications');
    const q = query(
      notificationsRef,
      where('read', '==', false)
    );

    const snapshot = await getDocs(q);
    return snapshot.size;
  } catch (error) {
    console.error('Error getting unread notification count:', error);
    return 0;
  }
}; 