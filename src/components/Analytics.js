import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import AnalyticsChart from './AnalyticsChart';
import { getAnalyticsData, subscribeToAnalytics } from '../services/dashboardService';

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

const Analytics = () => {
  const [analyticsData, setAnalyticsData] = useState({
    bikes: [],
    users: [],
    rides: [],
    reviews: [],
    stats: {
      totalBikes: 0,
      activeBikes: 0,
      inUseBikes: 0,
      maintenanceBikes: 0,
      totalUsers: 0,
      verifiedUsers: 0,
      activeRides: 0,
      totalRides: 0,
      totalReviews: 0,
      averageRating: 0
    }
  });
  const [recentRentals, setRecentRentals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [lastUpdateTime, setLastUpdateTime] = useState(new Date());
  const [showUpdateIndicator, setShowUpdateIndicator] = useState(false);

  useEffect(() => {
    setLoading(true);
    
    // Initial data fetch
    const initialFetch = async () => {
      try {
        const data = await getAnalyticsData();
        setAnalyticsData(data);
        
        // Set recent rentals
        const recentRides = data.rides
          .sort((a, b) => (b.startTime?.seconds || 0) - (a.startTime?.seconds || 0))
          .slice(0, 5)
          .map(ride => ({
            id: ride.id,
            bikeId: ride.bikeId,
            userId: ride.userId,
            status: ride.isActive ? 'active' : 'completed',
            createdAt: ride.startTime?.toDate() || new Date()
          }));
        
        setRecentRentals(recentRides);
        setLastUpdateTime(new Date());
        setLoading(false);
      } catch (error) {
        console.error('Error fetching analytics data:', error);
        setLoading(false);
      }
    };
    
    initialFetch();
    
    // Set up real-time listener
    const unsubscribe = subscribeToAnalytics((data) => {
      setAnalyticsData(data);
      
      // Set recent rentals
      const recentRides = data.rides
        .sort((a, b) => (b.startTime?.seconds || 0) - (a.startTime?.seconds || 0))
        .slice(0, 5)
        .map(ride => ({
          id: ride.id,
          bikeId: ride.bikeId,
          userId: ride.userId,
          status: ride.isActive ? 'active' : 'completed',
          createdAt: ride.startTime?.toDate() || new Date()
        }));
      
      setRecentRentals(recentRides);
      setLastUpdateTime(new Date());
      setShowUpdateIndicator(true);
      
      // Hide update indicator after 3 seconds
      setTimeout(() => {
        setShowUpdateIndicator(false);
      }, 3000);
    });
    
    // Cleanup listener on component unmount
    return () => {
      unsubscribe();
    };
  }, []);

  const formatDate = (date) => {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return <LoadingMessage>Loading analytics data...</LoadingMessage>;
  }

  const { stats } = analyticsData;

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
      
      <StatsGrid>
        <StatCard>
          <StatTitle>Total Users</StatTitle>
          <StatValue>{stats.totalUsers}</StatValue>
        </StatCard>
        
        <StatCard>
          <StatTitle>Total Bikes</StatTitle>
          <StatValue>{stats.totalBikes}</StatValue>
        </StatCard>
        
        <StatCard>
          <StatTitle>Available Bikes</StatTitle>
          <StatValue color={colors.success}>{stats.activeBikes}</StatValue>
        </StatCard>
        
        <StatCard>
          <StatTitle>In-Use Bikes</StatTitle>
          <StatValue color={colors.accent}>{stats.inUseBikes}</StatValue>
        </StatCard>
        
        <StatCard>
          <StatTitle>Bikes in Maintenance</StatTitle>
          <StatValue color={colors.warning}>{stats.maintenanceBikes}</StatValue>
        </StatCard>
        
        <StatCard>
          <StatTitle>Active Rides</StatTitle>
          <StatValue color={colors.success}>{stats.activeRides}</StatValue>
        </StatCard>
        
        <StatCard>
          <StatTitle>Total Rides</StatTitle>
          <StatValue color={colors.accent}>{stats.totalRides}</StatValue>
        </StatCard>
        
        <StatCard>
          <StatTitle>Avg. Rating</StatTitle>
          <StatValue color={colors.pineGreen}>{stats.averageRating} ★</StatValue>
        </StatCard>
      </StatsGrid>
      
      <AnalyticsChart data={analyticsData} />
      
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
                    {formatDate(rental.createdAt)}
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