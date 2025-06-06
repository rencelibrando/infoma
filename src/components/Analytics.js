import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import AnalyticsChart from './AnalyticsChart';
import RatingsChart from './RatingsChart';
import { getAnalyticsData, subscribeToAnalytics, clearReviewsCache, clearAllCache } from '../services/dashboardService';
import { useAnalytics } from '../context/AnalyticsContext';

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
  warning: '#FFC107',
  danger: '#F44336'
};

const Container = styled.div`
  padding: 20px;
`;

const Title = styled.h2`
  margin-bottom: 30px;
  color: ${colors.darkGray};
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
`;

const StatCard = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  }
`;

const StatTitle = styled.div`
  font-size: 14px;
  color: ${colors.mediumGray};
  margin-bottom: 10px;
`;

const StatValue = styled.div`
  font-size: 28px;
  font-weight: bold;
  color: ${props => props.color || colors.pineGreen};
`;

const RecentActivitiesCard = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
`;

const SectionTitle = styled.h3`
  margin-bottom: 20px;
  color: ${colors.darkGray};
  font-size: 18px;
`;

const ActivityList = styled.ul`
  list-style-type: none;
  padding: 0;
`;

const ActivityItem = styled.li`
  padding: 12px 0;
  border-bottom: 1px solid ${colors.lightGray};
  display: flex;
  align-items: center;
  
  &:last-child {
    border-bottom: none;
  }
`;

const ActivityIcon = styled.div`
  width: 40px;
  height: 40px;
  background-color: ${props => props.bgColor || colors.lightPineGreen};
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
`;

const ActivityContent = styled.div`
  flex: 1;
`;

const ActivityTitle = styled.div`
  font-weight: 500;
  color: ${colors.darkGray};
`;

const ActivityTime = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  margin-top: 5px;
`;

const LoadingMessage = styled.div`
  text-align: center;
  padding: 40px;
  color: ${colors.mediumGray};
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

const RefreshButton = styled.button`
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  border: none;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  margin-left: auto;
  margin-bottom: 10px;
  
  &:hover {
    background-color: ${colors.lightPineGreen};
  }
  
  span {
    margin-right: 5px;
  }
`;

const DebugSection = styled.div`
  background-color: #f0f8ff;
  border: 2px solid ${colors.pineGreen};
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
  font-family: monospace;
  font-size: 12px;
`;

const DebugTitle = styled.h4`
  color: ${colors.pineGreen};
  margin: 0 0 10px 0;
`;

const DebugInfo = styled.div`
  margin: 5px 0;
  color: ${colors.darkGray};
`;

const LoadingContainer = styled.div`
  text-align: center;
  padding: 40px;
`;

const Spinner = styled.div`
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-top: 4px solid ${colors.pineGreen};
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;

const LoadingText = styled.div`
  margin-top: 10px;
  color: ${colors.pineGreen};
`;

const ErrorContainer = styled.div`
  text-align: center;
  padding: 40px;
  color: ${colors.danger};
`;

const ErrorText = styled.div`
  margin-top: 10px;
`;

const Analytics = () => {
  const { data, loading, error } = useAnalytics();
  const [recentRentals, setRecentRentals] = useState([]);
  const [lastUpdateTime, setLastUpdateTime] = useState(new Date());
  const [showUpdateIndicator, setShowUpdateIndicator] = useState(false);

  useEffect(() => {
    if (data) {
      // Set recent rentals
      const recentRides = data.rides
        .sort((a, b) => (b.startDate?.seconds || 0) - (a.startDate?.seconds || 0))
        .slice(0, 5)
        .map(ride => ({
          id: ride.id,
          bikeId: ride.bikeId,
          userId: ride.userId,
          status: ride.isActive ? 'active' : 'completed',
          createdAt: ride.startDate?.toDate() || new Date()
        }));
      
      setRecentRentals(recentRides);
      setLastUpdateTime(new Date());
      setShowUpdateIndicator(true);
      
      // Hide update indicator after 3 seconds
      setTimeout(() => {
        setShowUpdateIndicator(false);
      }, 3000);
    }
  }, [data]);

  if (loading) {
    return (
      <LoadingContainer>
        <Spinner />
        <LoadingText>Loading analytics...</LoadingText>
      </LoadingContainer>
    );
  }

  if (error) {
    return (
      <ErrorContainer>
        <ErrorText>Error loading analytics: {error}</ErrorText>
      </ErrorContainer>
    );
  }

  const { stats } = data;

  return (
    <Container>
      <Title>Dashboard Overview</Title>
      
      {showUpdateIndicator && (
        <RefreshIndicator>
          <span>🔄</span> Dashboard data updated
        </RefreshIndicator>
      )}
      
      <LastUpdateTime>
        Last updated: {lastUpdateTime.toLocaleTimeString()}
      </LastUpdateTime>
      
      <RefreshButton onClick={() => {}}>
        <span>🔄</span> Refresh Data
      </RefreshButton>
      
      {stats.totalBikes > 0 ? (
        <StatsGrid>
          <StatCard>
            <StatTitle>Total Bikes</StatTitle>
            <StatValue>{stats.totalBikes}</StatValue>
          </StatCard>
          
          <StatCard>
            <StatTitle>Active Bikes</StatTitle>
            <StatValue color={colors.success}>{stats.activeBikes}</StatValue>
          </StatCard>
          
          <StatCard>
            <StatTitle>Bikes in Use</StatTitle>
            <StatValue color={colors.accent}>{stats.inUseBikes}</StatValue>
          </StatCard>
          
          <StatCard>
            <StatTitle>Maintenance Bikes</StatTitle>
            <StatValue color={colors.warning}>{stats.maintenanceBikes}</StatValue>
          </StatCard>
          
          <StatCard>
            <StatTitle>Active Rides</StatTitle>
            <StatValue color={colors.accent}>{stats.activeRides}</StatValue>
          </StatCard>
          
          <StatCard>
            <StatTitle>Total Rides</StatTitle>
            <StatValue>{stats.totalRides}</StatValue>
          </StatCard>
          
          <StatCard>
            <StatTitle>Total Users</StatTitle>
            <StatValue>{stats.totalUsers}</StatValue>
          </StatCard>
          
          <StatCard>
            <StatTitle>Average Rating</StatTitle>
            <StatValue color="#FFB400">
              {stats.averageRating} <span style={{ fontSize: '20px' }}>⭐</span>
            </StatValue>
          </StatCard>
        </StatsGrid>
      ) : (
        <p>No bikes found in the analytics data.</p>
      )}
      
      <AnalyticsChart data={data} />
      
      <RatingsChart data={data} />
      
      <RecentActivitiesCard>
        <SectionTitle>Recent Rides</SectionTitle>
        {recentRentals.length === 0 ? (
          <p>No recent rental activities found.</p>
        ) : (
          <ActivityList>
            {recentRentals.map(rental => (
              <ActivityItem key={rental.id}>
                <ActivityIcon bgColor={rental.status === 'active' ? colors.success : colors.accent}>
                  {rental.status === 'active' ? 'A' : 'C'}
                </ActivityIcon>
                <ActivityContent>
                  <ActivityTitle>
                    Bike {rental.bikeId} rented by {rental.userId || 'Unknown User'}
                  </ActivityTitle>
                  <ActivityTime>
                    {new Date(rental.createdAt).toLocaleString()}
                  </ActivityTime>
                </ActivityContent>
              </ActivityItem>
            ))}
          </ActivityList>
        )}
      </RecentActivitiesCard>
    </Container>
  );
};

export default Analytics; 