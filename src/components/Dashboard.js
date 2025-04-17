// src/components/Dashboard.js
import React, { useState, useEffect } from 'react';
import { auth } from '../firebase';
import { useNavigate } from 'react-router-dom';
import BikesList from './BikesList';
import AddBike from './AddBike';
import EditBike from './EditBike';
import UsersList from './UsersList';
import Analytics from './Analytics';
import BikesMap from './BikesMap';
import { initializeBikesData } from '../services/bikeService';
import styled from 'styled-components';

// Pine green and gray theme colors
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  red: '#d32f2f'
};

const DashboardContainer = styled.div`
  display: flex;
  min-height: 100vh;
`;

const Sidebar = styled.div`
  width: 250px;
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  padding: 20px;
  box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
`;

const Content = styled.div`
  flex: 1;
  padding: 20px;
  background-color: ${colors.lightGray};
`;

const MenuOption = styled.p`
  padding: 12px;
  cursor: pointer;
  background-color: ${props => props.active ? colors.lightPineGreen : 'transparent'};
  margin-bottom: 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
  font-weight: ${props => props.active ? 'bold' : 'normal'};
  
  &:hover {
    background-color: ${props => props.active ? colors.lightPineGreen : 'rgba(255, 255, 255, 0.1)'};
  }
`;

const LogoutOption = styled(MenuOption)`
  margin-top: 50px;
  color: ${colors.white};
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 20px;
  
  &:hover {
    background-color: rgba(211, 47, 47, 0.2);
  }
`;

const Logo = styled.div`
  font-size: 1.5rem;
  font-weight: bold;
  margin-bottom: 10px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
`;

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState('overview');
  const [selectedBike, setSelectedBike] = useState(null);
  const navigate = useNavigate();

  // Check authentication
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(user => {
      if (!user) {
        navigate('/login');
      } else {
        // Initialize bike data when dashboard loads
        initializeBikesData().catch(error => {
          console.error("Error initializing bikes data:", error);
        });
      }
    });
    
    return () => unsubscribe();
  }, [navigate]);

  const handleLogout = async () => {
    try {
      await auth.signOut();
      navigate('/login');
    } catch (error) {
      console.error("Logout error", error);
    }
  };
  
  const handleEditBike = (bike) => {
    setSelectedBike(bike);
    setActiveTab('edit');
  };
  
  const handleEditSuccess = () => {
    setSelectedBike(null);
    setActiveTab('bikes');
  };
  
  const handleCancelEdit = () => {
    setSelectedBike(null);
    setActiveTab('bikes');
  };

  return (
    <DashboardContainer>
      <Sidebar>
        <Logo>Bambike Admin</Logo>
        <div>
          <MenuOption 
            active={activeTab === 'overview'}
            onClick={() => setActiveTab('overview')}
          >
            Overview
          </MenuOption>
          <MenuOption 
            active={activeTab === 'bikes'}
            onClick={() => setActiveTab('bikes')}
          >
            Manage Bikes
          </MenuOption>
          <MenuOption 
            active={activeTab === 'map'}
            onClick={() => setActiveTab('map')}
          >
            Bikes Map
          </MenuOption>
          <MenuOption 
            active={activeTab === 'add'}
            onClick={() => setActiveTab('add')}
          >
            Add New Bike
          </MenuOption>
          <MenuOption 
            active={activeTab === 'users'}
            onClick={() => setActiveTab('users')}
          >
            Manage Users
          </MenuOption>
          <LogoutOption onClick={handleLogout}>
            Logout
          </LogoutOption>
        </div>
      </Sidebar>
      
      <Content>
        {activeTab === 'overview' && (
          <Analytics />
        )}
        {activeTab === 'bikes' && (
          <BikesList onEditBike={handleEditBike} />
        )}
        {activeTab === 'map' && (
          <BikesMap />
        )}
        {activeTab === 'add' && (
          <AddBike onSuccess={() => setActiveTab('bikes')} />
        )}
        {activeTab === 'edit' && selectedBike && (
          <EditBike 
            bike={selectedBike} 
            onSuccess={handleEditSuccess} 
            onCancel={handleCancelEdit} 
          />
        )}
        {activeTab === 'users' && (
          <UsersList />
        )}
      </Content>
    </DashboardContainer>
  );
};

export default Dashboard;