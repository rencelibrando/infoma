import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { db } from '../firebase';
import { collection, getDocs, limit, query } from 'firebase/firestore';

const Container = styled.div`
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
`;

const Title = styled.h2`
  margin-bottom: 20px;
`;

const StatusCard = styled.div`
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
`;

const StatusItem = styled.div`
  margin-bottom: 10px;
  display: flex;
  align-items: center;
`;

const StatusDot = styled.span`
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 10px;
  background-color: ${props => props.status === 'ok' ? '#4CAF50' : 
    props.status === 'warning' ? '#FFC107' : 
    props.status === 'error' ? '#F44336' : '#CCCCCC'};
`;

const StatusLabel = styled.span`
  font-weight: 500;
  margin-right: 8px;
`;

const StatusValue = styled.span`
  color: #666;
`;

const Button = styled.button`
  background-color: #1D3C34;
  color: white;
  border: none;
  padding: 10px 15px;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 10px;
  
  &:hover {
    opacity: 0.9;
  }
`;

const ErrorDetails = styled.pre`
  background-color: #f5f5f5;
  padding: 10px;
  font-size: 12px;
  overflow: auto;
  margin-top: 10px;
  max-height: 200px;
  border-radius: 4px;
`;

const StatusCheck = ({ children }) => {
  const [status, setStatus] = useState({
    firebase: { status: 'checking', message: 'Checking connection...' },
    bikes: { status: 'checking', message: 'Checking bikes collection...' },
    users: { status: 'checking', message: 'Checking users collection...' },
    reviews: { status: 'checking', message: 'Checking reviews collection...' },
    recharts: { status: 'checking', message: 'Checking Recharts library...' },
    googleMaps: { status: 'checking', message: 'Checking Google Maps API...' }
  });
  
  const [loading, setLoading] = useState(true);
  const [errors, setErrors] = useState([]);
  const [checksComplete, setChecksComplete] = useState(false);
  
  const runChecks = async () => {
    setLoading(true);
    setErrors([]);
    
    // Reset status
    setStatus({
      firebase: { status: 'checking', message: 'Checking connection...' },
      bikes: { status: 'checking', message: 'Checking bikes collection...' },
      users: { status: 'checking', message: 'Checking users collection...' },
      reviews: { status: 'checking', message: 'Checking reviews collection...' },
      recharts: { status: 'checking', message: 'Checking Recharts library...' },
      googleMaps: { status: 'checking', message: 'Checking Google Maps API...' }
    });
    
    // Check Firebase connection
    try {
      // Check bikes collection
      const bikesQuery = query(collection(db, 'bikes'), limit(1));
      const bikesSnapshot = await getDocs(bikesQuery);
      const bikesCount = bikesSnapshot.size;
      
      setStatus(prev => ({
        ...prev,
        firebase: { status: 'ok', message: 'Connected successfully' },
        bikes: { 
          status: bikesCount > 0 ? 'ok' : 'warning', 
          message: bikesCount > 0 ? `Found ${bikesCount} bikes` : 'No bikes found' 
        }
      }));
      
      // Check users collection
      try {
        const usersQuery = query(collection(db, 'users'), limit(1));
        const usersSnapshot = await getDocs(usersQuery);
        const usersCount = usersSnapshot.size;
        
        setStatus(prev => ({
          ...prev,
          users: { 
            status: usersCount > 0 ? 'ok' : 'warning', 
            message: usersCount > 0 ? `Found ${usersCount} users` : 'No users found' 
          }
        }));
      } catch (error) {
        setStatus(prev => ({
          ...prev,
          users: { status: 'error', message: `Error: ${error.message}` }
        }));
        setErrors(prev => [...prev, { component: 'Users', error }]);
      }
      
      // Check reviews subcollection
      try {
        // Try to get a review from the first bike
        if (bikesSnapshot.size > 0) {
          const firstBikeId = bikesSnapshot.docs[0].id;
          const reviewsPath = `bikes/${firstBikeId}/reviews`;
          
          try {
            const reviewsQuery = query(collection(db, reviewsPath), limit(1));
            const reviewsSnapshot = await getDocs(reviewsQuery);
            
            setStatus(prev => ({
              ...prev,
              reviews: { 
                status: 'ok', 
                message: `Checked reviews for bike ${firstBikeId}` 
              }
            }));
          } catch (error) {
            setStatus(prev => ({
              ...prev,
              reviews: { 
                status: 'warning', 
                message: `Could not find reviews for first bike: ${error.message}` 
              }
            }));
          }
        } else {
          setStatus(prev => ({
            ...prev,
            reviews: { 
              status: 'warning', 
              message: 'No bikes to check for reviews' 
            }
          }));
        }
      } catch (error) {
        setStatus(prev => ({
          ...prev,
          reviews: { status: 'error', message: `Error: ${error.message}` }
        }));
        setErrors(prev => [...prev, { component: 'Reviews', error }]);
      }
      
    } catch (error) {
      setStatus(prev => ({
        ...prev,
        firebase: { status: 'error', message: `Error: ${error.message}` },
        bikes: { status: 'error', message: 'Could not check bikes' },
        users: { status: 'error', message: 'Could not check users' },
        reviews: { status: 'error', message: 'Could not check reviews' }
      }));
      setErrors(prev => [...prev, { component: 'Firebase', error }]);
    }
    
    // Check Recharts
    try {
      const rechartsAvailable = typeof require('recharts') !== 'undefined';
      setStatus(prev => ({
        ...prev,
        recharts: { 
          status: rechartsAvailable ? 'ok' : 'error', 
          message: rechartsAvailable ? 'Recharts is available' : 'Recharts not found' 
        }
      }));
    } catch (error) {
      setStatus(prev => ({
        ...prev,
        recharts: { status: 'error', message: `Error: ${error.message}` }
      }));
      setErrors(prev => [...prev, { component: 'Recharts', error }]);
    }
    
    // Check Google Maps API
    try {
      const googleMapsLoaded = window.google && window.google.maps;
      setStatus(prev => ({
        ...prev,
        googleMaps: { 
          status: googleMapsLoaded ? 'ok' : 'warning', 
          message: googleMapsLoaded ? 'Google Maps API loaded' : 'Google Maps API not yet loaded (may load on demand)' 
        }
      }));
    } catch (error) {
      setStatus(prev => ({
        ...prev,
        googleMaps: { status: 'warning', message: `Could not check: ${error.message}` }
      }));
    }
    
    setLoading(false);
    setChecksComplete(true);
  };
  
  useEffect(() => {
    runChecks();
  }, []);
  
  if (checksComplete && !errors.some(e => e.component === 'Firebase')) {
    return children;
  }
  
  return (
    <Container>
      <Title>System Status Check</Title>
      
      <StatusCard>
        <h3>Database & API Status</h3>
        {Object.entries(status).map(([key, value]) => (
          <StatusItem key={key}>
            <StatusDot status={value.status} />
            <StatusLabel>{key}: </StatusLabel>
            <StatusValue>{value.message}</StatusValue>
          </StatusItem>
        ))}
        
        <Button onClick={runChecks} disabled={loading}>
          {loading ? 'Running Checks...' : 'Run Checks Again'}
        </Button>
      </StatusCard>
      
      {errors.length > 0 && (
        <StatusCard>
          <h3>Errors</h3>
          {errors.map((error, index) => (
            <div key={index}>
              <p><strong>{error.component} Error:</strong></p>
              <ErrorDetails>
                {error.error.toString()}
                {error.error.stack && `\n${error.error.stack}`}
              </ErrorDetails>
            </div>
          ))}
        </StatusCard>
      )}
    </Container>
  );
};

export default StatusCheck; 