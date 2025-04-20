import React, { useState, useEffect, useMemo } from 'react';
import { db } from '../firebase';
import styled from 'styled-components';
import UserDetailsDialog from './UserDetailsDialog';
import { 
  getUsers, 
  updateUserRole, 
  subscribeToUsers,
  updateUserBlockStatus,
  deleteUser
} from '../services/userService';
import { getUserRoles } from '../services/dashboardService';

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

const Container = styled.div`
  padding: 20px;
`;

const Title = styled.h2`
  margin-bottom: 20px;
  color: ${colors.darkGray};
`;

const TableContainer = styled.div`
  overflow-x: auto;
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin-top: 20px;
`;

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  background-color: ${colors.white};
`;

const TableHead = styled.thead`
  background-color: ${colors.lightGray};
`;

const TableRow = styled.tr`
  &:nth-child(even) {
    background-color: ${colors.lightGray};
  }
  
  &:hover {
    background-color: rgba(29, 60, 52, 0.05);
    cursor: pointer;
  }
`;

const TableHeader = styled.th`
  padding: 12px 15px;
  text-align: left;
  font-weight: bold;
  border-bottom: 1px solid #ddd;
  color: ${colors.darkGray};
`;

const Avatar = styled.div`
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: ${colors.lightGray};
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: ${colors.mediumGray};
  overflow: hidden;
`;

const AvatarImage = styled.img`
  width: 100%;
  height: 100%;
  object-fit: cover;
`;

const UserName = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
`;

const TableCell = styled.td`
  padding: 12px 15px;
  border-bottom: 1px solid #ddd;
  color: ${colors.darkGray};
`;

const LoadingMessage = styled.div`
  text-align: center;
  padding: 20px;
  font-style: italic;
  color: ${colors.mediumGray};
`;

const ErrorMessage = styled.div`
  text-align: center;
  padding: 20px;
  color: #d32f2f;
  background-color: #ffebee;
  border-radius: 4px;
  margin-top: 20px;
`;

const SearchContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
`;

const SearchInput = styled.input`
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  flex: 1;
  font-size: 14px;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 2px rgba(29, 60, 52, 0.1);
  }
