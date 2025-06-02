/**
 * Utility functions for coordinate validation and handling
 * Used to prevent Google Maps Polyline setAt errors
 */

/**
 * Validates if a coordinate point is valid for Google Maps
 * @param {Object} point - The coordinate point to validate
 * @param {number} point.lat - Latitude
 * @param {number} point.lng - Longitude
 * @returns {boolean} - True if the coordinate is valid
 */
export const isValidCoordinate = (point) => {
  return (
    point &&
    typeof point === 'object' &&
    typeof point.lat === 'number' &&
    typeof point.lng === 'number' &&
    !isNaN(point.lat) &&
    !isNaN(point.lng) &&
    isFinite(point.lat) &&
    isFinite(point.lng) &&
    Math.abs(point.lat) <= 90 &&
    Math.abs(point.lng) <= 180
  );
};

/**
 * Filters an array of coordinate points to only include valid ones
 * @param {Array} path - Array of coordinate points
 * @param {string} debugLabel - Optional label for debugging logs
 * @returns {Array} - Filtered array of valid coordinates
 */
export const filterValidCoordinates = (path, debugLabel = '') => {
  if (!Array.isArray(path)) {
    if (debugLabel) {
      console.warn(`Path ${debugLabel} is not an array:`, path);
    }
    return [];
  }

  const validPath = path.filter((point, index) => {
    const isValid = isValidCoordinate(point);
    if (!isValid && debugLabel) {
      console.warn(`Invalid coordinate filtered out ${debugLabel} at index ${index}:`, point);
    }
    return isValid;
  });

  if (debugLabel && validPath.length !== path.length) {
    console.log(`Filtered ${debugLabel}: ${validPath.length}/${path.length} valid points`);
  }

  return validPath;
};

/**
 * Validates if a coordinate path is suitable for rendering a Polyline
 * @param {Array} path - Array of coordinate points
 * @param {string} debugLabel - Optional label for debugging logs
 * @returns {boolean} - True if the path can be used for a Polyline
 */
export const isValidPolylinePath = (path, debugLabel = '') => {
  const validPath = filterValidCoordinates(path, debugLabel);
  const isValid = validPath.length >= 2;
  
  if (!isValid && debugLabel && path.length > 0) {
    console.log(`Insufficient valid points for polyline ${debugLabel}: ${validPath.length}/${path.length}`);
  }
  
  return isValid;
};

/**
 * Prepares a coordinate path for safe use with Google Maps Polyline
 * @param {Array} path - Array of coordinate points
 * @param {string} debugLabel - Optional label for debugging logs
 * @returns {Array|null} - Valid coordinate array or null if insufficient points
 */
export const preparePolylinePath = (path, debugLabel = '') => {
  const validPath = filterValidCoordinates(path, debugLabel);
  
  if (validPath.length < 2) {
    if (debugLabel && path.length > 0) {
      console.log(`Cannot create polyline ${debugLabel}: ${validPath.length}/${path.length} valid points`);
    }
    return null;
  }
  
  return validPath;
}; 