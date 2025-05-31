import React, { useState, useEffect } from 'react';
import styled from 'styled-components';

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
  info: '#2196F3'
};

const DashboardContainer = styled.div`
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, ${colors.white} 0%, #f8f9fa 100%);
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.15);
  padding: 20px;
  z-index: 1000;
  max-height: 50vh;
  overflow-y: auto;
  transition: transform 0.3s ease;
  transform: ${props => props.isMinimized ? 'translateY(calc(100% - 60px))' : 'translateY(0)'};
`;

const DashboardHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  cursor: pointer;
`;

const RideTitle = styled.h3`
  color: ${colors.darkGray};
  margin: 0;
  font-size: 18px;
  font-weight: 600;
`;

const MinimizeButton = styled.button`
  background: none;
  border: none;
  font-size: 20px;
  color: ${colors.mediumGray};
  cursor: pointer;
  padding: 5px;
  border-radius: 50%;
  transition: background-color 0.2s ease;
  
  &:hover {
    background-color: ${colors.lightGray};
  }
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
`;

const StatCard = styled.div`
  background: ${colors.lightGray};
  padding: 15px;
  border-radius: 12px;
  text-align: center;
  transition: transform 0.2s ease;
  
  &:hover {
    transform: translateY(-2px);
  }
`;

const StatValue = styled.div`
  font-size: 24px;
  font-weight: bold;
  color: ${props => props.color || colors.pineGreen};
  margin-bottom: 5px;
`;

const StatLabel = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  font-weight: 500;
`;

const ControlsContainer = styled.div`
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  flex-wrap: wrap;
`;

const ControlButton = styled.button`
  flex: 1;
  min-width: 120px;
  padding: 12px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  
  ${props => {
    switch(props.variant) {
      case 'primary':
        return `
          background: linear-gradient(135deg, ${colors.pineGreen} 0%, ${colors.lightPineGreen} 100%);
          color: ${colors.white};
          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(29, 60, 52, 0.3);
          }
        `;
      case 'danger':
        return `
          background: linear-gradient(135deg, ${colors.danger} 0%, #b71c1c 100%);
          color: ${colors.white};
          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(211, 47, 47, 0.3);
          }
        `;
      case 'warning':
        return `
          background: linear-gradient(135deg, ${colors.warning} 0%, #f57f17 100%);
          color: ${colors.white};
          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(255, 193, 7, 0.3);
          }
        `;
      default:
        return `
          background: ${colors.lightGray};
          color: ${colors.darkGray};
          &:hover {
            background: ${colors.mediumGray};
            color: ${colors.white};
          }
        `;
    }
  }}
`;

const EmergencyButton = styled(ControlButton)`
  background: ${colors.danger};
  color: ${colors.white};
  font-weight: bold;
  animation: ${props => props.isEmergency ? 'pulse 1s infinite' : 'none'};
  
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

const Speedometer = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, ${colors.info} 0%, #1976d2 100%);
  color: ${colors.white};
  border-radius: 50%;
  width: 80px;
  height: 80px;
  margin: 0 auto 15px;
  flex-direction: column;
  box-shadow: 0 4px 15px rgba(33, 150, 243, 0.3);
`;

const SpeedValue = styled.div`
  font-size: 20px;
  font-weight: bold;
`;

const SpeedUnit = styled.div`
  font-size: 10px;
  opacity: 0.8;
`;

const ProgressBar = styled.div`
  width: 100%;
  height: 8px;
  background: ${colors.lightGray};
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 15px;
`;

const ProgressFill = styled.div`
  height: 100%;
  background: linear-gradient(90deg, ${colors.success} 0%, ${colors.pineGreen} 100%);
  width: ${props => props.percentage}%;
  transition: width 0.3s ease;
`;

