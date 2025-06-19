import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { ref, set, get, onValue, off } from 'firebase/database';
import { doc, setDoc, getDoc } from 'firebase/firestore';
import { db, realtimeDb, auth } from '../../firebase';

const SettingsContainer = styled.div`
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
`;

const SettingRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid #eee;
  
  &:last-child {
    border-bottom: none;
  }
`;

const SettingTitle = styled.div`
  h4 {
    margin: 0 0 4px 0;
    font-size: 16px;
  }
  
  p {
    margin: 0;
    font-size: 13px;
    color: #666;
  }
`;

const ToggleSwitch = styled.label`
  position: relative;
  display: inline-block;
  width: 60px;
  height: 34px;
  
  input {
    opacity: 0;
    width: 0;
    height: 0;
  }
  
  span {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #ccc;
    transition: .4s;
    border-radius: 34px;
  }
  
  span:before {
    position: absolute;
    content: "";
    height: 26px;
    width: 26px;
    left: 4px;
    bottom: 4px;
    background-color: white;
    transition: .4s;
    border-radius: 50%;
  }
  
  input:checked + span {
    background-color: #2D5A4C;
  }
  
  input:focus + span {
    box-shadow: 0 0 1px #2D5A4C;
  }
  
  input:checked + span:before {
    transform: translateX(26px);
  }
`;

const StatusIndicator = styled.div`
  display: flex;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
  
  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: 6px;
    background-color: ${props => props.active ? '#4CAF50' : '#F44336'};
  }
`;

const SaveButton = styled.button`
  background: #1D3C34;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 10px 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 16px;
  
  &:hover {
    background: #2D5A4C;
  }
  
  &:disabled {
    background: #cccccc;
    cursor: not-allowed;
  }
`;

const ErrorMessage = styled.div`
  background: #fee;
  border: 1px solid #fcc;
  border-radius: 4px;
  padding: 8px;
  margin-top: 8px;
  font-size: 12px;
  color: #c33;
