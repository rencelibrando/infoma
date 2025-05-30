import React from 'react';
import styled from 'styled-components';
import { QRCodeSVG } from 'qrcode.react';
import { useNavigate } from 'react-router-dom';

// Pine green and gray theme colors (matching BikesList.js)
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
  warning: '#FFC107'
};

const Modal = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

const ModalContent = styled.div`
  background-color: ${colors.white};
  padding: 24px;
  border-radius: 8px;
  position: relative;
  width: 600px;
  max-width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
`;

const CloseButton = styled.button`
  position: absolute;
  top: 16px;
  right: 16px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: ${colors.darkGray};
  
  &:hover {
    color: ${colors.danger};
  }
`;

const BikeHeader = styled.div`
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
`;

const BikeImage = styled.img`
  width: 120px;
  height: 120px;
  border-radius: 8px;
  object-fit: cover;
`;

const BikeInfo = styled.div`
  flex: 1;
`;

const BikeName = styled.h2`
  margin: 0 0 8px 0;
  color: ${colors.darkGray};
`;

const BikeType = styled.div`
  color: ${colors.mediumGray};
  font-size: 16px;
  margin-bottom: 8px;
`;

const BikePrice = styled.div`
  font-size: 18px;
  font-weight: 500;
  color: ${colors.darkGray};
`;

const DetailSection = styled.div`
  margin-bottom: 16px;
`;

const SectionTitle = styled.h3`
  margin: 0 0 12px 0;
  font-size: 16px;
  color: ${colors.darkGray};
`;

const DetailGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
`;

const DetailItem = styled.div`
  margin-bottom: 8px;
`;

const DetailLabel = styled.div`
  font-size: 14px;
  color: ${colors.mediumGray};
  margin-bottom: 4px;
`;

const DetailValue = styled.div`
  font-size: 16px;
  color: ${colors.darkGray};
`;

const StatusContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

const StatusBadge = styled.span`
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  background-color: ${props => 
    props.isInUse ? '#ffebee' : 
    props.isAvailable ? '#e8f5e9' : 
    '#fff3cd'};
  color: ${props => 
    props.isInUse ? '#b71c1c' : 
    props.isAvailable ? '#2e7d32' : 
    '#856404'};
  display: inline-block;
  width: fit-content;
`;

const LockStatus = styled.span`
  font-size: 14px;
  color: ${props => props.locked ? colors.warning : colors.mediumGray};
  display: flex;
  align-items: center;
  gap: 5px;
  
  &:before {
    content: "${props => props.locked ? '🔒' : '🔓'}";
  }
`;

const QRCodeContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background-color: ${colors.lightGray};
  border-radius: 8px;
`;

const QRCodeLabel = styled.div`
  margin: 12px 0;
  font-size: 14px;
  color: ${colors.darkGray};
`;

const ActionSection = styled.div`
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid ${colors.lightGray};
`;

const Button = styled.button`
  padding: 10px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  background-color: ${props => 
    props.danger ? colors.danger : 
    props.edit ? colors.lightPineGreen : 
    props.success ? colors.success :
    props.locked ? colors.warning : colors.pineGreen};
  color: white;
  transition: all 0.3s ease;
  font-size: 14px;
  
  &:hover {
    opacity: 0.85;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const DetailRow = styled.div`
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
`;

const BikeDetailsDialog = ({ 
  bike, 
  onClose, 
  onEdit, 
  onDelete, 
  onToggleLock,
  onStartRide,
  processingBikeAction 
}) => {
  const navigate = useNavigate();

  if (!bike) return null;

  const handleStartRide = () => {
    onStartRide(bike);
    onClose();
  };

  const handleDelete = () => {
    if (window.confirm('Are you sure you want to delete this bike?')) {
      onDelete(bike.id);
      onClose();
    }
  };

  return (
    <Modal onClick={onClose}>
      <ModalContent onClick={e => e.stopPropagation()}>
        <CloseButton onClick={onClose}>×</CloseButton>
        
        <BikeHeader>
          <BikeImage src={bike.imageUrl} alt={bike.name} />
          <BikeInfo>
            <BikeName>{bike.name}</BikeName>
            <BikeType>{bike.type}</BikeType>
            <BikePrice>${parseFloat(bike.priceValue || 0).toFixed(2)}</BikePrice>
            <StatusContainer>
              <StatusBadge 
                isInUse={bike.isInUse}
                isAvailable={bike.isAvailable} 
                isLocked={bike.isLocked}
              >
                {bike.isInUse 
                  ? 'In Use' 
                  : bike.isAvailable
                    ? 'Available' 
                    : 'Unavailable'}
              </StatusBadge>
              {(bike.isAvailable || bike.isInUse) && (
                <LockStatus locked={bike.isLocked}>
                  {bike.isLocked ? 'Locked' : 'Unlocked'}
                </LockStatus>
              )}
            </StatusContainer>
          </BikeInfo>
        </BikeHeader>

        <DetailSection>
          <SectionTitle>Bike Details</SectionTitle>
          <DetailGrid>
            <DetailItem>
              <DetailLabel>Hardware ID</DetailLabel>
              <DetailValue>{bike.hardwareId || 'Not assigned'}</DetailValue>
            </DetailItem>
            <DetailItem>
              <DetailLabel>QR Code</DetailLabel>
              <DetailValue>{bike.qrCode || 'Not assigned'}</DetailValue>
            </DetailItem>
            <DetailItem>
              <DetailLabel>Created On</DetailLabel>
              <DetailValue>
                {bike.createdAt ? new Date(bike.createdAt.toDate()).toLocaleDateString() : 'Unknown'}
              </DetailValue>
            </DetailItem>
            <DetailItem>
              <DetailLabel>Last Updated</DetailLabel>
              <DetailValue>
                {bike.updatedAt ? new Date(bike.updatedAt.toDate()).toLocaleDateString() : 'Unknown'}
              </DetailValue>
            </DetailItem>
            <DetailItem>
              <DetailLabel>Battery Level</DetailLabel>
              <DetailValue>{bike.batteryLevel ? `${bike.batteryLevel}%` : 'Unknown'}</DetailValue>
            </DetailItem>
          </DetailGrid>
        </DetailSection>

        {bike.hardwareId && (
          <QRCodeContainer>
            <QRCodeSVG 
              value={JSON.stringify({bikeId: bike.id, hardwareId: bike.hardwareId})}
              size={200}
              bgColor={colors.white}
              fgColor={colors.pineGreen}
            />
            <QRCodeLabel>Scan to identify this bike</QRCodeLabel>
          </QRCodeContainer>
        )}

        <ActionSection>
          {!bike.isInUse && (
            <Button 
              locked={bike.isLocked}
              onClick={() => onToggleLock(bike)}
              disabled={processingBikeAction === bike.id}
            >
              {processingBikeAction === bike.id
                ? 'Processing...'
                : bike.isLocked
                  ? 'Unlock'
                  : 'Lock'}
            </Button>
          )}
          
          {bike.isAvailable && bike.isLocked && (
            <Button 
              success 
              onClick={handleStartRide}
            >
              Start Ride
            </Button>
          )}
          
          <Button edit onClick={() => onEdit(bike)}>Edit</Button>
          <Button danger onClick={handleDelete}>Delete</Button>
        </ActionSection>
      </ModalContent>
    </Modal>
  );
};

export default BikeDetailsDialog; 