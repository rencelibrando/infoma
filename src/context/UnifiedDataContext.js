import React, { createContext, useContext, useState, useEffect } from 'react';
import { getAnalyticsData, subscribeToAnalytics } from '../services/dashboardService';

// Create unified context
const UnifiedDataContext = createContext(null);

// Custom hooks for accessing the context
export const useDataContext = () => useContext(UnifiedDataContext);
export const useAnalytics = () => useContext(UnifiedDataContext); // Alias for compatibility

export const UnifiedDataProvider = ({ children }) => {
  // Shared state for all components
  const [dashboardData, setDashboardData] = useState({
    bikes: [],
    users: [],
    rides: [],
    reviews: [],
    stats: {
      totalBikes: 0,
      activeBikes: 0,
      inUseBikes: 0,
      maintenanceBikes: 0,
      totalUsers: 0,
      activeRides: 0,
      totalRides: 0,
      totalReviews: 0,
      averageRating: 0
    }
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdateTime, setLastUpdateTime] = useState(new Date());
  const [showUpdateIndicator, setShowUpdateIndicator] = useState(false);

  // Load data and set up real-time listeners
  useEffect(() => {
    console.log('Initializing UnifiedDataContext and loading shared data...');
    setLoading(true);
    setError(null);
    
    // Set up real-time listener for all collections (single subscription)
    const unsubscribe = subscribeToAnalytics((updatedData) => {
      console.log(`UnifiedDataContext received update - Bikes: ${updatedData.bikes?.length}`);
      
      // Filter bikes with consistent logic
      const validBikes = updatedData.bikes?.filter(bike => 
        bike !== null && 
        typeof bike === 'object' &&
        !bike.isDeleted &&
        !bike.isTest &&
        bike.id
      ) || [];
      
      const filteredData = {
        ...updatedData,
        bikes: validBikes
      };
      
      setDashboardData(filteredData);
      setLastUpdateTime(new Date());
      setLoading(false);
      setError(null);
      setShowUpdateIndicator(true);
      
      // Hide update indicator after 3 seconds
      setTimeout(() => {
        setShowUpdateIndicator(false);
      }, 3000);
    });
    
    // Cleanup listener on component unmount
    return () => {
      console.log('Cleaning up UnifiedDataContext listeners');
      unsubscribe();
    };
  }, []);

  const refreshData = () => {
    setLoading(true);
    setError(null);
    // The subscription will automatically fetch fresh data
  };

  // Context value with all shared data and state
  const contextValue = {
    // Data (DataContext compatibility)
    bikes: dashboardData.bikes,
    users: dashboardData.users, 
    rides: dashboardData.rides,
    reviews: dashboardData.reviews,
    stats: dashboardData.stats,
    
    // Analytics data (AnalyticsContext compatibility)
    data: dashboardData,
    
    // State
    loading,
    error,
    lastUpdateTime,
    showUpdateIndicator,
    
    // Methods
    refreshData,
    
    // Meta information
    bikeTypes: [...new Set(dashboardData.bikes.map(bike => bike.type))],
    
    // Helper function for components to trigger their own loading indicators
    setComponentLoading: (component, isLoading) => {
      console.log(`Component ${component} loading state: ${isLoading}`);
    }
  };

  return (
    <UnifiedDataContext.Provider value={contextValue}>
      {children}
    </UnifiedDataContext.Provider>
  );
};

export default UnifiedDataContext; 