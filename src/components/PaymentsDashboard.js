import React, { useState, useEffect } from 'react';
import { collection, getDocs, doc, updateDoc, orderBy, query, onSnapshot, setDoc, getDoc, where } from 'firebase/firestore';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { db, storage } from '../firebase';
import { useAuth } from '../context/AuthContext';
import styled from 'styled-components';
import { FiSearch, FiFilter, FiDownload, FiUpload, FiCheck, FiX, FiEye, FiSettings, FiDollarSign, FiClock, FiCheckCircle, FiAlertCircle } from 'react-icons/fi';

// Pine green theme to match BikesList
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  accent: '#FF8C00',
  success: '#4CAF50',
  danger: '#d32f2f',
  warning: '#FFC107',
  lightRed: '#ffebee',
  lightGreen: '#e8f5e9',
  lightBlue: '#e3f2fd'
};

const Container = styled.div`
  padding: 20px;
  background-color: ${colors.lightGray};
  min-height: 100vh;
`;

const Header = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
`;

const Title = styled.h1`
  color: ${colors.darkGray};
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 12px;
`;

const HeaderActions = styled.div`
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
`;

const HeaderButton = styled.button`
  padding: 12px 20px;
  background-color: ${props => props.secondary ? colors.lightGray : colors.pineGreen};
  color: ${props => props.secondary ? colors.darkGray : colors.white};
  border: ${props => props.secondary ? `2px solid ${colors.mediumGray}` : 'none'};
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
  white-space: nowrap;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(29, 60, 52, 0.15);
    background-color: ${props => props.secondary ? colors.white : colors.lightPineGreen};
  }
`;

// Stats Section
const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
`;

const StatCard = styled.div`
  background: linear-gradient(135deg, ${colors.white} 0%, #fafafa 100%);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(29, 60, 52, 0.08);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 25px rgba(29, 60, 52, 0.15);
  }
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: ${props => {
      switch (props.type) {
        case 'total': return `linear-gradient(90deg, ${colors.pineGreen}, ${colors.lightPineGreen})`;
        case 'pending': return `linear-gradient(90deg, ${colors.warning}, #ffcc02)`;
        case 'confirmed': return `linear-gradient(90deg, ${colors.success}, #45a049)`;
        case 'rejected': return `linear-gradient(90deg, ${colors.danger}, #c62828)`;
        default: return `linear-gradient(90deg, ${colors.pineGreen}, ${colors.lightPineGreen})`;
      }
    }};
  }
`;

const StatHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
`;

const StatIcon = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${props => {
    switch (props.type) {
      case 'total': return 'rgba(29, 60, 52, 0.1)';
      case 'pending': return 'rgba(255, 193, 7, 0.1)';
      case 'confirmed': return 'rgba(76, 175, 80, 0.1)';
      case 'rejected': return 'rgba(211, 47, 47, 0.1)';
      default: return 'rgba(29, 60, 52, 0.1)';
    }
  }};
  color: ${props => {
    switch (props.type) {
      case 'total': return colors.pineGreen;
      case 'pending': return colors.warning;
      case 'confirmed': return colors.success;
      case 'rejected': return colors.danger;
      default: return colors.pineGreen;
    }
  }};
`;

const StatValue = styled.div`
  font-size: 24px;
  font-weight: 700;
  color: ${colors.darkGray};
  margin-bottom: 4px;
`;

const StatLabel = styled.div`
  font-size: 14px;
  color: ${colors.mediumGray};
  font-weight: 500;
`;

// Filters Section
const FiltersCard = styled.div`
  background: ${colors.white};
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(29, 60, 52, 0.08);
`;

const FiltersHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-weight: 600;
  color: ${colors.darkGray};
`;

const FiltersRow = styled.div`
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
`;

const FilterGroup = styled.div`
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 180px;
`;

const FilterLabel = styled.label`
  font-size: 12px;
  font-weight: 600;
  color: ${colors.mediumGray};
  text-transform: uppercase;
  letter-spacing: 0.5px;
`;

const FilterSelect = styled.select`
  padding: 10px 12px;
  border: 2px solid rgba(29, 60, 52, 0.1);
  border-radius: 8px;
  background-color: ${colors.white};
  color: ${colors.darkGray};
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 3px rgba(29, 60, 52, 0.1);
  }
`;

const SearchInput = styled.input`
  padding: 10px 12px;
  border: 2px solid rgba(29, 60, 52, 0.1);
  border-radius: 8px;
  background-color: ${colors.white};
  color: ${colors.darkGray};
  font-weight: 500;
  min-width: 250px;
  transition: all 0.2s ease;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 3px rgba(29, 60, 52, 0.1);
  }
  
  &::placeholder {
    color: ${colors.mediumGray};
  }
`;

// Payment Settings Section
const PaymentSettingsCard = styled.div`
  background: ${colors.white};
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(29, 60, 52, 0.08);
`;

const SettingsHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
`;

const SettingsTitle = styled.h2`
  color: ${colors.darkGray};
  margin: 0;
  font-size: 20px;
  font-weight: 600;
