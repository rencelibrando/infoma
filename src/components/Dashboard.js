// src/components/Dashboard.js
import React, { useState, useEffect } from 'react';
import { auth } from '../firebase';
import { useNavigate } from 'react-router-dom';
import BikesList from './BikesList';
import AddBike from './AddBike';
import EditBike from './EditBike';
import styled from 'styled-components';

const DashboardContainer = styled.div`
  display: flex;
  min-height: 100vh;
`;

const Sidebar = styled.div`
  width: 250px;
  background-color: #333;
  color: white;
  padding: 20px;
`;

const Content = styled.div`
  flex: 1;
  padding: 20px;
  background-color: #f5f5f5;
`;

const MenuOption = styled.p`
  padding: 10px;
  cursor: pointer;
  background-color: ${props => props.active ? '#444' : 'transparent'};
  margin-bottom: 5px;
  border-radius: 4px;
  transition: all 0.2s ease;
  
  &:hover {
    background-color: ${props => props.active ? '#444' : '#3a3a3a'};
  }
`;

const LogoutOption = styled(MenuOption)`
  margin-top: 50px;
  color: #ff6666;
  
  &:hover {
    background-color: #5a2a2a;
  }
`;

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState('bikes');
  const [selectedBike, setSelectedBike] = useState(null);
  const navigate = useNavigate();

  // Check authentication
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(user => {
      if (!user) {
        navigate('/login');
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
        <h2>Bike Rental Admin</h2>
        <div style={{ marginTop: '30px' }}>
          <MenuOption 
            active={activeTab === 'bikes'}
            onClick={() => setActiveTab('bikes')}
          >
            Manage Bikes
          </MenuOption>
          <MenuOption 
            active={activeTab === 'add'}
            onClick={() => setActiveTab('add')}
          >
            Add New Bike
          </MenuOption>
          <LogoutOption onClick={handleLogout}>
            Logout
          </LogoutOption>
        </div>
      </Sidebar>
      
      <Content>
        {activeTab === 'bikes' && (
          <BikesList onEditBike={handleEditBike} />
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
      </Content>
    </DashboardContainer>
  );
};

export default Dashboard;