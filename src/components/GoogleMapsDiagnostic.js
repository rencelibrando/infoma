import React, { useEffect, useState } from 'react';

const GoogleMapsDiagnostic = () => {
  const [diagnostics, setDiagnostics] = useState({
    apiKeyPresent: false,
    googleMapsLoaded: false,
    secureContext: false,
    networkStatus: 'unknown',
    browserInfo: '',
    timestamp: new Date().toISOString()
  });

  const apiKey = process.env.REACT_APP_GOOGLE_MAPS_API_KEY || "AIzaSyCmhSJa07ZS67ZcKnJOmwHHMz-qzKhjShE";

  useEffect(() => {
    const runDiagnostics = async () => {
      const newDiagnostics = {
        apiKeyPresent: !!apiKey && apiKey.length > 0,
        apiKeyValid: apiKey.startsWith('AIza'),
        googleMapsLoaded: !!(window.google?.maps),
        secureContext: window.isSecureContext,
        protocol: window.location.protocol,
        hostname: window.location.hostname,
        port: window.location.port,
        browserInfo: navigator.userAgent,
        timestamp: new Date().toISOString(),
        networkStatus: navigator.onLine ? 'online' : 'offline'
      };

      // Test network connectivity
      try {
        const response = await fetch('https://www.google.com/favicon.ico', {
          method: 'HEAD',
          mode: 'no-cors'
        });
        newDiagnostics.networkConnectivity = 'good';
      } catch (error) {
        newDiagnostics.networkConnectivity = 'poor';
        newDiagnostics.networkError = error.message;
      }

      // Test Google Maps API endpoint
      try {
        const testUrl = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places`;
        const response = await fetch(testUrl, { method: 'HEAD', mode: 'no-cors' });
        newDiagnostics.apiEndpointAccessible = true;
      } catch (error) {
        newDiagnostics.apiEndpointAccessible = false;
        newDiagnostics.apiError = error.message;
      }

      setDiagnostics(newDiagnostics);
    };

    runDiagnostics();

    // Check for Google Maps loading every second for 10 seconds
    const interval = setInterval(() => {
      if (window.google?.maps) {
        setDiagnostics(prev => ({
          ...prev,
          googleMapsLoaded: true,
          mapsLoadedAt: new Date().toISOString()
        }));
        clearInterval(interval);
      }
    }, 1000);

    // Clear interval after 10 seconds
    setTimeout(() => clearInterval(interval), 10000);

    return () => clearInterval(interval);
  }, [apiKey]);

  const getStatusColor = (status) => {
    if (status === true || status === 'good' || status === 'online') return '#4CAF50';
    if (status === false || status === 'poor' || status === 'offline') return '#f44336';
    return '#ff9800';
  };

  const getStatusIcon = (status) => {
    if (status === true || status === 'good' || status === 'online') return '✅';
    if (status === false || status === 'poor' || status === 'offline') return '❌';
    return '⚠️';
  };

  const copyDiagnostics = () => {
    const diagnosticsText = JSON.stringify(diagnostics, null, 2);
    navigator.clipboard.writeText(diagnosticsText).then(() => {
      alert('Diagnostics copied to clipboard!');
    });
  };

  return (
    <div style={{
      position: 'fixed',
      top: '20px',
      right: '20px',
      width: '350px',
      backgroundColor: 'white',
      border: '1px solid #ddd',
      borderRadius: '8px',
      padding: '15px',
      boxShadow: '0 4px 20px rgba(0, 0, 0, 0.15)',
      zIndex: 10000,
      fontSize: '12px',
      maxHeight: '80vh',
      overflowY: 'auto'
    }}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '15px',
        borderBottom: '1px solid #eee',
        paddingBottom: '10px'
      }}>
        <h3 style={{ margin: 0, fontSize: '16px', color: '#333' }}>
          🗺️ Google Maps Diagnostics
        </h3>
        <button
          onClick={copyDiagnostics}
          style={{
            backgroundColor: '#1D3C34',
            color: 'white',
            border: 'none',
            padding: '4px 8px',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '10px'
          }}
        >
          📋 Copy
        </button>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>API Key Present:</span>
          <span style={{ color: getStatusColor(diagnostics.apiKeyPresent) }}>
            {getStatusIcon(diagnostics.apiKeyPresent)} {diagnostics.apiKeyPresent.toString()}
          </span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>API Key Valid Format:</span>
          <span style={{ color: getStatusColor(diagnostics.apiKeyValid) }}>
            {getStatusIcon(diagnostics.apiKeyValid)} {diagnostics.apiKeyValid?.toString()}
          </span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>Google Maps Loaded:</span>
          <span style={{ color: getStatusColor(diagnostics.googleMapsLoaded) }}>
            {getStatusIcon(diagnostics.googleMapsLoaded)} {diagnostics.googleMapsLoaded.toString()}
          </span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>Secure Context:</span>
          <span style={{ color: getStatusColor(diagnostics.secureContext) }}>
            {getStatusIcon(diagnostics.secureContext)} {diagnostics.secureContext.toString()}
          </span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>Network Status:</span>
          <span style={{ color: getStatusColor(diagnostics.networkStatus) }}>
            {getStatusIcon(diagnostics.networkStatus)} {diagnostics.networkStatus}
          </span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>Protocol:</span>
          <span style={{ color: diagnostics.protocol === 'https:' ? '#4CAF50' : '#ff9800' }}>
            {diagnostics.protocol}
          </span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>Hostname:</span>
          <span>{diagnostics.hostname}</span>
        </div>

        {diagnostics.networkConnectivity && (
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>Network Connectivity:</span>
            <span style={{ color: getStatusColor(diagnostics.networkConnectivity) }}>
              {getStatusIcon(diagnostics.networkConnectivity)} {diagnostics.networkConnectivity}
            </span>
          </div>
        )}

        {diagnostics.mapsLoadedAt && (
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>Maps Loaded At:</span>
            <span style={{ color: '#4CAF50' }}>
              {new Date(diagnostics.mapsLoadedAt).toLocaleTimeString()}
            </span>
          </div>
        )}

        <div style={{
          marginTop: '10px',
          padding: '8px',
          backgroundColor: '#f8f9fa',
          borderRadius: '4px',
          fontSize: '10px'
        }}>
          <strong>API Key (first 20 chars):</strong><br/>
          <code>{apiKey.substring(0, 20)}...</code>
        </div>

        {(diagnostics.networkError || diagnostics.apiError) && (
          <div style={{
            marginTop: '10px',
            padding: '8px',
            backgroundColor: '#ffebee',
            borderRadius: '4px',
            fontSize: '10px',
            color: '#d32f2f'
          }}>
            <strong>Errors:</strong><br/>
            {diagnostics.networkError && <div>Network: {diagnostics.networkError}</div>}
            {diagnostics.apiError && <div>API: {diagnostics.apiError}</div>}
          </div>
        )}

        <div style={{
          marginTop: '10px',
          fontSize: '10px',
          color: '#666',
          textAlign: 'center'
        }}>
          Last updated: {new Date(diagnostics.timestamp).toLocaleTimeString()}
        </div>
      </div>
    </div>
  );
};

export default GoogleMapsDiagnostic; 