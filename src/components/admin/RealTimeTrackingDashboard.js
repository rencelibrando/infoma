/* global google */
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { collection, query, where, onSnapshot, orderBy, limit, getDoc, doc, updateDoc } from 'firebase/firestore';
import { db } from '../../firebase';
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
  width: 300px;
  max-height: 400px;
  overflow-y: auto;
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
  
  const unsubscribeRefs = useRef([]);

  // Setup real-time listeners for active rides
  useEffect(() => {
    const setupRealTimeListeners = () => {
      // Listen for active rides
      const ridesQuery = query(
        collection(db, 'rides'),
        where('status', 'in', ['active', 'paused']),
        orderBy('startTime', 'desc')
      );

      const unsubscribeRides = onSnapshot(ridesQuery, (snapshot) => {
        const rides = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        
        setActiveRides(rides);
        calculateStats(rides);
        checkForAlerts(rides);
        
        // Setup location listeners for each ride
        setupLocationListeners(rides);
      });

      unsubscribeRefs.current.push(unsubscribeRides);
    };

    setupRealTimeListeners();

    return () => {
      unsubscribeRefs.current.forEach(unsubscribe => unsubscribe());
    };
  }, []);

  const setupLocationListeners = (rides) => {
    // Clear existing location listeners
    unsubscribeRefs.current = unsubscribeRefs.current.filter(unsub => {
      if (unsub.isLocationListener) {
        unsub();
        return false;
      }
      return true;
    });

    rides.forEach(ride => {
      const locationQuery = query(
        collection(db, 'rideLocations'),
        where('rideId', '==', ride.id),
        orderBy('timestamp', 'desc'),
        limit(1)
      );

      const unsubscribeLocation = onSnapshot(locationQuery, (snapshot) => {
        if (!snapshot.empty) {
          const locationData = snapshot.docs[0].data();
          
          setActiveRides(prevRides => 
            prevRides.map(r => 
              r.id === ride.id 
                ? { ...r, currentLocation: locationData }
                : r
            )
          );
        }
      });

      unsubscribeLocation.isLocationListener = true;
      unsubscribeRefs.current.push(unsubscribeLocation);
    });
  };

  const calculateStats = (rides) => {
    const totalActiveRides = rides.length;
    let totalDistance = 0;
    let totalSpeed = 0;
    let speedCount = 0;
    let emergencyAlerts = 0;

    rides.forEach(ride => {
      if (ride.currentLocation) {
        totalDistance += ride.currentLocation.totalDistance || 0;
        if (ride.currentLocation.speed) {
          totalSpeed += ride.currentLocation.speed;
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
      if (ride.currentLocation) {
        const lastUpdate = new Date(ride.currentLocation.timestamp?.toDate?.() || ride.currentLocation.timestamp);
        const timeSinceUpdate = Date.now() - lastUpdate.getTime();

        // Check for stale location data
        if (timeSinceUpdate > 5 * 60 * 1000) { // 5 minutes
          newAlerts.push({
            id: `stale-${ride.id}`,
            severity: 'medium',
            message: `No location update for ${ride.userName} in ${Math.round(timeSinceUpdate / 60000)} minutes`,
            rideId: ride.id,
            timestamp: Date.now()
          });
        }

        // Check for emergency status
        if (ride.status === 'emergency') {
          newAlerts.push({
            id: `emergency-${ride.id}`,
            severity: 'high',
            message: `Emergency alert from ${ride.userName}`,
            rideId: ride.id,
            timestamp: Date.now()
          });
        }

        // Check for unusual speed
        if (ride.currentLocation.speed > 50) { // 50 km/h
          newAlerts.push({
            id: `speed-${ride.id}`,
            severity: 'medium',
            message: `${ride.userName} traveling at unusual speed: ${ride.currentLocation.speed.toFixed(1)} km/h`,
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
    if (ride.currentLocation) {
      setMapCenter({
        lat: ride.currentLocation.latitude,
        lng: ride.currentLocation.longitude
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
    if (ride.status === 'emergency') color = colors.danger;
    else if (ride.status === 'paused') color = colors.warning;

    return {
      path: google.maps.SymbolPath.CIRCLE,
      scale: 8,
      fillColor: color,
      fillOpacity: 1,
      strokeColor: colors.white,
      strokeWeight: 2
    };
  };

  const formatDuration = (startTime) => {
    const start = new Date(startTime?.toDate?.() || startTime);
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
      </ControlsContainer>

      {/* Map */}
      <MapWrapper>
        <MapContainer
          center={mapCenter}
          zoom={mapZoom}
          onLoad={() => console.log('Map loaded')}
        >
          {/* Render active ride markers */}
          {activeRides.map(ride => (
            ride.currentLocation && (
              <Marker
                key={ride.id}
                position={{
                  lat: ride.currentLocation.latitude,
                  lng: ride.currentLocation.longitude
                }}
                icon={getMarkerIcon(ride)}
                onClick={() => handleRideClick(ride)}
              />
            )
          ))}

          {/* Show info window for selected ride */}
          {selectedRide && selectedRide.currentLocation && (
            <InfoWindow
              position={{
                lat: selectedRide.currentLocation.latitude,
                lng: selectedRide.currentLocation.longitude
              }}
              onCloseClick={() => setSelectedRide(null)}
            >
              <InfoWindowContent>
                <h3>{selectedRide.userName}</h3>
                <p><strong>Bike:</strong> {selectedRide.bikeId}</p>
                <p><strong>Duration:</strong> {formatDuration(selectedRide.startTime)}</p>
                <p><strong>Distance:</strong> {((selectedRide.currentLocation.totalDistance || 0) / 1000).toFixed(2)} km</p>
                <p><strong>Speed:</strong> {(selectedRide.currentLocation.speed || 0).toFixed(1)} km/h</p>
                <p><strong>Status:</strong> 
                  <RideStatus status={selectedRide.status}>{selectedRide.status}</RideStatus>
                </p>
                <p><strong>Last Update:</strong> {new Date(selectedRide.currentLocation.timestamp?.toDate?.() || selectedRide.currentLocation.timestamp).toLocaleTimeString()}</p>
              </InfoWindowContent>
            </InfoWindow>
          )}

          {/* Heatmap layer */}
          {showHeatmap && (
            <HeatmapLayer
              data={activeRides
                .filter(ride => ride.currentLocation)
                .map(ride => ({
                  location: new google.maps.LatLng(
                    ride.currentLocation.latitude,
                    ride.currentLocation.longitude
                  ),
                  weight: ride.currentLocation.speed || 1
                }))
              }
            />
          )}
        </MapContainer>
      </MapWrapper>

      {/* Active Rides List */}
      <RidesList>
        <h3 style={{ marginBottom: '15px', color: colors.darkGray }}>Active Rides</h3>
        {activeRides.map(ride => (
          <RideItem key={ride.id} onClick={() => handleRideClick(ride)}>
            <RideInfo>
              <div style={{ fontWeight: 'bold' }}>{ride.userName}</div>
              <div style={{ fontSize: '14px', color: colors.mediumGray }}>
                Bike: {ride.bikeId} • Duration: {formatDuration(ride.startTime)}
              </div>
            </RideInfo>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <RideStatus status={ride.status}>{ride.status}</RideStatus>
              <div style={{ fontSize: '14px' }}>
                {ride.currentLocation ? 
                  `${((ride.currentLocation.totalDistance || 0) / 1000).toFixed(1)} km` : 
                  'No location'
                }
              </div>
            </div>
          </RideItem>
        ))}
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