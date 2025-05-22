import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { AreaChart, Area, ResponsiveContainer, Tooltip, XAxis, YAxis, CartesianGrid, Legend } from 'recharts';

// Enhanced color theme
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C', 
  mediumPineGreen: '#356859',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  chartGradientStart: '#2D5A4C',
  chartGradientEnd: '#B5FFD9',
  accent: '#FD5901'
};

const ChartContainer = styled.div`
  background-color: ${colors.white};
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  margin-bottom: 30px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 6px 25px rgba(0, 0, 0, 0.12);
  }
`;

const ChartTitle = styled.h3`
  margin-bottom: 20px;
  color: ${colors.darkGray};
  font-size: 20px;
  font-weight: 600;
  position: relative;
  
  &:after {
    content: '';
    position: absolute;
    bottom: -8px;
    left: 0;
    width: 40px;
    height: 3px;
    background-color: ${colors.pineGreen};
    border-radius: 2px;
  }
`;

const ChartToggle = styled.div`
  display: flex;
  margin: 20px 0;
  background-color: ${colors.lightGray};
  border-radius: 30px;
  padding: 3px;
  width: fit-content;
`;

const ToggleButton = styled.button`
  padding: 8px 16px;
  background-color: ${props => props.active ? colors.pineGreen : 'transparent'};
  color: ${props => props.active ? colors.white : colors.darkGray};
  border: none;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: ${props => props.active ? '600' : '400'};
  
  &:hover {
    background-color: ${props => props.active ? colors.pineGreen : 'rgba(0,0,0,0.05)'};
    color: ${props => props.active ? colors.white : colors.darkGray};
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

// Custom tooltip for better look and feel
const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    return (
      <div 
        style={{ 
          backgroundColor: '#fff', 
          padding: '10px 15px', 
          border: 'none',
          borderRadius: '8px',
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)'
        }}
      >
        <p style={{ margin: '0 0 5px', fontWeight: 'bold', color: colors.darkGray }}>{label}</p>
        <p style={{ margin: '0', color: colors.pineGreen }}>
          <span style={{ fontWeight: 'bold' }}>{payload[0].value}</span> rides
        </p>
      </div>
    );
  }
  return null;
};

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
      
      // Count rides per day (only if we have rides)
      if (rides && rides.length > 0) {
        rides.forEach(ride => {
          if (!ride.startDate) return;
          
          const rideDate = ride.startDate.toDate ? ride.startDate.toDate() : new Date(ride.startDate);
          // Only count if within the last 7 days
          if ((today - rideDate) / (1000 * 60 * 60 * 24) <= 7) {
            const dayStr = rideDate.toLocaleDateString('en-US', { weekday: 'short' });
            if (dataMap.has(dayStr)) {
              const dayData = dataMap.get(dayStr);
              dayData.count += 1;
            }
          }
        });
      }
      
      // Convert map to array
      return Array.from(dataMap.entries())
        .map(([label, { count, date }]) => ({ 
          name: label,
          value: count,
          date
        }))
        .sort((a, b) => a.date - b.date);
    } else {
      // Last 6 months
      for (let i = 5; i >= 0; i--) {
        const date = new Date(today.getFullYear(), today.getMonth() - i, 1);
        const monthStr = date.toLocaleDateString('en-US', { month: 'short' });
        dataMap.set(monthStr, { count: 0, date });
      }
      
      // Count rides per month (only if we have rides)
      if (rides && rides.length > 0) {
        rides.forEach(ride => {
          if (!ride.startDate) return;
          
          const rideDate = ride.startDate.toDate ? ride.startDate.toDate() : new Date(ride.startDate);
          const monthsAgo = (today.getFullYear() - rideDate.getFullYear()) * 12 + 
                            today.getMonth() - rideDate.getMonth();
          
          if (monthsAgo <= 5 && monthsAgo >= 0) {
            const monthStr = rideDate.toLocaleDateString('en-US', { month: 'short' });
            if (dataMap.has(monthStr)) {
              const monthData = dataMap.get(monthStr);
              monthData.count += 1;
            }
          }
        });
      }
      
      // Convert map to array
      return Array.from(dataMap.entries())
        .map(([label, { count, date }]) => ({ 
          name: label,
          value: count,
          date
        }))
        .sort((a, b) => a.date - b.date);
    }
  };
  
  if (loading) {
    return (
      <ChartContainer>
        <ChartTitle>Ride Activity {chartType === 'weekly' ? '(Last 7 Days)' : '(Last 6 Months)'}</ChartTitle>
        <NoDataMessage>Loading chart data...</NoDataMessage>
      </ChartContainer>
    );
  }
  
  if (error) {
    return (
      <ChartContainer>
        <ChartTitle>Ride Activity {chartType === 'weekly' ? '(Last 7 Days)' : '(Last 6 Months)'}</ChartTitle>
        <ErrorMessage>{error}</ErrorMessage>
      </ChartContainer>
    );
  }
  
  const hasData = chartData.some(item => item.value > 0);
  
  return (
    <ChartContainer>
      <ChartTitle>Ride Activity {chartType === 'weekly' ? '(Last 7 Days)' : '(Last 6 Months)'}</ChartTitle>
      
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
      
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart 
          data={chartData} 
          margin={{ top: 10, right: 30, left: 0, bottom: 5 }}
          animationDuration={1000}
          animationEasing="ease-out"
        >
          <defs>
            <linearGradient id="rideGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor={colors.chartGradientStart} stopOpacity={0.8}/>
              <stop offset="95%" stopColor={colors.chartGradientEnd} stopOpacity={0.2}/>
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
          <XAxis 
            dataKey="name" 
            tick={{ fill: colors.darkGray }} 
            axisLine={{ stroke: colors.mediumGray }} 
          />
          <YAxis 
            tick={{ fill: colors.darkGray }} 
            axisLine={{ stroke: colors.mediumGray }}
            allowDecimals={false}
          />
          <Tooltip content={<CustomTooltip />} />
          <Legend wrapperStyle={{ marginTop: 10 }} />
          <Area
            type="monotone" 
            dataKey="value"
            name="Ride Count" 
            stroke={colors.pineGreen}
            fillOpacity={1}
            fill="url(#rideGradient)"
            strokeWidth={2}
            activeDot={{ r: 6, fill: colors.pineGreen, stroke: colors.white, strokeWidth: 2 }}
            isAnimationActive={true}
          />
        </AreaChart>
      </ResponsiveContainer>
      
      {!hasData && (
        <NoDataMessage>No ride data available</NoDataMessage>
      )}
    </ChartContainer>
  );
};

export default AnalyticsChart; 