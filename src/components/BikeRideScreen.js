import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getBikeById } from '../services/bikeService';
import { startBikeRide, endBikeRide, updateBikeLocation } from '../services/bikeLocationService';
import { auth } from '../firebase';
import styled from 'styled-components';
import { Marker } from '@react-google-maps/api';
import MapContainer from './MapContainer';
import BikeActivityOverview from './BikeActivityOverview';

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

const RideContainer = styled.div`
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
`;

const Title = styled.h2`
  margin-bottom: 20px;
  color: ${colors.darkGray};
`;

const BikeInfoCard = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 16px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
`;

const BikeImage = styled.img`
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  margin-right: 16px;
`;

const BikeDetails = styled.div`
  flex: 1;
`;

const BikeName = styled.h3`
  font-size: 18px;
  margin-bottom: 4px;
  color: ${colors.darkGray};
`;

const BikeType = styled.p`
  font-size: 14px;
  color: ${colors.mediumGray};
  margin-bottom: 4px;
`;

const BikePrice = styled.p`
  font-size: 16px;
  font-weight: bold;
  color: ${colors.pineGreen};
`;

const MapWrapper = styled.div`
  height: 300px;
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
`;

const RideStats = styled.div`
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
`;

const StatCard = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
  padding: 12px;
  flex: 1;
  text-align: center;
`;

const StatValue = styled.div`
  font-size: 18px;
  font-weight: bold;
  color: ${colors.pineGreen};
`;

const StatLabel = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  margin-top: 4px;
`;

const ActionButton = styled.button`
  background-color: ${props => props.primary ? colors.danger : colors.pineGreen};
  color: ${colors.white};
  border: none;
  border-radius: 8px;
  padding: 12px;
  font-size: 16px;
  font-weight: bold;
  width: 100%;
  cursor: pointer;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  
  &:hover {
    opacity: 0.9;
  }
  
  &:disabled {
    background-color: ${colors.mediumGray};
    cursor: not-allowed;
  }
`;

const ErrorMessage = styled.div`
  color: ${colors.danger};
  background-color: #ffebee;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
`;

const LoadingMessage = styled.div`
  text-align: center;
  padding: 40px;
  color: ${colors.mediumGray};
