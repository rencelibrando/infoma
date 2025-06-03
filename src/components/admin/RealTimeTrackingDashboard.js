/* global google */
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { collection, query, where, onSnapshot, orderBy, limit, getDoc, doc, updateDoc } from 'firebase/firestore';
import { ref, onValue, off, child, get } from 'firebase/database';
import { db, realtimeDb } from '../../firebase';
import styled from 'styled-components';
import { Marker, InfoWindow, Polyline, Circle, HeatmapLayer } from '@react-google-maps/api';
import MapContainer from '../MapContainer';
import { useDataContext } from '../../context/DataContext';

// Enhanced color theme
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
  warning: '#FFC107',
  info: '#2196F3',
  purple: '#9C27B0',
  teal: '#009688',
  indigo: '#3F51B5'
};

const DashboardContainer = styled.div`
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: 100vh;
`;

const Title = styled.h2`
  margin-bottom: 20px;
  color: ${colors.darkGray};
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 28px;
  font-weight: 600;
`;

const LiveIndicator = styled.div`
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: ${colors.danger};
  animation: pulse 2s infinite;
  
  @keyframes pulse {
    0% {
      box-shadow: 0 0 0 0 rgba(211, 47, 47, 0.7);
    }
    70% {
      box-shadow: 0 0 0 10px rgba(211, 47, 47, 0);
    }
    100% {
      box-shadow: 0 0 0 0 rgba(211, 47, 47, 0);
    }
  }
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
`;

const StatCard = styled.div`
  background: linear-gradient(135deg, ${colors.white} 0%, #f8f9fa 100%);
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
  }
`;

const StatValue = styled.div`
  font-size: 32px;
  font-weight: bold;
  color: ${props => props.color || colors.pineGreen};
  margin-bottom: 5px;
`;

const StatLabel = styled.div`
  font-size: 14px;
  color: ${colors.mediumGray};
  font-weight: 500;
`;

const AlertsContainer = styled.div`
  background: ${colors.white};
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
`;

const AlertItem = styled.div`
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: ${props => {
    switch(props.severity) {
      case 'high': return 'rgba(211, 47, 47, 0.1)';
      case 'medium': return 'rgba(255, 193, 7, 0.1)';
      default: return 'rgba(33, 150, 243, 0.1)';
    }
  }};
  border-left: 4px solid ${props => {
    switch(props.severity) {
      case 'high': return colors.danger;
      case 'medium': return colors.warning;
      default: return colors.info;
    }
  }};
`;

const AlertIcon = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${props => {
    switch(props.severity) {
      case 'high': return colors.danger;
      case 'medium': return colors.warning;
      default: return colors.info;
    }
  }};
  color: white;
  font-size: 18px;
`;

const MapWrapper = styled.div`
  height: 600px;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin-bottom: 20px;
`;

const ControlsContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
  align-items: center;
  background: ${colors.white};
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
`;

const ToggleButton = styled.button`
  background: ${props => props.active ? 
    `linear-gradient(135deg, ${colors.pineGreen} 0%, ${colors.lightPineGreen} 100%)` : 
    colors.lightGray};
  color: ${props => props.active ? colors.white : colors.darkGray};
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
`;

const RidesList = styled.div`
  background: ${colors.white};
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
`;

const RideItem = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: ${colors.lightGray};
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: ${colors.pineGreen};
    color: ${colors.white};
    transform: translateX(5px);
  }
`;

const RideInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

const RideStatus = styled.span`
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  background: ${props => {
    switch(props.status) {
      case 'active': return colors.success;
      case 'paused': return colors.warning;
      case 'emergency': return colors.danger;
      default: return colors.info;
    }
  }};
  color: white;
`;

const InfoWindowContent = styled.div`
  padding: 15px;
  width: 350px;
  max-height: 500px;
  overflow-y: auto;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  
  @keyframes pulse {
    0% {
      box-shadow: 0 0 0 0 rgba(76, 175, 80, 0.7);
    }
    70% {
      box-shadow: 0 0 0 10px rgba(76, 175, 80, 0);
    }
    100% {
      box-shadow: 0 0 0 0 rgba(76, 175, 80, 0);
    }
  }
