/**
 * Request deduplication utility to prevent redundant API calls
 * Multiple components requesting the same data will share a single request
 */
class RequestDeduplicator {
  constructor() {
    this.pendingRequests = new Map();
    this.cache = new Map();
    this.cacheExpiry = new Map();
  }

  /**
   * Deduplicate requests by key
   * @param {string} key - Unique identifier for the request
   * @param {Function} requestFn - Function that makes the actual request
   * @param {number} cacheTtl - Cache time-to-live in milliseconds (default: 30 seconds)
   * @returns {Promise} - The request result
   */
  async deduplicate(key, requestFn, cacheTtl = 30000) {
    // Check if we have a valid cached result
    const cached = this.getCached(key);
    if (cached !== null) {
      console.log(`RequestDeduplicator: Returning cached result for ${key}`);
      return cached;
    }

    // Check if there's already a pending request for this key
    if (this.pendingRequests.has(key)) {
      console.log(`RequestDeduplicator: Joining existing request for ${key}`);
      return this.pendingRequests.get(key);
    }

    // Create new request
    console.log(`RequestDeduplicator: Creating new request for ${key}`);
    const requestPromise = this.executeRequest(key, requestFn, cacheTtl);
    
    // Store the promise so other calls can join it
    this.pendingRequests.set(key, requestPromise);

    return requestPromise;
  }

  /**
   * Execute the actual request and handle caching
   */
  async executeRequest(key, requestFn, cacheTtl) {
    try {
      const result = await requestFn();
      
      // Cache the result
      this.cache.set(key, result);
      this.cacheExpiry.set(key, Date.now() + cacheTtl);
      
      // Remove from pending requests
      this.pendingRequests.delete(key);
      
      return result;
    } catch (error) {
      // Remove from pending requests on error
      this.pendingRequests.delete(key);
      throw error;
    }
  }

  /**
   * Get cached result if valid
   */
  getCached(key) {
    if (!this.cache.has(key)) {
      return null;
    }

    const expiry = this.cacheExpiry.get(key);
    if (Date.now() > expiry) {
      // Cache expired
      this.cache.delete(key);
      this.cacheExpiry.delete(key);
      return null;
    }

    return this.cache.get(key);
  }

  /**
   * Invalidate cache for a specific key
   */
  invalidate(key) {
    this.cache.delete(key);
    this.cacheExpiry.delete(key);
    
    // Also cancel pending request if any
    if (this.pendingRequests.has(key)) {
      this.pendingRequests.delete(key);
    }
    
    console.log(`RequestDeduplicator: Invalidated cache for ${key}`);
  }

  /**
   * Clear all cache and pending requests
   */
  clearAll() {
    this.cache.clear();
    this.cacheExpiry.clear();
    this.pendingRequests.clear();
    console.log('RequestDeduplicator: Cleared all cache and pending requests');
  }

  /**
   * Get cache statistics
   */
  getStats() {
    return {
      cacheSize: this.cache.size,
      pendingRequests: this.pendingRequests.size,
      cacheKeys: Array.from(this.cache.keys())
    };
  }
}

// Create singleton instance
const requestDeduplicator = new RequestDeduplicator();

export default requestDeduplicator;

// Helper function for easy usage
export const deduplicate = (key, requestFn, cacheTtl) => 
  requestDeduplicator.deduplicate(key, requestFn, cacheTtl);

// Helper function to invalidate specific cache
export const invalidateCache = (key) => 
  requestDeduplicator.invalidate(key);

// Helper function to clear all cache
export const clearAllCache = () => 
  requestDeduplicator.clearAll(); 