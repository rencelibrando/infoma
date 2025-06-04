/* global google */
import React, { useState, useEffect, useCallback } from 'react';
import styled from 'styled-components';
import { Marker, Polyline } from '@react-google-maps/api';
import MapContainer from '../MapContainer';
import { getRideRouteSummary, getFilteredRideHistory } from '../../services/bikeLocationService';

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

const SectionHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  flex-wrap: wrap;
  gap: 10px;
  padding: 15px;
  background: transparent;
  border-bottom: none;
`;

const Title = styled.h3`
  margin: 0;
  color: ${colors.darkGray};
  font-size: 20px;
  font-weight: 600;
`;

const FiltersContainer = styled.div`
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
`;

const FilterSelect = styled.select`
  padding: 6px 10px;
  border: 1px solid ${colors.lightGray};
  border-radius: 6px;
  font-size: 12px;
  background: ${colors.white};
  color: ${colors.darkGray};
  cursor: pointer;
  transition: border-color 0.2s ease;

  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
`;

const FilterInput = styled.input`
  padding: 8px 12px;
  border: 2px solid ${colors.lightGray};
  border-radius: 8px;
  font-size: 14px;
  background: ${colors.white};
  color: ${colors.darkGray};
  transition: border-color 0.2s ease;

  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
`;

const ContentContainer = styled.div`
  padding: 15px;
  background: ${colors.white};
`;

const LoadingSpinner = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 30px;
  color: ${colors.mediumGray};
  font-size: 14px;
`;

const RideGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
  margin-bottom: 15px;