`;

const RealTimeTrackingDashboard = () => {
  // Get data from context
  const { 
    bikes, 
    rides: allRides, 
    loading: contextLoading, 
    lastUpdateTime 
  } = useDataContext();

  const [activeRides, setActiveRides] = useState([]);
  const [selectedRide, setSelectedRide] = useState(null);
  const [showHeatmap, setShowHeatmap] = useState(false);
  const [showTrails, setShowTrails] = useState(true);
  const [alerts, setAlerts] = useState([]);
  const [stats, setStats] = useState({
    totalActiveRides: 0,
    totalDistance: 0,
    averageSpeed: 0,
    emergencyAlerts: 0
  });
  const [mapCenter, setMapCenter] = useState({ lat: 14.5995, lng: 120.9842 });
  const [mapZoom, setMapZoom] = useState(13);
  const [liveLocations, setLiveLocations] = useState({});
  
  const unsubscribeRefs = useRef([]);
  const realtimeListeners = useRef([]);

  // Setup real-time listeners for active rides using Realtime Database
  useEffect(() => {
    const setupRealTimeListeners = () => {
      console.log('Setting up real-time listeners...');
      
      // Listen for active rides from Realtime Database
      const activeRidesRef = ref(realtimeDb, 'activeRides');
      const activeRidesListener = onValue(activeRidesRef, (snapshot) => {
        const activeRidesData = snapshot.val() || {};
        const rides = Object.entries(activeRidesData).map(([userId, rideData]) => ({
          ...rideData,
          userId: userId,
          id: rideData.rideId || rideData.id
        }));
        
        console.log('Active rides updated:', rides.length);
        setActiveRides(rides);
        calculateStats(rides);
        checkForAlerts(rides);
      });

      // Listen for live locations from Realtime Database
      const liveLocationRef = ref(realtimeDb, 'liveLocation');
      const liveLocationListener = onValue(liveLocationRef, (snapshot) => {
        const liveLocationData = snapshot.val() || {};
        console.log('Live locations updated:', Object.keys(liveLocationData).length);
        setLiveLocations(liveLocationData);
      });

      realtimeListeners.current.push(
        { ref: activeRidesRef, listener: activeRidesListener },
        { ref: liveLocationRef, listener: liveLocationListener }
      );
    };

    setupRealTimeListeners();

    return () => {
      // Clean up Realtime Database listeners
      realtimeListeners.current.forEach(({ ref: dbRef, listener }) => {
        off(dbRef, 'value', listener);
      });
      realtimeListeners.current = [];
      
      // Clean up Firestore listeners
      unsubscribeRefs.current.forEach(unsubscribe => unsubscribe());
      unsubscribeRefs.current = [];
    };
  }, []);

  const calculateStats = (rides) => {
    const totalActiveRides = rides.length;
    let totalDistance = 0;
    let totalSpeed = 0;
    let speedCount = 0;
    let emergencyAlerts = 0;

    rides.forEach(ride => {
      if (ride.currentLocation) {
        totalDistance += ride.totalDistance || 0;
        if (ride.currentSpeed) {
          totalSpeed += ride.currentSpeed;
          speedCount++;
        }
        if (ride.status === 'emergency') {
          emergencyAlerts++;
        }
      }
    });

    setStats({
      totalActiveRides,
      totalDistance: totalDistance / 1000, // Convert to km
      averageSpeed: speedCount > 0 ? totalSpeed / speedCount : 0,
      emergencyAlerts
    });
  };

  const checkForAlerts = (rides) => {
    const newAlerts = [];

    rides.forEach(ride => {
      const liveLocation = liveLocations[ride.userId];
      
      if (liveLocation) {
        const lastUpdate = new Date(liveLocation.timestamp || liveLocation.deviceTimestamp);
        const timeSinceUpdate = Date.now() - lastUpdate.getTime();

        // Check for stale location data
        if (timeSinceUpdate > 5 * 60 * 1000) { // 5 minutes
          newAlerts.push({
            id: `stale-${ride.userId}`,
            severity: 'medium',
            message: `No location update for ${ride.userName} in ${Math.round(timeSinceUpdate / 60000)} minutes`,
            rideId: ride.id,
            timestamp: Date.now()
          });
        }

        // Check for emergency status
        if (ride.status === 'emergency') {
          newAlerts.push({
            id: `emergency-${ride.userId}`,
            severity: 'high',
            message: `Emergency alert from ${ride.userName}`,
            rideId: ride.id,
            timestamp: Date.now()
          });
        }

        // Check for unusual speed
        if (liveLocation.speed > 50) { // 50 km/h
          newAlerts.push({
            id: `speed-${ride.userId}`,
            severity: 'medium',
            message: `${ride.userName} traveling at unusual speed: ${liveLocation.speed.toFixed(1)} km/h`,
            rideId: ride.id,
            timestamp: Date.now()
          });
        }
      }
    });

    setAlerts(newAlerts);
  };

  const handleRideClick = (ride) => {
    setSelectedRide(ride);
    const liveLocation = liveLocations[ride.userId];
    if (liveLocation) {
      setMapCenter({
        lat: liveLocation.latitude,
        lng: liveLocation.longitude
      });
      setMapZoom(16);
    }
  };

  const handleEmergencyResponse = async (rideId) => {
    try {
      await updateDoc(doc(db, 'rides', rideId), {
        emergencyResponded: true,
        emergencyResponseTime: new Date()
      });
      
      // Remove emergency alert
      setAlerts(prev => prev.filter(alert => alert.id !== `emergency-${rideId}`));
    } catch (error) {
      console.error('Error responding to emergency:', error);
    }
  };

  const getMarkerIcon = (ride) => {
    let color = colors.success;
    let scale = 12;
    
    if (ride.status === 'emergency') {
      color = colors.danger;
      scale = 16; // Larger for emergency
    } else if (ride.status === 'paused') {
      color = colors.warning;
      scale = 14;
    }

    // Create a more visible marker with bike icon
    return {
      path: google.maps.SymbolPath.CIRCLE,
      scale: scale,
      fillColor: color,
      fillOpacity: 0.9,
      strokeColor: colors.white,
      strokeWeight: 3,
      strokeOpacity: 1,
      // Add a pulsing effect for active rides
      anchor: new google.maps.Point(0, 0),
      labelOrigin: new google.maps.Point(0, 0)
    };
  };

  const formatDuration = (startTime) => {
    const start = new Date(startTime);
    const duration = Date.now() - start.getTime();
    const hours = Math.floor(duration / (1000 * 60 * 60));
    const minutes = Math.floor((duration % (1000 * 60 * 60)) / (1000 * 60));
    return `${hours}h ${minutes}m`;
  };

  return (
    <DashboardContainer>
      <Title>
        <LiveIndicator />
        Real-Time Tracking Dashboard
      </Title>

      {/* Stats Grid */}
      <StatsGrid>
        <StatCard>
          <StatValue color={colors.pineGreen}>{bikes ? bikes.length : 0}</StatValue>
          <StatLabel>Total Bikes</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color={colors.success}>
            {bikes ? bikes.filter(bike => bike.isAvailable && !bike.isInUse).length : 0}
          </StatValue>
          <StatLabel>Available</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color={colors.warning}>
            {bikes ? bikes.filter(bike => bike.isInUse).length : 0}
          </StatValue>
          <StatLabel>In Use</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color={colors.success}>{stats.totalActiveRides}</StatValue>
          <StatLabel>Active Rides</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color={colors.info}>{stats.totalDistance.toFixed(1)} km</StatValue>
          <StatLabel>Total Distance Today</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color={colors.purple}>{stats.averageSpeed.toFixed(1)} km/h</StatValue>
          <StatLabel>Average Speed</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color={stats.emergencyAlerts > 0 ? colors.danger : colors.success}>
            {stats.emergencyAlerts}
          </StatValue>
          <StatLabel>Emergency Alerts</StatLabel>
        </StatCard>
      </StatsGrid>

      {/* Alerts Section */}
      {alerts.length > 0 && (
        <AlertsContainer>
          <h3 style={{ marginBottom: '15px', color: colors.darkGray }}>Active Alerts</h3>
          {alerts.map(alert => (
            <AlertItem key={alert.id} severity={alert.severity}>
              <AlertIcon severity={alert.severity}>
                {alert.severity === 'high' ? '⚠️' : alert.severity === 'medium' ? '⚡' : 'ℹ️'}
              </AlertIcon>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>
                  {alert.message}
                </div>
                <div style={{ fontSize: '12px', color: colors.mediumGray }}>
                  {new Date(alert.timestamp).toLocaleTimeString()}
                </div>
              </div>
              {alert.severity === 'high' && (
                <button
                  onClick={() => handleEmergencyResponse(alert.rideId)}
                  style={{
                    background: colors.danger,
                    color: 'white',
                    border: 'none',
                    padding: '8px 16px',
                    borderRadius: '4px',
                    cursor: 'pointer'
                  }}
                >
                  Respond
                </button>
              )}
            </AlertItem>
          ))}
        </AlertsContainer>
      )}

      {/* Controls */}
      <ControlsContainer>
        <ToggleButton
          active={showTrails}
          onClick={() => setShowTrails(!showTrails)}
        >
          Show Trails
        </ToggleButton>
        <ToggleButton
          active={showHeatmap}
          onClick={() => setShowHeatmap(!showHeatmap)}
        >
          Heatmap View
        </ToggleButton>
        <div style={{ 
          display: 'flex', 
          alignItems: 'center', 
          gap: '15px',
          marginLeft: '20px',
          padding: '10px',
          backgroundColor: '#f8f9fa',
          borderRadius: '8px',
          fontSize: '12px'
        }}>
          <strong>Map Legend:</strong>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <div style={{
              width: '12px',
              height: '12px',
              borderRadius: '50%',
              backgroundColor: colors.success
            }} />
            <span>Active Ride</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <div style={{
              width: '12px',
              height: '12px',
              borderRadius: '50%',
              backgroundColor: colors.danger
            }} />
            <span>Emergency</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <div style={{
              width: '0',
              height: '0',
              borderLeft: '6px solid transparent',
              borderRight: '6px solid transparent',
              borderBottom: `12px solid ${colors.teal}`
            }} />
            <span>Available Bike</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <div style={{
              width: '0',
              height: '0',
              borderLeft: '6px solid transparent',
              borderRight: '6px solid transparent',
              borderBottom: `12px solid ${colors.mediumGray}`
            }} />
            <span>Unavailable Bike</span>
          </div>
        </div>
      </ControlsContainer>

      {/* Map */}
      <MapWrapper>
        <MapContainer
          center={mapCenter}
          zoom={mapZoom}
          onLoad={() => console.log('Map loaded')}
        >
          {/* Render all bike markers - available bikes */}
          {bikes && bikes.map(bike => {
            // Don't show bikes that are currently in use (they'll be shown as active rides)
            const isInActiveRide = activeRides.some(ride => ride.bikeId === bike.id);
            
            return !isInActiveRide && (
              <Marker
                key={`bike-${bike.id}`}
                position={{
                  lat: bike.latitude,
                  lng: bike.longitude
                }}
                icon={{
                  path: google.maps.SymbolPath.BACKWARD_CLOSED_ARROW,
                  scale: 6,
                  fillColor: bike.isAvailable ? colors.teal : colors.mediumGray,
                  fillOpacity: 0.8,
                  strokeColor: colors.white,
                  strokeWeight: 2,
                  strokeOpacity: 1,
                  rotation: 0
                }}
                title={`${bike.name} - ${bike.isAvailable ? 'Available' : 'Unavailable'}`}
                onClick={() => {
                  // Show bike info
                  setSelectedRide({
                    userName: bike.isAvailable ? 'Available Bike' : 'Unavailable Bike',
                    bikeId: bike.id,
                    status: bike.isAvailable ? 'available' : 'unavailable',
                    bikeName: bike.name,
                    bikeData: bike,
                    isStaticBike: true
                  });
                  setMapCenter({ lat: bike.latitude, lng: bike.longitude });
                  setMapZoom(16);
                }}
              />
            );
          })}

          {/* Render active ride markers using live locations */}
          {activeRides.map(ride => {
            const liveLocation = liveLocations[ride.userId];
            const fallbackLocation = ride.currentLocation; // From activeRides data
            
            // Use live location first, fallback to currentLocation from ride data
            const markerLocation = liveLocation || fallbackLocation;
            
            return markerLocation && markerLocation.latitude && markerLocation.longitude && (
              <Marker
                key={`active-ride-${ride.userId}`}
                position={{
                  lat: parseFloat(markerLocation.latitude),
                  lng: parseFloat(markerLocation.longitude)
                }}
                icon={getMarkerIcon(ride)}
                onClick={() => handleRideClick(ride)}
                title={`${ride.userName} - ${ride.status.toUpperCase()} - ${liveLocation ? 'LIVE' : 'LAST KNOWN'}`}
                zIndex={1000} // Higher z-index for active rides
              />
            );
          })}

          {/* Show info window for selected ride or bike */}
          {selectedRide && (selectedRide.isStaticBike ? 
            // Static bike info window
            <InfoWindow
              position={{
                lat: selectedRide.bikeData.latitude,
                lng: selectedRide.bikeData.longitude
              }}
              onCloseClick={() => setSelectedRide(null)}
            >
              <InfoWindowContent>
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '10px', 
                  marginBottom: '15px',
                  borderBottom: '2px solid #e0e0e0',
                  paddingBottom: '10px'
                }}>
                  <div style={{
                    width: '12px',
                    height: '12px',
                    borderRadius: '50%',
                    backgroundColor: selectedRide.bikeData.isAvailable ? colors.teal : colors.mediumGray
                  }} />
                  <h3 style={{ margin: 0, color: colors.darkGray }}>
                    🚲 {selectedRide.bikeData.name}
                  </h3>
                  <span style={{
                    backgroundColor: selectedRide.bikeData.isAvailable ? colors.teal : colors.mediumGray,
                    color: 'white',
                    padding: '2px 8px',
                    borderRadius: '12px',
                    fontSize: '10px',
                    fontWeight: 'bold',
                    textTransform: 'uppercase'
                  }}>
                    {selectedRide.bikeData.isAvailable ? 'AVAILABLE' : 'UNAVAILABLE'}
                  </span>
                </div>
                
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '15px' }}>
                  <div>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>🆔 Bike ID:</strong><br />
                      <span style={{ color: colors.mediumGray }}>{selectedRide.bikeData.id}</span>
                    </p>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>🚴‍♀️ Type:</strong><br />
                      <span style={{ color: colors.pineGreen }}>{selectedRide.bikeData.type || 'Standard'}</span>
                    </p>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>💰 Price:</strong><br />
                      <span style={{ color: colors.info, fontWeight: 'bold' }}>
                        {selectedRide.bikeData.price || 'N/A'}
                      </span>
                    </p>
                  </div>
                  
                  <div>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>📍 Location:</strong><br />
                      <span style={{ color: colors.mediumGray, fontSize: '11px' }}>
                        {selectedRide.bikeData.latitude.toFixed(4)}, {selectedRide.bikeData.longitude.toFixed(4)}
                      </span>
                    </p>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>🔒 Status:</strong><br />
                      <span style={{ color: selectedRide.bikeData.isLocked ? colors.danger : colors.success }}>
                        {selectedRide.bikeData.isLocked ? '🔒 Locked' : '🔓 Unlocked'}
                      </span>
                    </p>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>⚙️ In Use:</strong><br />
                      <span style={{ color: selectedRide.bikeData.isInUse ? colors.warning : colors.success }}>
                        {selectedRide.bikeData.isInUse ? '🟡 Yes' : '🟢 No'}
                      </span>
                    </p>
                  </div>
                </div>
                
                {selectedRide.bikeData.description && (
                  <div style={{ 
                    backgroundColor: '#f8f9fa', 
                    padding: '10px', 
                    borderRadius: '8px',
                    marginBottom: '10px'
                  }}>
                    <p style={{ margin: '0', fontSize: '12px', color: colors.mediumGray }}>
                      <strong>📝 Description:</strong><br />
                      {selectedRide.bikeData.description}
                    </p>
                  </div>
                )}
                
                <div style={{ 
                  backgroundColor: selectedRide.bikeData.isAvailable ? '#e8f5e8' : '#fff3cd',
                  padding: '8px',
                  borderRadius: '6px',
                  border: `2px solid ${selectedRide.bikeData.isAvailable ? colors.teal : colors.mediumGray}`,
                  textAlign: 'center'
                }}>
                  <span style={{ 
                    fontSize: '12px', 
                    fontWeight: 'bold',
                    color: selectedRide.bikeData.isAvailable ? colors.teal : colors.mediumGray
                  }}>
                    {selectedRide.bikeData.isAvailable ? '✅ READY FOR RENT' : '❌ NOT AVAILABLE'}
                  </span>
                </div>
              </InfoWindowContent>
            </InfoWindow>
            :
            // Active ride info window  
            (liveLocations[selectedRide.userId] || selectedRide.currentLocation) && (
            <InfoWindow
              position={{
                lat: (liveLocations[selectedRide.userId] || selectedRide.currentLocation).latitude,
                lng: (liveLocations[selectedRide.userId] || selectedRide.currentLocation).longitude
              }}
              onCloseClick={() => setSelectedRide(null)}
            >
              <InfoWindowContent>
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '10px', 
                  marginBottom: '15px',
                  borderBottom: '2px solid #e0e0e0',
                  paddingBottom: '10px'
                }}>
                  <div style={{
                    width: '12px',
                    height: '12px',
                    borderRadius: '50%',
                    backgroundColor: selectedRide.status === 'emergency' ? colors.danger : 
                                   selectedRide.status === 'paused' ? colors.warning : colors.success,
                    animation: 'pulse 2s infinite'
                  }} />
                  <h3 style={{ margin: 0, color: colors.darkGray }}>
                    🚴‍♂️ {selectedRide.userName}
                  </h3>
                  <span style={{
                    backgroundColor: selectedRide.status === 'emergency' ? colors.danger : 
                                   selectedRide.status === 'paused' ? colors.warning : colors.success,
                    color: 'white',
                    padding: '2px 8px',
                    borderRadius: '12px',
                    fontSize: '10px',
                    fontWeight: 'bold',
                    textTransform: 'uppercase'
                  }}>
                    {selectedRide.status}
                  </span>
                  <span style={{
                    backgroundColor: liveLocations[selectedRide.userId] ? colors.success : colors.warning,
                    color: 'white',
                    padding: '1px 6px',
                    borderRadius: '8px',
                    fontSize: '9px',
                    fontWeight: 'bold'
                  }}>
                    {liveLocations[selectedRide.userId] ? 'LIVE' : 'CACHED'}
                  </span>
                </div>
                
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '15px' }}>
                  <div>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>🚲 Bike ID:</strong><br />
                      <span style={{ color: colors.mediumGray }}>{selectedRide.bikeId}</span>
                    </p>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>⏱️ Duration:</strong><br />
                      <span style={{ color: colors.pineGreen, fontWeight: 'bold' }}>
                        {formatDuration(selectedRide.lastLocationUpdate)}
                      </span>
                    </p>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>📍 Distance:</strong><br />
                      <span style={{ color: colors.info, fontWeight: 'bold' }}>
                        {((selectedRide.totalDistance || 0) / 1000).toFixed(2)} km
                      </span>
                    </p>
                  </div>
                  
                  <div>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>🏃‍♂️ Current Speed:</strong><br />
                      <span style={{ color: colors.success, fontWeight: 'bold' }}>
                        {((liveLocations[selectedRide.userId] || selectedRide.currentLocation)?.speed || 0).toFixed(1)} km/h
                      </span>
                    </p>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>🎯 Accuracy:</strong><br />
                      <span style={{ color: colors.mediumGray }}>
                        ±{((liveLocations[selectedRide.userId] || selectedRide.currentLocation)?.accuracy || 0).toFixed(0)} m
                      </span>
                    </p>
                    <p style={{ margin: '5px 0', fontSize: '13px' }}>
                      <strong>🧭 Bearing:</strong><br />
                      <span style={{ color: colors.mediumGray }}>
                        {((liveLocations[selectedRide.userId] || selectedRide.currentLocation)?.bearing || 0).toFixed(0)}°
                      </span>
                    </p>
                  </div>
                </div>
                
                <div style={{ 
                  backgroundColor: '#f8f9fa', 
                  padding: '10px', 
                  borderRadius: '8px',
                  marginBottom: '10px'
                }}>
                  <p style={{ margin: '0', fontSize: '12px', color: colors.mediumGray }}>
                    <strong>📧 User Email:</strong> {selectedRide.userEmail || 'N/A'}<br />
                    <strong>🔄 Updates:</strong> {(liveLocations[selectedRide.userId] || selectedRide.currentLocation)?.locationCount || 0}<br />
                    <strong>🕐 Last Update:</strong> {new Date((liveLocations[selectedRide.userId] || selectedRide.currentLocation)?.timestamp || Date.now()).toLocaleTimeString()}
                  </p>
                </div>
                
                <div style={{ 
                  backgroundColor: (liveLocations[selectedRide.userId] || selectedRide.currentLocation)?.isActive ? '#e8f5e8' : '#fff3cd',
                  padding: '8px',
                  borderRadius: '6px',
                  border: `2px solid ${(liveLocations[selectedRide.userId] || selectedRide.currentLocation)?.isActive ? colors.success : colors.warning}`,
                  textAlign: 'center'
                }}>
                  <span style={{ 
                    fontSize: '12px', 
                    fontWeight: 'bold',
                    color: (liveLocations[selectedRide.userId] || selectedRide.currentLocation)?.isActive ? colors.success : colors.warning
                  }}>
                    {(liveLocations[selectedRide.userId] || selectedRide.currentLocation)?.isActive ? '🟢 LIVE TRACKING' : '🟡 TRACKING PAUSED'}
                  </span>
                </div>
                
                {selectedRide.status === 'emergency' && (
                  <div style={{ 
                    backgroundColor: '#ffebee', 
                    border: `2px solid ${colors.danger}`,
                    padding: '10px', 
                    borderRadius: '8px',
                    marginTop: '10px',
                    textAlign: 'center'
                  }}>
                    <span style={{ 
                      color: colors.danger, 
                      fontWeight: 'bold',
                      fontSize: '13px'
                    }}>
                      🚨 EMERGENCY ALERT ACTIVE
                    </span>
                  </div>
                )}
              </InfoWindowContent>
            </InfoWindow>
          ))}

          {/* Heatmap layer */}
          {showHeatmap && (
            <HeatmapLayer
              data={Object.values(liveLocations)
                .filter(location => location.isActive)
                .map(location => ({
                  location: new google.maps.LatLng(
                    location.latitude,
                    location.longitude
                  ),
                  weight: location.speed || 1
                }))
              }
            />
          )}
        </MapContainer>
      </MapWrapper>

      {/* Active Rides List */}
      <RidesList>
        <h3 style={{ marginBottom: '15px', color: colors.darkGray }}>Active Rides</h3>
        {activeRides.map(ride => {
          const liveLocation = liveLocations[ride.userId];
          return (
            <RideItem key={ride.userId} onClick={() => handleRideClick(ride)}>
              <RideInfo>
                <div style={{ fontWeight: 'bold' }}>{ride.userName}</div>
                <div style={{ fontSize: '14px', color: colors.mediumGray }}>
                  Bike: {ride.bikeId} • Duration: {formatDuration(ride.lastLocationUpdate)}
                </div>
              </RideInfo>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <RideStatus status={ride.status}>{ride.status}</RideStatus>
                <div style={{ fontSize: '14px' }}>
                  {ride.totalDistance ? 
                    `${((ride.totalDistance || 0) / 1000).toFixed(1)} km` : 
                    'No location'
                  }
                </div>
                {liveLocation && (
                  <div style={{ fontSize: '12px', color: colors.success }}>
                    LIVE • {liveLocation.speed?.toFixed(1) || 0} km/h
                  </div>
                )}
              </div>
            </RideItem>
          );
        })}
        {activeRides.length === 0 && (
          <div style={{ textAlign: 'center', color: colors.mediumGray, padding: '20px' }}>
            No active rides at the moment
          </div>
        )}
      </RidesList>
    </DashboardContainer>
  );
};

export default RealTimeTrackingDashboard; 