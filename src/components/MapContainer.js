import React, { useState, useEffect, useRef } from 'react';
import { LoadScriptNext, GoogleMap } from '@react-google-maps/api';

// Define maps API key
const GOOGLE_MAPS_API_KEY = process.env.REACT_APP_GOOGLE_MAPS_API_KEY || "AIzaSyASfb-LFSstZrbPUIgPn1rKOqNTFF6mhhk";

// Libraries to load with the Maps API
const libraries = ['places', 'geometry'];

// Cache container to maintain map instance across renders
const mapCache = {
  map: null,
  lastCenter: null
};

const MapContainer = ({ center, zoom, children, className, style, onLoad }) => {
  const [isScriptLoaded, setIsScriptLoaded] = useState(false);
  const [isMapLoaded, setIsMapLoaded] = useState(false);
  const [loadError, setLoadError] = useState(null);
  const mapRef = useRef(null);
  const [mapKey] = useState(`google-map-${Date.now()}`);
  
  // Track whether component is mounted
  const isMounted = useRef(true);
  
  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  // Handle script loading
  const handleScriptLoad = () => {
    console.log("Google Maps script loaded successfully");
    if (isMounted.current) {
      setIsScriptLoaded(true);
    }
  };

  // Handle map loading
  const handleMapLoad = (map) => {
    console.log("Google Map instance loaded successfully");
    mapRef.current = map;
    mapCache.map = map;
    mapCache.lastCenter = center;
    
    if (isMounted.current) {
      setIsMapLoaded(true);
    }

    // Call the external onLoad handler if provided
    if (onLoad && typeof onLoad === 'function') {
      onLoad(map);
    }
  };

  // Handle script loading errors
  const handleScriptError = (error) => {
    console.error("Error loading Google Maps script:", error);
    setLoadError(`Failed to load Google Maps API: ${error.message || 'Unknown error'}`);
  };

  // Update center if needed
  useEffect(() => {
    if (mapRef.current && center && mapCache.lastCenter) {
      const hasLocationChanged = 
        center.lat !== mapCache.lastCenter.lat || 
        center.lng !== mapCache.lastCenter.lng;
        
      if (hasLocationChanged) {
        console.log("Updating map center to:", center);
        mapRef.current.panTo(center);
        mapCache.lastCenter = center;
      }
    }
  }, [center]);

  if (loadError) {
    return (
      <div style={style || { height: '100%', width: '100%' }} className={className}>
        <div style={{ 
          padding: '20px', 
          backgroundColor: 'white', 
          borderRadius: '8px', 
          textAlign: 'center',
          boxShadow: '0 2px 5px rgba(0,0,0,0.2)',
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          maxWidth: '80%'
        }}>
          <h3 style={{ color: 'red', marginBottom: '10px' }}>Maps API Error</h3>
          <p>{loadError}</p>
          <p>Please check console for more details.</p>
          <button 
            onClick={() => window.location.reload()}
            style={{
              marginTop: '10px',
              padding: '8px 16px',
              backgroundColor: '#4285F4',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            Reload Page
          </button>
        </div>
      </div>
    );
  }

  return (
    <LoadScriptNext
      googleMapsApiKey={GOOGLE_MAPS_API_KEY}
      libraries={libraries}
      onLoad={handleScriptLoad}
      onError={handleScriptError}
      loadingElement={
        <div style={style || { height: '100%', width: '100%' }} className={className}>
          <div style={{ 
            position: 'absolute', 
            top: '50%', 
            left: '50%', 
            transform: 'translate(-50%, -50%)'
          }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ 
                border: '4px solid #f3f3f3',
                borderTop: '4px solid #3498db',
                borderRadius: '50%',
                width: '30px',
                height: '30px',
                margin: '0 auto 10px',
                animation: 'spin 2s linear infinite',
              }}></div>
              <style>{`
                @keyframes spin {
                  0% { transform: rotate(0deg); }
                  100% { transform: rotate(360deg); }
                }
              `}</style>
              <p>Loading Google Maps...</p>
            </div>
          </div>
        </div>
      }
    >
      <GoogleMap
        mapContainerStyle={style || { height: '100%', width: '100%' }}
        center={center}
        zoom={zoom}
        mapContainerClassName={className}
        onLoad={handleMapLoad}
        options={{
          fullscreenControl: false,
          mapTypeControl: true,
          streetViewControl: false,
          gestureHandling: 'greedy',
          clickableIcons: false
        }}
      >
        {isScriptLoaded && isMapLoaded ? children : null}
      </GoogleMap>
    </LoadScriptNext>
  );
};

export default MapContainer; 