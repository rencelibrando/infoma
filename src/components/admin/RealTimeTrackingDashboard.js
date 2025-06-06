/* global google */
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { collection, query, where, onSnapshot, orderBy, limit, getDoc, doc, updateDoc, getDocs } from 'firebase/firestore';
import { ref, onValue, off, child, get } from 'firebase/database';
import { db, realtimeDb } from '../../firebase';
import styled from 'styled-components';
import { Marker, InfoWindow, Polyline, Circle, HeatmapLayer, GoogleMap, useJsApiLoader } from '@react-google-maps/api';
import MapContainer from '../MapContainer';
import { useDataContext } from '../../context/DataContext';
import { auth } from '../../firebase';

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
  const {
    bikes,
    allRides
  } = useDataContext();

  const [activeTab, setActiveTab] = useState('live');
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
  const [authError, setAuthError] = useState(null);
  
  const unsubscribeRefs = useRef([]);
  const realtimeListeners = useRef([]);

  // New state for ride history
  const [rideHistory, setRideHistory] = useState([]);
  const [selectedHistoryRide, setSelectedHistoryRide] = useState(null);
  const [historyFilters, setHistoryFilters] = useState({
    status: 'all',
    dateRange: 'week',
    userId: '',
    sortBy: 'startTime',
    sortOrder: 'desc'
  });
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);

  // Route modal state
  const [showRouteModal, setShowRouteModal] = useState(false);
  const [routeData, setRouteData] = useState(null);
  const [isLoadingRoute, setIsLoadingRoute] = useState(false);

  // Monitor authentication status
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged((user) => {
      if (user) {
        console.log('User is authenticated:', user.uid);
        setAuthError(null);
      } else {
        console.warn('User is not authenticated');
        setAuthError('User not authenticated');
      }
    });

    return () => unsubscribe();
  }, []);

  // Setup real-time listeners for active rides using Realtime Database
  useEffect(() => {
    const setupRealTimeListeners = () => {
      console.log('Setting up real-time listeners...');
      
      // Listen for active rides from Realtime Database
      const activeRidesRef = ref(realtimeDb, 'activeRides');
      const activeRidesListener = onValue(activeRidesRef, (snapshot) => {
        try {
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
          setAuthError(null); // Clear auth error on successful data fetch
        } catch (error) {
          console.error('Error processing active rides data:', error);
          setAuthError('Error loading active rides');
        }
      }, (error) => {
        console.error('Firebase permission error for active rides:', error);
        setAuthError('Permission denied for active rides');
      });

      // Listen for live locations from Realtime Database
      const liveLocationRef = ref(realtimeDb, 'liveLocation');
      const liveLocationListener = onValue(liveLocationRef, (snapshot) => {
        try {
          const liveLocationData = snapshot.val() || {};
          console.log('Live locations updated:', Object.keys(liveLocationData).length);
          setLiveLocations(liveLocationData);
          setAuthError(null); // Clear auth error on successful data fetch
        } catch (error) {
          console.error('Error processing live location data:', error);
          setAuthError('Error loading live locations');
        }
      }, (error) => {
        console.error('Firebase permission error for live locations:', error);
        setAuthError('Permission denied for live locations');
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

  const formatTime = (milliseconds) => {
    if (!milliseconds) return 'N/A';
    const hours = Math.floor(milliseconds / (1000 * 60 * 60));
    const minutes = Math.floor((milliseconds % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((milliseconds % (1000 * 60)) / 1000);
    
    if (hours > 0) {
      return `${hours}h ${minutes}m ${seconds}s`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds}s`;
    } else {
      return `${seconds}s`;
    }
  };

  // Format distance in kilometers
  const formatDistanceKm = (distanceInMeters) => {
    if (!distanceInMeters) return '0.0 km';
    return `${(distanceInMeters / 1000).toFixed(2)} km`;
  };

  // Format date and time
  const formatDateTime = (timestamp) => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  // Format speed
  const formatSpeed = (speedInKmH) => {
    if (!speedInKmH) return '0.0 km/h';
    return `${speedInKmH.toFixed(1)} km/h`;
  };

  // Fetch ride history
  const fetchRideHistory = async () => {
    setIsLoadingHistory(true);
    try {
      let ridesQuery = collection(db, 'rides');
      let queryConstraints = [];

      // Add status filter
      if (historyFilters.status !== 'all') {
        queryConstraints.push(where('status', '==', historyFilters.status));
      }

      // Add date range filter
      if (historyFilters.dateRange !== 'all') {
        const now = new Date();
        let startDate;

        switch (historyFilters.dateRange) {
          case 'today':
            startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
            break;
          case 'week':
            startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
            break;
          case 'month':
            startDate = new Date(now.getFullYear(), now.getMonth(), 1);
            break;
          case 'year':
            startDate = new Date(now.getFullYear(), 0, 1);
            break;
          default:
            startDate = null;
        }

        if (startDate) {
          queryConstraints.push(where('startTime', '>=', startDate.getTime()));
        }
      }

      // Add user filter
      if (historyFilters.userId) {
        queryConstraints.push(where('userId', '==', historyFilters.userId));
      }

      // Add sorting and limit
      queryConstraints.push(orderBy(historyFilters.sortBy, historyFilters.sortOrder));
      queryConstraints.push(limit(50));

      const q = query(ridesQuery, ...queryConstraints);
      const snapshot = await getDocs(q);

      const rides = await Promise.all(
        snapshot.docs.map(async (rideDoc) => {
          const rideData = rideDoc.data();
          
          // Fetch user data
          let userData = {};
          if (rideData.userId) {
            try {
              const userDoc = await getDoc(doc(db, 'users', rideData.userId));
              if (userDoc.exists()) {
                userData = userDoc.data();
              }
            } catch (error) {
              console.warn('Could not fetch user data:', error);
            }
          }

          // Calculate ride statistics
          const duration = rideData.endTime ? 
            (rideData.endTime - rideData.startTime) : 
            (Date.now() - rideData.startTime);

          let maxSpeed = 0;
          let averageSpeed = 0;
          let totalDistance = rideData.totalDistance || 0;

          if (rideData.path && rideData.path.length > 1) {
            const speeds = rideData.path
              .map(point => point.speed || 0)
              .filter(speed => speed > 0);
            
            if (speeds.length > 0) {
              maxSpeed = Math.max(...speeds);
              averageSpeed = speeds.reduce((sum, speed) => sum + speed, 0) / speeds.length;
            }

            // Calculate distance if not available
            if (!totalDistance && rideData.path.length > 1) {
              for (let i = 1; i < rideData.path.length; i++) {
                const prev = rideData.path[i - 1];
                const curr = rideData.path[i];
                totalDistance += getDistanceBetweenPoints(prev, curr) * 1000;
              }
            }
          }

          return {
            id: rideDoc.id,
            ...rideData,
            user: userData,
            duration,
            maxSpeed,
            averageSpeed,
            totalDistance,
            startLocation: rideData.path?.[0] || null,
            endLocation: rideData.path?.[rideData.path?.length - 1] || null
          };
        })
      );

      setRideHistory(rides);
    } catch (error) {
      console.error('Error fetching ride history:', error);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  // Helper function to calculate distance between two points
  const getDistanceBetweenPoints = (point1, point2) => {
    const R = 6371; // Earth's radius in km
    const dLat = (point2.latitude - point1.latitude) * Math.PI / 180;
    const dLon = (point2.longitude - point1.longitude) * Math.PI / 180;
    const a = 
      Math.sin(dLat/2) * Math.sin(dLat/2) +
      Math.cos(point1.latitude * Math.PI / 180) * Math.cos(point2.latitude * Math.PI / 180) * 
      Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
  };

  // Fetch ride history when tab changes or filters change
  useEffect(() => {
    if (activeTab === 'history') {
      fetchRideHistory();
    }
  }, [activeTab, historyFilters]);

  const handleRideHistoryClick = async (ride) => {
    try {
      setSelectedHistoryRide(ride);
      setShowRouteModal(true);
      setIsLoadingRoute(true);
      setRouteData(null);

      // Generate route data from ride information
      const routeSummary = {
        rideId: ride.id,
        userId: ride.userId,
        userName: ride.user?.fullName || ride.user?.displayName || ride.user?.name || 'Unknown User',
        userEmail: ride.user?.email || 'No email',
        userPhone: ride.user?.phoneNumber || ride.user?.phone || 'No phone',
        bikeId: ride.bikeId,
        status: ride.status,
        startTime: ride.startTime,
        endTime: ride.endTime,
        duration: ride.duration,
        totalDistance: ride.totalDistance || 0,
        maxSpeed: ride.maxSpeed || 0,
        averageSpeed: ride.averageSpeed || 0,
        path: ride.path || [],
        startLocation: ride.path?.[0] || null,
        endLocation: ride.path?.[ride.path?.length - 1] || null,
        statistics: {
          totalDistance: ride.totalDistance || 0,
          maxSpeed: ride.maxSpeed || 0,
          averageSpeed: ride.averageSpeed || 0,
          duration: ride.duration || 0,
          pointCount: ride.path?.length || 0,
          completionRate: ride.status === 'completed' ? 100 : 
                          ride.status === 'active' ? 50 : 0
        }
      };

      setRouteData(routeSummary);
    } catch (error) {
      console.error('Error loading route data:', error);
      alert('Unable to load route data for this ride.');
    } finally {
      setIsLoadingRoute(false);
    }
  };

  // Close route modal
  const closeRouteModal = () => {
    setShowRouteModal(false);
    setSelectedHistoryRide(null);
    setRouteData(null);
    setIsLoadingRoute(false);
  };

  // Get user initials for avatar
  const getUserInitials = (name) => {
    if (!name) return 'U';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  };

  // Route Modal Component
  const RouteModal = () => {
    const { isLoaded } = useJsApiLoader({
      id: 'google-map-script',
      googleMapsApiKey: process.env.REACT_APP_GOOGLE_MAPS_API_KEY || "AIzaSyASfb-LFSstZrbPUIgPn1rKOqNTFF6mhhk"
    });

    if (!showRouteModal || !selectedHistoryRide) return null;

    return (
      <div style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 2000,
        padding: '20px'
      }}>
        <div style={{
          backgroundColor: colors.white,
          borderRadius: '12px',
          padding: '24px',
          maxWidth: '95vw',
          maxHeight: '95vh',
          overflow: 'auto',
          position: 'relative',
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.3)'
        }}>
          {/* Close Button */}
          <button
            onClick={closeRouteModal}
            style={{
              position: 'absolute',
              top: '16px',
              right: '16px',
              background: colors.danger,
              color: 'white',
              border: 'none',
              borderRadius: '50%',
              width: '32px',
              height: '32px',
              cursor: 'pointer',
              fontSize: '16px',
              fontWeight: 'bold',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              zIndex: 2001
            }}
          >
            ×
          </button>

          {/* Modal Header */}
          <div style={{
            marginBottom: '20px',
            paddingRight: '40px'
          }}>
            <h2 style={{
              margin: '0 0 8px 0',
              color: colors.darkGray,
              fontSize: '24px',
              fontWeight: 'bold'
            }}>
              🗺️ Route Summary
            </h2>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              marginBottom: '12px'
            }}>
              <div style={{
                width: '40px',
                height: '40px',
                borderRadius: '50%',
                background: `linear-gradient(135deg, ${colors.pineGreen}, ${colors.lightPineGreen})`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white',
                fontWeight: 'bold',
                fontSize: '14px'
              }}>
                {getUserInitials(selectedHistoryRide.user?.fullName || selectedHistoryRide.user?.displayName || selectedHistoryRide.user?.name)}
              </div>
              <div>
                <div style={{ fontWeight: 'bold', fontSize: '16px', color: colors.darkGray }}>
                  {selectedHistoryRide.user?.fullName || selectedHistoryRide.user?.displayName || selectedHistoryRide.user?.name || 'Unknown User'}
                </div>
                <div style={{ fontSize: '14px', color: colors.mediumGray }}>
                  {selectedHistoryRide.user?.email || 'No email'}
                </div>
                {(selectedHistoryRide.user?.phoneNumber || selectedHistoryRide.user?.phone) && (
                  <div style={{ fontSize: '13px', color: colors.mediumGray, marginTop: '2px' }}>
                    📞 {selectedHistoryRide.user?.phoneNumber || selectedHistoryRide.user?.phone}
                  </div>
                )}
              </div>
            </div>
            <div style={{
              padding: '8px 12px',
              borderRadius: '20px',
              fontSize: '12px',
              fontWeight: 'bold',
              textTransform: 'uppercase',
              backgroundColor: selectedHistoryRide.status === 'completed' ? colors.success :
                            selectedHistoryRide.status === 'active' ? colors.warning :
                            selectedHistoryRide.status === 'cancelled' ? colors.danger : colors.mediumGray,
              color: 'white',
              display: 'inline-block'
            }}>
              {selectedHistoryRide.status === 'completed' ? '✅ Completed' :
               selectedHistoryRide.status === 'active' ? '🚴 Active' :
               selectedHistoryRide.status === 'cancelled' ? '❌ Cancelled' : selectedHistoryRide.status}
            </div>
          </div>

          {isLoadingRoute ? (
            <div style={{
              textAlign: 'center',
              padding: '60px',
              fontSize: '16px',
              color: colors.mediumGray
            }}>
              🔄 Loading route data...
            </div>
          ) : routeData ? (
            <>
              {/* Map Container */}
              <div style={{
                height: '450px',
                width: '800px',
                maxWidth: '100%',
                borderRadius: '12px',
                overflow: 'hidden',
                marginBottom: '20px',
                border: `2px solid ${colors.lightGray}`
              }}>
                {isLoaded && (
                  <GoogleMap
                    mapContainerStyle={{ width: '100%', height: '100%' }}
                    center={
                      routeData.startLocation ? 
                      { lat: routeData.startLocation.latitude, lng: routeData.startLocation.longitude } :
                      { lat: 14.5995, lng: 120.9842 }
                    }
                    zoom={14}
                    options={{
                      disableDefaultUI: false,
                      zoomControl: true,
                      streetViewControl: false,
                      mapTypeControl: true,
                      fullscreenControl: true
                    }}
                  >
                    {/* Route polyline */}
                    {routeData.path && routeData.path.length > 1 && (
                      <Polyline
                        path={routeData.path.map(point => ({
                          lat: point.latitude || point.lat,
                          lng: point.longitude || point.lng
                        }))}
                        options={{
                          strokeColor: colors.pineGreen,
                          strokeOpacity: 0.8,
                          strokeWeight: 4,
                          geodesic: true
                        }}
                      />
                    )}

                    {/* Start marker */}
                    {routeData.startLocation && (
                      <Marker
                        position={{
                          lat: routeData.startLocation.latitude || routeData.startLocation.lat,
                          lng: routeData.startLocation.longitude || routeData.startLocation.lng
                        }}
                        icon={{
                          url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                              <circle cx="12" cy="12" r="8" fill="${colors.success}" stroke="white" stroke-width="3"/>
                              <text x="12" y="16" text-anchor="middle" fill="white" font-size="10" font-weight="bold">S</text>
                            </svg>
                          `),
                          scaledSize: { width: 24, height: 24 }
                        }}
                        title="Start Location"
                      />
                    )}

                    {/* End marker */}
                    {routeData.endLocation && (
                      <Marker
                        position={{
                          lat: routeData.endLocation.latitude || routeData.endLocation.lat,
                          lng: routeData.endLocation.longitude || routeData.endLocation.lng
                        }}
                        icon={{
                          url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                              <circle cx="12" cy="12" r="8" fill="${colors.danger}" stroke="white" stroke-width="3"/>
                              <text x="12" y="16" text-anchor="middle" fill="white" font-size="10" font-weight="bold">E</text>
                            </svg>
                          `),
                          scaledSize: { width: 24, height: 24 }
                        }}
                        title="End Location"
                      />
                    )}
                  </GoogleMap>
                )}
              </div>

              {/* Route Statistics */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
                gap: '16px',
                marginBottom: '20px'
              }}>
                <div style={{
                  backgroundColor: colors.lightGray,
                  padding: '16px',
                  borderRadius: '8px',
                  textAlign: 'center'
                }}>
                  <div style={{
                    fontSize: '20px',
                    fontWeight: 'bold',
                    color: colors.pineGreen,
                    marginBottom: '4px'
                  }}>
                    {formatDistanceKm(routeData.statistics?.totalDistance || 0)}
                  </div>
                  <div style={{
                    fontSize: '12px',
                    color: colors.mediumGray,
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }}>
                    Total Distance
                  </div>
                </div>

                <div style={{
                  backgroundColor: colors.lightGray,
                  padding: '16px',
                  borderRadius: '8px',
                  textAlign: 'center'
                }}>
                  <div style={{
                    fontSize: '20px',
                    fontWeight: 'bold',
                    color: colors.info,
                    marginBottom: '4px'
                  }}>
                    {formatTime(routeData.statistics?.duration || 0)}
                  </div>
                  <div style={{
                    fontSize: '12px',
                    color: colors.mediumGray,
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }}>
                    Duration
                  </div>
                </div>

                <div style={{
                  backgroundColor: colors.lightGray,
                  padding: '16px',
                  borderRadius: '8px',
                  textAlign: 'center'
                }}>
                  <div style={{
                    fontSize: '20px',
                    fontWeight: 'bold',
                    color: colors.warning,
                    marginBottom: '4px'
                  }}>
                    {(routeData.statistics?.maxSpeed || 0).toFixed(1)} km/h
                  </div>
                  <div style={{
                    fontSize: '12px',
                    color: colors.mediumGray,
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }}>
                    Max Speed
                  </div>
                </div>

                <div style={{
                  backgroundColor: colors.lightGray,
                  padding: '16px',
                  borderRadius: '8px',
                  textAlign: 'center'
                }}>
                  <div style={{
                    fontSize: '20px',
                    fontWeight: 'bold',
                    color: colors.success,
                    marginBottom: '4px'
                  }}>
                    {(routeData.statistics?.averageSpeed || 0).toFixed(1)} km/h
                  </div>
                  <div style={{
                    fontSize: '12px',
                    color: colors.mediumGray,
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }}>
                    Avg Speed
                  </div>
                </div>

                <div style={{
                  backgroundColor: colors.lightGray,
                  padding: '16px',
                  borderRadius: '8px',
                  textAlign: 'center'
                }}>
                  <div style={{
                    fontSize: '20px',
                    fontWeight: 'bold',
                    color: colors.purple,
                    marginBottom: '4px'
                  }}>
                    {routeData.statistics?.pointCount || 0}
                  </div>
                  <div style={{
                    fontSize: '12px',
                    color: colors.mediumGray,
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }}>
                    GPS Points
                  </div>
                </div>

                <div style={{
                  backgroundColor: colors.lightGray,
                  padding: '16px',
                  borderRadius: '8px',
                  textAlign: 'center'
                }}>
                  <div style={{
                    fontSize: '20px',
                    fontWeight: 'bold',
                    color: colors.teal,
                    marginBottom: '4px'
                  }}>
                    {routeData.statistics?.completionRate || 0}%
                  </div>
                  <div style={{
                    fontSize: '12px',
                    color: colors.mediumGray,
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }}>
                    Completion
                  </div>
                </div>
              </div>

              {/* Ride Details */}
              <div style={{
                backgroundColor: '#f8f9fa',
                padding: '16px',
                borderRadius: '8px',
                marginBottom: '16px'
              }}>
                <h4 style={{
                  margin: '0 0 12px 0',
                  color: colors.darkGray,
                  fontSize: '16px'
                }}>
                  📋 Ride Details
                </h4>
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '12px',
                  fontSize: '14px'
                }}>
                  <div>
                    <strong>👤 User Name:</strong> {routeData.userName || 'Unknown User'}
                  </div>
                  <div>
                    <strong>📧 Email:</strong> {routeData.userEmail || 'Not provided'}
                  </div>
                  <div>
                    <strong>📞 Phone:</strong> {routeData.userPhone || 'Not provided'}
                  </div>
                  <div>
                    <strong>🆔 Ride ID:</strong> {routeData.rideId}
                  </div>
                  <div>
                    <strong>🚲 Bike ID:</strong> {routeData.bikeId}
                  </div>
                  <div>
                    <strong>👤 User ID:</strong> {routeData.userId}
                  </div>
                  <div>
                    <strong>🏁 Start Time:</strong> {formatDateTime(routeData.startTime)}
                  </div>
                  <div>
                    <strong>🏁 End Time:</strong> {
                      routeData.endTime ? formatDateTime(routeData.endTime) : 
                      routeData.status === 'active' ? 'In Progress' : 'Not completed'
                    }
                  </div>
                </div>
              </div>

              {/* Location Details */}
              {(routeData.startLocation || routeData.endLocation) && (
                <div style={{
                  backgroundColor: '#f0f8f0',
                  padding: '16px',
                  borderRadius: '8px'
                }}>
                  <h4 style={{
                    margin: '0 0 12px 0',
                    color: colors.darkGray,
                    fontSize: '16px'
                  }}>
                    📍 Location Information
                  </h4>
                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
                    gap: '12px',
                    fontSize: '14px'
                  }}>
                    {routeData.startLocation && (
                      <div>
                        <strong>🟢 Start Location:</strong><br/>
                        {(routeData.startLocation.latitude || routeData.startLocation.lat)?.toFixed(6)}, {(routeData.startLocation.longitude || routeData.startLocation.lng)?.toFixed(6)}
                      </div>
                    )}
                    {routeData.endLocation && (
                      <div>
                        <strong>🔴 End Location:</strong><br/>
                        {(routeData.endLocation.latitude || routeData.endLocation.lat)?.toFixed(6)}, {(routeData.endLocation.longitude || routeData.endLocation.lng)?.toFixed(6)}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </>
          ) : (
            <div style={{
              textAlign: 'center',
              padding: '60px',
              fontSize: '16px',
              color: colors.mediumGray
            }}>
              ❌ No route data available for this ride.
            </div>
          )}
        </div>
      </div>
    );
  };

  const renderRideHistoryContent = () => (
    <>
      {/* Enhanced Header Section */}
      <div style={{
        background: 'linear-gradient(135deg, #1D3C34 0%, #2D5A4C 100%)',
        padding: '20px',
        borderRadius: '12px',
        marginBottom: '20px',
        color: 'white',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{
          position: 'absolute',
          top: '-20px',
          right: '-20px',
          width: '100px',
          height: '100px',
          background: 'rgba(255, 255, 255, 0.1)',
          borderRadius: '50%',
          opacity: 0.3
        }}></div>
        <div style={{
          position: 'absolute',
          bottom: '-30px',
          left: '-30px',
          width: '120px',
          height: '120px',
          background: 'rgba(255, 255, 255, 0.05)',
          borderRadius: '50%',
          opacity: 0.5
        }}></div>
        
        <div style={{ position: 'relative', zIndex: 2 }}>
          <h2 style={{ 
            margin: '0 0 10px 0', 
            fontSize: '24px', 
            fontWeight: '600',
            display: 'flex',
            alignItems: 'center',
            gap: '10px'
          }}>
            📊 Ride History Analytics
          </h2>
          <p style={{ 
            margin: 0, 
            opacity: 0.9, 
            fontSize: '14px' 
          }}>
            Track and analyze historical ride data with advanced filters and detailed insights
          </p>
        </div>
      </div>

      {/* Enhanced Filter Section */}
      <div style={{
        background: 'white',
        padding: '20px',
        borderRadius: '12px',
        marginBottom: '20px',
        border: '1px solid #e0e0e0',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.05)'
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '15px',
          marginBottom: '15px',
          flexWrap: 'wrap'
        }}>
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '8px',
            fontWeight: '600',
            color: colors.darkGray,
            fontSize: '16px'
          }}>
            🔍 Filters
          </div>
          
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
            <select
              value={historyFilters.status}
              onChange={(e) => setHistoryFilters(prev => ({ ...prev, status: e.target.value }))}
              style={{
                padding: '8px 12px',
                border: '2px solid #e0e0e0',
                borderRadius: '8px',
                fontSize: '14px',
                backgroundColor: 'white',
                cursor: 'pointer',
                transition: 'border-color 0.2s ease',
                minWidth: '120px'
              }}
              onFocus={(e) => e.target.style.borderColor = colors.pineGreen}
              onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
            >
              <option value="all">📋 All Status</option>
              <option value="completed">✅ Completed</option>
              <option value="active">🚴 Active</option>
              <option value="cancelled">❌ Cancelled</option>
            </select>

            <select
              value={historyFilters.dateRange}
              onChange={(e) => setHistoryFilters(prev => ({ ...prev, dateRange: e.target.value }))}
              style={{
                padding: '8px 12px',
                border: '2px solid #e0e0e0',
                borderRadius: '8px',
                fontSize: '14px',
                backgroundColor: 'white',
                cursor: 'pointer',
                transition: 'border-color 0.2s ease',
                minWidth: '120px'
              }}
              onFocus={(e) => e.target.style.borderColor = colors.pineGreen}
              onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
            >
              <option value="all">📅 All Time</option>
              <option value="today">📆 Today</option>
              <option value="week">📊 This Week</option>
              <option value="month">🗓️ This Month</option>
              <option value="year">📈 This Year</option>
            </select>

            <input
              type="text"
              placeholder="🔍 Search by User ID..."
              value={historyFilters.userId}
              onChange={(e) => setHistoryFilters(prev => ({ ...prev, userId: e.target.value }))}
              style={{
                padding: '8px 12px',
                border: '2px solid #e0e0e0',
                borderRadius: '8px',
                fontSize: '14px',
                backgroundColor: 'white',
                transition: 'border-color 0.2s ease',
                minWidth: '200px'
              }}
              onFocus={(e) => e.target.style.borderColor = colors.pineGreen}
              onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
            />

            <button
              onClick={fetchRideHistory}
              disabled={isLoadingHistory}
              style={{
                padding: '8px 16px',
                backgroundColor: colors.pineGreen,
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                fontSize: '14px',
                fontWeight: '600',
                cursor: isLoadingHistory ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s ease',
                opacity: isLoadingHistory ? 0.7 : 1,
                display: 'flex',
                alignItems: 'center',
                gap: '5px'
              }}
              onMouseEnter={(e) => {
                if (!isLoadingHistory) {
                  e.target.style.backgroundColor = colors.lightPineGreen;
                  e.target.style.transform = 'translateY(-1px)';
                }
              }}
              onMouseLeave={(e) => {
                e.target.style.backgroundColor = colors.pineGreen;
                e.target.style.transform = 'translateY(0)';
              }}
            >
              {isLoadingHistory ? '⏳ Loading...' : '🔄 Refresh'}
            </button>
          </div>
        </div>

        {/* Statistics Summary */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
          gap: '15px',
          marginTop: '15px',
          padding: '15px',
          backgroundColor: '#f8f9fa',
          borderRadius: '8px',
          border: '1px solid #e9ecef'
        }}>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '20px', fontWeight: 'bold', color: colors.pineGreen }}>
              {rideHistory.length}
            </div>
            <div style={{ fontSize: '12px', color: colors.mediumGray }}>Total Rides</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '20px', fontWeight: 'bold', color: colors.info }}>
              {rideHistory.filter(r => r.status === 'completed').length}
            </div>
            <div style={{ fontSize: '12px', color: colors.mediumGray }}>Completed</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '20px', fontWeight: 'bold', color: colors.warning }}>
              {rideHistory.filter(r => r.status === 'active').length}
            </div>
            <div style={{ fontSize: '12px', color: colors.mediumGray }}>Active</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '20px', fontWeight: 'bold', color: colors.success }}>
              {formatDistanceKm(rideHistory.reduce((sum, ride) => sum + (ride.totalDistance || 0), 0))}
            </div>
            <div style={{ fontSize: '12px', color: colors.mediumGray }}>Total Distance</div>
          </div>
        </div>
      </div>

      {/* Enhanced Rides List */}
      <div style={{
        background: 'white',
        borderRadius: '12px',
        border: '1px solid #e0e0e0',
        overflow: 'hidden',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.05)'
      }}>
        <div style={{
          padding: '20px',
          borderBottom: '1px solid #e9ecef',
          background: 'linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)'
        }}>
          <h3 style={{ 
            margin: 0, 
            color: colors.darkGray, 
            fontSize: '18px',
            fontWeight: '600',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            🚴 Ride Records
            <span style={{
              fontSize: '12px',
              fontWeight: 'normal',
              color: colors.mediumGray,
              background: 'white',
              padding: '2px 8px',
              borderRadius: '12px',
              border: '1px solid #dee2e6'
            }}>
              {rideHistory.length} rides
            </span>
          </h3>
        </div>

        <RidesList style={{ maxHeight: '600px', overflowY: 'auto' }}>
          {isLoadingHistory ? (
            <div style={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              padding: '40px',
              flexDirection: 'column',
              gap: '10px'
            }}>
              <div style={{
                width: '40px',
                height: '40px',
                border: '4px solid #f3f3f3',
                borderTop: '4px solid #1D3C34',
                borderRadius: '50%',
                animation: 'spin 1s linear infinite'
              }}></div>
              <div style={{ color: colors.mediumGray }}>Loading ride history...</div>
            </div>
          ) : rideHistory.length > 0 ? (
            rideHistory.map(ride => (
              <div
                key={ride.id}
                style={{
                  padding: '16px 20px',
                  borderBottom: '1px solid #f0f0f0',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  background: 'white',
                  position: 'relative'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = '#f8f9fa';
                  e.currentTarget.style.borderLeft = '4px solid #1D3C34';
                  e.currentTarget.style.paddingLeft = '16px';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = 'white';
                  e.currentTarget.style.borderLeft = 'none';
                  e.currentTarget.style.paddingLeft = '20px';
                }}
                onClick={() => handleRideHistoryClick(ride)}
              >
                {/* Ride Header */}
                <div style={{ 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  alignItems: 'flex-start',
                  marginBottom: '12px'
                }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ 
                      display: 'flex', 
                      alignItems: 'center', 
                      gap: '10px',
                      marginBottom: '4px'
                    }}>
                      <div style={{
                        width: '32px',
                        height: '32px',
                        borderRadius: '50%',
                        background: `linear-gradient(135deg, ${colors.pineGreen}, ${colors.lightPineGreen})`,
                        color: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '14px',
                        fontWeight: 'bold'
                      }}>
                        {getUserInitials(ride.user?.fullName || ride.user?.displayName || ride.user?.name)}
                      </div>
                      <div>
                        <div style={{ 
                          fontWeight: '600', 
                          fontSize: '15px',
                          color: colors.darkGray
                        }}>
                          {ride.user?.fullName || ride.user?.displayName || ride.user?.name || 'Unknown User'}
                        </div>
                        <div style={{ 
                          fontSize: '12px', 
                          color: colors.mediumGray,
                          display: 'flex',
                          alignItems: 'center',
                          gap: '8px'
                        }}>
                          🚲 {ride.bikeId} • 🆔 {ride.id.substring(0, 8)}...
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <div style={{ 
                    display: 'flex', 
                    flexDirection: 'column', 
                    alignItems: 'flex-end',
                    gap: '6px'
                  }}>
                    <div style={{
                      padding: '4px 12px',
                      borderRadius: '20px',
                      fontSize: '11px',
                      fontWeight: 'bold',
                      textTransform: 'uppercase',
                      backgroundColor: ride.status === 'completed' ? colors.success :
                                    ride.status === 'active' ? colors.warning :
                                    ride.status === 'cancelled' ? colors.danger : colors.mediumGray,
                      color: 'white',
                      boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)'
                    }}>
                      {ride.status === 'completed' ? '✅ Completed' :
                       ride.status === 'active' ? '🚴 Active' :
                       ride.status === 'cancelled' ? '❌ Cancelled' : ride.status}
                    </div>
                    
                    <div style={{ 
                      fontSize: '11px', 
                      color: colors.mediumGray,
                      textAlign: 'right'
                    }}>
                      {formatDateTime(ride.startTime)}
                    </div>
                  </div>
                </div>

                {/* Ride Stats Grid */}
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
                  gap: '12px',
                  marginBottom: '12px'
                }}>
                  <div style={{
                    background: '#f8f9fa',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    textAlign: 'center',
                    border: '1px solid #e9ecef'
                  }}>
                    <div style={{ 
                      fontSize: '16px', 
                      fontWeight: 'bold', 
                      color: colors.info,
                      marginBottom: '2px'
                    }}>
                      {ride.duration ? formatTime(ride.duration) : 'N/A'}
                    </div>
                    <div style={{ fontSize: '10px', color: colors.mediumGray }}>⏱️ Duration</div>
                  </div>
                  
                  <div style={{
                    background: '#f8f9fa',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    textAlign: 'center',
                    border: '1px solid #e9ecef'
                  }}>
                    <div style={{ 
                      fontSize: '16px', 
                      fontWeight: 'bold', 
                      color: colors.success,
                      marginBottom: '2px'
                    }}>
                      {formatDistanceKm(ride.totalDistance || 0)}
                    </div>
                    <div style={{ fontSize: '10px', color: colors.mediumGray }}>📍 Distance</div>
                  </div>
                  
                  <div style={{
                    background: '#f8f9fa',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    textAlign: 'center',
                    border: '1px solid #e9ecef'
                  }}>
                    <div style={{ 
                      fontSize: '16px', 
                      fontWeight: 'bold', 
                      color: colors.purple,
                      marginBottom: '2px'
                    }}>
                      {formatSpeed(ride.averageSpeed || 0)}
                    </div>
                    <div style={{ fontSize: '10px', color: colors.mediumGray }}>🎯 Avg Speed</div>
                  </div>
                  
                  <div style={{
                    background: '#f8f9fa',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    textAlign: 'center',
                    border: '1px solid #e9ecef'
                  }}>
                    <div style={{ 
                      fontSize: '16px', 
                      fontWeight: 'bold', 
                      color: colors.accent,
                      marginBottom: '2px'
                    }}>
                      ₱{(ride.cost || 0).toFixed(2)}
                    </div>
                    <div style={{ fontSize: '10px', color: colors.mediumGray }}>💰 Cost</div>
                  </div>
                </div>

                {/* Route Availability Indicator */}
                <div style={{ 
                  display: 'flex', 
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  fontSize: '12px'
                }}>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px'
                  }}>
                    {ride.path && ride.path.length > 0 ? (
                      <>
                        <div style={{
                          width: '8px',
                          height: '8px',
                          borderRadius: '50%',
                          backgroundColor: colors.success,
                          animation: 'pulse 2s infinite'
                        }}></div>
                        <span style={{ color: colors.success, fontWeight: '600' }}>
                          ✅ Route Available ({ride.path.length} points)
                        </span>
                      </>
                    ) : (
                      <>
                        <div style={{
                          width: '8px',
                          height: '8px',
                          borderRadius: '50%',
                          backgroundColor: colors.warning
                        }}></div>
                        <span style={{ color: colors.warning, fontWeight: '600' }}>
                          ⚠️ No Route Data
                        </span>
                      </>
                    )}
                  </div>
                  
                  <div style={{
                    background: 'linear-gradient(135deg, #1D3C34, #2D5A4C)',
                    color: 'white',
                    padding: '4px 8px',
                    borderRadius: '4px',
                    fontSize: '10px',
                    fontWeight: 'bold'
                  }}>
                    👁️ View Details
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div style={{
              textAlign: 'center',
              padding: '60px 20px',
              color: colors.mediumGray
            }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>📭</div>
              <div style={{ fontSize: '18px', fontWeight: '600', marginBottom: '8px' }}>
                No rides found
              </div>
              <div style={{ fontSize: '14px' }}>
                Try adjusting your filters or check back later
              </div>
            </div>
          )}
        </RidesList>
      </div>

      {/* Route Modal */}
      <RouteModal />
    </>
  );

  return (
    <DashboardContainer>
      <Title>
        <LiveIndicator />
        Real-Time Tracking Dashboard
        {authError && (
          <span style={{ 
            marginLeft: '15px',
            padding: '4px 8px',
            backgroundColor: colors.danger,
            color: 'white',
            borderRadius: '4px',
            fontSize: '12px',
            fontWeight: 'normal'
          }}>
            ⚠️ Auth Issue
          </span>
        )}
      </Title>

      {/* Tab Navigation */}
      <div style={{
        background: colors.white,
        borderRadius: '12px',
        boxShadow: '0 4px 20px rgba(0, 0, 0, 0.08)',
        marginBottom: '20px',
        overflow: 'hidden'
      }}>
        <div style={{
          display: 'flex',
          borderBottom: `2px solid ${colors.lightGray}`
        }}>
          <button
            onClick={() => setActiveTab('live')}
            style={{
              background: activeTab === 'live' ? 
                `linear-gradient(135deg, ${colors.pineGreen} 0%, ${colors.lightPineGreen} 100%)` : 
                'transparent',
              color: activeTab === 'live' ? colors.white : colors.darkGray,
              border: 'none',
              padding: '15px 25px',
              cursor: 'pointer',
              fontSize: '16px',
              fontWeight: '600',
              transition: 'all 0.2s ease',
              position: 'relative'
            }}
          >
            📍 Live Tracking
          </button>
          <button
            onClick={() => setActiveTab('history')}
            style={{
              background: activeTab === 'history' ? 
                `linear-gradient(135deg, ${colors.pineGreen} 0%, ${colors.lightPineGreen} 100%)` : 
                'transparent',
              color: activeTab === 'history' ? colors.white : colors.darkGray,
              border: 'none',
              padding: '15px 25px',
              cursor: 'pointer',
              fontSize: '16px',
              fontWeight: '600',
              transition: 'all 0.2s ease',
              position: 'relative'
            }}
          >
            🕐 Ride History
          </button>
        </div>

        <div style={{ padding: activeTab === 'live' ? '20px' : '0' }}>
          {activeTab === 'live' ? (
            <>
              {/* Authentication Error Banner */}
              {authError && (
                <div style={{
                  backgroundColor: '#ffebee',
                  border: `2px solid ${colors.danger}`,
                  borderRadius: '8px',
                  padding: '15px',
                  marginBottom: '20px',
                  textAlign: 'center'
                }}>
                  <div style={{ color: colors.danger, fontWeight: 'bold', marginBottom: '10px' }}>
                    🚨 Authentication Issue Detected
                  </div>
                  <div style={{ color: colors.mediumGray, fontSize: '14px', marginBottom: '15px' }}>
                    {authError} - Some data may not be available.
                  </div>
                  <button
                    onClick={() => {
                      console.log('Retrying data fetch...');
                      setAuthError(null);
                      window.location.reload();
                    }}
                    style={{
                      backgroundColor: colors.danger,
                      color: 'white',
                      border: 'none',
                      padding: '8px 16px',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontWeight: 'bold'
                    }}
                  >
                    🔄 Reload Dashboard
                  </button>
                </div>
              )}

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
                            backgroundColor: colors.danger,
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
                                {selectedRide.duration ? formatTime(selectedRide.duration) : formatDuration(selectedRide.startTime)}
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
                          Bike: {ride.bikeId} • Duration: {ride.duration ? formatTime(ride.duration) : formatDuration(ride.startTime)}
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
            </>
          ) : (
            <div style={{ padding: '20px' }}>
              {renderRideHistoryContent()}
            </div>
          )}
        </div>
      </div>
      
      {/* Route Modal for Ride History */}
      {showRouteModal && <RouteModal />}
    </DashboardContainer>
  );
};

export default RealTimeTrackingDashboard; 