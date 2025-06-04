import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { format } from 'date-fns';
import { getMessages, updateMessageStatus, sendResponse, getFaqs, addFaq, updateFaq, deleteFaq } from '../services/supportService';

// Enhanced modern color palette
const colors = {
  primary: '#065f46', // Dark green (emerald-800)
  primaryLight: '#047857', // emerald-700
  primaryDark: '#064e3b', // emerald-900
  secondary: '#134e4a', // Dark teal green
  secondaryLight: '#0f766e',
  accent: '#f59e0b', // Amber
  accentLight: '#fbbf24',
  gray50: '#f9fafb',
  gray100: '#f3f4f6',
  gray200: '#e5e7eb',
  gray300: '#d1d5db',
  gray400: '#9ca3af',
  gray500: '#6b7280',
  gray600: '#4b5563',
  gray700: '#374151',
  gray800: '#1f2937',
  gray900: '#111827',
  white: '#ffffff',
  success: '#047857', // Dark green for success
  warning: '#f59e0b',
  error: '#ef4444',
  green: '#047857', // Dark green
  purple: '#8b5cf6',
  pink: '#ec4899',
  indigo: '#6366f1'
};

// Enhanced SVG Icons with better styling
const IconMessage = ({ size = 18 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M20 4H4C2.9 4 2.01 4.9 2.01 6L2 18C2 19.1 2.9 20 4 20H20C21.1 20 22 19.1 22 18V6C22 4.9 21.1 4 20 4ZM20 18H4V8L12 13L20 8V18ZM12 11L4 6H20L12 11Z" fill="currentColor"/>
  </svg>
);

const IconFaq = ({ size = 18 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM12 20C7.59 20 4 16.41 4 12C4 7.59 7.59 4 12 4C16.41 4 20 7.59 20 12C20 16.41 16.41 20 12 20ZM12 16C12.83 16 13.5 15.33 13.5 14.5C13.5 13.67 12.83 13 12 13C11.17 13 10.5 13.67 10.5 14.5C10.5 15.33 11.17 16 12 16ZM11 12H13V7H11V12Z" fill="currentColor"/>
  </svg>
);

const IconClock = ({ size = 18 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor">
    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/>
  </svg>
);

const IconAdd = ({ size = 18 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M19 13H13V19H11V13H5V11H11V5H13V11H19V13Z" fill="currentColor"/>
  </svg>
);

const IconEdit = ({ size = 16 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M3 17.25V21H6.75L17.81 9.94L14.06 6.19L3 17.25ZM20.71 7.04C21.1 6.65 21.1 6.02 20.71 5.63L18.37 3.29C17.98 2.9 17.35 2.9 16.96 3.29L15.13 5.12L18.88 8.87L20.71 7.04Z" fill="currentColor"/>
  </svg>
);

const IconDelete = ({ size = 16 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M6 19C6 20.1 6.9 21 8 21H16C17.1 21 18 20.1 18 19V7H6V19ZM19 4H15.5L14.5 3H9.5L8.5 4H5V6H19V4Z" fill="currentColor"/>
  </svg>
);

const IconSave = ({ size = 16 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M9 16.2L4.8 12L3.4 13.4L9 19L21 7L19.6 5.6L9 16.2Z" fill="currentColor"/>
  </svg>
);

const IconReply = ({ size = 16 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor">
    <path d="M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z"/>
  </svg>
);

const IconSearch = ({ size = 18 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M21 21L16.514 16.506L21 21ZM19 10.5C19 15.194 15.194 19 10.5 19C5.806 19 2 15.194 2 10.5C2 5.806 5.806 2 10.5 2C15.194 2 19 5.806 19 10.5Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);

const IconFilter = ({ size = 18 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M3 4C3 3.44772 3.44772 3 4 3H20C20.5523 3 21 3.44772 21 4C21 4.55228 20.5523 5 20 5H4C3.44772 5 3 4.55228 3 4Z" fill="currentColor"/>
    <path d="M3 12C3 11.4477 3.44772 11 4 11H14C14.5523 11 15 11.4477 15 12C15 12.5523 14.5523 13 14 13H4C3.44772 13 3 12.5523 3 12Z" fill="currentColor"/>
    <path d="M3 20C3 19.4477 3.44772 19 4 19H10C10.5523 19 11 19.4477 11 20C11 20.5523 10.5523 21 10 21H4C3.44772 21 3 20.5523 3 20Z" fill="currentColor"/>
  </svg>
);

// Modern Container with improved layout
const Container = styled.div`
  padding: 32px;
  max-width: 1400px;
  margin: 0 auto;
  background: linear-gradient(135deg, ${colors.gray50} 0%, ${colors.white} 100%);
  min-height: 100vh;
  
  @media (max-width: 768px) {
    padding: 20px 16px;
  }
`;

// Enhanced title with modern typography
const Title = styled.h1`
  margin-bottom: 32px;
  color: ${colors.gray900};
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, ${colors.primary} 0%, ${colors.primaryLight} 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  position: relative;
  letter-spacing: -0.025em;
  
  &::after {
    content: '';
    position: absolute;
    bottom: -8px;
    left: 0;
    width: 60px;
    height: 4px;
    background: linear-gradient(90deg, ${colors.primary}, ${colors.secondary});
    border-radius: 2px;
  }
  
  @media (max-width: 768px) {
    font-size: 28px;
    margin-bottom: 24px;
  }
`;

// Enhanced search container with modern design
const SearchContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
  background: ${colors.white};
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  border: 1px solid ${colors.gray200};
  
  @media (max-width: 768px) {
    flex-direction: column;
    gap: 12px;
  }
`;

// Modern search input with enhanced styling
const SearchInput = styled.div`
  position: relative;
  flex: 1;
  
  input {
    width: 100%;
    padding: 12px 16px 12px 44px;
    border: 2px solid ${colors.gray200};
    border-radius: 12px;
    font-size: 14px;
    background: ${colors.gray50};
    transition: all 0.2s ease;
    
    &:focus {
      outline: none;
      border-color: ${colors.primary};
      background: ${colors.white};
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }
    
    &::placeholder {
      color: ${colors.gray400};
    }
  }
  
  .search-icon {
    position: absolute;
    left: 14px;
    top: 50%;
    transform: translateY(-50%);
    color: ${colors.gray400};
    transition: color 0.2s ease;
  }
  
  &:focus-within .search-icon {
    color: ${colors.primary};
  }
`;

// Enhanced select with modern styling
const Select = styled.div`
  position: relative;
  min-width: 180px;
  
  select {
    width: 100%;
    padding: 12px 16px;
    border: 2px solid ${colors.gray200};
    border-radius: 12px;
    background: ${colors.gray50};
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;
    appearance: none;
    background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='m6 8 4 4 4-4'/%3e%3c/svg%3e");
    background-position: right 12px center;
    background-repeat: no-repeat;
    background-size: 16px;
    padding-right: 40px;
    
    &:focus {
      outline: none;
      border-color: ${colors.primary};
      background: ${colors.white};
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }
  }
  
  @media (max-width: 768px) {
    min-width: 100%;
  }
`;

// Enhanced button with modern design system
const Button = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
  
  ${props => {
    if (props.primary) {
      return `
        background: linear-gradient(135deg, ${colors.primary} 0%, ${colors.primaryLight} 100%);
        color: ${colors.white};
        box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.3);
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 10px 15px -3px rgba(59, 130, 246, 0.4);
        }
      `;
    } else if (props.secondary) {
      return `
        background: ${colors.white};
        color: ${colors.gray700};
        border: 2px solid ${colors.gray200};
        
        &:hover {
          background: ${colors.gray50};
          border-color: ${colors.gray300};
        }
      `;
    } else if (props.success) {
      return `
        background: linear-gradient(135deg, ${colors.success} 0%, ${colors.secondaryLight} 100%);
        color: ${colors.white};
        box-shadow: 0 4px 6px -1px rgba(16, 185, 129, 0.3);
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 10px 15px -3px rgba(16, 185, 129, 0.4);
        }
      `;
    } else if (props.danger) {
      return `
        background: linear-gradient(135deg, ${colors.error} 0%, #f87171 100%);
        color: ${colors.white};
        box-shadow: 0 4px 6px -1px rgba(239, 68, 68, 0.3);
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 10px 15px -3px rgba(239, 68, 68, 0.4);
        }
      `;
    } else {
      return `
        background: linear-gradient(135deg, ${colors.gray600} 0%, ${colors.gray700} 100%);
        color: ${colors.white};
        box-shadow: 0 4px 6px -1px rgba(75, 85, 99, 0.3);
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 10px 15px -3px rgba(75, 85, 99, 0.4);
        }
      `;
    }
  }}
  
  &:active {
    transform: translateY(0);
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none !important;
    box-shadow: none !important;
  }
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
    transition: left 0.6s;
  }
  
  &:hover::before {
    left: 100%;
  }
`;

// Enhanced messages container with improved card design
const MessagesContainer = styled.div`
  background: ${colors.white};
  border-radius: 20px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  margin-top: 24px;
  overflow: hidden;
  border: 1px solid ${colors.gray100};
`;

// Enhanced message item with modern hover effects
const MessageItem = styled.div`
  padding: 24px;
  border-bottom: 1px solid ${colors.gray100};
  display: flex;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    width: 4px;
    height: 100%;
    background: ${props => {
      switch (props.status) {
        case 'new': return colors.error;
        case 'in-progress': return colors.warning;
        case 'resolved': return colors.success;
        default: return colors.gray300;
      }
    }};
    transform: scaleY(0);
    transition: transform 0.3s ease;
  }
  
  &:hover {
    background: linear-gradient(135deg, ${colors.gray50} 0%, ${colors.green}05 100%);
    transform: translateX(8px);
    
    &::before {
      transform: scaleY(1);
    }
  }
  
  &:last-child {
    border-bottom: none;
  }
`;

// Enhanced avatar with better styling
const MessageAvatar = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, ${colors.primary} 0%, ${colors.primaryLight} 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  color: ${colors.white};
  margin-right: 16px;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: -2px;
    left: -2px;
    right: -2px;
    bottom: -2px;
    background: linear-gradient(135deg, ${colors.primary}, ${colors.secondary}, ${colors.accent});
    border-radius: 50%;
    z-index: -1;
    opacity: 0;
    transition: opacity 0.3s ease;
  }
  
  &:hover::before {
    opacity: 1;
  }
`;

const AvatarImage = styled.img`
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
`;

// Enhanced message content with better typography
const MessageContent = styled.div`
  flex: 1;
  min-width: 0;
`;

const MessageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
  
  @media (max-width: 768px) {
    flex-direction: column;
    gap: 4px;
  }
`;

const UserName = styled.div`
  font-weight: 600;
  color: ${colors.gray900};
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
`;

const MessageDate = styled.div`
  font-size: 12px;
  color: ${colors.gray500};
  font-weight: 500;
  white-space: nowrap;
`;

const MessagePreview = styled.div`
  color: ${colors.gray600};
  line-height: 1.5;
  margin-top: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
`;

const MessageSubject = styled.div`
  font-weight: 600;
  color: ${colors.gray800};
  margin-bottom: 4px;
  font-size: 15px;
`;

// Enhanced status badge with modern design
const StatusBadge = styled.span`
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  
  ${props => {
    switch (props.status) {
      case 'new':
        return `
          background: linear-gradient(135deg, ${colors.error}15 0%, ${colors.error}25 100%);
          color: ${colors.error};
          border: 1px solid ${colors.error}30;
        `;
      case 'in-progress':
        return `
          background: linear-gradient(135deg, ${colors.warning}15 0%, ${colors.warning}25 100%);
          color: ${colors.warning};
          border: 1px solid ${colors.warning}30;
        `;
      case 'resolved':
        return `
          background: linear-gradient(135deg, ${colors.success}15 0%, ${colors.success}25 100%);
          color: ${colors.success};
          border: 1px solid ${colors.success}30;
        `;
      case 'open':
        return `
          background: linear-gradient(135deg, ${colors.success}15 0%, ${colors.success}25 100%);
          color: ${colors.success};
          border: 1px solid ${colors.success}30;
        `;
      case 'closed':
        return `
          background: linear-gradient(135deg, ${colors.gray400}15 0%, ${colors.gray400}25 100%);
          color: ${colors.gray600};
          border: 1px solid ${colors.gray400}30;
        `;
      default:
        return `
          background: linear-gradient(135deg, ${colors.gray400}15 0%, ${colors.gray400}25 100%);
          color: ${colors.gray600};
          border: 1px solid ${colors.gray400}30;
        `;
    }
  }}
`;

// Enhanced modal with modern design
const Modal = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 20px;
  animation: fadeIn 0.3s ease;
  
  @keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
  }
`;

const ModalContent = styled.div`
  background: ${colors.white};
  border-radius: 24px;
  width: 100%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 32px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid ${colors.gray200};
  animation: slideUp 0.3s ease;
  
  @keyframes slideUp {
    from { 
      opacity: 0;
      transform: translateY(20px) scale(0.95);
    }
    to { 
      opacity: 1;
      transform: translateY(0) scale(1);
    }
  }
  
  @media (max-width: 768px) {
    padding: 24px;
    margin: 16px;
    max-height: calc(100vh - 32px);
  }
`;

const ModalHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid ${colors.gray100};
`;

const ModalTitle = styled.h3`
  margin: 0;
  color: ${colors.gray900};
  font-size: 24px;
  font-weight: 700;
`;

const CloseButton = styled.button`
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: ${colors.gray400};
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  
  &:hover {
    color: ${colors.gray600};
    background: ${colors.gray100};
  }
`;

// Enhanced user info section
const UserInfo = styled.div`
  display: flex;
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, ${colors.gray50} 0%, ${colors.green}05 100%);
  border-radius: 16px;
  border: 1px solid ${colors.gray200};
`;

const UserDetails = styled.div`
  margin-left: 16px;
  flex: 1;
`;

const UserInfoItem = styled.div`
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  
  strong {
    color: ${colors.gray700};
    font-weight: 600;
    min-width: 80px;
  }
  
  span {
    color: ${colors.gray900};
  }
`;

// Enhanced message display
const FullMessage = styled.div`
  padding: 20px;
  border: 2px solid ${colors.gray100};
  border-radius: 16px;
  line-height: 1.6;
  color: ${colors.gray700};
  margin-bottom: 24px;
  background: ${colors.white};
  font-size: 15px;
`;

// Enhanced admin response section
const AdminResponseSection = styled.div`
  margin-bottom: 24px;
  border-left: 4px solid ${colors.primary};
  background: linear-gradient(135deg, ${colors.primary}05 0%, ${colors.primary}10 100%);
  border-radius: 0 16px 16px 0;
  overflow: hidden;
`;

const AdminResponseHeader = styled.div`
  padding: 16px 20px 12px;
  background: linear-gradient(135deg, ${colors.primary}10 0%, ${colors.primary}15 100%);
  border-bottom: 1px solid ${colors.primary}20;
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: ${colors.primary};
  font-size: 14px;
`;

const AdminResponseContent = styled.div`
  padding: 20px;
  line-height: 1.6;
  color: ${colors.gray700};
  font-size: 15px;
`;

const AdminResponseDate = styled.div`
  font-size: 12px;
  color: ${colors.gray500};
  font-weight: 500;
  margin-left: auto;
`;

// Enhanced response indicator
const ResponseIndicator = styled.div`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, ${colors.primary}15 0%, ${colors.primary}25 100%);
  color: ${colors.primary};
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid ${colors.primary}30;
  
  svg {
    width: 12px;
    height: 12px;
  }
`;

// Enhanced response section
const ResponseSection = styled.div`
  margin-top: 24px;
`;

const ResponseTextarea = styled.textarea`
  width: 100%;
  padding: 16px;
  border: 2px solid ${colors.gray200};
  border-radius: 16px;
  min-height: 150px;
  resize: vertical;
  margin-bottom: 16px;
  font-size: 14px;
  line-height: 1.5;
  background: ${colors.gray50};
  transition: all 0.2s ease;
  
  &:focus {
    outline: none;
    border-color: ${colors.primary};
    background: ${colors.white};
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
  
  &::placeholder {
    color: ${colors.gray400};
  }
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
  
  @media (max-width: 768px) {
    flex-direction: column-reverse;
  }
`;

// Enhanced loading and empty states
const LoadingMessage = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  font-style: italic;
  color: ${colors.gray500};
  gap: 12px;
`;

const LoadingSpinner = styled.div`
  border: 3px solid ${colors.gray200};
  border-top: 3px solid ${colors.primary};
  border-radius: 50%;
  width: 24px;
  height: 24px;
  animation: spin 1s linear infinite;
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 80px 20px;
  color: ${colors.gray500};
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
`;

const EmptyStateIcon = styled.div`
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, ${colors.gray100} 0%, ${colors.gray200} 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  
  svg {
    width: 40px;
    height: 40px;
    color: ${colors.gray400};
  }
`;

// Enhanced tabs container
const TabsContainer = styled.div`
  display: flex;
  margin-bottom: 32px;
  background: ${colors.white};
  border-radius: 16px;
  padding: 6px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  border: 1px solid ${colors.gray200};
  overflow-x: auto;
  
  @media (max-width: 768px) {
    margin-bottom: 24px;
  }
`;

const Tab = styled.div`
  padding: 12px 24px;
  cursor: pointer;
  font-weight: 600;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  transition: all 0.2s ease;
  position: relative;
  flex: 1;
  justify-content: center;
  
  ${props => props.active ? `
    background: linear-gradient(135deg, ${colors.primary} 0%, ${colors.primaryLight} 100%);
    color: ${colors.white};
    box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.3);
  ` : `
    color: ${colors.gray600};
    
    &:hover {
      color: ${colors.gray800};
      background: ${colors.gray50};
    }
  `}
  
  @media (max-width: 768px) {
    padding: 10px 16px;
    font-size: 14px;
  }
`;

// FAQ specific styles with enhanced design
const FAQContainer = styled.div`
  margin-top: 24px;
`;

const FAQItem = styled.div`
  margin-bottom: 16px;
  background: ${colors.white};
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid ${colors.gray200};
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15);
  }
`;

const FAQHeader = styled.div`
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  background: ${props => props.isOpen ? 
    `linear-gradient(135deg, ${colors.gray50} 0%, ${colors.green}05 100%)` : 
    colors.white
  };
  transition: all 0.2s ease;
  
  &:hover {
    background: linear-gradient(135deg, ${colors.gray50} 0%, ${colors.green}05 100%);
  }
`;

const FAQTitle = styled.div`
  font-weight: 600;
  color: ${colors.gray900};
  font-size: 16px;
  flex: 1;
  margin-right: 16px;
`;

const FAQContent = styled.div`
  padding: ${props => props.isOpen ? '20px' : '0'};
  height: ${props => props.isOpen ? 'auto' : '0'};
  overflow: hidden;
  transition: all 0.3s ease;
  border-top: ${props => props.isOpen ? `1px solid ${colors.gray200}` : 'none'};
  line-height: 1.6;
  color: ${colors.gray700};
  background: ${colors.gray50};
`;

const FAQActions = styled.div`
  display: flex;
  gap: 8px;
  align-items: center;
`;

const IconButton = styled.button`
  background: none;
  border: none;
  color: ${colors.gray400};
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  transition: all 0.2s ease;
  
  &:hover {
    color: ${colors.gray600};
    background: rgba(0, 0, 0, 0.05);
  }
`;

const FAQEditor = styled.div`
  margin: 24px 0;
  background: ${colors.white};
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  padding: 24px;
  border: 1px solid ${colors.gray200};
`;

const FormTitle = styled.h3`
  margin-bottom: 20px;
  color: ${colors.gray900};
  font-size: 20px;
  font-weight: 700;
  position: relative;
  padding-bottom: 12px;
  
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 40px;
    height: 3px;
    background: linear-gradient(90deg, ${colors.primary}, ${colors.secondary});
    border-radius: 2px;
  }
`;

// Operating hours styles
const OperatingHoursContainer = styled.div`
  margin-top: 24px;
  background: ${colors.white};
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  padding: 24px;
  border: 1px solid ${colors.gray200};
`;

const OperatingHoursTable = styled.table`
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
`;

const OperatingHoursRow = styled.tr`
  background: ${props => props.index % 2 === 0 ? colors.white : colors.gray50};
  transition: all 0.2s ease;
  
  &:hover {
    background: linear-gradient(135deg, ${colors.green}05 0%, ${colors.green}10 100%);
  }
`;

const OperatingHoursCell = styled.td`
  padding: 16px;
  border-bottom: 1px solid ${colors.gray200};
  color: ${colors.gray700};
  font-weight: 500;
  
  &:last-child {
    border-bottom: none;
  }
`;

const OperatingHoursHead = styled.th`
  padding: 16px;
  text-align: left;
  font-weight: 600;
  background: linear-gradient(135deg, ${colors.gray100} 0%, ${colors.gray200} 100%);
  color: ${colors.gray800};
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
`;

const TimeInput = styled.input`
  width: 120px;
  padding: 8px 12px;
  border: 2px solid ${colors.gray200};
  border-radius: 8px;
  background: ${colors.gray50};
  transition: all 0.2s ease;
  
  &:focus {
    outline: none;
    border-color: ${colors.primary};
    background: ${colors.white};
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const Input = styled.input`
  display: block;
  width: 100%;
  padding: 14px 16px;
  margin-bottom: 16px;
  border: 2px solid ${colors.gray200};
  border-radius: 12px;
  font-size: 14px;
  background: ${colors.gray50};
  transition: all 0.2s ease;
  
  &:focus {
    outline: none;
    border-color: ${colors.primary};
    background: ${colors.white};
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
  
  &::placeholder {
    color: ${colors.gray400};
  }
`;

const Textarea = styled.textarea`
  display: block;
  width: 100%;
  padding: 14px 16px;
  margin-bottom: 16px;
  border: 2px solid ${colors.gray200};
  border-radius: 12px;
  font-size: 14px;
  min-height: 120px;
  resize: vertical;
  background: ${colors.gray50};
  transition: all 0.2s ease;
  line-height: 1.5;
  
  &:focus {
    outline: none;
    border-color: ${colors.primary};
    background: ${colors.white};
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
  
  &::placeholder {
    color: ${colors.gray400};
  }
`;

// MessageDetails container for the modal
const MessageDetails = styled.div`
  margin-bottom: 32px;
`;

// Helper function to get user initials
const getUserInitials = (name) => {
  if (!name) return '?';
  return name.split(' ').map(n => n[0]).join('').toUpperCase();
};

const CustomerSupportMessages = () => {
  const [messages, setMessages] = useState([]);
  const [filteredMessages, setFilteredMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [selectedMessage, setSelectedMessage] = useState(null);
  const [responseText, setResponseText] = useState('');
  
  // New state for tabs, FAQs, and operating hours
  const [activeTab, setActiveTab] = useState('messages');
  const [faqs, setFaqs] = useState([]);
  const [operatingHours, setOperatingHours] = useState([]);
  const [editingFAQ, setEditingFAQ] = useState(null);
  const [newFAQ, setNewFAQ] = useState({ question: '', answer: '' });
  const [openFAQs, setOpenFAQs] = useState({});
  const [editingHours, setEditingHours] = useState(false);
  const [tempOperatingHours, setTempOperatingHours] = useState([]);
  const [loadingOperation, setLoadingOperation] = useState({
    messages: false,
    faqs: false,
    saveResponse: false
  });

  // Fetch messages from Firebase
  useEffect(() => {
    const fetchMessages = async () => {
      setLoading(true);
      try {
        const messagesData = await getMessages();
        console.log('Fetched messages:', messagesData); // Debug log
        setMessages(messagesData);
        setFilteredMessages(messagesData);
      } catch (error) {
        console.error('Error fetching messages:', error);
      } finally {
        setLoading(false);
      }
    };
    
    // Fetch FAQs
    const fetchFaqs = async () => {
      setLoadingOperation(prev => ({ ...prev, faqs: true }));
      try {
        const faqData = await getFaqs();
        setFaqs(faqData);
      } catch (error) {
        console.error('Error fetching FAQs:', error);
      } finally {
        setLoadingOperation(prev => ({ ...prev, faqs: false }));
      }
    };
    
    fetchMessages();
    fetchFaqs();
  }, []);

  // Initialize operating hours separately
  useEffect(() => {
    // For now, we'll keep using mock operating hours
    const initialOperatingHours = [
      { day: 'Monday', open: '09:00', close: '18:00', closed: false },
      { day: 'Tuesday', open: '09:00', close: '18:00', closed: false },
      { day: 'Wednesday', open: '09:00', close: '18:00', closed: false },
      { day: 'Thursday', open: '09:00', close: '18:00', closed: false },
      { day: 'Friday', open: '09:00', close: '20:00', closed: false },
      { day: 'Saturday', open: '10:00', close: '16:00', closed: false },
      { day: 'Sunday', open: '10:00', close: '14:00', closed: true }
    ];
    
    setOperatingHours(initialOperatingHours);
    setTempOperatingHours(initialOperatingHours);
  }, []);

  // Filter messages based on search term and status
  useEffect(() => {
    console.log('Filtering messages. Total messages:', messages.length); // Debug log
    let result = messages;
    
    if (searchTerm) {
      const lowercasedSearch = searchTerm.toLowerCase();
      result = result.filter(message => 
        message.userName.toLowerCase().includes(lowercasedSearch) ||
        message.subject.toLowerCase().includes(lowercasedSearch) ||
        message.message.toLowerCase().includes(lowercasedSearch)
      );
    }
    
    if (statusFilter !== 'all') {
      result = result.filter(message => message.status === statusFilter);
    }
    
    console.log('Filtered messages:', result.length); // Debug log
    setFilteredMessages(result);
  }, [searchTerm, statusFilter, messages]);

  const handleMessageClick = (message) => {
    setSelectedMessage(message);
  };

  const handleCloseModal = () => {
    setSelectedMessage(null);
    setResponseText('');
  };

  const handleSubmitResponse = async () => {
    if (!responseText.trim()) return;
    
    setLoadingOperation(prev => ({ ...prev, saveResponse: true }));
    try {
      await sendResponse(selectedMessage.id, responseText);
      
      // Update messages list
      const updatedMessages = messages.map(msg => 
        msg.id === selectedMessage.id 
          ? {...msg, response: responseText, status: 'resolved'} 
          : msg
      );
      setMessages(updatedMessages);
      
      handleCloseModal();
    } catch (error) {
      console.error('Error sending response:', error);
      alert('Failed to send response. Please try again.');
    } finally {
      setLoadingOperation(prev => ({ ...prev, saveResponse: false }));
    }
  };
  
  const handleStatusChange = async (messageId, newStatus) => {
    try {
      await updateMessageStatus(messageId, newStatus);
      
      // Update messages list
      const updatedMessages = messages.map(msg => 
        msg.id === messageId ? {...msg, status: newStatus} : msg
      );
      setMessages(updatedMessages);
      
      if (selectedMessage && selectedMessage.id === messageId) {
        setSelectedMessage({...selectedMessage, status: newStatus});
      }
    } catch (error) {
      console.error('Error updating message status:', error);
      alert('Failed to update status. Please try again.');
    }
  };
  
  // FAQ functions
  const toggleFAQ = (id) => {
    setOpenFAQs(prev => ({
      ...prev,
      [id]: !prev[id]
    }));
  };
  
  const handleEditFAQ = (faq) => {
    setEditingFAQ(faq);
  };
  
  const handleDeleteFAQ = async (id) => {
    if (window.confirm('Are you sure you want to delete this FAQ?')) {
      try {
        await deleteFaq(id);
        setFaqs(faqs.filter(faq => faq.id !== id));
      } catch (error) {
        console.error('Error deleting FAQ:', error);
        alert('Failed to delete FAQ. Please try again.');
      }
    }
  };
  
  const handleSaveFAQ = async () => {
    if (!editingFAQ.question || !editingFAQ.answer) {
      alert('Please fill in both question and answer fields.');
      return;
    }
    
    try {
      if (editingFAQ.id) {
        // Update existing FAQ
        await updateFaq(editingFAQ.id, {
          question: editingFAQ.question,
          answer: editingFAQ.answer
        });
        
        setFaqs(faqs.map(faq => 
          faq.id === editingFAQ.id ? editingFAQ : faq
        ));
      } else {
        // Add new FAQ
        const newFaqData = await addFaq({
          question: editingFAQ.question,
          answer: editingFAQ.answer
        });
        
        setFaqs([...faqs, newFaqData]);
      }
      
      setEditingFAQ(null);
      setNewFAQ({ question: '', answer: '' });
    } catch (error) {
      console.error('Error saving FAQ:', error);
      alert('Failed to save FAQ. Please try again.');
    }
  };
  
  const handleCancelFAQ = () => {
    setEditingFAQ(null);
    setNewFAQ({ question: '', answer: '' });
  };
  
  // Operating hours functions
  const handleEditHours = () => {
    setEditingHours(true);
    setTempOperatingHours([...operatingHours]);
  };
  
  const handleSaveHours = () => {
    // This will be implemented when backend is ready
    setOperatingHours([...tempOperatingHours]);
    setEditingHours(false);
  };
  
  const handleCancelHours = () => {
    setTempOperatingHours([...operatingHours]);
    setEditingHours(false);
  };
  
  const handleHoursChange = (index, field, value) => {
    const updatedHours = [...tempOperatingHours];
    
    if (field === 'closed') {
      updatedHours[index].closed = !updatedHours[index].closed;
    } else {
      updatedHours[index][field] = value;
    }
    
    setTempOperatingHours(updatedHours);
  };

  return (
    <Container>
      <Title>Customer Support</Title>
      
      <TabsContainer>
        <Tab 
          active={activeTab === 'messages'} 
          onClick={() => setActiveTab('messages')}
        >
          <IconMessage /> Messages
        </Tab>
        <Tab 
          active={activeTab === 'faqs'} 
          onClick={() => setActiveTab('faqs')}
        >
          <IconFaq /> FAQs
        </Tab>
        <Tab 
          active={activeTab === 'hours'} 
          onClick={() => setActiveTab('hours')}
        >
          <IconClock /> Operating Hours
        </Tab>
      </TabsContainer>
      
      {activeTab === 'messages' && (
        <>
          <SearchContainer>
            <SearchInput>
              <div className="search-icon">
                <IconSearch />
              </div>
              <input
                type="text"
                placeholder="Search messages..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </SearchInput>
            <Select>
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="all">All statuses</option>
                <option value="new">New</option>
                <option value="in-progress">In Progress</option>
                <option value="resolved">Resolved</option>
              </select>
            </Select>
          </SearchContainer>
          
          <MessagesContainer>
            {loading ? (
              <LoadingMessage>
                <LoadingSpinner />
                Loading messages...
              </LoadingMessage>
            ) : filteredMessages.length === 0 ? (
              <EmptyState>
                <EmptyStateIcon>
                  <IconMessage size={40} />
                </EmptyStateIcon>
                <div>No messages found</div>
                <div style={{ fontSize: '14px', maxWidth: '400px' }}>
                  {searchTerm || statusFilter !== 'all' ? 
                    'Try adjusting your search or filter criteria' : 
                    'When customers send support messages, they will appear here'}
                </div>
              </EmptyState>
            ) : (
              <>
                {console.log('Rendering messages:', filteredMessages.length)}
                {filteredMessages.map(message => (
                  <MessageItem key={message.id} status={message.status} onClick={() => handleMessageClick(message)}>
                    <MessageAvatar>
                      {message.userAvatar ? (
                        <AvatarImage src={message.userAvatar} alt={message.userName} />
                      ) : (
                        getUserInitials(message.userName)
                      )}
                    </MessageAvatar>
                    <MessageContent>
                      <MessageHeader>
                        <UserName>
                          {message.userName}
                          <StatusBadge status={message.status}>
                            {message.status === 'in-progress' ? 'In Progress' : message.status}
                          </StatusBadge>
                          {message.response && (
                            <ResponseIndicator>
                              <IconReply />
                              Replied
                            </ResponseIndicator>
                          )}
                        </UserName>
                        <MessageDate>{format(message.dateCreated, 'MMM d, yyyy HH:mm')}</MessageDate>
                      </MessageHeader>
                      <MessageSubject>{message.subject}</MessageSubject>
                      <MessagePreview>{message.message}</MessagePreview>
                    </MessageContent>
                  </MessageItem>
                ))}
              </>
            )}
          </MessagesContainer>
          
          {selectedMessage && (
            <Modal>
              <ModalContent>
                <ModalHeader>
                  <ModalTitle>{selectedMessage.subject}</ModalTitle>
                  <CloseButton onClick={handleCloseModal}>&times;</CloseButton>
                </ModalHeader>
                
                <MessageDetails>
                  <UserInfo>
                    <MessageAvatar>
                      {selectedMessage.userAvatar ? (
                        <AvatarImage src={selectedMessage.userAvatar} alt={selectedMessage.userName} />
                      ) : (
                        getUserInitials(selectedMessage.userName)
                      )}
                    </MessageAvatar>
                    <UserDetails>
                      <UserInfoItem>
                        <strong>Name:</strong> <span>{selectedMessage.userName}</span>
                      </UserInfoItem>
                      <UserInfoItem>
                        <strong>Email:</strong> <span>{selectedMessage.userEmail}</span>
                      </UserInfoItem>
                      <UserInfoItem>
                        <strong>Phone:</strong> <span>{selectedMessage.userPhone}</span>
                      </UserInfoItem>
                      <UserInfoItem>
                        <strong>Date:</strong> <span>{format(selectedMessage.dateCreated, 'MMM d, yyyy HH:mm')}</span>
                      </UserInfoItem>
                      <UserInfoItem>
                        <strong>Status:</strong> 
                        <Select style={{marginLeft: '10px', minWidth: 'auto'}}>
                          <select
                            value={selectedMessage.status}
                            onChange={(e) => handleStatusChange(selectedMessage.id, e.target.value)}
                            style={{padding: '4px 8px'}}
                          >
                            <option value="new">New</option>
                            <option value="in-progress">In Progress</option>
                            <option value="resolved">Resolved</option>
                          </select>
                        </Select>
                      </UserInfoItem>
                    </UserDetails>
                  </UserInfo>
                  
                  <FullMessage>
                    {selectedMessage.message}
                  </FullMessage>
                  
                  {selectedMessage.response && (
                    <AdminResponseSection>
                      <AdminResponseHeader>
                        <div>Admin Response</div>
                        {selectedMessage.respondedAt && (
                          <AdminResponseDate>
                            {format(selectedMessage.respondedAt.toDate(), 'MMM d, yyyy HH:mm')}
                          </AdminResponseDate>
                        )}
                      </AdminResponseHeader>
                      <AdminResponseContent>
                        {selectedMessage.response}
                      </AdminResponseContent>
                    </AdminResponseSection>
                  )}
                  
                  {!selectedMessage.response && (
                    <ResponseSection>
                      <FormTitle>Reply to this message</FormTitle>
                      <ResponseTextarea 
                        value={responseText}
                        onChange={(e) => setResponseText(e.target.value)}
                        placeholder="Type your response here..."
                      />
                      <ButtonGroup>
                        <Button secondary onClick={handleCloseModal}>Cancel</Button>
                        <Button primary onClick={handleSubmitResponse}>
                          <IconSave /> Send Response
                        </Button>
                      </ButtonGroup>
                    </ResponseSection>
                  )}
                </MessageDetails>
              </ModalContent>
            </Modal>
          )}
        </>
      )}
      
      {activeTab === 'faqs' && (
        <FAQContainer>
          <Button primary onClick={() => setEditingFAQ({ id: null, question: '', answer: '' })}>
            <IconAdd /> Add New FAQ
          </Button>
          
          {editingFAQ && (
            <FAQEditor>
              <FormTitle>{editingFAQ.id ? 'Edit FAQ' : 'Add New FAQ'}</FormTitle>
              <Input 
                placeholder="Question"
                value={editingFAQ.question}
                onChange={(e) => setEditingFAQ({...editingFAQ, question: e.target.value})}
              />
              <Textarea 
                placeholder="Answer"
                value={editingFAQ.answer}
                onChange={(e) => setEditingFAQ({...editingFAQ, answer: e.target.value})}
              />
              <ButtonGroup>
                <Button secondary onClick={handleCancelFAQ}>Cancel</Button>
                <Button success onClick={handleSaveFAQ}>
                  <IconSave /> Save
                </Button>
              </ButtonGroup>
            </FAQEditor>
          )}
          
          {faqs.length === 0 ? (
            <EmptyState>
              <EmptyStateIcon>
                <IconFaq size={40} />
              </EmptyStateIcon>
              <div>No FAQs found</div>
              <div style={{ fontSize: '14px' }}>Add one to get started</div>
            </EmptyState>
          ) : (
            faqs.map(faq => (
              <FAQItem key={faq.id}>
                <FAQHeader 
                  isOpen={openFAQs[faq.id]} 
                  onClick={() => toggleFAQ(faq.id)}
                >
                  <FAQTitle>{faq.question}</FAQTitle>
                  <FAQActions onClick={e => e.stopPropagation()}>
                    <IconButton onClick={() => handleEditFAQ(faq)}>
                      <IconEdit />
                    </IconButton>
                    <IconButton onClick={() => handleDeleteFAQ(faq.id)}>
                      <IconDelete />
                    </IconButton>
                    {openFAQs[faq.id] ? '▲' : '▼'}
                  </FAQActions>
                </FAQHeader>
                <FAQContent isOpen={openFAQs[faq.id]}>
                  {faq.answer}
                </FAQContent>
              </FAQItem>
            ))
          )}
        </FAQContainer>
      )}
      
      {activeTab === 'hours' && (
        <OperatingHoursContainer>
          <FormTitle>Operating Hours</FormTitle>
          
          {!editingHours ? (
            <>
              <OperatingHoursTable>
                <thead>
                  <tr>
                    <OperatingHoursHead>Day</OperatingHoursHead>
                    <OperatingHoursHead>Open</OperatingHoursHead>
                    <OperatingHoursHead>Close</OperatingHoursHead>
                    <OperatingHoursHead>Status</OperatingHoursHead>
                  </tr>
                </thead>
                <tbody>
                  {operatingHours.map((hours, index) => (
                    <OperatingHoursRow key={hours.day} index={index}>
                      <OperatingHoursCell>{hours.day}</OperatingHoursCell>
                      <OperatingHoursCell>{hours.closed ? '-' : hours.open}</OperatingHoursCell>
                      <OperatingHoursCell>{hours.closed ? '-' : hours.close}</OperatingHoursCell>
                      <OperatingHoursCell>
                        <StatusBadge status={hours.closed ? 'closed' : 'open'}>
                          {hours.closed ? 'Closed' : 'Open'}
                        </StatusBadge>
                      </OperatingHoursCell>
                    </OperatingHoursRow>
                  ))}
                </tbody>
              </OperatingHoursTable>
              <Button 
                style={{ marginTop: '20px' }} 
                onClick={handleEditHours}
              >
                <IconEdit /> Edit Hours
              </Button>
            </>
          ) : (
            <>
              <OperatingHoursTable>
                <thead>
                  <tr>
                    <OperatingHoursHead>Day</OperatingHoursHead>
                    <OperatingHoursHead>Open</OperatingHoursHead>
                    <OperatingHoursHead>Close</OperatingHoursHead>
                    <OperatingHoursHead>Closed</OperatingHoursHead>
                  </tr>
                </thead>
                <tbody>
                  {tempOperatingHours.map((hours, index) => (
                    <OperatingHoursRow key={hours.day} index={index}>
                      <OperatingHoursCell>{hours.day}</OperatingHoursCell>
                      <OperatingHoursCell>
                        <TimeInput 
                          type="time" 
                          value={hours.open}
                          onChange={(e) => handleHoursChange(index, 'open', e.target.value)}
                          disabled={hours.closed}
                        />
                      </OperatingHoursCell>
                      <OperatingHoursCell>
                        <TimeInput 
                          type="time" 
                          value={hours.close}
                          onChange={(e) => handleHoursChange(index, 'close', e.target.value)}
                          disabled={hours.closed}
                        />
                      </OperatingHoursCell>
                      <OperatingHoursCell>
                        <input 
                          type="checkbox" 
                          checked={hours.closed}
                          onChange={() => handleHoursChange(index, 'closed')}
                        />
                      </OperatingHoursCell>
                    </OperatingHoursRow>
                  ))}
                </tbody>
              </OperatingHoursTable>
              <ButtonGroup style={{ marginTop: '20px' }}>
                <Button secondary onClick={handleCancelHours}>Cancel</Button>
                <Button success onClick={handleSaveHours}>
                  <IconSave /> Save Changes
                </Button>
              </ButtonGroup>
            </>
          )}
        </OperatingHoursContainer>
      )}
    </Container>
  );
};

export default CustomerSupportMessages; 