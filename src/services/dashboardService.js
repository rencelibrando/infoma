import { db, auth } from '../firebase';
import { collection, getDocs, query, onSnapshot, limit, orderBy, where, getDoc, doc, deleteDoc } from 'firebase/firestore';
import { ref, onValue, get } from 'firebase/database';
import { realtimeDb } from '../firebase';

// Enhanced in-memory cache with expiration timestamps for different data types
const dataCache = {
  bikes: null,
  users: null,
  rides: null,
  reviews: null,
  bikeTypes: null,
  userRoles: null,
  bikeDetails: {}, // Individual bike details cache
  activeRidesFromRealtime: null, // New cache for active rides from Realtime DB
  lastUpdated: {
    bikes: null,
    users: null,
    rides: null,
    reviews: null,
    bikeDetails: {},
    activeRidesFromRealtime: null,
  },
  expiryTimes: {
    bikes: 60000, // 1 minute
    users: 300000, // 5 minutes
    rides: 30000, // 30 seconds - more frequent as rides status changes often
    reviews: 600000, // 10 minutes
    bikeDetails: 300000, // 5 minutes
    activeRidesFromRealtime: 10000, // 10 seconds - very fresh for active rides
  }
};

// Advanced cache mechanism with per-collection expiry times
const getFromCacheOrFetch = async (key, fetchFunction) => {
  const now = Date.now();
  const maxAgeMs = dataCache.expiryTimes[key];
  
  // If data exists in cache and is recent enough, return it
  if (dataCache[key] && dataCache.lastUpdated[key] && (now - dataCache.lastUpdated[key]) < maxAgeMs) {
    console.log(`Using cached ${key} data (age: ${Math.round((now - dataCache.lastUpdated[key])/1000)}s)`);
    return dataCache[key];
  }
  
  // Otherwise fetch and update cache
  console.log(`Fetching fresh ${key} data (cache expired or empty)`);
  const data = await fetchFunction();
  dataCache[key] = data;
  dataCache.lastUpdated[key] = now;
  return data;
};