`;

const SettingsGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

const SettingGroup = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

const SettingLabel = styled.label`
  font-size: 14px;
  font-weight: 600;
  color: ${colors.darkGray};
  margin-bottom: 6px;
`;

const SettingInput = styled.input`
  padding: 12px 16px;
  border: 2px solid rgba(29, 60, 52, 0.1);
  border-radius: 8px;
  background-color: ${colors.white};
  color: ${colors.darkGray};
  font-weight: 500;
  transition: all 0.2s ease;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 3px rgba(29, 60, 52, 0.1);
  }
  
  &::placeholder {
    color: ${colors.mediumGray};
  }
`;

const FileUploadArea = styled.div`
  border: 2px dashed rgba(29, 60, 52, 0.2);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, ${colors.white} 0%, #fafafa 100%);
  
  &:hover {
    border-color: ${colors.pineGreen};
    background: ${colors.lightGray};
    transform: translateY(-2px);
  }
`;

const FileInput = styled.input`
  display: none;
`;

const QRPreview = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
`;

const QRImage = styled.img`
  max-width: 120px;
  border-radius: 8px;
  border: 2px solid rgba(29, 60, 52, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
  
  &:hover {
    transform: scale(1.05);
    border-color: ${colors.pineGreen};
    box-shadow: 0 4px 12px rgba(29, 60, 52, 0.15);
  }
`;

const SaveButton = styled.button`
  background: linear-gradient(135deg, ${colors.pineGreen} 0%, ${colors.lightPineGreen} 100%);
  color: ${colors.white};
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 16px;
  width: 100%;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(29, 60, 52, 0.25);
  }
  
  &:disabled {
    background: ${colors.mediumGray};
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }
`;

// Payments List Section
const PaymentsCard = styled.div`
  background: ${colors.white};
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(29, 60, 52, 0.08);
  overflow: hidden;
`;

const PaymentsHeader = styled.div`
  padding: 20px;
  border-bottom: 1px solid rgba(29, 60, 52, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

const PaymentsTitle = styled.h3`
  margin: 0;
  color: ${colors.darkGray};
  font-size: 18px;
  font-weight: 600;
`;

const PaymentsGrid = styled.div`
  display: grid;
  gap: 1px;
  background: rgba(29, 60, 52, 0.05);
`;

const PaymentCard = styled.div`
  background: ${colors.white};
  padding: 16px;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  border-radius: 12px;
  margin-bottom: 12px;
  border: 2px solid transparent;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  
  &:hover {
    background: ${colors.lightGray};
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(29, 60, 52, 0.15);
    border-color: ${colors.pineGreen};
  }
  
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    border-radius: 12px 0 0 12px;
    background: ${props => {
      switch (props.status) {
        case 'PENDING': return colors.warning;
        case 'CONFIRMED': return colors.success;
        case 'REJECTED': return colors.danger;
        default: return colors.mediumGray;
      }
    }};
  }
  
  ${props => props.expanded && `
    background: ${colors.lightGray};
    border-color: ${colors.pineGreen};
    box-shadow: 0 8px 25px rgba(29, 60, 52, 0.15);
  `}
`;

const PaymentCardHeader = styled.div`
  display: grid;
  grid-template-columns: auto 1fr auto auto auto;
  align-items: center;
  gap: 16px;
  margin-bottom: ${props => props.expanded ? '16px' : '0'};
  padding-bottom: ${props => props.expanded ? '12px' : '0'};
  border-bottom: ${props => props.expanded ? `2px solid rgba(29, 60, 52, 0.1)` : 'none'};
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr auto;
    gap: 12px;
  }
`;

const PaymentUser = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 180px;
`;

const UserName = styled.div`
  font-weight: 700;
  color: ${colors.darkGray};
  font-size: 16px;
  line-height: 1.2;
`;

const UserEmail = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  font-weight: 500;
  line-height: 1.2;
`;

const PaymentInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: center;
  min-width: 100px;
`;

const PaymentAmount = styled.div`
  font-size: 18px;
  font-weight: 700;
  color: ${colors.pineGreen};
  line-height: 1.2;
`;

const PaymentDate = styled.div`
  font-size: 11px;
  color: ${colors.mediumGray};
  font-weight: 500;
`;

const PaymentBike = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: center;
  min-width: 120px;
`;

const BikeType = styled.div`
  font-size: 14px;
  font-weight: 600;
  color: ${colors.darkGray};
  line-height: 1.2;
`;

const BikeDuration = styled.div`
  font-size: 11px;
  color: ${colors.mediumGray};
  font-weight: 500;
`;

const PaymentScreenshot = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 50px;
`;

const ScreenshotThumbnail = styled.img`
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid rgba(29, 60, 52, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: scale(1.1);
    border-color: ${colors.pineGreen};
    box-shadow: 0 4px 12px rgba(29, 60, 52, 0.15);
  }
`;

