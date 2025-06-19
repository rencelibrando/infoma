import React, { useState } from 'react';
import styled from 'styled-components';
import { updateUserBlockStatus, deleteUser, updateIdVerificationStatus } from '../services/userService';
import { getFunctions, httpsCallable } from 'firebase/functions';

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

const FullWidthDetailItem = styled(DetailItem)`
  grid-column: 1 / -1;
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
  background-color: rgba(0, 0, 0, 0.05);
  color: ${colors.mediumGray};
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

const TextArea = styled.textarea`
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-height: 100px;
  resize: vertical;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
`;

const IdImageContainer = styled.div`
  width: 100%;
  margin: 20px 0;
  border: 1px solid ${colors.lightGray};
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  min-height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: ${colors.lightGray};
`;

const IdImage = styled.img`
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
`;

const BadgeStatus = styled.span`
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  background-color: ${props =>
    props.status === 'verified' ? 'rgba(76, 175, 80, 0.2)' :
    props.status === 'pending' ? 'rgba(255, 152, 0, 0.2)' :
    props.status === 'rejected' ? 'rgba(244, 67, 54, 0.2)' :
    'rgba(0, 0, 0, 0.1)'
  };
  color: ${props =>
    props.status === 'verified' ? colors.success :
    props.status === 'pending' ? colors.warning :
    props.status === 'rejected' ? colors.error :
    colors.mediumGray
  };
  margin-left: 8px;
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
    props.warning ? colors.warning :
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
  onRoleChange 
}) => {
  const [activeTab, setActiveTab] = useState('info');
  const [editingRole, setEditingRole] = useState(false);
  const [selectedRole, setSelectedRole] = useState(user?.role?.toLowerCase() || 'user');
  const [confirmingBlock, setConfirmingBlock] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [verificationNote, setVerificationNote] = useState('');
  const [confirmingReject, setConfirmingReject] = useState(false);
  const [confirmingApprove, setConfirmingApprove] = useState(false);

  if (!user) return null;

  const handleRoleChange = async () => {
    await onRoleChange(user.id, selectedRole);
    setEditingRole(false);
  };

  const handleBlockUser = async () => {
    try {
      setProcessing(true);
      setError(null);
      setSuccess(null);
      await updateUserBlockStatus(user.id, !user.isBlocked);
      setConfirmingBlock(false);
      setSuccess(`User ${!user.isBlocked ? 'blocked' : 'unblocked'} successfully`);
      
      // Update local user object to reflect the change
      user.isBlocked = !user.isBlocked;
    } catch (err) {
      console.error("Error blocking/unblocking user:", err);
      setError(err.message || "Failed to update user block status. Please try again.");
    } finally {
      setProcessing(false);
    }
  };

  const handleDeleteUser = async () => {
    try {
      setProcessing(true);
      setError(null);
      setSuccess(null);
      const result = await deleteUser(user.id);
      setConfirmingDelete(false);
      setSuccess(result.message || 'User deleted successfully');
      
      // Wait a moment to show success message, then close
      setTimeout(() => {
        onClose();
      }, 1500);
    } catch (err) {
      console.error("Error deleting user:", err);
      setError(err.message || "Failed to delete user. Please try again.");
      setProcessing(false);
    }
  };

  const handleApproveId = async () => {
    try {
      setProcessing(true);
      setError(null);
      setSuccess(null);
      
      await updateIdVerificationStatus(user.id, 'verified', null);
      
      // Update local user object
      user.idVerificationStatus = 'verified';
      user.isIdVerified = true;
      user.idVerificationNote = null;
      
      setConfirmingApprove(false);
      setSuccess("ID verification approved successfully");
      
      // Close after 1.5 seconds with refresh flag
      setTimeout(() => {
        onClose(true); // Pass true to indicate refresh needed
      }, 1500);
    } catch (err) {
      console.error("Error approving ID verification:", err);
      setError(err.message || "Failed to approve ID verification. Please try again.");
      setProcessing(false);
    }
  };

  const handleRejectId = async () => {
    if (!verificationNote.trim()) {
      setError("Please provide a reason for rejection");
      return;
    }
    
    try {
      setProcessing(true);
      setError(null);
      setSuccess(null);
      
      await updateIdVerificationStatus(user.id, 'rejected', verificationNote);
      
      // Update local user object
      user.idVerificationStatus = 'rejected';
      user.isIdVerified = false;
      user.idVerificationNote = verificationNote;
      
      setConfirmingReject(false);
      setSuccess("ID verification rejected successfully");
      
      // Close after 1.5 seconds with refresh flag
      setTimeout(() => {
        onClose(true); // Pass true to indicate refresh needed
      }, 1500);
    } catch (err) {
      console.error("Error rejecting ID verification:", err);
      setError(err.message || "Failed to reject ID verification. Please try again.");
      setProcessing(false);
    }
  };

  const getInitials = (name) => {
    if (!name) return '?';
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  };

  // Format dates from Firestore timestamps or timestamp objects
  const formatDate = (timestamp) => {
    if (!timestamp) return 'Not available';
    
    try {
      if (timestamp.seconds) {
        return new Date(timestamp.seconds * 1000).toLocaleString();
      } else if (typeof timestamp === 'number') {
        return new Date(timestamp).toLocaleString();
      } else if (timestamp instanceof Date) {
        return timestamp.toLocaleString();
      }
    } catch (e) {
      console.error("Error formatting date:", e);
    }
    
    return 'Invalid date format';
  };

  // Check if user has submitted ID for verification
  const hasSubmittedId = user.idImageUrl && user.idVerificationStatus;

  return (
    <Modal onClick={onClose}>
      <ModalContent onClick={e => e.stopPropagation()}>
        <CloseButton onClick={onClose}>&times;</CloseButton>
        
        <UserHeader>
          <UserAvatar>
            {user.profilePictureUrl ? (
              <UserImage src={user.profilePictureUrl} alt={user.fullName || user.displayName} />
            ) : user.photoURL ? (
              <UserImage src={user.photoURL} alt={user.fullName || user.displayName} />
            ) : (
              getInitials(user.fullName || user.displayName || user.email)
            )}
          </UserAvatar>
          
          <UserInfo>
            <UserName>
              {user.fullName || user.displayName || 'Anonymous User'}
              {user.idVerificationStatus && (
                <BadgeStatus status={user.idVerificationStatus}>
                  {user.idVerificationStatus}
                </BadgeStatus>
              )}
            </UserName>
            <UserEmail>{user.email}</UserEmail>
          </UserInfo>
        </UserHeader>
        
        <Tabs>
          <Tab 
            active={activeTab === 'info'} 
            onClick={() => setActiveTab('info')}
          >
            User Information
          </Tab>
          {hasSubmittedId && (
            <Tab 
              active={activeTab === 'id-verification'} 
              onClick={() => setActiveTab('id-verification')}
            >
              ID Verification
            </Tab>
          )}
          <Tab 
            active={activeTab === 'management'} 
            onClick={() => setActiveTab('management')}
          >
            Account Management
          </Tab>
        </Tabs>
        
        {activeTab === 'info' && (
          <DetailSection>
            <SectionTitle>Basic Information</SectionTitle>
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
                <DetailLabel>Phone</DetailLabel>
                <DetailValue>{user.phoneNumber || 'Not provided'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Role</DetailLabel>
                <DetailValue>{user.role ? user.role.charAt(0).toUpperCase() + user.role.slice(1) : 'User'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>User ID</DetailLabel>
                <DetailValue style={{wordBreak: 'break-all', fontSize: '13px'}}>{user.id || 'Unknown'}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Account Created</DetailLabel>
                <DetailValue>{formatDate(user.createdAt)}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Last Login</DetailLabel>
                <DetailValue>{formatDate(user.lastLoginAt)}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Last Updated</DetailLabel>
                <DetailValue>{formatDate(user.lastUpdated)}</DetailValue>
              </DetailItem>
              <DetailItem>
                <DetailLabel>Account Status</DetailLabel>
                <DetailValue>
                  {user.isActive === false ? 'Inactive' : 'Active'}
                </DetailValue>
              </DetailItem>
            </DetailGrid>

            {(user.address || user.city || user.state || user.country || user.postalCode) && (
              <>
                <SectionTitle style={{marginTop: '20px'}}>Address Information</SectionTitle>
                <DetailGrid>
                  {user.address && (
                    <FullWidthDetailItem>
                      <DetailLabel>Address</DetailLabel>
                      <DetailValue>{user.address}</DetailValue>
                    </FullWidthDetailItem>
                  )}
                  
                  {user.city && (
                    <DetailItem>
                      <DetailLabel>City</DetailLabel>
                      <DetailValue>{user.city}</DetailValue>
                    </DetailItem>
                  )}
                  
                  {user.state && (
                    <DetailItem>
                      <DetailLabel>State/Province</DetailLabel>
                      <DetailValue>{user.state}</DetailValue>
                    </DetailItem>
                  )}
                  
                  {user.country && (
                    <DetailItem>
                      <DetailLabel>Country</DetailLabel>
                      <DetailValue>{user.country}</DetailValue>
                    </DetailItem>
                  )}
                  
                  {user.postalCode && (
                    <DetailItem>
                      <DetailLabel>Postal Code</DetailLabel>
                      <DetailValue>{user.postalCode}</DetailValue>
                    </DetailItem>
                  )}
                </DetailGrid>
              </>
            )}

            {(user.emergencyContact || user.preferredBikeType || user.ridingExperience || 
              user.membershipTier || user.marketingOptIn !== undefined) && (
              <>
                <SectionTitle style={{marginTop: '20px'}}>Preferences & Additional Info</SectionTitle>
                <DetailGrid>
                  {user.emergencyContact && (
                    <FullWidthDetailItem>
                      <DetailLabel>Emergency Contact</DetailLabel>
                      <DetailValue>{user.emergencyContact}</DetailValue>
                    </FullWidthDetailItem>
                  )}
                  
                  {user.preferredBikeType && (
                    <DetailItem>
                      <DetailLabel>Preferred Bike Type</DetailLabel>
                      <DetailValue>{user.preferredBikeType}</DetailValue>
                    </DetailItem>
                  )}
                  
                  {user.marketingOptIn !== undefined && (
                    <DetailItem>
                      <DetailLabel>Marketing Opt-in</DetailLabel>
                      <DetailValue>
                        {user.marketingOptIn === true ? 'Yes' : 
                        user.marketingOptIn === false ? 'No' : 'Not specified'}
                      </DetailValue>
                    </DetailItem>
                  )}
                  
                  {user.ridingExperience && (
                    <DetailItem>
                      <DetailLabel>Riding Experience</DetailLabel>
                      <DetailValue>{user.ridingExperience}</DetailValue>
                    </DetailItem>
                  )}
                  
                  {user.membershipTier && (
                    <DetailItem>
                      <DetailLabel>Membership Tier</DetailLabel>
                      <DetailValue>{user.membershipTier}</DetailValue>
                    </DetailItem>
                  )}
                  
                  {user.paymentMethods && user.paymentMethods.length > 0 && (
                    <DetailItem>
                      <DetailLabel>Payment Methods</DetailLabel>
                      <DetailValue>{user.paymentMethods.length} saved</DetailValue>
                    </DetailItem>
                  )}
                </DetailGrid>
              </>
            )}

            {user.notes && (
              <>
                <SectionTitle style={{marginTop: '20px'}}>Admin Notes</SectionTitle>
                <DetailValue style={{
                  whiteSpace: 'pre-wrap', 
                  backgroundColor: colors.lightGray, 
                  padding: '10px', 
                  borderRadius: '4px'
                }}>
                  {user.notes}
                </DetailValue>
              </>
            )}
          </DetailSection>
        )}
        
        {activeTab === 'id-verification' && hasSubmittedId && (
          <DetailSection>
            <SectionTitle>ID Verification</SectionTitle>
            
            {error && (
              <div style={{
                padding: '10px', 
                marginBottom: '16px', 
                backgroundColor: 'rgba(244, 67, 54, 0.1)', 
                color: colors.error,
                borderRadius: '4px'
              }}>
                {error}
              </div>
            )}
            
            {success && (
              <div style={{
                padding: '10px', 
                marginBottom: '16px', 
                backgroundColor: 'rgba(76, 175, 80, 0.1)', 
                color: colors.success,
                borderRadius: '4px'
              }}>
                {success}
              </div>
            )}
            
            <DetailGrid>
              <DetailItem>
                <DetailLabel>ID Type</DetailLabel>
                <DetailValue>{user.idType || 'Not specified'}</DetailValue>
              </DetailItem>
              
              <DetailItem>
                <DetailLabel>Submission Date</DetailLabel>
                <DetailValue>{formatDate(user.idSubmissionDate)}</DetailValue>
              </DetailItem>
              
              <DetailItem>
                <DetailLabel>Verification Status</DetailLabel>
                <DetailValue>
                  <BadgeStatus status={user.idVerificationStatus}>
                    {user.idVerificationStatus || 'not_submitted'}
                  </BadgeStatus>
                </DetailValue>
              </DetailItem>
              
              {user.idVerificationNote && (
                <FullWidthDetailItem>
                  <DetailLabel>Verification Note</DetailLabel>
                  <DetailValue style={{
                    backgroundColor: 'rgba(244, 67, 54, 0.05)', 
                    padding: '8px', 
                    borderRadius: '4px'
                  }}>
                    {user.idVerificationNote}
                  </DetailValue>
                </FullWidthDetailItem>
              )}
            </DetailGrid>
            
            <IdImageContainer>
              {user.idImageUrl ? (
                <IdImage 
                  src={user.idImageUrl} 
                  alt={`${user.fullName || user.displayName}'s ID`} 
                />
              ) : (
                <div>No ID image available</div>
              )}
            </IdImageContainer>
            
            <div>
              {user.idVerificationStatus === 'pending' && (
                <ActionSection>
                  <Button 
                    warning
                    onClick={() => setConfirmingReject(true)}
                    disabled={processing}
                  >
                    Reject ID
                  </Button>
                  <Button 
                    success
                    onClick={() => setConfirmingApprove(true)}
                    disabled={processing}
                  >
                    Approve ID
                  </Button>
                </ActionSection>
              )}
              
              {confirmingApprove && (
                <div>
                  <DetailValue style={{marginBottom: '10px'}}>
                    Are you sure you want to approve this ID? This will grant the user access to all restricted features.
                  </DetailValue>
                  
                  <ActionSection>
                    <Button 
                      secondary 
                      onClick={() => setConfirmingApprove(false)}
                      disabled={processing}
                    >
                      Cancel
                    </Button>
                    <Button 
                      success
                      onClick={handleApproveId}
                      disabled={processing}
                    >
                      {processing ? 'Processing...' : 'Confirm Approval'}
                    </Button>
                  </ActionSection>
                </div>
              )}
              
              {confirmingReject && (
                <div>
                  <DetailValue style={{marginBottom: '10px'}}>
                    Please provide a reason for rejection. This will be shown to the user.
                  </DetailValue>
                  
                  <TextArea
                    value={verificationNote}
                    onChange={(e) => setVerificationNote(e.target.value)}
                    placeholder="Enter reason for rejection (required)"
                  />
                  
                  <ActionSection>
                    <Button 
                      secondary 
                      onClick={() => setConfirmingReject(false)}
                      disabled={processing}
                    >
                      Cancel
                    </Button>
                    <Button 
                      danger
                      onClick={handleRejectId}
                      disabled={processing || !verificationNote.trim()}
                    >
                      {processing ? 'Processing...' : 'Confirm Rejection'}
                    </Button>
                  </ActionSection>
                </div>
              )}
              
              {user.idVerificationStatus === 'verified' && (
                <div style={{
                  padding: '10px',
                  backgroundColor: 'rgba(76, 175, 80, 0.1)', 
                  color: colors.success,
                  borderRadius: '4px',
                  textAlign: 'center',
                  marginTop: '10px'
                }}>
                  This ID has been verified and approved
                </div>
              )}
              
              {user.idVerificationStatus === 'rejected' && (
                <ActionSection>
                  <Button 
                    warning
                    onClick={() => setConfirmingReject(true)}
                    disabled={processing}
                  >
                    Update Rejection Reason
                  </Button>
                  <Button 
                    success
                    onClick={() => setConfirmingApprove(true)}
                    disabled={processing}
                  >
                    Change to Approved
                  </Button>
                </ActionSection>
              )}
            </div>
          </DetailSection>
        )}
        
        {activeTab === 'management' && (
          <DetailSection>
            <SectionTitle>Role Management</SectionTitle>
            
            <DetailItem>
              <DetailLabel>Current Role</DetailLabel>
              {editingRole ? (
                <div>
                  <RoleSelect 
                    value={selectedRole} 
                    onChange={e => setSelectedRole(e.target.value)}
                  >
                    <option value="user">User</option>
                    <option value="admin">Admin</option>
                    <option value="manager">Manager</option>
                  </RoleSelect>
                  
                  <ActionSection>
                    <Button 
                      secondary 
                      onClick={() => setEditingRole(false)}
                    >
                      Cancel
                    </Button>
                    <Button 
                      onClick={handleRoleChange}
                    >
                      Save Role
                    </Button>
                  </ActionSection>
                </div>
              ) : (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <DetailValue>{user.role ? user.role.charAt(0).toUpperCase() + user.role.slice(1) : 'User'}</DetailValue>
                  <Button 
                    secondary
                    onClick={() => setEditingRole(true)}
                  >
                    Change Role
                  </Button>
                </div>
              )}
            </DetailItem>
            
            <SectionTitle style={{marginTop: '24px'}}>Account Actions</SectionTitle>
            
            {error && (
              <div style={{
                padding: '10px', 
                marginBottom: '16px', 
                backgroundColor: 'rgba(244, 67, 54, 0.1)', 
                color: colors.error,
                borderRadius: '4px'
              }}>
                {error}
              </div>
            )}
            
            {success && (
              <div style={{
                padding: '10px', 
                marginBottom: '16px', 
                backgroundColor: 'rgba(76, 175, 80, 0.1)', 
                color: colors.success,
                borderRadius: '4px'
              }}>
                {success}
              </div>
            )}
            
            <DetailItem>
              <DetailLabel>Block User</DetailLabel>
              {confirmingBlock ? (
                <div>
                  <DetailValue style={{marginBottom: '10px', color: colors.error}}>
                    Are you sure you want to {user.isBlocked ? 'unblock' : 'block'} this user? 
                    {!user.isBlocked && ' They will not be able to login or use the app.'}
                  </DetailValue>
                  
                  <ActionSection>
                    <Button 
                      secondary 
                      onClick={() => setConfirmingBlock(false)}
                      disabled={processing}
                    >
                      Cancel
                    </Button>
                    <Button 
                      danger
                      onClick={handleBlockUser}
                      disabled={processing}
                    >
                      {processing ? 'Processing...' : `Confirm ${user.isBlocked ? 'Unblock' : 'Block'}`}
                    </Button>
                  </ActionSection>
                </div>
              ) : (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <DetailValue>{user.isBlocked ? 'Currently blocked' : 'Not blocked'}</DetailValue>
                  <Button 
                    danger
                    onClick={() => setConfirmingBlock(true)}
                    disabled={processing}
                  >
                    {user.isBlocked ? 'Unblock User' : 'Block User'}
                  </Button>
                </div>
              )}
            </DetailItem>
            
            <DetailItem style={{marginTop: '16px'}}>
              <DetailLabel>Delete User</DetailLabel>
              {confirmingDelete ? (
                <div>
                  <DetailValue style={{marginBottom: '10px', color: colors.error}}>
                    <strong>Warning:</strong> This action cannot be undone. This will permanently delete the user account and all associated data.
                  </DetailValue>
                  
                  <ActionSection>
                    <Button 
                      secondary 
                      onClick={() => setConfirmingDelete(false)}
                      disabled={processing}
                    >
                      Cancel
                    </Button>
                    <Button 
                      danger
                      onClick={handleDeleteUser}
                      disabled={processing}
                    >
                      {processing ? 'Processing...' : 'Permanently Delete'}
                    </Button>
                  </ActionSection>
                </div>
              ) : (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <DetailValue>Delete this user account</DetailValue>
                  <Button 
                    danger
                    onClick={() => setConfirmingDelete(true)}
                    disabled={processing}
                  >
                    Delete User
                  </Button>
                </div>
              )}
            </DetailItem>
          </DetailSection>
        )}
        
        <ActionSection>
          <Button secondary onClick={onClose}>Close</Button>
        </ActionSection>
      </ModalContent>
    </Modal>
  );
};

export default UserDetailsDialog; 