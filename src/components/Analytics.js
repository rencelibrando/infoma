import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { db } from '../firebase';
import { collection, getDocs, query, where, orderBy, limit } from 'firebase/firestore';
import AnalyticsChart from './AnalyticsChart';

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

const Analytics = () => {
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalBikes: 0,
    availableBikes: 0,
    rentalCount: 0
  });
  const [recentRentals, setRecentRentals] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        
        // Fetch Users Count
        const usersSnapshot = await getDocs(collection(db, 'users'));
        const usersCount = usersSnapshot.size;
        
        // Fetch Bikes Data
        const bikesSnapshot = await getDocs(collection(db, 'bikes'));
        const bikesCount = bikesSnapshot.size;
        const availableBikes = bikesSnapshot.docs.filter(doc => doc.data().isAvailable).length;
        
        // Fetch Rentals Count
        const rentalsSnapshot = await getDocs(collection(db, 'rentals'));
        const rentalsCount = rentalsSnapshot.size;
        
        // Fetch Recent Rentals
        const recentRentalsQuery = query(
          collection(db, 'rentals'),
          orderBy('createdAt', 'desc'),
          limit(5)
        );
        const recentRentalsSnapshot = await getDocs(recentRentalsQuery);
        const recentRentalsData = recentRentalsSnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data(),
          createdAt: doc.data().createdAt?.toDate() || new Date()
        }));
        
        setStats({
          totalUsers: usersCount,
          totalBikes: bikesCount,
          availableBikes,
          rentalCount: rentalsCount
        });
        
        setRecentRentals(recentRentalsData);
      } catch (error) {
        console.error('Error fetching analytics data:', error);
      } finally {
        setLoading(false);
      }
    };
    
    fetchData();
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

  return (
    <Container>
      <Title>Dashboard Overview</Title>
      
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
          <StatValue color={colors.success}>{stats.availableBikes}</StatValue>
        </StatCard>
        
        <StatCard>
          <StatTitle>Total Rentals</StatTitle>
          <StatValue color={colors.accent}>{stats.rentalCount}</StatValue>
        </StatCard>
      </StatsGrid>
      
      <AnalyticsChart />
      
      <RecentActivitiesCard>
        <SectionTitle>Recent Rentals</SectionTitle>
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
                    {rental.bikeId} rented by {rental.userId || 'Unknown User'}
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