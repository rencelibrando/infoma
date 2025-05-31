import React, { useState, useEffect, useCallback, useRef } from 'react';
import { collection, query, where, onSnapshot, orderBy, limit } from 'firebase/firestore';
import { db } from '../firebase';
import styled, { keyframes, css } from 'styled-components';
import { Marker, InfoWindow, Polyline, Circle } from '@react-google-maps/api';
import MapContainer from './MapContainer';

// Grab-inspired color palette
const theme = {
  primary: '#00B14F',
  primaryDark: '#009944',
  secondary: '#1A1A1A',
  accent: '#FF6B35',
  warning: '#FFB800',
  danger: '#E74C3C',
  success: '#27AE60',
  info: '#3498DB',
  white: '#FFFFFF',
  lightGray: '#F8F9FA',
  mediumGray: '#6C757D',
  darkGray: '#343A40',
  background: '#F5F7FA',
  shadow: 'rgba(0, 0, 0, 0.1)',
  cardShadow: '0 4px 20px rgba(0, 0, 0, 0.08)',
  hoverShadow: '0 8px 30px rgba(0, 0, 0, 0.12)'
};

// Animations
const pulse = keyframes`
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.7;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
`;

const ripple = keyframes`
  0% {
    transform: scale(0);
    opacity: 1;
  }
  100% {
    transform: scale(4);
    opacity: 0;
  }
`;

const slideIn = keyframes`
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
`;

const MapWrapper = styled.div`
  background: linear-gradient(135deg, ${theme.background} 0%, #E8F4FD 100%);
  min-height: 100vh;
  position: relative;
  overflow: hidden;
`;

const Header = styled.div`
  background: ${theme.white};
  padding: 20px 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: ${theme.cardShadow};
  position: relative;
  z-index: 100;
`;

const Title = styled.h1`
  color: ${theme.secondary};
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 15px;
`;

const LiveIndicator = styled.div`
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: ${theme.danger};
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: ${theme.danger};
    transform: translate(-50%, -50%);
    animation: ${ripple} 2s infinite;
  }
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  padding: 20px 30px;
`;

const StatCard = styled.div`
  background: ${theme.white};
  border-radius: 16px;
  padding: 24px;
  box-shadow: ${theme.cardShadow};
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: ${theme.hoverShadow};
  }
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 4px;
    background: ${props => props.color || theme.primary};
  }
`;

const StatValue = styled.div`
  font-size: 36px;
  font-weight: 800;
  color: ${props => props.color || theme.primary};
  margin-bottom: 8px;
  line-height: 1;
`;

const StatLabel = styled.div`
  font-size: 14px;
  color: ${theme.mediumGray};
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
`;

const StatTrend = styled.div`
  font-size: 12px;
  color: ${props => props.positive ? theme.success : theme.danger};
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
`;

const MapSection = styled.div`
  height: calc(100vh - 280px);
  margin: 0 30px 30px 30px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: ${theme.hoverShadow};
  position: relative;
`;

const MapControls = styled.div`
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

const ControlButton = styled.button`
  background: ${props => props.active ? theme.primary : theme.white};
  color: ${props => props.active ? theme.white : theme.secondary};
  border: none;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: ${theme.cardShadow};
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 140px;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: ${theme.hoverShadow};
    background: ${props => props.active ? theme.primaryDark : theme.lightGray};
  }
  
  &:active {
    transform: translateY(0);
  }
`;

const FilterControls = styled.div`
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

const FilterSelect = styled.select`
  background: ${theme.white};
  border: 2px solid transparent;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: ${theme.secondary};
  cursor: pointer;
  box-shadow: ${theme.cardShadow};
  transition: all 0.3s ease;
  min-width: 160px;
  
  &:focus {
    outline: none;
    border-color: ${theme.primary};
    box-shadow: 0 0 0 3px rgba(0, 177, 79, 0.1);
  }
  
  &:hover {
    background: ${theme.lightGray};
  }
`;

