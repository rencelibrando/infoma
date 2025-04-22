// src/components/BookingManagement.js
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { 
  getAllBookings, 
  updateBooking, 
  deleteBooking,
  getBookingsByBike,
  getBookingsByDateRange,
  calculateBookingDuration,
  getRevenueByPeriod
} from '../services/bookingService';
import { getBikes } from '../services/bikeService';
import { format } from 'date-fns';

// Pine green and gray theme colors consistent with app
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  red: '#d32f2f',
  amber: '#ffc107',
  green: '#4caf50',
  blue: '#2196f3',
  lightBlue: '#e3f2fd',
  lightGreen: '#e8f5e9',
  lightRed: '#ffebee',
  lightAmber: '#fff8e1'
};

const Container = styled.div`
  width: 100%;
`;

const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  flex-wrap: wrap;
  gap: 15px;
`;

const PageTitle = styled.h2`
  font-size: 28px;
  color: ${colors.darkGray};
  margin: 0;
`;

const RevenueSummaryContainer = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
`;

const RevenueCard = styled.div`
  background-color: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
  border-top: 5px solid ${props => props.period === 'day' 
    ? colors.blue 
    : props.period === 'week' 
    ? colors.pineGreen 
    : colors.green};
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  }
`;

const RevenueAmount = styled.div`
  font-size: 28px;
  font-weight: bold;
  color: ${colors.pineGreen};
  margin-bottom: 8px;
`;

const RevenuePeriod = styled.div`
  font-size: 16px;
  color: ${colors.mediumGray};
  text-transform: capitalize;
  font-weight: 500;
  margin-bottom: 10px;
`;

const BookingCount = styled.div`
  font-size: 15px;
  color: ${colors.mediumGray};
  background-color: ${colors.lightGray};
  padding: 6px 15px;
  border-radius: 20px;
  display: inline-block;
`;

const FilterPanel = styled.div`
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin-bottom: 30px;
`;

const FilterPanelTitle = styled.h3`
  font-size: 18px;
  color: ${colors.darkGray};
  margin: 0 0 20px 0;
  display: flex;
  align-items: center;
  
  svg {
    margin-right: 8px;
  }
`;

const FilterContainer = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 15px;
`;

const FilterGroup = styled.div`
  display: flex;
  flex-direction: column;
`;

const FilterLabel = styled.label`
  font-weight: 500;
  margin-bottom: 8px;
  color: ${colors.darkGray};
  font-size: 14px;
`;

const FilterSelect = styled.select`
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #ddd;
  background-color: white;
  width: 100%;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
  
  &:focus {
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 3px rgba(29, 60, 52, 0.1);
    outline: none;
  }
`;

const FilterInput = styled.input`
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #ddd;
  width: 100%;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
  
  &:focus {
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 3px rgba(29, 60, 52, 0.1);
    outline: none;
  }
`;

const DateFilterContainer = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
`;

const FilterActions = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  flex-wrap: wrap;
  gap: 15px;
`;

const ClearFiltersButton = styled.button`
  padding: 8px 16px;
  background-color: ${colors.lightGray};
  color: ${colors.mediumGray};
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
  
  &:hover {
    background-color: #e0e0e0;
  }
`;

const ActiveFiltersContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
`;

const FilterTag = styled.div`
  display: flex;
  align-items: center;
  background-color: ${colors.lightPineGreen};
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  
  span {
    margin-left: 8px;
    cursor: pointer;
    font-weight: bold;
  }
`;

const SortContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
`;

const SortLabel = styled.label`
  margin-right: 8px;
  color: ${colors.darkGray};
  font-weight: 500;
  font-size: 14px;
`;

const BookingsTable = styled.div`
  width: 100%;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
`;

const TableHeader = styled.div`
  display: grid;
  grid-template-columns: 1.5fr 1fr 1.5fr 1fr 1fr 1fr 1fr 1.5fr;
  background-color: ${colors.pineGreen};
  color: white;
  font-weight: 600;
  padding: 18px 15px;
  
  @media (max-width: 768px) {
    display: none;
  }
`;

const TableRow = styled.div`
  display: grid;
  grid-template-columns: 1.5fr 1fr 1.5fr 1fr 1fr 1fr 1fr 1.5fr;
  padding: 18px 15px;
  border-bottom: 1px solid #eee;
  transition: all 0.2s;
  align-items: center;
  
  &:hover {
    background-color: #f9f9f9;
    transform: translateY(-2px);
    box-shadow: 0 2px 5px rgba(0,0,0,0.05);
  }
  
  @media (max-width: 768px) {
    display: flex;
    flex-direction: column;
    padding: 20px 15px;
    position: relative;
    margin-bottom: 15px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    border-left: 5px solid ${props => {
      if (props.status === 'CONFIRMED') return colors.green;
      if (props.status === 'PENDING') return colors.amber;
      if (props.status === 'CANCELLED') return colors.red;
      if (props.status === 'COMPLETED') return colors.blue;
      return colors.pineGreen;
    }};
  }
`;

const TableCell = styled.div`
  display: flex;
  align-items: center;
  
  @media (max-width: 768px) {
    padding: 10px 0;
    border-bottom: 1px solid #f0f0f0;
    
    &:last-child {
      border-bottom: none;
      padding-bottom: 0;
    }
    
    &:first-child {
      padding-top: 0;
    }
    
    &::before {
      content: '${props => props.label}';
      width: 40%;
      font-weight: 600;
      color: ${colors.mediumGray};
    }
  }
`;

const ViewDetailsButton = styled.button`
  background: none;
  border: none;
  color: ${colors.pineGreen};
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
  font-weight: 500;
  transition: color 0.2s;
  
  &:hover {
    color: ${colors.lightPineGreen};
  }
`;

const BikeImage = styled.img`
  width: 45px;
  height: 45px;
  border-radius: 8px;
  margin-right: 10px;
  object-fit: cover;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
`;

const BikeNameHeader = styled.div`
  font-weight: 600;
  font-size: 16px;
  color: ${colors.darkGray};
  margin-bottom: 5px;
`;

const BookingTypeTag = styled.span`
  display: inline-block;
  padding: 3px 8px;
  border-radius: 20px;
  font-size: 11px;
  margin-left: 8px;
  background-color: ${props => props.isHourly ? colors.lightBlue : colors.lightGreen};
  color: ${props => props.isHourly ? colors.blue : colors.green};
  font-weight: 600;
`;

const StatusBadge = styled.span`
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  
  background-color: ${props => {
    if (props.status === 'CONFIRMED') return colors.lightGreen;
    if (props.status === 'PENDING') return colors.lightAmber;
    if (props.status === 'CANCELLED') return colors.lightRed;
    if (props.status === 'COMPLETED') return colors.lightBlue;
    return 'rgba(0, 0, 0, 0.1)';
  }};
  
  color: ${props => {
    if (props.status === 'CONFIRMED') return colors.green;
    if (props.status === 'PENDING') return colors.amber;
    if (props.status === 'CANCELLED') return colors.red;
    if (props.status === 'COMPLETED') return colors.blue;
    return colors.darkGray;
  }};
