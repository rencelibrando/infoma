// src/components/Login.js
import React, { useState, useEffect } from 'react';
import { auth } from '../firebase';
import { signInWithEmailAndPassword } from 'firebase/auth';
import { useNavigate } from 'react-router-dom';
import styled, { keyframes } from 'styled-components';

const fadeIn = keyframes`
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
`;

const LoginContainer = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
  background-image: url('/images/bike-background.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(0,0,0,0.65) 0%, rgba(0,0,0,0.45) 100%);
    z-index: 1;
    transition: background 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  }

  @media (max-width: 992px) {
    flex-direction: column;
  }
`;

const CompanyName = styled.div`
  color: white;
  margin-right: 80px;
  margin-bottom: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  text-align: left;
  position: relative;
  z-index: 2;
  text-shadow: 0 5px 6px rgba(0, 0, 0, 0.3);
  animation: ${fadeIn} 0.8s ease-out;
  max-width: 500px;
  width: 100%;
  
  @media (max-width: 1000px) {
    margin-right: 0;
    margin-bottom: 40px;
    justify-content: center;
  }
`;

const CompanyTextContainer = styled.div`
  display: flex;
  flex-direction: column;
  margin-left: 20px;
  justify-content: center;
`;

const CompanyLogo = styled.span`
  font-size: 80px;
  font-weight: 1000;
  letter-spacing: 2px;
  display: block;
  line-height: 1;
  color: white;
  text-transform: uppercase;
  margin-bottom: 10px;
`;

const CompanyTagline = styled.span`
  font-size: 28px;
  font-weight: 500;
  display: block;
  letter-spacing: 2px;
  color: white;
`;

const LogoImage = styled.img`
  width: 120px;
  height: 120px;
  object-fit: contain;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.3));
`;

const LoginForm = styled.form`
  background: rgba(255, 255, 255, 0.75);
  padding: 40px;
  border-radius: 24px;
  box-shadow: 
    0 10px 30px rgba(0, 0, 0, 0.18),
    0 6px 12px rgba(0, 0, 0, 0.1),
    0 0 1px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  backdrop-filter: blur(12px);
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid rgba(255, 255, 255, 0.5);
  position: relative;
  z-index: 2;
  animation: ${fadeIn} 0.8s ease-out;
  transform: translateY(0) scale(1);
  
  &:hover, &:focus-within {
    background: rgba(255, 255, 255, 0.85);
    transform: translateY(-15px) scale(1.02);
    box-shadow: 
      0 20px 40px rgba(0, 0, 0, 0.25),
      0 12px 20px rgba(0, 0, 0, 0.15),
      0 0 1px rgba(0, 0, 0, 0.1);
    
    & ~ ${LoginContainer}::before {
      background: linear-gradient(135deg, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0.6) 100%);
    }
  }
  
  &::before {
    content: '';
    position: absolute;
    top: -2px;
    left: -2px;
    right: -2px;
    bottom: -2px;
    border-radius: 25px;
    background: linear-gradient(135deg, rgba(255,255,255,0.4), rgba(0,128,0,0.2));
    z-index: -1;
    opacity: 0;
    transition: opacity 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  }
  
  &:hover::before {
    opacity: 1;
  }
`;

const FormTitle = styled.h2`
  color: #1b5e20;
  font-size: 32px;
  margin-bottom: 25px;
  text-align: center;
  font-weight: 600;
  letter-spacing: 0.5px;
  position: relative;
  
  &::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: -10px;
    transform: translateX(-50%);
    width: 60px;
    height: 3px;
    background: linear-gradient(90deg, #43a047, #1b5e20);
    border-radius: 3px;
  }
`;

const FormGroup = styled.div`
  margin-bottom: 24px;
  position: relative;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
  }
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #2e7d32;
  transition: color 0.2s ease;
  
  ${FormGroup}:hover & {
    color: #1b5e20;
  }
`;

const Input = styled.input`
  width: 100%;
  padding: 14px 18px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.85);
  transition: all 0.3s ease;
  outline: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  text-indent: 0;
  box-sizing: border-box;
  
  &:focus {
    border-color: #4CAF50;
    box-shadow: 0 0 0 3px rgba(76, 175, 80, 0.25);
    background: rgba(255, 255, 255, 0.95);
  }
  
  &:hover {
    background: rgba(255, 255, 255, 0.95);
    border-color: rgba(76, 175, 80, 0.3);
  }
