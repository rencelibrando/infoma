// Google Maps configuration for Infoma Real-time Tracking
// This file centralizes all map-related settings and constants

// ====================================================================
// IMPORTANT: ADD YOUR GOOGLE MAPS API KEY HERE OR IN .env.local
// ====================================================================
// Option 1: Create .env.local file in infoma-main directory with:
//   REACT_APP_GOOGLE_MAPS_API_KEY=your_actual_api_key_here
// 
// Option 2: Replace 'YOUR_API_KEY_HERE' below with your actual API key
// ====================================================================

export const MAPS_CONFIG = {
  // Google Maps API settings
  API_KEY: process.env.REACT_APP_GOOGLE_MAPS_API_KEY || 'AIzaSyAgkXbkJMvuxJs9d1bBYlSgxxgp1lcoRaU',
  LIBRARIES: ['places', 'geometry'],
  LANGUAGE: 'en',
  REGION: 'PH', // Philippines
  
  // Map display settings
  DEFAULT_CENTER: {
    lat: 14.5995,  // Manila, Philippines
    lng: 120.9842
  },
  DEFAULT_ZOOM: 13,
  MIN_ZOOM: 10,
  MAX_ZOOM: 20,
  
  // Map styling options
  MAP_STYLES: [
    {
      featureType: 'poi',
      elementType: 'labels',
      stylers: [{ visibility: 'off' }]
    },
    {
      featureType: 'transit',
      elementType: 'labels',
      stylers: [{ visibility: 'off' }]
    }
  ],
  
  // Real-time tracking settings
  TRACKING: {
    UPDATE_INTERVAL: 5000,        // 5 seconds
    LOCATION_TIMEOUT: 30000,      // 30 seconds before location is stale
    AUTO_CENTER_DURATION: 2000,   // 2 seconds
    MARKER_ANIMATION_DURATION: 300,
    CLUSTER_MAX_ZOOM: 15
  },
  
  // Marker configuration
  MARKERS: {
    BIKE: {
      SIZE: 32,
      SCALE: 1,
      ANIMATION: 'BOUNCE'
    },
    USER: {
      SIZE: 24,
      SCALE: 0.8,
      ANIMATION: 'DROP'
    },
    CLUSTER: {
      GRID_SIZE: 60,
      MAX_ZOOM: 15,
      STYLES: [
        {
          textColor: 'white',
          textSize: 11,
          height: 40,
          width: 40,
          backgroundColor: '#3B82F6'
        },
        {
          textColor: 'white', 
          textSize: 12,
          height: 50,
          width: 50,
          backgroundColor: '#1D4ED8'
        },
        {
          textColor: 'white',
          textSize: 13,
          height: 60,
          width: 60,
          backgroundColor: '#1E3A8A'
        }
      ]
    }
  },
  
  // Colors for different states
  COLORS: {
    ACTIVE_RIDE: '#10B981',      // Green
    AVAILABLE_BIKE: '#3B82F6',   // Blue
    MAINTENANCE: '#F59E0B',      // Amber
    UNAVAILABLE: '#EF4444',      // Red
    EMERGENCY: '#DC2626',        // Dark red
    LOW_BATTERY: '#F97316',      // Orange
    OFFLINE: '#6B7280'           // Gray
  },
  
  // Icon URLs and paths
  ICONS: {
    BIKE_AVAILABLE: '/images/bike-available.png',
    BIKE_IN_USE: '/images/bike-in-use.png',
    BIKE_MAINTENANCE: '/images/bike-maintenance.png',
    USER_ACTIVE: '/images/user-active.png',
    USER_EMERGENCY: '/images/user-emergency.png',
    STATION: '/images/station.png'
  },
  
  // Geofencing and boundaries
  GEOFENCE: {
    METRO_MANILA: {
      NORTH: 14.7500,
      SOUTH: 14.4000,
      EAST: 121.1500,
      WEST: 120.8000
    },
    CEBU: {
      NORTH: 10.3500,
      SOUTH: 10.2500,
      EAST: 123.9500,
      WEST: 123.8500
    }
  },
  
  // Performance optimization
  PERFORMANCE: {
    DEBOUNCE_DELAY: 300,         // ms
    THROTTLE_INTERVAL: 1000,     // ms  
    MAX_MARKERS: 1000,           // Maximum markers to display
    VIEWPORT_PADDING: 50,        // pixels
    SMOOTH_ANIMATION: true
  }
};

