import React, { useState } from 'react';
import styled from 'styled-components';
import UserIdVerification from './UserIdVerification';

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
    color: ${colors.error};
  }
`;

const UserHeader = styled.div`
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
`;

const UserAvatar = styled.div`
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background-color: ${colors.lightGray};
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: ${colors.mediumGray};
  overflow: hidden;
`;

const UserImage = styled.img`
  width: 100%;
  height: 100%;
  object-fit: cover;
`;

const UserInfo = styled.div`
  flex: 1;
`;

const UserName = styled.h2`
  margin: 0 0 8px 0;
  color: ${colors.darkGray};
`;

const UserEmail = styled.div`
  color: ${colors.mediumGray};
  font-size: 16px;
  margin-bottom: 8px;
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

const StatusBadge = styled.span`
  display: inline-block;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
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

const Tabs = styled.div`
  display: flex;
  border-bottom: 1px solid ${colors.lightGray};
  margin-bottom: 16px;
`;

const Tab = styled.button`
  padding: 8px 16px;
  background: none;
  border: none;
  border-bottom: 2px solid ${props => props.active ? colors.pineGreen : 'transparent'};
  color: ${props => props.active ? colors.pineGreen : colors.mediumGray};
  font-weight: ${props => props.active ? 'bold' : 'normal'};
  cursor: pointer;
  
  &:hover {
    color: ${colors.pineGreen};
  }
`;

const RoleSelect = styled.select`
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  width: 100%;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
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
    props.danger ? colors.error : 
    props.secondary ? colors.lightGray : 
    props.success ? colors.success :
    colors.pineGreen};
  color: ${props => props.secondary ? colors.darkGray : colors.white};
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