`;

const PaymentBadge = styled.span`
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  
  background-color: ${props => {
    if (props.status === 'paid') return colors.lightGreen;
    return colors.lightRed;
  }};
  
  color: ${props => {
    if (props.status === 'paid') return colors.green;
    return colors.red;
  }};
`;

const ActionButton = styled.button`
  padding: 8px 15px;
  margin-right: 8px;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  background-color: ${props => {
    if (props.confirm) return colors.green;
    if (props.cancel) return colors.red;
    if (props.complete) return colors.blue;
    return colors.pineGreen;
  }};
  color: white;
  transition: all 0.2s;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
  
  svg {
    margin-right: 6px;
  }
  
  &:hover {
    opacity: 0.9;
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0,0,0,0.15);
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }
  
  @media (max-width: 576px) {
    margin-bottom: 8px;
    width: 100%;
  }
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  
  @media (max-width: 576px) {
    flex-direction: column;
  }
`;

const EmptyState = styled.div`
  padding: 70px 20px;
  text-align: center;
  color: ${colors.mediumGray};
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  
  svg {
    font-size: 48px;
    margin-bottom: 15px;
    color: ${colors.lightPineGreen};
    opacity: 0.7;
  }
`;

const EmptyStateMessage = styled.div`
  font-size: 18px;
  margin-bottom: 10px;
  color: ${colors.darkGray};
`;

const EmptyStateSubtext = styled.div`
  font-size: 14px;
  max-width: 400px;
  margin: 0 auto;
`;

const PaginationContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  margin-top: 30px;
  gap: 10px;
  align-items: center;
`;

const PageInfo = styled.div`
  color: ${colors.mediumGray};
  font-size: 14px;
  margin-right: 15px;
`;

const PaginationButton = styled.button`
  padding: 10px 15px;
  border: 1px solid #ddd;
  background-color: ${props => props.active ? colors.pineGreen : colors.white};
  color: ${props => props.active ? colors.white : colors.darkGray};
  border-radius: 8px;
  cursor: ${props => props.disabled ? 'not-allowed' : 'pointer'};
  opacity: ${props => props.disabled ? 0.5 : 1};
  transition: all 0.2s;
  font-weight: ${props => props.active ? 600 : 400};
  
  &:hover:not(:disabled) {
    background-color: ${props => props.active ? colors.pineGreen : colors.lightGray};
    transform: translateY(-2px);
  }
`;

const ModalOverlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(3px);
`;

const ModalContent = styled.div`
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.2);
  padding: 30px;
  max-width: 900px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  animation: modalFadeIn 0.3s ease;
  
  @keyframes modalFadeIn {
    from {
      opacity: 0;
      transform: translateY(-20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
`;

const ModalHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  border-bottom: 1px solid #eee;
  padding-bottom: 20px;
`;

const ModalTitle = styled.h3`
  margin: 0;
  color: ${colors.darkGray};
  font-size: 24px;
`;

const CloseButton = styled.button`
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: ${colors.mediumGray};
  transition: color 0.2s;
  
  &:hover {
    color: ${colors.darkGray};
  }
`;

const TabsContainer = styled.div`
  display: flex;
  margin-bottom: 25px;
  border-bottom: 1px solid #eee;
`;

const TabButton = styled.button`
  padding: 12px 20px;
  background: none;
  border: none;
  border-bottom: 3px solid ${props => props.active ? colors.pineGreen : 'transparent'};
  color: ${props => props.active ? colors.pineGreen : colors.mediumGray};
  font-weight: ${props => props.active ? 600 : 400};
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    color: ${colors.pineGreen};
  }
`;

const DetailsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 30px;
`;

const DetailSection = styled.div`
  margin-bottom: 30px;
  background-color: ${colors.lightGray};
  border-radius: 12px;
  padding: 20px;
`;

const DetailSectionTitle = styled.h4`
  margin: 0 0 20px;
  color: ${colors.darkGray};
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  
  svg {
    margin-right: 8px;
    color: ${colors.pineGreen};
  }
`;

const DetailItem = styled.div`
  margin-bottom: 15px;
  
  &:last-child {
    margin-bottom: 0;
  }
`;

const DetailLabel = styled.div`
  font-size: 13px;
  color: ${colors.mediumGray};
  margin-bottom: 6px;
`;

const DetailValue = styled.div`
  font-size: 15px;
  color: ${colors.darkGray};
  font-weight: ${props => props.bold ? 600 : 400};
`;

const BikeDetailCard = styled.div`
  display: flex;
  align-items: center;
  background-color: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
`;

const BikeDetailImage = styled.img`
  width: 120px;
  height: 120px;
  border-radius: 12px;
  object-fit: cover;
  margin-right: 25px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
`;

const BikeDetailInfo = styled.div`
  flex: 1;
`;

const BikeDetailName = styled.div`
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 8px;
  color: ${colors.darkGray};
`;

const BikeDetailType = styled.div`
  font-size: 15px;
  color: ${colors.mediumGray};
  margin-bottom: 15px;
`;

const BikeDetailPrice = styled.div`
  font-weight: 600;
  color: ${colors.pineGreen};
  font-size: 18px;
`;

const ActionButtonsContainer = styled.div`
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
  
  @media (max-width: 576px) {
    flex-direction: column;
  }
`;

const BookingInfoCard = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

const BookingInfoLabel = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  font-weight: 500;
`;

const BookingInfoValue = styled.div`
  font-size: 14px;
  font-weight: ${props => props.bold ? '600' : 'normal'};
  color: ${colors.darkGray};
`;

const BookingPrice = styled.div`
  font-weight: 600;
  font-size: 15px;
  color: ${colors.pineGreen};
`;

const BookingDuration = styled.div`
  font-weight: 500;
  color: ${colors.blue};
  background-color: ${colors.lightBlue};
  border-radius: 4px;
  padding: 3px 6px;
  display: inline-block;
  font-size: 13px;
`;

const BookingSummarySection = styled.div`
  background-color: white;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
`;

const BookingSummaryTitle = styled.h3`
  font-size: 18px;
  color: ${colors.darkGray};
  margin: 0 0 20px 0;
  display: flex;
  align-items: center;
  
  svg {
    margin-right: 8px;
    color: ${colors.pineGreen};
  }
`;

const BookingSummaryGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 30px;
`;

const SummaryCard = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

const SummaryLabel = styled.div`
  font-size: 14px;
  color: ${colors.mediumGray};
  font-weight: 500;
`;

const SummaryValue = styled.div`
  font-size: 24px;
  font-weight: 600;
  color: ${colors.pineGreen};
`;

const SummarySubtext = styled.div`
  font-size: 13px;
  color: ${colors.mediumGray};
`;

const BikeBookingsSection = styled.div`
  background-color: white;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
`;

const BikeBookingsList = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
`;

const BikeBookingCard = styled.div`
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 15px;
  background-color: ${colors.lightGray};
  transition: transform 0.2s, box-shadow 0.2s;
  
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 4px 10px rgba(0,0,0,0.08);
  }
`;

const BikeHeader = styled.div`
  display: flex;
  margin-bottom: 15px;
  align-items: center;
`;

const BikeImageSmall = styled.img`
  width: 60px;
  height: 60px;
  border-radius: 8px;
  margin-right: 15px;
  object-fit: cover;
`;

const BikeInfo = styled.div`
  flex: 1;
`;

const BikeStats = styled.div`
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
`;

const BikeStat = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  background-color: rgba(0,0,0,0.05);
  padding: 4px 8px;
  border-radius: 4px;
`;

const BookingMiniList = styled.div`
  max-height: 200px;
  overflow-y: auto;
  border-top: 1px solid #eee;
  padding-top: 10px;
`;

const BookingMiniItem = styled.div`
  padding: 8px;
  border-bottom: 1px solid #f5f5f5;
  font-size: 13px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  &:hover {
    background-color: white;
  }
`;

const BookingDate = styled.div`
  font-size: 13px;
  color: ${colors.mediumGray};
`;

const ViewAllButton = styled.button`
  width: 100%;
  padding: 8px;
  margin-top: 10px;
  background: none;
  border: 1px dashed ${colors.pineGreen};
  color: ${colors.pineGreen};
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  
  &:hover {
    background-color: ${colors.lightPineGreen};
    color: white;
  }
`;

const BikeNameDisplay = styled.div`
  display: flex;
  align-items: center;
`;

// Add a toggle button styled component
const ViewToggleButton = styled.button`
  background-color: ${props => props.active ? colors.pineGreen : 'white'};
  color: ${props => props.active ? 'white' : colors.pineGreen};
  border: 1px solid ${colors.pineGreen};
  padding: 8px 15px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
  margin-left: 10px;
  
  &:hover {
    background-color: ${colors.lightPineGreen};
    color: white;
  }
`;

const BookingStatusPill = styled.div`
  background-color: ${props => props.color ? `${props.color}20` : '#f0f0f0'};
  color: ${props => props.color || colors.darkGray};
  padding: 8px 15px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
`;

const BookingManagement = () => {
  const [bookings, setBookings] = useState([]);
  const [filteredBookings, setFilteredBookings] = useState([]);
  const [bikes, setBikes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('all');
  const [bikeFilter, setBikeFilter] = useState('all');
  const [bookingTypeFilter, setBookingTypeFilter] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [startDateFilter, setStartDateFilter] = useState('');
  const [endDateFilter, setEndDateFilter] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [updating, setUpdating] = useState(false);
  const [sortBy, setSortBy] = useState('startTime');
  const [sortDirection, setSortDirection] = useState('desc');
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [activeTab, setActiveTab] = useState('details');
  const [revenueData, setRevenueData] = useState({
    day: { totalRevenue: 0, bookings: 0 },
    week: { totalRevenue: 0, bookings: 0 },
    month: { totalRevenue: 0, bookings: 0 }
  });
  const [bikeBookingsMap, setBikeBookingsMap] = useState({});
  const [selectedBikeId, setSelectedBikeId] = useState(null);
  const [showAllBikesView, setShowAllBikesView] = useState(true);
  
  const bookingsPerPage = 10;

  // Clear all filters
  const clearFilters = () => {
    setStatusFilter('all');
    setBikeFilter('all');
    setBookingTypeFilter('all');
    setSearchTerm('');
    setStartDateFilter('');
    setEndDateFilter('');
    setSortBy('startTime');
    setSortDirection('desc');
  };

  // Remove individual filter
  const removeFilter = (filterType) => {
    switch (filterType) {
      case 'status':
        setStatusFilter('all');
        break;
      case 'bike':
        setBikeFilter('all');
        break;
      case 'type':
        setBookingTypeFilter('all');
        break;
      case 'search':
        setSearchTerm('');
        break;
      case 'dateRange':
        setStartDateFilter('');
        setEndDateFilter('');
        break;
      default:
        break;
    }
  };

  // Get active filters for filter tags
  const getActiveFilters = () => {
    const filters = [];
    
    if (statusFilter !== 'all') {
      filters.push({
        type: 'status',
        label: `Status: ${statusFilter}`,
      });
    }
    
    if (bikeFilter !== 'all') {
      const bike = bikes.find(b => b.id === bikeFilter);
      filters.push({
        type: 'bike',
        label: `Bike: ${bike ? bike.name : bikeFilter}`,
      });
    }
    
    if (bookingTypeFilter !== 'all') {
      filters.push({
        type: 'type',
        label: `Type: ${bookingTypeFilter}`,
      });
    }
    
    if (searchTerm) {
      filters.push({
        type: 'search',
        label: `Search: ${searchTerm}`,
      });
    }
    
    if (startDateFilter && endDateFilter) {
      filters.push({
        type: 'dateRange',
        label: `Date: ${startDateFilter} to ${endDateFilter}`,
      });
    }
    
    return filters;
  };

  useEffect(() => {
    // Load bookings, bikes, and revenue data
    const loadData = async () => {
      try {
        setLoading(true);
        
        // Load bookings and bikes in parallel
        const [bookingsData, bikesData] = await Promise.all([
          getAllBookings(),
          getBikes()
        ]);
        
        setBookings(bookingsData);
        setFilteredBookings(bookingsData);
        setBikes(bikesData);
        
        // Group bookings by bike
        const groupedBookings = {};
        bikesData.forEach(bike => {
          groupedBookings[bike.id] = bookingsData.filter(booking => booking.bikeId === bike.id);
        });
        setBikeBookingsMap(groupedBookings);
        
        // Load revenue data
        const [dayRevenue, weekRevenue, monthRevenue] = await Promise.all([
          getRevenueByPeriod('day'),
          getRevenueByPeriod('week'),
          getRevenueByPeriod('month')
        ]);
        
        setRevenueData({
          day: dayRevenue,
          week: weekRevenue,
          month: monthRevenue
        });
      } catch (error) {
        console.error('Error loading booking data:', error);
      } finally {
        setLoading(false);
      }
    };
    
    loadData();
  }, []);

  // Handle filter changes
  useEffect(() => {
    let filtered = [...bookings];
    
    // Filter by status
    if (statusFilter !== 'all') {
      filtered = filtered.filter(booking => booking.status === statusFilter);
    }
    
    // Filter by bike
    if (bikeFilter !== 'all') {
      filtered = filtered.filter(booking => booking.bikeId === bikeFilter);
    }
    
    // Filter by booking type (hourly/daily)
    if (bookingTypeFilter !== 'all') {
      const isHourly = bookingTypeFilter === 'hourly';
      filtered = filtered.filter(booking => booking.isHourly === isHourly);
    }
    
    // Filter by search term (user name)
    if (searchTerm) {
      filtered = filtered.filter(booking => 
        (booking.userName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
         booking.bikeName?.toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }
    
    // Filter by date range
    if (startDateFilter && endDateFilter) {
      const startDate = new Date(startDateFilter);
      const endDate = new Date(endDateFilter);
      endDate.setHours(23, 59, 59); // Set to end of day
      
      filtered = filtered.filter(booking => {
        const bookingStart = booking.startTime;
        return bookingStart >= startDate && bookingStart <= endDate;
      });
    }
    
    // Apply sorting
    filtered.sort((a, b) => {
      let valueA, valueB;
      
      switch (sortBy) {
        case 'startTime':
          valueA = a.startTime ? a.startTime.getTime() : 0;
          valueB = b.startTime ? b.startTime.getTime() : 0;
          break;
        case 'createdAt':
          valueA = a.createdAt ? a.createdAt.getTime() : 0;
          valueB = b.createdAt ? b.createdAt.getTime() : 0;
          break;
        case 'price':
          valueA = parseFloat(a.totalPrice) || 0;
          valueB = parseFloat(b.totalPrice) || 0;
          break;
        case 'duration':
          valueA = a.endTime && a.startTime ? a.endTime.getTime() - a.startTime.getTime() : 0;
          valueB = b.endTime && b.startTime ? b.endTime.getTime() - b.startTime.getTime() : 0;
          break;
        default:
          valueA = a[sortBy] || '';
          valueB = b[sortBy] || '';
      }
      
      // For string values
      if (typeof valueA === 'string') {
        return sortDirection === 'asc' 
          ? valueA.localeCompare(valueB)
          : valueB.localeCompare(valueA);
      }
      
      // For numeric values
      return sortDirection === 'asc' ? valueA - valueB : valueB - valueA;
    });
    
    setFilteredBookings(filtered);
    setCurrentPage(1); // Reset to first page when filters change
  }, [statusFilter, bikeFilter, bookingTypeFilter, searchTerm, startDateFilter, endDateFilter, sortBy, sortDirection, bookings]);

  // Get current bookings for pagination
  const indexOfLastBooking = currentPage * bookingsPerPage;
  const indexOfFirstBooking = indexOfLastBooking - bookingsPerPage;
  const currentBookings = filteredBookings.slice(indexOfFirstBooking, indexOfLastBooking);
  const totalPages = Math.ceil(filteredBookings.length / bookingsPerPage);

  // Handle status change
  const handleStatusChange = async (bookingId, newStatus) => {
    try {
      setUpdating(true);
      await updateBooking(bookingId, { status: newStatus });
      
      // Update local state
      setBookings(prevBookings => 
        prevBookings.map(booking => 
          booking.id === bookingId ? { ...booking, status: newStatus } : booking
        )
      );
      
      // Also update selected booking if in modal
      if (selectedBooking && selectedBooking.id === bookingId) {
        setSelectedBooking(prevBooking => ({
          ...prevBooking,
          status: newStatus
        }));
      }
    } catch (error) {
      console.error('Error updating booking status:', error);
    } finally {
      setUpdating(false);
    }
  };

  // Handle payment status change
  const handlePaymentStatusChange = async (bookingId, newStatus) => {
    try {
      setUpdating(true);
      await updateBooking(bookingId, { paymentStatus: newStatus });
      
      // Update local state
      setBookings(prevBookings => 
        prevBookings.map(booking => 
          booking.id === bookingId ? { ...booking, paymentStatus: newStatus } : booking
        )
      );
      
      // Also update selected booking if in modal
      if (selectedBooking && selectedBooking.id === bookingId) {
        setSelectedBooking(prevBooking => ({
          ...prevBooking,
          paymentStatus: newStatus
        }));
      }
    } catch (error) {
      console.error('Error updating payment status:', error);
    } finally {
      setUpdating(false);
    }
  };

  // Handle booking deletion
  const handleDeleteBooking = async (bookingId) => {
    if (window.confirm('Are you sure you want to delete this booking? This action cannot be undone.')) {
      try {
        setUpdating(true);
        await deleteBooking(bookingId);
        
        // Update local state
        setBookings(prevBookings => 
          prevBookings.filter(booking => booking.id !== bookingId)
        );
        
        // Close modal if open
        if (selectedBooking && selectedBooking.id === bookingId) {
          setShowDetailsModal(false);
        }
      } catch (error) {
        console.error('Error deleting booking:', error);
      } finally {
        setUpdating(false);
      }
    }
  };
  
  // Handle showing booking details
  const handleViewDetails = (booking) => {
    setSelectedBooking(booking);
    setShowDetailsModal(true);
    setActiveTab('details'); // Reset to details tab
  };
  
  // Handle sort change
  const handleSortChange = (field) => {
    if (sortBy === field) {
      // Toggle direction if same field
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      // New field, default to descending for dates, ascending for others
      setSortBy(field);
      setSortDirection(['startTime', 'createdAt'].includes(field) ? 'desc' : 'asc');
    }
  };

  // Handle viewing all bookings for a specific bike
  const handleViewBikeBookings = (bikeId) => {
    setSelectedBikeId(bikeId);
    setBikeFilter(bikeId);
    setShowAllBikesView(false);
  };

  // Handle returning to all bikes view
  const handleViewAllBikes = () => {
    setSelectedBikeId(null);
    setBikeFilter('all');
    setShowAllBikesView(true);
  };

  if (loading) {
    return (
      <Container>
        <PageHeader>
          <PageTitle>Booking Management</PageTitle>
        </PageHeader>
        <EmptyState>
          <div>Loading bookings data...</div>
        </EmptyState>
      </Container>
    );
  }

  // Get active filters for display
  const activeFilters = getActiveFilters();

  return (
    <Container>
      <PageHeader>
        <PageTitle>Booking Management</PageTitle>
        <div>
          <ViewToggleButton 
            active={showAllBikesView}
            onClick={handleViewAllBikes}
          >
            All Bookings
          </ViewToggleButton>
          <ViewToggleButton 
            active={!showAllBikesView}
            onClick={() => setShowAllBikesView(false)}
          >
            Per Bike View
          </ViewToggleButton>
        </div>
      </PageHeader>
      
      {/* Revenue Summary */}
      <RevenueSummaryContainer>
        <RevenueCard period="day">
          <RevenueAmount>₱{revenueData.day.totalRevenue.toFixed(2)}</RevenueAmount>
          <RevenuePeriod>Today's Revenue</RevenuePeriod>
          <BookingCount>{revenueData.day.bookings} bookings</BookingCount>
        </RevenueCard>
        <RevenueCard period="week">
          <RevenueAmount>₱{revenueData.week.totalRevenue.toFixed(2)}</RevenueAmount>
          <RevenuePeriod>This Week's Revenue</RevenuePeriod>
          <BookingCount>{revenueData.week.bookings} bookings</BookingCount>
        </RevenueCard>
        <RevenueCard period="month">
          <RevenueAmount>₱{revenueData.month.totalRevenue.toFixed(2)}</RevenueAmount>
          <RevenuePeriod>This Month's Revenue</RevenuePeriod>
          <BookingCount>{revenueData.month.bookings} bookings</BookingCount>
        </RevenueCard>
      </RevenueSummaryContainer>
      
      {/* Booking Summary */}
      <BookingSummarySection>
        <BookingSummaryTitle>
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
          Booking Overview
        </BookingSummaryTitle>
        <BookingSummaryGrid>
          <SummaryCard>
            <SummaryLabel>Total Bookings</SummaryLabel>
            <SummaryValue>{bookings.length}</SummaryValue>
            <SummarySubtext>All-time bookings</SummarySubtext>
          </SummaryCard>
          
          <SummaryCard>
            <SummaryLabel>Active Bookings</SummaryLabel>
            <SummaryValue>{bookings.filter(b => b.status === 'CONFIRMED').length}</SummaryValue>
            <SummarySubtext>Currently confirmed bookings</SummarySubtext>
          </SummaryCard>
          
          <SummaryCard>
            <SummaryLabel>Pending Bookings</SummaryLabel>
            <SummaryValue>{bookings.filter(b => b.status === 'PENDING').length}</SummaryValue>
            <SummarySubtext>Awaiting confirmation</SummarySubtext>
          </SummaryCard>
          
          <SummaryCard>
            <SummaryLabel>Hourly vs Daily</SummaryLabel>
            <SummaryValue>
              {bookings.filter(b => b.isHourly).length} / {bookings.filter(b => !b.isHourly).length}
            </SummaryValue>
            <SummarySubtext>Hourly / Daily bookings</SummarySubtext>
          </SummaryCard>
        </BookingSummaryGrid>
      </BookingSummarySection>
      
      {/* Bike Bookings Overview */}
      <BikeBookingsSection>
        <BookingSummaryTitle>
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="5" cy="16" r="3"></circle>
            <circle cx="19" cy="16" r="3"></circle>
            <path d="M19 16v-6a2 2 0 0 0-2-2H9"></path>
            <path d="M5 16v-2h10"></path>
          </svg>
          {showAllBikesView ? 'All Bikes Booking Overview' : `Bookings for ${bikes.find(b => b.id === selectedBikeId)?.name || 'Selected Bike'}`}
        </BookingSummaryTitle>
        
        {showAllBikesView ? (
          <BikeBookingsList>
            {bikes.map(bike => {
              const bikeBookings = bikeBookingsMap[bike.id] || [];
              const activeBookings = bikeBookings.filter(b => b.status === 'CONFIRMED').length;
              const completedBookings = bikeBookings.filter(b => b.status === 'COMPLETED').length;
              
              return (
                <BikeBookingCard key={bike.id}>
                  <BikeHeader>
                    {bike.imageUrl && <BikeImageSmall src={bike.imageUrl} alt={bike.name} />}
                    <BikeInfo>
                      <BikeNameHeader>{bike.name}</BikeNameHeader>
                      <BikeStats>
                        <BikeStat>
                          <strong>{bikeBookings.length}</strong> bookings
                        </BikeStat>
                        <BikeStat>
                          <strong>{activeBookings}</strong> active
                        </BikeStat>
                        <BikeStat>
                          <strong>{completedBookings}</strong> completed
                        </BikeStat>
                      </BikeStats>
                    </BikeInfo>
                  </BikeHeader>
                  
                  <BookingMiniList>
                    {bikeBookings.slice(0, 5).map(booking => (
                      <BookingMiniItem key={booking.id}>
                        <div>
                          <StatusBadge status={booking.status} style={{ fontSize: '10px', padding: '3px 8px' }}>
                            {booking.status}
                          </StatusBadge>
                          <span style={{ marginLeft: '8px' }}>{booking.userName}</span>
                        </div>
                        <BookingDate>
                          {booking.startTime ? format(booking.startTime, 'MMM d, yyyy') : 'N/A'}
                        </BookingDate>
                      </BookingMiniItem>
                    ))}
                    
                    {bikeBookings.length === 0 && (
                      <div style={{ padding: '15px 0', textAlign: 'center', color: colors.mediumGray }}>
                        No bookings yet for this bike
                      </div>
                    )}
                  </BookingMiniList>
                  
                  {bikeBookings.length > 0 && (
                    <ViewAllButton onClick={() => handleViewBikeBookings(bike.id)}>
                      View all {bikeBookings.length} bookings
                    </ViewAllButton>
                  )}
                </BikeBookingCard>
              );
            })}
          </BikeBookingsList>
        ) : (
          <div>
            <button 
              style={{ 
                background: 'none', 
                border: 'none', 
                color: colors.pineGreen, 
                cursor: 'pointer', 
                display: 'flex', 
                alignItems: 'center', 
                marginBottom: '20px' 
              }}
              onClick={handleViewAllBikes}
            >
              <svg 
                xmlns="http://www.w3.org/2000/svg" 
                width="16" 
                height="16" 
                viewBox="0 0 24 24" 
                fill="none" 
                stroke="currentColor" 
                strokeWidth="2" 
                strokeLinecap="round" 
                strokeLinejoin="round"
                style={{ marginRight: '8px' }}
              >
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
              </svg>
              Back to all bikes
            </button>
            
            {/* Add after the "Back to all bikes" button in the Bike Bookings Overview section */}
            <div style={{
              background: colors.lightGray,
              padding: '20px',
              borderRadius: '10px',
              marginBottom: '20px'
            }}>
              {/* Bike header with stats for selected bike */}
              {selectedBikeId && (
                <>
                  {(() => {
                    const selectedBike = bikes.find(b => b.id === selectedBikeId);
                    if (!selectedBike) return null;
                  
                    const bikeBookings = bikeBookingsMap[selectedBikeId] || [];
                    const pendingBookings = bikeBookings.filter(b => b.status === 'PENDING').length;
                    const activeBookings = bikeBookings.filter(b => b.status === 'CONFIRMED').length;
                    const completedBookings = bikeBookings.filter(b => b.status === 'COMPLETED').length;
                    const cancelledBookings = bikeBookings.filter(b => b.status === 'CANCELLED').length;
                    
                    // Calculate total revenue for this bike
                    const totalRevenue = bikeBookings.reduce((sum, booking) => {
                      return sum + (parseFloat(booking.totalPrice) || 0);
                    }, 0);
                    
                    return (
                      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '20px' }}>
                        {selectedBike.imageUrl && (
                          <img 
                            src={selectedBike.imageUrl} 
                            alt={selectedBike.name}
                            style={{ 
                              width: '120px', 
                              height: '120px', 
                              borderRadius: '10px',
                              objectFit: 'cover'
                            }}
                          />
                        )}
                        
                        <div style={{ flex: 1 }}>
                          <h2 style={{ margin: '0 0 10px 0', color: colors.darkGray }}>
                            {selectedBike.name}
                          </h2>
                          
                          <div style={{ display: 'flex', gap: '15px', marginBottom: '15px', flexWrap: 'wrap' }}>
                            <div style={{ 
                              padding: '10px 15px', 
                              backgroundColor: colors.lightPineGreen, 
                              color: 'white',
                              borderRadius: '8px',
                              fontWeight: '500'
                            }}>
                              {bikeBookings.length} Total Bookings
                            </div>
                            
                            <div style={{ 
                              padding: '10px 15px', 
                              backgroundColor: colors.lightBlue, 
                              color: colors.blue,
                              borderRadius: '8px',
                              fontWeight: '500'
                            }}>
                              ₱{totalRevenue.toFixed(2)} Revenue
                            </div>
                          </div>
                          
                          <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
                            <BookingStatusPill color={colors.amber}>
                              {pendingBookings} Pending
                            </BookingStatusPill>
                            
                            <BookingStatusPill color={colors.green}>
                              {activeBookings} Active
                            </BookingStatusPill>
                            
                            <BookingStatusPill color={colors.blue}>
                              {completedBookings} Completed
                            </BookingStatusPill>
                            
                            <BookingStatusPill color={colors.red}>
                              {cancelledBookings} Cancelled
                            </BookingStatusPill>
                          </div>
                        </div>
                      </div>
                    );
                  })()}
                </>
              )}
            </div>
            
            {/* Display filtered bookings for the selected bike */}
            {/* They will be shown in the main bookings table below */}
          </div>
        )}
      </BikeBookingsSection>
      
      {/* Filters */}
      <FilterPanel>
        <FilterPanelTitle>
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
          </svg>
          Filter Bookings
        </FilterPanelTitle>
        
        <FilterContainer>
          <FilterGroup>
            <FilterLabel>Status</FilterLabel>
            <FilterSelect 
              value={statusFilter} 
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="all">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="CONFIRMED">Confirmed</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="COMPLETED">Completed</option>
            </FilterSelect>
          </FilterGroup>
          
          <FilterGroup>
            <FilterLabel>Bike</FilterLabel>
            <FilterSelect 
              value={bikeFilter} 
              onChange={(e) => setBikeFilter(e.target.value)}
            >
              <option value="all">All Bikes</option>
              {bikes.map(bike => (
                <option key={bike.id} value={bike.id}>
                  {bike.name}
                </option>
              ))}
            </FilterSelect>
          </FilterGroup>
          
          <FilterGroup>
            <FilterLabel>Type</FilterLabel>
            <FilterSelect
              value={bookingTypeFilter}
              onChange={(e) => setBookingTypeFilter(e.target.value)}
            >
              <option value="all">All Types</option>
              <option value="hourly">Hourly</option>
              <option value="daily">Daily</option>
            </FilterSelect>
          </FilterGroup>
          
          <FilterGroup>
            <FilterLabel>Search</FilterLabel>
            <FilterInput 
              type="text" 
              value={searchTerm} 
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search user or bike name"
            />
          </FilterGroup>
          
          <FilterGroup>
            <FilterLabel>Start Date</FilterLabel>
            <FilterInput
              type="date"
              value={startDateFilter}
              onChange={(e) => setStartDateFilter(e.target.value)}
            />
          </FilterGroup>
          
          <FilterGroup>
            <FilterLabel>End Date</FilterLabel>
            <FilterInput
              type="date"
              value={endDateFilter}
              onChange={(e) => setEndDateFilter(e.target.value)}
            />
          </FilterGroup>
        </FilterContainer>
        
        {/* Active Filters */}
        {activeFilters.length > 0 && (
          <ActiveFiltersContainer>
            {activeFilters.map((filter, index) => (
              <FilterTag key={index}>
                {filter.label}
                <span onClick={() => removeFilter(filter.type)}>×</span>
              </FilterTag>
            ))}
          </ActiveFiltersContainer>
        )}
        
        <FilterActions>
          {activeFilters.length > 0 && (
            <ClearFiltersButton onClick={clearFilters}>
              Clear All Filters
            </ClearFiltersButton>
          )}
          
          <SortContainer>
            <SortLabel>Sort by:</SortLabel>
            <FilterSelect
              value={sortBy}
              onChange={(e) => handleSortChange(e.target.value)}
            >
              <option value="startTime">Start Date</option>
              <option value="createdAt">Creation Date</option>
              <option value="price">Price</option>
              <option value="duration">Duration</option>
              <option value="bikeName">Bike Name</option>
              <option value="userName">User Name</option>
            </FilterSelect>
            <FilterSelect
              value={sortDirection}
              onChange={(e) => setSortDirection(e.target.value)}
            >
              <option value="asc">Ascending</option>
              <option value="desc">Descending</option>
            </FilterSelect>
          </SortContainer>
        </FilterActions>
      </FilterPanel>
      
      {/* Bookings Table */}
      <BookingsTable>
        <TableHeader>
          <div>Bike</div>
          <div>User</div>
          <div>Booking Period</div>
          <div>Duration</div>
          <div>Total Price</div>
          <div>Status</div>
          <div>Created</div>
          <div>Actions</div>
        </TableHeader>
        
        {currentBookings.length === 0 ? (
          <EmptyState>
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <EmptyStateMessage>No bookings found</EmptyStateMessage>
            <EmptyStateSubtext>
              {activeFilters.length > 0 
                ? 'Try changing or clearing your filters to see more results.' 
                : 'There are no bookings in the system yet.'}
            </EmptyStateSubtext>
          </EmptyState>
        ) : (
          currentBookings.map(booking => (
            <TableRow key={booking.id} status={booking.status}>
              <TableCell label="Bike:">
                <BikeNameDisplay>
                  {booking.bikeImageUrl && (
                    <BikeImage src={booking.bikeImageUrl} alt={booking.bikeName} />
                  )}
                  <div>
                    <BookingInfoValue bold>{booking.bikeName || 'Unknown Bike'}</BookingInfoValue>
                    <BookingTypeTag isHourly={booking.isHourly}>
                      {booking.isHourly ? 'Hourly' : 'Daily'}
                    </BookingTypeTag>
                  </div>
                </BikeNameDisplay>
              </TableCell>
              
              <TableCell label="User:">
                <BookingInfoCard>
                  <BookingInfoLabel>Customer</BookingInfoLabel>
                  <BookingInfoValue bold>{booking.userName || 'Unknown User'}</BookingInfoValue>
                </BookingInfoCard>
              </TableCell>
              
              <TableCell label="Booking Period:">
                <BookingInfoCard>
                  <BookingInfoLabel>Dates</BookingInfoLabel>
                  <BookingInfoValue>
                    {booking.startTime ? (
                      <>
                        {format(booking.startTime, 'MMM d, yyyy')}
                        {booking.isHourly && (
                          <>
                            <br />
                            {format(booking.startTime, 'h:mm a')} - 
                            {booking.endTime && format(booking.endTime, 'h:mm a')}
                          </>
                        )}
                      </>
                    ) : 'N/A'}
                  </BookingInfoValue>
                  <BookingDuration>
                    {calculateBookingDuration(booking)}
                  </BookingDuration>
                </BookingInfoCard>
              </TableCell>
              
              <TableCell label="Duration:">
                <BookingInfoCard>
                  <BookingInfoLabel>Duration</BookingInfoLabel>
                  <BookingDuration>
                    {calculateBookingDuration(booking)}
                  </BookingDuration>
                </BookingInfoCard>
              </TableCell>
              
              <TableCell label="Total Price:">
                <BookingInfoCard>
                  <BookingInfoLabel>Price</BookingInfoLabel>
                  <BookingPrice>₱{parseFloat(booking.totalPrice).toFixed(2)}</BookingPrice>
                </BookingInfoCard>
              </TableCell>
              
              <TableCell label="Status:">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <StatusBadge status={booking.status}>
                    {booking.status}
                  </StatusBadge>
                  <PaymentBadge status={booking.paymentStatus}>
                    {booking.paymentStatus}
                  </PaymentBadge>
                </div>
              </TableCell>
              
              <TableCell label="Created:">
                {booking.createdAt ? format(booking.createdAt, 'MMM d, yyyy') : 'N/A'}
              </TableCell>
              
              <TableCell label="Actions:">
                <ButtonGroup>
                  <ViewDetailsButton onClick={() => handleViewDetails(booking)}>
                    Details
                  </ViewDetailsButton>
                  
                  {booking.status === 'PENDING' && (
                    <ActionButton 
                      confirm
                      disabled={updating}
                      onClick={() => handleStatusChange(booking.id, 'CONFIRMED')}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <polyline points="20 6 9 17 4 12"></polyline>
                      </svg>
                      Confirm
                    </ActionButton>
                  )}
                  
                  {(booking.status === 'PENDING' || booking.status === 'CONFIRMED') && (
                    <ActionButton 
                      cancel
                      disabled={updating}
                      onClick={() => handleStatusChange(booking.id, 'CANCELLED')}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                      </svg>
                      Cancel
                    </ActionButton>
                  )}
                  
                  {booking.status === 'CONFIRMED' && (
                    <ActionButton 
                      complete
                      disabled={updating}
                      onClick={() => handleStatusChange(booking.id, 'COMPLETED')}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                        <polyline points="22 4 12 14.01 9 11.01"></polyline>
                      </svg>
                      Complete
                    </ActionButton>
                  )}
                  
                  {booking.paymentStatus === 'unpaid' && (
                    <ActionButton 
                      disabled={updating}
                      onClick={() => handlePaymentStatusChange(booking.id, 'paid')}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <rect x="2" y="4" width="20" height="16" rx="2"></rect>
                        <line x1="12" y1="16" x2="12" y2="16.01"></line>
                        <path d="M17 10 L7 10"></path>
                        <path d="M12 7 L12 13"></path>
                      </svg>
                      Pay
                    </ActionButton>
                  )}
                </ButtonGroup>
              </TableCell>
            </TableRow>
          ))
        )}
      </BookingsTable>
      
      {/* Pagination */}
      {totalPages > 1 && (
        <PaginationContainer>
          <PageInfo>
            Showing {indexOfFirstBooking + 1}-{Math.min(indexOfLastBooking, filteredBookings.length)} of {filteredBookings.length} bookings
          </PageInfo>
          
          <PaginationButton
            onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
            disabled={currentPage === 1}
          >
            Previous
          </PaginationButton>
          
          {[...Array(totalPages)].map((_, index) => {
            // Only show a few page numbers around the current page
            if (
              index === 0 || 
              index === totalPages - 1 || 
              (index >= currentPage - 2 && index <= currentPage + 1)
            ) {
              return (
                <PaginationButton
                  key={index}
                  active={currentPage === index + 1}
                  onClick={() => setCurrentPage(index + 1)}
                >
                  {index + 1}
                </PaginationButton>
              );
            } else if (
              index === currentPage - 3 || 
              index === currentPage + 2
            ) {
              return <span key={index}>...</span>;
            }
            return null;
          })}
          
          <PaginationButton
            onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
            disabled={currentPage === totalPages}
          >
            Next
          </PaginationButton>
        </PaginationContainer>
      )}
      
      {/* Booking Details Modal */}
      {showDetailsModal && selectedBooking && (
        <ModalOverlay onClick={() => setShowDetailsModal(false)}>
          <ModalContent onClick={e => e.stopPropagation()}>
            <ModalHeader>
              <ModalTitle>Booking Details</ModalTitle>
              <CloseButton onClick={() => setShowDetailsModal(false)}>×</CloseButton>
            </ModalHeader>
            
            {/* Tabs for organizing booking details */}
            <TabsContainer>
              <TabButton 
                active={activeTab === 'details'} 
                onClick={() => setActiveTab('details')}
              >
                Booking Details
              </TabButton>
              <TabButton 
                active={activeTab === 'bike'} 
                onClick={() => setActiveTab('bike')}
              >
                Bike Information
              </TabButton>
              <TabButton 
                active={activeTab === 'user'} 
                onClick={() => setActiveTab('user')}
              >
                User Information
              </TabButton>
            </TabsContainer>
            
            {activeTab === 'details' && (
              <>
                {/* Status and quick actions */}
                <div style={{ 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  alignItems: 'center', 
                  marginBottom: '20px',
                  backgroundColor: selectedBooking.status === 'PENDING' ? colors.lightAmber :
                                selectedBooking.status === 'CONFIRMED' ? colors.lightGreen :
                                selectedBooking.status === 'CANCELLED' ? colors.lightRed :
                                colors.lightBlue,
                  padding: '15px',
                  borderRadius: '10px'
                }}>
                  <div>
                    <span style={{ fontWeight: 600 }}>Status: </span>
                    <StatusBadge status={selectedBooking.status}>
                      {selectedBooking.status}
                    </StatusBadge>
                  </div>
                  <div>
                    <span style={{ fontWeight: 600 }}>Payment: </span>
                    <PaymentBadge status={selectedBooking.paymentStatus}>
                      {selectedBooking.paymentStatus}
                    </PaymentBadge>
                  </div>
                </div>
                
                {/* Key Booking Information Card */}
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '20px',
                  marginBottom: '30px',
                  backgroundColor: colors.lightGray,
                  padding: '20px',
                  borderRadius: '12px'
                }}>
                  <div>
                    <DetailLabel>Customer</DetailLabel>
                    <DetailValue bold style={{ fontSize: '18px', marginTop: '5px' }}>
                      {selectedBooking.userName || 'Unknown User'}
                    </DetailValue>
                  </div>
                  
                  <div>
                    <DetailLabel>Bike</DetailLabel>
                    <DetailValue bold style={{ fontSize: '18px', marginTop: '5px' }}>
                      {selectedBooking.bikeName || 'Unknown Bike'}
                    </DetailValue>
                  </div>
                  
                  <div>
                    <DetailLabel>Duration</DetailLabel>
                    <DetailValue style={{ marginTop: '5px' }}>
                      <BookingDuration style={{ fontSize: '16px', padding: '6px 12px' }}>
                        {calculateBookingDuration(selectedBooking)}
                      </BookingDuration>
                    </DetailValue>
                  </div>
                  
                  <div>
                    <DetailLabel>Total Price</DetailLabel>
                    <DetailValue style={{ fontSize: '20px', color: colors.pineGreen, fontWeight: 'bold', marginTop: '5px' }}>
                      ₱{parseFloat(selectedBooking.totalPrice).toFixed(2)}
                    </DetailValue>
                  </div>
                </div>
                
                <DetailsGrid>
                  {/* Booking Information */}
                  <DetailSection>
                    <DetailSectionTitle>
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                        <line x1="16" y1="2" x2="16" y2="6"></line>
                        <line x1="8" y1="2" x2="8" y2="6"></line>
                        <line x1="3" y1="10" x2="21" y2="10"></line>
                      </svg>
                      Booking Information
                    </DetailSectionTitle>
                    
                    <DetailItem>
                      <DetailLabel>Booking ID</DetailLabel>
                      <DetailValue bold>{selectedBooking.id}</DetailValue>
                    </DetailItem>
                    
                    <DetailItem>
                      <DetailLabel>Booking Type</DetailLabel>
                      <DetailValue>
                        {selectedBooking.isHourly ? 'Hourly Rental' : 'Daily Rental'}
                      </DetailValue>
                    </DetailItem>
                    
                    <DetailItem>
                      <DetailLabel>Created At</DetailLabel>
                      <DetailValue>
                        {selectedBooking.createdAt 
                          ? format(selectedBooking.createdAt, 'MMM d, yyyy h:mm a') 
                          : 'N/A'}
                      </DetailValue>
                    </DetailItem>
                  </DetailSection>
                  
                  {/* Time and Duration */}
                  <DetailSection>
                    <DetailSectionTitle>
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <polyline points="12 6 12 12 16 14"></polyline>
                      </svg>
                      Time and Duration
                    </DetailSectionTitle>
                    
                    <DetailItem>
                      <DetailLabel>Start Time</DetailLabel>
                      <DetailValue>
                        {selectedBooking.startTime 
                          ? format(selectedBooking.startTime, 'MMM d, yyyy h:mm a')
                          : 'N/A'}
                      </DetailValue>
                    </DetailItem>
                    
                    <DetailItem>
                      <DetailLabel>End Time</DetailLabel>
                      <DetailValue>
                        {selectedBooking.endTime 
                          ? format(selectedBooking.endTime, 'MMM d, yyyy h:mm a')
                          : 'N/A'}
                      </DetailValue>
                    </DetailItem>
                  </DetailSection>
                </DetailsGrid>
              </>
            )}
            
            {activeTab === 'bike' && (
              <>
                {/* Bike Details */}
                <BikeDetailCard>
                  {selectedBooking.bikeImageUrl && (
                    <BikeDetailImage 
                      src={selectedBooking.bikeImageUrl} 
                      alt={selectedBooking.bikeName} 
                    />
                  )}
                  <BikeDetailInfo>
                    <BikeDetailName>{selectedBooking.bikeName}</BikeDetailName>
                    <BikeDetailType>{selectedBooking.bikeType}</BikeDetailType>
                    <BikeDetailPrice>₱{parseFloat(selectedBooking.totalPrice).toFixed(2)}</BikeDetailPrice>
                  </BikeDetailInfo>
                </BikeDetailCard>
                
                <DetailSection>
                  <DetailSectionTitle>
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <circle cx="12" cy="12" r="10"></circle>
                      <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                    Bike Location
                  </DetailSectionTitle>
                  
                  <DetailItem>
                    <DetailLabel>Location</DetailLabel>
                    <DetailValue>{selectedBooking.location || 'No location data available'}</DetailValue>
                  </DetailItem>
                </DetailSection>
              </>
            )}
            
            {activeTab === 'user' && (
              <DetailSection>
                <DetailSectionTitle>
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                  User Information
                </DetailSectionTitle>
                
                <DetailItem>
                  <DetailLabel>User Name</DetailLabel>
                  <DetailValue bold>{selectedBooking.userName}</DetailValue>
                </DetailItem>
                
                <DetailItem>
                  <DetailLabel>User ID</DetailLabel>
                  <DetailValue>{selectedBooking.userId}</DetailValue>
                </DetailItem>
                
                {selectedBooking.notes && (
                  <DetailItem>
                    <DetailLabel>Notes</DetailLabel>
                    <DetailValue>{selectedBooking.notes}</DetailValue>
                  </DetailItem>
                )}
              </DetailSection>
            )}
            
            {/* Modal Actions */}
            <ActionButtonsContainer>
              {selectedBooking.status === 'PENDING' && (
                <ActionButton 
                  confirm
                  disabled={updating}
                  onClick={() => handleStatusChange(selectedBooking.id, 'CONFIRMED')}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  Confirm Booking
                </ActionButton>
              )}
              
              {(selectedBooking.status === 'PENDING' || selectedBooking.status === 'CONFIRMED') && (
                <ActionButton 
                  cancel
                  disabled={updating}
                  onClick={() => handleStatusChange(selectedBooking.id, 'CANCELLED')}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                  Cancel Booking
                </ActionButton>
              )}
              
              {selectedBooking.status === 'CONFIRMED' && (
                <ActionButton 
                  complete
                  disabled={updating}
                  onClick={() => handleStatusChange(selectedBooking.id, 'COMPLETED')}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                  </svg>
                  Mark Completed
                </ActionButton>
              )}
              
              {selectedBooking.paymentStatus === 'unpaid' && (
                <ActionButton 
                  disabled={updating}
                  onClick={() => handlePaymentStatusChange(selectedBooking.id, 'paid')}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="2" y="4" width="20" height="16" rx="2"></rect>
                    <line x1="12" y1="16" x2="12" y2="16.01"></line>
                    <path d="M17 10 L7 10"></path>
                    <path d="M12 7 L12 13"></path>
                  </svg>
                  Mark as Paid
                </ActionButton>
              )}
              
              <ActionButton 
                cancel
                disabled={updating}
                onClick={() => handleDeleteBooking(selectedBooking.id)}
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="3 6 5 6 21 6"></polyline>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  <line x1="10" y1="11" x2="10" y2="17"></line>
                  <line x1="14" y1="11" x2="14" y2="17"></line>
                </svg>
                Delete Booking
              </ActionButton>
            </ActionButtonsContainer>
          </ModalContent>
        </ModalOverlay>
      )}
    </Container>
  );
};

export default BookingManagement; 