const SearchInput = styled.input`
  background: ${theme.white};
  border: 2px solid transparent;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  color: ${theme.secondary};
  box-shadow: ${theme.cardShadow};
  transition: all 0.3s ease;
  min-width: 200px;
  
  &:focus {
    outline: none;
    border-color: ${theme.primary};
    box-shadow: 0 0 0 3px rgba(0, 177, 79, 0.1);
  }
  
  &::placeholder {
    color: ${theme.mediumGray};
  }
`;

const RiderPanel = styled.div`
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 20px;
  background: ${theme.white};
  border-radius: 16px;
  box-shadow: ${theme.hoverShadow};
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
  transition: all 0.3s ease;
  
  ${props => !props.visible && css`
    transform: translateY(100%);
    opacity: 0;
    pointer-events: none;
  `}
`;

const RiderList = styled.div`
  padding: 20px;
`;

const RiderItem = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  
  &:hover {
    background: ${theme.lightGray};
    border-color: ${theme.primary};
  }
  
  &:last-child {
    margin-bottom: 0;
  }
`;

const RiderInfo = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

const RiderAvatar = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: ${props => props.color || theme.primary};
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${theme.white};
  font-weight: 700;
  font-size: 16px;
`;

const RiderDetails = styled.div``;

const RiderName = styled.div`
  font-weight: 600;
  color: ${theme.secondary};
  font-size: 14px;
`;

const RiderStatus = styled.div`
  font-size: 12px;
  color: ${theme.mediumGray};
  display: flex;
  align-items: center;
  gap: 4px;
`;

const StatusDot = styled.div`
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: ${props => {
    switch (props.status) {
      case 'live': return theme.success;
      case 'delayed': return theme.warning;
      case 'offline': return theme.danger;
      default: return theme.mediumGray;
    }
  }};
  animation: ${props => props.status === 'live' ? pulse : 'none'} 2s infinite;
`;

const RiderMetrics = styled.div`
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: ${theme.mediumGray};
`;

const MetricItem = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
`;

const MetricValue = styled.div`
  font-weight: 600;
  color: ${theme.secondary};
`;

const MetricLabel = styled.div`
  font-size: 10px;
  text-transform: uppercase;
`;

const CustomInfoWindow = styled.div`
  background: ${theme.white};
  border-radius: 16px;
  padding: 20px;
  min-width: 300px;
  box-shadow: ${theme.hoverShadow};
`;

const InfoHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: between;
  margin-bottom: 16px;
  gap: 12px;
`;

const InfoTitle = styled.h3`
  margin: 0;
  color: ${theme.secondary};
  font-size: 18px;
  font-weight: 700;
`;

const InfoDetails = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
`;

const InfoItem = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

const InfoLabel = styled.div`
  font-size: 11px;
  color: ${theme.mediumGray};
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.5px;
`;

const InfoValue = styled.div`
  font-size: 14px;
  color: ${theme.secondary};
  font-weight: 600;
`;

const LoadingOverlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
`;

const LoadingSpinner = styled.div`
  width: 50px;
  height: 50px;
  border: 4px solid ${theme.lightGray};
  border-top: 4px solid ${theme.primary};
  border-radius: 50%;
  animation: spin 1s linear infinite;
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;

const NotificationToast = styled.div`
  position: fixed;
  top: 20px;
  right: 20px;
  background: ${theme.white};
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: ${theme.hoverShadow};
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 12px;
  animation: ${slideIn} 0.3s ease;
  border-left: 4px solid ${theme.success};
