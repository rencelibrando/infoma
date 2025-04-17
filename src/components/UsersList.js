import React, { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, getDocs, doc, updateDoc } from 'firebase/firestore';
import styled from 'styled-components';

// Pine green and gray theme colors
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  accent: '#FF8C00'
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
  }
`;

const TableHeader = styled.th`
  padding: 12px 15px;
  text-align: left;
  font-weight: bold;
  border-bottom: 1px solid #ddd;
  color: ${colors.darkGray};
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

const UsersList = () => {
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [editingUser, setEditingUser] = useState(null);

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        setLoading(true);
        const querySnapshot = await getDocs(collection(db, 'users'));
        const usersList = querySnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data(),
          role: doc.data().role || (doc.data().isAdmin ? 'Admin' : 'User')
        }));
        setUsers(usersList);
        setFilteredUsers(usersList);
        setError(null);
      } catch (err) {
        console.error('Error fetching users:', err);
        setError('Failed to load users. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchUsers();
  }, []);

  useEffect(() => {
    // Filter users based on search term and role filter
    const filtered = users.filter(user => {
      const matchesSearch = 
        searchTerm === '' || 
        (user.fullName && user.fullName.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (user.displayName && user.displayName.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (user.email && user.email.toLowerCase().includes(searchTerm.toLowerCase()));
        
      const matchesRole = 
        roleFilter === 'all' || 
        user.role.toLowerCase() === roleFilter.toLowerCase();
        
      return matchesSearch && matchesRole;
    });
    
    setFilteredUsers(filtered);
  }, [searchTerm, roleFilter, users]);

  const handleRoleChange = async (userId, newRole) => {
    try {
      // Update in Firestore
      const userRef = doc(db, 'users', userId);
      await updateDoc(userRef, { 
        role: newRole,
        isAdmin: newRole === 'Admin'
      });
      
      // Update local state
      setUsers(users.map(user => 
        user.id === userId ? { ...user, role: newRole } : user
      ));
      
      setEditingUser(null);
    } catch (err) {
      console.error('Error updating user role:', err);
      alert('Failed to update user role');
    }
  };

  if (loading) {
    return <LoadingMessage>Loading users...</LoadingMessage>;
  }

  if (error) {
    return <ErrorMessage>{error}</ErrorMessage>;
  }

  return (
    <Container>
      <Title>User Management</Title>
      
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
          <option value="admin">Admin</option>
          <option value="user">User</option>
          <option value="manager">Manager</option>
        </Select>
      </SearchContainer>
      
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableHeader>Name</TableHeader>
              <TableHeader>Email</TableHeader>
              <TableHeader>Phone</TableHeader>
              <TableHeader>User ID</TableHeader>
              <TableHeader>Role</TableHeader>
              <TableHeader>Actions</TableHeader>
            </TableRow>
          </TableHead>
          <tbody>
            {filteredUsers.length === 0 ? (
              <TableRow>
                <TableCell colSpan="6" style={{ textAlign: 'center' }}>No users found</TableCell>
              </TableRow>
            ) : (
              filteredUsers.map(user => (
                <TableRow key={user.id}>
                  <TableCell>{user.fullName || user.displayName || 'N/A'}</TableCell>
                  <TableCell>{user.email || 'N/A'}</TableCell>
                  <TableCell>{user.phoneNumber || user.phone || 'N/A'}</TableCell>
                  <TableCell>{user.id}</TableCell>
                  <TableCell>
                    {editingUser === user.id ? (
                      <RoleSelect
                        value={user.role}
                        onChange={(e) => handleRoleChange(user.id, e.target.value)}
                        autoFocus
                      >
                        <option value="Admin">Admin</option>
                        <option value="Manager">Manager</option>
                        <option value="User">User</option>
                      </RoleSelect>
                    ) : (
                      <span style={{ 
                        backgroundColor: user.role === 'Admin' ? colors.pineGreen : user.role === 'Manager' ? colors.accent : colors.lightGray,
                        color: user.role === 'User' ? colors.darkGray : colors.white,
                        padding: '3px 8px',
                        borderRadius: '4px',
                        fontSize: '12px'
                      }}>
                        {user.role}
                      </span>
                    )}
                  </TableCell>
                  <TableCell>
                    {editingUser === user.id ? (
                      <Button secondary onClick={() => setEditingUser(null)}>
                        Cancel
                      </Button>
                    ) : (
                      <Button onClick={() => setEditingUser(user.id)}>
                        Edit Role
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </tbody>
        </Table>
      </TableContainer>
    </Container>
  );
};

export default UsersList; 