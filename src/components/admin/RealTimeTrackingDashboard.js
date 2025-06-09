/* global google */
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { collection, query, where, onSnapshot, orderBy, limit, getDoc, doc, updateDoc, getDocs } from 'firebase/firestore';
import { ref, onValue, off, child, get } from 'firebase/database';
import { db, realtimeDb } from '../../firebase';
import styled from 'styled-components';
import { Marker, InfoWindow, Polyline, Circle, GoogleMap, useJsApiLoader } from '@react-google-maps/api';
import MapContainer from '../MapContainer';
import { useDataContext } from '../../context/DataContext';
import { auth } from '../../firebase';

// Add CSS for pulsing animations
const pulseStyles = `
  @keyframes liveLocationPulse {
    0% {
      transform: scale(1);
      opacity: 0.8;
    }
    50% {
      transform: scale(1.1);
      opacity: 0.5;
    }
    100% {
      transform: scale(1);
      opacity: 0.8;
    }
  }
  
  @keyframes ripple {
    0% {
      transform: scale(0.8);
      opacity: 1;
    }
    100% {
      transform: scale(2.5);
      opacity: 0;
    }
  }
  
  .live-pulse-circle {
    animation: liveLocationPulse 2s ease-in-out infinite;
  }
  
  .live-ripple-circle {
    animation: ripple 3s ease-out infinite;
  }
`;

// Inject styles into document head
if (typeof document !== 'undefined') {
  const existingStyle = document.getElementById('live-location-styles');
  if (!existingStyle) {
    const style = document.createElement('style');
    style.id = 'live-location-styles';
    style.textContent = pulseStyles;
    document.head.appendChild(style);
  }
}

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

// Add CSS animations for InfoWindow visualizations
const injectAnimationStyles = () => {
  // Check if animations are already injected
  if (document.getElementById('live-tracking-animations')) return;
  
  const style = document.createElement('style');
  style.id = 'live-tracking-animations';
  style.textContent = `
    @keyframes liveLocationPulse {
      0%, 100% { 
        transform: scale(1); 
        opacity: 0.3; 
      }
      50% { 
        transform: scale(1.1); 
        opacity: 0.5; 
      }
    }
    
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    
    @keyframes ripple {
      0% {
        transform: scale(1);
        opacity: 0.6;
      }
      100% {
        transform: scale(2);
        opacity: 0;
      }
    }

    @keyframes circleHover {
      0% { transform: scale(1); opacity: 0.6; }
      100% { transform: scale(1.05); opacity: 0.8; }
    }

    /* Interactive circle styles */
    .clickable-circle {
      cursor: pointer !important;
      transition: all 0.2s ease;
    }

    .clickable-circle:hover {
      animation: circleHover 0.3s ease-in-out;
    }

    /* Map container pointer styles */
    .gm-style div[role="button"] {
      cursor: pointer !important;
    }

    /* Enhanced pulse animation for live circles */
    .live-pulse-circle {
      animation: liveLocationPulse 3s infinite ease-in-out;
    }

    .live-pulse-circle-fast {
      animation: liveLocationPulse 2s infinite ease-in-out;
    }

    .live-pulse-circle-slow {
      animation: liveLocationPulse 4s infinite ease-in-out;
    }
  `;
  document.head.appendChild(style);
};

const DashboardContainer = styled.div`
  padding: 15px; // Reduced from 20px
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: calc(100vh - 60px); // Reduced
`;

const Title = styled.h2`
  color: ${colors.pineGreen};
  margin-bottom: 15px; // Reduced from 20px
  display: flex;
  align-items: center;
  gap: 10px; // Reduced from 15px
  font-size: 22px; // Reduced from 24px
  font-weight: 700;
`;

const LiveIndicator = styled.div`
  width: 8px; // Reduced from 10px
  height: 8px;
  background: ${colors.success};
  border-radius: 50%;
  animation: pulse 2s infinite;
  
  @keyframes pulse {
    0% { transform: scale(1); opacity: 1; }
    50% { transform: scale(1.1); opacity: 0.7; }
    100% { transform: scale(1); opacity: 1; }
  }
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); // Reduced from 250px
  gap: 12px; // Reduced from 15px
  margin-bottom: 15px; // Reduced from 20px
`;

const StatCard = styled.div`
  background: ${colors.white};
  border-radius: 8px; // Reduced from 12px
  padding: 12px; // Reduced from 15px
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); // Reduced
  transition: all 0.2s ease;
  border-left: 4px solid ${props => props.color || colors.pineGreen};
  
  &:hover {
    transform: translateY(-1px); // Reduced from -2px
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  }
`;

const StatValue = styled.div`
  font-size: 20px; // Reduced from 24px
  font-weight: bold;
  color: ${props => props.color || colors.pineGreen};
  margin-bottom: 3px; // Reduced from 5px
`;

const StatLabel = styled.div`
  font-size: 11px; // Reduced from 12px
  color: ${colors.mediumGray};
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.5px;
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
  height: 500px; // Reduced from 600px
  border-radius: 8px; // Reduced from 12px
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  position: relative;
`;

const ControlsContainer = styled.div`
  background: ${colors.white};
  border-radius: 8px; // Reduced from 12px
  padding: 12px; // Reduced from 15px
  margin-bottom: 15px; // Reduced from 20px
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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
  border-radius: 8px; // Reduced from 12px
  padding: 12px; // Reduced from 15px
  margin-top: 15px; // Reduced from 20px
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  max-height: 400px; // Reduced from 500px
  overflow-y: auto;
`;