`;

const EnhancedRealTimeMap = () => {
  const [activeRides, setActiveRides] = useState([]);
  const [riderLocations, setRiderLocations] = useState({});
  const [users, setUsers] = useState({});
  const [selectedRider, setSelectedRider] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Map controls
  const [showRoutes, setShowRoutes] = useState(true);
  const [showHeatmap, setShowHeatmap] = useState(false);
  const [autoFollow, setAutoFollow] = useState(false);
  const [showRiderPanel, setShowRiderPanel] = useState(true);
  
  // Filters
  const [filterStatus, setFilterStatus] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  
  // Map state
  const [mapCenter, setMapCenter] = useState({ lat: 14.5995, lng: 120.9842 }); // Manila
  const [mapRef, setMapRef] = useState(null);
  const [lastUpdate, setLastUpdate] = useState(new Date());
  
  // Statistics
  const [stats, setStats] = useState({
    activeRiders: 0,
    liveRiders: 0,
    totalDistance: 0,
    averageSpeed: 0
  });

  // Setup real-time listeners
  useEffect(() => {
    const setupRealTimeListeners = () => {
      try {
        // Listen to active rides
        const ridesQuery = query(
          collection(db, 'rides'),
          where('status', '==', 'active'),
          orderBy('startTime', 'desc'),
          limit(100)
        );

        const unsubscribeRides = onSnapshot(ridesQuery, (snapshot) => {
          const rides = snapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
          }));
          
          setActiveRides(rides);
          setupLocationListeners(rides);
          setLastUpdate(new Date());
        }, (error) => {
          console.error('Error listening to rides:', error);
          setError('Failed to load active rides');
        });

        return unsubscribeRides;
      } catch (err) {
        console.error('Error setting up listeners:', err);
        setError('Failed to initialize real-time tracking');
        return () => {};
      }
    };

    const unsubscribe = setupRealTimeListeners();
    setLoading(false);

    return unsubscribe;
  }, []);

  // Setup location listeners for active rides
  const setupLocationListeners = (rides) => {
    const locationUnsubscribers = [];

    rides.forEach(ride => {
      if (ride.id) {
        const locationQuery = query(
          collection(db, 'locations'),
          where('rideId', '==', ride.id),
          orderBy('timestamp', 'desc'),
          limit(1)
        );

        const unsubscribe = onSnapshot(locationQuery, (snapshot) => {
          snapshot.docs.forEach(doc => {
            const locationData = doc.data();
            setRiderLocations(prev => ({
              ...prev,
              [ride.id]: {
                ...locationData,
                lastUpdate: new Date(locationData.timestamp),
                isMoving: locationData.speed > 0.5,
                rideId: ride.id,
                userId: ride.userId
              }
            }));
          });
          calculateStats();
        });

        locationUnsubscribers.push(unsubscribe);
      }
    });

    return () => {
      locationUnsubscribers.forEach(unsub => unsub());
    };
  };

  // Calculate real-time statistics
  const calculateStats = useCallback(() => {
    const locations = Object.values(riderLocations);
    const liveLocations = locations.filter(loc => 
      (new Date() - new Date(loc.lastUpdate)) < 30000
    );

    setStats({
      activeRiders: locations.length,
      liveRiders: liveLocations.length,
      totalDistance: locations.reduce((sum, loc) => sum + (loc.distance || 0), 0),
      averageSpeed: locations.length > 0 
        ? locations.reduce((sum, loc) => sum + (loc.speed || 0), 0) / locations.length * 3.6
        : 0
    });
  }, [riderLocations]);

  // Filter riders based on status and search
  const filteredRiders = Object.entries(riderLocations).filter(([rideId, location]) => {
    const status = getLocationStatus(location.lastUpdate);
    const statusMatch = filterStatus === 'all' || status === filterStatus;
    const searchMatch = !searchTerm || 
      (users[location.userId]?.name || '').toLowerCase().includes(searchTerm.toLowerCase());
    
    return statusMatch && searchMatch;
  });

  // Get location status
  const getLocationStatus = (lastUpdate) => {
    const now = new Date();
    const diff = now - new Date(lastUpdate);
    
    if (diff < 30000) return 'live';
    if (diff < 120000) return 'delayed';
    return 'offline';
  };

  // Custom marker icon based on rider status
  const getMarkerIcon = (location) => {
    const status = getLocationStatus(location.lastUpdate);
    const colors = {
      live: theme.success,
      delayed: theme.warning,
      offline: theme.danger
    };

    // Check if Google Maps is loaded
    if (window.google && window.google.maps) {
      return {
        path: window.google.maps.SymbolPath.CIRCLE,
        fillColor: colors[status],
        fillOpacity: 1,
        strokeColor: theme.white,
        strokeWeight: 3,
        scale: 8
      };
    }

    // Fallback to default marker if Google Maps not loaded
    return {
      url: `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(`
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="12" cy="12" r="8" fill="${colors[status]}" stroke="#FFFFFF" stroke-width="3"/>
        </svg>
      `)}`,
      scaledSize: new (window.google?.maps?.Size || function(w, h) { this.width = w; this.height = h; })(24, 24),
      anchor: new (window.google?.maps?.Point || function(x, y) { this.x = x; this.y = y; })(12, 12)
    };
  };

  // Handle rider selection
  const handleRiderSelect = (rideId, location) => {
    setSelectedRider(rideId);
    if (mapRef) {
      mapRef.panTo({ lat: location.latitude, lng: location.longitude });
      mapRef.setZoom(16);
    }
  };

  if (error) {
    return (
      <MapWrapper>
        <div style={{ 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center', 
          height: '100vh',
          flexDirection: 'column',
          gap: '20px'
        }}>
          <div style={{ fontSize: '48px' }}>🚨</div>
          <h2 style={{ color: theme.danger }}>Error Loading Map</h2>
          <p style={{ color: theme.mediumGray }}>{error}</p>
        </div>
      </MapWrapper>
    );
  }

  return (
    <MapWrapper>
      {loading && (
        <LoadingOverlay>
          <LoadingSpinner />
        </LoadingOverlay>
      )}
      
      <Header>
        <Title>
          <LiveIndicator />
          Real-Time Rider Tracking
        </Title>
        <div style={{ fontSize: '14px', color: theme.mediumGray }}>
          Last updated: {lastUpdate.toLocaleTimeString()}
        </div>
      </Header>

      <StatsGrid>
        <StatCard color={theme.primary}>
          <StatValue color={theme.primary}>{stats.activeRiders}</StatValue>
          <StatLabel>Active Riders</StatLabel>
          <StatTrend positive={stats.activeRiders > 0}>
            📍 Currently tracking
          </StatTrend>
        </StatCard>

        <StatCard color={theme.success}>
          <StatValue color={theme.success}>{stats.liveRiders}</StatValue>
          <StatLabel>Live Updates</StatLabel>
          <StatTrend positive={true}>
            🔴 Last 30 seconds
          </StatTrend>
        </StatCard>

        <StatCard color={theme.info}>
          <StatValue color={theme.info}>{stats.averageSpeed.toFixed(1)}</StatValue>
          <StatLabel>Avg Speed (km/h)</StatLabel>
          <StatTrend positive={stats.averageSpeed > 0}>
            🚴‍♂️ Real-time average
          </StatTrend>
        </StatCard>

        <StatCard color={theme.accent}>
          <StatValue color={theme.accent}>{stats.totalDistance.toFixed(1)}</StatValue>
          <StatLabel>Total Distance (km)</StatLabel>
          <StatTrend positive={true}>
            📏 All active rides
          </StatTrend>
        </StatCard>
      </StatsGrid>

      <MapSection>
        <MapControls>
          <ControlButton 
            active={showRoutes} 
            onClick={() => setShowRoutes(!showRoutes)}
          >
            🛣️ Routes
          </ControlButton>
          
          <ControlButton 
            active={autoFollow} 
            onClick={() => setAutoFollow(!autoFollow)}
          >
            🎯 Auto Follow
          </ControlButton>
          
          <ControlButton 
            active={showRiderPanel} 
            onClick={() => setShowRiderPanel(!showRiderPanel)}
          >
            📋 Riders
          </ControlButton>
        </MapControls>

        <FilterControls>
          <FilterSelect 
            value={filterStatus} 
            onChange={(e) => setFilterStatus(e.target.value)}
          >
            <option value="all">All Riders</option>
            <option value="live">Live Only</option>
            <option value="delayed">Delayed</option>
            <option value="offline">Offline</option>
          </FilterSelect>
          
          <SearchInput
            type="text"
            placeholder="🔍 Search riders..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </FilterControls>

        <MapContainer 
          center={mapCenter}
          zoom={14}
          onLoad={setMapRef}
          style={{ height: '100%', width: '100%' }}
          options={{
            styles: [
              {
                featureType: 'poi',
                elementType: 'labels',
                stylers: [{ visibility: 'off' }]
              },
              {
                featureType: 'transit',
                elementType: 'labels',
                stylers: [{ visibility: 'off' }]
              }
            ],
            disableDefaultUI: true,
            zoomControl: true,
            mapTypeControl: false,
            scaleControl: true,
            streetViewControl: false,
            rotateControl: false,
            fullscreenControl: true
          }}
        >
          {/* Rider markers */}
          {filteredRiders.map(([rideId, location]) => (
            <Marker
              key={`rider_${rideId}`}
              position={{ lat: location.latitude, lng: location.longitude }}
              onClick={() => handleRiderSelect(rideId, location)}
              icon={getMarkerIcon(location)}
              title={`${users[location.userId]?.name || 'Rider'} - ${(location.speed * 3.6).toFixed(1)} km/h`}
            />
          ))}

          {/* Selected rider info window */}
          {selectedRider && riderLocations[selectedRider] && (
            <InfoWindow
              position={{ 
                lat: riderLocations[selectedRider].latitude, 
                lng: riderLocations[selectedRider].longitude 
              }}
              onCloseClick={() => setSelectedRider(null)}
            >
              <CustomInfoWindow>
                <InfoHeader>
                  <RiderAvatar color={theme.primary}>
                    {(users[riderLocations[selectedRider].userId]?.name || 'R')[0].toUpperCase()}
                  </RiderAvatar>
                  <div>
                    <InfoTitle>
                      {users[riderLocations[selectedRider].userId]?.name || 'Unknown Rider'}
                    </InfoTitle>
                    <StatusDot status={getLocationStatus(riderLocations[selectedRider].lastUpdate)} />
                  </div>
                </InfoHeader>
                
                <InfoDetails>
                  <InfoItem>
                    <InfoLabel>Speed</InfoLabel>
                    <InfoValue>{(riderLocations[selectedRider].speed * 3.6).toFixed(1)} km/h</InfoValue>
                  </InfoItem>
                  
                  <InfoItem>
                    <InfoLabel>Accuracy</InfoLabel>
                    <InfoValue>{riderLocations[selectedRider].accuracy?.toFixed(1) || 'N/A'} m</InfoValue>
                  </InfoItem>
                  
                  <InfoItem>
                    <InfoLabel>Last Update</InfoLabel>
                    <InfoValue>
                      {new Date(riderLocations[selectedRider].lastUpdate).toLocaleTimeString()}
                    </InfoValue>
                  </InfoItem>
                  
                  <InfoItem>
                    <InfoLabel>Ride ID</InfoLabel>
                    <InfoValue>{selectedRider.slice(-8)}</InfoValue>
                  </InfoItem>
                </InfoDetails>
              </CustomInfoWindow>
            </InfoWindow>
          )}
        </MapContainer>

        <RiderPanel visible={showRiderPanel}>
          <RiderList>
            <h3 style={{ margin: '0 0 16px 0', color: theme.secondary }}>
              Active Riders ({filteredRiders.length})
            </h3>
            
            {filteredRiders.map(([rideId, location]) => {
              const user = users[location.userId];
              const status = getLocationStatus(location.lastUpdate);
              
              return (
                <RiderItem 
                  key={rideId}
                  onClick={() => handleRiderSelect(rideId, location)}
                >
                  <RiderInfo>
                    <RiderAvatar color={theme.primary}>
                      {(user?.name || 'R')[0].toUpperCase()}
                    </RiderAvatar>
                    <RiderDetails>
                      <RiderName>{user?.name || 'Unknown Rider'}</RiderName>
                      <RiderStatus>
                        <StatusDot status={status} />
                        {status.charAt(0).toUpperCase() + status.slice(1)}
                      </RiderStatus>
                    </RiderDetails>
                  </RiderInfo>
                  
                  <RiderMetrics>
                    <MetricItem>
                      <MetricValue>{(location.speed * 3.6).toFixed(1)}</MetricValue>
                      <MetricLabel>km/h</MetricLabel>
                    </MetricItem>
                    <MetricItem>
                      <MetricValue>{location.accuracy?.toFixed(0) || 'N/A'}</MetricValue>
                      <MetricLabel>accuracy</MetricLabel>
                    </MetricItem>
                  </RiderMetrics>
                </RiderItem>
              );
            })}
          </RiderList>
        </RiderPanel>
      </MapSection>
    </MapWrapper>
  );
};

export default EnhancedRealTimeMap; 