const NoScreenshot = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: 2px dashed rgba(29, 60, 52, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: ${colors.mediumGray};
  background: rgba(29, 60, 52, 0.05);
`;

const StatusAndActions = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  min-width: 120px;
`;

const ExpandIndicator = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  color: ${colors.mediumGray};
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
  cursor: pointer;
  
  &::after {
    content: '${props => props.expanded ? '▼' : '▶'}';
    font-size: 8px;
    transition: all 0.3s ease;
  }
  
  &:hover {
    color: ${colors.pineGreen};
  }
`;

const PaymentDetails = styled.div`
  max-height: ${props => props.expanded ? '1000px' : '0'};
  opacity: ${props => props.expanded ? '1' : '0'};
  overflow: hidden;
  transition: all 0.4s ease;
  margin-top: ${props => props.expanded ? '16px' : '0'};
`;

const DetailsGrid = styled.div`
  display: grid;
  grid-template-columns: 2fr 3fr;
  gap: 20px;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 16px;
  }
`;

const UserInfoSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(29, 60, 52, 0.05) 0%, rgba(29, 60, 52, 0.02) 100%);
  border-radius: 10px;
  border: 1px solid rgba(29, 60, 52, 0.1);
`;

const PaymentDetailsSection = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
`;

const InfoItem = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
  background: rgba(29, 60, 52, 0.02);
  border-radius: 6px;
  border: 1px solid rgba(29, 60, 52, 0.08);
  transition: all 0.2s ease;
  
  &:hover {
    background: rgba(29, 60, 52, 0.05);
    border-color: rgba(29, 60, 52, 0.15);
  }
`;

const InfoLabel = styled.span`
  font-size: 10px;
  color: ${colors.mediumGray};
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 2px;
`;

const InfoValue = styled.span`
  font-size: 13px;
  color: ${colors.darkGray};
  font-weight: 600;
  line-height: 1.3;
  word-break: break-word;
`;

const FullScreenshotSection = styled.div`
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  padding: 12px;
  background: rgba(29, 60, 52, 0.02);
  border-radius: 8px;
  border: 1px solid rgba(29, 60, 52, 0.08);
`;

const FullScreenshot = styled.img`
  max-width: 200px;
  max-height: 200px;
  object-fit: contain;
  border-radius: 8px;
  border: 2px solid rgba(29, 60, 52, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: scale(1.05);
    border-color: ${colors.pineGreen};
    box-shadow: 0 4px 12px rgba(29, 60, 52, 0.15);
  }
`;

const PaymentActions = styled.div`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
`;

const ActionButton = styled.button`
  padding: 8px 16px;
  border: 2px solid;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  
  ${props => {
    if (props.variant === 'confirm') {
      return `
        background-color: ${colors.success};
        color: ${colors.white};
        border-color: ${colors.success};
        
        &:hover {
          background-color: transparent;
          color: ${colors.success};
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(76, 175, 80, 0.25);
        }
      `;
    } else if (props.variant === 'reject') {
      return `
        background-color: ${colors.danger};
        color: ${colors.white};
        border-color: ${colors.danger};
        
        &:hover {
          background-color: transparent;
          color: ${colors.danger};
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(211, 47, 47, 0.25);
        }
      `;
    } else {
      return `
        background-color: ${colors.pineGreen};
        color: ${colors.white};
        border-color: ${colors.pineGreen};
        
        &:hover {
          background-color: transparent;
          color: ${colors.pineGreen};
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(29, 60, 52, 0.25);
        }
      `;
    }
  }}
  
  &:disabled {
    background-color: ${colors.mediumGray};
    color: ${colors.white};
    border-color: ${colors.mediumGray};
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
    
    &:hover {
      background-color: ${colors.mediumGray};
      color: ${colors.white};
      transform: none;
      box-shadow: none;
    }
  }
`;

const PaymentStatus = styled.span`
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: ${props => {
    switch (props.status) {
      case 'PENDING': return 'rgba(255, 193, 7, 0.1)';
      case 'CONFIRMED': return 'rgba(76, 175, 80, 0.1)';
      case 'REJECTED': return 'rgba(211, 47, 47, 0.1)';
      default: return 'rgba(102, 102, 102, 0.1)';
    }
  }};
  color: ${props => {
    switch (props.status) {
      case 'PENDING': return colors.warning;
      case 'CONFIRMED': return colors.success;
      case 'REJECTED': return colors.danger;
      default: return colors.mediumGray;
    }
  }};
  border: 1px solid ${props => {
    switch (props.status) {
      case 'PENDING': return 'rgba(255, 193, 7, 0.3)';
      case 'CONFIRMED': return 'rgba(76, 175, 80, 0.3)';
      case 'REJECTED': return 'rgba(211, 47, 47, 0.3)';
      default: return 'rgba(102, 102, 102, 0.3)';
    }
  }};
`;

const StatusBadge = styled.span`
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  transition: all 0.2s ease;
  background: ${props => {
    switch (props.status) {
      case 'PENDING': return 'rgba(255, 193, 7, 0.1)';
      case 'CONFIRMED': return 'rgba(76, 175, 80, 0.1)';
      case 'REJECTED': return 'rgba(211, 47, 47, 0.1)';
      default: return 'rgba(102, 102, 102, 0.1)';
    }
  }};
  color: ${props => {
    switch (props.status) {
      case 'PENDING': return colors.warning;
      case 'CONFIRMED': return colors.success;
      case 'REJECTED': return colors.danger;
      default: return colors.mediumGray;
    }
  }};
  border: 1px solid ${props => {
    switch (props.status) {
      case 'PENDING': return 'rgba(255, 193, 7, 0.3)';
      case 'CONFIRMED': return 'rgba(76, 175, 80, 0.3)';
      case 'REJECTED': return 'rgba(211, 47, 47, 0.3)';
      default: return 'rgba(102, 102, 102, 0.3)';
    }
  }};
`;