// Helper functions for map operations
export const mapHelpers = {
  // Check if coordinates are within Philippines bounds
  isWithinPhilippines: (lat, lng) => {
    return lat >= 4.5 && lat <= 21.5 && lng >= 116.0 && lng <= 127.0;
  },
  
  // Get appropriate zoom level based on number of markers
  getOptimalZoom: (markerCount) => {
    if (markerCount <= 5) return 15;
    if (markerCount <= 20) return 13;
    if (markerCount <= 50) return 11;
    return 10;
  },
  
  // Calculate bounds for multiple markers
  calculateBounds: (markers) => {
    if (!markers || markers.length === 0) return null;
    
    let north = -90, south = 90, east = -180, west = 180;
    
    markers.forEach(marker => {
      const lat = marker.position?.lat || marker.lat;
      const lng = marker.position?.lng || marker.lng;
      
      if (lat && lng) {
        north = Math.max(north, lat);
        south = Math.min(south, lat);
        east = Math.max(east, lng);
        west = Math.min(west, lng);
      }
    });
    
    return { north, south, east, west };
  },
  
  // Format coordinates for display
  formatCoordinates: (lat, lng, precision = 6) => {
    if (typeof lat !== 'number' || typeof lng !== 'number') {
      return 'Invalid coordinates';
    }
    return `${lat.toFixed(precision)}, ${lng.toFixed(precision)}`;
  },
  
  // Calculate distance between two points (in km)
  calculateDistance: (lat1, lng1, lat2, lng2) => {
    const R = 6371; // Earth's radius in km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLng = (lng2 - lng1) * Math.PI / 180;
    const a = 
      Math.sin(dLat/2) * Math.sin(dLat/2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
      Math.sin(dLng/2) * Math.sin(dLng/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
  },
  
  // Get marker icon based on status
  getMarkerIcon: (type, status, size = 32) => {
    const color = MAPS_CONFIG.COLORS[status?.toUpperCase()] || MAPS_CONFIG.COLORS.OFFLINE;
    
    return {
      url: `data:image/svg+xml,${encodeURIComponent(`
        <svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="12" cy="12" r="10" fill="${color}" stroke="white" stroke-width="2"/>
          <text x="12" y="16" text-anchor="middle" fill="white" font-size="10" font-weight="bold">
            ${type === 'bike' ? 'B' : 'U'}
          </text>
        </svg>
      `)}`,
      scaledSize: { width: size, height: size },
      anchor: { x: size/2, y: size/2 }
    };
  },
  
  // Validate Google Maps API key
  validateApiKey: () => {
    const apiKey = MAPS_CONFIG.API_KEY;
    if (!apiKey) {
      console.error('❌ Google Maps API key is missing. Please set REACT_APP_GOOGLE_MAPS_API_KEY environment variable.');
      return false;
    }
    if (apiKey.length < 30) {
      console.warn('⚠️ Google Maps API key appears to be invalid (too short).');
      return false;
    }
    return true;
  }
};

// Export map configuration validation
export const validateMapConfig = () => {
  const errors = [];
  
  if (!mapHelpers.validateApiKey()) {
    errors.push('Invalid or missing Google Maps API key');
  }
  
  if (!MAPS_CONFIG.DEFAULT_CENTER.lat || !MAPS_CONFIG.DEFAULT_CENTER.lng) {
    errors.push('Invalid default center coordinates');
  }
  
  if (MAPS_CONFIG.DEFAULT_ZOOM < 1 || MAPS_CONFIG.DEFAULT_ZOOM > 20) {
    errors.push('Invalid default zoom level');
  }
  
  return {
    isValid: errors.length === 0,
    errors
  };
};

// Performance monitoring for maps
export const mapsPerformance = {
  startTimer: (operation) => {
    const start = performance.now();
    return () => {
      const end = performance.now();
      console.log(`🗺️ Map operation '${operation}' took ${(end - start).toFixed(2)}ms`);
    };
  },
  
  logMapEvent: (event, data) => {
    console.log(`🗺️ Map Event: ${event}`, data);
  }
};

export default MAPS_CONFIG; 