import React, { useState, useEffect } from 'react';
import { LoadScript, GoogleMap } from '@react-google-maps/api';

const MapContainer = ({ center, zoom, children, className, style, onLoad }) => {
  const [key, setKey] = useState(`google-map-${Date.now()}`);
  const [mounted, setMounted] = useState(true);

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        // Unmount the component when tab becomes hidden
        setMounted(false);
      } else if (document.visibilityState === 'visible') {
        // Force remount with new key when tab becomes visible again
        setKey(`google-map-${Date.now()}`);
        setMounted(true);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  if (!mounted) {
    return <div style={style || { height: '100%', width: '100%' }} className={className}>Map loading...</div>;
  }

  return (
    <LoadScript
      googleMapsApiKey="AIzaSyCmhSJa07ZS67ZcKnJOmwHHMz-qzKhjShE"
      loadingElement={<div>Loading Maps...</div>}
      key={key}
    >
      <GoogleMap
        mapContainerStyle={style || { height: '100%', width: '100%' }}
        center={center}
        zoom={zoom}
        mapContainerClassName={className}
        onLoad={onLoad}
      >
        {children}
      </GoogleMap>
    </LoadScript>
  );
};

export default MapContainer; 