const Modal = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
`;

const ModalContent = styled.div`
  background: ${colors.white};
  border-radius: 12px;
  max-width: 90vw;
  max-height: 90vh;
  position: relative;
  overflow: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
`;

const CloseButton = styled.button`
  position: absolute;
  top: 16px;
  right: 16px;
  background: ${colors.danger};
  color: ${colors.white};
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all 0.3s ease;
  z-index: 1001;
  
  &:hover {
    transform: scale(1.1);
    box-shadow: 0 4px 12px rgba(211, 47, 47, 0.3);
  }
`;

const ModalImage = styled.img`
  max-width: 600px;
  max-height: 600px;
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 8px;
  padding: 20px;
`;

const LoadingContainer = styled.div`
  text-align: center;
  padding: 60px 20px;
  color: ${colors.mediumGray};
  
  &::before {
    content: '';
    display: inline-block;
    width: 40px;
    height: 40px;
    border: 3px solid rgba(29, 60, 52, 0.1);
    border-top: 3px solid ${colors.pineGreen};
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 16px;
  }
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
  
  div {
    font-size: 16px;
    font-weight: 500;
  }
`;

const SuccessMessage = styled.div`
  background: ${colors.lightGreen};
  color: ${colors.success};
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 12px;
  font-weight: 500;
  border: 1px solid rgba(76, 175, 80, 0.3);
  display: flex;
  align-items: center;
  gap: 8px;
`;

const ErrorMessage = styled.div`
  background: ${colors.lightRed};
  color: ${colors.danger};
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 12px;
  font-weight: 500;
  border: 1px solid rgba(211, 47, 47, 0.3);
  display: flex;
  align-items: center;
  gap: 8px;
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 60px 20px;
  color: ${colors.mediumGray};
  background: ${colors.white};
  border-radius: 12px;
  border: 2px dashed rgba(29, 60, 52, 0.2);
  
  div {
    font-size: 16px;
    font-weight: 500;
    margin-bottom: 8px;
  }
  
  span {
    font-size: 14px;
    color: ${colors.mediumGray};
  }
