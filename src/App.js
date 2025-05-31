// src/App.js
import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import BikeRideScreen from './components/BikeRideScreen';
import StatusCheck from './components/StatusCheck';
import { auth } from './firebase';
import GoogleMapsPreloader from './components/GoogleMapsPreloader';

// Auth history handler component to prevent back navigation to login
const AuthHistoryHandler = () => {
  const navigate = useNavigate();
  const location = useLocation();
  
  useEffect(() => {
    // Handle user authentication state changes
    const unsubscribe = auth.onAuthStateChanged(user => {
      // If user is authenticated and tries to access login page, redirect to dashboard
      if (user && location.pathname === '/login') {
        navigate('/dashboard', { replace: true });
      }
    });
    
    return () => unsubscribe();
  }, [navigate, location]);
  
  // Handle browser back button
  useEffect(() => {
    const handleBackButton = (event) => {
      // If user is authenticated and current location is dashboard
      if (auth.currentUser && 
         (location.pathname === '/dashboard' || 
          location.pathname.startsWith('/ride/'))) {
        // Prevent default action
        event.preventDefault();
        // Push the same route to history to keep user in admin area
        navigate(location.pathname);
      }
    };
    
    window.addEventListener('popstate', handleBackButton);
    
    return () => {
      window.removeEventListener('popstate', handleBackButton);
    };
  }, [navigate, location]);
  
  return null;
};

function App() {
  return (
    <Router>
      <GoogleMapsPreloader />
      <AuthHistoryHandler />
      <Routes>
        {/* Login route - NOT wrapped in StatusCheck so users can always access it */}
        <Route path="/login" element={<Login />} />
        
        {/* Protected routes - wrapped in StatusCheck for system verification */}
        <Route path="/dashboard" element={
          <ProtectedRoute>
            <StatusCheck>
              <Dashboard />
            </StatusCheck>
          </ProtectedRoute>
        } />
        
        <Route path="/ride/:bikeId" element={
          <ProtectedRoute>
            <StatusCheck>
              <BikeRideScreen />
            </StatusCheck>
          </ProtectedRoute>
        } />
        
        {/* Default redirect to login */}
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </Router>
  );
}

// Protected route component
const ProtectedRoute = ({ children }) => {
  const navigate = useNavigate();
  const [loading, setLoading] = React.useState(true);
  const [isAuthenticated, setIsAuthenticated] = React.useState(false);
  
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(user => {
      setIsAuthenticated(!!user);
      setLoading(false);
      
      if (!user) {
        navigate('/login', { replace: true });
      }
    });
    
    return () => unsubscribe();
  }, [navigate]);
  
  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        backgroundColor: '#f5f5f5'
      }}>
        <div>
          <div style={{
            border: '4px solid #f3f3f3',
            borderTop: '4px solid #1D3C34',
            borderRadius: '50%',
            width: '40px',
            height: '40px',
            animation: 'spin 2s linear infinite',
            margin: '0 auto 20px'
          }}></div>
          <p style={{ color: '#666', textAlign: 'center' }}>Loading...</p>
          <style>{`
            @keyframes spin {
              0% { transform: rotate(0deg); }
              100% { transform: rotate(360deg); }
            }
          `}</style>
        </div>
      </div>
    );
  }
  
  return isAuthenticated ? children : null;
};

export default App;