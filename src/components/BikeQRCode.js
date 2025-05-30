import React from 'react';
import { QRCodeSVG } from 'qrcode.react';
import styled from 'styled-components';

// Pine green and gray theme colors
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  warning: '#FFC107',
  success: '#4CAF50',
};

const QRContainer = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  min-width: 300px;
`;

const BikeName = styled.h3`
  margin: 15px 0 5px;
  color: ${colors.darkGray};
  text-align: center;
  font-size: 18px;
  font-weight: 600;
`;

const BikeId = styled.p`
  margin: 5px 0 5px;
  color: ${colors.mediumGray};
  font-size: 14px;
  text-align: center;
  font-family: 'Courier New', monospace;
`;

const QRCodeValue = styled.p`
  margin: 5px 0 15px;
  color: ${colors.pineGreen};
  font-size: 16px;
  text-align: center;
  font-family: 'Courier New', monospace;
  font-weight: bold;
  word-break: break-all;
  max-width: 200px;
`;

const QRLabel = styled.div`
  margin-top: 15px;
  font-size: 12px;
  color: ${colors.mediumGray};
  text-align: center;
`;

const StatusContainer = styled.div`
  display: flex;
  gap: 10px;
  margin: 10px 0;
  flex-wrap: wrap;
  justify-content: center;
`;

const StatusLabel = styled.div`
  background-color: ${props => {
    if (props.type === 'locked') return props.value ? colors.warning : colors.success;
    if (props.type === 'available') return props.value ? colors.success : colors.mediumGray;
    if (props.type === 'inUse') return props.value ? '#ff5722' : colors.success;
    return colors.lightGray;
  }};
  color: ${colors.white};
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
`;

const StatusIcon = styled.span`
  margin-right: 5px;
  font-size: 12px;
`;

const WarningText = styled.p`
  margin: 10px 0;
  color: colors.warning;
  font-size: 12px;
  text-align: center;
  font-style: italic;
`;

const BikeQRCode = ({ bike }) => {
  if (!bike) {
    return null;
  }

  // Get the effective QR code (prefer qrCode over hardwareId)
  const effectiveQRCode = bike.qrCode || bike.hardwareId;
  
  if (!effectiveQRCode) {
    return (
      <QRContainer>
        <BikeName>{bike.name}</BikeName>
        <WarningText>No QR code assigned to this bike</WarningText>
      </QRContainer>
    );
  }

  // Generate QR code value - use simple string format for mobile app compatibility
  const qrValue = effectiveQRCode;

  return (
    <QRContainer>
      <BikeName>{bike.name}</BikeName>
      <BikeId>Bike ID: {bike.id}</BikeId>
      <QRCodeValue>{effectiveQRCode}</QRCodeValue>
      
      <StatusContainer>
        <StatusLabel type="locked" value={bike.isLocked}>
          <StatusIcon>{bike.isLocked ? '🔒' : '🔓'}</StatusIcon>
          {bike.isLocked ? 'Locked' : 'Unlocked'}
        </StatusLabel>
        
        <StatusLabel type="available" value={bike.isAvailable}>
          <StatusIcon>{bike.isAvailable ? '✅' : '❌'}</StatusIcon>
          {bike.isAvailable ? 'Available' : 'Unavailable'}
        </StatusLabel>
        
        <StatusLabel type="inUse" value={bike.isInUse}>
          <StatusIcon>{bike.isInUse ? '🚴' : '🅿️'}</StatusIcon>
          {bike.isInUse ? 'In Use' : 'Parked'}
        </StatusLabel>
      </StatusContainer>
      
      <QRCodeSVG 
        value={qrValue}
        size={200}
        bgColor={colors.white}
        fgColor={colors.pineGreen}
        level="H"
        includeMargin={true}
      />
      
      <QRLabel>
        Scan this QR code with the mobile app to unlock
      </QRLabel>
      
      {bike.qrCode && bike.hardwareId && bike.qrCode !== bike.hardwareId && (
        <WarningText>
          Note: This bike has both QR code and hardware ID. Using QR code.
        </WarningText>
      )}
      
      {!bike.qrCode && bike.hardwareId && (
        <WarningText>
          Note: Using legacy hardware ID. Consider updating to new QR code format.
        </WarningText>
      )}
    </QRContainer>
  );
};

export default BikeQRCode; 