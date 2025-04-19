import React, { useEffect, useState } from 'react';
import { loadGoogleMapsApi } from '../utils/googleMapsLoader';

// This component preloads the Google Maps API but doesn't render anything
// It's used at the app root level to ensure Maps is loaded early
const GoogleMapsPreloader = () => {
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadMaps = async () => {
      try {
        // Load maps early
        await loadGoogleMapsApi();
        console.log('Google Maps API preloaded successfully');
        setLoaded(true);
      } catch (err) {
        console.error('Error preloading Google Maps API:', err);
        setError(err.message);
      }
    };

    loadMaps();
  }, []);

  // This component doesn't render anything visible
  return null;
};

export default GoogleMapsPreloader; 