const UserDetailsDialog = ({ 
  user, 
  onClose, 
  onRoleChange,
  onVerificationStatusChange 
}) => {
  const [activeTab, setActiveTab] = useState('info');
  const [showIdVerification, setShowIdVerification] = useState(false);
  const [editingRole, setEditingRole] = useState(false);
  const [selectedRole, setSelectedRole] = useState(user?.role || 'User');

  if (!user) return null;

  const handleRoleChange = async () => {
    await onRoleChange(user.id, selectedRole);
    setEditingRole(false);
  };

  const handleVerificationClick = () => {
    if (user.idUrl) {
      setShowIdVerification(true);
    } else {
      alert("User hasn't uploaded an ID document");
    }
  };

  const getInitials = (name) => {
    if (!name) return '?';
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    
    try {
      const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
      return date.toLocaleDateString();
    } catch (e) {
      return 'Invalid date';
    }
  };

  return (
    <Modal onClick={onClose}>
      <ModalContent onClick={e => e.stopPropagation()}>
        <CloseButton onClick={onClose}>×</CloseButton>
        
        <UserHeader>
          <UserAvatar>
            {user.profilePictureUrl ? 
              <UserImage src={user.profilePictureUrl} alt={user.fullName || user.displayName} /> :
              user.photoURL ? 
                <UserImage src={user.photoURL} alt={user.fullName || user.displayName} /> :
                getInitials(user.fullName || user.displayName)
            }
          </UserAvatar>
          <UserInfo>
            <UserName>{user.fullName || user.displayName || 'Unnamed User'}</UserName>
            <UserEmail>{user.email || 'No email provided'}</UserEmail>
            
            <StatusBadge status={user.idVerificationStatus || 'unverified'}>
              {user.idVerificationStatus ? 
                user.idVerificationStatus.charAt(0).toUpperCase() + user.idVerificationStatus.slice(1) : 
                'Unverified'}
            </StatusBadge>
          </UserInfo>
        </UserHeader>

        <Tabs>
          <Tab 
            active={activeTab === 'info'} 
            onClick={() => setActiveTab('info')}
          >
            User Info
          </Tab>
          <Tab 
            active={activeTab === 'role'} 
            onClick={() => setActiveTab('role')}
          >
            Role & Permissions
          </Tab>
          {user.idUrl && (
            <Tab 
              active={activeTab === 'verification'} 
              onClick={() => setActiveTab('verification')}
            >
              ID Verification
            </Tab>
          )}
        </Tabs>

        {activeTab === 'info' && (
          <DetailSection>
            <SectionTitle>Personal Information</SectionTitle>
            <DetailGrid>
              <DetailItem>
                <DetailLabel>Full Name</DetailLabel>
                <DetailValue>{user.fullName || 'Not provided'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Display Name</DetailLabel>
                <DetailValue>{user.displayName || 'Not provided'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Email</DetailLabel>
                <DetailValue>{user.email || 'Not provided'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Phone Number</DetailLabel>
                <DetailValue>{user.phoneNumber || 'Not provided'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Age</DetailLabel>
                <DetailValue>{user.age || 'Not provided'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Street</DetailLabel>
                <DetailValue>{user.street || 'Not provided'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Barangay</DetailLabel>
                <DetailValue>{user.barangay || 'Not provided'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>City</DetailLabel>
                <DetailValue>{user.city || 'Not provided'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Created On</DetailLabel>
                <DetailValue>{formatDate(user.createdAt)}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Last Login</DetailLabel>
                <DetailValue>{formatDate(user.lastLoginAt)}</DetailValue>
              </DetailItem>
            </DetailGrid>
          </DetailSection>
        )}

        {activeTab === 'role' && (
          <DetailSection>
            <SectionTitle>Role Management</SectionTitle>
            <DetailItem>
              <DetailLabel>Current Role</DetailLabel>
              {editingRole ? (
                <>
                  <RoleSelect
                    value={selectedRole}
                    onChange={e => setSelectedRole(e.target.value)}
                  >
                    <option value="User">User</option>
                    <option value="Admin">Admin</option>
                    <option value="Manager">Manager</option>
                  </RoleSelect>
                  <ActionSection>
                    <Button secondary onClick={() => setEditingRole(false)}>Cancel</Button>
                    <Button onClick={handleRoleChange}>Save Role</Button>
                  </ActionSection>
                </>
              ) : (
                <DetailValue>
                  {user.role || 'User'} 
                  <Button 
                    style={{ marginLeft: '10px', padding: '4px 8px', fontSize: '12px' }}
                    onClick={() => setEditingRole(true)}
                  >
                    Change
                  </Button>
                </DetailValue>
              )}
            </DetailItem>
          </DetailSection>
        )}

        {activeTab === 'verification' && (
          <DetailSection>
            <SectionTitle>ID Verification</SectionTitle>
            <DetailItem>
              <DetailLabel>Status</DetailLabel>
              <DetailValue>
                <StatusBadge status={user.idVerificationStatus || 'unverified'}>
                  {user.idVerificationStatus ? 
                    user.idVerificationStatus.charAt(0).toUpperCase() + user.idVerificationStatus.slice(1) : 
                    'Unverified'}
                </StatusBadge>
              </DetailValue>
            </DetailItem>
            <Button 
              onClick={handleVerificationClick}
              disabled={!user.idUrl}
            >
              View ID Document
            </Button>
          </DetailSection>
        )}

        <ActionSection>
          <Button secondary onClick={onClose}>Close</Button>
          <Button 
            onClick={handleVerificationClick}
            disabled={!user.idUrl}
          >
            Review ID Verification
          </Button>
        </ActionSection>
      </ModalContent>

      {showIdVerification && (
        <div style={{ position: 'absolute', zIndex: 1001 }}>
          <UserIdVerification 
            user={user} 
            onClose={() => setShowIdVerification(false)}
            onStatusChange={(status) => {
              onVerificationStatusChange(user.id, status);
              setShowIdVerification(false);
            }}
          />
        </div>
      )}
    </Modal>
  );
};

export default UserDetailsDialog; 