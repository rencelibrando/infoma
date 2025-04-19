import React, { useState, useEffect, useRef, forwardRef } from 'react';
import { GoogleMap } from '@react-google-maps/api';
import { loadGoogleMapsApi, isMapsLoaded } from '../utils/googleMapsLoader';

// Cache container to maintain map instance across renders
const mapCache = {
  map: null,
  lastCenter: null
};

// Use forwardRef to properly handle ref passing from parent components
const MapContainer = forwardRef(({ center, zoom = 14, children, className, style, onLoad }, ref) => {
  const [isMapLoaded, setIsMapLoaded] = useState(false);
  const [loadError, setLoadError] = useState(null);
  const mapRef = useRef(null);
  
  // Track whether component is mounted
  const isMounted = useRef(true);
  
  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  // Load Google Maps API
  useEffect(() => {
    async function initMap() {
      try {
        // Check if maps API is already loaded
        if (isMapsLoaded()) {
          setIsMapLoaded(true);
          return;
        }
        
        // Load maps API
        await loadGoogleMapsApi();
        
        if (isMounted.current) {
          setIsMapLoaded(true);
        }
      } catch (error) {
        console.error("Error loading Google Maps:", error);
        if (isMounted.current) {
          setLoadError(error.message);
        }
      }
    }
    
    initMap();
  }, []);

  // Handle map loading
  const handleMapLoad = (map) => {
    console.log("Google Map instance loaded successfully");
    mapRef.current = map;
    mapCache.map = map;
    mapCache.lastCenter = center;
    
    // Use the ref if provided
    if (ref) {
      if (typeof ref === 'function') {
        ref(map);
      } else {
        ref.current = map;
      }
    }

    // Call the external onLoad handler if provided
    if (onLoad && typeof onLoad === 'function') {
      onLoad(map);
    }
  };

  if (loadError) {
    return (
      <div style={{ 
        height: '100%', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center', 
        backgroundColor: '#f5f5f5' 
      }}>
        <div style={{ 
          padding: '20px', 
          backgroundColor: 'white', 
          borderRadius: '8px', 
          maxWidth: '80%', 
          textAlign: 'center' 
        }}>
          <h3 style={{ color: 'red', marginBottom: '10px' }}>Error Loading Maps</h3>
          <p>{loadError}</p>
          <p>Please check browser console for more details.</p>
        </div>
      </div>
    );
  }

  if (!isMapLoaded) {
    return (
      <div style={{ 
        height: '100%', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center' 
      }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ 
            border: '4px solid #f3f3f3', 
            borderTop: '4px solid #1D3C34', 
            borderRadius: '50%', 
            width: '30px', 
            height: '30px', 
            animation: 'spin 2s linear infinite',
            margin: '0 auto 10px auto'
          }} />
          <p>Loading map...</p>
          <style>{`
            @keyframes spin {
              0% { transform: rotate(0deg); }
              100% { transform: rotate(360deg); }
            }
          `}</style>
        </div>
      </div>
    );
  }

  return (
    <GoogleMap
      center={center}
      zoom={zoom}
      mapContainerClassName={className}
      mapContainerStyle={style || { height: '100%', width: '100%' }}
      onLoad={handleMapLoad}
    >
      {children}
    </GoogleMap>
  );
});

MapContainer.displayName = 'MapContainer';

export default MapContainer; 