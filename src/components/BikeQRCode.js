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
`;

const BikeName = styled.h3`
  margin: 15px 0 5px;
  color: ${colors.darkGray};
  text-align: center;
`;

const BikeId = styled.p`
  margin: 5px 0 15px;
  color: ${colors.mediumGray};
  font-size: 14px;
  text-align: center;
`;

const QRLabel = styled.div`
  margin-top: 15px;
  font-size: 12px;
  color: ${colors.mediumGray};
  text-align: center;
`;

const StatusLabel = styled.div`
  background-color: ${props => props.locked ? '#fff3cd' : '#e8f5e9'};
  color: ${props => props.locked ? '#856404' : '#2e7d32'};
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const StatusIcon = styled.span`
  margin-right: 5px;
  font-size: 14px;
`;

const BikeQRCode = ({ bike }) => {
  if (!bike || !bike.hardwareId) {
    return null;
  }

  const qrValue = JSON.stringify({
    bikeId: bike.id,
    hardwareId: bike.hardwareId,
    name: bike.name,
    isLocked: bike.isLocked || false,
    isAvailable: bike.isAvailable
  });

  return (
    <QRContainer>
      <BikeName>{bike.name}</BikeName>
      <BikeId>Hardware ID: {bike.hardwareId}</BikeId>
      
      <StatusLabel locked={bike.isLocked}>
        <StatusIcon>{bike.isLocked ? '🔒' : '🔓'}</StatusIcon>
        {bike.isLocked ? 'Locked' : 'Unlocked'}
      </StatusLabel>
      
      <QRCodeSVG 
        value={qrValue}
        size={200}
        bgColor={colors.white}
        fgColor={colors.pineGreen}
        level="H"
        includeMargin={true}
      />
      <QRLabel>Scan to identify this bike</QRLabel>
    </QRContainer>
  );
};

export default BikeQRCode; 