import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import AnalyticsChart from './AnalyticsChart';
import RatingsChart from './RatingsChart';
import { getAnalyticsData } from '../services/dashboardService';

// Pine green and gray theme colors
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  accent: '#FF8C00'
};

const Container = styled.div`
  padding: 20px;
`;

const Title = styled.h2`
  margin-bottom: 20px;
  color: ${colors.darkGray};
`;

const LoadingMessage = styled.div`
  text-align: center;
  padding: 40px;
  color: ${colors.mediumGray};
`;

const ErrorMessage = styled.div`
  color: #d32f2f;
  background-color: #ffebee;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
  text-align: center;
`;

const LastUpdateTime = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  text-align: right;
  margin-bottom: 5px;
`;

const BikeActivityOverview = () => {
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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdateTime, setLastUpdateTime] = useState(new Date());

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const data = await getAnalyticsData();
        setAnalyticsData(data);
        setLastUpdateTime(new Date());
        setError(null);
      } catch (err) {
        console.error('Error fetching analytics data:', err);
        setError('Failed to load analytics data: ' + err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return <LoadingMessage>Loading activity data...</LoadingMessage>;
  }

  if (error) {
    return <ErrorMessage>{error}</ErrorMessage>;
  }

  return (
    <Container>
      <Title>Ride Activity Overview</Title>
      
      <LastUpdateTime>
        Last updated: {lastUpdateTime.toLocaleTimeString()}
      </LastUpdateTime>
      
      <AnalyticsChart data={analyticsData} />
      
      <RatingsChart data={analyticsData} />
    </Container>
  );
};

export default BikeActivityOverview; 