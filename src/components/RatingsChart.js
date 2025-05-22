import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { LineChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis, CartesianGrid, Legend, ReferenceLine } from 'recharts';

// Enhanced color theme matching AnalyticsChart
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  mediumPineGreen: '#356859',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  goldYellow: '#FFC107',
  lineColor: '#2D5A4C',
  accent: '#FD5901',
  averageLine: '#757575'
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

const AverageIndicator = styled.div`
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  font-size: 14px;
  
  span {
    display: inline-flex;
    align-items: center;
    margin-left: 5px;
    font-weight: 600;
    color: ${colors.pineGreen};
  }
  
  .star {
    color: ${colors.goldYellow};
    margin-right: 4px;
  }
`;

const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    const rating = payload[0].value;
    const countText = payload[1] ? `${payload[1].value} reviews` : '';
    
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
        <p style={{ margin: '0 0 3px', color: colors.pineGreen }}>
          <span style={{ color: colors.goldYellow, marginRight: '4px' }}>★</span>
          <span style={{ fontWeight: 'bold' }}>{rating?.toFixed(1) || 'No data'}</span>
        </p>
        {countText && (
          <p style={{ margin: 0, fontSize: '12px', color: colors.mediumGray }}>
            {countText}
          </p>
        )}
      </div>
    );
  }
  return null;
};