`;

const Select = styled.select`
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: ${colors.white};
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
`;

const Button = styled.button`
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background-color: ${props => props.secondary ? colors.lightGray : props.danger ? '#d32f2f' : colors.pineGreen};
  color: ${props => props.secondary ? colors.darkGray : colors.white};
  cursor: pointer;
  font-weight: ${props => props.bold ? 'bold' : 'normal'};
  
  &:hover {
    opacity: 0.9;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const RoleSelect = styled.select`
  padding: 5px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: ${colors.white};
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
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

const Modal = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
`;

const RefreshIndicator = styled.div`
  position: fixed;
  top: 10px;
  right: 10px;
  background-color: ${colors.pineGreen};
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  z-index: 1000;
  animation: fadeOut 2s forwards;
  animation-delay: 1s;
  
  @keyframes fadeOut {
    to {
      opacity: 0;
      visibility: hidden;
    }
  }
`;

const LastUpdateTime = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  text-align: right;
  margin-bottom: 5px;
`;

// Helper function to get user initials
const getUserInitials = (name) => {
  if (!name) return '?';
  return name.split(' ').map(n => n[0]).join('').toUpperCase();
};

const UsersList = () => {
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [editingUser, setEditingUser] = useState(null);
  const [selectedUser, setSelectedUser] = useState(null);
  const [showDetailsDialog, setShowDetailsDialog] = useState(false);
  const [detailsUser, setDetailsUser] = useState(null);
  const [lastUpdateTime, setLastUpdateTime] = useState(new Date());
  const [showUpdateIndicator, setShowUpdateIndicator] = useState(false);
  const [availableRoles, setAvailableRoles] = useState(['User', 'Admin', 'Manager']);
  const [optionsLoaded, setOptionsLoaded] = useState(false);

  // Preload options data
  useEffect(() => {
    const preloadOptions = async () => {
      try {
        const roles = await getUserRoles();
        if (roles && roles.length > 0) {
          setAvailableRoles(roles);
        }
        setOptionsLoaded(true);
      } catch (error) {
        console.error('Error loading role options:', error);
        // Still mark as loaded even on error to prevent blocking
        setOptionsLoaded(true);
      }
    };
    
    preloadOptions();
  }, []);

  useEffect(() => {
    setLoading(true);
    
    // Initial data fetch
    const initialFetch = async () => {
      try {
        const userData = await getUsers();
        setUsers(userData);
        setFilteredUsers(userData);
        setLastUpdateTime(new Date());
        setError(null);
      } catch (err) {
        console.error('Error fetching users:', err);
        setError('Failed to load users. Please try again later.');
      } finally {
        setLoading(false);
      }
    };
    
    initialFetch();
    
    // Set up real-time listener
    const unsubscribe = subscribeToUsers((updatedUsers) => {
      setUsers(updatedUsers);
      setLastUpdateTime(new Date());
      setShowUpdateIndicator(true);
      
      // Hide update indicator after 3 seconds
      setTimeout(() => {
        setShowUpdateIndicator(false);
      }, 3000);
    });
    
    // Cleanup listener on component unmount
    return () => {
      unsubscribe();
    };
  }, []);

  // Memoize the filtered users to avoid recomputation on every render
  const memoizedFilteredUsers = useMemo(() => {
    // Filter users based on search term, role filter, and verification status
    return users.filter(user => {
      const matchesSearch = 
        searchTerm === '' || 
        (user.fullName && user.fullName.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (user.displayName && user.displayName.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (user.email && user.email.toLowerCase().includes(searchTerm.toLowerCase()));
        
      const matchesRole = 
        roleFilter === 'all' || 
        user.role?.toLowerCase() === roleFilter.toLowerCase();
      
      const matchesVerification = 
        roleFilter === 'all' || 
        user.role?.toLowerCase() === roleFilter.toLowerCase();
        
      return matchesSearch && matchesRole && matchesVerification;
    });
  }, [searchTerm, roleFilter, users]);

  // Update filtered users when memoized result changes
  useEffect(() => {
    setFilteredUsers(memoizedFilteredUsers);
  }, [memoizedFilteredUsers]);

  const handleRoleChange = async (userId, newRole) => {
    try {
      await updateUserRole(userId, newRole);
      
      // Update local state (this will be redundant with real-time updates)
      setUsers(users.map(user => 
        user.id === userId ? { ...user, role: newRole } : user
      ));
      
      setEditingUser(null);
    } catch (err) {
      console.error('Error updating user role:', err);
      alert('Failed to update user role');
    }
  };

  const handleRowClick = (user) => {
    setDetailsUser(user);
    setShowDetailsDialog(true);
  };

  const handleCloseDetailsDialog = () => {
    setShowDetailsDialog(false);
    setDetailsUser(null);
  };

  if (loading && !optionsLoaded) {
    return <LoadingMessage>Loading users...</LoadingMessage>;
  }

  return (
    <Container>
      <Title>User Management</Title>
      
      {showUpdateIndicator && (
        <RefreshIndicator>
          <span>🔄</span> User data updated
        </RefreshIndicator>
      )}
      
      <LastUpdateTime>
        Last updated: {lastUpdateTime.toLocaleTimeString()}
      </LastUpdateTime>
      
      <SearchContainer>
        <SearchInput
          type="text"
          placeholder="Search users by name or email..."
          value={searchTerm}
          onChange={e => setSearchTerm(e.target.value)}
        />
        
        <Select 
          value={roleFilter} 
          onChange={e => setRoleFilter(e.target.value)}
        >
          <option value="all">All Roles</option>
          {availableRoles.map(role => (
            <option key={role} value={role.toLowerCase()}>
              {role}
            </option>
          ))}
        </Select>
      </SearchContainer>

      {loading ? (
        <LoadingMessage>Loading users data...</LoadingMessage>
      ) : error ? (
        <ErrorMessage>{error}</ErrorMessage>
      ) : (
        <TableContainer>
          <Table>
            <TableHead>
              <tr>
                <TableHeader>Name</TableHeader>
                <TableHeader>Email</TableHeader>
                <TableHeader>Phone</TableHeader>
                <TableHeader>Role</TableHeader>
              </tr>
            </TableHead>
            <tbody>
              {filteredUsers.length > 0 ? (
                filteredUsers.map(user => (
                  <TableRow key={user.id} onClick={() => handleRowClick(user)}>
                    <TableCell>
                      <UserName>
                        <Avatar>
                          {user.profilePictureUrl ? 
                            <AvatarImage src={user.profilePictureUrl} alt={user.fullName || user.displayName} /> :
                            user.photoURL ?
                              <AvatarImage src={user.photoURL} alt={user.fullName || user.displayName} /> :
                              getUserInitials(user.fullName || user.displayName)
                          }
                        </Avatar>
                        {user.fullName || user.displayName || 'N/A'}
                      </UserName>
                    </TableCell>
                    <TableCell>{user.email || 'N/A'}</TableCell>
                    <TableCell>{user.phoneNumber || 'N/A'}</TableCell>
                    <TableCell>{user.role || 'User'}</TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan="5" style={{ textAlign: 'center' }}>
                    No users found matching your filters.
                  </TableCell>
                </TableRow>
              )}
            </tbody>
          </Table>
        </TableContainer>
      )}
      
      {showDetailsDialog && detailsUser && (
        <UserDetailsDialog
          user={detailsUser}
          onClose={handleCloseDetailsDialog}
          onRoleChange={handleRoleChange}
        />
      )}
    </Container>
  );
};

export default UsersList; 