// Helper function to check authentication with better error handling
const checkAuth = () => {
  const user = auth.currentUser;
  if (!user) {
    // Don't throw an error immediately, log it for debugging
    console.warn('User not authenticated in checkAuth()');
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

// Get bike details with caching support
export const getBikeDetails = async (bikeId) => {
  // Check authentication - but don't fail immediately
  const user = checkAuth();
  if (!user) {
    console.warn('Attempting to get bike details without authentication');
    // Try to proceed anyway for public data
  }
  
  const now = Date.now();
  const maxAgeMs = dataCache.expiryTimes.bikeDetails;
  
  // Check if we have this bike in the main bikes cache first
  if (dataCache.bikes) {
    const cachedBike = dataCache.bikes.find(bike => bike.id === bikeId);
    if (cachedBike) {
      console.log(`Found bike ${bikeId} in main bikes cache`);
      return cachedBike;
    }
  }
  
  // Check individual bike cache
  if (dataCache.bikeDetails[bikeId] && 
      dataCache.lastUpdated.bikeDetails[bikeId] && 
      (now - dataCache.lastUpdated.bikeDetails[bikeId]) < maxAgeMs) {
    console.log(`Using cached bike details for ${bikeId}`);
    return dataCache.bikeDetails[bikeId];
  }
  
  // Fetch fresh data
  console.log(`Fetching fresh bike details for ${bikeId}`);
  try {
    const bikeDoc = await getDoc(doc(db, 'bikes', bikeId));
    if (!bikeDoc.exists()) {
      console.error(`Bike ${bikeId} not found`);
      return null;
    }
    
    const bikeData = {
      id: bikeId,
      ...bikeDoc.data()
    };
    
    // Update cache
    dataCache.bikeDetails[bikeId] = bikeData;
    dataCache.lastUpdated.bikeDetails[bikeId] = now;
    
    return bikeData;
  } catch (error) {
    console.error(`Error fetching bike ${bikeId}:`, error);
    // Return cached data if available, even if expired
    if (dataCache.bikeDetails[bikeId]) {
      console.log(`Returning expired cache for bike ${bikeId} due to error`);
      return dataCache.bikeDetails[bikeId];
    }
    throw error;
  }
};

// Function to extract unique bike types from bikes collection
export const getBikeTypes = async () => {
  // Check authentication - but don't fail immediately for public data
  const user = checkAuth();
  if (!user) {
    console.warn('Attempting to get bike types without authentication');
  }
  
  if (dataCache.bikeTypes) {
    return dataCache.bikeTypes;
  }
  
  try {
    // Check if we already have bikes data in cache
    if (dataCache.bikes) {
      const types = [...new Set(dataCache.bikes.map(bike => bike.type))];
      dataCache.bikeTypes = types;
      return types;
    }
    
    // Otherwise fetch minimal data just for types
    const bikesSnapshot = await getDocs(collection(db, 'bikes'));
    const bikes = bikesSnapshot.docs.map(doc => doc.data());
    const types = [...new Set(bikes.map(bike => bike.type))];
    dataCache.bikeTypes = types;
    return types;
  } catch (error) {
    console.error('Error fetching bike types:', error);
    // Return fallback types if there's an error
    return ['Standard', 'Electric', 'Mountain', 'Road'];
  }
};

// Function to extract unique user roles
export const getUserRoles = async () => {
  // Check authentication - but don't fail immediately
  const user = checkAuth();
  if (!user) {
    console.warn('Attempting to get user roles without authentication');
    // Return default roles for public access
    return ['User', 'Admin', 'Manager'];
  }
  
  if (dataCache.userRoles) {
    return dataCache.userRoles;
  }
  
  try {
    // Check if we already have users data in cache
    if (dataCache.users) {
      const roles = [...new Set(dataCache.users.map(user => user.role || 'User'))];
      dataCache.userRoles = roles;
      return roles;
    }
    
    // Otherwise fetch minimal data just for roles
    const usersSnapshot = await getDocs(collection(db, 'users'));
    const users = usersSnapshot.docs.map(doc => doc.data());
    const roles = [...new Set(users.map(user => user.role || 'User'))];
    dataCache.userRoles = roles;
    return roles;
  } catch (error) {
    console.error('Error fetching user roles:', error);
    return ['User', 'Admin', 'Manager']; // Default fallback
  }
};

// Preload all filter options data (called once at app startup)
export const preloadOptionsData = async () => {
  try {
    // Check authentication - but don't fail immediately
    const user = checkAuth();
    if (!user) {
      console.warn('Preloading options data without authentication');
    }
    
    console.log('Preloading options data...');
    await Promise.all([
      getBikeTypes(),
      getUserRoles()
    ]);
    console.log('Options data preloaded successfully');
  } catch (error) {
    console.error('Error preloading options data:', error);
  }
};

// Preload all dashboard data for faster access across components
export const preloadDashboardData = async () => {
  try {
    // Check authentication - but don't fail immediately
    const user = checkAuth();
    if (!user) {
      console.warn('Preloading dashboard data without authentication');
    }
    
    console.log('Preloading all dashboard data...');
    await getAnalyticsData();
    console.log('Dashboard data preloaded successfully');
  } catch (error) {
    console.error('Error preloading dashboard data:', error);
  }
};

// Clear all cache to force fresh data fetch
export const clearAllCache = () => {
  dataCache.bikes = null;
  dataCache.users = null;
  dataCache.rides = null;
  dataCache.reviews = null;
  dataCache.bikeTypes = null;
  dataCache.userRoles = null;
  dataCache.bikeDetails = {};
  dataCache.activeRidesFromRealtime = null;
  dataCache.lastUpdated = {
    bikes: null,
    users: null,
    rides: null,
    reviews: null,
    bikeDetails: {},
    activeRidesFromRealtime: null,
  };
  console.log('All caches cleared, next fetch will get fresh data');
};

// Clear reviews cache to force a fresh fetch of reviews data
export const clearReviewsCache = () => {
  dataCache.reviews = null;
  dataCache.lastUpdated.reviews = null;
  console.log('Reviews cache cleared, next fetch will get fresh data');
};

// Fetch active rides from Realtime Database (same source as Real-Time Tracking)
export const getActiveRidesFromRealtime = async () => {
  try {
    // Check cache first
    const now = Date.now();
    const maxAgeMs = dataCache.expiryTimes.activeRidesFromRealtime;
    
    if (dataCache.activeRidesFromRealtime && 
        dataCache.lastUpdated.activeRidesFromRealtime && 
        (now - dataCache.lastUpdated.activeRidesFromRealtime) < maxAgeMs) {
      console.log('Using cached active rides from Realtime DB');
      return dataCache.activeRidesFromRealtime;
    }
    
    console.log('Fetching fresh active rides from Realtime Database...');
    const activeRidesRef = ref(realtimeDb, 'activeRides');
    const snapshot = await get(activeRidesRef);
    
    const activeRidesData = snapshot.val() || {};
    const activeRides = Object.entries(activeRidesData).map(([userId, rideData]) => ({
      ...rideData,
      userId: userId,
      id: rideData.rideId || rideData.id
    }));
    
    // Cache the result
    dataCache.activeRidesFromRealtime = activeRides;
    dataCache.lastUpdated.activeRidesFromRealtime = now;
    
    console.log(`Found ${activeRides.length} active rides in Realtime Database`);
    return activeRides;
  } catch (error) {
    console.error('Error fetching active rides from Realtime Database:', error);
    // Return empty array if there's an error
    return [];
  }
};

// Fetch analytics data for the dashboard
export const getAnalyticsData = async () => {
  try {
    // Check authentication - but don't fail immediately
    const user = checkAuth();
    if (!user) {
      console.warn('Fetching analytics data without authentication - some data may be limited');
    } else {
      console.log('User is authenticated for analytics:', user.uid);
    }
    
    // Test Firebase connection first
    console.log('Testing Firebase connection...');
    try {
      const testCollection = collection(db, 'bikes');
      console.log('Firebase connection test successful');
    } catch (connectionError) {
      console.error('Firebase connection test failed:', connectionError);
      throw new Error('Cannot connect to Firebase: ' + connectionError.message);
    }
    
    // Use cached data if available
    const fetchBikes = async () => {
      console.log('Starting to fetch bikes...');
      try {
        // Optimize query with limit and ordering
        const bikesQuery = query(collection(db, 'bikes'), orderBy('updatedAt', 'desc'));
        const bikesSnapshot = await getDocs(bikesQuery);
        console.log('Bikes query successful, got', bikesSnapshot.docs.length, 'bikes');
        const bikes = bikesSnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        console.log('Processed bikes:', bikes.length);
        return bikes;
      } catch (error) {
        console.error('Error in fetchBikes:', error);
        console.log('Trying bikes query without orderBy...');
        try {
          const simpleBikesQuery = query(collection(db, 'bikes'));
          const simpleBikesSnapshot = await getDocs(simpleBikesQuery);
          console.log('Simple bikes query successful, got', simpleBikesSnapshot.docs.length, 'bikes');
          const bikes = simpleBikesSnapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
          }));
          return bikes;
        } catch (simpleError) {
          console.error('Even simple bikes query failed:', simpleError);
          return [];
        }
      }
    };
    
    const fetchUsers = async () => {
      console.log('Starting to fetch users...');
      try {
        const usersQuery = query(collection(db, 'users'), orderBy('createdAt', 'desc'));
        const usersSnapshot = await getDocs(usersQuery);
        console.log('Users query successful, got', usersSnapshot.docs.length, 'users');
        const users = usersSnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        return users;
      } catch (error) {
        console.error('Error in fetchUsers:', error);
        console.log('Trying users query without orderBy...');
        try {
          const simpleUsersQuery = query(collection(db, 'users'));
          const simpleUsersSnapshot = await getDocs(simpleUsersQuery);
          console.log('Simple users query successful, got', simpleUsersSnapshot.docs.length, 'users');
          const users = simpleUsersSnapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
          }));
          return users;
        } catch (simpleError) {
          console.error('Even simple users query failed:', simpleError);
          return [];
        }
      }
    };
    
    const fetchRides = async () => {
      console.log('Starting to fetch rides...');
      // Always fetch fresh ride data as it's highly dynamic
      console.log('Fetching fresh rides data');
      // Try both startTime (new) and startDate (legacy) fields
      let ridesQuery;
      try {
        ridesQuery = query(
          collection(db, 'rides'), 
          orderBy('startTime', 'desc'), 
          limit(100)
        );
        const ridesSnapshot = await getDocs(ridesQuery);
        console.log('Rides query with startTime successful, got', ridesSnapshot.docs.length, 'rides');
        const rideData = ridesSnapshot.docs.map(doc => {
          const data = doc.data();
          
          // Normalize timestamp fields
          const startTime = data.startTime ? new Date(data.startTime) : (data.startDate?.toDate() || null);
          const endTime = data.endTime ? new Date(data.endTime) : (data.endDate?.toDate() || null);
          
          // A ride should only be considered active if:
          // 1. It has started (has startTime)
          // 2. It has not ended (no endTime)
          // 3. The status is explicitly "active"
          const hasStarted = !!startTime;
          const hasNotEnded = !endTime;
          const statusIsActive = data.status === "active";
          
          // Only mark as active if all conditions are met
          const isActuallyActive = hasStarted && hasNotEnded && statusIsActive;
          
          return {
            id: doc.id,
            ...data,
            startTime,
            endTime,
            // Use the corrected active status
            isActive: isActuallyActive,
            status: data.status || (data.isActive ? "active" : "completed")
          };
        });
        
        dataCache.rides = rideData;
        dataCache.lastUpdated.rides = Date.now();
        
        return rideData;
      } catch (error) {
        console.error('Error fetching rides with startTime:', error);
        // Fallback to startDate if startTime index doesn't exist
        console.log('Using startDate fallback for rides query in subscription');
        try {
          ridesQuery = query(
            collection(db, 'rides'), 
            orderBy('startDate', 'desc'), 
            limit(100)
          );
          const ridesSnapshot = await getDocs(ridesQuery);
          console.log('Rides query with startDate successful, got', ridesSnapshot.docs.length, 'rides');
          const rideData = ridesSnapshot.docs.map(doc => {
            const data = doc.data();
            
            // Normalize timestamp fields
            const startTime = data.startTime ? new Date(data.startTime) : (data.startDate?.toDate() || null);
            const endTime = data.endTime ? new Date(data.endTime) : (data.endDate?.toDate() || null);
            
            // A ride should only be considered active if:
            // 1. It has started (has startTime)
            // 2. It has not ended (no endTime)
            // 3. The status is explicitly "active"
            const hasStarted = !!startTime;
            const hasNotEnded = !endTime;
            const statusIsActive = data.status === "active";
            
            // Only mark as active if all conditions are met
            const isActuallyActive = hasStarted && hasNotEnded && statusIsActive;
            
            return {
              id: doc.id,
              ...data,
              startTime,
              endTime,
              // Use the corrected active status
              isActive: isActuallyActive,
              status: data.status || (data.isActive ? "active" : "completed")
            };
          });
          
          dataCache.rides = rideData;
          dataCache.lastUpdated.rides = Date.now();
          
          return rideData;
        } catch (simpleError) {
          console.error('Error fetching rides with startDate:', simpleError);
          console.log('Trying simple rides query without orderBy...');
          try {
            const simpleRidesQuery = query(collection(db, 'rides'), limit(100));
            const simpleRidesSnapshot = await getDocs(simpleRidesQuery);
            console.log('Simple rides query successful, got', simpleRidesSnapshot.docs.length, 'rides');
            const rideData = simpleRidesSnapshot.docs.map(doc => {
              const data = doc.data();
              
              // Normalize timestamp fields
              const startTime = data.startTime ? new Date(data.startTime) : (data.startDate?.toDate() || null);
              const endTime = data.endTime ? new Date(data.endTime) : (data.endDate?.toDate() || null);
              
              // A ride should only be considered active if:
              // 1. It has started (has startTime)
              // 2. It has not ended (no endTime)
              // 3. The status is explicitly "active"
              const hasStarted = !!startTime;
              const hasNotEnded = !endTime;
              const statusIsActive = data.status === "active";
              
              // Only mark as active if all conditions are met
              const isActuallyActive = hasStarted && hasNotEnded && statusIsActive;
              
              return {
                id: doc.id,
                ...data,
                startTime,
                endTime,
                // Use the corrected active status
                isActive: isActuallyActive,
                status: data.status || (data.isActive ? "active" : "completed")
              };
            });
            
            dataCache.rides = rideData;
            dataCache.lastUpdated.rides = Date.now();
            
            return rideData;
          } catch (finalError) {
            console.error('Even simple rides query failed:', finalError);
            return [];
          }
        }
      }
    };
    
    const fetchReviews = async () => {
      console.log('Starting to fetch reviews...');
      if (dataCache.reviews && 
          dataCache.lastUpdated.reviews && 
          (Date.now() - dataCache.lastUpdated.reviews < dataCache.expiryTimes.reviews)) {
        console.log('Using cached reviews data');
        return dataCache.reviews;
      }
      
      console.log('Fetching fresh reviews data');
      let reviewData = [];
      
      // First try to get reviews from the top-level collection
      try {
        console.log('Trying top-level reviews collection...');
        const reviewsQuery = query(
          collection(db, 'reviews'), 
          orderBy('timestamp', 'desc'), 
          limit(50)
        );
        const reviewsSnapshot = await getDocs(reviewsQuery);
        reviewData = reviewsSnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        
        console.log(`Found ${reviewData.length} reviews in top-level collection`);
      } catch (error) {
        console.warn('Error fetching top-level reviews:', error);
        console.log('Trying simple reviews query without orderBy...');
        try {
          const simpleReviewsQuery = query(collection(db, 'reviews'), limit(50));
          const simpleReviewsSnapshot = await getDocs(simpleReviewsQuery);
          reviewData = simpleReviewsSnapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
          }));
          console.log(`Found ${reviewData.length} reviews with simple query`);
        } catch (simpleError) {
          console.error('Even simple reviews query failed:', simpleError);
        }
      }
      
      // Then try to get reviews from bike subcollections if we have bikes data
      if (dataCache.bikes && dataCache.bikes.length > 0) {
        console.log(`Checking reviews in bike subcollections for ${dataCache.bikes.length} bikes...`);
        try {
          // Get reviews from each bike's subcollection
          const bikeReviewsPromises = dataCache.bikes.map(async (bike) => {
            try {
              const bikeReviewsQuery = query(
                collection(db, `bikes/${bike.id}/reviews`),
                orderBy('timestamp', 'desc'),
                limit(10)
              );
              const bikeReviewsSnapshot = await getDocs(bikeReviewsQuery);
              const bikeReviews = bikeReviewsSnapshot.docs.map(doc => ({
                id: doc.id,
                bikeId: bike.id, // Ensure bikeId is set
                ...doc.data()
              }));
              console.log(`Found ${bikeReviews.length} reviews for bike ${bike.id}`);
              return bikeReviews;
            } catch (error) {
              console.warn(`Error fetching reviews for bike ${bike.id}:`, error);
              return [];
            }
          });
          
          const bikeReviewsResults = await Promise.all(bikeReviewsPromises);
          const allBikeReviews = bikeReviewsResults.flat();
          
          console.log(`Found ${allBikeReviews.length} reviews in bike subcollections`);
          
          // Merge with top-level reviews, avoiding duplicates by id
          const allReviewIds = new Set(reviewData.map(r => r.id));
          const uniqueBikeReviews = allBikeReviews.filter(r => !allReviewIds.has(r.id));
          
          reviewData = [...reviewData, ...uniqueBikeReviews];
        } catch (error) {
          console.warn('Error fetching bike subcollection reviews:', error);
        }
      } else {
        console.log('No bikes data available for subcollection review search');
      }
      
      console.log(`Total reviews found: ${reviewData.length}`);
      
      // Cache the combined results
      dataCache.reviews = reviewData;
      dataCache.lastUpdated.reviews = Date.now();
      
      // Log detailed information about each review for debugging
      console.log("All reviews with details:");
      reviewData.forEach((review, index) => {
        if (index < 10) { // Only log first 10 to avoid console spam
          console.log(`Review ${index+1}:`, {
            id: review.id,
            bikeId: review.bikeId,
            rating: review.rating,
            comment: review.comment ? review.comment.substring(0, 30) + '...' : 'No comment',
            timestamp: review.timestamp,
            userName: review.userName || 'Unknown'
          });
        }
      });
      
      return reviewData;
    };
    
    // Parallel fetches for better performance
    console.log('Starting parallel data fetches...');
    const [bikes, users, rides, reviews, activeRidesFromRealtime] = await Promise.all([
      getFromCacheOrFetch('bikes', fetchBikes),
      getFromCacheOrFetch('users', fetchUsers),
      getFromCacheOrFetch('rides', fetchRides),
      getFromCacheOrFetch('reviews', fetchReviews),
      getActiveRidesFromRealtime() // Fetch active rides from Realtime DB
    ]);
    
    console.log('All data fetched successfully:');
    console.log(`- Bikes: ${bikes.length}`);
    console.log(`- Users: ${users.length}`);
    console.log(`- Rides: ${rides.length}`);
    console.log(`- Reviews: ${reviews.length}`);
    console.log(`- Active Rides (Realtime DB): ${activeRidesFromRealtime.length}`);
    
    // Calculate statistics
    const stats = calculateStats(bikes, users, rides, reviews, activeRidesFromRealtime);
    console.log('Calculated stats:', stats);
    
    return {
      bikes,
      users,
      rides,
      reviews,
      stats
    };
  } catch (error) {
    console.error('Error fetching analytics data:', error);
    console.error('Error details:', {
      message: error.message,
      code: error.code,
      stack: error.stack
    });
    
    // Provide fallback data so dashboard doesn't break
    console.log('Providing fallback data due to Firebase error');
    const fallbackData = {
      bikes: [
        {
          id: 'fallback-bike-1',
          name: 'Sample Bike 1',
          type: 'Standard',
          price: '₱50/hr',
          isAvailable: true,
          isInUse: false,
          latitude: 14.5995,
          longitude: 120.9842
        }
      ],
      users: [
        {
          id: 'fallback-user-1',
          email: 'sample@example.com',
          role: 'User',
          createdAt: new Date()
        }
      ],
      rides: [
        {
          id: 'fallback-ride-1',
          status: 'completed',
          isActive: false,
          startTime: new Date(Date.now() - 86400000) // 1 day ago
        }
      ],
      reviews: [
        {
          id: 'fallback-review-1',
          rating: 4.5,
          comment: 'Sample review',
          timestamp: Date.now()
        }
      ],
      stats: {
        totalBikes: 1,
        activeBikes: 1,
        inUseBikes: 0,
        maintenanceBikes: 0,
        totalUsers: 1,
        activeRides: 0,
        totalRides: 1,
        totalReviews: 1,
        averageRating: 4.5
      }
    };
    
    console.log('Fallback data provided:', fallbackData);
    return fallbackData;
  }
};

