// src/components/Dashboard.js
import React, { useState, useEffect } from 'react';
import { auth } from '../firebase';
import { onAuthStateChanged } from 'firebase/auth';
import { useNavigate, useLocation } from 'react-router-dom';
import Analytics from './Analytics';
import BikesList from './BikesList';
import BookingManagement from './BookingManagement';
import EditBike from './EditBike';
import UsersList from './UsersList';
import BikeReviews from './BikeReviews';
import CustomerSupportMessages from './CustomerSupportMessages';
import RealTimeTrackingDashboard from './admin/RealTimeTrackingDashboard';
import PaymentsDashboard from './PaymentsDashboard';
import { preloadOptionsData, preloadDashboardData, clearAllCache } from '../services/dashboardService';
import { DataProvider } from '../context/DataContext';
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
  position: relative;
  overflow: hidden;
`;

const Sidebar = styled.div`
  width: 250px;
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  padding: 25px 20px;
  box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  overflow-y: auto;
  z-index: 1000;
  
  @media (max-width: 768px) {
    width: 200px;
  }
  
  @media (max-width: 576px) {
    width: ${props => props.isOpen ? '250px' : '0'};
    transform: ${props => props.isOpen ? 'translateX(0)' : 'translateX(-100%)'};
    transition: all 0.3s ease;
    padding-top: 60px;
  }
`;

const Content = styled.div`
  flex: 1;
  padding: 30px;
  background-color: ${colors.lightGray};
  margin-left: 250px;
  overflow-y: auto;
  height: 100vh;
  
  @media (max-width: 768px) {
    margin-left: 200px;
    padding: 25px;
  }
  
  @media (max-width: 576px) {
    margin-left: ${props => props.sidebarOpen ? '250px' : '0'};
    transition: margin-left 0.3s ease;
    padding: 20px 15px;
  }
`;

const MenuToggle = styled.div`
  display: none; /* Hide by default */
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 1001;
  cursor: pointer;
  background-color: ${colors.pineGreen};
  color: white;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 5px rgba(0,0,0,0.2);
  
  /* Only show on mobile */
  @media (max-width: 576px) {
    display: flex;
    left: ${props => props.sidebarOpen ? '210px' : '20px'};
    transition: left 0.3s ease;
  }
`;

const MenuOption = styled.p`
  padding: 14px 16px;
  cursor: pointer;
  background-color: ${props => props.active ? colors.lightPineGreen : 'transparent'};
  margin-bottom: 12px;
  border-radius: 6px;
  transition: all 0.2s ease;
  font-weight: ${props => props.active ? 'bold' : 'normal'};
  
  &:hover {
    background-color: ${props => props.active ? colors.lightPineGreen : 'rgba(255, 255, 255, 0.1)'};
  }
`;

const LogoutOption = styled(MenuOption)`
  margin-top: 60px;
  color: ${colors.white};
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 25px;
  
  &:hover {
    background-color: rgba(211, 47, 47, 0.2);
  }
`;

/* Add a custom fixed header for the sidebar to ensure clean display */
const SidebarHeader = styled.div`
  width: 100%;
  padding: 0;
  margin-bottom: 30px;
`;

const Logo = styled.div`
  font-size: 1.6rem;
  font-weight: bold;
  padding-bottom: 25px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: block;
  width: 100%;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  letter-spacing: 0.5px;
