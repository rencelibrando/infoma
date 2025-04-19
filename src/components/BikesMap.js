import React, { useState, useEffect, useCallback } from 'react';
import { getBikes, toggleBikeLock, subscribeToBikes, fixBikeCoordinates } from '../services/bikeService';
import { getActiveRides } from '../services/bikeLocationService';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '../firebase';
import styled from 'styled-components';
import { Marker, InfoWindow } from '@react-google-maps/api';
import BikeQRCode from './BikeQRCode';
import MapContainer from './MapContainer';
import { useDataContext } from '../context/DataContext';

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
  // Get data from context
  const { 
    bikes, 
    rides: allRides, 
    loading: contextLoading, 
    lastUpdateTime,
    showUpdateIndicator 
  } = useDataContext();
  
  // Local component state
  const [map, setMap] = useState(null);
  const [activeRides, setActiveRides] = useState([]);
  const [users, setUsers] = useState({});
  const [selectedBike, setSelectedBike] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [mapCenter, setMapCenter] = useState({ lat: 14.5995, lng: 120.9842 }); // Default to Manila
  const [showRefresh, setShowRefresh] = useState(false);
  const [processingBikeId, setProcessingBikeId] = useState(null);
  
  // Filter out just the active rides from context data
  useEffect(() => {
    if (allRides) {
      const active = allRides.filter(ride => ride.isActive);
      setActiveRides(active);
      
      // Extract unique user IDs
      const userIds = [...new Set(active.map(ride => ride.userId))];
      
      // If we already have data for these users, don't refetch
      if (userIds.every(id => users[id])) {
        return;
      }
      
      // Otherwise fetch missing user data
      if (userIds.length > 0) {
        setLoading(true);
        const fetchMissingUsers = async () => {
          try {
            // Create a map of user IDs to promises
            const userPromises = userIds
              .filter(id => !users[id]) // Only fetch users we don't have
              .map(async (userId) => {
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
            
            if (userPromises.length === 0) {
              setLoading(false);
              return;
            }
            
            const newUsers = await Promise.all(userPromises);
            
            // Create a map of user IDs to user data
            const usersMap = {...users};
            newUsers.forEach(user => {
              usersMap[user.id] = user;
            });
            
            setUsers(usersMap);
          } catch (error) {
            console.error('Error fetching users:', error);
          } finally {
            setLoading(false);
          }
        };
        
        fetchMissingUsers();
      }
    }
  }, [allRides, users]);
  
  // Set map center based on first bike with valid coordinates
  useEffect(() => {
    if (bikes && bikes.length > 0) {
      // Find the first bike with valid coordinates
      const validBike = bikes.find(bike => 
        bike.latitude && bike.longitude && 
        !isNaN(parseFloat(bike.latitude)) && 
        !isNaN(parseFloat(bike.longitude))
      );
      
      if (validBike) {
        const newCenter = {
          lat: parseFloat(validBike.latitude),
          lng: parseFloat(validBike.longitude)
        };
        setMapCenter(newCenter);
      }
    }
  }, [bikes]);
  
  // Show refresh indicator when update happens
  useEffect(() => {
    if (showUpdateIndicator) {
      setShowRefresh(true);
      setTimeout(() => {
        setShowRefresh(false);
      }, 3000);
    }
  }, [showUpdateIndicator]);

  // Function to handle map load
  const handleMapLoad = useCallback((mapInstance) => {
    console.log("Map loaded successfully", mapInstance);
    setMap(mapInstance);
  }, []);
  
  // Get user for a bike that's in use
  const getBikeUser = (bike) => {
    if (!bike.isInUse) return null;
    
    // Find the ride for this bike
    const ride = activeRides.find(r => r.bikeId === bike.id && r.isActive);
    if (!ride) return null;
    
    // Get the user for this ride
    return users[ride.userId] || { name: 'Unknown User' };
  };
  
  // Handle bike lock toggle
  const handleToggleLock = async (bike) => {
    try {
      const newLockState = !bike.isLocked;
      
      // Prevent locking a bike that's in use
      if (newLockState && bike.isInUse) {
        alert("Cannot lock a bike that is currently in use.");
        return;
      }
      
      setProcessingBikeId(bike.id);
      await toggleBikeLock(bike.id, newLockState);
      alert(`Bike has been ${newLockState ? 'locked' : 'unlocked'}.`);
      
      // The data context will update automatically
      setSelectedBike(null); // Close the info window
    } catch (error) {
      console.error('Error toggling bike lock:', error);
      alert(`Error toggling bike lock: ${error.message}`);
    } finally {
      setProcessingBikeId(null);
    }
  };
  
  // Show loading state while context data is loading
  if (contextLoading && !bikes) {
    return <LoadingMessage>Loading bike map data...</LoadingMessage>;
  }

  return (
    <BikesMapContainer>
      <Title>Bikes Map</Title>
      
      <StatsContainer>
        <StatCard>
          <StatValue color={colors.pineGreen}>{bikes.length}</StatValue>
          <StatLabel>Total Bikes</StatLabel>
        </StatCard>
        
        <StatCard>
          <StatValue color={colors.success}>
            {bikes.filter(bike => bike.isAvailable && !bike.isInUse).length}
          </StatValue>
          <StatLabel>Available</StatLabel>
        </StatCard>
        
        <StatCard>
          <StatValue color={colors.warning}>
            {bikes.filter(bike => bike.isInUse).length}
          </StatValue>
          <StatLabel>In Use</StatLabel>
        </StatCard>
        
        <StatCard>
          <StatValue color={bikes.length > 0 ? colors.pineGreen : colors.mediumGray}>
            {activeRides.length}
          </StatValue>
          <StatLabel>Active Rides</StatLabel>
        </StatCard>
      </StatsContainer>
      
      <LastUpdateTime>
        Last updated: {new Date(lastUpdateTime).toLocaleTimeString()}
      </LastUpdateTime>
      
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
            onLoad={handleMapLoad}
            style={{ height: '100%', width: '100%' }}
          >
            {map && bikes && bikes.length > 0 && (
              <>
                {bikes.map(bike => (
                  <Marker
                    key={bike.id}
                    position={{ lat: bike.latitude, lng: bike.longitude }}
                    onClick={() => setSelectedBike(bike)}
                    icon={{
                      url: bike.isInUse 
                        ? '/images/bike-marker-in-use.png' 
                        : bike.isAvailable 
                          ? '/images/bike-marker-available.png'
                          : '/images/bike-marker-unavailable.png',
                      scaledSize: new window.google.maps.Size(30, 30)
                    }}
                  />
                ))}
                
                {selectedBike && (
                  <InfoWindow
                    position={{ lat: selectedBike.latitude, lng: selectedBike.longitude }}
                    onCloseClick={() => setSelectedBike(null)}
                  >
                    <InfoWindowContent>
                      <BikeTitle>{selectedBike.name}</BikeTitle>
                      <BikeDetail>Type: {selectedBike.type}</BikeDetail>
                      <BikeDetail>Price: {selectedBike.price}</BikeDetail>
                      <StatusBadge 
                        isInUse={selectedBike.isInUse} 
                        isAvailable={selectedBike.isAvailable && !selectedBike.isInUse}
                      >
                        {selectedBike.isInUse ? "In Use" : 
                         selectedBike.isAvailable ? "Available" : "Unavailable"}
                      </StatusBadge>
                      
                      {selectedBike.isInUse && selectedBike.currentRider && getBikeUser(selectedBike) && (
                        <UserBadge>
                          <UserIcon>
                            {getBikeUser(selectedBike).name?.charAt(0) || 'U'}
                          </UserIcon>
                          <UserInfo>
                            {getBikeUser(selectedBike).name || 'Unknown User'}
                          </UserInfo>
                        </UserBadge>
                      )}
                      
                      <ActionButton
                        locked={!selectedBike.isAvailable}
                        onClick={() => handleToggleLock(selectedBike)}
                        disabled={loading || processingBikeId === selectedBike.id}
                      >
                        {processingBikeId === selectedBike.id ? "Processing..." : 
                         selectedBike.isAvailable ? "Lock Bike" : "Unlock Bike"}
                      </ActionButton>
                    </InfoWindowContent>
                  </InfoWindow>
                )}
              </>
            )}
          </MapContainer>
        )}
      </MapWrapper>
      
      {showUpdateIndicator && (
        <RefreshIndicator>
          <span role="img" aria-label="refresh">🔄</span> Data refreshed
        </RefreshIndicator>
      )}
    </BikesMapContainer>
  );
};

export default BikesMap; 