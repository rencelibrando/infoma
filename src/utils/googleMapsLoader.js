// Centralized Google Maps API loader
// This prevents multiple API loads and handles initialization properly

const GOOGLE_MAPS_API_KEY = process.env.REACT_APP_GOOGLE_MAPS_API_KEY || "AIzaSyASfb-LFSstZrbPUIgPn1rKOqNTFF6mhhk";
const LIBRARIES = ['places', 'geometry'];

let isLoaded = false;
let isLoading = false;
let loadPromise = null;
let callbacks = [];

export const loadGoogleMapsApi = () => {
  // If already loaded, return resolved promise
  if (isLoaded && window.google?.maps) {
    return Promise.resolve(window.google.maps);
  }
  
  // If currently loading, return the existing promise
  if (isLoading && loadPromise) {
    return loadPromise;
  }
  
  // Start loading process
  isLoading = true;
  
  loadPromise = new Promise((resolve, reject) => {
    // Add this callback to the queue
    callbacks.push(resolve);
    
    // If we're the first one to request loading, set up the script
    if (callbacks.length === 1) {
      const callbackName = `gmapsInitCallback_${Date.now()}`;
      
      // Set up the callback function
      window[callbackName] = () => {
        console.log('Google Maps API loaded successfully');
        isLoaded = true;
        isLoading = false;
        
        // Resolve all pending promises
        callbacks.forEach(cb => cb(window.google.maps));
        callbacks = [];
        
        // Clean up
        delete window[callbackName];
      };
      
      try {
        // Create script element
        const script = document.createElement('script');
        script.src = `https://maps.googleapis.com/maps/api/js?key=${GOOGLE_MAPS_API_KEY}&libraries=${LIBRARIES.join(',')}&callback=${callbackName}&v=weekly&loading=async`;
        script.async = true;
        script.defer = true;
        
        // Handle errors
        script.onerror = (err) => {
          console.error('Error loading Google Maps API:', err);
          isLoading = false;
          
          // Reject all pending promises
          callbacks.forEach(cb => reject(new Error('Failed to load Google Maps API')));
          callbacks = [];
          
          // Clean up
          delete window[callbackName];
        };
        
        // Add script to document
        document.head.appendChild(script);
      } catch (error) {
        console.error('Error setting up Google Maps API:', error);
        isLoading = false;
        
        // Reject all pending promises
        callbacks.forEach(cb => reject(error));
        callbacks = [];
        
        // Clean up
        delete window[callbackName];
      }
    }
  });
  
  return loadPromise;
};

// Check if maps is already loaded
export const isMapsLoaded = () => {
  return isLoaded && window.google?.maps;
}; 