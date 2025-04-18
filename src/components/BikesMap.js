import React, { useState, useEffect } from 'react';
import { getBikes, toggleBikeLock, subscribeToBikes, fixBikeCoordinates } from '../services/bikeService';
import { getActiveRides } from '../services/bikeLocationService';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '../firebase';
import styled from 'styled-components';
import { Marker, InfoWindow } from '@react-google-maps/api';
import BikeQRCode from './BikeQRCode';
import MapContainer from './MapContainer';

// Pine green and gray theme colors
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  accent: '#FF8C00',
  success: '#4CAF50',
  danger: '#d32f2f',
  warning: '#FFC107'
};

const BikesMapContainer = styled.div`
  padding: 20px;
`;

const Title = styled.h2`
  margin-bottom: 20px;
  color: ${colors.darkGray};
`;

const MapWrapper = styled.div`
  height: 600px;
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
`;

const StatsContainer = styled.div`
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
`;

const StatCard = styled.div`
  background-color: ${colors.white};
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  flex: 1;
  text-align: center;
`;

const StatValue = styled.div`
  font-size: 24px;
  font-weight: bold;
  color: ${props => props.color || colors.pineGreen};
`;

const StatLabel = styled.div`
  font-size: 14px;
  color: ${colors.mediumGray};
  margin-top: 5px;
`;

const ActionButton = styled.button`
  background-color: ${props => props.locked ? colors.warning : colors.success};
  color: white;
  border: none;
  border-radius: 4px;
  padding: 6px 12px;
  cursor: pointer;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  
  &:hover {
    opacity: 0.85;
  }
`;

const InfoWindowContent = styled.div`
  padding: 5px;
  width: 150px;
`;

const BikeTitle = styled.h3`
  font-size: 14px;
  margin-bottom: 5px;
  color: ${colors.darkGray};
`;

const BikeDetail = styled.p`
  font-size: 12px;
  margin: 3px 0;
  color: ${colors.mediumGray};
`;

const UserBadge = styled.div`
  display: flex;
  align-items: center;
  background-color: #e8f5e9;
  border-radius: 4px;
  padding: 5px;
  margin-top: 8px;
`;

const UserIcon = styled.div`
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: ${colors.pineGreen};
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  margin-right: 5px;
`;

const UserInfo = styled.div`
  font-size: 11px;
  color: ${colors.pineGreen};
`;

const LoadingMessage = styled.div`
  text-align: center;
  padding: 40px;
  color: ${colors.mediumGray};
`;

const ErrorMessage = styled.div`
  text-align: center;
  padding: 20px;
  color: ${colors.danger};
  background-color: #ffebee;
  border-radius: 4px;
  margin: 20px 0;
`;

const StatusBadge = styled.span`
  display: inline-block;
  padding: 3px 6px;
  border-radius: 4px;
  font-size: 11px;
  background-color: ${props => 
    props.isInUse ? '#ffebee' : 
    props.isAvailable ? '#e8f5e9' : 
    '#fff3cd'};
  color: ${props => 
    props.isInUse ? '#b71c1c' : 
    props.isAvailable ? '#2e7d32' : 
    '#856404'};
  margin-top: 5px;
`;

const RefreshIndicator = styled.div`
  position: fixed;
  top: 10px;
  right: 10px;
  background-color: ${colors.pineGreen};
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  z-index: 1000;
  animation: fadeOut 2s forwards;
  animation-delay: 1s;
  
  @keyframes fadeOut {
    to {
      opacity: 0;
      visibility: hidden;
    }
  }
`;

const LastUpdateTime = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  text-align: right;
  margin-bottom: 5px;
