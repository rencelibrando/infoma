import React, { useState } from 'react';
import styled from 'styled-components';

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
  warning: '#FFA000',
  error: '#F44336'
};

const VerificationContainer = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 24px;
`;

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid ${colors.lightGray};
`;

const Title = styled.h2`
  margin: 0;
  color: ${colors.darkGray};
`;

const CloseButton = styled.button`
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: ${colors.mediumGray};
  
  &:hover {
    color: ${colors.darkGray};
  }
`;

const Section = styled.div`
  margin-bottom: 24px;
`;

const SectionTitle = styled.h3`
  margin: 0 0 12px 0;
  color: ${colors.darkGray};
  font-size: 16px;
`;

const UserInfoGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
`;

const InfoItem = styled.div`
  padding: 12px;
  background-color: ${colors.lightGray};
  border-radius: 6px;
`;

const Label = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  margin-bottom: 4px;
`;

const Value = styled.div`
  font-weight: 500;
  color: ${colors.darkGray};
`;

const IdImageContainer = styled.div`
  margin: 20px 0;
  text-align: center;
`;

const IdImage = styled.img`
  max-width: 100%;
  max-height: 400px;
  border-radius: 8px;
  border: 1px solid ${colors.lightGray};
`;

const ActionButtons = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid ${colors.lightGray};
`;

const Button = styled.button`
  padding: 10px 20px;
  border-radius: 4px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  
  background-color: ${props => {
    if (props.approve) return colors.success;
    if (props.decline) return colors.error;
    if (props.secondary) return colors.lightGray;
    return colors.pineGreen;
  }};
  
  color: ${props => props.secondary ? colors.darkGray : colors.white};
  
  &:hover {
    opacity: 0.9;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const StatusBadge = styled.span`
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  margin-left: 10px;
  background-color: ${props => {
    if (props.status === 'approved') return 'rgba(76, 175, 80, 0.1)';
    if (props.status === 'pending') return 'rgba(255, 160, 0, 0.1)';
    if (props.status === 'declined') return 'rgba(244, 67, 54, 0.1)';
    return 'rgba(0, 0, 0, 0.05)';
  }};
  color: ${props => {
    if (props.status === 'approved') return colors.success;
    if (props.status === 'pending') return colors.warning;
    if (props.status === 'declined') return colors.error;
    return colors.mediumGray;
  }};
`;

const NoIdContainer = styled.div`
  padding: 40px;
  text-align: center;
  background-color: ${colors.lightGray};
  border-radius: 8px;
  color: ${colors.mediumGray};
`;

const UserIdVerification = ({ user, onClose, onStatusChange }) => {
  const [isLoading, setIsLoading] = useState(false);
  
  if (!user) {
    return null;
  }
  
  const handleStatusChange = async (status) => {
    setIsLoading(true);
    try {
      await onStatusChange(status);
    } catch (error) {
      console.error('Error updating status:', error);
      alert('Failed to update verification status');
    } finally {
      setIsLoading(false);
    }
  };
  
  const formatDateFromTimestamp = (timestamp) => {
    if (!timestamp) return 'N/A';
    
    // Handle different timestamp formats
    let date;
    if (typeof timestamp === 'number') {
      date = new Date(timestamp);
    } else if (timestamp.seconds) {
      // Firestore timestamp
      date = new Date(timestamp.seconds * 1000);
    } else {
      return 'Invalid date';
    }
    
    return date.toLocaleString();
  };
  
  return (
    <VerificationContainer>
      <Header>
        <div>
          <Title>User ID Verification</Title>
          {user.idVerificationStatus && (
            <StatusBadge status={user.idVerificationStatus}>
              {user.idVerificationStatus.charAt(0).toUpperCase() + user.idVerificationStatus.slice(1)}
            </StatusBadge>
          )}
        </div>
        <CloseButton onClick={onClose}>&times;</CloseButton>
      </Header>
      
      <Section>
        <SectionTitle>User Information</SectionTitle>
        <UserInfoGrid>
          <InfoItem>
            <Label>Name</Label>
            <Value>{user.fullName || user.displayName || 'N/A'}</Value>
          </InfoItem>
          <InfoItem>
            <Label>Email</Label>
            <Value>{user.email || 'N/A'}</Value>
          </InfoItem>
          <InfoItem>
            <Label>Age</Label>
            <Value>{user.age || 'N/A'}</Value>
          </InfoItem>
          <InfoItem>
            <Label>Phone</Label>
            <Value>{user.phoneNumber || 'N/A'}</Value>
          </InfoItem>
          <InfoItem>
            <Label>Account Created</Label>
            <Value>{formatDateFromTimestamp(user.createdAt) || 'N/A'}</Value>
          </InfoItem>
          <InfoItem>
            <Label>Last Updated</Label>
            <Value>{formatDateFromTimestamp(user.lastUpdated) || 'N/A'}</Value>
          </InfoItem>
        </UserInfoGrid>
      </Section>
      
      <Section>
        <SectionTitle>ID Document</SectionTitle>
        {user.idUrl ? (
          <IdImageContainer>
            <IdImage src={user.idUrl} alt="User ID" />
          </IdImageContainer>
        ) : (
          <NoIdContainer>
            <p>No ID document has been uploaded by this user</p>
          </NoIdContainer>
        )}
      </Section>
      
      {user.idUrl && (
        <ActionButtons>
          <Button 
            secondary 
            onClick={onClose}
            disabled={isLoading}
          >
            Cancel
          </Button>
          
          <Button 
            decline
            onClick={() => handleStatusChange('declined')}
            disabled={isLoading || user.idVerificationStatus === 'declined'}
          >
            Decline ID
          </Button>
          
          <Button 
            approve
            onClick={() => handleStatusChange('approved')}
            disabled={isLoading || user.idVerificationStatus === 'approved'}
          >
            Approve ID
          </Button>
        </ActionButtons>
      )}
    </VerificationContainer>
  );
};

export default UserIdVerification; 