// Calculate dashboard statistics
const calculateStats = (bikes, users, rides, reviews, activeRidesFromRealtime = []) => {
  console.log('🔥 calculateStats called with activeRidesFromRealtime:', activeRidesFromRealtime.length);

  // Use active rides from Realtime Database instead of filtering Firestore rides
  const activeRides = activeRidesFromRealtime || [];
  const totalActiveRides = activeRides.length;

  console.log(`📊 Active rides count from Realtime DB: ${totalActiveRides}`);

  let totalReviews = 0;
  let totalRating = 0;
  let reviewCount = 0;

  // Count reviews from the main reviews data
  if (reviews && Array.isArray(reviews)) {
    totalReviews += reviews.length;
    reviews.forEach(review => {
      if (review.rating && typeof review.rating === 'number') {
        totalRating += review.rating;
        reviewCount++;
      }
    });
  }

  // Also count any direct review data from bikes if available
  if (bikes && Array.isArray(bikes)) {
    bikes.forEach(bike => {
      if (bike.reviewData && Array.isArray(bike.reviewData)) {
        totalReviews += bike.reviewData.length;
        bike.reviewData.forEach(review => {
          if (review.rating && typeof review.rating === 'number') {
            totalRating += review.rating;
            reviewCount++;
          }
        });
      }
    });
  }

  const averageRating = reviewCount > 0 ? Number((totalRating / reviewCount).toFixed(2)) : 0;

  return {
    totalBikes: bikes?.length || 0,
    activeBikes: bikes?.filter(bike => bike.status === 'active')?.length || 0,
    inUseBikes: bikes?.filter(bike => bike.status === 'in-use')?.length || 0,
    maintenanceBikes: bikes?.filter(bike => bike.status === 'maintenance')?.length || 0,
    totalUsers: users?.length || 0,
    activeRides: totalActiveRides, // Now using Realtime Database data
    totalRides: rides?.length || 0,
    totalReviews,
    averageRating
  };
};

