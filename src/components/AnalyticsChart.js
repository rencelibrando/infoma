import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { BarChart, Bar, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

// Pine green and gray theme colors
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff'
};

const ChartContainer = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin-bottom: 30px;
`;

const ChartTitle = styled.h3`
  margin-bottom: 20px;
  color: ${colors.darkGray};
  font-size: 18px;
`;

const ChartToggle = styled.div`
  display: flex;
  margin-bottom: 15px;
`;

const ToggleButton = styled.button`
  padding: 6px 12px;
  background-color: ${props => props.active ? colors.pineGreen : colors.lightGray};
  color: ${props => props.active ? colors.white : colors.darkGray};
  border: none;
  border-radius: ${props => props.position === 'left' ? '4px 0 0 4px' : '0 4px 4px 0'};
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background-color: ${props => props.active ? colors.pineGreen : colors.mediumGray};
    color: ${colors.white};
  }
`;

const NoDataMessage = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: ${colors.mediumGray};
  font-style: italic;
`;

const ErrorMessage = styled.div`
  color: #d32f2f;
  background-color: #ffebee;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
  text-align: center;
`;

const AnalyticsChart = ({ data = { rides: [] } }) => {
  const [chartData, setChartData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [chartType, setChartType] = useState('weekly'); // weekly or monthly
  const [error, setError] = useState(null);
  
  useEffect(() => {
    try {
      setLoading(true);
      
      // Use the rides data from props
      const rides = data.rides || [];
      
      // Process chart data based on the selected chart type
      const processedData = processChartData(rides, chartType);
      setChartData(processedData);
      setError(null);
    } catch (error) {
      console.error('Error processing ride data:', error);
      setError('Failed to process ride data: ' + error.message);
    } finally {
      setLoading(false);
    }
  }, [data, chartType]);
  
  // Process chart data based on the selected chart type
  const processChartData = (rides, type) => {
    if (!rides || rides.length === 0) {
      return [];
    }
    
    const today = new Date();
    const dataMap = new Map();
    
    if (type === 'weekly') {
      // Last 7 days
      for (let i = 6; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        const dayStr = date.toLocaleDateString('en-US', { weekday: 'short' });
        dataMap.set(dayStr, { count: 0, date });
      }
      
      // Count rides per day
      rides.forEach(ride => {
        if (!ride.startTime) return;
        
        const rideDate = ride.startTime.toDate ? ride.startTime.toDate() : new Date(ride.startTime);
        // Only count if within the last 7 days
        if ((today - rideDate) / (1000 * 60 * 60 * 24) <= 7) {
          const dayStr = rideDate.toLocaleDateString('en-US', { weekday: 'short' });
          if (dataMap.has(dayStr)) {
            dataMap.get(dayStr).count += 1;
          }
        }
      });
      
      // Convert map to array and sort by date
      return Array.from(dataMap.entries())
        .map(([label, { count, date }]) => ({ name: label, value: count, date }))
        .sort((a, b) => a.date - b.date);
    } else {
      // Last 6 months
      for (let i = 5; i >= 0; i--) {
        const date = new Date(today.getFullYear(), today.getMonth() - i, 1);
        const monthStr = date.toLocaleDateString('en-US', { month: 'short' });
        dataMap.set(monthStr, { count: 0, date });
      }
      
      // Count rides per month
      rides.forEach(ride => {
        if (!ride.startTime) return;
        
        const rideDate = ride.startTime.toDate ? ride.startTime.toDate() : new Date(ride.startTime);
        const monthsAgo = (today.getFullYear() - rideDate.getFullYear()) * 12 + 
                          today.getMonth() - rideDate.getMonth();
        
        if (monthsAgo <= 5 && monthsAgo >= 0) {
          const monthStr = rideDate.toLocaleDateString('en-US', { month: 'short' });
          if (dataMap.has(monthStr)) {
            dataMap.get(monthStr).count += 1;
          }
        }
      });
      
      // Convert map to array and sort by date
      return Array.from(dataMap.entries())
        .map(([label, { count, date }]) => ({ name: label, value: count, date }))
        .sort((a, b) => a.date - b.date);
    }
  };
  
  if (loading) {
    return (
      <ChartContainer>
        <ChartTitle>Ride {chartType === 'weekly' ? 'Activity (Last 7 Days)' : 'Trends (Last 6 Months)'}</ChartTitle>
        <NoDataMessage>Loading chart data...</NoDataMessage>
      </ChartContainer>
    );
  }
  
  if (error) {
    return (
      <ChartContainer>
        <ChartTitle>Ride {chartType === 'weekly' ? 'Activity (Last 7 Days)' : 'Trends (Last 6 Months)'}</ChartTitle>
        <ErrorMessage>{error}</ErrorMessage>
      </ChartContainer>
    );
  }
  
  return (
    <ChartContainer>
      <ChartTitle>Ride {chartType === 'weekly' ? 'Activity (Last 7 Days)' : 'Trends (Last 6 Months)'}</ChartTitle>
      
      <ChartToggle>
        <ToggleButton 
          active={chartType === 'weekly'} 
          position="left"
          onClick={() => setChartType('weekly')}
        >
          Weekly
        </ToggleButton>
        <ToggleButton 
          active={chartType === 'monthly'} 
          position="right"
          onClick={() => setChartType('monthly')}
        >
          Monthly
        </ToggleButton>
      </ChartToggle>
      
      {chartData.length === 0 ? (
        <NoDataMessage>No ride data available</NoDataMessage>
      ) : (
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="value" name="Rides" fill={colors.pineGreen} />
          </BarChart>
        </ResponsiveContainer>
      )}
    </ChartContainer>
  );
};

export default AnalyticsChart; 