const RideCard = styled.div`
  border: 1px solid ${colors.lightGray};
  border-radius: 6px; // Reduced from 8px
  padding: 10px; // Reduced from 12px
  margin-bottom: 8px; // Reduced from 10px
  transition: all 0.2s ease;
  cursor: pointer;
  
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    border-color: ${colors.pineGreen};
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
  padding: 8px; // Reduced from 10px
  min-width: 250px; // Reduced from 300px
  max-width: 350px; // Reduced from 400px
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  
  h3 {
    margin: 0 0 8px 0; // Reduced from 10px
    font-size: 14px; // Reduced from 16px
    font-weight: 600;
  }
  
  p {
    margin: 3px 0; // Reduced from 5px
    font-size: 11px; // Reduced from 12px
    line-height: 1.3; // Reduced from 1.4
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
  
  // New state for tracking ride paths and trails
  const [ridePaths, setRidePaths] = useState({}); // Store complete paths for each ride
  const [rideTrails, setRideTrails] = useState({}); // Store trail data for display
  const [showMarkerClusters, setShowMarkerClusters] = useState(false);
  const [trailSettings, setTrailSettings] = useState({
    maxTrailLength: 50, // Maximum number of points in trail
    trailOpacity: 0.7,
    trailWeight: 3,
    fadeTrail: true
  });

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

  // Helper function to determine if a ride is truly active
  const isRideTrulyActive = (ride) => {
    const hasStarted = !!ride.startTime;
    const hasNotEnded = !ride.endTime;
    const endedStatuses = ["completed", "cancelled", "ended", "finished"];
    
    // More flexible status checking - handle case variations and missing status
    const statusIsActive = !ride.status || // No status - assume active for backward compatibility
                          ride.status.toLowerCase() === "active" || 
                          !endedStatuses.includes(ride.status.toLowerCase());
    
    const result = hasStarted && hasNotEnded && statusIsActive;
    
    // Debug logging for troubleshooting
    console.log(`🔍 Checking ride ${ride.userId}:`, {
      hasStarted,
      hasNotEnded, 
      statusIsActive,
      status: ride.status,
      startTime: ride.startTime,
      endTime: ride.endTime,
      result
    });
    
    return result;
  };

  // Predefined bike locations for Metro Manila
  const predefinedBikes = [
    {
      id: 'bike-001',
      name: 'Bambike Manila 01',
      latitude: 14.5995,
      longitude: 120.9842,
      isAvailable: true,
      maintenanceStatus: 'operational',
      batteryLevel: 85,
      lastLocationUpdate: new Date(Date.now() - 10 * 60 * 1000) // 10 minutes ago
    },
    {
      id: 'bike-002', 
      name: 'Bambike Makati 02',
      latitude: 14.5547,
      longitude: 121.0244,
      isAvailable: true,
      maintenanceStatus: 'operational',
      batteryLevel: 92,
      lastLocationUpdate: new Date(Date.now() - 5 * 60 * 1000) // 5 minutes ago
    },
    {
      id: 'bike-003',
      name: 'Bambike BGC 03', 
      latitude: 14.5515,
      longitude: 121.0473,
      isAvailable: false,
      maintenanceStatus: 'operational',
      batteryLevel: 67,
      lastLocationUpdate: new Date(Date.now() - 15 * 60 * 1000) // 15 minutes ago
    },
    {
      id: 'bike-004',
      name: 'Bambike Ortigas 04',
      latitude: 14.5866,
      longitude: 121.0635,
      isAvailable: true,
      maintenanceStatus: 'operational', 
      batteryLevel: 78,
      lastLocationUpdate: new Date(Date.now() - 8 * 60 * 1000) // 8 minutes ago
    },
    {
      id: 'bike-005',
      name: 'Bambike Quezon 05',
      latitude: 14.6760,
      longitude: 121.0437,
      isAvailable: true,
      maintenanceStatus: 'maintenance',
      batteryLevel: 45,
      lastLocationUpdate: new Date(Date.now() - 120 * 60 * 1000) // 2 hours ago
    },
    {
      id: 'bike-006',
      name: 'Bambike Pasig 06',
      latitude: 14.5764,
      longitude: 121.0851,
      isAvailable: false,
      maintenanceStatus: 'operational',
      batteryLevel: 23,
      lastLocationUpdate: new Date(Date.now() - 30 * 60 * 1000) // 30 minutes ago
    },
    {
      id: 'bike-007',
      name: 'Bambike Taguig 07', 
      latitude: 14.5176,
      longitude: 121.0509,
      isAvailable: true,
      maintenanceStatus: 'operational',
      batteryLevel: 89,
      lastLocationUpdate: new Date(Date.now() - 3 * 60 * 1000) // 3 minutes ago
    },
    {
      id: 'bike-008',
      name: 'Bambike Marikina 08',
      latitude: 14.6507,
      longitude: 121.1029,
      isAvailable: true,
      maintenanceStatus: 'operational',
      batteryLevel: 76,
      lastLocationUpdate: new Date(Date.now() - 12 * 60 * 1000) // 12 minutes ago
    },
    {
      id: 'bike-009',
      name: 'Bambike Paranaque 09',
      latitude: 14.4793,
      longitude: 121.0198,
      isAvailable: false,
      maintenanceStatus: 'maintenance',
      batteryLevel: 34,
      lastLocationUpdate: new Date(Date.now() - 240 * 60 * 1000) // 4 hours ago
    },
    {
      id: 'bike-010',
      name: 'Bambike Muntinlupa 10',
      latitude: 14.4063,
      longitude: 121.0346,
      isAvailable: true,
      maintenanceStatus: 'operational',
      batteryLevel: 91,
      lastLocationUpdate: new Date(Date.now() - 7 * 60 * 1000) // 7 minutes ago
    }
  ];

  // Combine real bikes data with predefined bikes for demonstration
  const allBikes = [...(bikes || []), ...predefinedBikes];

  // Add debugging for data availability (moved after state declarations)
  useEffect(() => {
    console.log('Dashboard - Bikes data:', bikes?.length || 0, bikes);
    console.log('Dashboard - Active rides:', activeRides?.length || 0, activeRides);
    console.log('Dashboard - Live locations:', Object.keys(liveLocations).length, liveLocations);
  }, [bikes, activeRides, liveLocations]);

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

  // Enhanced trail management
  const updateRideTrail = useCallback((userId, newLocation) => {
    if (!newLocation || !newLocation.latitude || !newLocation.longitude) return;

    setRidePaths(prev => {
      const currentPath = prev[userId] || [];
      const newPoint = {
        lat: parseFloat(newLocation.latitude),
        lng: parseFloat(newLocation.longitude),
        timestamp: newLocation.timestamp || Date.now(),
        speed: newLocation.speed || 0,
        bearing: newLocation.bearing || 0
      };

      // Add new point and limit trail length
      const updatedPath = [...currentPath, newPoint];
      if (updatedPath.length > trailSettings.maxTrailLength) {
        updatedPath.shift(); // Remove oldest point
      }

      return {
        ...prev,
        [userId]: updatedPath
      };
    });

    // Update trail display data
    setRideTrails(prev => {
      const path = prev[userId] || [];
      const newTrailPoint = {
        lat: parseFloat(newLocation.latitude),
        lng: parseFloat(newLocation.longitude),
        timestamp: Date.now()
      };

      const updatedTrail = [...path, newTrailPoint];
      if (updatedTrail.length > trailSettings.maxTrailLength) {
        updatedTrail.shift();
      }

      return {
        ...prev,
        [userId]: updatedTrail
      };
    });
  }, [trailSettings.maxTrailLength]);

  // Setup real-time listeners for active rides using Realtime Database
  // This effect handles real-time ride tracking and automatically removes ended rides
  useEffect(() => {
    // Inject CSS animations for InfoWindow visualizations
    injectAnimationStyles();
    
    const setupRealTimeListeners = () => {
      console.log('Setting up real-time listeners...');
      
      // Listen for active rides from Realtime Database
      const activeRidesRef = ref(realtimeDb, 'activeRides');
      const activeRidesListener = onValue(activeRidesRef, (snapshot) => {
        try {
          const activeRidesData = snapshot.val() || {};
          console.log('🔥 Raw activeRides data from Firebase:', activeRidesData);
          
          const rides = Object.entries(activeRidesData).map(([userId, rideData]) => {
            // Process and validate the ride data
            const processedRide = {
              ...rideData,
              userId: userId,
              id: rideData.rideId || rideData.id || userId
            };
            
            // Ensure startTime is properly formatted
            if (rideData.startTime) {
              // Handle different timestamp formats
              if (typeof rideData.startTime === 'string') {
                processedRide.startTime = new Date(rideData.startTime);
              } else if (typeof rideData.startTime === 'number') {
                // Handle both milliseconds and seconds timestamps
                const timestamp = rideData.startTime;
                processedRide.startTime = new Date(timestamp < 10000000000 ? timestamp * 1000 : timestamp);
              } else if (rideData.startTime && typeof rideData.startTime === 'object') {
                // Handle Firestore timestamp objects
                if (rideData.startTime.seconds) {
                  processedRide.startTime = new Date(rideData.startTime.seconds * 1000);
                } else if (rideData.startTime.toDate) {
                  processedRide.startTime = rideData.startTime.toDate();
                } else {
                  processedRide.startTime = new Date(rideData.startTime);
                }
              }
              
              // Validate the parsed date
              if (isNaN(processedRide.startTime.getTime())) {
                console.warn(`Invalid startTime for user ${userId}:`, rideData.startTime);
                processedRide.startTime = null;
              }
            } else {
              // If no startTime, try to use other timestamp fields
              const timestampFields = ['createdAt', 'timestamp', 'deviceTimestamp'];
              for (const field of timestampFields) {
                if (rideData[field]) {
                  try {
                    processedRide.startTime = new Date(rideData[field]);
                    if (!isNaN(processedRide.startTime.getTime())) {
                      break;
                    }
                  } catch (e) {
                    // Continue to next field
                  }
                }
              }
              
              // If still no valid startTime, set to null
              if (!processedRide.startTime || isNaN(processedRide.startTime.getTime())) {
                console.warn(`No valid timestamp found for user ${userId}`, rideData);
                processedRide.startTime = null;
              }
            }
            
            // Process endTime if it exists
            if (rideData.endTime) {
              if (typeof rideData.endTime === 'string') {
                processedRide.endTime = new Date(rideData.endTime);
              } else if (typeof rideData.endTime === 'number') {
                const timestamp = rideData.endTime;
                processedRide.endTime = new Date(timestamp < 10000000000 ? timestamp * 1000 : timestamp);
              } else if (rideData.endTime && typeof rideData.endTime === 'object') {
                if (rideData.endTime.seconds) {
                  processedRide.endTime = new Date(rideData.endTime.seconds * 1000);
                } else if (rideData.endTime.toDate) {
                  processedRide.endTime = rideData.endTime.toDate();
                } else {
                  processedRide.endTime = new Date(rideData.endTime);
                }
              }
              
              // Validate the parsed endTime
              if (isNaN(processedRide.endTime.getTime())) {
                console.warn(`Invalid endTime for user ${userId}:`, rideData.endTime);
                processedRide.endTime = null;
              }
            }
            
            return processedRide;
          });
          
          // TEMPORARILY DISABLED FILTERING FOR DEBUGGING - Show all rides
          console.log('⚠️ TEMPORARILY SHOWING ALL RIDES FOR DEBUGGING');
          const trulyActiveRides = rides; // Show all rides for now
          
          // Keep the filtering logic commented out for now
          // const trulyActiveRides = rides.filter(ride => {
          //   const isActuallyActive = isRideTrulyActive(ride);
          //   
          //   // Log rides that are being filtered out for debugging
          //   if (!isActuallyActive) {
          //     const reason = !ride.startTime ? 'no start time' :
          //                  ride.endTime ? 'has end time' :
          //                  ['completed', 'cancelled', 'ended', 'finished'].includes(ride.status?.toLowerCase()) ? 'ended status' :
          //                  'unknown reason';
          //     console.log(`🗑️ Filtering out ride ${ride.userId}: ${reason} (status: ${ride.status || 'none'}, endTime: ${!!ride.endTime})`);
          //   }
          //   
          //   return isActuallyActive;
          // });
          
          console.log(`🚴 Raw rides from DB: ${rides.length}, Truly active rides: ${trulyActiveRides.length}`);
          
          // Log summary of filtering if there's a difference
          if (rides.length !== trulyActiveRides.length) {
            const filteredCount = rides.length - trulyActiveRides.length;
            console.log(`✅ Filtered out ${filteredCount} ended/invalid rides from map display`);
          }
          
          // Debug timestamp processing
          trulyActiveRides.forEach(ride => {
            if (!ride.startTime) {
              console.warn(`Missing startTime for active ride ${ride.id}:`, ride);
            }
          });
          
          // Clean up trails for rides that are no longer active
          const activeUserIds = new Set(trulyActiveRides.map(ride => ride.userId));
          setRidePaths(prevPaths => {
            const cleanedPaths = {};
            Object.keys(prevPaths).forEach(userId => {
              if (activeUserIds.has(userId)) {
                cleanedPaths[userId] = prevPaths[userId];
              } else {
                console.log(`Removing trail for ended ride: ${userId}`);
              }
            });
            return cleanedPaths;
          });
          
          setRideTrails(prevTrails => {
            const cleanedTrails = {};
            Object.keys(prevTrails).forEach(userId => {
              if (activeUserIds.has(userId)) {
                cleanedTrails[userId] = prevTrails[userId];
              }
            });
            return cleanedTrails;
          });
          
          setActiveRides(trulyActiveRides);
          calculateStats(trulyActiveRides);
          checkForAlerts(trulyActiveRides);
          setAuthError(null); // Clear auth error on successful data fetch
          
          // Clean up live location data for users who are no longer in active rides
          setLiveLocations(prevLiveLocations => {
            const cleanedLiveLocations = {};
            const activeUserIds = new Set(trulyActiveRides.map(ride => ride.userId));
            
            Object.keys(prevLiveLocations).forEach(userId => {
              if (activeUserIds.has(userId)) {
                cleanedLiveLocations[userId] = prevLiveLocations[userId];
              } else {
                console.log(`Removing stale live location data for ended ride: ${userId}`);
              }
            });
            
            return cleanedLiveLocations;
          });
          
          // Clear selected ride if it's no longer active
          setSelectedRide(prevSelected => {
            if (prevSelected && prevSelected.userId && !prevSelected.isStaticBike) {
              const isStillActive = trulyActiveRides.some(ride => ride.userId === prevSelected.userId);
              if (!isStillActive) {
                console.log(`Clearing selected ride - ride ${prevSelected.userId} is no longer active`);
                return null;
              }
            }
            return prevSelected;
          });
        } catch (error) {
          console.error('Error processing active rides data:', error);
          setAuthError('Error loading active rides');
        }
              }, (error) => {
        console.error('🔥 Firebase error for active rides:', error);
        console.error('🔥 Error details:', error.code, error.message);
        setAuthError(`Firebase error: ${error.message}`);
      });

      // Listen for live locations from Realtime Database
      const liveLocationRef = ref(realtimeDb, 'liveLocation');
      const liveLocationListener = onValue(liveLocationRef, (snapshot) => {
        try {
          const liveLocationData = snapshot.val() || {};
          console.log('📍 Live locations updated:', Object.keys(liveLocationData).length, liveLocationData);
          
          // Update live locations with trail updates
          setLiveLocations(prevLocations => {
            const newLocations = { ...liveLocationData };
            
            // Update trails for each user with new location data
            Object.entries(newLocations).forEach(([userId, locationData]) => {
              if (locationData && locationData.latitude && locationData.longitude) {
                // Only update trail if location has actually changed
                const prevLocation = prevLocations[userId];
                if (!prevLocation || 
                    prevLocation.latitude !== locationData.latitude || 
                    prevLocation.longitude !== locationData.longitude) {
                  updateRideTrail(userId, locationData);
                }
              }
            });
            
            return newLocations;
          });
          
          setAuthError(null);
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
  }, [updateRideTrail]);

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
    console.log('🔥 handleRideClick called with:', ride);
    
    // Use unified position calculation for consistency
    const { position, locationStatus } = getUnifiedPosition(ride, 'activeRide');
    
    // Store the ride with position for InfoWindow consistency
    const rideWithPosition = {
      ...ride,
      position,
      locationStatus
    };
    
    setSelectedRide(rideWithPosition);
    
    // Center map on the calculated position
    setMapCenter(position);
      setMapZoom(16);
    
    console.log('🔥 selectedRide set with position:', rideWithPosition);
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

  // Consolidated marker icon function - replaces getMarkerIcon, getEnhancedMarkerIcon, and getEnhancedBikeIcon
  const getUnifiedMarkerIcon = (type, data, options = {}) => {
    const { locationStatus = 'unknown', size = 24 } = options;
    
    let baseColor, strokeColor;
    
    if (type === 'bike') {
      // Static bike icon logic - solid colors
      if (!data.isAvailable) {
        baseColor = colors.danger;
        strokeColor = '#8B0000';
      } else if (data.maintenanceStatus === 'maintenance') {
        baseColor = colors.warning;
        strokeColor = '#FF8C00';
      } else if (data.batteryLevel && data.batteryLevel < 20) {
        baseColor = '#FF6B35';
        strokeColor = '#CC5429';
      } else {
        baseColor = colors.success;
        strokeColor = '#008000';
      }
    } else if (type === 'activeRide') {
      // Active ride marker logic - solid colors
      if (data.status === 'emergency') {
        baseColor = colors.danger;
        strokeColor = '#8B0000';
      } else if (data.status === 'paused') {
        baseColor = colors.warning;
        strokeColor = '#FF8C00';
      } else {
        baseColor = colors.success;
        strokeColor = '#008000';
      }
    }
    
    return {
      url: `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(`
        <svg width="${size}" height="${size}" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <filter id="shadow" x="-50%" y="-50%" width="200%" height="200%">
              <feDropShadow dx="1" dy="1" stdDeviation="1" flood-color="rgba(0,0,0,0.3)"/>
            </filter>
          </defs>
          <!-- Solid background circle -->
          <circle cx="12" cy="12" r="10" 
                  fill="${baseColor}" 
                  stroke="${strokeColor}" 
                  stroke-width="2" 
                  filter="url(#shadow)"/>
          <!-- Icon letter -->
          <text x="12" y="16" text-anchor="middle" fill="white" font-size="12" font-weight="bold">
            ${type === 'bike' ? 'B' : 'U'}
          </text>
        </svg>
      `)}`,
      scaledSize: new google.maps.Size(size, size),
      anchor: new google.maps.Point(size/2, size/2)
    };
  };

  // Unified position calculation function
  const getUnifiedPosition = (data, type, fallbackOptions = {}) => {
    let position = null;
    let locationStatus = 'unknown';
    let locationAge = null;
    
    if (type === 'bike') {
      // Strategy 1: Use current latitude/longitude
      if (data.latitude && data.longitude && 
          !isNaN(parseFloat(data.latitude)) && !isNaN(parseFloat(data.longitude))) {
        position = {
          lat: parseFloat(data.latitude),
          lng: parseFloat(data.longitude)
        };
        
        if (data.lastLocationUpdate) {
          const lastUpdate = new Date(data.lastLocationUpdate);
          const now = new Date();
          locationAge = (now - lastUpdate) / (1000 * 60);
          
          locationStatus = locationAge < 60 ? 'recent' : 
                          locationAge < 24 * 60 ? 'stale' : 'old';
        } else {
          locationStatus = 'stale';
        }
      }
      // Strategy 2: Use last known location
      else if (data.lastKnownLocation?.latitude && data.lastKnownLocation?.longitude) {
        position = {
          lat: parseFloat(data.lastKnownLocation.latitude),
          lng: parseFloat(data.lastKnownLocation.longitude)
        };
        locationStatus = 'lastKnown';
        
        if (data.lastKnownLocation.timestamp) {
          const lastUpdate = new Date(data.lastKnownLocation.timestamp);
          locationAge = (Date.now() - lastUpdate.getTime()) / (1000 * 60);
        }
      }
      // Strategy 3: Use initial deployment location
      else if (data.initialLocation?.latitude && data.initialLocation?.longitude) {
        position = {
          lat: parseFloat(data.initialLocation.latitude),
          lng: parseFloat(data.initialLocation.longitude)
        };
        locationStatus = 'initial';
      }
    } else if (type === 'activeRide') {
      // For active rides, prioritize live location over stored location
      const liveLocation = liveLocations[data.userId];
      const rideLocation = data.currentLocation;
      const location = liveLocation || rideLocation;
      
      if (location?.latitude && location?.longitude) {
        position = {
          lat: parseFloat(location.latitude),
          lng: parseFloat(location.longitude)
        };
        locationStatus = liveLocation ? 'recent' : 'stale';
      }
    }
    
    // Fallback to default location if no valid position found
    if (!position) {
      const { lat = 14.5995, lng = 120.9842, addRandomOffset = true } = fallbackOptions;
      const offset = addRandomOffset ? (Math.random() - 0.5) * 0.01 : 0;
      
      position = {
        lat: lat + offset,
        lng: lng + offset
      };
      locationStatus = 'fallback';
    }
    
    return { position, locationStatus, locationAge };
  };

  // Get trail color based on ride status and speed
  const getTrailColor = (ride) => {
    const liveLocation = liveLocations[ride.userId];
    const speed = liveLocation?.speed || 0;
    
    if (ride.status === 'emergency') return colors.danger;
    if (ride.status === 'paused') return colors.warning;
    if (speed > 25) return colors.info;
    if (speed > 15) return colors.success;
    return colors.pineGreen;
  };

  // Clear trails for a specific ride
  const clearRideTrail = (userId) => {
    setRidePaths(prev => {
      const updated = { ...prev };
      delete updated[userId];
      return updated;
    });
    setRideTrails(prev => {
      const updated = { ...prev };
      delete updated[userId];
      return updated;
    });
  };

  // Clear all trails
  const clearAllTrails = () => {
    setRidePaths({});
    setRideTrails({});
  };

  const formatDuration = (startTime) => {
    // Handle null, undefined, or invalid startTime
    if (!startTime) {
      return 'N/A';
    }
    
    try {
      let start;
      
      // Handle different timestamp formats
      if (typeof startTime === 'object' && startTime.toDate) {
        // Firestore Timestamp object
        start = startTime.toDate();
      } else if (typeof startTime === 'object' && startTime.seconds) {
        // Firestore Timestamp-like object
        start = new Date(startTime.seconds * 1000);
      } else if (typeof startTime === 'number') {
        // Unix timestamp (milliseconds or seconds)
        start = startTime > 1000000000000 ? new Date(startTime) : new Date(startTime * 1000);
      } else if (typeof startTime === 'string') {
        // ISO string or other date string
        start = new Date(startTime);
      } else {
        // Fallback
        start = new Date(startTime);
      }
      
      // Validate the date
      if (isNaN(start.getTime())) {
        console.warn('Invalid startTime provided to formatDuration:', startTime);
        return 'Invalid time';
      }
      
      const duration = Date.now() - start.getTime();
      
      // Handle negative duration (future start time)
      if (duration < 0) {
        return 'Not started';
      }
      
      const hours = Math.floor(duration / (1000 * 60 * 60));
      const minutes = Math.floor((duration % (1000 * 60 * 60)) / (1000 * 60));
      
      // Handle very long durations
      if (hours > 999) {
        return '999h+';
      }
      
      return `${hours}h ${minutes}m`;
    } catch (error) {
      console.error('Error in formatDuration:', error, 'startTime:', startTime);
      return 'Error';
    }
  };

  const formatTime = (milliseconds) => {
    // Handle null, undefined, or invalid values
    if (!milliseconds || typeof milliseconds !== 'number' || isNaN(milliseconds)) {
      return 'N/A';
    }
    
    // Handle negative values
    if (milliseconds < 0) {
      return 'N/A';
    }
    
    const hours = Math.floor(milliseconds / (1000 * 60 * 60));
    const minutes = Math.floor((milliseconds % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((milliseconds % (1000 * 60)) / 1000);
    
    // Handle very long durations
    if (hours > 999) {
      return '999h+';
    }
    
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

          // Calculate ride statistics with improved accuracy and fallbacks
          const duration = rideData.endTime ? 
            (rideData.endTime - rideData.startTime) : 
            (Date.now() - rideData.startTime);

          let maxSpeed = rideData.maxSpeed || 0;
          let averageSpeed = rideData.averageSpeed || 0;
          let totalDistance = rideData.totalDistance || rideData.distanceTraveled || 0;

          // Enhanced path-based calculations with better validation
          if (rideData.path && rideData.path.length > 1) {
            // Calculate speeds from path data if not available in ride record
            const speeds = rideData.path
              .map(point => {
                // Handle different speed field formats
                const speed = point.speed || point.speedKmh || point.currentSpeed || 0;
                return typeof speed === 'number' && speed > 0 && speed < 100 ? speed : 0;
              })
              .filter(speed => speed > 0);
            
            if (speeds.length > 0) {
              const pathMaxSpeed = Math.max(...speeds);
              const pathAvgSpeed = speeds.reduce((sum, speed) => sum + speed, 0) / speeds.length;
              
              // Use path-calculated values if ride record values are missing or zero
              if (!maxSpeed || maxSpeed === 0) {
                maxSpeed = pathMaxSpeed;
              }
              if (!averageSpeed || averageSpeed === 0) {
                averageSpeed = pathAvgSpeed;
              }
            }

            // Calculate distance from path if not available or zero
            if (!totalDistance || totalDistance === 0) {
              for (let i = 1; i < rideData.path.length; i++) {
                const prev = rideData.path[i - 1];
                const curr = rideData.path[i];
                
                // Validate coordinates before calculating distance
                if (prev && curr && 
                    typeof prev.latitude === 'number' && typeof prev.longitude === 'number' &&
                    typeof curr.latitude === 'number' && typeof curr.longitude === 'number' &&
                    Math.abs(prev.latitude) <= 90 && Math.abs(prev.longitude) <= 180 &&
                    Math.abs(curr.latitude) <= 90 && Math.abs(curr.longitude) <= 180) {
                  
                  const segmentDistance = getDistanceBetweenPoints(prev, curr);
                  // Filter out unrealistic GPS jumps (> 100km between points)
                  if (segmentDistance < 100) {
                    totalDistance += segmentDistance * 1000; // Convert to meters
                  }
                }
              }
            }
          }

          // Final validation and cleanup
          maxSpeed = Math.max(0, Math.min(maxSpeed || 0, 100)); // Cap at 100 km/h
          averageSpeed = Math.max(0, Math.min(averageSpeed || 0, 100)); // Cap at 100 km/h
          totalDistance = Math.max(0, totalDistance || 0); // Ensure non-negative

          return {
            id: rideDoc.id,
            ...rideData,
            user: userData,
            duration,
            maxSpeed,
            averageSpeed,
            totalDistance,
            // Ensure consistent field naming for compatibility
            distanceTraveled: totalDistance, // Add alias for mobile compatibility
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
                        icon={getUnifiedMarkerIcon('bike', routeData.startLocation)}
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
                        icon={getUnifiedMarkerIcon('bike', routeData.endLocation)}
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
        borderRadius: '8px', // Reduced from 12px
        boxShadow: '0 2px 12px rgba(0, 0, 0, 0.08)', // Reduced
        marginBottom: '15px', // Reduced from 20px
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
              padding: '10px 18px', // Reduced from 15px 25px
              cursor: 'pointer',
              fontSize: '14px', // Reduced from 16px
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
              padding: '10px 18px', // Reduced from 15px 25px
              cursor: 'pointer',
              fontSize: '14px', // Reduced from 16px
              fontWeight: '600',
              transition: 'all 0.2s ease',
              position: 'relative'
            }}
          >
            🕐 Ride History
          </button>
        </div>

        <div style={{ padding: activeTab === 'live' ? '15px' : '0' }}> {/* Reduced from 20px */}
          {activeTab === 'live' ? (
            <>
              {/* Authentication Error Banner */}
              {authError && (
                <div style={{
                  backgroundColor: '#ffebee',
                  border: `2px solid ${colors.danger}`,
                  borderRadius: '6px', // Reduced from 8px
                  padding: '10px', // Reduced from 15px
                  marginBottom: '15px', // Reduced from 20px
                  textAlign: 'center'
                }}>
                  <div style={{ color: colors.danger, fontWeight: 'bold', marginBottom: '8px' }}> {/* Reduced from 10px */}
                    🚨 Authentication Issue Detected
                  </div>
                  <div style={{ color: colors.mediumGray, fontSize: '12px', marginBottom: '10px' }}> {/* Reduced from 14px, 15px */}
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
                      padding: '6px 12px', // Reduced from 8px 16px
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontWeight: 'bold',
                      fontSize: '12px' // Added
                    }}
                  >
                    🔄 Reload Dashboard
                  </button>
                </div>
              )}

              {/* Alerts Section */}
              {alerts.length > 0 && (
                <div style={{
                  background: colors.white,
                  borderRadius: '6px', // Reduced from 8px
                  padding: '10px', // Reduced from 15px
                  marginBottom: '15px', // Reduced from 20px
                  boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)', // Reduced
                  border: `2px solid ${colors.warning}`
                }}>
                  <h3 style={{ marginBottom: '10px', color: colors.darkGray, fontSize: '14px' }}> {/* Reduced from 15px, added fontSize */}
                    Active Alerts
                  </h3>
                  {alerts.map(alert => (
                    <div key={alert.id} style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px', // Reduced from 10px
                      padding: '8px', // Reduced from 10px
                      marginBottom: '6px', // Reduced from 8px
                      borderRadius: '4px', // Reduced from 6px
                      backgroundColor: alert.severity === 'high' ? '#ffebee' : 
                                     alert.severity === 'medium' ? '#fff3e0' : '#e3f2fd',
                      border: `1px solid ${alert.severity === 'high' ? colors.danger : 
                                          alert.severity === 'medium' ? colors.warning : colors.info}`
                    }}>
                      <div style={{ fontSize: '16px' }}> {/* Reduced from 18px */}
                        {alert.severity === 'high' ? '⚠️' : alert.severity === 'medium' ? '⚡' : 'ℹ️'}
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 'bold', marginBottom: '3px', fontSize: '12px' }}> {/* Reduced from 5px, added fontSize */}
                          {alert.message}
                        </div>
                        <div style={{ fontSize: '10px', color: colors.mediumGray }}> {/* Reduced from 12px */}
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
                            padding: '6px 12px', // Reduced from 8px 16px
                            borderRadius: '4px',
                            cursor: 'pointer',
                            fontSize: '11px' // Reduced from 12px
                          }}
                        >
                          Respond
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}

              {/* Stats Grid */}
              <StatsGrid>
                <StatCard>
                  <StatValue color={colors.pineGreen}>{allBikes?.length || 0}</StatValue>
                  <StatLabel>Total Bikes</StatLabel>
                </StatCard>
                <StatCard>
                  <StatValue color={colors.success}>
                    {allBikes ? allBikes.filter(bike => bike?.isAvailable && !bike?.isInUse).length : 0}
                  </StatValue>
                  <StatLabel>Available</StatLabel>
                </StatCard>
                <StatCard>
                  <StatValue color={colors.warning}>
                    {bikes ? bikes.filter(bike => bike?.isInUse).length : 0}
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
                <StatCard>
                  <StatValue color={colors.info}>{Object.keys(liveLocations).length}</StatValue>
                  <StatLabel>Live Locations</StatLabel>
                </StatCard>
              </StatsGrid>

              {/* Controls */}
              <ControlsContainer>
                <button
                  onClick={clearAllTrails}
                  style={{
                    background: 'linear-gradient(135deg, #FF6B6B, #FF8E8E)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '6px', // Reduced from 8px
                    padding: '6px 12px', // Reduced from 10px 20px
                    cursor: 'pointer',
                    fontSize: '12px', // Reduced from 14px
                    fontWeight: '500',
                    transition: 'all 0.2s ease'
                  }}
                >
                  🧹 Clear Trails
                </button>
                
                {/* Trail Settings */}
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  marginLeft: '20px',
                  padding: '8px 12px',
                  backgroundColor: '#f8f9fa',
                  borderRadius: '8px',
                  fontSize: '12px',
                  border: '1px solid #e9ecef'
                }}>
                  <label style={{ fontWeight: 'bold', color: colors.darkGray }}>Trail Length:</label>
                  <select
                    value={trailSettings.maxTrailLength}
                    onChange={(e) => setTrailSettings(prev => ({ 
                      ...prev, 
                      maxTrailLength: parseInt(e.target.value) 
                    }))}
                    style={{
                      padding: '4px 8px',
                      border: '1px solid #ddd',
                      borderRadius: '4px',
                      fontSize: '12px',
                      backgroundColor: 'white'
                    }}
                  >
                    <option value={10}>Very Short (10)</option>
                    <option value={25}>Short (25)</option>
                    <option value={50}>Medium (50)</option>
                    <option value={100}>Long (100)</option>
                    <option value={200}>Very Long (200)</option>
                  </select>
                  
                  <label style={{ fontWeight: 'bold', color: colors.darkGray, marginLeft: '15px' }}>Trail Weight:</label>
                  <input
                    type="range"
                    min="1"
                    max="8"
                    value={trailSettings.trailWeight}
                    onChange={(e) => setTrailSettings(prev => ({ 
                      ...prev, 
                      trailWeight: parseInt(e.target.value) 
                    }))}
                    style={{ width: '60px' }}
                  />
                  <span style={{ minWidth: '20px', textAlign: 'center' }}>{trailSettings.trailWeight}px</span>
                  
                  <label style={{ fontWeight: 'bold', color: colors.darkGray, marginLeft: '15px' }}>Opacity:</label>
                  <input
                    type="range"
                    min="0.1"
                    max="1"
                    step="0.1"
                    value={trailSettings.trailOpacity}
                    onChange={(e) => setTrailSettings(prev => ({ 
                      ...prev, 
                      trailOpacity: parseFloat(e.target.value) 
                    }))}
                    style={{ width: '60px' }}
                  />
                  <span style={{ minWidth: '30px', textAlign: 'center' }}>{Math.round(trailSettings.trailOpacity * 100)}%</span>
                </div>
                
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '10px', // Reduced from 15px
                  marginLeft: '15px', // Reduced from 20px
                  padding: '6px 8px', // Reduced from 10px
                  backgroundColor: '#f8f9fa',
                  borderRadius: '6px', // Reduced from 8px
                  fontSize: '11px' // Reduced from 12px
                }}>
                  <strong style={{ fontSize: '11px' }}>Map Legend:</strong>
                  
                  {/* Bike Status */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '3px' }}>
                    <div style={{
                      width: '12px',
                      height: '12px',
                      borderRadius: '50%',
                      backgroundColor: colors.success,
                      border: '1px solid white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '7px',
                      fontWeight: 'bold',
                      color: 'white'
                    }}>B</div>
                    <span>Available Bike</span>
                  </div>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '3px' }}>
                    <div style={{
                      width: '12px',
                      height: '12px',
                      borderRadius: '50%',
                      backgroundColor: colors.danger,
                      border: '1px solid white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '7px',
                      fontWeight: 'bold',
                      color: 'white'
                    }}>B</div>
                    <span>Unavailable Bike</span>
                  </div>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '3px' }}>
                    <div style={{
                      width: '12px',
                      height: '12px',
                      borderRadius: '50%',
                      backgroundColor: colors.warning,
                      border: '1px solid white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '7px',
                      fontWeight: 'bold',
                      color: 'white'
                    }}>B</div>
                    <span>Maintenance</span>
                  </div>

                  {/* Active Rides */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '3px' }}>
                    <div style={{
                      width: '12px',
                      height: '12px',
                        borderRadius: '50%',
                      backgroundColor: colors.success, 
                      border: '1px solid white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '7px',
                      fontWeight: 'bold',
                      color: 'white'
                    }}>U</div>
                    <span>Active Ride</span>
                  </div>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '3px' }}>
                      <div style={{
                      width: '12px',
                      height: '12px',
                        borderRadius: '50%',
                        backgroundColor: colors.danger,
                      border: '1px solid white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '7px',
                      fontWeight: 'bold',
                      color: 'white', 
                      animation: 'pulse 1.5s infinite'
                    }}>U</div>
                    <span>Emergency</span>
                  </div>

                  {/* Trails */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '3px' }}>
                    <div style={{
                      width: '16px',
                      height: '2px',
                      backgroundColor: colors.pineGreen,
                      borderRadius: '1px'
                    }} />
                    <span>Ride Trail</span>
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
                  {/* Enhanced bike markers with persistent display and visual indicators */}
                  {allBikes && Array.isArray(allBikes) && allBikes.length > 0 && allBikes.map(bike => {
                    if (!bike || !bike.id) {
                      console.warn('Invalid bike data:', bike);
                      return null;
                    }

                    // Check if this bike is currently in use by an active ride
                    const isInActiveRide = activeRides && activeRides.some(ride => 
                      ride.bikeId === bike.id || ride.bikeId === bike.name
                    );
                    
                    // Skip rendering static marker if bike is in active ride (active ride marker will show instead)
                    if (isInActiveRide) {
                      return null;
                    }

                    // Use unified position calculation
                    const { position: bikePosition, locationStatus, locationAge } = getUnifiedPosition(bike, 'bike');

                    // Create tooltip text with location status information
                    const createTooltipText = (bike, locationStatus, locationAge) => {
                      const statusText = bike.isAvailable ? 'Available' : 'Unavailable';
                      const maintenanceText = bike.maintenanceStatus === 'maintenance' ? ' (Maintenance)' : '';
                      const batteryText = bike.batteryLevel ? ` | Battery: ${bike.batteryLevel}%` : '';
                      
                      let locationText = '';
                      switch (locationStatus) {
                        case 'recent':
                          locationText = locationAge ? ` | Updated ${Math.round(locationAge)} min ago` : ' | Recently updated';
                          break;
                        case 'stale':
                          locationText = locationAge ? ` | Updated ${Math.round(locationAge)} min ago` : ' | Stale location';
                          break;
                        case 'old':
                          locationText = locationAge ? ` | Updated ${Math.round(locationAge / 60)} hours ago` : ' | Old location';
                          break;
                        case 'lastKnown':
                          locationText = ' | Last known location';
                          break;
                        case 'initial':
                          locationText = ' | Initial deployment location';
                          break;
                        case 'fallback':
                          locationText = ' | Estimated location (no GPS data)';
                          break;
                        default:
                          locationText = ' | Location status unknown';
                      }
                      
                      return `${bike.name || bike.id} - ${statusText}${maintenanceText}${batteryText}${locationText}`;
                    };

                    return (
                      <Marker
                        key={`enhanced-bike-${bike.id}`}
                        position={bikePosition}
                        icon={getUnifiedMarkerIcon('bike', bike, { locationStatus, size: 24 })}
                        title={createTooltipText(bike, locationStatus, locationAge)}
                        onClick={() => {
                          console.log('Enhanced bike clicked:', bike, 'Location status:', locationStatus);
                          // Show enhanced bike info with location status
                          setSelectedRide({
                            userName: bike.isAvailable ? `Available Bike (${bike.name || bike.id})` : 
                                     `Unavailable Bike (${bike.name || bike.id})`,
                            bikeId: bike.id,
                            status: bike.isAvailable ? 'available' : 'unavailable',
                            bikeName: bike.name,
                            bikeData: bike,
                            isStaticBike: true,
                            locationStatus: locationStatus,
                            locationAge: locationAge,
                            enhancedInfo: {
                              batteryLevel: bike.batteryLevel,
                              maintenanceStatus: bike.maintenanceStatus,
                              lastLocationUpdate: bike.lastLocationUpdate,
                              isInActiveRide: false
                            },
                            position: bikePosition // Add consistent position for InfoWindow
                          });
                          setMapCenter(bikePosition);
                          setMapZoom(16);
                        }}
                        zIndex={locationStatus === 'recent' ? 200 : 
                               locationStatus === 'stale' ? 150 : 
                               locationStatus === 'old' ? 100 : 
                               locationStatus === 'lastKnown' ? 80 : 
                               locationStatus === 'initial' ? 60 : 40} // Z-index based on location reliability
                      />
                    );
                  })}

                  {/* Render movement trails for each active ride */}
                  {showTrails && Object.entries(rideTrails).map(([userId, trail]) => {
                    const ride = activeRides.find(r => r.userId === userId);
                    if (!ride || !trail || trail.length < 2) return null;

                    return (
                      <Polyline
                        key={`trail-${userId}`}
                        path={trail}
                        options={{
                          strokeColor: getTrailColor(ride),
                          strokeOpacity: trailSettings.trailOpacity,
                          strokeWeight: trailSettings.trailWeight,
                          geodesic: true,
                          clickable: false,
                          icons: [{
                            icon: {
                              path: google.maps.SymbolPath.FORWARD_OPEN_ARROW,
                              scale: 2,
                              strokeColor: getTrailColor(ride),
                              strokeWeight: 2
                            },
                            offset: '100%'
                          }]
                        }}
                      />
                    );
                  })}

                  {/* Render enhanced active ride markers */}
                  {activeRides && Array.isArray(activeRides) && activeRides.length > 0 && activeRides.map(ride => {
                    // Validate ride data
                    if (!ride || !ride.userId) {
                      console.warn('Invalid ride data:', ride);
                      return null;
                    }

                    // Use unified position calculation for consistency
                    const { position: markerPosition, locationStatus } = getUnifiedPosition(ride, 'activeRide');
                    
                    return (
                      <Marker
                        key={`active-ride-${ride.userId}`}
                        position={markerPosition}
                        icon={getUnifiedMarkerIcon('activeRide', ride, { locationStatus, size: 28 })}
                        label={{
                          text: ride.userName?.substring(0, 2).toUpperCase() || ride.userId?.substring(0, 2).toUpperCase() || 'R',
                          color: 'white',
                          fontWeight: 'bold',
                          fontSize: '12px'
                        }}
                        onClick={() => {
                          console.log('Active ride clicked:', ride);
                          handleRideClick(ride);
                        }}
                        title={`${ride.userName || ride.userId} - ${ride.status?.toUpperCase() || 'ACTIVE'} - ${locationStatus?.toUpperCase() || 'UNKNOWN'}`}
                        zIndex={1000} // Higher z-index for active rides
                        animation={ride.status === 'emergency' && typeof google !== 'undefined' && google.maps ? 
                          google.maps.Animation.BOUNCE : undefined}
                      />
                    );
                  })}

                  {/* Add circles for ALL active rides - with different styles based on GPS availability */}
                  {activeRides.map(ride => {
                    // Use unified position calculation for consistency
                    const { position: centerPosition, locationStatus } = getUnifiedPosition(ride, 'activeRide');
                    
                    // Different visual styles based on GPS availability
                    const isLiveGPS = locationStatus === 'recent';
                    const isCachedGPS = locationStatus === 'stale';
                    const isNoGPS = locationStatus === 'fallback';
                    
                    // Colors and opacity based on GPS status
                    let fillOpacity, strokeOpacity, strokeWeight;
                    if (isLiveGPS) {
                      fillOpacity = [0.3, 0.15, 0.08];
                      strokeOpacity = [0.7, 0.5, 0.3];
                      strokeWeight = [2, 2, 1];
                    } else if (isCachedGPS) {
                      fillOpacity = [0.2, 0.1, 0.05];
                      strokeOpacity = [0.5, 0.3, 0.2];
                      strokeWeight = [2, 1, 1];
                    } else {
                      fillOpacity = [0.15, 0.08, 0.04];
                      strokeOpacity = [0.4, 0.25, 0.15];
                      strokeWeight = [1, 1, 1];
                    }
                    
                    const baseColor = getTrailColor(ride);
                    const warningColor = isNoGPS ? colors.warning : baseColor;
                    
                    return (
                      <React.Fragment key={`pulse-group-${ride.userId}`}>
                        {/* Inner circle - ALWAYS VISIBLE */}
                        <Circle
                          key={`pulse-inner-${ride.userId}`}
                          center={centerPosition}
                          radius={25} // 25 meter radius
                          options={{
                            fillColor: warningColor,
                            fillOpacity: fillOpacity[0],
                            strokeColor: warningColor,
                            strokeOpacity: strokeOpacity[0],
                            strokeWeight: strokeWeight[0],
                            clickable: true,
                            zIndex: 502,
                            ...(isLiveGPS && { 
                              strokeDashArray: isNoGPS ? '5,5' : undefined // Dashed for no GPS
                            })
                          }}
                          onClick={() => {
                            console.log('Inner circle clicked for ride:', ride, 'GPS Status:', 
                                      isLiveGPS ? 'LIVE' : isCachedGPS ? 'CACHED' : 'NO GPS');
                            handleRideClick(ride);
                          }}
                        />
                        
                        {/* Middle circle - ALWAYS VISIBLE */}
                        <Circle
                          key={`pulse-middle-${ride.userId}`}
                          center={centerPosition}
                          radius={40} // 40 meter radius
                          options={{
                            fillColor: warningColor,
                            fillOpacity: fillOpacity[1],
                            strokeColor: warningColor,
                            strokeOpacity: strokeOpacity[1],
                            strokeWeight: strokeWeight[1],
                            clickable: true,
                            zIndex: 501,
                            ...(isNoGPS && { 
                              strokeDashArray: '8,8' // Dashed for no GPS
                            })
                          }}
                          onClick={() => {
                            console.log('Middle circle clicked for ride:', ride, 'GPS Status:', 
                                      isLiveGPS ? 'LIVE' : isCachedGPS ? 'CACHED' : 'NO GPS');
                            handleRideClick(ride);
                          }}
                        />
                        
                        {/* Outer circle - ALWAYS VISIBLE */}
                        <Circle
                          key={`pulse-outer-${ride.userId}`}
                          center={centerPosition}
                          radius={60} // 60 meter radius
                          options={{
                            fillColor: warningColor,
                            fillOpacity: fillOpacity[2],
                            strokeColor: warningColor,
                            strokeOpacity: strokeOpacity[2],
                            strokeWeight: strokeWeight[2],
                            clickable: true,
                            zIndex: 500,
                            ...(isNoGPS && { 
                              strokeDashArray: '10,10' // Dashed for no GPS
                            })
                          }}
                          onClick={() => {
                            console.log('Outer circle clicked for ride:', ride, 'GPS Status:', 
                                      isLiveGPS ? 'LIVE' : isCachedGPS ? 'CACHED' : 'NO GPS');
                            handleRideClick(ride);
                          }}
                        />
                        
                        {/* Center indicator dot - ALWAYS VISIBLE with different styles */}
                        <Circle
                          key={`center-dot-${ride.userId}`}
                          center={centerPosition}
                          radius={8} // 8 meter radius for center dot
                          options={{
                            fillColor: isLiveGPS ? '#ffffff' : isCachedGPS ? '#fff3cd' : '#ffcccc',
                            fillOpacity: 0.9,
                            strokeColor: warningColor,
                            strokeOpacity: 1,
                            strokeWeight: 2,
                            clickable: true,
                            zIndex: 505,
                            ...(isNoGPS && { 
                              strokeDashArray: '3,3' // Dashed for no GPS
                            })
                          }}
                          onClick={() => {
                            console.log('Center dot clicked for ride:', ride, 'GPS Status:', 
                                      isLiveGPS ? 'LIVE' : isCachedGPS ? 'CACHED' : 'NO GPS');
                            handleRideClick(ride);
                          }}
                        />
                      </React.Fragment>
                    );
                  })}

                  {/* Show info window for selected ride or bike */}
                  {selectedRide && (selectedRide.isStaticBike ? 
                    // Enhanced Static bike info window
                    <InfoWindow
                      position={selectedRide.position || {
                        lat: selectedRide.bikeData.latitude || 14.5995,
                        lng: selectedRide.bikeData.longitude || 120.9842
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
                            backgroundColor: selectedRide.bikeData.isAvailable ? colors.success : colors.danger,
                            animation: selectedRide.enhancedInfo?.maintenanceStatus === 'maintenance' ? 'pulse 2s infinite' : 'none'
                          }} />
                          <h3 style={{ margin: 0, color: colors.darkGray }}>
                            🚲 {selectedRide.bikeName || selectedRide.bikeData.name || selectedRide.bikeId || 'Unknown Bike'}
                          </h3>
                          <span style={{
                            backgroundColor: selectedRide.bikeData.isAvailable ? colors.success : colors.danger,
                            color: 'white',
                            padding: '2px 8px',
                            borderRadius: '12px',
                            fontSize: '10px',
                            fontWeight: 'bold',
                            textTransform: 'uppercase'
                          }}>
                            {selectedRide.bikeData.isAvailable ? 'AVAILABLE' : 'UNAVAILABLE'}
                          </span>
                          {selectedRide.locationStatus && (
                            <span style={{
                              backgroundColor: selectedRide.locationStatus === 'recent' ? colors.success : 
                                             selectedRide.locationStatus === 'stale' ? colors.warning :
                                             selectedRide.locationStatus === 'old' ? '#FF8000' :
                                             selectedRide.locationStatus === 'lastKnown' ? colors.info :
                                             selectedRide.locationStatus === 'initial' ? colors.indigo :
                                             colors.danger,
                              color: 'white',
                              padding: '1px 6px',
                              borderRadius: '8px',
                              fontSize: '9px',
                              fontWeight: 'bold'
                            }}>
                              {selectedRide.locationStatus === 'recent' ? 'LIVE GPS' :
                               selectedRide.locationStatus === 'stale' ? 'STALE GPS' :
                               selectedRide.locationStatus === 'old' ? 'OLD GPS' :
                               selectedRide.locationStatus === 'lastKnown' ? 'LAST KNOWN' :
                               selectedRide.locationStatus === 'initial' ? 'INITIAL LOC' :
                               'NO GPS'}
                            </span>
                          )}
                        </div>
                        
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '15px' }}>
                          <div>
                            <p style={{ margin: '5px 0', fontSize: '13px' }}>
                              <strong>🆔 Bike ID:</strong><br />
                              <span style={{ color: colors.mediumGray }}>{selectedRide.bikeData.id || selectedRide.bikeId}</span>
                            </p>
                            <p style={{ margin: '5px 0', fontSize: '13px' }}>
                              <strong>🚴‍♀️ Type:</strong><br />
                              <span style={{ color: colors.pineGreen }}>{selectedRide.bikeData.type || 'Standard'}</span>
                            </p>
                            <p style={{ margin: '5px 0', fontSize: '13px' }}>
                              <strong>💰 Price:</strong><br />
                              <span style={{ color: colors.info, fontWeight: 'bold' }}>
                                {selectedRide.bikeData.price || selectedRide.bikeData.pricePerHour ? 
                                  `₱${selectedRide.bikeData.price || selectedRide.bikeData.pricePerHour}/hr` : 'N/A'}
                              </span>
                            </p>
                          </div>
                          
                          <div>
                            <p style={{ margin: '5px 0', fontSize: '13px' }}>
                              <strong>🔋 Battery:</strong><br />
                              <span style={{ 
                                color: (selectedRide.enhancedInfo?.batteryLevel || 0) > 50 ? colors.success :
                                       (selectedRide.enhancedInfo?.batteryLevel || 0) > 20 ? colors.warning : colors.danger,
                                fontWeight: 'bold' 
                              }}>
                                {selectedRide.enhancedInfo?.batteryLevel || 'N/A'}%
                              </span>
                            </p>
                            <p style={{ margin: '5px 0', fontSize: '13px' }}>
                              <strong>🔧 Maintenance:</strong><br />
                              <span style={{ 
                                color: selectedRide.enhancedInfo?.maintenanceStatus === 'maintenance' ? colors.warning : colors.success,
                                fontWeight: 'bold' 
                              }}>
                                {selectedRide.enhancedInfo?.maintenanceStatus === 'maintenance' ? 'Required' : 'Good'}
                              </span>
                            </p>
                            <p style={{ margin: '5px 0', fontSize: '13px' }}>
                              <strong>🚫 In Use:</strong><br />
                              <span style={{ 
                                color: selectedRide.enhancedInfo?.isInActiveRide ? colors.warning : colors.success,
                                fontWeight: 'bold' 
                              }}>
                                {selectedRide.enhancedInfo?.isInActiveRide ? 'Yes' : 'No'}
                              </span>
                            </p>
                          </div>
                        </div>
                        
                        {/* Location Status Section */}
                        <div style={{ 
                          backgroundColor: selectedRide.locationStatus === 'recent' ? '#e8f5e8' :
                                       selectedRide.locationStatus === 'stale' ? '#fff3cd' :
                                       selectedRide.locationStatus === 'old' ? '#ffe6cc' :
                                       selectedRide.locationStatus === 'lastKnown' ? '#e3f2fd' :
                                       selectedRide.locationStatus === 'initial' ? '#f3e5f5' :
                                       '#ffebee',
                          padding: '10px', 
                          borderRadius: '8px',
                          border: `2px solid ${selectedRide.locationStatus === 'recent' ? colors.success :
                                              selectedRide.locationStatus === 'stale' ? colors.warning :
                                              selectedRide.locationStatus === 'old' ? '#FF8000' :
                                              selectedRide.locationStatus === 'lastKnown' ? colors.info :
                                              selectedRide.locationStatus === 'initial' ? colors.indigo :
                                              colors.danger}`,
                          marginBottom: '10px'
                        }}>
                          <p style={{ margin: '0', fontSize: '12px', color: colors.mediumGray }}>
                            <strong>📍 Location Status:</strong><br />
                            {selectedRide.locationStatus === 'recent' && 
                              `✅ Recently updated location${selectedRide.locationAge ? ` (${Math.round(selectedRide.locationAge)} min ago)` : ''}`}
                            {selectedRide.locationStatus === 'stale' && 
                              `⚠️ Stale location data${selectedRide.locationAge ? ` (${Math.round(selectedRide.locationAge)} min ago)` : ''}`}
                            {selectedRide.locationStatus === 'old' && 
                              `⏰ Old location data${selectedRide.locationAge ? ` (${Math.round(selectedRide.locationAge / 60)} hours ago)` : ''}`}
                            {selectedRide.locationStatus === 'lastKnown' && 
                              `📍 Last known position${selectedRide.locationAge ? ` (${Math.round(selectedRide.locationAge / 60)} hours ago)` : ''}`}
                            {selectedRide.locationStatus === 'initial' && 
                              '🏭 Initial deployment location'}
                            {selectedRide.locationStatus === 'fallback' && 
                              '❓ Estimated location (no GPS data available)'}
                            <br />
                            <strong>🕐 Last Update:</strong> {selectedRide.enhancedInfo?.lastLocationUpdate ? 
                              new Date(selectedRide.enhancedInfo.lastLocationUpdate).toLocaleString() : 'Never'}
                          </p>
                        </div>
                        
                        {selectedRide.bikeData?.description && (
                          <div style={{ 
                            backgroundColor: '#f8f9fa', 
                            padding: '8px', 
                            borderRadius: '6px',
                            marginBottom: '10px'
                          }}>
                            <p style={{ margin: '0', fontSize: '12px', color: colors.mediumGray }}>
                              <strong>📝 Description:</strong><br />
                              {selectedRide.bikeData.description}
                            </p>
                          </div>
                        )}
                        
                        <div style={{ 
                          backgroundColor: selectedRide.bikeData?.isAvailable ? '#e8f5e8' : '#ffebee',
                          padding: '8px',
                          borderRadius: '6px',
                          border: `2px solid ${selectedRide.bikeData?.isAvailable ? colors.success : colors.danger}`,
                          textAlign: 'center'
                        }}>
                          <span style={{ 
                            fontSize: '12px', 
                            fontWeight: 'bold',
                            color: selectedRide.bikeData?.isAvailable ? colors.success : colors.danger
                          }}>
                            {selectedRide.bikeData?.isAvailable ? '✅ READY FOR BOOKING' : '❌ NOT AVAILABLE'}
                          </span>
                        </div>
                      </InfoWindowContent>
                    </InfoWindow>
                    :
                    // Active ride info window  
                    (() => {
                      // Use unified position calculation for consistency
                      const { position: infoWindowPosition } = getUnifiedPosition(selectedRide, 'activeRide');
                      
                      return (
                        <InfoWindow
                          position={infoWindowPosition}
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
                                🚴‍♂️ {selectedRide.userName || selectedRide.userId || 'Unknown User'}
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
                                {selectedRide.status || 'ACTIVE'}
                              </span>
                              <span style={{
                                backgroundColor: liveLocations[selectedRide.userId] ? colors.success : selectedRide.currentLocation ? colors.warning : colors.danger,
                                color: 'white',
                                padding: '1px 6px',
                                borderRadius: '8px',
                                fontSize: '9px',
                                fontWeight: 'bold'
                              }}>
                                {liveLocations[selectedRide.userId] ? 'LIVE' : selectedRide.currentLocation ? 'CACHED' : 'NO GPS'}
                              </span>
                            </div>
                            
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '15px' }}>
                              <div>
                                <p style={{ margin: '5px 0', fontSize: '13px' }}>
                                  <strong>🚲 Bike ID:</strong><br />
                                  <span style={{ color: colors.mediumGray }}>{selectedRide.bikeId || 'N/A'}</span>
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
                                    {((selectedRide.totalDistance || selectedRide.distanceTraveled || 0) / 1000).toFixed(2)} km
                                  </span>
                                </p>
                              </div>
                              
                              <div>
                                <p style={{ margin: '5px 0', fontSize: '13px' }}>
                                  <strong>🏃‍♂️ Current Speed:</strong><br />
                                  <span style={{ color: colors.success, fontWeight: 'bold' }}>
                                    {selectedRide.currentLocation ? (selectedRide.currentLocation.speed || 0).toFixed(1) : 'N/A'} km/h
                                  </span>
                                </p>
                                <p style={{ margin: '5px 0', fontSize: '13px' }}>
                                  <strong>🎯 Accuracy:</strong><br />
                                  <span style={{ color: colors.mediumGray }}>
                                    {selectedRide.currentLocation ? `±${(selectedRide.currentLocation.accuracy || 0).toFixed(0)} m` : 'N/A'}
                                  </span>
                                </p>
                                <p style={{ margin: '5px 0', fontSize: '13px' }}>
                                  <strong>📅 Started:</strong><br />
                                  <span style={{ color: colors.mediumGray }}>
                                    {selectedRide.startTime ? new Date(selectedRide.startTime).toLocaleTimeString() : 'N/A'}
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
                                <strong>👤 User ID:</strong> {selectedRide.userId || 'N/A'}<br />
                                <strong>📧 User Email:</strong> {selectedRide.userEmail || 'N/A'}<br />
                                <strong>🔄 GPS Updates:</strong> {selectedRide.currentLocation?.locationCount || 0}<br />
                                <strong>🕐 Last Update:</strong> {selectedRide.currentLocation?.timestamp ? new Date(selectedRide.currentLocation.timestamp).toLocaleTimeString() : 'Never'}
                              </p>
                            </div>
                            
                            {!selectedRide.currentLocation && (
                              <div style={{ 
                                backgroundColor: '#fff3cd',
                                padding: '8px',
                                borderRadius: '6px',
                                border: `2px solid ${colors.warning}`,
                                textAlign: 'center',
                                marginBottom: '10px'
                              }}>
                                <span style={{ 
                                  fontSize: '12px', 
                                  fontWeight: 'bold',
                                  color: colors.warning
                                }}>
                                  ⚠️ WAITING FOR GPS DATA - Ride started but no location updates yet
                                </span>
                              </div>
                            )}
                            
                            <div style={{ 
                              backgroundColor: selectedRide.status === 'emergency' ? '#ffebee' : '#e8f5e8',
                              padding: '8px',
                              borderRadius: '6px',
                              border: `2px solid ${selectedRide.status === 'emergency' ? colors.danger : colors.success}`,
                              textAlign: 'center'
                            }}>
                              <span style={{ 
                                fontSize: '12px', 
                                fontWeight: 'bold',
                                color: selectedRide.status === 'emergency' ? colors.danger : colors.success
                              }}>
                                {selectedRide.status === 'emergency' ? '🚨 EMERGENCY STATUS' : 
                                 selectedRide.status === 'paused' ? '⏸️ RIDE PAUSED' : 
                                 '✅ ACTIVE RIDE'}
                              </span>
                            </div>
                            
                            {/* Live Location Rings Visualization */}
                            {selectedRide.currentLocation && (
                              <div style={{ 
                                marginTop: '15px',
                                padding: '15px',
                                backgroundColor: '#f8f9fa',
                                borderRadius: '8px',
                                border: '2px solid #e9ecef',
                                textAlign: 'center'
                              }}>
                                <div style={{ 
                                  fontSize: '12px', 
                                  fontWeight: 'bold', 
                                  color: colors.darkGray,
                                  marginBottom: '10px' 
                                }}>
                                  📍 Live Location Rings
                                </div>
                                
                                {/* Visual representation of pulsing circles */}
                                <div style={{ 
                                  position: 'relative',
                                  display: 'flex',
                                  justifyContent: 'center',
                                  alignItems: 'center',
                                  height: '120px',
                                  marginBottom: '10px'
                                }}>
                                  {/* Outer ring - 60m */}
                                  <div style={{
                                    position: 'absolute',
                                    width: '100px',
                                    height: '100px',
                                    borderRadius: '50%',
                                    border: `3px solid ${getTrailColor(selectedRide)}`,
                                    backgroundColor: getTrailColor(selectedRide),
                                    opacity: 0.1,
                                    animation: 'liveLocationPulse 3s ease-in-out infinite'
                                  }} />
                                  
                                  {/* Middle ring - 40m */}
                                  <div style={{
                                    position: 'absolute',
                                    width: '70px',
                                    height: '70px',
                                    borderRadius: '50%',
                                    border: `3px solid ${getTrailColor(selectedRide)}`,
                                    backgroundColor: getTrailColor(selectedRide),
                                    opacity: 0.2,
                                    animation: 'liveLocationPulse 2.5s ease-in-out infinite 0.3s'
                                  }} />
                                  
                                  {/* Inner ring - 25m */}
                                  <div style={{
                                    position: 'absolute',
                                    width: '40px',
                                    height: '40px',
                                    borderRadius: '50%',
                                    border: `3px solid ${getTrailColor(selectedRide)}`,
                                    backgroundColor: getTrailColor(selectedRide),
                                    opacity: 0.3,
                                    animation: 'liveLocationPulse 2s ease-in-out infinite 0.6s'
                                  }} />
                                  
                                  {/* Center dot */}
                                  <div style={{
                                    position: 'absolute',
                                    width: '12px',
                                    height: '12px',
                                    borderRadius: '50%',
                                    backgroundColor: '#ffffff',
                                    border: `3px solid ${getTrailColor(selectedRide)}`,
                                    boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
                                    zIndex: 10
                                  }} />
                                </div>
                                
                                {/* Legend */}
                                <div style={{ 
                                  fontSize: '10px', 
                                  color: colors.mediumGray,
                                  lineHeight: '1.3'
                                }}>
                                  <div style={{ marginBottom: '3px' }}>
                                    <span style={{ color: getTrailColor(selectedRide), fontWeight: 'bold' }}>●</span> Live Position
                                  </div>
                                  <div style={{ marginBottom: '3px' }}>
                                    Accuracy: ±{(selectedRide.currentLocation.accuracy || 0).toFixed(0)}m
                                  </div>
                                  <div>
                                    Rings: 25m • 40m • 60m radius
                                  </div>
                                </div>
                              </div>
                            )}
                            
                            {/* No GPS visualization */}
                            {!selectedRide.currentLocation && liveLocations[selectedRide.userId] && (
                              <div style={{ 
                                marginTop: '15px',
                                padding: '15px',
                                backgroundColor: '#fff3cd',
                                borderRadius: '8px',
                                border: '2px solid #ffc107',
                                textAlign: 'center'
                              }}>
                                <div style={{ 
                                  fontSize: '12px', 
                                  fontWeight: 'bold', 
                                  color: colors.warning,
                                  marginBottom: '10px' 
                                }}>
                                  📍 Last Known Location
                                </div>
                                
                                <div style={{ 
                                  position: 'relative',
                                  display: 'flex',
                                  justifyContent: 'center',
                                  alignItems: 'center',
                                  height: '60px',
                                  marginBottom: '10px'
                                }}>
                                  {/* Static circle for cached location */}
                                  <div style={{
                                    width: '40px',
                                    height: '40px',
                                    borderRadius: '50%',
                                    border: `3px dashed ${colors.warning}`,
                                    backgroundColor: colors.warning,
                                    opacity: 0.2
                                  }} />
                                  
                                  {/* Center dot */}
                                  <div style={{
                                    position: 'absolute',
                                    width: '8px',
                                    height: '8px',
                                    borderRadius: '50%',
                                    backgroundColor: colors.warning,
                                    border: '2px solid #ffffff'
                                  }} />
                                </div>
                                
                                <div style={{ 
                                  fontSize: '10px', 
                                  color: colors.mediumGray,
                                  lineHeight: '1.3'
                                }}>
                                  Cached position • No live updates
                                </div>
                              </div>
                            )}
                            
                            {/* Waiting for GPS visualization */}
                            {!selectedRide.currentLocation && (
                              <div style={{ 
                                marginTop: '15px',
                                padding: '15px',
                                backgroundColor: '#ffebee',
                                borderRadius: '8px',
                                border: '2px solid #f44336',
                                textAlign: 'center'
                              }}>
                                <div style={{ 
                                  fontSize: '12px', 
                                  fontWeight: 'bold', 
                                  color: colors.danger,
                                  marginBottom: '10px' 
                                }}>
                                  📍 Waiting for GPS
                                </div>
                                
                                <div style={{ 
                                  position: 'relative',
                                  display: 'flex',
                                  justifyContent: 'center',
                                  alignItems: 'center',
                                  height: '60px',
                                  marginBottom: '10px'
                                }}>
                                  {/* Searching animation */}
                                  <div style={{
                                    width: '30px',
                                    height: '30px',
                                    borderRadius: '50%',
                                    border: `3px solid ${colors.lightGray}`,
                                    borderTop: `3px solid ${colors.danger}`,
                                    animation: 'spin 1s linear infinite'
                                  }} />
                                </div>
                                
                                <div style={{ 
                                  fontSize: '10px', 
                                  color: colors.mediumGray,
                                  lineHeight: '1.3'
                                }}>
                                  Searching for GPS signal...
                                </div>
                              </div>
                            )}
                          </InfoWindowContent>
                        </InfoWindow>
                      );
                    })()
                  )}
                </MapContainer>
              </MapWrapper>

              {/* Active Rides List */}
              <RidesList>
                <h3 style={{ marginBottom: '10px', color: colors.darkGray, fontSize: '14px' }}> {/* Reduced marginBottom from 15px, added fontSize */}
                  Active Rides ({activeRides?.length || 0})
                </h3>
                {activeRides && activeRides.length > 0 ? (
                  activeRides.map(ride => {
                    const liveLocation = liveLocations[ride.userId];
                    const hasLocation = !!(liveLocation || ride.currentLocation);
                    
                    return (
                      <RideCard key={ride.userId || ride.id} onClick={() => handleRideClick(ride)} style={{
                        padding: '10px', // Reduced from default padding
                        marginBottom: '8px', // Reduced spacing between cards
                        borderRadius: '6px' // Reduced from default border radius
                      }}>
                        <RideInfo>
                          <div style={{ fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px' }}> {/* Reduced gap from 8px, added fontSize */}
                            {ride.userName || ride.userId || 'Unknown User'}
                            {liveLocation && (
                              <span style={{
                                backgroundColor: colors.success,
                                color: 'white',
                                padding: '1px 4px', // Reduced from 1px 6px
                                borderRadius: '6px', // Reduced from 8px
                                fontSize: '8px', // Reduced from 9px
                                fontWeight: 'bold'
                              }}>
                                LIVE
                              </span>
                            )}
                          </div>
                          <div style={{ fontSize: '12px', color: colors.mediumGray, marginTop: '2px' }}> {/* Reduced from 14px, added marginTop */}
                            Bike: {ride.bikeId || 'N/A'} • Duration: {ride.duration ? formatTime(ride.duration) : formatDuration(ride.startTime)}
                          </div>
                          <div style={{ fontSize: '11px', color: colors.mediumGray, marginTop: '1px' }}> {/* Reduced from 12px, added marginTop */}
                            User ID: {ride.userId} • Status: {ride.status || 'active'}
                            {ride.userEmail && ` • ${ride.userEmail}`}
                          </div>
                        </RideInfo>
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '4px' }}> {/* Reduced gap from 5px */}
                          <RideStatus status={ride.status || 'active'} style={{
                            fontSize: '10px', // Added smaller font size
                            padding: '2px 6px' // Added smaller padding
                          }}>
                            {ride.status || 'active'}
                          </RideStatus>
                          <div style={{ fontSize: '12px', textAlign: 'right' }}> {/* Reduced from 14px */}
                            {ride.totalDistance ? 
                              `${((ride.totalDistance || 0) / 1000).toFixed(1)} km` : 
                              hasLocation ? 'Tracking...' : 'No location'
                            }
                          </div>
                          {(liveLocation || ride.currentLocation) && (
                            <div style={{ fontSize: '10px', color: colors.success, textAlign: 'right' }}> {/* Reduced from 12px */}
                              {liveLocation ? 'LIVE' : 'CACHED'} • {(liveLocation?.speed || ride.currentLocation?.speed || 0).toFixed(1)} km/h
                            </div>
                          )}
                          {!hasLocation && (
                            <div style={{ fontSize: '10px', color: colors.danger, textAlign: 'right' }}> {/* Reduced from 11px */}
                              ⚠️ No GPS data
                            </div>
                          )}
                        </div>
                      </RideCard>
                    );
                  })
                ) : (
                  <div style={{ textAlign: 'center', color: colors.mediumGray, padding: '15px' }}> {/* Reduced padding from 20px */}
                    <div style={{ fontSize: '36px', marginBottom: '8px' }}>🚴‍♂️</div> {/* Reduced from 48px, 10px */}
                    <div style={{ fontSize: '14px', fontWeight: 'bold', marginBottom: '4px' }}> {/* Reduced from 16px, 5px */}
                      No active rides at the moment
                    </div>
                    <div style={{ fontSize: '12px' }}> {/* Reduced from 14px */}
                      Active rides will appear here when users start cycling
                    </div>
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