`;

const BikeRideScreen = () => {
  const { bikeId } = useParams();
  const navigate = useNavigate();
  const [bike, setBike] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [rideActive, setRideActive] = useState(false);
  const [rideId, setRideId] = useState(null);
  const [currentLocation, setCurrentLocation] = useState(null);
  const [locationWatchId, setLocationWatchId] = useState(null);
  const [duration, setDuration] = useState(0);
  const [processing, setProcessing] = useState(false);
  const [mapKey, setMapKey] = useState('ride-map-' + Date.now());

  // Fetch bike data
  useEffect(() => {
    const fetchBike = async () => {
      try {
        setLoading(true);
        const bikeData = await getBikeById(bikeId);
        if (!bikeData) {
          setError('Bike not found');
          return;
        }
        setBike(bikeData);
        
        // Set initial location from bike's location if available
        if (bikeData.latitude && bikeData.longitude) {
          setCurrentLocation({
            lat: bikeData.latitude,
            lng: bikeData.longitude
          });
        }
      } catch (err) {
        setError('Failed to fetch bike: ' + err.message);
      } finally {
        setLoading(false);
      }
    };
    
    fetchBike();
  }, [bikeId]);
  
  // Handle watch position and update bike location
  const startLocationTracking = useCallback(() => {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by your browser');
      return;
    }
    
    // Watch position and update when it changes
    const watchId = navigator.geolocation.watchPosition(
      async (position) => {
        const newLocation = {
          lat: position.coords.latitude,
          lng: position.coords.longitude
        };
        
        setCurrentLocation(newLocation);
        
        // Update bike location in Firestore
        try {
          await updateBikeLocation(bikeId, newLocation.lat, newLocation.lng);
        } catch (error) {
          console.error('Error updating bike location:', error);
        }
      },
      (error) => {
        console.error('Error getting location:', error);
        setError(`Location error: ${error.message}`);
      },
      { 
        enableHighAccuracy: true, 
        maximumAge: 10000, 
        timeout: 5000 
      }
    );
    
    setLocationWatchId(watchId);
  }, [bikeId]);
  
  // Stop watching position
  const stopLocationTracking = useCallback(() => {
    if (locationWatchId !== null && navigator.geolocation) {
      navigator.geolocation.clearWatch(locationWatchId);
      setLocationWatchId(null);
    }
  }, [locationWatchId]);
  
  // Clean up location tracking on unmount
  useEffect(() => {
    return () => {
      stopLocationTracking();
    };
  }, [stopLocationTracking]);
  
  // Start the ride
  const initializeRide = async () => {
    try {
      setLoading(true);
      
      // Get the current user
      const currentUser = auth.currentUser;
      if (!currentUser) {
        setError("You must be logged in to start a ride");
        return;
      }
      
      // Start the ride
      const rideData = await startBikeRide(bikeId, currentUser.uid);
      setRideId(rideData.rideId);
      setRideActive(true);
      
      // Get current position
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (position) => {
            const { latitude, longitude } = position.coords;
            setCurrentLocation({ lat: latitude, lng: longitude });
          },
          (error) => {
            console.error("Error getting location:", error);
          }
        );
      }
      
    } catch (error) {
      setError("Failed to start ride: " + error.message);
    } finally {
      setLoading(false);
    }
  };
  
  // End the ride
  const handleEndRide = async () => {
    try {
      setProcessing(true);
      
      if (!currentLocation) {
        // If no current location, try to get one last position
        navigator.geolocation.getCurrentPosition(
          async (position) => {
            const finalLocation = {
              lat: position.coords.latitude,
              lng: position.coords.longitude
            };
            
            await endBikeRide(rideId, finalLocation.lat, finalLocation.lng);
            
            stopLocationTracking();
            setRideActive(false);
            setRideId(null);
            
            // Refresh bike data
            const updatedBike = await getBikeById(bikeId);
            setBike(updatedBike);
            
            // Navigate to ride summary
            navigate('/ride-summary', { 
              state: { 
                bikeId: bikeId,
                rideDuration: duration,
                bikeName: bike.name,
                bikePrice: bike.priceValue
              } 
            });
          },
          async (error) => {
            // Even if we can't get the location, end the ride with last known location
            if (currentLocation) {
              await endBikeRide(rideId, currentLocation.lat, currentLocation.lng);
            } else {
              await endBikeRide(rideId, 0, 0);
            }
            
            stopLocationTracking();
            setRideActive(false);
            setRideId(null);
            
            // Navigate to ride summary
            navigate('/ride-summary', { 
              state: { 
                bikeId: bikeId,
                rideDuration: duration,
                bikeName: bike.name,
                bikePrice: bike.priceValue
              } 
            });
          }
        );
      } else {
        // End the ride with current location
        await endBikeRide(rideId, currentLocation.lat, currentLocation.lng);
        
        stopLocationTracking();
        setRideActive(false);
        setRideId(null);
        
        // Navigate to ride summary
        navigate('/ride-summary', { 
          state: { 
            bikeId: bikeId,
            rideDuration: duration,
            bikeName: bike.name,
            bikePrice: bike.priceValue
          } 
        });
      }
    } catch (error) {
      setError(`Failed to end ride: ${error.message}`);
      setProcessing(false);
    }
  };
  
  // Format duration as MM:SS
  const formatDuration = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };
  
  // Add visibility change listener
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        console.log('Tab is visible again - refreshing ride map');
        setMapKey('ride-map-' + Date.now());
        
        // If ride is active, make sure to update location
        if (rideActive && currentLocation) {
          try {
            updateBikeLocation(bikeId, currentLocation.lat, currentLocation.lng);
          } catch (error) {
            console.error('Error updating bike location on visibility change:', error);
          }
        }
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [bikeId, rideActive, currentLocation]);
  
  if (loading) return <LoadingMessage>Loading bike details...</LoadingMessage>;
  if (error) return <ErrorMessage>{error}</ErrorMessage>;
  if (!bike) return <ErrorMessage>Bike not found</ErrorMessage>;
  
  return (
    <RideContainer>
      <Title>{rideActive ? 'Active Ride' : 'Start Ride'}</Title>
      
      <BikeInfoCard>
        <BikeImage src={bike.imageUrl} alt={bike.name} />
        <BikeDetails>
          <BikeName>{bike.name}</BikeName>
          <BikeType>{bike.type}</BikeType>
          <BikePrice>{bike.price}</BikePrice>
        </BikeDetails>
      </BikeInfoCard>
      
      {rideActive && (
        <RideStats>
          <StatCard>
            <StatValue>{formatDuration(duration)}</StatValue>
            <StatLabel>Duration</StatLabel>
          </StatCard>
          
          <StatCard>
            <StatValue>
              ${((duration / 3600) * bike.priceValue).toFixed(2)}
            </StatValue>
            <StatLabel>Estimated Cost</StatLabel>
          </StatCard>
        </RideStats>
      )}
      
      <MapWrapper>
        <MapContainer
          center={currentLocation || { lat: bike.latitude, lng: bike.longitude }}
          zoom={15}
          onLoad={map => {
            console.log("Ride map loaded successfully");
          }}
        >
          {currentLocation && (
            <Marker
              position={currentLocation}
              icon={{
                url: 'https://maps.google.com/mapfiles/ms/icons/blue-dot.png',
              }}
            />
          )}
        </MapContainer>
      </MapWrapper>
      
      {rideActive && (
        <BikeActivityOverview />
      )}
      
      {rideActive ? (
        <ActionButton 
          primary
          onClick={handleEndRide}
          disabled={processing}
        >
          {processing ? 'Ending Ride...' : 'End Ride'}
        </ActionButton>
      ) : (
        <ActionButton
          onClick={initializeRide}
          disabled={processing || bike.isInUse || bike.isLocked}
        >
          {processing ? 'Starting Ride...' : 'Start Ride'}
        </ActionButton>
      )}
      
      {!bike.isAvailable && !rideActive && (
        <ErrorMessage>
          This bike is currently {bike.isLocked ? 'locked' : 'in use by another user'} and is not available.
        </ErrorMessage>
      )}
    </RideContainer>
  );
};

export default BikeRideScreen; 