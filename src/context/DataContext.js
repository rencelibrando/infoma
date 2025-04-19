import React, { createContext, useContext, useState, useEffect } from 'react';
import { getAnalyticsData, subscribeToAnalytics } from '../services/dashboardService';

// Create context
const DataContext = createContext(null);

// Custom hook for accessing the context
export const useDataContext = () => useContext(DataContext);

export const DataProvider = ({ children }) => {
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
      verifiedUsers: 0,
      activeRides: 0,
      totalRides: 0,
      totalReviews: 0,
      averageRating: 0
    }
  });
  const [loading, setLoading] = useState(true);
  const [lastUpdateTime, setLastUpdateTime] = useState(new Date());
  const [showUpdateIndicator, setShowUpdateIndicator] = useState(false);

  // Load data and set up real-time listeners
  useEffect(() => {
    console.log('Initializing DataContext and loading shared data...');
    setLoading(true);
    
    // Initial data fetch
    const initialFetch = async () => {
      try {
        const data = await getAnalyticsData();
        setDashboardData(data);
        setLastUpdateTime(new Date());
        setLoading(false);
        console.log('Initial data loaded successfully');
      } catch (error) {
        console.error('Error fetching initial dashboard data:', error);
        setLoading(false);
      }
    };
    
    initialFetch();
    
    // Set up real-time listener for all collections
    const unsubscribe = subscribeToAnalytics((updatedData) => {
      setDashboardData(updatedData);
      setLastUpdateTime(new Date());
      setShowUpdateIndicator(true);
      
      // Hide update indicator after 3 seconds
      setTimeout(() => {
        setShowUpdateIndicator(false);
      }, 3000);
    });
    
    // Cleanup listener on component unmount
    return () => {
      console.log('Cleaning up DataContext listeners');
      unsubscribe();
    };
  }, []);

  // Context value with all shared data and state
  const contextValue = {
    // Data
    bikes: dashboardData.bikes,
    users: dashboardData.users, 
    rides: dashboardData.rides,
    reviews: dashboardData.reviews,
    stats: dashboardData.stats,
    
    // State
    loading,
    lastUpdateTime,
    showUpdateIndicator,
    
    // Meta information
    bikeTypes: [...new Set(dashboardData.bikes.map(bike => bike.type))],
    
    // Helper function for components to trigger their own loading indicators
    setComponentLoading: (component, isLoading) => {
      console.log(`Component ${component} loading state: ${isLoading}`);
    }
  };

  return (
    <DataContext.Provider value={contextValue}>
      {children}
    </DataContext.Provider>
  );
};

export default DataContext; 