`;

const ContentSection = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  padding: 25px;
  margin-bottom: 25px;
`;

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState('overview');
  const [selectedBike, setSelectedBike] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [isInitialized, setIsInitialized] = useState(false);
  const [authError, setAuthError] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();

  // Check authentication and secure browser history
  useEffect(() => {
    // Function to prevent back navigation
    const blockBackNavigation = () => {
      window.history.pushState(null, document.title, window.location.href);
    };

    // Check authentication
    const unsubscribe = auth.onAuthStateChanged(user => {
      if (!user) {
        navigate('/login', { replace: true });
      } else {
        // User is authenticated - block back navigation to login
        blockBackNavigation();
        
        // Monitor authentication state
        console.log('User authenticated in dashboard:', user.uid);
        setAuthError(null);
        
        // Initialize data when dashboard loads
        const initializeData = async () => {
          try {
            console.log('Initializing dashboard data...');
            
            // Clear all caches first to ensure fresh data
            clearAllCache();
            
            // Start preloading dashboard data in parallel with options data
            const preloadPromises = [
              preloadOptionsData().catch(error => {
                console.warn('Failed to preload options data:', error.message);
                return null; // Don't fail the entire initialization
              }),
              preloadDashboardData().catch(error => {
                console.warn('Failed to preload dashboard data:', error.message);
                return null; // Don't fail the entire initialization
              })
            ];
            
            // Wait for all preloads to complete
            await Promise.allSettled(preloadPromises);
            
            setIsInitialized(true);
            console.log('Dashboard data initialized successfully');
          } catch (error) {
            console.error("Error initializing dashboard data:", error);
            // Still mark as initialized to prevent blocking UI
            setIsInitialized(true);
          }
        };
        
        initializeData();
      }
    });
    
    // Listen for page navigation attempts
    window.addEventListener('popstate', blockBackNavigation);
    
    // Store the current path in session storage
    sessionStorage.setItem('lastAuthRoute', location.pathname);
    
    // Clean up
    return () => {
      unsubscribe();
      window.removeEventListener('popstate', blockBackNavigation);
    };
  }, [navigate, location.pathname]);

  const handleLogout = async () => {
    try {
      // Clear any stored routes before logout
      sessionStorage.removeItem('lastAuthRoute');
      
      // Sign out the user
      await auth.signOut();
      
      // Force navigation to login with replacement
      navigate('/login', { replace: true });
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

  // Toggle sidebar for mobile
  const toggleSidebar = () => {
    setSidebarOpen(!sidebarOpen);
  };

  // Close sidebar when clicking on menu item (mobile only)
  const handleMenuClick = (tab) => {
    setActiveTab(tab);
    if (window.innerWidth <= 576) {
      setSidebarOpen(false);
    }
  };

  // Loading state for dashboard
  if (!isInitialized) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        fontSize: '18px',
        color: '#666' 
      }}>
        <div>
          <div>Initializing dashboard...</div>
          <div style={{ textAlign: 'center', marginTop: '10px', fontSize: '14px' }}>
            Loading shared data for faster performance
          </div>
        </div>
      </div>
    );
  }

  return (
    <DataProvider>
      <DashboardContainer>
        {/* Mobile menu toggle - completely separate from the sidebar */}
        <MenuToggle onClick={toggleSidebar} sidebarOpen={sidebarOpen}>
          {sidebarOpen ? '✕' : '☰'}
        </MenuToggle>
        
        {/* Sidebar with clean logo, no X button */}
        <Sidebar isOpen={sidebarOpen}>
          <SidebarHeader>
            <Logo>Bambike Admin</Logo>
          </SidebarHeader>
          <div>
            <MenuOption 
              active={activeTab === 'overview'}
              onClick={() => handleMenuClick('overview')}
            >
              Overview
            </MenuOption>
            <MenuOption 
              active={activeTab === 'bikes'}
              onClick={() => handleMenuClick('bikes')}
            >
              Manage Bikes
            </MenuOption>
            <MenuOption 
              active={activeTab === 'bookings'}
              onClick={() => handleMenuClick('bookings')}
            >
              Bookings
            </MenuOption>
            <MenuOption 
              active={activeTab === 'payments'}
              onClick={() => handleMenuClick('payments')}
            >
              Payments
            </MenuOption>
            <MenuOption 
              active={activeTab === 'users'}
              onClick={() => handleMenuClick('users')}
            >
              Manage Users
            </MenuOption>
            <MenuOption 
              active={activeTab === 'reviews'}
              onClick={() => handleMenuClick('reviews')}
            >
              Bike Reviews
            </MenuOption>
            <MenuOption 
              active={activeTab === 'customerSupport'}
              onClick={() => handleMenuClick('customerSupport')}
            >
              Customer Support Messages
            </MenuOption>
            <MenuOption 
              active={activeTab === 'realTimeTracking'}
              onClick={() => handleMenuClick('realTimeTracking')}
            >
              Real Time Tracking
            </MenuOption>
            <LogoutOption onClick={handleLogout}>
              Logout
            </LogoutOption>
          </div>
        </Sidebar>
        
        <Content sidebarOpen={sidebarOpen}>
          {/* Authentication Error Banner */}
          {authError && (
            <div style={{
              backgroundColor: '#ffebee',
              border: '2px solid #d32f2f',
              borderRadius: '8px',
              padding: '15px',
              marginBottom: '20px',
              textAlign: 'center'
            }}>
              <div style={{ color: '#d32f2f', fontWeight: 'bold', marginBottom: '10px' }}>
                🚨 Authentication Issue Detected
              </div>
              <div style={{ color: '#666666', fontSize: '14px', marginBottom: '15px' }}>
                {authError} - Some dashboard features may be limited.
              </div>
              <button
                onClick={() => {
                  console.log('Retrying authentication...');
                  setAuthError(null);
                  window.location.reload();
                }}
                style={{
                  backgroundColor: '#d32f2f',
                  color: 'white',
                  border: 'none',
                  padding: '8px 16px',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontWeight: 'bold'
                }}
              >
                🔄 Reload Dashboard
              </button>
            </div>
          )}
          
          <ContentSection>
            {activeTab === 'overview' && (
              <Analytics />
            )}
            {activeTab === 'bikes' && (
              <BikesList onEditBike={handleEditBike} />
            )}
            {activeTab === 'bookings' && (
              <BookingManagement />
            )}
            {activeTab === 'payments' && (
              <PaymentsDashboard />
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
            {activeTab === 'reviews' && (
              <BikeReviews />
            )}
            {activeTab === 'customerSupport' && (
              <CustomerSupportMessages />
            )}
            {activeTab === 'realTimeTracking' && (
              <RealTimeTrackingDashboard />
            )}
          </ContentSection>
        </Content>
      </DashboardContainer>
    </DataProvider>
  );
};

export default Dashboard;