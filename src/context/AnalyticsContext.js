import React, { createContext, useContext, useState, useEffect } from 'react';
import { subscribeToAnalytics, clearAllCache } from '../services/dashboardService';

const AnalyticsContext = createContext();

export const useAnalytics = () => {
  const context = useContext(AnalyticsContext);
  if (!context) {
    throw new Error('useAnalytics must be used within an AnalyticsProvider');
  }
  return context;
};

export const AnalyticsProvider = ({ children }) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    
    // Clear all caches to ensure fresh data
    clearAllCache();
    
    // Set up real-time subscription
    const unsubscribe = subscribeToAnalytics((analyticsData) => {
      console.log('AnalyticsContext received data update');
      setData(analyticsData);
      setLoading(false);
      setError(null);
    });

    // Cleanup subscription on unmount
    return () => {
      console.log('AnalyticsContext cleaning up subscription');
      unsubscribe();
    };
  }, []);

  const refreshData = () => {
    setLoading(true);
    setError(null);
    clearAllCache();
    // The subscription will automatically fetch fresh data
  };

  const value = {
    data,
    loading,
    error,
    refreshData
  };

  return (
    <AnalyticsContext.Provider value={value}>
      {children}
    </AnalyticsContext.Provider>
  );
};

export default AnalyticsContext; 