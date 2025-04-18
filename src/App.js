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

// Enhanced protected route component
const ProtectedRoute = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const user = auth.currentUser;
  
  useEffect(() => {
    // If this is a protected route, save it as the last authenticated route
    if (user) {
      sessionStorage.setItem('lastAuthRoute', location.pathname);
    }
  }, [location.pathname, user]);
  
  if (!user) {
    // Redirect to login if not authenticated
    return <Navigate to="/login" replace />;
  }
  
  return children;
};

function App() {
  return (
    <Router>
      {/* History management component */}
      <AuthHistoryHandler />
      
      {/* Preload Google Maps API */}
      <GoogleMapsPreloader />
      
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/status" element={<StatusCheck />} />
        <Route 
          path="/dashboard" 
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/ride/:bikeId" 
          element={
            <ProtectedRoute>
              <BikeRideScreen />
            </ProtectedRoute>
          } 
        />
        
        {/* Catch all route to redirect to dashboard if authenticated */}
        <Route path="*" element={
          auth.currentUser ? 
            <Navigate to="/dashboard" replace /> : 
            <Navigate to="/login" replace />
        } />
      </Routes>
    </Router>
  );
}

export default App;