const RideDashboard = ({ 
  rideData, 
  onPauseRide, 
  onResumeRide, 
  onEndRide, 
  onEmergency,
  isMinimized,
  onToggleMinimize 
}) => {
  const [currentSpeed, setCurrentSpeed] = useState(0);
  const [isEmergencyActive, setIsEmergencyActive] = useState(false);

  useEffect(() => {
    if (rideData?.currentLocation?.speed) {
      setCurrentSpeed(rideData.currentLocation.speed);
    }
  }, [rideData?.currentLocation?.speed]);

  const formatTime = (milliseconds) => {
    const totalSeconds = Math.floor(milliseconds / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    
    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const formatDistance = (meters) => {
    if (meters < 1000) {
      return `${Math.round(meters)}m`;
    }
    return `${(meters / 1000).toFixed(2)}km`;
  };

  const handleEmergency = () => {
    setIsEmergencyActive(true);
    onEmergency();
    
    // Auto-disable emergency state after 30 seconds
    setTimeout(() => {
      setIsEmergencyActive(false);
    }, 30000);
  };

  const calculateProgress = () => {
    if (!rideData?.targetDistance) return 0;
    const currentDistance = rideData?.currentLocation?.totalDistance || 0;
    return Math.min((currentDistance / rideData.targetDistance) * 100, 100);
  };

  if (!rideData) return null;

  const duration = Date.now() - (rideData.startTime?.toDate?.()?.getTime() || rideData.startTime);
  const distance = rideData.currentLocation?.totalDistance || 0;
  const averageSpeed = duration > 0 ? (distance / 1000) / (duration / 3600000) : 0;

  return (
    <DashboardContainer isMinimized={isMinimized}>
      <DashboardHeader onClick={onToggleMinimize}>
        <RideTitle>
          {isMinimized ? 'Ride in Progress' : 'Ride Dashboard'}
        </RideTitle>
        <MinimizeButton>
          {isMinimized ? '▲' : '▼'}
        </MinimizeButton>
      </DashboardHeader>

      {!isMinimized && (
        <>
          {/* Current Speed Display */}
          <Speedometer>
            <SpeedValue>{currentSpeed.toFixed(0)}</SpeedValue>
            <SpeedUnit>km/h</SpeedUnit>
          </Speedometer>

          {/* Progress Bar for Target Distance */}
          {rideData.targetDistance && (
            <>
              <ProgressBar>
                <ProgressFill percentage={calculateProgress()} />
              </ProgressBar>
              <div style={{ 
                textAlign: 'center', 
                fontSize: '12px', 
                color: colors.mediumGray,
                marginBottom: '15px'
              }}>
                {calculateProgress().toFixed(1)}% to destination
              </div>
            </>
          )}

          {/* Ride Statistics */}
          <StatsGrid>
            <StatCard>
              <StatValue color={colors.info}>{formatTime(duration)}</StatValue>
              <StatLabel>Duration</StatLabel>
            </StatCard>
            <StatCard>
              <StatValue color={colors.success}>{formatDistance(distance)}</StatValue>
              <StatLabel>Distance</StatLabel>
            </StatCard>
            <StatCard>
              <StatValue color={colors.purple}>{averageSpeed.toFixed(1)}</StatValue>
              <StatLabel>Avg Speed</StatLabel>
            </StatCard>
            <StatCard>
              <StatValue color={colors.accent}>
                {rideData.currentLocation?.calories || 0}
              </StatValue>
              <StatLabel>Calories</StatLabel>
            </StatCard>
          </StatsGrid>

          {/* Control Buttons */}
          <ControlsContainer>
            {rideData.status === 'active' ? (
              <ControlButton variant="warning" onClick={onPauseRide}>
                ⏸️ Pause Ride
              </ControlButton>
            ) : (
              <ControlButton variant="primary" onClick={onResumeRide}>
                ▶️ Resume Ride
              </ControlButton>
            )}
            
            <ControlButton variant="primary" onClick={onEndRide}>
              🏁 End Ride
            </ControlButton>
          </ControlsContainer>

          {/* Emergency Button */}
          <EmergencyButton 
            isEmergency={isEmergencyActive}
            onClick={handleEmergency}
            disabled={isEmergencyActive}
          >
            {isEmergencyActive ? '🚨 Emergency Sent!' : '🆘 Emergency'}
          </EmergencyButton>

          {/* Additional Info */}
          <div style={{ 
            marginTop: '15px', 
            padding: '10px', 
            background: colors.lightGray, 
            borderRadius: '8px',
            fontSize: '12px',
            color: colors.mediumGray 
          }}>
            <div>Bike ID: {rideData.bikeId}</div>
            <div>Started: {new Date(rideData.startTime?.toDate?.() || rideData.startTime).toLocaleTimeString()}</div>
            {rideData.currentLocation?.accuracy && (
              <div>GPS Accuracy: ±{Math.round(rideData.currentLocation.accuracy)}m</div>
            )}
          </div>
        </>
      )}
    </DashboardContainer>
  );
};

export default RideDashboard; 