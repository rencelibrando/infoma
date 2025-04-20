import { db } from '../firebase';
import { collection, getDocs, query, onSnapshot, limit, orderBy, where, getDoc, doc } from 'firebase/firestore';

// Enhanced in-memory cache with expiration timestamps for different data types
const dataCache = {
  bikes: null,
  users: null,
  rides: null,
  reviews: null,
  bikeTypes: null,
  userRoles: null,
  bikeDetails: {}, // Individual bike details cache
  lastUpdated: {
    bikes: null,
    users: null,
    rides: null,
    reviews: null,
    bikeDetails: {},
  },
  expiryTimes: {
    bikes: 60000, // 1 minute
    users: 300000, // 5 minutes
    rides: 30000, // 30 seconds - more frequent as rides status changes often
    reviews: 600000, // 10 minutes
    bikeDetails: 300000, // 5 minutes
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

// Get bike details with caching support
export const getBikeDetails = async (bikeId) => {
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
    throw error;
  }
};

// Function to extract unique bike types from bikes collection
export const getBikeTypes = async () => {
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
    return [];
  }
};

// Function to extract unique user roles
export const getUserRoles = async () => {
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
    console.log('Preloading all dashboard data...');
    await getAnalyticsData();
    console.log('Dashboard data preloaded successfully');
  } catch (error) {
    console.error('Error preloading dashboard data:', error);
  }
};

// Clear reviews cache to force a fresh fetch of reviews data
export const clearReviewsCache = () => {
  dataCache.reviews = null;
  dataCache.lastUpdated.reviews = null;
  console.log('Reviews cache cleared, next fetch will get fresh data');
};

// Fetch analytics data for the dashboard
export const getAnalyticsData = async () => {
  try {
    // Use cached data if available
    const fetchBikes = async () => {
      // Optimize query with limit and ordering
      const bikesQuery = query(collection(db, 'bikes'), orderBy('updatedAt', 'desc'));
      const bikesSnapshot = await getDocs(bikesQuery);
      return bikesSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
    };
    
    const fetchUsers = async () => {
      const usersQuery = query(collection(db, 'users'), orderBy('createdAt', 'desc'));
      const usersSnapshot = await getDocs(usersQuery);
      return usersSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
    };
    
    const fetchRides = async () => {
      // Prioritize active rides and recent ones
      const ridesQuery = query(
        collection(db, 'rides'), 
        orderBy('startTime', 'desc'), 
        limit(100)
      );
      const ridesSnapshot = await getDocs(ridesQuery);
      return ridesSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
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
    const [bikes, users, rides, reviews] = await Promise.all([
      getFromCacheOrFetch('bikes', fetchBikes),
      getFromCacheOrFetch('users', fetchUsers),
      getFromCacheOrFetch('rides', fetchRides),
      getFromCacheOrFetch('reviews', fetchReviews)
    ]);
    
    // Calculate statistics
    return {
      bikes,
      users,
      rides,
      reviews,
      stats: calculateStats(bikes, users, rides, reviews)
    };
  } catch (error) {
    console.error('Error fetching analytics data:', error);
    throw error;
  }
};

// Calculate dashboard statistics
const calculateStats = (bikes, users, rides, reviews) => {
  const activeRides = rides.filter(ride => ride.isActive);
  const availableBikes = bikes.filter(bike => bike.isAvailable && !bike.isInUse);
  const inUseBikes = bikes.filter(bike => bike.isInUse);
  const verifiedUsers = users.filter(user => user.idVerificationStatus === 'approved');
  
  // Check if bikes have totalReviews and averageRating directly attached
  const bikesWithReviews = bikes.filter(bike => 
    bike.totalReviews !== undefined && bike.averageRating !== undefined
  );
  
  let totalReviewsCount = reviews.length;
  let calculatedAvgRating = calculateAverageRating(reviews);
  
  // If we have bikes with direct review data, use that instead
  if (bikesWithReviews.length > 0) {
    console.log('Using direct review data from bike documents');
    totalReviewsCount = bikesWithReviews.reduce((total, bike) => total + (bike.totalReviews || 0), 0);
    
    // Calculate weighted average based on number of reviews per bike
    if (totalReviewsCount > 0) {
      const weightedSum = bikesWithReviews.reduce((sum, bike) => {
        return sum + (bike.averageRating || 0) * (bike.totalReviews || 0);
      }, 0);
      calculatedAvgRating = (weightedSum / totalReviewsCount).toFixed(1);
    }
    
    console.log(`Using bike direct data: ${totalReviewsCount} reviews, avg rating: ${calculatedAvgRating}`);
  }
  
  return {
    totalBikes: bikes.length,
    activeBikes: availableBikes.length,
    inUseBikes: inUseBikes.length,
    maintenanceBikes: bikes.length - availableBikes.length - inUseBikes.length,
    
    totalUsers: users.length,
    verifiedUsers: verifiedUsers.length,
    
    activeRides: activeRides.length,
    totalRides: rides.length,
    
    totalReviews: totalReviewsCount,
    averageRating: calculatedAvgRating
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
        const ridesQuery = query(
          collection(db, 'rides'), 
          orderBy('startTime', 'desc'), 
          limit(100)
        );
        const ridesSnapshot = await getDocs(ridesQuery);
        const rideData = ridesSnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        
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
        stats: calculateStats(bikes, users, rides, reviews)
      };
      
      callback(completeData);
    } catch (error) {
      console.error('Error in analytics subscription:', error);
    }
  }, (error) => {
    console.error('Error in bikes listener:', error);
  });
  
  unsubscribeFunctions.push(bikesUnsubscribe);
  
  // Only subscribe to the rides collection separately for active ride updates
  // as these change more frequently
  const ridesQuery = query(
    collection(db, 'rides'),
    where('isActive', '==', true)
  );
  
  const ridesUnsubscribe = onSnapshot(ridesQuery, async (ridesSnapshot) => {
    try {
      if (!dataCache.bikes) {
        console.log('No bikes data available yet, skipping rides update');
        return;
      }
      
      console.log('Active rides update received');
      const activeRides = ridesSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      
      // Only update the active rides in the cache
      if (dataCache.rides) {
        // Find all non-active rides in current cache
        const nonActiveRides = dataCache.rides.filter(ride => !ride.isActive);
        
        // Combine with new active rides
        dataCache.rides = [...activeRides, ...nonActiveRides];
        dataCache.lastUpdated.rides = Date.now();
        
        // Notify subscribers with updated data
        if (dataCache.bikes && dataCache.users && dataCache.reviews) {
          console.log('Notifying subscribers of rides update...');
          callback({
            bikes: dataCache.bikes,
            users: dataCache.users,
            rides: dataCache.rides,
            reviews: dataCache.reviews,
            stats: calculateStats(
              dataCache.bikes, 
              dataCache.users, 
              dataCache.rides, 
              dataCache.reviews
            )
          });
        }
      }
    } catch (error) {
      console.error('Error in active rides subscription:', error);
    }
  });
  
  unsubscribeFunctions.push(ridesUnsubscribe);
  
  // Return a function that unsubscribes from all listeners
  return () => {
    console.log('Unsubscribing from all analytics listeners');
    unsubscribeFunctions.forEach(unsubscribe => unsubscribe());
  };
}; 