`;

const PaymentsDashboard = () => {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [filteredPayments, setFilteredPayments] = useState([]);
  const [selectedImage, setSelectedImage] = useState(null);
  const [processingId, setProcessingId] = useState(null);
  const [users, setUsers] = useState({});
  const [loadingUsers, setLoadingUsers] = useState(false);
  
  // New state for expanded payment details
  const [expandedPayment, setExpandedPayment] = useState(null);
  
  // Settings states
  const [businessName, setBusinessName] = useState('');
  const [gcashNumber, setGCashNumber] = useState('');
  const [qrFile, setQrFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [currentQRUrl, setCurrentQRUrl] = useState('');

  const { user, isAuthenticated, loading: authLoading } = useAuth();

  // Function to fetch user data for payments
  const fetchUsersForPayments = async (paymentsData) => {
    try {
      setLoadingUsers(true);
      const userIds = [...new Set(paymentsData.map(payment => payment.userId).filter(Boolean))];
      
      if (userIds.length === 0) {
        setLoadingUsers(false);
        return {};
      }

      const usersData = {};
      
      // Fetch each user's data
      for (const userId of userIds) {
        try {
          const userDoc = await getDoc(doc(db, 'users', userId));
          if (userDoc.exists()) {
            const userData = userDoc.data();
            usersData[userId] = {
              name: userData.fullName || userData.name || userData.displayName || 'Unknown User',
              email: userData.email || 'No email provided',
              phone: userData.phoneNumber || userData.phone || 'No phone provided'
            };
          } else {
            usersData[userId] = {
              name: 'Unknown User',
              email: 'No email provided',
              phone: 'No phone provided'
            };
          }
        } catch (error) {
          console.error(`Error fetching user data for ${userId}:`, error);
          usersData[userId] = {
            name: 'Error loading user',
            email: 'Error loading email',
            phone: 'Error loading phone'
          };
        }
      }
      
      setLoadingUsers(false);
      return usersData;
    } catch (error) {
      console.error('Error fetching users for payments:', error);
      setLoadingUsers(false);
      return {};
    }
  };

  // Load payment settings from Firestore
  useEffect(() => {
    const loadPaymentSettings = async () => {
      try {
        const settingsDoc = await getDoc(doc(db, 'settings', 'payment'));
        if (settingsDoc.exists()) {
          setBusinessName(settingsDoc.data().businessName);
          setGCashNumber(settingsDoc.data().gcashNumber);
          setCurrentQRUrl(settingsDoc.data().qrCodeUrl);
        } else {
          // Create default settings if they don't exist
          const defaultSettings = {
            gcashNumber: '09123456789',
            businessName: 'Bambike Cycles',
            qrCodeUrl: ''
          };
          await setDoc(doc(db, 'settings', 'payment'), defaultSettings);
          setBusinessName(defaultSettings.businessName);
          setGCashNumber(defaultSettings.gcashNumber);
          setCurrentQRUrl(defaultSettings.qrCodeUrl);
        }
      } catch (error) {
        console.error('Error loading payment settings:', error);
      }
    };

    if (isAuthenticated) {
      loadPaymentSettings();
    }
  }, [isAuthenticated]);

  // Load payments from Firestore with real-time updates
  const loadPayments = async () => {
    try {
      setLoading(true);
      
      // First, let's try a simple query without any authentication filters
      const paymentsRef = collection(db, 'payments');
      
      // Try to get all payments (this will be limited by Firebase rules)
      const snapshot = await getDocs(paymentsRef);
      
      if (snapshot.empty) {
        setPayments([]);
        setUsers({});
        setLoading(false);
        return;
      }
      
      const paymentsData = [];
      
      snapshot.forEach((doc) => {
        const data = doc.data();
        
        // Validate required fields
        const requiredFields = ['userId', 'amount', 'status', 'createdAt'];
        const missingFields = requiredFields.filter(field => !data.hasOwnProperty(field));
        
        if (missingFields.length > 0) {
          console.warn(`Document ${doc.id} missing fields:`, missingFields);
        }
        
        paymentsData.push({
          id: doc.id,
          ...data,
          // Ensure amount is a number
          amount: typeof data.amount === 'number' ? data.amount : parseFloat(data.amount) || 0
        });
      });
      
      // Fetch user data for all payments
      const usersData = await fetchUsersForPayments(paymentsData);
      
      setPayments(paymentsData);
      setUsers(usersData);
      setLoading(false);
      
    } catch (error) {
      console.error('Error loading payments:', error);
      setLoading(false);
    }
  };

  // Load payments on component mount
  useEffect(() => {
    if (isAuthenticated) {
      loadPayments();
    }
  }, [isAuthenticated]);

  // Filter payments based on status and search term
  useEffect(() => {
    let filtered = payments;

    // Filter by status
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(payment => payment.status === statusFilter);
    }

    // Filter by search term - now includes user name and email
    if (searchTerm) {
      filtered = filtered.filter(payment => {
        const userData = users[payment.userId];
        const searchLower = searchTerm.toLowerCase();
        
        return (
          payment.mobileNumber?.toLowerCase().includes(searchLower) ||
          payment.referenceNumber?.toLowerCase().includes(searchLower) ||
          payment.bikeType?.toLowerCase().includes(searchLower) ||
          userData?.name?.toLowerCase().includes(searchLower) ||
          userData?.email?.toLowerCase().includes(searchLower) ||
          userData?.phone?.toLowerCase().includes(searchLower)
        );
      });
    }

    setFilteredPayments(filtered);
  }, [payments, statusFilter, searchTerm, users]);

  // Calculate statistics
  const stats = {
    total: payments.length,
    pending: payments.filter(p => p.status === 'PENDING').length,
    confirmed: payments.filter(p => p.status === 'CONFIRMED').length,
    rejected: payments.filter(p => p.status === 'REJECTED').length
  };

  const handlePaymentAction = async (paymentId, newStatus) => {
    setProcessingId(paymentId);
    try {
      const paymentRef = doc(db, 'payments', paymentId);
      await updateDoc(paymentRef, {
        status: newStatus,
        processedAt: new Date(),
        processedBy: user?.email || 'admin' // Use actual authenticated user email
      });
      
      // Reload payments to reflect the update
      loadPayments();
    } catch (error) {
      console.error('Error updating payment:', error);
      alert('Failed to update payment status. Please try again.');
    }
    setProcessingId(null);
  };

  // Handle payment settings update
  const handleSettingsUpdate = async () => {
    // Validate input fields
    if (!gcashNumber || !businessName) {
      setError({ 
        type: 'error', 
        text: 'Please fill in both GCash number and Business name before updating.' 
      });
      return;
    }

    // Validate GCash number format (basic validation)
    if (!/^09\d{9}$/.test(gcashNumber)) {
      setError({ 
        type: 'error', 
        text: 'Please enter a valid GCash number (format: 09XXXXXXXXX).' 
      });
      return;
    }

    setIsUploading(true);
    setError({ type: '', text: '' });

    try {
      let updatedSettings = { 
        gcashNumber: gcashNumber.trim(),
        businessName: businessName.trim(),
        qrCodeUrl: currentQRUrl || ''
      };

      // Upload new QR code if provided
      if (qrFile) {
        const qrRef = ref(storage, `payment-settings/qr-code-${Date.now()}.jpg`);
        const uploadResult = await uploadBytes(qrRef, qrFile);
        const downloadURL = await getDownloadURL(qrRef);
        updatedSettings.qrCodeUrl = downloadURL;
      }

      // Update settings in Firestore
      await setDoc(doc(db, 'settings', 'payment'), updatedSettings, { merge: true });
      
      setBusinessName(updatedSettings.businessName);
      setGCashNumber(updatedSettings.gcashNumber);
      setCurrentQRUrl(updatedSettings.qrCodeUrl);
      setQrFile(null);
      
      // Reset file input
      const fileInput = document.getElementById('qr-upload');
      if (fileInput) {
        fileInput.value = '';
      }
      
      setError({ type: 'success', text: 'Payment settings updated successfully!' });
      
      // Clear message after 5 seconds
      setTimeout(() => setError({ type: '', text: '' }), 5000);
    } catch (error) {
      console.error('Error updating payment settings:', error);
      let errorMessage = 'Failed to update payment settings. ';
      
      if (error.code === 'permission-denied') {
        errorMessage += 'Permission denied. Please check your access rights.';
      } else if (error.code === 'unavailable') {
        errorMessage += 'Service temporarily unavailable. Please try again later.';
      } else {
        errorMessage += `Error: ${error.message}`;
      }
      
      setError({ type: 'error', text: errorMessage });
    }
    
    setIsUploading(false);
  };

  const handleQRFileChange = (event) => {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
      if (!validTypes.includes(file.type)) {
        setError({ 
          type: 'error', 
          text: 'Please select a valid image file (JPEG, PNG, or GIF).' 
        });
        event.target.value = ''; // Clear the input
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        setError({ 
          type: 'error', 
          text: 'File size must be less than 5MB.' 
        });
        event.target.value = ''; // Clear the input
        return;
      }

      setQrFile(file);
      setError({ type: '', text: '' }); // Clear any previous errors
    }
  };

  const handleGCashNumberChange = (e) => {
    const value = e.target.value;
    // Only allow numbers and ensure it starts with 09
    if (value === '' || /^09\d{0,9}$/.test(value)) {
      setGCashNumber(value);
    }
  };

  const handleBusinessNameChange = (e) => {
    const value = e.target.value;
    // Limit business name to 50 characters
    if (value.length <= 50) {
      setBusinessName(value);
    }
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    
    // Handle different timestamp formats
    if (timestamp.seconds) {
      // Firestore Timestamp object
      return new Date(timestamp.seconds * 1000).toLocaleString();
    } else if (timestamp instanceof Date) {
      // JavaScript Date object
      return timestamp.toLocaleString();
    } else if (typeof timestamp === 'number') {
      // Unix timestamp
      return new Date(timestamp).toLocaleString();
    } else {
      return 'Invalid Date';
    }
  };

  const formatAmount = (amount) => {
    return `₱${parseFloat(amount).toFixed(2)}`;
  };

  // Show loading while authentication is being checked
  if (authLoading) {
    return (
      <Container>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '50vh' 
        }}>
          <div>Loading authentication...</div>
        </div>
      </Container>
    );
  }

  // Show login prompt if not authenticated
  if (!isAuthenticated) {
    return (
      <Container>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '50vh',
          flexDirection: 'column'
        }}>
          <h2>Please log in to access the Payments Dashboard</h2>
          <p>You need to be authenticated to view payment information.</p>
        </div>
      </Container>
    );
  }

  if (loading) {
    return (
      <LoadingContainer>
        Loading payments...
      </LoadingContainer>
    );
  }

  return (
    <Container>
      <Header>
        <Title>
          <FiDollarSign size={20} />
          Payments Dashboard
        </Title>
        <HeaderActions>
          <HeaderButton>
            <FiDownload size={20} />
            Export Payments
          </HeaderButton>
          <HeaderButton>
            <FiUpload size={20} />
            Import Payments
          </HeaderButton>
        </HeaderActions>
      </Header>

      {/* Payment Settings Section */}
      <PaymentSettingsCard>
        <SettingsHeader>
          <FiSettings size={20} />
          <SettingsTitle>Payment Settings Management</SettingsTitle>
        </SettingsHeader>
        <SettingsGrid>
          <SettingGroup>
            <SettingLabel>GCash Mobile Number</SettingLabel>
            <SettingInput
              type="text"
              value={gcashNumber}
              onChange={handleGCashNumberChange}
              placeholder="09123456789"
            />
          </SettingGroup>
          
          <SettingGroup>
            <SettingLabel>Business Name</SettingLabel>
            <SettingInput
              type="text"
              value={businessName}
              onChange={handleBusinessNameChange}
              placeholder="Bambike Cycles"
            />
          </SettingGroup>
          
          <SettingGroup style={{ gridColumn: '1 / -1' }}>
            <SettingLabel>GCash QR Code</SettingLabel>
            <FileUploadArea onClick={() => document.getElementById('qr-upload').click()}>
              <FiUpload size={24} color={colors.pineGreen} />
              <div style={{ marginTop: '8px', fontWeight: '600', color: colors.darkGray }}>
                {qrFile ? qrFile.name : 'Click to upload QR code image'}
              </div>
              <div style={{ fontSize: '12px', color: colors.mediumGray, marginTop: '4px' }}>
                Supports JPEG, PNG, GIF (Max 5MB)
              </div>
            </FileUploadArea>
            <FileInput
              id="qr-upload"
              type="file"
              accept="image/*"
              onChange={handleQRFileChange}
            />
            
            {currentQRUrl && (
              <QRPreview>
                <div style={{ fontSize: '14px', fontWeight: '600', color: colors.darkGray }}>
                  Current QR Code:
                </div>
                <QRImage
                  src={currentQRUrl}
                  alt="Current QR Code"
                  onClick={() => setSelectedImage(currentQRUrl)}
                />
              </QRPreview>
            )}
          </SettingGroup>
          
          <SaveButton
            onClick={handleSettingsUpdate}
            disabled={isUploading}
            style={{ gridColumn: '1 / -1' }}
          >
            {isUploading ? (
              <>
                <FiUpload size={16} />
                Updating Settings...
              </>
            ) : (
              <>
                <FiCheck size={16} />
                Update Settings
              </>
            )}
          </SaveButton>
          
          {error && error.text && (
            <div style={{ gridColumn: '1 / -1' }}>
              {error.type === 'success' ? (
                <SuccessMessage>
                  <FiCheckCircle size={16} />
                  {error.text}
                </SuccessMessage>
              ) : (
                <ErrorMessage>
                  <FiAlertCircle size={16} />
                  {error.text}
                </ErrorMessage>
              )}
            </div>
          )}
        </SettingsGrid>
      </PaymentSettingsCard>

      {/* Statistics Cards */}
      <StatsGrid>
        <StatCard type="total">
          <StatHeader>
            <StatIcon type="total">
              <FiDollarSign size={20} />
            </StatIcon>
            <StatValue>{stats.total}</StatValue>
          </StatHeader>
          <StatLabel>Total Payments</StatLabel>
        </StatCard>
        <StatCard type="pending">
          <StatHeader>
            <StatIcon type="pending">
              <FiClock size={20} />
            </StatIcon>
            <StatValue>{stats.pending}</StatValue>
          </StatHeader>
          <StatLabel>Pending Review</StatLabel>
        </StatCard>
        <StatCard type="confirmed">
          <StatHeader>
            <StatIcon type="confirmed">
              <FiCheckCircle size={20} />
            </StatIcon>
            <StatValue>{stats.confirmed}</StatValue>
          </StatHeader>
          <StatLabel>Confirmed</StatLabel>
        </StatCard>
        <StatCard type="rejected">
          <StatHeader>
            <StatIcon type="rejected">
              <FiX size={20} />
            </StatIcon>
            <StatValue>{stats.rejected}</StatValue>
          </StatHeader>
          <StatLabel>Rejected</StatLabel>
        </StatCard>
      </StatsGrid>

      {/* Filters */}
      <FiltersCard>
        <FiltersHeader>
          <FiFilter size={20} />
          Filters
        </FiltersHeader>
        <FiltersRow>
          <FilterGroup>
            <FilterLabel>Status</FilterLabel>
            <FilterSelect
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All Payments</option>
              <option value="PENDING">Pending</option>
              <option value="CONFIRMED">Confirmed</option>
              <option value="REJECTED">Rejected</option>
            </FilterSelect>
          </FilterGroup>
          <FilterGroup>
            <FilterLabel>Search</FilterLabel>
            <SearchInput
              type="text"
              placeholder="Search by user name, email, mobile, reference, or bike type..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </FilterGroup>
        </FiltersRow>
      </FiltersCard>

      {/* Payments List */}
      {filteredPayments.length === 0 ? (
        <EmptyState>
          <div>
            {loading ? '⏳ Loading payments...' : '📄 No payments found'}
          </div>
          <span>
            {loading ? 'Please wait while we fetch the payment data.' : 'Try adjusting your filters or search criteria.'}
          </span>
        </EmptyState>
      ) : (
        <PaymentsCard>
          <PaymentsHeader>
            <PaymentsTitle>Payment History ({filteredPayments.length})</PaymentsTitle>
          </PaymentsHeader>
          
          <PaymentsGrid>
            {filteredPayments.map(payment => (
              <PaymentCard
                key={payment.id}
                status={payment.status}
                expanded={expandedPayment === payment.id}
                onClick={() => setExpandedPayment(expandedPayment === payment.id ? null : payment.id)}
              >
                <PaymentCardHeader expanded={expandedPayment === payment.id}>
                  <PaymentUser>
                    <UserName>
                      {loadingUsers ? '⏳ Loading...' : (users[payment.userId]?.name || '👤 Unknown User')}
                    </UserName>
                    <UserEmail>
                      {loadingUsers ? 'Loading email...' : (users[payment.userId]?.email || 'No email provided')}
                    </UserEmail>
                  </PaymentUser>
                  
                  <PaymentInfo>
                    <PaymentAmount>
                      {payment.amount ? formatAmount(payment.amount) : 'N/A'}
                    </PaymentAmount>
                    <PaymentDate>
                      {formatDate(payment.createdAt)}
                    </PaymentDate>
                  </PaymentInfo>
                  
                  <PaymentBike>
                    <BikeType>{payment.bikeType || 'N/A'}</BikeType>
                    <BikeDuration>{payment.duration || 'N/A'}</BikeDuration>
                  </PaymentBike>
                  
                  <PaymentScreenshot>
                    {payment.screenshotUrl ? (
                      <ScreenshotThumbnail
                        src={payment.screenshotUrl}
                        alt="Payment screenshot"
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedImage(payment.screenshotUrl);
                        }}
                      />
                    ) : (
                      <NoScreenshot>📷</NoScreenshot>
                    )}
                  </PaymentScreenshot>
                  
                  <StatusAndActions>
                    <PaymentStatus status={payment.status}>
                      {payment.status === 'PENDING' ? '⏳' : payment.status === 'CONFIRMED' ? '✅' : '❌'}
                      {payment.status}
                    </PaymentStatus>
                    <ExpandIndicator expanded={expandedPayment === payment.id}>
                      Details
                    </ExpandIndicator>
                  </StatusAndActions>
                </PaymentCardHeader>
                
                <PaymentDetails expanded={expandedPayment === payment.id}>
                  <DetailsGrid>
                    <UserInfoSection>
                      <InfoItem>
                        <InfoLabel>👤 Customer Name</InfoLabel>
                        <InfoValue>{loadingUsers ? 'Loading...' : (users[payment.userId]?.name || 'Unknown User')}</InfoValue>
                      </InfoItem>
                      
                      <InfoItem>
                        <InfoLabel>✉️ Email Address</InfoLabel>
                        <InfoValue>{loadingUsers ? 'Loading...' : (users[payment.userId]?.email || 'No email provided')}</InfoValue>
                      </InfoItem>
                      
                      <InfoItem>
                        <InfoLabel>📱 Contact Number</InfoLabel>
                        <InfoValue>{loadingUsers ? 'Loading...' : (users[payment.userId]?.phone || payment.mobileNumber || 'No phone provided')}</InfoValue>
                      </InfoItem>
                    </UserInfoSection>
                    
                    <PaymentDetailsSection>
                      <InfoItem>
                        <InfoLabel>🆔 Payment ID</InfoLabel>
                        <InfoValue>{payment.id}</InfoValue>
                      </InfoItem>
                      
                      <InfoItem>
                        <InfoLabel>🔢 Reference Number</InfoLabel>
                        <InfoValue>{payment.referenceNumber || 'N/A'}</InfoValue>
                      </InfoItem>
                      
                      <InfoItem>
                        <InfoLabel>📱 Mobile Number</InfoLabel>
                        <InfoValue>{payment.mobileNumber || 'N/A'}</InfoValue>
                      </InfoItem>
                      
                      <InfoItem>
                        <InfoLabel>💰 Amount</InfoLabel>
                        <InfoValue>{payment.amount ? formatAmount(payment.amount) : 'N/A'}</InfoValue>
                      </InfoItem>
                      
                      <InfoItem>
                        <InfoLabel>📅 Created Date</InfoLabel>
                        <InfoValue>{formatDate(payment.createdAt)}</InfoValue>
                      </InfoItem>
                      
                      <InfoItem>
                        <InfoLabel>🏷️ Status</InfoLabel>
                        <InfoValue>
                          <StatusBadge status={payment.status}>{payment.status}</StatusBadge>
                        </InfoValue>
                      </InfoItem>
                      
                      {payment.screenshotUrl && (
                        <FullScreenshotSection>
                          <InfoLabel>📸 Payment Screenshot</InfoLabel>
                          <FullScreenshot
                            src={payment.screenshotUrl}
                            alt="Payment screenshot"
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedImage(payment.screenshotUrl);
                            }}
                          />
                        </FullScreenshotSection>
                      )}
                    </PaymentDetailsSection>
                  </DetailsGrid>
                  
                  {payment.status === 'PENDING' && (
                    <PaymentActions style={{ marginTop: '16px', justifyContent: 'center' }}>
                      <ActionButton
                        variant="confirm"
                        onClick={(e) => {
                          e.stopPropagation();
                          handlePaymentAction(payment.id, 'CONFIRMED');
                        }}
                        disabled={processingId === payment.id}
                      >
                        <FiCheck size={14} />
                        {processingId === payment.id ? 'Processing...' : 'Confirm Payment'}
                      </ActionButton>
                      <ActionButton
                        variant="reject"
                        onClick={(e) => {
                          e.stopPropagation();
                          handlePaymentAction(payment.id, 'REJECTED');
                        }}
                        disabled={processingId === payment.id}
                      >
                        <FiX size={14} />
                        {processingId === payment.id ? 'Processing...' : 'Reject Payment'}
                      </ActionButton>
                    </PaymentActions>
                  )}
                </PaymentDetails>
              </PaymentCard>
            ))}
          </PaymentsGrid>
        </PaymentsCard>
      )}

      {/* Image Modal */}
      {selectedImage && (
        <Modal onClick={() => setSelectedImage(null)}>
          <ModalContent onClick={(e) => e.stopPropagation()}>
            <CloseButton onClick={() => setSelectedImage(null)}>×</CloseButton>
            <ModalImage
              src={selectedImage}
              alt="Payment screenshot"
            />
          </ModalContent>
        </Modal>
      )}
    </Container>
  );
};

export default PaymentsDashboard; 