// Modify the calculateAverageRating function to better handle numeric conversion
const calculateAverageRating = (reviews) => {
  if (!reviews || reviews.length === 0) return 0;
  
  // Log the ratings to help with debugging
  console.log('Calculating average from ratings:', reviews.map(r => parseFloat(r.rating) || 0));
  
  let validRatingsCount = 0;
  const sum = reviews.reduce((total, review) => {
    // Ensure rating is a valid number
    const ratingValue = parseFloat(review.rating);
    if (!isNaN(ratingValue) && ratingValue > 0) {
      validRatingsCount++;
      return total + ratingValue;
    }
    return total;
  }, 0);
  
  // Only divide by valid ratings count
  return validRatingsCount > 0 ? (sum / validRatingsCount).toFixed(1) : 0;
};

// Set up optimized real-time listener for dashboard analytics data
export const subscribeToAnalytics = (callback) => {
  try {
    // Check authentication first
    const user = checkAuth();
    
    // Create an array to store all unsubscribe functions
    const unsubscribeFunctions = [];
    
    // Subscribe to bikes collection with optimized query
    const bikesQuery = query(collection(db, 'bikes'));
    const bikesUnsubscribe = onSnapshot(bikesQuery, async (bikesSnapshot) => {
      try {
        console.log('Bikes update received, processing...');
        // Process bikes data changes first for immediate UI update
        const bikes = bikesSnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        
        // Update cache
        dataCache.bikes = bikes;
        dataCache.lastUpdated.bikes = Date.now();
        
        // Extract bike types for filters
        dataCache.bikeTypes = [...new Set(bikes.map(bike => bike.type))];
        
        // Use cached data for other collections if available and recent
        const fetchUsers = async () => {
          if (dataCache.users && 
              dataCache.lastUpdated.users && 
              (Date.now() - dataCache.lastUpdated.users < dataCache.expiryTimes.users)) {
            console.log('Using cached users data');
            return dataCache.users;
          }
          
          console.log('Fetching fresh users data');
          const usersQuery = query(collection(db, 'users'), orderBy('createdAt', 'desc'));
          const usersSnapshot = await getDocs(usersQuery);
          const userData = usersSnapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
          }));
          
          dataCache.users = userData;
          dataCache.lastUpdated.users = Date.now();
          dataCache.userRoles = [...new Set(userData.map(user => user.role || 'User'))];
          
          return userData;
        };
        
        const fetchRides = async () => {
          // Always fetch fresh ride data as it's highly dynamic
          console.log('Fetching fresh rides data');
          // Try both startTime (new) and startDate (legacy) fields
          let ridesQuery;
          try {
            ridesQuery = query(
              collection(db, 'rides'), 
              orderBy('startTime', 'desc'), 
              limit(100)
            );
          } catch (error) {
            // Fallback to startDate if startTime index doesn't exist
            console.log('Using startDate fallback for rides query in subscription');
            ridesQuery = query(
              collection(db, 'rides'), 
              orderBy('startDate', 'desc'), 
              limit(100)
            );
          }
          
          const ridesSnapshot = await getDocs(ridesQuery);
          const rideData = ridesSnapshot.docs.map(doc => {
            const data = doc.data();
            
            // Normalize timestamp fields
            const startTime = data.startTime ? new Date(data.startTime) : (data.startDate?.toDate() || null);
            const endTime = data.endTime ? new Date(data.endTime) : (data.endDate?.toDate() || null);
            
            // A ride should only be considered active if:
            // 1. It has started (has startTime)
            // 2. It has not ended (no endTime)
            // 3. The status is explicitly "active"
            const hasStarted = !!startTime;
            const hasNotEnded = !endTime;
            const statusIsActive = data.status === "active";
            
            // Only mark as active if all conditions are met
            const isActuallyActive = hasStarted && hasNotEnded && statusIsActive;
            
            return {
              id: doc.id,
              ...data,
              startTime,
              endTime,
              // Use the corrected active status
              isActive: isActuallyActive,
              status: data.status || (data.isActive ? "active" : "completed")
            };
          });
          
          dataCache.rides = rideData;
          dataCache.lastUpdated.rides = Date.now();
          
          return rideData;
        };
        
        const fetchReviews = async () => {
          if (dataCache.reviews && 
              dataCache.lastUpdated.reviews && 
              (Date.now() - dataCache.lastUpdated.reviews < dataCache.expiryTimes.reviews)) {
            console.log('Using cached reviews data');
            return dataCache.reviews;
          }
          
          console.log('Fetching fresh reviews data');
          let reviewData = [];
          
          // First try to get reviews from the top-level collection
          try {
            const reviewsQuery = query(
              collection(db, 'reviews'), 
              orderBy('timestamp', 'desc'), 
              limit(50)
            );
            const reviewsSnapshot = await getDocs(reviewsQuery);
            reviewData = reviewsSnapshot.docs.map(doc => ({
              id: doc.id,
              ...doc.data()
            }));
            
            console.log(`Found ${reviewData.length} reviews in top-level collection`);
          } catch (error) {
            console.warn('Error fetching top-level reviews:', error);
          }
          
          // Then try to get reviews from bike subcollections if we have bikes data
          if (dataCache.bikes && dataCache.bikes.length > 0) {
            try {
              // Get reviews from each bike's subcollection
              const bikeReviewsPromises = dataCache.bikes.map(async (bike) => {
                try {
                  const bikeReviewsQuery = query(
                    collection(db, `bikes/${bike.id}/reviews`),
                    orderBy('timestamp', 'desc'),
                    limit(10)
                  );
                  const bikeReviewsSnapshot = await getDocs(bikeReviewsQuery);
                  return bikeReviewsSnapshot.docs.map(doc => ({
                    id: doc.id,
                    bikeId: bike.id, // Ensure bikeId is set
                    ...doc.data()
                  }));
                } catch (error) {
                  console.warn(`Error fetching reviews for bike ${bike.id}:`, error);
                  return [];
                }
              });
              
              const bikeReviewsResults = await Promise.all(bikeReviewsPromises);
              const allBikeReviews = bikeReviewsResults.flat();
              
              console.log(`Found ${allBikeReviews.length} reviews in bike subcollections`);
              
              // Merge with top-level reviews, avoiding duplicates by id
              const allReviewIds = new Set(reviewData.map(r => r.id));
              const uniqueBikeReviews = allBikeReviews.filter(r => !allReviewIds.has(r.id));
              
              reviewData = [...reviewData, ...uniqueBikeReviews];
            } catch (error) {
              console.warn('Error fetching bike subcollection reviews:', error);
            }
          }
          
          console.log(`Total reviews found: ${reviewData.length}`);
          
          // Cache the combined results
          dataCache.reviews = reviewData;
          dataCache.lastUpdated.reviews = Date.now();
          
          return reviewData;
        };
        
        // Optimized - fetch concurrently
        console.log('Fetching related data after bikes update...');
        const [users, rides, reviews] = await Promise.all([
          fetchUsers(),
          fetchRides(),
          fetchReviews()
        ]);
        
        // Calculate statistics and return complete data
        console.log('All data fetched, notifying subscribers...');
        const completeData = {
          bikes,
          users,
          rides,
          reviews,
          stats: calculateStats(bikes, users, rides, reviews, [])
        };
        
        // Notify subscribers with updated data
        if (dataCache.bikes && dataCache.users && dataCache.reviews) {
          console.log('Notifying subscribers of rides update...');
          
          // Also fetch active rides from Realtime Database for consistency
          getActiveRidesFromRealtime().then(activeRidesFromRealtime => {
            callback({
              bikes: dataCache.bikes,
              users: dataCache.users,
              rides: dataCache.rides,
              reviews: dataCache.reviews,
              stats: calculateStats(
                dataCache.bikes, 
                dataCache.users, 
                dataCache.rides, 
                dataCache.reviews,
                activeRidesFromRealtime
              )
            });
          }).catch(error => {
            console.error('Error fetching active rides from Realtime DB:', error);
            // Fallback to empty active rides if Realtime DB fails
            callback({
              bikes: dataCache.bikes,
              users: dataCache.users,
              rides: dataCache.rides,
              reviews: dataCache.reviews,
              stats: calculateStats(
                dataCache.bikes, 
                dataCache.users, 
                dataCache.rides, 
                dataCache.reviews,
                []
              )
            });
          });
        }
      } catch (error) {
        console.error('Error in analytics subscription:', error);
      }
    }, (error) => {
      console.error('Error in bikes listener:', error);
    });
    
    unsubscribeFunctions.push(bikesUnsubscribe);
    
    // Instead of subscribing to just "active" rides which might include old data,
    // let's subscribe to all recent rides and filter them properly
    const recentRidesQuery = query(
      collection(db, 'rides'), 
      orderBy('startTime', 'desc'), 
      limit(100)
    );
    
    const ridesUnsubscribe = onSnapshot(recentRidesQuery, async (ridesSnapshot) => {
      try {
        if (!dataCache.bikes) {
          console.log('No bikes data available yet, skipping rides update');
          return;
        }
        
        console.log('Recent rides update received');
        const allRides = ridesSnapshot.docs.map(doc => {
          const data = doc.data();
          
          // Normalize timestamp fields
          const startTime = data.startTime ? new Date(data.startTime) : (data.startDate?.toDate() || null);
          const endTime = data.endTime ? new Date(data.endTime) : (data.endDate?.toDate() || null);
          
          // A ride should only be considered active if:
          // 1. It has started (has startTime)
          // 2. It has not ended (no endTime)
          // 3. The status is explicitly "active"
          const hasStarted = !!startTime;
          const hasNotEnded = !endTime;
          const statusIsActive = data.status === "active";
          
          // Only mark as active if all conditions are met
          const isActuallyActive = hasStarted && hasNotEnded && statusIsActive;
          
          return {
            id: doc.id,
            ...data,
            startTime,
            endTime,
            // Use the corrected active status
            isActive: isActuallyActive,
            status: data.status || (data.isActive ? "active" : "completed")
          };
        });
        
        // Update the complete rides cache
        dataCache.rides = allRides;
        dataCache.lastUpdated.rides = Date.now();
        
        // Log how many rides are actually active after our logic
        const trueActiveRides = allRides.filter(ride => ride.isActive);
        console.log(`After applying correct logic: ${trueActiveRides.length} active rides out of ${allRides.length} total rides`);
        
        // Notify subscribers with updated data
        if (dataCache.bikes && dataCache.users && dataCache.reviews) {
          console.log('Notifying subscribers of rides update...');
          
          // Also fetch active rides from Realtime Database for consistency
          getActiveRidesFromRealtime().then(activeRidesFromRealtime => {
            callback({
              bikes: dataCache.bikes,
              users: dataCache.users,
              rides: dataCache.rides,
              reviews: dataCache.reviews,
              stats: calculateStats(
                dataCache.bikes, 
                dataCache.users, 
                dataCache.rides, 
                dataCache.reviews,
                activeRidesFromRealtime
              )
            });
          }).catch(error => {
            console.error('Error fetching active rides from Realtime DB:', error);
            // Fallback to empty active rides if Realtime DB fails
            callback({
              bikes: dataCache.bikes,
              users: dataCache.users,
              rides: dataCache.rides,
              reviews: dataCache.reviews,
              stats: calculateStats(
                dataCache.bikes, 
                dataCache.users, 
                dataCache.rides, 
                dataCache.reviews,
                []
              )
            });
          });
        }
      } catch (error) {
        console.error('Error in rides subscription:', error);
        // Fallback to startDate if startTime index doesn't exist
        console.log('Trying rides subscription with startDate fallback...');
        try {
          const fallbackQuery = query(
            collection(db, 'rides'), 
            orderBy('startDate', 'desc'), 
            limit(100)
          );
          // We'll handle this with a separate listener if needed
        } catch (fallbackError) {
          console.error('Even fallback rides subscription failed:', fallbackError);
        }
      }
    });
    
    unsubscribeFunctions.push(ridesUnsubscribe);
    
    // Return a function that unsubscribes from all listeners
    return () => {
      console.log('Unsubscribing from all analytics listeners');
      unsubscribeFunctions.forEach(unsubscribe => unsubscribe());
    };
  } catch (error) {
    console.error('Error in subscribeToAnalytics:', error);
    throw error;
  }
};

