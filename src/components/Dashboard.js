// src/components/Dashboard.js
import React, { useState, useEffect } from 'react';
import { auth } from '../firebase';
import { useNavigate } from 'react-router-dom';
import BikesList from './BikesList';
import AddBike from './AddBike';
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

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState('bikes');
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

  return (
    <DashboardContainer>
      <Sidebar>
        <h2>Bike Rental Admin</h2>
        <div style={{ marginTop: '30px' }}>
          <p 
            style={{ 
              padding: '10px', 
              cursor: 'pointer',
              backgroundColor: activeTab === 'bikes' ? '#444' : 'transparent'
            }}
            onClick={() => setActiveTab('bikes')}
          >
            Manage Bikes
          </p>
          <p 
            style={{ 
              padding: '10px', 
              cursor: 'pointer',
              backgroundColor: activeTab === 'add' ? '#444' : 'transparent'
            }}
            onClick={() => setActiveTab('add')}
          >
            Add New Bike
          </p>
          <p 
            style={{ 
              padding: '10px', 
              cursor: 'pointer',
              marginTop: '50px',
              color: '#ff6666'
            }}
            onClick={handleLogout}
          >
            Logout
          </p>
        </div>
      </Sidebar>
      
      <Content>
        {activeTab === 'bikes' && <BikesList />}
        {activeTab === 'add' && <AddBike onSuccess={() => setActiveTab('bikes')} />}
      </Content>
    </DashboardContainer>
  );
};

export default Dashboard;