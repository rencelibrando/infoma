import React, { useState, useEffect, useCallback, useRef } from 'react';
import { GoogleMap, Marker, Polyline, InfoWindow } from '@react-google-maps/api';
import { collection, onSnapshot, query, where, orderBy, limit, doc, setDoc, getDoc } from 'firebase/firestore';
import { auth, db } from '../../firebase';
import './EnhancedRealTimeTracking.css';
import { preparePolylinePath, filterValidCoordinates } from '../../utils/coordinateUtils';

const mapContainerStyle = {
  width: '100%',
  height: '600px'
};

const defaultCenter = {
  lat: 14.5995, // Manila, Philippines
  lng: 120.9842
};

const mapOptions = {
  disableDefaultUI: false,
  zoomControl: true,
  streetViewControl: false,
  mapTypeControl: true,
  fullscreenControl: true,
  styles: [
    {
      featureType: 'poi',
      elementType: 'labels',
      stylers: [{ visibility: 'off' }]
    }
  ]
};

const EnhancedRealTimeTracking = () => {
  const [activeRiders, setActiveRiders] = useState({});
  const [selectedRider, setSelectedRider] = useState(null);
  const [riderPaths, setRiderPaths] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [dataError, setDataError] = useState(null);
  const [mapCenter, setMapCenter] = useState(defaultCenter);
  const [mapZoom, setMapZoom] = useState(12);
  const [showPaths, setShowPaths] = useState(true);
  const [autoCenter, setAutoCenter] = useState(true);
  const [filterOptions, setFilterOptions] = useState({
    showAll: true,
    showActive: true,
    showIdle: true,
    minSpeed: 0,
    maxSpeed: 100
  });

  const mapRef = useRef(null);
  const unsubscribeRef = useRef(null);
  const pathUnsubscribeRef = useRef({});

  const calculateDistance = (lat1, lon1, lat2, lon2) => {
    const R = 6371; // Earth's radius in km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
  };

  const formatTime = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString();
  };

  const formatDuration = (startTime) => {
    const duration = Date.now() - startTime;
    const minutes = Math.floor(duration / 60000);
    const hours = Math.floor(minutes / 60);
    if (hours > 0) {
      return `${hours}h ${minutes % 60}m`;
    }
    return `${minutes}m`;
  };

  const getMarkerIcon = (rider) => {
    const speed = rider.currentSpeed || 0;
    const isMoving = speed > 1; // Consider moving if speed > 1 km/h
    const color = isMoving ? '#4CAF50' : '#FF9800'; // Green for moving, orange for stationary
    
    return {
      url: `data:image/svg+xml,${encodeURIComponent(`
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16">
          <circle cx="8" cy="8" r="6" fill="${color}" stroke="#FFFFFF" stroke-width="2"/>
        </svg>
      `)}`,
      scaledSize: { width: 16, height: 16 },
      anchor: { x: 8, y: 8 }
    };
  };

  const centerMapOnRiders = useCallback(() => {
    if (!mapRef.current || Object.keys(activeRiders).length === 0) return;

    const riderLocations = Object.values(activeRiders)
      .filter(rider => rider.lastLocation)
      .map(rider => ({
        lat: rider.lastLocation.latitude,
        lng: rider.lastLocation.longitude
      }));

    if (riderLocations.length === 0) return;

    // Calculate bounds manually
    const bounds = {
      north: Math.max(...riderLocations.map(loc => loc.lat)),
      south: Math.min(...riderLocations.map(loc => loc.lat)),
      east: Math.max(...riderLocations.map(loc => loc.lng)),
      west: Math.min(...riderLocations.map(loc => loc.lng))
    };

    mapRef.current.fitBounds(bounds);
  }, [activeRiders]);

  useEffect(() => {
    if (autoCenter && Object.keys(activeRiders).length > 0) {
      centerMapOnRiders();
    }
  }, [activeRiders, autoCenter, centerMapOnRiders]);

  useEffect(() => {
    setIsLoading(true);
    setDataError(null);

    // Check if user is authenticated
    if (!auth.currentUser) {
      setDataError('User not authenticated. Please log in as an admin.');
      setIsLoading(false);
      return;
    }

    console.log('Starting real-time tracking for user:', auth.currentUser.email);

    // Listen to active rides from the rides collection
    let activeRidesQuery;
    try {
      // Now we can use orderBy since indexes are deployed
      console.log('Attempting to query rides by status=active with orderBy');
      activeRidesQuery = query(
        collection(db, 'rides'),
        where('status', '==', 'active'),
        orderBy('startTime', 'desc')
      );
    } catch (error) {
      console.log('Status query failed, using isActive fallback:', error);
      // Fallback to isActive field
      try {
        activeRidesQuery = query(
          collection(db, 'rides'),
          where('isActive', '==', true),
          orderBy('startTime', 'desc')
        );
      } catch (fallbackError) {
        console.log('isActive query failed, using simple query:', fallbackError);
        // Last resort - simple query without orderBy
        activeRidesQuery = query(
          collection(db, 'rides'),
          where('status', '==', 'active')
        );
      }
    }

    console.log('Setting up rides listener...');
    unsubscribeRef.current = onSnapshot(
      activeRidesQuery,
      (snapshot) => {
        console.log('Received snapshot with', snapshot.docs.length, 'documents');
        const riders = {};
        
        snapshot.docs.forEach(doc => {
          const data = doc.data();
          console.log('Processing ride document:', doc.id, data);
          
          // Skip inactive rides if we're getting all rides
          const isActiveRide = data.status === 'active' || data.isActive === true;
          if (!isActiveRide) {
            console.log('Skipping inactive ride:', doc.id);
            return;
          }
          
          // Create rider object from ride data
          const rider = {
            id: doc.id,
            rideId: doc.id,
            userName: data.userName || data.userDisplayName || 'Unknown User',
            userEmail: data.userEmail || 'No email',
            bikeId: data.bikeId,
            startTime: data.startTime || data.startDate?.toMillis() || Date.now(),
            lastLocationUpdate: data.lastLocationUpdate || data.updatedAt || Date.now(),
            isActive: isActiveRide,
            currentSpeed: data.currentSpeed || 0,
            // Handle location data - check multiple possible field names
            lastLocation: data.lastLocation || 
                         (data.currentLatitude && data.currentLongitude ? {
                           latitude: data.currentLatitude,
                           longitude: data.currentLongitude,
                           accuracy: data.locationAccuracy || 0
                         } : null) ||
                         (data.path && data.path.length > 0 ? 
                           data.path[data.path.length - 1] : null),
            distanceTraveled: data.distanceTraveled || 0,
            averageSpeed: data.averageSpeed || 0,
            maxSpeed: data.maxSpeed || 0
          };
          
          console.log('Created rider object:', rider);
          
          // Only include riders with valid location data
          if (rider.lastLocation && rider.lastLocation.latitude && rider.lastLocation.longitude) {
            riders[doc.id] = rider;
            console.log('Added rider with valid location:', doc.id);
          } else {
            console.log('Skipped rider without valid location:', doc.id, rider.lastLocation);
          }
        });

        console.log('Active riders loaded:', Object.keys(riders).length);
        setActiveRiders(riders);
        setIsLoading(false);

        // Load paths for new riders
        Object.keys(riders).forEach(rideId => {
          if (!pathUnsubscribeRef.current[rideId]) {
            loadRiderPath(rideId);
          }
        });

        // Clean up paths for riders no longer active
        Object.keys(pathUnsubscribeRef.current).forEach(rideId => {
          if (!riders[rideId]) {
            pathUnsubscribeRef.current[rideId]();
            delete pathUnsubscribeRef.current[rideId];
            setRiderPaths(prev => {
              const newPaths = { ...prev };
              delete newPaths[rideId];
              return newPaths;
            });
          }
        });
      },
      (error) => {
        console.error('Error fetching active rides:', error);
        console.error('Error details:', {
          code: error.code,
          message: error.message,
          details: error.details
        });
        setDataError(`Failed to load real-time data: ${error.message}. Code: ${error.code}`);
        setIsLoading(false);
      }
    );

    return () => {
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
      }
      Object.values(pathUnsubscribeRef.current).forEach(unsubscribe => unsubscribe());
    };
  }, []);

  const loadRiderPath = (rideId) => {
    // First try to get locations from a dedicated locations collection
    let pathQuery = query(
      collection(db, 'locations'),
      where('rideId', '==', rideId),
      orderBy('timestamp', 'desc'),
      limit(50)
    );

    pathUnsubscribeRef.current[rideId] = onSnapshot(pathQuery, (snapshot) => {
      if (snapshot.docs.length > 0) {
        // Use locations collection data
        const path = snapshot.docs.map(doc => {
          const data = doc.data();
          return {
            lat: data.latitude,
            lng: data.longitude,
            timestamp: data.timestamp
          };
        }).reverse(); // Reverse to get chronological order

        // Filter valid coordinates before storing
        const validPath = filterValidCoordinates(path, `locations_${rideId}`);
        if (validPath.length > 0) {
          setRiderPaths(prev => ({
            ...prev,
            [rideId]: validPath
          }));
        }
      } else {
        // Fallback: try to get path from the ride document itself
        const rideDocRef = doc(db, 'rides', rideId);
        const rideUnsubscribe = onSnapshot(rideDocRef, (rideDoc) => {
          if (rideDoc.exists()) {
            const rideData = rideDoc.data();
            const ridePath = rideData.path || [];
            
            if (ridePath.length > 0) {
              const path = ridePath.map(point => ({
                lat: point.latitude || point.lat,
                lng: point.longitude || point.lng,
                timestamp: point.timestamp
              }));

              // Filter valid coordinates before storing
              const validPath = filterValidCoordinates(path, `ride_${rideId}`);
              if (validPath.length > 0) {
                setRiderPaths(prev => ({
                  ...prev,
                  [rideId]: validPath
                }));
              }
            }
          }
        });
        
        // Replace the empty locations subscription with the ride document subscription
        pathUnsubscribeRef.current[rideId] = rideUnsubscribe;
      }
    }, (error) => {
      console.warn(`Error loading path for ride ${rideId}:`, error);
      // Try ride document fallback on error
      const rideDocRef = doc(db, 'rides', rideId);
      pathUnsubscribeRef.current[rideId] = onSnapshot(rideDocRef, (rideDoc) => {
        if (rideDoc.exists()) {
          const rideData = rideDoc.data();
          const ridePath = rideData.path || [];
          
          if (ridePath.length > 0) {
            const path = ridePath.map(point => ({
              lat: point.latitude || point.lat,
              lng: point.longitude || point.lng,
              timestamp: point.timestamp
            }));

            // Filter valid coordinates before storing
            const validPath = filterValidCoordinates(path, `ride_${rideId}`);
            if (validPath.length > 0) {
              setRiderPaths(prev => ({
                ...prev,
                [rideId]: validPath
              }));
            }
          }
        }
      });
    });
  };

  const filteredRiders = Object.values(activeRiders).filter(rider => {
    if (!filterOptions.showAll) {
      const speed = rider.currentSpeed || 0;
      const isMoving = speed > 1;
      
      if (!filterOptions.showActive && isMoving) return false;
      if (!filterOptions.showIdle && !isMoving) return false;
      if (speed < filterOptions.minSpeed || speed > filterOptions.maxSpeed) return false;
    }
    return true;
  });

  const totalRiders = Object.keys(activeRiders).length;
  const movingRiders = Object.values(activeRiders).filter(r => (r.currentSpeed || 0) > 1).length;

  if (isLoading) {
    return (
      <div className="real-time-tracking">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Loading real-time tracking data...</p>
        </div>
      </div>
    );
  }

  if (dataError) {
    return (
      <div className="real-time-tracking">
        <div className="error-container">
          <h3>Error Loading Data</h3>
          <p>{dataError}</p>
          <button onClick={() => window.location.reload()}>Retry</button>
        </div>
      </div>
    );
  }

  return (
    <div className="real-time-tracking">
      <div className="tracking-header">
        <h2>Real-Time Rider Tracking</h2>
        <div className="tracking-stats">
          <div className="stat-item">
            <span className="stat-value">{totalRiders}</span>
            <span className="stat-label">Active Riders</span>
          </div>
          <div className="stat-item">
            <span className="stat-value">{movingRiders}</span>
            <span className="stat-label">Moving</span>
          </div>
          <div className="stat-item">
            <span className="stat-value">{totalRiders - movingRiders}</span>
            <span className="stat-label">Stationary</span>
          </div>
        </div>
      </div>

      <div className="tracking-controls">
        <div className="control-group">
          <label>
            <input
              type="checkbox"
              checked={showPaths}
              onChange={(e) => setShowPaths(e.target.checked)}
            />
            Show Rider Paths
          </label>
          <label>
            <input
              type="checkbox"
              checked={autoCenter}
              onChange={(e) => setAutoCenter(e.target.checked)}
            />
            Auto Center Map
          </label>
        </div>

        <div className="control-group">
          <label>
            <input
              type="checkbox"
              checked={filterOptions.showActive}
              onChange={(e) => setFilterOptions(prev => ({
                ...prev,
                showActive: e.target.checked
              }))}
            />
            Show Moving Riders
          </label>
          <label>
            <input
              type="checkbox"
              checked={filterOptions.showIdle}
              onChange={(e) => setFilterOptions(prev => ({
                ...prev,
                showIdle: e.target.checked
              }))}
            />
            Show Stationary Riders
          </label>
        </div>

        <button 
          onClick={centerMapOnRiders}
          className="center-map-btn"
        >
          Center on All Riders
        </button>
      </div>

      <div className="tracking-content">
        <div className="map-container">
          {!window.google?.maps && (
            <div style={{
              height: '600px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              backgroundColor: '#f5f5f5',
              border: '2px solid #ddd',
              borderRadius: '8px'
            }}>
              <div style={{ textAlign: 'center' }}>
                <p>⏳ Waiting for Google Maps to load...</p>
                <p style={{ fontSize: '14px', color: '#666' }}>
                  If this persists, refresh the page
                </p>
              </div>
            </div>
          )}
          
          {window.google?.maps && (
            <GoogleMap
              mapContainerStyle={mapContainerStyle}
              center={mapCenter}
              zoom={mapZoom}
              options={mapOptions}
              onLoad={(map) => {
                console.log('✅ Real-time tracking map loaded successfully');
                mapRef.current = map;
              }}
              onError={(error) => {
                console.error('❌ Real-time tracking map error:', error);
                setDataError('Failed to load Google Maps');
              }}
              onClick={() => setSelectedRider(null)}
            >
              {/* Rider Markers */}
              {filteredRiders.map(rider => {
                if (!rider.lastLocation) return null;
                
                return (
                  <Marker
                    key={rider.id}
                    position={{
                      lat: rider.lastLocation.latitude,
                      lng: rider.lastLocation.longitude
                    }}
                    icon={getMarkerIcon(rider)}
                    onClick={() => setSelectedRider(rider)}
                    title={`${rider.userName} - ${(rider.currentSpeed || 0).toFixed(1)} km/h`}
                  />
                );
              })}

              {/* Rider Paths */}
              {showPaths && Object.entries(riderPaths).map(([rideId, path]) => {
                const rider = activeRiders[rideId];
                if (!rider) return null;
                
                // Use utility function to prepare safe polyline path
                const validPath = preparePolylinePath(path, `path_${rideId}`);
                if (!validPath) return null;

                return (
                  <Polyline
                    key={`path-${rideId}`}
                    path={validPath}
                    options={{
                      strokeColor: '#2196F3',
                      strokeOpacity: 0.7,
                      strokeWeight: 3,
                      geodesic: true
                    }}
                  />
                );
              })}

              {/* Info Window */}
              {selectedRider && selectedRider.lastLocation && (
                <InfoWindow
                  position={{
                    lat: selectedRider.lastLocation.latitude,
                    lng: selectedRider.lastLocation.longitude
                  }}
                  onCloseClick={() => setSelectedRider(null)}
                >
                  <div className="rider-info-window">
                    <h4>{selectedRider.userName}</h4>
                    <p><strong>Email:</strong> {selectedRider.userEmail}</p>
                    <p><strong>Ride ID:</strong> {selectedRider.rideId}</p>
                    <p><strong>Speed:</strong> {(selectedRider.currentSpeed || 0).toFixed(1)} km/h</p>
                    <p><strong>Duration:</strong> {formatDuration(selectedRider.startTime || selectedRider.lastLocationUpdate)}</p>
                    <p><strong>Last Update:</strong> {formatTime(selectedRider.lastLocationUpdate)}</p>
                    <p><strong>Accuracy:</strong> {selectedRider.lastLocation.accuracy?.toFixed(1) || 'N/A'}m</p>
                  </div>
                </InfoWindow>
              )}
            </GoogleMap>
          )}
        </div>

        <div className="riders-sidebar">
          <h3>Active Riders ({filteredRiders.length})</h3>
          <div className="riders-list">
            {filteredRiders.length === 0 ? (
              <div className="no-riders">
                <p>No active riders found</p>
              </div>
            ) : (
              filteredRiders.map(rider => (
                <div 
                  key={rider.id}
                  className={`rider-card ${selectedRider?.id === rider.id ? 'selected' : ''}`}
                  onClick={() => {
                    setSelectedRider(rider);
                    if (rider.lastLocation) {
                      setMapCenter({
                        lat: rider.lastLocation.latitude,
                        lng: rider.lastLocation.longitude
                      });
                      setMapZoom(16);
                    }
                  }}
                >
                  <div className="rider-header">
                    <span className="rider-name">{rider.userName}</span>
                    <span className={`rider-status ${(rider.currentSpeed || 0) > 1 ? 'moving' : 'stationary'}`}>
                      {(rider.currentSpeed || 0) > 1 ? 'Moving' : 'Stationary'}
                    </span>
                  </div>
                  <div className="rider-details">
                    <p>Speed: {(rider.currentSpeed || 0).toFixed(1)} km/h</p>
                    <p>Duration: {formatDuration(rider.startTime || rider.lastLocationUpdate)}</p>
                    <p>Last seen: {formatTime(rider.lastLocationUpdate)}</p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default EnhancedRealTimeTracking; 