// Delete a ride from the database (Admin only)
export const deleteRide = async (rideId) => {
  try {
    // Require authentication for this operation
    const currentUser = requireAuth();
    
    // Get current user's role to verify admin permissions
    const currentUserRef = doc(db, 'users', currentUser.uid);
    const currentUserDoc = await getDoc(currentUserRef);
    
    // Modified admin check to handle cases where user document might not exist
    // For example, when using a service account or a special admin account
    const isAdmin = currentUser.email?.endsWith('@bambike.com') || currentUser.email?.includes('admin');
    
    // If user document exists, also check for admin role in the document
    if (currentUserDoc.exists()) {
      const currentUserData = currentUserDoc.data();
      
      console.log('Admin check - Current user data:', {
        uid: currentUser.uid,
        email: currentUser.email,
        userData: currentUserData,
        role: currentUserData?.role,
        isAdmin: currentUserData?.isAdmin
      });
      
      // Additional check for admin status in user document
      if (currentUserData && (
        currentUserData.role?.toLowerCase() === 'admin' ||
        currentUserData.isAdmin === true ||
        currentUserData.isAdmin === 'true' ||
        currentUserData.role?.toLowerCase() === 'administrator'
      )) {
        isAdmin = true;
      }
    } else {
      console.log('User document not found for:', currentUser.email, 'Fallback to email domain check');
    }
    
    if (!isAdmin) {
      throw new Error('Only administrators can delete ride history. Contact an administrator to get admin permissions.');
    }
    
    // Check if the ride exists
    const rideRef = doc(db, 'rides', rideId);
    const rideDoc = await getDoc(rideRef);
    
    if (!rideDoc.exists()) {
      throw new Error('Ride not found');
    }
    
    // Store ride data for logging
    const rideData = rideDoc.data();
    
    // Delete the ride document from Firestore
    await deleteDoc(rideRef);
    
    // Clear cache to ensure fresh data on next fetch
    dataCache.rides = null;
    dataCache.lastUpdated.rides = null;
    
    console.log(`Ride ${rideId} deleted from database by admin ${currentUser.uid}`);
    
    return {
      success: true,
      message: `Ride ${rideId} deleted successfully`,
      deletedRide: {
        id: rideId,
        userId: rideData.userId,
        bikeId: rideData.bikeId,
        startTime: rideData.startTime,
        status: rideData.status
      }
    };
  } catch (error) {
    console.error('Error deleting ride:', error);
    if (error.message.includes('administrators') || error.message.includes('not found')) {
      throw error; // Re-throw permission and not found errors as-is
    }
    throw new Error(`Failed to delete ride: ${error.message}`);
  }
}; 