`;

const Button = styled.button`
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #4CAF50 0%, #2E7D32 100%);
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
  margin-top: 15px;
  box-shadow: 0 4px 12px rgba(46, 125, 50, 0.25);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
    transition: left 0.7s ease;
  }
  
  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #43a047 0%, #1b5e20 100%);
    transform: translateY(-3px);
    box-shadow: 0 6px 15px rgba(46, 125, 50, 0.35);
    
    &::before {
      left: 100%;
    }
  }
  
  &:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: 0 3px 8px rgba(46, 125, 50, 0.2);
  }
  
  &:disabled {
    background: linear-gradient(135deg, #cccccc 0%, #999999 100%);
    cursor: not-allowed;
    opacity: 0.7;
  }
`;

const ErrorMessage = styled.p`
  color: #d32f2f;
  background-color: rgba(211, 47, 47, 0.08);
  padding: 12px;
  border-radius: 12px;
  font-size: 14px;
  margin-bottom: 24px;
  text-align: center;
  border-left: 3px solid #d32f2f;
  animation: ${fadeIn} 0.3s ease-out;
`;

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [formFocused, setFormFocused] = useState(false);
  const [formHovered, setFormHovered] = useState(false);
  const navigate = useNavigate();

  // Check if user is already authenticated
  useEffect(() => {
    const checkAuth = () => {
      // If user is already authenticated, redirect to dashboard
      if (auth.currentUser) {
        // Get the last authenticated route or default to dashboard
        const lastRoute = sessionStorage.getItem('lastAuthRoute') || '/dashboard';
        navigate(lastRoute, { replace: true });
      }
    };
    
    // Check immediately
    checkAuth();
    
    // Also set up a listener for auth state changes
    const unsubscribe = auth.onAuthStateChanged(user => {
      if (user) {
        const lastRoute = sessionStorage.getItem('lastAuthRoute') || '/dashboard';
        navigate(lastRoute, { replace: true });
        
        // Clear browser history to prevent back navigation to login
        if (window.history && window.history.pushState) {
          // Add a dummy state to replace the login page in history
          window.history.pushState(null, document.title, window.location.href);
          
          // Replace login with dashboard in browser history
          window.history.replaceState({ path: lastRoute }, document.title, lastRoute);
        }
      }
    });
    
    // Clean up listener
    return () => unsubscribe();
  }, [navigate]);

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    try {
      // Sign in the user
      await signInWithEmailAndPassword(auth, email, password);
      
      // Add a state entry to history to prevent accidental navigation back
      if (window.history && window.history.pushState) {
        window.history.pushState({ secure: true }, document.title, window.location.href);
      }
      
      // No need to manually navigate - the auth state listener will handle redirection
    } catch (error) {
      console.error("Login error:", error);
      setError(error.message || 'Failed to sign in');
    } finally {
      setLoading(false);
    }
  };

  return (
    <LoginContainer className={(formFocused || formHovered) ? "form-active" : ""}>
      <CompanyName>
        <LogoImage src="/images/bambikelogo.png" alt="Bambike Logo" />
        <CompanyTextContainer>
          <CompanyLogo>Bambike</CompanyLogo>
          <CompanyTagline>Revolution Cycles</CompanyTagline>
        </CompanyTextContainer>
      </CompanyName>
      <LoginForm 
        onSubmit={handleLogin}
        onFocus={() => setFormFocused(true)}
        onBlur={(e) => {
          // Only unfocus if the focus is leaving the form entirely
          if (!e.currentTarget.contains(document.activeElement)) {
            setFormFocused(false);
          }
        }}
        onMouseEnter={() => setFormHovered(true)}
        onMouseLeave={() => setFormHovered(false)}
      >
        <FormTitle>Admin Login</FormTitle>
        {error && <ErrorMessage>{error}</ErrorMessage>}
        
        <FormGroup>
          <Label htmlFor="email">Email Address</Label>
          <Input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Enter your email"
            required
          />
        </FormGroup>
        
        <FormGroup>
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter your password"
            required
          />
        </FormGroup>
        
        <Button type="submit" disabled={loading}>
          {loading ? 'Authenticating...' : 'Log In'}
        </Button>
      </LoginForm>
    </LoginContainer>
  );
};

// Update CSS to apply darker background when form is hovered or focused
const globalStyle = document.createElement('style');
globalStyle.innerHTML = `
  .form-active::before {
    background: linear-gradient(135deg, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0.7) 100%) !important;
    transition: background 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  }
`;
document.head.appendChild(globalStyle);

export default Login;