`;

const TrackingSettings = () => {
  const [isLocationRestrictionEnabled, setIsLocationRestrictionEnabled] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [saveStatus, setSaveStatus] = useState('');
  const [errorDetails, setErrorDetails] = useState('');
  const [initialLoad, setInitialLoad] = useState(true);
  const [connectionStatus, setConnectionStatus] = useState('checking');
  
  // Admin verification states
  const [adminStatus, setAdminStatus] = useState({
    isChecking: true,
    isAdmin: false,
    currentUser: null,
    adminDocExists: false,
    userDocData: null,
    debugInfo: []
  });
  
  // Check admin status
  useEffect(() => {
    const checkAdminStatus = async () => {
      const debugInfo = [];
      
      try {
        const currentUser = auth.currentUser;
        debugInfo.push(`Current user: ${currentUser ? currentUser.email : 'None'}`);
        
        if (!currentUser) {
          setAdminStatus({
            isChecking: false,
            isAdmin: false,
            currentUser: null,
            adminDocExists: false,
            userDocData: null,
            debugInfo
          });
          return;
        }
        
        // Check if user is in admins collection
        try {
          const adminDoc = await getDoc(doc(db, 'admins', currentUser.uid));
          const adminDocExists = adminDoc.exists();
          debugInfo.push(`Admin doc exists: ${adminDocExists}`);
          
          if (adminDocExists) {
            debugInfo.push(`Admin doc data: ${JSON.stringify(adminDoc.data())}`);
          }
        } catch (adminError) {
          debugInfo.push(`Admin doc check error: ${adminError.message}`);
        }
        
        // Check user document for admin status
        let userDocData = null;
        try {
          const userDoc = await getDoc(doc(db, 'users', currentUser.uid));
          if (userDoc.exists()) {
            userDocData = userDoc.data();
            debugInfo.push(`User doc data: ${JSON.stringify(userDocData)}`);
          } else {
            debugInfo.push('User doc does not exist');
          }
        } catch (userError) {
          debugInfo.push(`User doc check error: ${userError.message}`);
        }
        
        // Determine admin status
        const isEmailAdmin = currentUser.email?.endsWith('@bambike.com') || 
                            currentUser.email?.includes('admin');
        const isDocAdmin = userDocData?.isAdmin === true || 
                          userDocData?.isAdmin === 'true' || 
                          userDocData?.role?.toLowerCase() === 'admin';
        
        debugInfo.push(`Email admin check: ${isEmailAdmin}`);
        debugInfo.push(`Doc admin check: ${isDocAdmin}`);
        
        const finalAdminStatus = isEmailAdmin || isDocAdmin;
        debugInfo.push(`Final admin status: ${finalAdminStatus}`);
        
        setAdminStatus({
          isChecking: false,
          isAdmin: finalAdminStatus,
          currentUser,
          adminDocExists: false, // We'll update this if needed
          userDocData,
          debugInfo
        });
        
      } catch (error) {
        debugInfo.push(`Admin check error: ${error.message}`);
        setAdminStatus({
          isChecking: false,
          isAdmin: false,
          currentUser: null,
          adminDocExists: false,
          userDocData: null,
          debugInfo
        });
      }
    };
    
    checkAdminStatus();
  }, []);
  
  // Test Firebase connection on component mount
  useEffect(() => {
    const testConnection = async () => {
      try {
        console.log('Testing Firebase connection...');
        
        // Test Realtime Database read
        const rtdbTestRef = ref(realtimeDb, '.info/connected');
        const unsubscribe = onValue(rtdbTestRef, (snapshot) => {
          const connected = snapshot.val();
          console.log('Realtime Database connected:', connected);
          setConnectionStatus(connected ? 'connected' : 'disconnected');
        });
        
        // Test Firestore read
        try {
          const testDoc = await getDoc(doc(db, 'app_config', 'settings'));
          console.log('Firestore connection test successful');
        } catch (firestoreError) {
          console.error('Firestore connection test failed:', firestoreError);
        }
        
        return () => unsubscribe();
      } catch (error) {
        console.error('Firebase connection test failed:', error);
        setConnectionStatus('error');
        setErrorDetails(`Connection test failed: ${error.message}`);
      }
    };
    
    testConnection();
  }, []);
  
  // Load current settings on component mount
  useEffect(() => {
    const loadSettings = async () => {
      try {
        console.log('Loading settings...');
        
        // First try to load from Realtime Database
        const settingsRef = ref(realtimeDb, 'app_config');
        
        // Get current value
        const snapshot = await get(settingsRef);
        if (snapshot.exists()) {
          const data = snapshot.val();
          console.log('Loaded settings from Realtime Database:', data);
          if (data.locationRestrictionEnabled !== undefined) {
            setIsLocationRestrictionEnabled(data.locationRestrictionEnabled);
          }
        } else {
          console.log('No settings found in Realtime Database, checking Firestore...');
          
          // Fallback to Firestore
          try {
            const firestoreDoc = await getDoc(doc(db, 'app_config', 'settings'));
            if (firestoreDoc.exists()) {
              const data = firestoreDoc.data();
              console.log('Loaded settings from Firestore:', data);
              if (data.locationRestrictionEnabled !== undefined) {
                setIsLocationRestrictionEnabled(data.locationRestrictionEnabled);
              }
            }
          } catch (firestoreError) {
            console.error('Error loading from Firestore:', firestoreError);
            setErrorDetails(`Failed to load settings: ${firestoreError.message}`);
          }
        }
        
        // Set up real-time listener for future changes
        const listener = onValue(settingsRef, (snapshot) => {
          if (snapshot.exists()) {
            const data = snapshot.val();
            console.log('Settings updated:', data);
            if (data.locationRestrictionEnabled !== undefined) {
              setIsLocationRestrictionEnabled(data.locationRestrictionEnabled);
            }
          }
        }, (error) => {
          console.error('Error in real-time listener:', error);
          setErrorDetails(`Real-time listener error: ${error.message}`);
        });
        
        // Clean up listener on unmount
        return () => {
          off(settingsRef, 'value', listener);
        };
        
      } catch (error) {
        console.error('Error loading settings:', error);
        setErrorDetails(`Failed to load settings: ${error.message}`);
      } finally {
        setInitialLoad(false);
      }
    };
    
    loadSettings();
  }, []);
  
  // Handler for the toggle switch
  const handleToggleLocationRestriction = () => {
    setIsLocationRestrictionEnabled(!isLocationRestrictionEnabled);
  };
  
  // Save settings to Firebase
  const saveSettings = async () => {
    setIsSaving(true);
    setSaveStatus('');
    setErrorDetails('');
    
    console.log('=== SAVE SETTINGS DEBUG START ===');
    console.log('Saving settings:', { locationRestrictionEnabled: isLocationRestrictionEnabled });
    
    // Debug current user and authentication
    const currentUser = auth.currentUser;
    console.log('Current user:', currentUser);
    console.log('User email:', currentUser?.email);
    console.log('User UID:', currentUser?.uid);
    console.log('User verified:', currentUser?.emailVerified);
    
    // Get and log the user token
    try {
      if (currentUser) {
        const token = await currentUser.getIdToken(true);
        console.log('User token (first 50 chars):', token.substring(0, 50) + '...');
        
        const tokenResult = await currentUser.getIdTokenResult();
        console.log('Token claims:', tokenResult.claims);
      }
    } catch (tokenError) {
      console.error('Error getting token:', tokenError);
    }
    
    try {
      // Test 1: Save to Realtime Database first (primary)
      console.log('=== REALTIME DATABASE SAVE TEST ===');
      const rtdbRef = ref(realtimeDb, 'app_config/locationRestrictionEnabled');
      await set(rtdbRef, isLocationRestrictionEnabled);
      console.log('✓ Realtime Database save successful');
      
      // Test 2: Save to Firestore
      console.log('=== FIRESTORE SAVE TEST ===');
      console.log('Attempting to save to Firestore path: app_config/settings');
      
      const firestoreRef = doc(db, 'app_config', 'settings');
      console.log('Firestore reference created:', firestoreRef.path);
      
      const dataToSave = {
        locationRestrictionEnabled: isLocationRestrictionEnabled,
        updatedAt: new Date(),
        updatedBy: 'admin_dashboard',
        userEmail: currentUser?.email,
        userUID: currentUser?.uid
      };
      console.log('Data to save:', dataToSave);
      
      await setDoc(firestoreRef, dataToSave, { merge: true });
      console.log('✓ Firestore save successful');
      
      setSaveStatus('success');
      console.log('=== SAVE SETTINGS DEBUG END - SUCCESS ===');
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        setSaveStatus('');
      }, 3000);
      
    } catch (error) {
      console.error('=== SAVE SETTINGS DEBUG END - ERROR ===');
      console.error('Full error object:', error);
      console.error('Error code:', error.code);
      console.error('Error message:', error.message);
      console.error('Error stack:', error.stack);
      
      setSaveStatus('error');
      
      // Provide detailed error information
      let errorMessage = error.message;
      if (error.code === 'permission-denied') {
        errorMessage = `Permission denied. User: ${currentUser?.email}, UID: ${currentUser?.uid}. Check Firestore rules.`;
      } else if (error.code === 'network-request-failed') {
        errorMessage = 'Network error. Please check your internet connection.';
      } else if (error.code === 'unavailable') {
        errorMessage = 'Firebase service is currently unavailable. Please try again later.';
      }
      
      setErrorDetails(errorMessage);
    } finally {
      setIsSaving(false);
    }
  };
  
  return (
    <SettingsContainer>
      <h3>Tracking Settings</h3>
      
      {/* Admin Status Debug Section */}
      <div style={{ 
        background: '#f8f9fa', 
        border: '1px solid #dee2e6', 
        borderRadius: '4px', 
        padding: '12px', 
        marginBottom: '16px',
        fontSize: '12px' 
      }}>
        <h4 style={{ margin: '0 0 8px 0', fontSize: '14px' }}>Admin Status Debug</h4>
        {adminStatus.isChecking ? (
          <div>Checking admin status...</div>
        ) : (
          <div>
            <div style={{ fontWeight: 'bold', color: adminStatus.isAdmin ? '#28a745' : '#dc3545' }}>
              Admin Status: {adminStatus.isAdmin ? '✓ ADMIN' : '✗ NOT ADMIN'}
            </div>
            <div style={{ marginTop: '8px' }}>
              <strong>Debug Info:</strong>
              <ul style={{ margin: '4px 0', paddingLeft: '16px' }}>
                {adminStatus.debugInfo.map((info, index) => (
                  <li key={index}>{info}</li>
                ))}
              </ul>
            </div>
          </div>
        )}
      </div>
      
      {connectionStatus === 'checking' && (
        <div style={{ color: '#666', fontSize: '12px', marginBottom: '10px' }}>
          Checking Firebase connection...
        </div>
      )}
      
      {connectionStatus === 'disconnected' && (
        <div style={{ color: '#f44336', fontSize: '12px', marginBottom: '10px' }}>
          ⚠️ Firebase Realtime Database is disconnected
        </div>
      )}
      
      {connectionStatus === 'connected' && (
        <div style={{ color: '#4caf50', fontSize: '12px', marginBottom: '10px' }}>
          ✓ Firebase connected
        </div>
      )}
      
      {!adminStatus.isAdmin && !adminStatus.isChecking && (
        <div style={{ 
          background: '#fff3cd', 
          border: '1px solid #ffeaa7', 
          borderRadius: '4px', 
          padding: '12px', 
          marginBottom: '16px',
          color: '#856404'
        }}>
          ⚠️ You need admin privileges to modify tracking settings. Please contact an administrator.
        </div>
      )}
      
      <SettingRow>
        <SettingTitle>
          <h4>Location Restriction</h4>
          <p>When enabled, users must be within the Intramuros area to start rides.</p>
          <p>Disabling will remove the location boundary restriction for all users.</p>
          {saveStatus === 'success' && (
            <StatusIndicator active={true}>
              <span /><em>Settings saved successfully!</em>
            </StatusIndicator>
          )}
          {saveStatus === 'error' && (
            <StatusIndicator active={false}>
              <span /><em>Error saving settings. Please try again.</em>
            </StatusIndicator>
          )}
          {errorDetails && (
            <ErrorMessage>
              <strong>Error details:</strong> {errorDetails}
            </ErrorMessage>
          )}
        </SettingTitle>
        <div>
          <ToggleSwitch>
            <input 
              type="checkbox" 
              checked={isLocationRestrictionEnabled}
              onChange={handleToggleLocationRestriction}
              disabled={!adminStatus.isAdmin}
            />
            <span></span>
          </ToggleSwitch>
          <div style={{ textAlign: 'center', marginTop: '4px', fontSize: '12px' }}>
            {isLocationRestrictionEnabled ? 'Enabled' : 'Disabled'}
          </div>
        </div>
      </SettingRow>
      
      <SaveButton 
        onClick={saveSettings}
        disabled={isSaving || connectionStatus === 'disconnected' || !adminStatus.isAdmin}
      >
        {isSaving ? 'Saving...' : 'Save Settings'}
      </SaveButton>
      
      <div style={{ fontSize: '11px', color: '#999', marginTop: '8px' }}>
        Current value: {isLocationRestrictionEnabled ? 'Enabled' : 'Disabled'}
        {initialLoad && ' (Loading...)'}
      </div>
    </SettingsContainer>
  );
};

export default TrackingSettings; 