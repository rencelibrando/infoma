import React, { useState, useEffect } from 'react';
import { getBikes, toggleBikeLock, subscribeToBikes } from '../services/bikeService';
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
        setBikes(bikeData);
        setLastUpdateTime(new Date());
        
        // Update map center if needed
        if (bikeData.length > 0) {
          const validBike = bikeData.find(bike => bike.latitude && bike.longitude);
          if (validBike) {
            setMapCenter({ lat: validBike.latitude, lng: validBike.longitude });
          }
        }
      } catch (err) {
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
      
      // Update selected bike if it exists
      if (selectedBike) {
        const updatedSelectedBike = updatedBikes.find(b => b.id === selectedBike.id);
        if (updatedSelectedBike) {
          setSelectedBike(updatedSelectedBike);
        }
      }
      
      // Hide update indicator after 3 seconds
      setTimeout(() => {
        setShowUpdateIndicator(false);
      }, 3000);
    });
    
    // Set up a timer to refresh active rides data every 30 seconds
    const refreshInterval = setInterval(() => {
      fetchActiveRides();
    }, 30000);
    
    // Cleanup listeners on component unmount
    return () => {
      unsubscribe();
      clearInterval(refreshInterval);
    };
  }, [selectedBike]);
  
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

  // Update the marker icon logic to reflect current status
  const getMarkerIcon = (bike) => {
    if (bike.isInUse) {
      return 'https://maps.google.com/mapfiles/ms/icons/blue-dot.png';
    } else if (bike.isAvailable && bike.isLocked) {
      return 'https://maps.google.com/mapfiles/ms/icons/green-dot.png';
    } else {
      return 'https://maps.google.com/mapfiles/ms/icons/yellow-dot.png';
    }
  };
  
  if (loading && bikes.length === 0) return <LoadingMessage>Loading bikes map...</LoadingMessage>;
  if (error) return <ErrorMessage>{error}</ErrorMessage>;

  // Count statistics
  const totalBikes = bikes.length;
  const availableBikes = bikes.filter(bike => bike.isAvailable).length;
  const inUseBikes = bikes.filter(bike => bike.isInUse).length;
  const lockedBikes = bikes.filter(bike => bike.isLocked).length;

  return (
    <BikesMapContainer>
      <Title>Bikes Map View</Title>
      
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
      
      <MapWrapper>
        <MapContainer
          center={mapCenter}
          zoom={14}
          onLoad={map => {
            console.log("Map loaded successfully");
          }}
        >
          {bikes.map(bike => (
            bike.latitude && bike.longitude ? (
              <Marker
                key={bike.id}
                position={{ lat: bike.latitude, lng: bike.longitude }}
                onClick={() => setSelectedBike(bike)}
                icon={{
                  url: getMarkerIcon(bike)
                }}
              />
            ) : null
          ))}
          
          {selectedBike && (
            <InfoWindow
              position={{ lat: selectedBike.latitude, lng: selectedBike.longitude }}
              onCloseClick={() => setSelectedBike(null)}
            >
              <InfoWindowContent>
                <BikeTitle>{selectedBike.name}</BikeTitle>
                <BikeDetail><strong>ID:</strong> {selectedBike.hardwareId || 'N/A'}</BikeDetail>
                <BikeDetail><strong>Type:</strong> {selectedBike.type}</BikeDetail>
                <BikeDetail><strong>Price:</strong> ${parseFloat(selectedBike.priceValue || 0).toFixed(2)}</BikeDetail>
                <BikeDetail><strong>Last Updated:</strong> {selectedBike.lastLocationUpdate ? new Date(selectedBike.lastLocationUpdate.toDate()).toLocaleTimeString() : 'N/A'}</BikeDetail>
                
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
      </MapWrapper>
    </BikesMapContainer>
  );
};

export default BikesMap; 