const RatingsChart = ({ data = { reviews: [] } }) => {
  const [chartData, setChartData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [chartType, setChartType] = useState('weekly'); // weekly or monthly
  const [error, setError] = useState(null);
  const [averageRating, setAverageRating] = useState(0);
  const [totalReviewCount, setTotalReviewCount] = useState(0);
  
  useEffect(() => {
    try {
      setLoading(true);
      
      // Use the reviews data from props
      const reviews = data.reviews || [];
      setTotalReviewCount(reviews.length);
      
      // Calculate overall average rating
      if (reviews.length > 0) {
        const totalRating = reviews.reduce((sum, review) => sum + review.rating, 0);
        setAverageRating(totalRating / reviews.length);
      } else {
        setAverageRating(0);
      }
      
      // Process chart data based on the selected chart type
      const processedData = processChartData(reviews, chartType);
      setChartData(processedData);
      setError(null);
    } catch (error) {
      console.error('Error processing review data:', error);
      setError('Failed to process review data: ' + error.message);
    } finally {
      setLoading(false);
    }
  }, [data, chartType]);
  
  // Process chart data based on the selected chart type
  const processChartData = (reviews, type) => {
    const today = new Date();
    const dataMap = new Map();
    
    if (type === 'weekly') {
      // Last 7 days
      for (let i = 6; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        const dayStr = date.toLocaleDateString('en-US', { weekday: 'short' });
        dataMap.set(dayStr, { totalRating: 0, count: 0, date });
      }
      
      // Aggregate ratings per day (only if we have reviews)
      if (reviews && reviews.length > 0) {
        reviews.forEach(review => {
          if (!review.timestamp) return;
          
          const reviewDate = review.timestamp.toDate ? review.timestamp.toDate() : new Date(review.timestamp);
          // Only count if within the last 7 days
          if ((today - reviewDate) / (1000 * 60 * 60 * 24) <= 7) {
            const dayStr = reviewDate.toLocaleDateString('en-US', { weekday: 'short' });
            if (dataMap.has(dayStr)) {
              const dayData = dataMap.get(dayStr);
              dayData.totalRating += review.rating;
              dayData.count += 1;
            }
          }
        });
      }
      
      // Convert map to array and calculate average ratings
      return Array.from(dataMap.entries())
        .map(([label, { totalRating, count, date }]) => ({
          name: label,
          rating: count > 0 ? totalRating / count : null,
          count,
          date
        }))
        .sort((a, b) => a.date - b.date);
    } else {
      // Last 6 months
      for (let i = 5; i >= 0; i--) {
        const date = new Date(today.getFullYear(), today.getMonth() - i, 1);
        const monthStr = date.toLocaleDateString('en-US', { month: 'short' });
        dataMap.set(monthStr, { totalRating: 0, count: 0, date });
      }
      
      // Aggregate ratings per month (only if we have reviews)
      if (reviews && reviews.length > 0) {
        reviews.forEach(review => {
          if (!review.timestamp) return;
          
          const reviewDate = review.timestamp.toDate ? review.timestamp.toDate() : new Date(review.timestamp);
          const monthsAgo = (today.getFullYear() - reviewDate.getFullYear()) * 12 + 
                            today.getMonth() - reviewDate.getMonth();
          
          if (monthsAgo <= 5 && monthsAgo >= 0) {
            const monthStr = reviewDate.toLocaleDateString('en-US', { month: 'short' });
            if (dataMap.has(monthStr)) {
              const monthData = dataMap.get(monthStr);
              monthData.totalRating += review.rating;
              monthData.count += 1;
            }
          }
        });
      }
      
      // Convert map to array and calculate average ratings
      return Array.from(dataMap.entries())
        .map(([label, { totalRating, count, date }]) => ({
          name: label,
          rating: count > 0 ? totalRating / count : null,
          count,
          date
        }))
        .sort((a, b) => a.date - b.date);
    }
  };
  
  if (loading) {
    return (
      <ChartContainer>
        <ChartTitle>Rating Trends {chartType === 'weekly' ? '(Last 7 Days)' : '(Last 6 Months)'}</ChartTitle>
        <NoDataMessage>Loading chart data...</NoDataMessage>
      </ChartContainer>
    );
  }
  
  if (error) {
    return (
      <ChartContainer>
        <ChartTitle>Rating Trends {chartType === 'weekly' ? '(Last 7 Days)' : '(Last 6 Months)'}</ChartTitle>
        <ErrorMessage>{error}</ErrorMessage>
      </ChartContainer>
    );
  }
  
  return (
    <ChartContainer>
      <ChartTitle>Rating Trends {chartType === 'weekly' ? '(Last 7 Days)' : '(Last 6 Months)'}</ChartTitle>
      
      {averageRating > 0 && (
        <AverageIndicator>
          Average Rating: 
          <span>
            <span className="star">★</span> {averageRating.toFixed(1)}
          </span>
          <span style={{ fontSize: '13px', marginLeft: '8px', fontWeight: 'normal', color: colors.mediumGray }}>
            ({totalReviewCount} reviews)
          </span>
        </AverageIndicator>
      )}
      
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
        <LineChart
          data={chartData}
          margin={{ top: 10, right: 30, left: 0, bottom: 5 }}
          animationDuration={1000}
          animationEasing="ease-out"
        >
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
          <XAxis dataKey="name" tick={{ fill: colors.darkGray }} axisLine={{ stroke: colors.mediumGray }} />
          <YAxis 
            domain={[0, 5]} 
            ticks={[0, 1, 2, 3, 4, 5]}
            tick={{ fill: colors.darkGray }} 
            axisLine={{ stroke: colors.mediumGray }} 
          />
          <Tooltip content={<CustomTooltip />} />
          <Legend wrapperStyle={{ marginTop: 10 }} />
          
          {averageRating > 0 && (
            <ReferenceLine 
              y={averageRating} 
              stroke={colors.averageLine} 
              strokeDasharray="3 3" 
              strokeWidth={1}
              label={{ 
                value: `Average: ${averageRating.toFixed(1)}`, 
                position: 'right', 
                fill: colors.averageLine,
                fontSize: 12
              }} 
            />
          )}
          
          <Line
            type="monotone"
            dataKey="rating"
            name="Rating"
            stroke={colors.lineColor}
            dot={{ r: 4, strokeWidth: 2, stroke: colors.white }}
            activeDot={{ r: 6, fill: colors.pineGreen, stroke: colors.white, strokeWidth: 2 }}
            strokeWidth={2}
            connectNulls={true}
            isAnimationActive={true}
          />
          
          <Line
            type="monotone"
            dataKey="count"
            name="Reviews"
            stroke={colors.accent}
            strokeDasharray="3 3"
            dot={{ r: 3, strokeWidth: 2, stroke: colors.white }}
            activeDot={{ r: 5, fill: colors.accent, stroke: colors.white, strokeWidth: 2 }}
            strokeWidth={1.5}
            connectNulls={true}
            isAnimationActive={true}
            hide={!chartData.some(d => d.count > 0)}
          />
        </LineChart>
      </ResponsiveContainer>
      
      {!chartData.some(data => data.rating !== null) && (
        <NoDataMessage>No rating data available</NoDataMessage>
      )}
    </ChartContainer>
  );
};

export default RatingsChart; 