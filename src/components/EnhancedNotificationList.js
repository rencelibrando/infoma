import React from 'react';
import styled from 'styled-components';
import { format } from 'date-fns';

// Theme colors consistent with BookingManagement
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  red: '#d32f2f',
  amber: '#ffc107',
  green: '#4caf50',
  blue: '#2196f3',
  lightBlue: '#e3f2fd',
  lightGreen: '#e8f5e9',
  lightRed: '#ffebee',
  lightAmber: '#fff8e1'
};

const NotificationListContainer = styled.div`
  background: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
`;

const NotificationHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid ${colors.lightGray};
`;

const NotificationTitle = styled.h3`
  font-size: 18px;
  color: ${colors.darkGray};
  margin: 0;
  display: flex;
  align-items: center;
  font-weight: 600;
  
  svg {
    margin-right: 10px;
    color: ${colors.pineGreen};
  }
`;

const ClearAllButton = styled.button`
  background: ${colors.red};
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    background: #b71c1c;
    transform: translateY(-1px);
  }
  
  &:disabled {
    background: ${colors.mediumGray};
    cursor: not-allowed;
    transform: none;
  }
`;

const NotificationList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 500px;
  overflow-y: auto;
`;

const NotificationItem = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 15px;
  background: ${props => props.read ? colors.lightGray : colors.lightBlue};
  border-radius: 8px;
  border-left: 4px solid ${props => props.read ? colors.mediumGray : colors.blue};
  transition: all 0.2s;
  
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
`;

const NotificationContent = styled.div`
  flex: 1;
  margin-right: 15px;
`;

const NotificationMessage = styled.div`
  font-size: 14px;
  color: ${colors.darkGray};
  font-weight: ${props => props.read ? 'normal' : '600'};
  margin-bottom: 5px;
  line-height: 1.4;
`;

const NotificationMeta = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  display: flex;
  gap: 15px;
  align-items: center;
`;

const NotificationActions = styled.div`
  display: flex;
  gap: 8px;
  align-items: center;
`;

const ActionButton = styled.button`
  background: ${props => props.variant === 'delete' ? colors.red : colors.pineGreen};
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    opacity: 0.8;
    transform: translateY(-1px);
  }
`;

const ReadIndicator = styled.div`
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: ${props => props.read ? colors.mediumGray : colors.blue};
  margin-left: 10px;
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 40px 20px;
  color: ${colors.mediumGray};
  
  svg {
    margin-bottom: 15px;
    color: ${colors.lightPineGreen};
  }
`;

const EnhancedNotificationList = ({ 
  notifications = [], 
  onMarkAsRead, 
  onDeleteNotification, 
  onClearAll 
}) => {
  const formatNotificationDate = (notification) => {
    try {
      if (notification.updatedAt) {
        const date = notification.updatedAt instanceof Date 
          ? notification.updatedAt 
          : (notification.updatedAt.toDate ? notification.updatedAt.toDate() : new Date(notification.updatedAt));
        
        if (!isNaN(date.getTime())) {
          return format(date, 'MMM d, yyyy h:mm a');
        }
      }
      
      if (notification.createdAt) {
        const date = notification.createdAt instanceof Date 
          ? notification.createdAt 
          : (notification.createdAt.toDate ? notification.createdAt.toDate() : new Date(notification.createdAt));
        
        if (!isNaN(date.getTime())) {
          return format(date, 'MMM d, yyyy h:mm a');
        }
      }
      
      return 'Date unavailable';
    } catch (e) {
      console.error('Date formatting error:', e);
      return 'Date unavailable';
    }
  };

  const hasUnreadNotifications = notifications.some(n => !n.read);

  if (notifications.length === 0) {
    return (
      <NotificationListContainer>
        <NotificationHeader>
          <NotificationTitle>
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
            </svg>
            Notifications
          </NotificationTitle>
        </NotificationHeader>
        
        <EmptyState>
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
          </svg>
          <div style={{ fontSize: '16px', fontWeight: '500', marginBottom: '8px' }}>
            No notifications
          </div>
          <div style={{ fontSize: '14px' }}>
            You're all caught up! Notifications will appear here when there are booking updates.
          </div>
        </EmptyState>
      </NotificationListContainer>
    );
  }

  return (
    <NotificationListContainer>
      <NotificationHeader>
        <NotificationTitle>
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
          </svg>
          Notifications ({notifications.length})
        </NotificationTitle>
        
        <ClearAllButton 
          onClick={onClearAll}
          disabled={notifications.length === 0}
        >
          Clear All
        </ClearAllButton>
      </NotificationHeader>
      
      <NotificationList>
        {notifications.map((notification) => (
          <NotificationItem key={notification.id} read={notification.read}>
            <NotificationContent>
              <NotificationMessage read={notification.read}>
                {notification.message || notification.title || 'Notification'}
              </NotificationMessage>
              <NotificationMeta>
                <span>{formatNotificationDate(notification)}</span>
                {notification.type && (
                  <span style={{ 
                    background: notification.type === 'cancellation' ? colors.lightRed : colors.lightGreen,
                    color: notification.type === 'cancellation' ? colors.red : colors.green,
                    padding: '2px 8px',
                    borderRadius: '12px',
                    fontSize: '11px',
                    fontWeight: '500',
                    textTransform: 'uppercase'
                  }}>
                    {notification.type}
                  </span>
                )}
              </NotificationMeta>
            </NotificationContent>
            
            <NotificationActions>
              {!notification.read && (
                <ActionButton onClick={() => onMarkAsRead(notification.id)}>
                  Mark Read
                </ActionButton>
              )}
              <ActionButton 
                variant="delete" 
                onClick={() => onDeleteNotification(notification.id)}
              >
                Delete
              </ActionButton>
              <ReadIndicator read={notification.read} />
            </NotificationActions>
          </NotificationItem>
        ))}
      </NotificationList>
    </NotificationListContainer>
  );
};

export default EnhancedNotificationList; 