`;

const BikesMap = () => {
  const [bikes, setBikes] = useState([]);
  const [activeRides, setActiveRides] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedBike, setSelectedBike] = useState(null);
  const [mapCenter, setMapCenter] = useState({ lat: 0, lng: 0 });
  const [processingBikeAction, setProcessingBikeAction] = useState(null);
  const [users, setUsers] = useState({}); // To store user data
  const [lastUpdateTime, setLastUpdateTime] = useState(new Date());
  const [showUpdateIndicator, setShowUpdateIndicator] = useState(false);
  const [mapsApiLoaded, setMapsApiLoaded] = useState(false);

  // Add visibility change listener
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        console.log('Tab is visible again - refreshing map');
        fetchActiveRides();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  useEffect(() => {
    setLoading(true);
    
    // Initial data fetch
    const initialFetch = async () => {
      try {
        const bikeData = await getBikes();
        console.log("Loaded bikes data:", bikeData);
        
        // Check if bikes have valid coordinates
        const bikesWithCoords = bikeData.filter(bike => bike.latitude && bike.longitude);
        console.log(`Found ${bikesWithCoords.length}/${bikeData.length} bikes with valid coordinates`);
        
        setBikes(bikeData);
        setLastUpdateTime(new Date());
        
        // Update map center if needed
        if (bikeData.length > 0) {
          const validBike = bikeData.find(bike => bike.latitude && bike.longitude);
          if (validBike) {
            console.log("Setting map center to:", { lat: validBike.latitude, lng: validBike.longitude });
            setMapCenter({ lat: validBike.latitude, lng: validBike.longitude });
          } else {
            console.warn("No bikes with valid coordinates found for map centering");
          }
        }
      } catch (err) {
        console.error('Failed to fetch bikes:', err);
        setError('Failed to fetch bikes: ' + err.message);
      } finally {
        setLoading(false);
      }
    };
    
    initialFetch();
    fetchActiveRides();
    
    // Set up real-time listener for bikes
    const unsubscribe = subscribeToBikes((updatedBikes) => {
      setBikes(updatedBikes);
      setLastUpdateTime(new Date());
      setShowUpdateIndicator(true);
      
      // Update selected bike if it exists WITHOUT triggering the effect again
      if (selectedBike) {
        const updatedSelectedBike = updatedBikes.find(b => b.id === selectedBike.id);
        if (updatedSelectedBike) {
          setSelectedBike(prev => {
            // Only update if data actually changed to avoid loops
            if (JSON.stringify(prev) !== JSON.stringify(updatedSelectedBike)) {
              return updatedSelectedBike;
            }
            return prev;
          });
        }
      }
      
      // Hide update indicator after 3 seconds
      setTimeout(() => {
        setShowUpdateIndicator(false);
      }, 3000);
    });
    
    // Set up a timer to refresh active rides data every 60 seconds (changed from 30)
    const refreshInterval = setInterval(() => {
      fetchActiveRides();
    }, 60000);
    
    // Cleanup listeners on component unmount
    return () => {
      unsubscribe();
      clearInterval(refreshInterval);
    };
  }, []); // Removed selectedBike from dependencies to prevent loop
  
  // Only fetch active rides - bikes will come from real-time subscription
  const fetchActiveRides = async () => {
    try {
      const activeRidesData = await getActiveRides();
      setActiveRides(activeRidesData);
      
      // Fetch user information for active rides
      const userIds = [...new Set(activeRidesData.map(ride => ride.userId))];
      
      // Fetch user information
      const userPromises = userIds.map(async (userId) => {
        try {
          const userDoc = await getDoc(doc(db, "users", userId));
          if (userDoc.exists()) {
            return { id: userId, ...userDoc.data() };
          }
          return { id: userId, name: 'Unknown User' };
        } catch (error) {
          console.error(`Error fetching user ${userId}:`, error);
          return { id: userId, name: 'Unknown User' };
        }
      });
      
      const usersData = await Promise.all(userPromises);
      
      // Create a map of user IDs to user data
      const usersMap = {};
      usersData.forEach(user => {
        usersMap[user.id] = user;
      });
      
      setUsers(usersMap);
    } catch (error) {
      console.error('Error fetching active rides:', error);
    }
  };

  const handleToggleLock = async (bike) => {
    try {
      setProcessingBikeAction(bike.id);
      const newLockState = !bike.isLocked;
      
      // Prevent locking a bike that's in use
      if (newLockState && bike.isInUse) {
        alert("Cannot lock a bike that is currently in use.");
        setProcessingBikeAction(null);
        return;
      }
      
      // Warn user if they're unlocking a bike that's not in use
      if (!newLockState && !bike.isInUse) {
        if (!window.confirm("Unlocking this bike will mark it as UNAVAILABLE. Do you want to continue?")) {
          setProcessingBikeAction(null);
          return;
        }
      }
      
      await toggleBikeLock(bike.id, newLockState);
      
      // Immediately refresh the bike data
      const updatedBikes = await getBikes();
      setBikes(updatedBikes);
      
      // Show confirmation
      alert(`Bike has been ${newLockState ? 'locked' : 'unlocked'}.${!newLockState && !bike.isInUse ? ' The bike is now unavailable.' : ''}`);
      
      // Update selected bike if it's the one being toggled
      if (selectedBike && selectedBike.id === bike.id) {
        const updatedBike = updatedBikes.find(b => b.id === bike.id);
        if (updatedBike) {
          setSelectedBike(updatedBike);
        }
      }
    } catch (error) {
      console.error('Error toggling bike lock:', error);
      alert(`Error toggling bike lock: ${error.message}`);
    } finally {
      setProcessingBikeAction(null);
    }
  };

  // Get the user for a bike that's in use
  const getBikeUser = (bike) => {
    if (!bike.currentUserId) return null;
    
    // Find the ride for this bike
    const ride = activeRides.find(r => r.bikeId === bike.id && r.isActive);
    if (!ride) return null;
    
    // Get the user for this ride
    const user = users[ride.userId];
    return user || { name: 'Unknown User' };
  };

  // Add this debug function right after the getBikeUser function
  const checkBikeCoordinates = () => {
    const bikesWithCoords = bikes.filter(bike => 
      typeof bike.latitude === 'number' && 
      typeof bike.longitude === 'number' && 
      !isNaN(bike.latitude) && 
      !isNaN(bike.longitude)
    );
    
    console.log(`DEBUG: Found ${bikesWithCoords.length}/${bikes.length} bikes with valid coordinates`);
    
    if (bikesWithCoords.length === 0 && bikes.length > 0) {
      console.warn("WARNING: No bikes have valid coordinates. Example bike data:", bikes[0]);
      return false;
    }
    
    return bikesWithCoords.length > 0;
  };

  // Update the marker icon logic to reflect current status and make each bike unique
  const getMarkerIcon = (bike) => {
    // Generate a consistent color based on bike ID
    const generateColorFromId = (id) => {
      // Convert the id into a deterministic hue value (0-360)
      const hash = id.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % 360;
      return `hsl(${hash}, 80%, 50%)`;
    };

    // Create a unique fill color for each bike based on its ID
    const fillColor = generateColorFromId(bike.id);
    
    // Default marker with unique color per bike
    let strokeColor;
    let scale = 8;
    let strokeWidth = 2;
    
    // Check if this is the currently selected bike
    const isSelected = selectedBike && selectedBike.id === bike.id;
    
    // Modify appearance based on status
    if (bike.isInUse) {
      // In-use bikes get a blue border
      strokeColor = '#0000FF';
      scale = 9; // Slightly larger
    } else if (bike.isAvailable && bike.isLocked) {
      // Available and locked bikes get a green border
      strokeColor = '#00FF00';
    } else {
      // Other bikes get a yellow border
      strokeColor = '#FFFF00';
    }
    
    // If selected, make the marker stand out more
    if (isSelected) {
      scale += 2; // Increase size for selected bike
      strokeWidth = 3; // Thicker border
      strokeColor = '#FFFFFF'; // White border to make it pop
    }

    // Create a short identifier for the bike (first 4 chars of ID or name)
    const bikeLabel = bike.name && bike.name.trim() ? 
      bike.name.substring(0, 4) : 
      (bike.hardwareId ? bike.hardwareId.substring(5) : 'BIKE');

    // Return enhanced SVG marker definition with bike label
    const svgUrl = `data:image/svg+xml;utf-8,
        <svg xmlns="http://www.w3.org/2000/svg" width="${scale * 2 + 10}" height="${scale * 2 + 10}" viewBox="0 0 ${scale * 2 + 10} ${scale * 2 + 10}">
          <circle cx="${scale + 5}" cy="${scale + 5}" r="${scale}" fill="${fillColor}" stroke="${strokeColor}" stroke-width="${strokeWidth}" />
          <text x="${scale + 5}" y="${scale + 8}" font-family="Arial" font-size="${isSelected ? 9 : 8}" fill="white" text-anchor="middle" font-weight="bold">${bikeLabel}</text>
        </svg>`;
    
    // Check if Google Maps is available
    if (window.google && window.google.maps) {
      return {
        url: svgUrl,
        scaledSize: new window.google.maps.Size(scale * 2 + 10, scale * 2 + 10),
        anchor: new window.google.maps.Point(scale + 5, scale + 5)
      };
    } else {
      // Fallback if Google Maps API isn't loaded yet
      return {
        url: svgUrl
      };
    }
  };
  
  useEffect(() => {
    // Log all bike coordinates for debugging
    console.log("All Bikes Coordinates Debug:", bikes.map(bike => ({
      id: bike.id,
      name: bike.name || 'Unnamed',
      coord: { lat: bike.latitude, lng: bike.longitude },
      valid: Boolean(bike.latitude && bike.longitude && !isNaN(bike.latitude) && !isNaN(bike.longitude))
    })));
  }, [bikes]);

  if (loading && bikes.length === 0) return <LoadingMessage>Loading bikes map...</LoadingMessage>;
  if (error) return <ErrorMessage>{error}</ErrorMessage>;
  
  // Debug coordinate check
  const hasCoordinates = checkBikeCoordinates();
  if (!hasCoordinates) {
    console.warn("No bike coordinates available - map may appear empty");
  }

  // Count statistics
  const totalBikes = bikes.length;
  const availableBikes = bikes.filter(bike => bike.isAvailable).length;
  const inUseBikes = bikes.filter(bike => bike.isInUse).length;
  const lockedBikes = bikes.filter(bike => bike.isLocked).length;

  return (
    <BikesMapContainer>
      <Title>Bikes Map View</Title>
      
      {/* Debug Information */}
      <div style={{ marginBottom: '20px', padding: '10px', backgroundColor: '#f5f5f5', borderRadius: '8px' }}>
        <h3 style={{ marginBottom: '10px' }}>Debug Information</h3>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
          <div>
            <p><strong>Total Bikes:</strong> {bikes.length}</p>
            <p><strong>Map Center:</strong> {mapCenter.lat}, {mapCenter.lng}</p>
            <p><strong>Maps API Loaded:</strong> {mapsApiLoaded ? 'Yes' : 'No'}</p>
            <p><strong>Bikes with Coordinates:</strong> {bikes.filter(b => b.latitude && b.longitude).length}</p>
          </div>
          <div>
            <button 
              style={{ 
                padding: '8px 16px', 
                backgroundColor: '#1D3C34', 
                color: 'white', 
                border: 'none', 
                borderRadius: '4px',
                cursor: 'pointer'
              }}
              onClick={async () => {
                try {
                  const fixedCount = await fixBikeCoordinates();
                  alert(`Fixed coordinates for ${fixedCount} bikes. Please refresh the page.`);
                  // Refresh bikes data
                  const bikeData = await getBikes();
                  setBikes(bikeData);
                  setLastUpdateTime(new Date());
                } catch (error) {
                  console.error('Error fixing coordinates:', error);
                  alert(`Error fixing coordinates: ${error.message}`);
                }
              }}
            >
              Fix Missing Coordinates
            </button>
          </div>
        </div>
        <details>
          <summary style={{ cursor: 'pointer', fontWeight: 'bold', marginBottom: '5px' }}>Bike Coordinates</summary>
          <div style={{ maxHeight: '200px', overflow: 'auto', fontSize: '12px' }}>
            {bikes.map(bike => (
              <div key={bike.id} style={{ marginBottom: '5px', padding: '5px', border: '1px solid #ddd' }}>
                <p><strong>Bike ID:</strong> {bike.id} ({bike.name || 'Unnamed'})</p>
                <p>
                  <strong>Coordinates:</strong> {bike.latitude ? bike.latitude : 'NULL'}, {bike.longitude ? bike.longitude : 'NULL'}<br/>
                  <strong>Type:</strong> Lat: {typeof bike.latitude}, Lng: {typeof bike.longitude}<br/>
                  <strong>Valid Number:</strong> Lat: {!isNaN(Number(bike.latitude)) ? 'Yes' : 'No'}, Lng: {!isNaN(Number(bike.longitude)) ? 'Yes' : 'No'}<br/>
                  <strong>As Number:</strong> Lat: {Number(bike.latitude)}, Lng: {Number(bike.longitude)}
                </p>
                <p><strong>Status:</strong> {bike.isInUse ? 'In Use' : bike.isAvailable ? 'Available' : 'Unavailable'}</p>
              </div>
            ))}
          </div>
        </details>
        
        {/* Add a Test Marker button */}
        <div style={{ marginTop: '10px' }}>
          <button 
            style={{ 
              padding: '8px 16px', 
              backgroundColor: '#3B5998', 
              color: 'white', 
              border: 'none', 
              borderRadius: '4px',
              cursor: 'pointer',
              marginRight: '10px'
            }}
            onClick={() => {
              if (window.gmap && window.google && window.google.maps) {
                try {
                  // Place a test marker at the current map center
                  const testMarker = new window.google.maps.Marker({
                    position: mapCenter,
                    map: window.gmap,
                    title: 'Test Marker',
                    icon: {
                      path: window.google.maps.SymbolPath.STAR,
                      fillColor: '#D50000', // Bright red
                      fillOpacity: 1,
                      strokeColor: '#FFFFFF',
                      strokeWeight: 2,
                      scale: 12 // Larger for better visibility
                    },
                    zIndex: 1000, // Always on top
                    animation: window.google.maps.Animation.BOUNCE // Add animation
                  });
                  
                  console.log('Added test marker at map center:', mapCenter);
                  alert(`Test marker added at ${mapCenter.lat}, ${mapCenter.lng}`);
                } catch (e) {
                  console.error('Error adding test marker:', e);
                  alert(`Error adding test marker: ${e.message}`);
                }
              } else {
                alert('Map not initialized yet. Try again later.');
              }
            }}
          >
            Add Test Marker
          </button>
          
          <button 
            style={{ 
              padding: '8px 16px', 
              backgroundColor: '#4CAF50', 
              color: 'white', 
              border: 'none', 
              borderRadius: '4px',
              cursor: 'pointer' 
            }}
            onClick={() => {
              try {
                const defaultCoords = { lat: 14.554729, lng: 121.0244 }; // Manila coords
                
                // Update map center
                setMapCenter(defaultCoords);
                
                if (window.gmap) {
                  window.gmap.panTo(defaultCoords);
                  window.gmap.setZoom(14);
                  alert(`Map centered to default location: ${defaultCoords.lat}, ${defaultCoords.lng}`);
                } else {
                  alert('Map not initialized yet. Try refreshing the page.');
                }
              } catch (e) {
                console.error('Error resetting map center:', e);
                alert(`Error: ${e.message}`);
              }
            }}
          >
            Reset Map Center
          </button>
        </div>
      </div>
      
      {showUpdateIndicator && (
        <RefreshIndicator>
          <span>🔄</span> Bike data updated
        </RefreshIndicator>
      )}
      
      <LastUpdateTime>
        Last updated: {lastUpdateTime.toLocaleTimeString()}
      </LastUpdateTime>
      
      <StatsContainer>
        <StatCard>
          <StatValue>{totalBikes}</StatValue>
          <StatLabel>Total Bikes</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color={colors.success}>{availableBikes}</StatValue>
          <StatLabel>Available Bikes</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color={colors.accent}>{inUseBikes}</StatValue>
          <StatLabel>In Use</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color={colors.warning}>{lockedBikes}</StatValue>
          <StatLabel>Locked Bikes</StatLabel>
        </StatCard>
      </StatsContainer>
      
      {/* Add a Legend for markers */}
      <div style={{ 
        backgroundColor: 'white', 
        padding: '10px', 
        borderRadius: '4px',
        boxShadow: '0 2px 5px rgba(0,0,0,0.15)', 
        marginBottom: '15px',
        display: 'flex',
        flexWrap: 'wrap',
        gap: '15px',
        alignItems: 'center'
      }}>
        <div style={{ fontWeight: 'bold' }}>Map Legend:</div>
        
        {/* Available Bikes */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
          <div style={{ 
            width: '20px', 
            height: '20px', 
            borderRadius: '50%', 
            backgroundColor: '#00C853',
            border: '2px solid white',
            boxShadow: '0 1px 3px rgba(0,0,0,0.3)'
          }}></div>
          <span>Available Bikes</span>
        </div>
        
        {/* In-Use Bikes */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
          <div style={{ 
            width: '0', 
            height: '0', 
            borderLeft: '10px solid transparent',
            borderRight: '10px solid transparent',
            borderTop: '20px solid #FF3D00',
            filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.3))'
          }}></div>
          <span>In-Use Bikes</span>
        </div>
        
        {/* Unavailable Bikes */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
          <div style={{ 
            width: '18px', 
            height: '18px', 
            backgroundColor: '#FFAB00',
            border: '2px solid white',
            boxShadow: '0 1px 3px rgba(0,0,0,0.3)'
          }}></div>
          <span>Unavailable Bikes</span>
        </div>
        
        {/* Selected Bike */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
          <div style={{ 
            width: '24px', 
            height: '24px', 
            borderRadius: '50%', 
            backgroundColor: '#4CAF50',
            border: '3px solid white',
            boxShadow: '0 1px 5px rgba(0,0,0,0.5)'
          }}></div>
          <span>Selected Bike (Larger)</span>
        </div>
      </div>
      
      <MapWrapper>
        {error ? (
          <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: '#f5f5f5' }}>
            <div style={{ padding: '20px', backgroundColor: 'white', borderRadius: '8px', maxWidth: '80%', textAlign: 'center' }}>
              <h3 style={{ color: 'red', marginBottom: '10px' }}>Error Loading Maps</h3>
              <p>{error}</p>
              <p>Please check browser console for more details.</p>
            </div>
          </div>
        ) : (
          <MapContainer
            center={mapCenter}
            zoom={14}
            onLoad={map => {
              console.log("Map loaded successfully", map);
              console.log("Map center:", mapCenter);
              console.log("Current bikes data:", bikes.length);
              window.gmap = map; // Store for debugging
              setMapsApiLoaded(true);
              
              // Ensure map is draggable
              if (map) {
                map.setOptions({
                  draggable: true,
                  zoomControl: true,
                  scrollwheel: true,
                  disableDoubleClickZoom: false
                });
              }
              
              // Set custom loader styles
              const loader = document.createElement('div');
              loader.style.position = 'absolute';
              loader.style.top = '50%';
              loader.style.left = '50%';
              loader.style.transform = 'translate(-50%, -50%)';
              loader.style.background = 'rgba(255, 255, 255, 0.8)';
              loader.style.padding = '10px';
              loader.style.borderRadius = '4px';
              loader.textContent = 'Loading bike markers...';
              map.getDiv().appendChild(loader);
              
              // Manually add markers if React ones don't appear - OPTIMIZE THIS
              // Reduced the loading timing to allow dragging sooner
              setTimeout(() => {
                try {
                  // Check if we have bikes with coordinates
                  const bikesWithCoords = bikes.filter(bike => 
                    bike.latitude && bike.longitude && 
                    !isNaN(parseFloat(bike.latitude)) && 
                    !isNaN(parseFloat(bike.longitude))
                  );
                  
                  console.log(`Manually adding ${bikesWithCoords.length} markers as fallback`);
                  
                  // Use a more efficient method for bulk marker creation
                  if (map && window.google && window.google.maps && bikesWithCoords.length > 0) {
                    // Create markers in batches to improve performance
                    const addMarkersBatch = (startIdx, count) => {
                      const endIdx = Math.min(startIdx + count, bikesWithCoords.length);
                      
                      for (let i = startIdx; i < endIdx; i++) {
                        const bike = bikesWithCoords[i];
                        try {
                          // Create a marker manually using the native Google Maps API
                          const position = { 
                            lat: parseFloat(bike.latitude), 
                            lng: parseFloat(bike.longitude) 
                          };
                          
                          // Skip invalid positions
                          if (isNaN(position.lat) || isNaN(position.lng)) continue;
                          
                          // Determine icon based on status
                          let markerIcon;
                          
                          if (bike.isInUse) {
                            markerIcon = {
                              path: window.google.maps.SymbolPath.FORWARD_CLOSED_ARROW,
                              fillColor: '#FF3D00', // Bright orange-red
                              fillOpacity: 0.9,
                              strokeColor: '#FFFFFF',
                              strokeWeight: 3,
                              scale: 9,
                              rotation: 180 // Point down
                            };
                          } else if (bike.isAvailable) {
                            markerIcon = {
                              path: window.google.maps.SymbolPath.CIRCLE,
                              fillColor: '#00C853', // Bright green
                              fillOpacity: 0.9,
                              strokeColor: '#FFFFFF',
                              strokeWeight: 3,
                              scale: 11
                            };
                          } else {
                            markerIcon = {
                              path: 'M -10,-10 10,-10 10,10 -10,10 z', // Square path
                              fillColor: '#FFAB00', // Bright amber
                              fillOpacity: 0.9,
                              strokeColor: '#FFFFFF',
                              strokeWeight: 3,
                              scale: 1.1
                            };
                          }
                          
                          // Create a custom marker
                          const marker = new window.google.maps.Marker({
                            position: position,
                            map: map,
                            title: `Bike ${bike.hardwareId || bike.id} (${bike.name || 'Unnamed'})`,
                            icon: markerIcon,
                            zIndex: bike.isInUse ? 900 : bike.isAvailable ? 800 : 700,
                            optimized: true // Set to true for better performance
                          });
                          
                          // Add click listener
                          marker.addListener('click', () => {
                            setSelectedBike(bike);
                          });
                        } catch (e) {
                          console.error(`Error adding manual marker for bike ${bike.id}:`, e);
                        }
                      }
                      
                      // Process next batch if needed
                      if (endIdx < bikesWithCoords.length) {
                        setTimeout(() => {
                          addMarkersBatch(endIdx, count);
                        }, 0);
                      }
                    };
                    
                    // Start adding markers in batches of 10
                    addMarkersBatch(0, 10);
                  }
                  
                  // Remove the loader
                  if (loader.parentNode) {
                    loader.parentNode.removeChild(loader);
                  }
                } catch (e) {
                  console.warn('Error in manual marker addition:', e);
                  // Remove loader even if there's an error
                  if (loader.parentNode) {
                    loader.parentNode.removeChild(loader);
                  }
                }
              }, 500); // Reduced from 1000ms to 500ms
            }}
          >
            {mapsApiLoaded && bikes.map((bike, index) => {
              console.log(`Mapping bike ${index+1}/${bikes.length}: ${bike.id} (${bike.name || 'Unnamed'})`);
              console.log(`  Coordinates: ${bike.latitude}, ${bike.longitude} - Valid: ${Boolean(bike.latitude && bike.longitude)}`);
              
              // Skip rendering if coordinates are invalid
              if (!bike.latitude || !bike.longitude || 
                  isNaN(bike.latitude) || isNaN(bike.longitude)) {
                console.log(`  Skipping bike ${bike.id} - Invalid coordinates`);
                return null;
              }
              
              // Create position object for the marker
              const position = {
                lat: Number(bike.latitude),
                lng: Number(bike.longitude)
              };
              
              // Skip if position values are not valid numbers
              if (isNaN(position.lat) || isNaN(position.lng)) {
                console.log(`  Skipping bike ${bike.id} - Position values are not valid numbers`);
                return null;
              }
              
              console.log(`  Creating marker for bike ${bike.id} at position:`, position);
              
              // Use different shapes based on bike status
              const getMarkerShape = () => {
                // Different marker shapes based on bike status
                if (bike.isInUse) {
                  // Diamond shape for in-use bikes
                  return {
                    path: window.google?.maps?.SymbolPath?.FORWARD_CLOSED_ARROW || 0,
                    fillColor: '#FF3D00', // Bright orange-red for in-use
                    fillOpacity: 0.9,
                    strokeColor: '#FFFFFF',
                    strokeWeight: 3,
                    scale: bike.id === selectedBike?.id ? 12 : 9,
                    rotation: 180 // Point down
                  };
                } else if (bike.isAvailable) {
                  // Circle shape for available bikes
                  return {
                    path: window.google?.maps?.SymbolPath?.CIRCLE || 0,
                    fillColor: '#00C853', // Bright green for available
                    fillOpacity: 0.9,
                    strokeColor: '#FFFFFF',
                    strokeWeight: 3,
                    scale: bike.id === selectedBike?.id ? 14 : 11
                  };
                } else {
                  // Square shape for other bikes
                  return {
                    path: 'M -10,-10 10,-10 10,10 -10,10 z', // Square path
                    fillColor: '#FFAB00', // Bright amber for others
                    fillOpacity: 0.9,
                    strokeColor: '#FFFFFF',
                    strokeWeight: 3,
                    scale: bike.id === selectedBike?.id ? 1.4 : 1.1
                  };
                }
              };
              
              // Render marker with valid position
              return (
                <Marker
                  key={bike.id}
                  position={position}
                  onClick={() => setSelectedBike(bike)}
                  options={{
                    optimized: true,
                    visible: true,
                    zIndex: bike.id === selectedBike?.id ? 1000 : 
                            bike.isInUse ? 900 : 
                            bike.isAvailable ? 800 : 700 // Higher z-index for important markers
                  }}
                  icon={window.google?.maps ? getMarkerShape() : null}
                  label={bike.id === selectedBike?.id ? {
                    text: bike.name?.substring(0, 1) || (bike.hardwareId?.substring(0, 1) || "B"),
                    color: '#FFFFFF',
                    fontWeight: 'bold',
                    fontSize: '14px'
                  } : null}
                  title={`Bike ${bike.hardwareId || bike.id} (${bike.name || 'Unnamed'})
