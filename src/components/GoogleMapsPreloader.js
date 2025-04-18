import React, { useEffect, useState } from 'react';

const GOOGLE_MAPS_API_KEY = process.env.REACT_APP_GOOGLE_MAPS_API_KEY || "AIzaSyASfb-LFSstZrbPUIgPn1rKOqNTFF6mhhk";
const LIBRARIES = ['places', 'geometry'];

/**
 * A component that preloads the Google Maps API
 * This helps optimize map loading across the application
 */
const GoogleMapsPreloader = () => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Check if we've already loaded the API
    if (window.google?.maps) {
      setIsLoaded(true);
      return;
    }

    // Create a unique callback name
    const callbackName = `gmapsCallback_${Date.now()}`;
    
    // Set up the callback function
    window[callbackName] = () => {
      console.log('Google Maps API preloaded successfully');
      setIsLoaded(true);
      delete window[callbackName]; // Clean up
    };

    // Create script element
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${GOOGLE_MAPS_API_KEY}&libraries=${LIBRARIES.join(',')}&callback=${callbackName}&v=quarterly`;
    script.async = true;
    script.defer = true;
    
    // Handle errors
    script.onerror = (err) => {
      console.error('Error preloading Google Maps API:', err);
      setError('Failed to preload Google Maps API');
      delete window[callbackName]; // Clean up
    };

    // Add script to document
    document.head.appendChild(script);

    // Cleanup
    return () => {
      if (script.parentNode) {
        script.parentNode.removeChild(script);
      }
      delete window[callbackName];
    };
  }, []);

  // This component doesn't render anything visible
  return null;
};

export default GoogleMapsPreloader; 