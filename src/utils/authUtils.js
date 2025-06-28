import { auth } from '../firebase';
import { getDoc, doc } from 'firebase/firestore';
import { db } from '../firebase';

// Check for development environment
const isDev = process.env.NODE_ENV === 'development';

/**
 * Centralized authentication utility to reduce redundancy
 */
export class AuthUtils {
  
  /**
   * Check authentication without throwing errors
   * @returns {Object|null} - User object or null
   */
  static checkAuth() {
    const user = auth.currentUser;
    if (!user) {
      console.warn('User not authenticated');
      return null;
    }
    return user;
  }

  /**
   * Require authentication - throws error if not authenticated
   * @returns {Object} - User object
   * @throws {Error} - If user is not authenticated
   */
  static requireAuth() {
    const user = auth.currentUser;
    if (!user) {
      throw new Error('User not authenticated. Please log in to access this resource.');
    }
    return user;
  }

  /**
   * Check if current user is admin
   * @returns {Promise<boolean>} - True if user is admin
   */
  static async isAdmin() {
    const user = this.checkAuth();
    if (!user) return false;

    try {
      // Check Firestore admin document
      const adminDoc = await getDoc(doc(db, 'admins', user.uid));
      if (adminDoc.exists()) {
        return true;
      }

      // Check user document for admin role
      const userDoc = await getDoc(doc(db, 'users', user.uid));
      if (userDoc.exists()) {
        const userData = userDoc.data();
        const isAdmin = userData.role?.toLowerCase() === 'admin' || 
                       userData.isAdmin === true || 
                       userData.isAdmin === 'true';
        if (isAdmin) return true;
      }

      // Dev environment fallback
      if (isDev) {
        console.log('Development environment: granting admin privileges');
        return true;
      }

      return false;
    } catch (error) {
      console.error('Error checking admin status:', error);
      return false;
    }
  }

  /**
   * Require admin privileges - throws error if not admin
   * @returns {Promise<Object>} - User object
   * @throws {Error} - If user is not admin
   */
  static async requireAdmin() {
    const user = this.requireAuth();
    const isAdmin = await this.isAdmin();
    
    if (!isAdmin) {
      throw new Error('Access denied. Administrator privileges required.');
    }
    
    return user;
  }

  /**
   * Get current user with admin status
   * @returns {Promise<Object>} - User object with isAdmin property
   */
  static async getCurrentUserWithRole() {
    const user = this.checkAuth();
    if (!user) return null;

    const isAdmin = await this.isAdmin();
    
    return {
      ...user,
      isAdmin
    };
  }
}

// Legacy exports for backward compatibility
export const checkAuth = () => AuthUtils.checkAuth();
export const requireAuth = () => AuthUtils.requireAuth();
export const isAdmin = () => AuthUtils.isAdmin();
export const requireAdmin = () => AuthUtils.requireAdmin(); 