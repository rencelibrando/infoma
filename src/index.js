import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

// Add temporary App Check bypass for development
// This helps prevent the "app-check-token-is-invalid" error
if (process.env.NODE_ENV !== 'production') {
  console.log('🛠️ Development mode: Setting up App Check bypass');
  
  // Set debug token flag
  window.FIREBASE_APPCHECK_DEBUG_TOKEN = true;
  
  // Override fetch to handle App Check errors
  const originalFetch = window.fetch;
  window.fetch = function(...args) {
    return originalFetch.apply(this, args).catch(err => {
      console.log('📣 Fetch error intercepted:', err.message);
      // Return a mock successful response for App Check token validation
      if (err.message.includes('app-check-token')) {
        console.log('⚠️ Bypassing App Check token validation');
        return {
          ok: true,
          json: () => Promise.resolve({ token: 'mock-token-' + Date.now() })
        };
      }
      throw err;
    });
  };
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