`;

const RideCard = styled.div`
  background: linear-gradient(135deg, ${colors.white} 0%, #f8f9fa 100%);
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.2s ease;
  cursor: pointer;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
`;

const RideHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid ${colors.lightGray};
`;

const UserAvatar = styled.div`
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: ${props => props.src ? `url(${props.src})` : `linear-gradient(135deg, ${colors.pineGreen}, ${colors.lightPineGreen})`};
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 12px;
  flex-shrink: 0;
`;

const UserInfo = styled.div`
  flex: 1;
  min-width: 0;
`;

const UserName = styled.div`
  font-weight: 600;
  font-size: 13px;
  color: ${colors.darkGray};
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const UserEmail = styled.div`
  font-size: 10px;
  color: ${colors.mediumGray};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StatusBadge = styled.span`
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 9px;
  font-weight: bold;
  text-transform: uppercase;
  background: ${props => {
    switch(props.status) {
      case 'completed': return colors.success;
      case 'active': return colors.warning;
      case 'cancelled': return colors.danger;
      default: return colors.mediumGray;
    }
  }};
  color: white;
  flex-shrink: 0;
`;

const LocationIndicator = styled.div`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  border-radius: 8px;
  font-size: 8px;
  font-weight: bold;
  text-transform: uppercase;
  background: ${props => props.isLive ? colors.success : 
               props.hasLocation ? colors.info : colors.mediumGray};
  color: white;
  flex-shrink: 0;
`;

const LocationDot = styled.div`
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: ${props => props.isLive ? 'pulse 2s infinite' : 'none'};
  
  @keyframes pulse {
    0% {
      opacity: 1;
    }
    50% {
      opacity: 0.5;
    }
    100% {
      opacity: 1;
    }
  }
`;

const RideDetails = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 8px;
`;

const DetailItem = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
`;

const DetailLabel = styled.span`
  font-size: 9px;
  color: ${colors.mediumGray};
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.3px;
`;

const DetailValue = styled.span`
  font-size: 11px;
  font-weight: 600;
  color: ${colors.darkGray};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const ContactInfo = styled.div`
  background: ${colors.lightGray};
  padding: 6px;
  border-radius: 4px;
  margin: 6px 0;
  font-size: 9px;
`;

const ContactHeader = styled.div`
  font-weight: 600;
  margin-bottom: 3px;
  color: ${colors.darkGray};
`;

const ContactDetails = styled.div`
  color: ${colors.mediumGray};
  line-height: 1.3;
  
  div {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
`;

const ViewRouteButton = styled.button`
  background: linear-gradient(135deg, ${colors.pineGreen} 0%, ${colors.lightPineGreen} 100%);
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 6px;
  width: 100%;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(29, 60, 52, 0.3);
  }
`;

const RouteModal = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
`;

const ModalContent = styled.div`
  background: ${colors.white};
  border-radius: 8px;
  padding: 15px;
  max-width: 85vw;
  max-height: 85vh;
  overflow: auto;
  position: relative;
`;

const CloseButton = styled.button`
  position: absolute;
  top: 10px;
  right: 10px;
  background: ${colors.danger};
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: #b71c1c;
  }
`;

const ModalMap = styled.div`
  height: 400px;
  width: 700px;
  max-width: 100%;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 15px;
`;

const RouteStats = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
  margin-top: 15px;
`;

const StatCard = styled.div`
  background: ${colors.lightGray};
  padding: 10px;
  border-radius: 6px;
  text-align: center;
`;

const StatValue = styled.div`
  font-size: 16px;
  font-weight: bold;
  color: ${colors.pineGreen};
  margin-bottom: 3px;
`;

const StatLabel = styled.div`
  font-size: 10px;
  color: ${colors.mediumGray};
  text-transform: uppercase;
  letter-spacing: 0.3px;
`;

const EmptyState = styled.div`
  text-align: center;
  color: ${colors.mediumGray};
  padding: 30px;
  font-size: 14px;
`;

const StaticMapImage = styled.img`
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 6px;
  margin: 8px 0;
`;

const RideHistorySection = () => {
  const [rides, setRides] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRide, setSelectedRide] = useState(null);
  const [routeData, setRouteData] = useState(null);
  const [showRouteModal, setShowRouteModal] = useState(false);
  const [filters, setFilters] = useState({
    status: 'all',
    dateRange: 'week',
    limit: 20,
    sortBy: 'startTime',
    sortOrder: 'desc'
  });

  // Load ride history with filters
  const loadRideHistory = useCallback(async (currentFilters = filters) => {
    try {
      setLoading(true);
      console.log('Loading ride history with filters:', currentFilters);
      
      const rideHistory = await getFilteredRideHistory(currentFilters);
      
      console.log('Loaded rides:', rideHistory.length);
      setRides(rideHistory);
    } catch (error) {
      console.error('Error loading ride history:', error);
      setRides([]);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  // Initial load on component mount
  useEffect(() => {
    console.log('RideHistorySection mounted, loading initial data...');
    loadRideHistory();
  }, []);

  // Handle filter changes
  const handleFilterChange = async (newFilters) => {
    const updatedFilters = { ...filters, ...newFilters };
    console.log('Filter changed:', newFilters, 'Updated filters:', updatedFilters);
    
    setFilters(updatedFilters);
    await loadRideHistory(updatedFilters);
  };

  // View route details
  const viewRoute = async (ride) => {
    try {
      setSelectedRide(ride);
      setShowRouteModal(true);
      const routeSummary = await getRideRouteSummary(ride.id);
      setRouteData(routeSummary);
    } catch (error) {
      console.error('Error loading route data:', error);
      alert('Unable to load route data for this ride.');
    }
  };

  // Format time duration
  const formatDuration = (duration) => {
    if (!duration) return 'N/A';
    const hours = Math.floor(duration / (1000 * 60 * 60));
    const minutes = Math.floor((duration % (1000 * 60 * 60)) / (1000 * 60));
    return `${hours}h ${minutes}m`;
  };

  // Format distance
  const formatDistance = (distance) => {
    if (!distance) return '0 km';
    return `${(distance / 1000).toFixed(2)} km`;
  };

  // Get user initials for avatar
  const getUserInitials = (name) => {
    if (!name) return 'U';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  };

  return (
    <ContentContainer>
      <SectionHeader>
        <Title>🕐 Ride History</Title>
        <FiltersContainer>
          <FilterSelect
            value={filters.status}
            onChange={(e) => handleFilterChange({ status: e.target.value })}
          >
            <option value="all">All Status</option>
            <option value="completed">Completed</option>
            <option value="active">Active</option>
            <option value="cancelled">Cancelled</option>
          </FilterSelect>
          
          <FilterSelect
            value={filters.dateRange}
            onChange={(e) => handleFilterChange({ dateRange: e.target.value })}
          >
            <option value="all">All Time</option>
            <option value="today">Today</option>
            <option value="week">This Week</option>
            <option value="month">This Month</option>
            <option value="year">This Year</option>
          </FilterSelect>
          
          <FilterSelect
            value={filters.limit}
            onChange={(e) => handleFilterChange({ limit: parseInt(e.target.value) })}
          >
            <option value={10}>10 rides</option>
            <option value={20}>20 rides</option>
            <option value={50}>50 rides</option>
            <option value={100}>100 rides</option>
          </FilterSelect>
        </FiltersContainer>
      </SectionHeader>

      {loading ? (
        <LoadingSpinner>
          <div>🔄 Loading ride history...</div>
        </LoadingSpinner>
      ) : (
        <RideGrid>
          {rides.map(ride => (
            <RideCard key={ride.id}>
              <RideHeader>
                <UserAvatar src={ride.user?.profilePicture}>
                  {!ride.user?.profilePicture && getUserInitials(ride.user?.name || 'U')}
                </UserAvatar>
                <UserInfo>
                  <UserName>{ride.user?.name || 'Unknown User'}</UserName>
                  <UserEmail>{ride.user?.email || 'No email'}</UserEmail>
                  {ride.user?.phone && (
                    <div style={{ fontSize: '9px', color: colors.mediumGray, marginTop: '1px' }}>
                      📞 {ride.user.phone}
                    </div>
                  )}
                </UserInfo>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'flex-end' }}>
                  <StatusBadge status={ride.status}>{ride.status}</StatusBadge>
                  <LocationIndicator 
                    isLive={ride.status === 'active'}
                    hasLocation={ride.path && ride.path.length > 0 || ride.distanceTraveled > 0}
                  >
                    <LocationDot isLive={ride.status === 'active'} />
                    {ride.status === 'active' ? 'LIVE' : 
                     (ride.path && ride.path.length > 0 || ride.distanceTraveled > 0) ? 'TRACKED' : 'NO GPS'}
                  </LocationIndicator>
                </div>
              </RideHeader>

              <RideDetails>
                <DetailItem>
                  <DetailLabel>🚲 Bike ID</DetailLabel>
                  <DetailValue>{ride.bikeId}</DetailValue>
                </DetailItem>
                <DetailItem>
                  <DetailLabel>🕐 Duration</DetailLabel>
                  <DetailValue>{formatDuration(ride.duration)}</DetailValue>
                </DetailItem>
                <DetailItem>
                  <DetailLabel>📍 Distance</DetailLabel>
                  <DetailValue>{formatDistance(ride.distanceTraveled)}</DetailValue>
                </DetailItem>
                <DetailItem>
                  <DetailLabel>⚡ Max Speed</DetailLabel>
                  <DetailValue>{ride.maxSpeed ? `${ride.maxSpeed.toFixed(1)} km/h` : 'N/A'}</DetailValue>
                </DetailItem>
                <DetailItem>
                  <DetailLabel>📊 GPS Points</DetailLabel>
                  <DetailValue>{ride.path ? ride.path.length : 0} points</DetailValue>
                </DetailItem>
                <DetailItem>
                  <DetailLabel>🎯 Avg Speed</DetailLabel>
                  <DetailValue>{ride.averageSpeed ? `${ride.averageSpeed.toFixed(1)} km/h` : 'N/A'}</DetailValue>
                </DetailItem>
                <DetailItem>
                  <DetailLabel>🏁 Start Time</DetailLabel>
                  <DetailValue>
                    {ride.startTime ? new Date(ride.startTime).toLocaleString() : 'N/A'}
                  </DetailValue>
                </DetailItem>
                <DetailItem>
                  <DetailLabel>🏁 End Time</DetailLabel>
                  <DetailValue>
                    {ride.endTime ? new Date(ride.endTime).toLocaleString() : 
                     ride.status === 'active' ? 'In Progress' : 'N/A'}
                  </DetailValue>
                </DetailItem>
              </RideDetails>

              <ContactInfo>
                <ContactHeader>👤 Contact & Location Info</ContactHeader>
                <ContactDetails>
                  <div>📧 {ride.user?.email || 'No email'}</div>
                  {ride.user?.phone && (
                    <div>📞 {ride.user.phone}</div>
                  )}
                  <div>🆔 User ID: {ride.userId}</div>
                  <div>🎫 Ride ID: {ride.id}</div>
                  <div style={{ 
                    marginTop: '4px', 
                    padding: '2px 4px', 
                    backgroundColor: ride.status === 'active' ? '#e8f5e8' : '#f0f0f0',
                    borderRadius: '3px',
                    fontSize: '8px'
                  }}>
                    📡 {ride.status === 'active' ? 'Live Tracking Active' : 
                         (ride.path && ride.path.length > 0) ? `${ride.path.length} GPS points recorded` : 
                         'No location data'}
                  </div>
                </ContactDetails>
              </ContactInfo>

              {(ride.status === 'completed' || ride.status === 'active') && (
                <ViewRouteButton onClick={() => viewRoute(ride)}>
                  🗺️ View Route Summary
                </ViewRouteButton>
              )}
            </RideCard>
          ))}
        </RideGrid>
      )}

      {rides.length === 0 && !loading && (
        <EmptyState>
          📭 No rides found matching your criteria.
        </EmptyState>
      )}

      {/* Route Modal */}
      {showRouteModal && selectedRide && (
        <RouteModal onClick={() => setShowRouteModal(false)}>
          <ModalContent onClick={(e) => e.stopPropagation()}>
            <CloseButton onClick={() => setShowRouteModal(false)}>×</CloseButton>
            
            <h3 style={{ marginTop: 0, color: colors.darkGray }}>
              🗺️ Route Summary - {selectedRide.user?.name || 'Unknown User'}
            </h3>
            
            {!routeData ? (
              <div style={{ textAlign: 'center', padding: '40px' }}>
                🔄 Loading route data...
              </div>
            ) : (
              <>
                {/* Static Map Image (if available) */}
                {routeData.mapImageUrl && (
                  <StaticMapImage 
                    src={routeData.mapImageUrl} 
                    alt="Route Map"
                    onError={(e) => {
                      e.target.style.display = 'none';
                    }}
                  />
                )}

                <ModalMap>
                  <MapContainer
                    center={
                      routeData.startLocation ? 
                      { lat: routeData.startLocation.latitude, lng: routeData.startLocation.longitude } :
                      { lat: 14.5995, lng: 120.9842 }
                    }
                    zoom={14}
                  >
                    {/* Route polyline */}
                    {routeData.path && routeData.path.length > 1 && (
                      <Polyline
                        path={routeData.path.map(point => ({
                          lat: point.latitude,
                          lng: point.longitude
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
                          lat: routeData.startLocation.latitude,
                          lng: routeData.startLocation.longitude
                        }}
                        icon={{
                          path: google.maps.SymbolPath.CIRCLE,
                          scale: 8,
                          fillColor: colors.success,
                          fillOpacity: 1,
                          strokeColor: colors.white,
                          strokeWeight: 2
                        }}
                        title="Start Location"
                      />
                    )}

                    {/* End marker */}
                    {routeData.endLocation && (
                      <Marker
                        position={{
                          lat: routeData.endLocation.latitude,
                          lng: routeData.endLocation.longitude
                        }}
                        icon={{
                          path: google.maps.SymbolPath.CIRCLE,
                          scale: 8,
                          fillColor: colors.danger,
                          fillOpacity: 1,
                          strokeColor: colors.white,
                          strokeWeight: 2
                        }}
                        title="End Location"
                      />
                    )}
                  </MapContainer>
                </ModalMap>

                <RouteStats>
                  <StatCard>
                    <StatValue>{formatDistance(routeData.statistics?.totalDistance || 0)}</StatValue>
                    <StatLabel>Total Distance</StatLabel>
                  </StatCard>
                  <StatCard>
                    <StatValue>{(routeData.statistics?.maxSpeed || 0).toFixed(1)} km/h</StatValue>
                    <StatLabel>Max Speed</StatLabel>
                  </StatCard>
                  <StatCard>
                    <StatValue>{(routeData.statistics?.averageSpeed || 0).toFixed(1)} km/h</StatValue>
                    <StatLabel>Avg Speed</StatLabel>
                  </StatCard>
                  <StatCard>
                    <StatValue>{Math.floor((routeData.statistics?.duration || 0) / 60000)}m</StatValue>
                    <StatLabel>Duration</StatLabel>
                  </StatCard>
                  <StatCard>
                    <StatValue>{routeData.statistics?.pointCount || 0}</StatValue>
                    <StatLabel>GPS Points</StatLabel>
                  </StatCard>
                  <StatCard>
                    <StatValue>
                      {routeData.startLocation ? 
                        `${routeData.startLocation.latitude.toFixed(4)}, ${routeData.startLocation.longitude.toFixed(4)}` : 
                        'N/A'}
                    </StatValue>
                    <StatLabel>Start Location</StatLabel>
                  </StatCard>
                </RouteStats>
              </>
            )}
          </ModalContent>
        </RouteModal>
      )}
    </ContentContainer>
  );
};

export default RideHistorySection; 