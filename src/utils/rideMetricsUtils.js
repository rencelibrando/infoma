/**
 * Shared utility functions for ride metrics calculations
 * Ensures consistency across React components and admin dashboard
 */

// Constants for validation
export const MAX_REALISTIC_SPEED_KMH = 100; // 100 km/h maximum for bikes
export const MIN_ACCURACY_THRESHOLD = 50; // 50 meters minimum accuracy
export const MAX_GPS_JUMP_DISTANCE = 100; // 100km maximum jump between GPS points (in km)

/**
 * Calculate distance between two geographic points using Haversine formula
 * Returns distance in kilometers
 */
export const calculateDistanceBetweenPoints = (point1, point2) => {
  if (!point1 || !point2 || 
      typeof point1.latitude !== 'number' || typeof point1.longitude !== 'number' ||
      typeof point2.latitude !== 'number' || typeof point2.longitude !== 'number') {
    return 0;
  }

  const R = 6371; // Earth's radius in km
  const dLat = (point2.latitude - point1.latitude) * Math.PI / 180;
  const dLon = (point2.longitude - point1.longitude) * Math.PI / 180;
  const a = 
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(point1.latitude * Math.PI / 180) * Math.cos(point2.latitude * Math.PI / 180) * 
    Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
};

/**
 * Validate GPS coordinate values
 */
export const isValidGPSCoordinate = (latitude, longitude) => {
  return typeof latitude === 'number' && 
         typeof longitude === 'number' &&
         Math.abs(latitude) <= 90 && 
         Math.abs(longitude) <= 180 &&
         latitude !== 0 && 
         longitude !== 0;
};

/**
 * Validate if distance between two points is realistic (filters GPS jumps)
 */
export const isRealisticDistance = (distanceKm, timeIntervalMs) => {
  if (timeIntervalMs <= 0) return true; // Can't validate without time data
  
  const timeHours = timeIntervalMs / 3600000; // Convert to hours
  const speedKmh = distanceKm / timeHours;
  
  return speedKmh < MAX_REALISTIC_SPEED_KMH && distanceKm < MAX_GPS_JUMP_DISTANCE;
};

/**
 * Validate speed value
 */
export const isRealisticSpeed = (speedKmh) => {
  return typeof speedKmh === 'number' && 
         speedKmh >= 0 && 
         speedKmh < MAX_REALISTIC_SPEED_KMH;
};

/**
 * Calculate total distance from a path with GPS noise filtering
 */
export const calculateTotalDistanceFromPath = (path) => {
  if (!path || path.length < 2) return 0;
  
  let totalDistance = 0;
  
  for (let i = 1; i < path.length; i++) {
    const prev = path[i - 1];
    const curr = path[i];
    
    // Validate coordinates
    if (isValidGPSCoordinate(prev.latitude, prev.longitude) &&
        isValidGPSCoordinate(curr.latitude, curr.longitude)) {
      
      const segmentDistance = calculateDistanceBetweenPoints(prev, curr);
      const timeInterval = (curr.timestamp || curr.deviceTimestamp || 0) - 
                          (prev.timestamp || prev.deviceTimestamp || 0);
      
      // Only add distance if it's realistic
      if (isRealisticDistance(segmentDistance, timeInterval)) {
        totalDistance += segmentDistance;
      }
    }
  }
  
  return totalDistance * 1000; // Convert to meters for consistency
};

/**
 * Calculate speeds from path data with validation
 */
export const calculateSpeedsFromPath = (path) => {
  if (!path || path.length === 0) return { maxSpeed: 0, averageSpeed: 0 };
  
  const validSpeeds = path
    .map(point => {
      // Handle different speed field formats
      const speed = point.speed || point.speedKmh || point.currentSpeed || 0;
      return isRealisticSpeed(speed) ? speed : 0;
    })
    .filter(speed => speed > 0);
  
  if (validSpeeds.length === 0) {
    return { maxSpeed: 0, averageSpeed: 0 };
  }
  
  const maxSpeed = Math.max(...validSpeeds);
  const averageSpeed = validSpeeds.reduce((sum, speed) => sum + speed, 0) / validSpeeds.length;
  
  return { maxSpeed, averageSpeed };
};

/**
 * Process ride data with enhanced calculations and fallbacks
 */
export const processRideData = (rideData) => {
  const duration = rideData.endTime ? 
    (rideData.endTime - rideData.startTime) : 
    (Date.now() - rideData.startTime);

  // Get initial values from ride record
  let maxSpeed = rideData.maxSpeed || 0;
  let averageSpeed = rideData.averageSpeed || 0;
  let totalDistance = rideData.totalDistance || rideData.distanceTraveled || 0;

  // Enhanced calculations from path if available
  if (rideData.path && rideData.path.length > 1) {
    // Calculate speeds from path if not available in record
    if (!maxSpeed || !averageSpeed) {
      const pathSpeeds = calculateSpeedsFromPath(rideData.path);
      if (!maxSpeed) maxSpeed = pathSpeeds.maxSpeed;
      if (!averageSpeed) averageSpeed = pathSpeeds.averageSpeed;
    }
    
    // Calculate distance from path if not available
    if (!totalDistance) {
      totalDistance = calculateTotalDistanceFromPath(rideData.path);
    }
  }

  // Final validation and cleanup
  maxSpeed = Math.max(0, Math.min(maxSpeed || 0, MAX_REALISTIC_SPEED_KMH));
  averageSpeed = Math.max(0, Math.min(averageSpeed || 0, MAX_REALISTIC_SPEED_KMH));
  totalDistance = Math.max(0, totalDistance || 0);

  return {
    ...rideData,
    duration,
    maxSpeed,
    averageSpeed,
    totalDistance,
    distanceTraveled: totalDistance // Alias for compatibility
  };
};

/**
 * Format distance for display
 */
export const formatDistance = (distanceInMeters) => {
  if (typeof distanceInMeters !== 'number' || distanceInMeters < 0) return '0 m';
  
  if (distanceInMeters < 1000) {
    return `${Math.round(distanceInMeters)} m`;
  } else if (distanceInMeters < 10000) {
    return `${(distanceInMeters / 1000).toFixed(2)} km`;
  } else {
    return `${(distanceInMeters / 1000).toFixed(1)} km`;
  }
};

/**
 * Format speed for display
 */
export const formatSpeed = (speedInKmh) => {
  if (typeof speedInKmh !== 'number' || speedInKmh < 0) return '0 km/h';
  
  if (speedInKmh >= MAX_REALISTIC_SPEED_KMH) return '99+ km/h';
  if (speedInKmh < 10) return `${speedInKmh.toFixed(1)} km/h`;
  return `${Math.round(speedInKmh)} km/h`;
};

/**
 * Format duration for display
 */
export const formatDuration = (durationMs) => {
  if (typeof durationMs !== 'number' || durationMs < 0) return '0:00';
  
  const hours = Math.floor(durationMs / 3600000);
  const minutes = Math.floor((durationMs % 3600000) / 60000);
  const seconds = Math.floor((durationMs % 60000) / 1000);
  
  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  } else {
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
};

/**
 * Format date and time for display
 */
export const formatDateTime = (timestamp) => {
  if (!timestamp) return 'N/A';
  try {
    const date = new Date(timestamp);
    return date.toLocaleString();
  } catch (error) {
    return 'Invalid date';
  }
}; 