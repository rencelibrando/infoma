import { useState, useRef, useCallback } from 'react';

/**
 * Centralized loading state manager to reduce redundancy
 * Supports multiple concurrent loading operations
 */
export const useLoadingManager = () => {
  const [loadingStates, setLoadingStates] = useState({});
  const loadingTimeouts = useRef({});

  const setLoading = useCallback((key, isLoading, minLoadingTime = 0) => {
    if (isLoading) {
      // Clear any existing timeout for this key
      if (loadingTimeouts.current[key]) {
        clearTimeout(loadingTimeouts.current[key]);
      }
      
      setLoadingStates(prev => ({ ...prev, [key]: true }));
      
      // Set minimum loading time if specified
      if (minLoadingTime > 0) {
        loadingTimeouts.current[key] = setTimeout(() => {
          setLoadingStates(prev => ({ ...prev, [key]: false }));
          delete loadingTimeouts.current[key];
        }, minLoadingTime);
      }
    } else {
      // If there's a timeout running, don't immediately stop loading
      if (!loadingTimeouts.current[key]) {
        setLoadingStates(prev => ({ ...prev, [key]: false }));
      }
    }
  }, []);

  const isLoading = useCallback((key) => {
    return Boolean(loadingStates[key]);
  }, [loadingStates]);

  const isAnyLoading = useCallback(() => {
    return Object.values(loadingStates).some(Boolean);
  }, [loadingStates]);

  const clearLoading = useCallback((key) => {
    if (loadingTimeouts.current[key]) {
      clearTimeout(loadingTimeouts.current[key]);
      delete loadingTimeouts.current[key];
    }
    setLoadingStates(prev => ({ ...prev, [key]: false }));
  }, []);

  const clearAllLoading = useCallback(() => {
    Object.keys(loadingTimeouts.current).forEach(key => {
      clearTimeout(loadingTimeouts.current[key]);
    });
    loadingTimeouts.current = {};
    setLoadingStates({});
  }, []);

  return {
    setLoading,
    isLoading,
    isAnyLoading,
    clearLoading,
    clearAllLoading,
    loadingStates
  };
};

/**
 * Simple loading hook for single operations
 */
export const useLoading = (initialState = false) => {
  const [loading, setLoading] = useState(initialState);
  
  const withLoading = useCallback(async (asyncOperation) => {
    setLoading(true);
    try {
      const result = await asyncOperation();
      return result;
    } finally {
      setLoading(false);
    }
  }, []);

  return [loading, setLoading, withLoading];
}; 