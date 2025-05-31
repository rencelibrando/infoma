import React, { useState, useEffect, useCallback, useRef } from 'react';
import { collection, query, where, onSnapshot, orderBy, limit, getDoc, doc } from 'firebase/firestore';
import { db } from '../firebase';
import styled from 'styled-components';
import { Marker, InfoWindow, Polyline, Circle, HeatmapLayer } from '@react-google-maps/api';
import MapContainer from './MapContainer';

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

const RealTimeMapContainer = styled.div`
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

const MapWrapper = styled.div`
  height: 700px;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
`;

const StatsContainer = styled.div`
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

const StatTrend = styled.div`
  font-size: 12px;
  color: ${props => props.positive ? colors.success : colors.danger};
  margin-top: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
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

const FilterSelect = styled.select`
  padding: 10px 15px;
  border: 2px solid ${colors.lightGray};
  border-radius: 8px;
  font-size: 14px;
  background: ${colors.white};
  color: ${colors.darkGray};
  cursor: pointer;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
`;

const SearchInput = styled.input`
  padding: 10px 15px;
  border: 2px solid ${colors.lightGray};
  border-radius: 8px;
  font-size: 14px;
  background: ${colors.white};
  color: ${colors.darkGray};
  min-width: 200px;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
  
  &::placeholder {
    color: ${colors.mediumGray};
  }
`;

const InfoWindowContent = styled.div`
  padding: 15px;
  width: 280px;
  max-height: 400px;
  overflow-y: auto;
`;

const RiderTitle = styled.h3`
  font-size: 18px;
  margin-bottom: 12px;
  color: ${colors.darkGray};
  display: flex;
  align-items: center;
  gap: 8px;
`;

const RiderDetail = styled.div`
  font-size: 13px;
  margin: 8px 0;
  color: ${colors.mediumGray};
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

const DetailLabel = styled.span`
  font-weight: 600;
  color: ${colors.darkGray};
`;

const DetailValue = styled.span`
  color: ${props => props.color || colors.mediumGray};
  font-weight: 500;
`;

const StatusBadge = styled.div`
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: bold;
  background: ${props => {
    if (props.status === 'live') return colors.success;
    if (props.status === 'delayed') return colors.warning;
    if (props.status === 'offline') return colors.danger;
    return colors.mediumGray;
  }};
  color: white;
  margin-top: 10px;
`;

const LastUpdateTime = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  text-align: right;
  margin-bottom: 10px;
  background: ${colors.white};
  padding: 8px 12px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
`;

const LoadingMessage = styled.div`
  text-align: center;
  padding: 60px;
  color: ${colors.mediumGray};
  font-size: 18px;
  background: ${colors.white};
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
`;

const ErrorMessage = styled.div`
  text-align: center;
  padding: 30px;
  color: ${colors.danger};
  background: linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%);
  border-radius: 12px;
  margin: 20px 0;
  border: 1px solid ${colors.danger};