Status: ${bike.isInUse ? 'In Use' : bike.isAvailable ? 'Available' : 'Unavailable'}`}
                />
              );
            })}
            
            {selectedBike && mapsApiLoaded && (
              <InfoWindow
                position={{ lat: selectedBike.latitude, lng: selectedBike.longitude }}
                onCloseClick={() => setSelectedBike(null)}
              >
                <InfoWindowContent>
                  <BikeTitle>{selectedBike.name}</BikeTitle>
                  <BikeDetail><strong>ID:</strong> {selectedBike.hardwareId || selectedBike.id}</BikeDetail>
                  <BikeDetail><strong>Type:</strong> {selectedBike.type}</BikeDetail>
                  <BikeDetail><strong>Price:</strong> ${parseFloat(selectedBike.priceValue || 0).toFixed(2)}/hr</BikeDetail>
                  <BikeDetail><strong>Location:</strong> {selectedBike.latitude.toFixed(6)}, {selectedBike.longitude.toFixed(6)}</BikeDetail>
                  <BikeDetail><strong>Last Updated:</strong> {selectedBike.lastLocationUpdate ? 
                    (typeof selectedBike.lastLocationUpdate.toDate === 'function' ? 
                      selectedBike.lastLocationUpdate.toDate().toLocaleString() : 
                      (selectedBike.lastLocationUpdate instanceof Date ? 
                        selectedBike.lastLocationUpdate.toLocaleString() : 'Unknown')) : 'Unknown'}</BikeDetail>
                  
                  <StatusBadge 
                    isInUse={selectedBike.isInUse}
                    isAvailable={selectedBike.isAvailable} 
                    isLocked={selectedBike.isLocked}
                  >
                    {selectedBike.isInUse 
                      ? 'In Use' 
                      : selectedBike.isAvailable
                        ? 'Available' 
                        : 'Unavailable'}
                    {selectedBike.isLocked ? ' (Locked)' : ' (Unlocked)'}
                  </StatusBadge>
                  
                  {selectedBike.isInUse && (
                    <UserBadge>
                      <UserIcon>
                        {getBikeUser(selectedBike)?.name?.charAt(0) || 'U'}
                      </UserIcon>
                      <UserInfo>
                        In use by {getBikeUser(selectedBike)?.name || 'Unknown'}
                      </UserInfo>
                    </UserBadge>
                  )}
                  
                  {!selectedBike.isInUse ? (
                    <ActionButton
                      locked={selectedBike.isLocked}
                      onClick={() => handleToggleLock(selectedBike)}
                      disabled={processingBikeAction === selectedBike.id}
                    >
                      {processingBikeAction === selectedBike.id
                        ? 'Processing...'
                        : selectedBike.isLocked
                          ? 'Unlock Bike'
                          : 'Lock Bike'}
                    </ActionButton>
                  ) : (
                    <div style={{ fontSize: '12px', color: '#666', marginTop: '10px', textAlign: 'center' }}>
                      Bike is currently in use and unlocked
                    </div>
                  )}
                  
                  <BikeQRCode bike={selectedBike} />
                </InfoWindowContent>
              </InfoWindow>
            )}
          </MapContainer>
        )}
      </MapWrapper>
    </BikesMapContainer>
  );
};

export default BikesMap; 