`;

const RefreshIndicator = styled.div`
  position: fixed;
  top: 20px;
  right: 20px;
  background: linear-gradient(135deg, ${colors.success} 0%, #66BB6A 100%);
  color: white;
  padding: 12px 20px;
  border-radius: 25px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 1000;
  animation: slideIn 0.3s ease-out;
  box-shadow: 0 4px 20px rgba(76, 175, 80, 0.3);
  
  @keyframes slideIn {
    from {
      transform: translateX(100%);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }
`;

const AlertsContainer = styled.div`
  margin-bottom: 20px;
`;

const Alert = styled.div`
  background: ${props => {
    if (props.type === 'warning') return '#fff3cd';
    if (props.type === 'danger') return '#f8d7da';
    return '#d1ecf1';
  }};
  color: ${props => {
    if (props.type === 'warning') return '#856404';
    if (props.type === 'danger') return '#721c24';
    return '#0c5460';
  }};
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 10px;
  border-left: 4px solid ${props => {
    if (props.type === 'warning') return colors.warning;
    if (props.type === 'danger') return colors.danger;
    return colors.info;
  }};
  display: flex;
  align-items: center;
  gap: 10px;
`;

const RiderListSidebar = styled.div`
  position: fixed;
  right: ${props => props.isOpen ? '0' : '-350px'};
  top: 0;
  width: 350px;
  height: 100vh;
  background: ${colors.white};
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  transition: right 0.3s ease;
  z-index: 1001;
  overflow-y: auto;
  padding: 20px;
`;

const SidebarToggle = styled.button`
  position: fixed;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  background: ${colors.pineGreen};
  color: white;
  border: none;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  cursor: pointer;
  z-index: 1002;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
  
  &:hover {
    transform: translateY(-50%) scale(1.1);
  }
`;

const RiderListItem = styled.div`
  padding: 15px;
  border-bottom: 1px solid ${colors.lightGray};
  cursor: pointer;
  transition: background 0.2s ease;
  
  &:hover {
    background: ${colors.lightGray};
  }
  
  &:last-child {
    border-bottom: none;
  }
`;

const RealTimeRiderMap = () => {
  const [activeRides, setActiveRides] = useState([]);
  const [riderLocations, setRiderLocations] = useState({});
  const [riderRoutes, setRiderRoutes] = useState({});
  const [users, setUsers] = useState({});
  const [selectedRider, setSelectedRider] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [mapCenter, setMapCenter] = useState({ lat: 14.5995, lng: 120.9842 });
  const [showRoutes, setShowRoutes] = useState(true);
  const [showHeatmap, setShowHeatmap] = useState(false);
  const [autoFollow, setAutoFollow] = useState(false);
  const [lastUpdate, setLastUpdate] = useState(new Date());
  const [showUpdateIndicator, setShowUpdateIndicator] = useState(false);
  const [mapType, setMapType] = useState('roadmap');
  const [filterStatus, setFilterStatus] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [alerts, setAlerts] = useState([]);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [riderStats, setRiderStats] = useState({});
  
  // Refs for cleanup
  const unsubscribeRides = useRef(null);
  const unsubscribeLocations = useRef({});
  const updateIndicatorTimeout = useRef(null);
  const mapRef = useRef(null);

  // Set up real-time listeners
  useEffect(() => {
    setupRealTimeListeners();
    
    return () => {
      cleanup();
    };
  }, []);

  // Monitor for alerts
  useEffect(() => {
    checkForAlerts();
  }, [riderLocations, activeRides]);

  // Auto-follow selected rider
  useEffect(() => {
    if (autoFollow && selectedRider && riderLocations[selectedRider] && mapRef.current) {
      const location = riderLocations[selectedRider];
      mapRef.current.panTo({ lat: location.latitude, lng: location.longitude });
    }
  }, [autoFollow, selectedRider, riderLocations]);

  const setupRealTimeListeners = () => {
    try {
      // Listen to active rides with enhanced query - use new status field
      let ridesQuery;
      try {
        // Try to query by status first (new format)
        ridesQuery = query(
          collection(db, "rides"),
          where("status", "==", "active"),
          orderBy("startTime", "desc")
        );
      } catch (error) {
        // Fallback to isActive if status index doesn't exist yet
        console.log('Using isActive fallback for real-time rides query');
        ridesQuery = query(
          collection(db, "rides"),
          where("isActive", "==", true),
          orderBy("startDate", "desc")
        );
      }
      
      unsubscribeRides.current = onSnapshot(ridesQuery, (snapshot) => {
        const rides = snapshot.docs.map(doc => {
          const data = doc.data();
          return {
            id: doc.id,
            ...data,
            // Handle both new startTime (milliseconds) and legacy startDate
            startTime: data.startTime ? new Date(data.startTime) : (data.startDate?.toDate() || null),
            endTime: data.endTime ? new Date(data.endTime) : (data.endDate?.toDate() || null),
            lastUpdate: data.lastLocationUpdate?.toDate() || null,
            // Normalize status field
            isActive: data.status === "active" || data.isActive === true,
            status: data.status || (data.isActive ? "active" : "completed")
          };
        });
        
        setActiveRides(rides);
        
        // Set up location listeners for each active ride
        setupLocationListeners(rides);
        
        // Fetch user data for new rides
        fetchUserData(rides);
        
        // Calculate rider statistics
        calculateRiderStats(rides);
        
        setLoading(false);
        showUpdateNotification();
      }, (error) => {
        console.error('Error listening to rides:', error);
        setError('Failed to load active rides');
        setLoading(false);
      });
      
    } catch (error) {
      console.error('Error setting up listeners:', error);
      setError('Failed to initialize real-time tracking');
      setLoading(false);
    }
  };

  const setupLocationListeners = (rides) => {
    // Clean up existing location listeners
    Object.values(unsubscribeLocations.current).forEach(unsubscribe => {
      if (typeof unsubscribe === 'function') {
        unsubscribe();
      }
    });
    unsubscribeLocations.current = {};

    // Set up new location listeners
    rides.forEach(ride => {
      if (!ride.id) return;

      // Listen to current ride location with enhanced data
      const rideRef = collection(db, "rides");
      const rideQuery = query(rideRef, where("__name__", "==", ride.id));
      
      unsubscribeLocations.current[`ride_${ride.id}`] = onSnapshot(rideQuery, (snapshot) => {
        snapshot.docs.forEach(doc => {
          const rideData = doc.data();
          if (rideData.currentLatitude && rideData.currentLongitude) {
            const newLocation = {
              latitude: rideData.currentLatitude,
              longitude: rideData.currentLongitude,
              bearing: rideData.currentBearing || 0,
              speed: rideData.currentSpeed || 0,
              accuracy: rideData.locationAccuracy || 0,
              altitude: rideData.altitude || 0,
              lastUpdate: rideData.lastLocationUpdate?.toDate() || new Date(),
              userId: ride.userId,
              bikeId: ride.bikeId,
              batteryLevel: rideData.batteryLevel || 100,
              isMoving: (rideData.currentSpeed || 0) > 0.5 // Moving if speed > 0.5 m/s
            };
            
            setRiderLocations(prev => ({
              ...prev,
              [ride.id]: newLocation
            }));
            showUpdateNotification();
          }
        });
      });

      // Listen to location history for route tracking with more points
      if (showRoutes) {
        const locationHistoryQuery = query(
          collection(db, "rideLocationHistory"),
          where("rideId", "==", ride.id),
          orderBy("timestamp", "desc"),
          limit(100) // Increased to 100 location points for better route visualization
        );
        
        unsubscribeLocations.current[`route_${ride.id}`] = onSnapshot(locationHistoryQuery, (snapshot) => {
          const locations = snapshot.docs.map(doc => ({
            ...doc.data(),
            timestamp: doc.data().timestamp?.toDate() || new Date()
          })).reverse(); // Reverse to get chronological order
          
          if (locations.length > 1) {
            setRiderRoutes(prev => ({
              ...prev,
              [ride.id]: locations.map(loc => ({
                lat: loc.latitude,
                lng: loc.longitude,
                timestamp: loc.timestamp
              }))
            }));
          }
        });
      }
    });
  };

  const fetchUserData = async (rides) => {
    const userIds = [...new Set(rides.map(ride => ride.userId))];
    const newUsers = {};
    
    for (const userId of userIds) {
      if (!users[userId]) {
        try {
          const userDoc = await getDoc(doc(db, "users", userId));
          if (userDoc.exists()) {
            const userData = userDoc.data();
            newUsers[userId] = { 
              id: userId, 
              ...userData,
              profilePicture: userData.profilePicture || null,
              phoneNumber: userData.phoneNumber || 'N/A',
              email: userData.email || 'N/A'
            };
          } else {
            newUsers[userId] = { id: userId, name: 'Unknown User' };
          }
        } catch (error) {
          console.error(`Error fetching user ${userId}:`, error);
          newUsers[userId] = { id: userId, name: 'Unknown User' };
        }
      }
    }
    
    if (Object.keys(newUsers).length > 0) {
      setUsers(prev => ({ ...prev, ...newUsers }));
    }
  };

  const calculateRiderStats = (rides) => {
    const stats = {};
    
    rides.forEach(ride => {
      const location = riderLocations[ride.id];
      if (location) {
        const rideTime = new Date() - ride.startTime;
        const hours = rideTime / (1000 * 60 * 60);
        
        stats[ride.id] = {
          rideTime: rideTime,
          averageSpeed: location.speed || 0,
          distance: calculateRideDistance(ride.id),
          status: getLocationStatus(location.lastUpdate)
        };
      }
    });
    
    setRiderStats(stats);
  };

  const calculateRideDistance = (rideId) => {
    const route = riderRoutes[rideId];
    if (!route || route.length < 2) return 0;
    
    let distance = 0;
    for (let i = 1; i < route.length; i++) {
      distance += getDistanceBetweenPoints(route[i-1], route[i]);
    }
    return distance;
  };

  const getDistanceBetweenPoints = (point1, point2) => {
    const R = 6371e3; // Earth's radius in meters
    const φ1 = point1.lat * Math.PI/180;
    const φ2 = point2.lat * Math.PI/180;
    const Δφ = (point2.lat-point1.lat) * Math.PI/180;
    const Δλ = (point2.lng-point1.lng) * Math.PI/180;

    const a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
              Math.cos(φ1) * Math.cos(φ2) *
              Math.sin(Δλ/2) * Math.sin(Δλ/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

    return R * c;
  };

  const getLocationStatus = (lastUpdate) => {
    if (!lastUpdate) return 'offline';
    const timeDiff = new Date() - lastUpdate;
    if (timeDiff < 30000) return 'live'; // Less than 30 seconds
    if (timeDiff < 300000) return 'delayed'; // Less than 5 minutes
    return 'offline';
  };

  const checkForAlerts = () => {
    const newAlerts = [];
    
    // Check for offline riders
    Object.entries(riderLocations).forEach(([rideId, location]) => {
      const status = getLocationStatus(location.lastUpdate);
      if (status === 'offline') {
        const user = users[location.userId];
        newAlerts.push({
          id: `offline_${rideId}`,
          type: 'danger',
          message: `${user?.name || 'Unknown rider'} has been offline for more than 5 minutes`,
          rideId
        });
      }
    });
    
    // Check for low battery
    Object.entries(riderLocations).forEach(([rideId, location]) => {
      if (location.batteryLevel && location.batteryLevel < 20) {
        const user = users[location.userId];
        newAlerts.push({
          id: `battery_${rideId}`,
          type: 'warning',
          message: `${user?.name || 'Unknown rider'}'s bike battery is low (${location.batteryLevel}%)`,
          rideId
        });
      }
    });
    
    // Check for stationary riders (not moving for more than 10 minutes)
    Object.entries(riderLocations).forEach(([rideId, location]) => {
      if (!location.isMoving) {
        const ride = activeRides.find(r => r.id === rideId);
        if (ride && ride.startTime) {
          const stationaryTime = new Date() - location.lastUpdate;
          if (stationaryTime > 600000) { // 10 minutes
            const user = users[location.userId];
            newAlerts.push({
              id: `stationary_${rideId}`,
              type: 'warning',
              message: `${user?.name || 'Unknown rider'} has been stationary for more than 10 minutes`,
              rideId
            });
          }
        }
      }
    });
    
    setAlerts(newAlerts);
  };

  const showUpdateNotification = () => {
    setLastUpdate(new Date());
    setShowUpdateIndicator(true);
    
    if (updateIndicatorTimeout.current) {
      clearTimeout(updateIndicatorTimeout.current);
    }
    
    updateIndicatorTimeout.current = setTimeout(() => {
      setShowUpdateIndicator(false);
    }, 3000);
  };

  const cleanup = () => {
    if (unsubscribeRides.current) {
      unsubscribeRides.current();
    }
    
    Object.values(unsubscribeLocations.current).forEach(unsubscribe => {
      if (typeof unsubscribe === 'function') {
        unsubscribe();
      }
    });
    
    if (updateIndicatorTimeout.current) {
      clearTimeout(updateIndicatorTimeout.current);
    }
  };

  const handleMapLoad = useCallback((mapInstance) => {
    mapRef.current = mapInstance;
    console.log("Enhanced real-time rider map loaded", mapInstance);
  }, []);

  const getMarkerIcon = (location) => {
    const bearing = location.bearing || 0;
    const status = getLocationStatus(location.lastUpdate);
    
    let color = colors.info;
    if (status === 'offline') color = colors.danger;
    else if (status === 'delayed') color = colors.warning;
    else if (location.isMoving) color = colors.success;
    
    return {
      path: 'M0,-20 L-6,-10 L-3,-10 L-3,10 L3,10 L3,-10 L6,-10 Z',
      fillColor: color,
      fillOpacity: 1,
      strokeColor: colors.white,
      strokeWeight: 2,
      scale: 1.2,
      rotation: bearing,
      anchor: { x: 0, y: 0 }
    };
  };

  const formatSpeed = (speed) => {
    if (!speed || speed < 0) return '0 km/h';
    return `${(speed * 3.6).toFixed(1)} km/h`;
  };

  const formatDistance = (meters) => {
    if (meters < 1000) return `${meters.toFixed(0)} m`;
    return `${(meters / 1000).toFixed(2)} km`;
  };

  const formatDuration = (milliseconds) => {
    const hours = Math.floor(milliseconds / (1000 * 60 * 60));
    const minutes = Math.floor((milliseconds % (1000 * 60 * 60)) / (1000 * 60));
    return `${hours}h ${minutes}m`;
  };

  const formatLastUpdate = (date) => {
    if (!date) return 'Never';
    const now = new Date();
    const diffMs = now - date;
    const diffSecs = Math.floor(diffMs / 1000);
    
    if (diffSecs < 60) return `${diffSecs}s ago`;
    if (diffSecs < 3600) return `${Math.floor(diffSecs / 60)}m ago`;
    return date.toLocaleTimeString();
  };

  const filteredRiders = Object.entries(riderLocations).filter(([rideId, location]) => {
    const user = users[location.userId];
    const status = getLocationStatus(location.lastUpdate);
    
    // Filter by status
    if (filterStatus !== 'all' && status !== filterStatus) return false;
    
    // Filter by search term
    if (searchTerm && user?.name && !user.name.toLowerCase().includes(searchTerm.toLowerCase())) {
      return false;
    }
    
    return true;
  });

  const getHeatmapData = () => {
    return Object.values(riderLocations).map(location => ({
      location: new window.google.maps.LatLng(location.latitude, location.longitude),
      weight: location.speed || 1
    }));
  };

  if (loading) {
    return <LoadingMessage>🔄 Loading enhanced real-time rider tracking...</LoadingMessage>;
  }

  if (error) {
    return <ErrorMessage>❌ {error}</ErrorMessage>;
  }

  return (
    <RealTimeMapContainer>
      <Title>
        <LiveIndicator />
        Enhanced Real-Time Rider Tracking
      </Title>
      
      {/* Alerts Section */}
      {alerts.length > 0 && (
        <AlertsContainer>
          {alerts.map(alert => (
            <Alert key={alert.id} type={alert.type}>
              <span>⚠️</span>
              {alert.message}
            </Alert>
          ))}
        </AlertsContainer>
      )}
      
      {/* Enhanced Stats */}
      <StatsContainer>
        <StatCard>
          <StatValue color={colors.info}>{activeRides.length}</StatValue>
          <StatLabel>Active Rides</StatLabel>
          <StatTrend positive={activeRides.length > 0}>
            📈 {activeRides.length > 0 ? 'Active' : 'No rides'}
          </StatTrend>
        </StatCard>
        
        <StatCard>
          <StatValue color={colors.success}>
            {Object.keys(riderLocations).length}
          </StatValue>
          <StatLabel>Tracked Riders</StatLabel>
          <StatTrend positive={Object.keys(riderLocations).length > 0}>
            🎯 Real-time tracking
          </StatTrend>
        </StatCard>
        
        <StatCard>
          <StatValue color={colors.warning}>
            {filteredRiders.filter(([_, loc]) => getLocationStatus(loc.lastUpdate) === 'live').length}
          </StatValue>
          <StatLabel>Live Updates</StatLabel>
          <StatTrend positive={true}>
            🔴 Last 30 seconds
          </StatTrend>
        </StatCard>
        
        <StatCard>
          <StatValue color={colors.pineGreen}>
            {Object.values(riderLocations).length > 0 ? 
              (Object.values(riderLocations).reduce((avg, loc) => avg + (loc.speed || 0), 0) / 
               Object.keys(riderLocations).length * 3.6).toFixed(1) : '0'}
          </StatValue>
          <StatLabel>Avg Speed (km/h)</StatLabel>
          <StatTrend positive={true}>
            🚴‍♂️ Current average
          </StatTrend>
        </StatCard>
        
        <StatCard>
          <StatValue color={colors.purple}>
            {Object.values(riderStats).reduce((total, stat) => total + stat.distance, 0).toFixed(1)}
          </StatValue>
          <StatLabel>Total Distance (km)</StatLabel>
          <StatTrend positive={true}>
            📏 All active rides
          </StatTrend>
        </StatCard>
        
        <StatCard>
          <StatValue color={colors.teal}>
            {alerts.length}
          </StatValue>
          <StatLabel>Active Alerts</StatLabel>
          <StatTrend positive={alerts.length === 0}>
            {alerts.length === 0 ? '✅ All good' : '⚠️ Needs attention'}
          </StatTrend>
        </StatCard>
      </StatsContainer>
      
      {/* Enhanced Controls */}
      <ControlsContainer>
        <ToggleButton 
          active={showRoutes} 
          onClick={() => setShowRoutes(!showRoutes)}
        >
          🛣️ {showRoutes ? 'Hide' : 'Show'} Routes
        </ToggleButton>
        
        <ToggleButton 
          active={showHeatmap} 
          onClick={() => setShowHeatmap(!showHeatmap)}
        >
          🔥 {showHeatmap ? 'Hide' : 'Show'} Heatmap
        </ToggleButton>
        
        <ToggleButton 
          active={autoFollow} 
          onClick={() => setAutoFollow(!autoFollow)}
        >
          🎯 Auto Follow
        </ToggleButton>
        
        <FilterSelect 
          value={filterStatus} 
          onChange={(e) => setFilterStatus(e.target.value)}
        >
          <option value="all">All Riders</option>
          <option value="live">Live Only</option>
          <option value="delayed">Delayed</option>
          <option value="offline">Offline</option>
        </FilterSelect>
        
        <FilterSelect 
          value={mapType} 
          onChange={(e) => setMapType(e.target.value)}
        >
          <option value="roadmap">Roadmap</option>
          <option value="satellite">Satellite</option>
          <option value="hybrid">Hybrid</option>
          <option value="terrain">Terrain</option>
        </FilterSelect>
        
        <SearchInput
          type="text"
          placeholder="🔍 Search riders..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </ControlsContainer>
      
      <LastUpdateTime>
        🕒 Last updated: {lastUpdate.toLocaleTimeString()} | 
        📊 Tracking {filteredRiders.length} of {Object.keys(riderLocations).length} riders
      </LastUpdateTime>
      
      <MapWrapper>
        <MapContainer 
          center={mapCenter}
          zoom={14}
          onLoad={handleMapLoad}
          mapTypeId={mapType}
          style={{ height: '100%', width: '100%' }}
          options={{
            styles: mapType === 'roadmap' ? [
              {
                featureType: 'poi',
                elementType: 'labels',
                stylers: [{ visibility: 'off' }]
              }
            ] : undefined
          }}
        >
          {/* Enhanced rider location markers */}
          {filteredRiders.map(([rideId, location]) => (
            <Marker
              key={`rider_${rideId}`}
              position={{ lat: location.latitude, lng: location.longitude }}
              onClick={() => setSelectedRider(rideId)}
              icon={getMarkerIcon(location)}
              title={`${users[location.userId]?.name || 'Unknown'} - ${formatSpeed(location.speed)}`}
            />
          ))}
          
          {/* Enhanced route polylines with gradient colors */}
          {showRoutes && Object.entries(riderRoutes).map(([rideId, route]) => {
            if (!filteredRiders.find(([id]) => id === rideId)) return null;
            
            return (
              <Polyline
                key={`route_${rideId}`}
                path={route}
                options={{
                  strokeColor: getLocationStatus(riderLocations[rideId]?.lastUpdate) === 'live' ? colors.success : colors.info,
                  strokeOpacity: 0.8,
                  strokeWeight: 4,
                  geodesic: true,
                  icons: [{
                    icon: {
                      path: 'M 0,-1 0,1',
                      strokeOpacity: 1,
                      scale: 4
                    },
                    offset: '0',
                    repeat: '20px'
                  }]
                }}
              />
            );
          })}
          
          {/* Heatmap layer */}
          {showHeatmap && window.google && window.google.maps.visualization && (
            <HeatmapLayer
              data={getHeatmapData()}
              options={{
                radius: 50,
                opacity: 0.6
              }}
            />
          )}
          
          {/* Accuracy circles for selected rider */}
          {selectedRider && riderLocations[selectedRider] && riderLocations[selectedRider].accuracy > 0 && (
            <Circle
              center={{ 
                lat: riderLocations[selectedRider].latitude, 
                lng: riderLocations[selectedRider].longitude 
              }}
              radius={riderLocations[selectedRider].accuracy}
              options={{
                fillColor: colors.info,
                fillOpacity: 0.1,
                strokeColor: colors.info,
                strokeOpacity: 0.3,
                strokeWeight: 1
              }}
            />
          )}
          
          {/* Enhanced selected rider info window */}
          {selectedRider && riderLocations[selectedRider] && (
            <InfoWindow
              position={{ 
                lat: riderLocations[selectedRider].latitude, 
                lng: riderLocations[selectedRider].longitude 
              }}
              onCloseClick={() => setSelectedRider(null)}
            >
              <InfoWindowContent>
                <RiderTitle>
                  👤 {users[riderLocations[selectedRider].userId]?.name || 'Unknown Rider'}
                  <StatusBadge status={getLocationStatus(riderLocations[selectedRider].lastUpdate)}>
                    {getLocationStatus(riderLocations[selectedRider].lastUpdate).toUpperCase()}
                  </StatusBadge>
                </RiderTitle>
                
                <RiderDetail>
                  <DetailLabel>📱 Ride ID:</DetailLabel>
                  <DetailValue>{selectedRider}</DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>🚲 Bike ID:</DetailLabel>
                  <DetailValue>{riderLocations[selectedRider].bikeId}</DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>⚡ Speed:</DetailLabel>
                  <DetailValue color={riderLocations[selectedRider].isMoving ? colors.success : colors.mediumGray}>
                    {formatSpeed(riderLocations[selectedRider].speed)}
                  </DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>🧭 Bearing:</DetailLabel>
                  <DetailValue>{Math.round(riderLocations[selectedRider].bearing)}°</DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>📍 Accuracy:</DetailLabel>
                  <DetailValue>{riderLocations[selectedRider].accuracy?.toFixed(1) || 'N/A'} m</DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>🔋 Battery:</DetailLabel>
                  <DetailValue color={
                    riderLocations[selectedRider].batteryLevel > 50 ? colors.success :
                    riderLocations[selectedRider].batteryLevel > 20 ? colors.warning : colors.danger
                  }>
                    {riderLocations[selectedRider].batteryLevel || 'N/A'}%
                  </DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>⏱️ Ride Time:</DetailLabel>
                  <DetailValue>
                    {riderStats[selectedRider] ? formatDuration(riderStats[selectedRider].rideTime) : 'N/A'}
                  </DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>📏 Distance:</DetailLabel>
                  <DetailValue>
                    {riderStats[selectedRider] ? formatDistance(riderStats[selectedRider].distance) : 'N/A'}
                  </DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>🕒 Last Update:</DetailLabel>
                  <DetailValue>{formatLastUpdate(riderLocations[selectedRider].lastUpdate)}</DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>📧 Contact:</DetailLabel>
                  <DetailValue>{users[riderLocations[selectedRider].userId]?.email || 'N/A'}</DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>📞 Phone:</DetailLabel>
                  <DetailValue>{users[riderLocations[selectedRider].userId]?.phoneNumber || 'N/A'}</DetailValue>
                </RiderDetail>
                
                <RiderDetail>
                  <DetailLabel>🌍 Coordinates:</DetailLabel>
                  <DetailValue style={{ fontSize: '11px' }}>
                    {riderLocations[selectedRider].latitude.toFixed(6)}, {riderLocations[selectedRider].longitude.toFixed(6)}
                  </DetailValue>
                </RiderDetail>
              </InfoWindowContent>
            </InfoWindow>
          )}
        </MapContainer>
      </MapWrapper>
      
      {/* Sidebar Toggle */}
      <SidebarToggle onClick={() => setSidebarOpen(!sidebarOpen)}>
        {sidebarOpen ? '✕' : '📋'}
      </SidebarToggle>
      
      {/* Rider List Sidebar */}
      <RiderListSidebar isOpen={sidebarOpen}>
        <h3>Active Riders ({filteredRiders.length})</h3>
        {filteredRiders.map(([rideId, location]) => {
          const user = users[location.userId];
          const status = getLocationStatus(location.lastUpdate);
          const stats = riderStats[rideId];
          
          return (
            <RiderListItem 
              key={rideId}
              onClick={() => {
                setSelectedRider(rideId);
                if (mapRef.current) {
                  mapRef.current.panTo({ lat: location.latitude, lng: location.longitude });
                  mapRef.current.setZoom(16);
                }
              }}
            >
              <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>
                {user?.name || 'Unknown'}
              </div>
              <div style={{ fontSize: '12px', color: colors.mediumGray }}>
                Speed: {formatSpeed(location.speed)} | 
                Status: <span style={{ 
                  color: status === 'live' ? colors.success : 
                        status === 'delayed' ? colors.warning : colors.danger 
                }}>
                  {status.toUpperCase()}
                </span>
              </div>
              {stats && (
                <div style={{ fontSize: '11px', color: colors.mediumGray, marginTop: '3px' }}>
                  Time: {formatDuration(stats.rideTime)} | 
                  Distance: {formatDistance(stats.distance)}
                </div>
              )}
            </RiderListItem>
          );
        })}
      </RiderListSidebar>
      
      {/* Enhanced update indicator */}
      {showUpdateIndicator && (
        <RefreshIndicator>
          <span role="img" aria-label="live">🔴</span> 
          Live update received - {filteredRiders.length} riders tracked
        </RefreshIndicator>
      )}
    </RealTimeMapContainer>
  );
};

export default RealTimeRiderMap; 