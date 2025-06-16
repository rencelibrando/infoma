import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { 
  getAllBookings, 
  getAllBookingsFromAllCollections,
  updateBooking, 
  deleteBooking,
  getBookingsByBike,
  getBookingsByDateRange,
  calculateBookingDuration,
  getRevenueByPeriod,
  getBookingsByUserRole
} from '../services/bookingService';
import { getAllBikes as getBikes, updateBikeStatus } from '../services/bikeService';
import { getUsers } from '../services/userService';
import { format, parseISO, startOfWeek, endOfWeek, addDays, startOfDay, endOfDay } from 'date-fns';
import { db, auth } from '../firebase';
import { 
  collection, 
  query, 
  orderBy, 
  doc,
  getDocs,
  collectionGroup,
  addDoc,
  setDoc,
  where,
  getDoc
} from 'firebase/firestore';
import { onAuthStateChanged } from 'firebase/auth';
import { Calendar, dateFnsLocalizer } from 'react-big-calendar';
import 'react-big-calendar/lib/css/react-big-calendar.css';

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

// Container styled component
const Container = styled.div`
  width: 100%;
  max-width: 1800px;
  margin: 0 auto;
  padding: 0 20px;
`;

const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  flex-wrap: wrap;
  gap: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid ${colors.lightGray};
`;

const PageTitle = styled.h2`
  font-size: 30px;
  color: ${colors.darkGray};
  margin: 0;
  font-weight: 600;
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
  margin: 0 0 25px 0;
  display: flex;
  align-items: center;
  font-weight: 600;
  
  svg {
    margin-right: 10px;
    color: ${colors.pineGreen};
  }
`;

const BookingSummaryGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 30px;
  
  @media (max-width: 768px) {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 20px;
  }
`;

const SummaryCard = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
  background-color: ${colors.lightGray};
  border-radius: 12px;
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 15px rgba(0, 0, 0, 0.08);
  }
`;

const SummaryLabel = styled.div`
  font-size: 14px;
  color: ${colors.mediumGray};
  font-weight: 500;
`;

const SummaryValue = styled.div`
  font-size: 28px;
  font-weight: 600;
  color: ${colors.pineGreen};
`;

const SummarySubtext = styled.div`
  font-size: 13px;
  color: ${colors.mediumGray};
  margin-top: 5px;
`;

const BookingsTable = styled.div`
  width: 100%;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 25px;
`;

const TableHeader = styled.div`
  display: grid;
  grid-template-columns: 1.5fr 1fr 1.5fr 1fr 1fr 1fr 1fr 1.5fr;
  background-color: ${colors.pineGreen};
  color: white;
  font-weight: 600;
  padding: 18px 20px;
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
  
  @media (max-width: 768px) {
    display: none;
  }
`;

const TableRow = styled.div`
  display: grid;
  grid-template-columns: 1.5fr 1fr 1.5fr 1fr 1fr 1fr 1fr 1.5fr;
  padding: 20px;
  border-bottom: 1px solid #eee;
  transition: all 0.3s;
  align-items: center;
  
  &:hover {
    background-color: #f9f9f9;
    transform: translateY(-2px);
    box-shadow: 0 2px 5px rgba(0,0,0,0.05);
  }
  
  &:last-child {
    border-bottom: none;
  }
  
  @media (max-width: 768px) {
    display: flex;
    flex-direction: column;
    padding: 20px 15px;
    position: relative;
    margin: 15px;
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
    width: 100%;
    
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
  width: 50px;
  height: 50px;
  border-radius: 8px;
  margin-right: 12px;
  object-fit: cover;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
`;

const BikeNameDisplay = styled.div`
  display: flex;
  align-items: center;
`;

const BookingTypeTag = styled.span`
  display: inline-block;
  padding: 4px 10px;
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
  padding: 8px 16px;
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
  padding: 80px 20px;
  text-align: center;
  color: ${colors.mediumGray};
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  
  svg {
    font-size: 48px;
    margin-bottom: 20px;
    color: ${colors.lightPineGreen};
    opacity: 0.7;
  }
`;

const EmptyStateMessage = styled.div`
  font-size: 18px;
  margin-bottom: 10px;
  color: ${colors.darkGray};
  font-weight: 500;
`;

const EmptyStateSubtext = styled.div`
  font-size: 14px;
  max-width: 400px;
  margin: 0 auto;
  line-height: 1.5;
`;

const PaginationContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  margin-top: 30px;
  gap: 10px;
  align-items: center;
  margin-bottom: 40px;
  
  @media (max-width: 768px) {
    justify-content: center;
    flex-wrap: wrap;
  }
`;

const PageInfo = styled.div`
  color: ${colors.mediumGray};
  font-size: 14px;
  margin-right: 15px;
  
  @media (max-width: 768px) {
    width: 100%;
    text-align: center;
    margin-right: 0;
    margin-bottom: 10px;
  }
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

const BikeBookingsSection = styled.div`
  background-color: white;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
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

const BookingInfoCard = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

const BookingInfoLabel = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  font-weight: 500;
  margin-bottom: 2px;
`;

const BookingInfoValue = styled.div`
  font-size: 14px;
  font-weight: ${props => props.bold ? '600' : 'normal'};
  color: ${colors.darkGray};
  line-height: 1.4;
`;

const UserNameLink = styled.span`
  color: ${colors.pineGreen};
  cursor: pointer;
  text-decoration: underline;
  font-weight: inherit;
  
  &:hover {
    color: ${colors.lightPineGreen};
  }
`;

const UserInfoValue = styled.div`
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
  padding: 4px 8px;
  display: inline-block;
  font-size: 13px;
  margin-top: 4px;
`;

// Add a new styled component for filters
const FiltersSection = styled.div`
  background-color: white;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 25px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
`;

const FiltersGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 18px;
  margin-bottom: 20px;
  
  @media (max-width: 768px) {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }
`;

const FilterSelect = styled.select`
  width: 100%;
  padding: 12px 15px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  background-color: ${colors.white};
  font-size: 14px;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 2px rgba(29, 60, 52, 0.1);
  }
`;

const FilterInput = styled.input`
  width: 100%;
  padding: 12px 15px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  background-color: ${colors.white};
  font-size: 14px;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 2px rgba(29, 60, 52, 0.1);
  }
`;

const FilterButton = styled.button`
  background-color: ${colors.pineGreen};
  color: white;
  border: none;
  border-radius: 8px;
  padding: 12px 20px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  
  &:hover {
    background-color: ${colors.lightPineGreen};
  }
  
  svg {
    margin-right: 8px;
  }
`;

const ActiveFiltersContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 15px;
`;

const FilterTag = styled.div`
  display: flex;
  align-items: center;
  background-color: ${colors.lightGray};
  border-radius: 20px;
  padding: 8px 14px;
  font-size: 13px;
  color: ${colors.darkGray};
  
  button {
    background: none;
    border: none;
    cursor: pointer;
    color: ${colors.mediumGray};
    margin-left: 8px;
    display: flex;
    align-items: center;
    
    &:hover {
      color: ${colors.red};
    }
  }
`;

// New styled components for calendar view
const CalendarContainer = styled.div`
  background-color: white;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  height: 700px;

  .rbc-calendar {
    height: 100%;
  }

  .rbc-event {
    background-color: ${colors.pineGreen};
  }

  .rbc-event.confirmed {
    background-color: ${colors.green};
  }

  .rbc-event.pending {
    background-color: ${colors.amber};
  }

  .rbc-event.cancelled {
    background-color: ${colors.red};
    text-decoration: line-through;
  }

  .rbc-event.completed {
    background-color: ${colors.blue};
  }

  .rbc-toolbar button {
    color: ${colors.darkGray};
  }

  .rbc-toolbar button.rbc-active {
    background-color: ${colors.pineGreen};
    color: white;
  }
  
  @media (max-width: 768px) {
    height: 600px;
    padding: 15px;
  }
`;

const ViewSwitchContainer = styled.div`
  display: flex;
  margin-bottom: 25px;
  border-radius: 8px;
  overflow: hidden;
  width: fit-content;
  border: 1px solid ${colors.pineGreen};
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
`;

const ViewSwitchButton = styled.button`
  background-color: ${props => props.active ? colors.pineGreen : 'white'};
  color: ${props => props.active ? 'white' : colors.pineGreen};
  border: none;
  padding: 12px 20px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    background-color: ${props => props.active ? colors.pineGreen : colors.lightGray};
  }
  
  &:first-child {
    border-right: 1px solid ${colors.pineGreen};
  }
`;

const BookingTooltip = styled.div`
  background-color: white;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
  max-width: 280px;
  border-left: 4px solid ${props => {
    // Safely access nested properties with optional chaining
    const status = props?.children?.props?.event?.status;
    if (status === 'CONFIRMED') return colors.green;
    if (status === 'PENDING') return colors.amber;
    if (status === 'CANCELLED') return colors.red;
    if (status === 'COMPLETED') return colors.blue;
    return colors.pineGreen;
  }};
  
  h4 {
    margin: 0 0 12px;
    color: ${colors.darkGray};
    font-size: 16px;
    border-bottom: 1px solid ${colors.lightGray};
    padding-bottom: 8px;
  }
  
  p {
    margin: 6px 0;
    font-size: 14px;
    color: ${colors.mediumGray};
    
    strong {
      color: ${colors.darkGray};
    }
  }
  
  button {
    background-color: ${colors.pineGreen};
    color: white;
    border: none;
    border-radius: 4px;
    padding: 8px 12px;
    margin-top: 12px;
    font-size: 13px;
    cursor: pointer;
    width: 100%;
    
    &:hover {
      background-color: ${colors.lightPineGreen};
    }
  }
`;

const ModalWrapper = styled.div`
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 1000;
  width: 80%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  background-color: white;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15), 0 5px 15px rgba(0, 0, 0, 0.10);
  padding: 0;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 10px;
  }

  &::-webkit-scrollbar-thumb {
    background: #c4c4c4;
    border-radius: 10px;
  }

  & > h3 {
    margin: 0;
    color: ${colors.darkGray};
    background: linear-gradient(to right, #ffffff, #f9f9f9);
    padding: 25px 30px;
    border-bottom: 1px solid #eee;
    font-size: 22px;
    position: sticky;
    top: 0;
    z-index: 10;
    backdrop-filter: blur(5px);
  }
`;

const UserInfoPopup = styled.div`
  position: absolute;
  background-color: white;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
  max-width: 280px;
  border-left: 4px solid ${colors.pineGreen};
  z-index: 1000;
`;

const UserInfoHeader = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 15px;
`;

const UserInfoRow = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 8px;
`;

const UserInfoLabel = styled.div`
  font-size: 13px;
  color: ${colors.mediumGray};
  font-weight: 500;
  margin-right: 8px;
`;

const Notification = styled.div`
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 2000;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 350px;
  animation: slideIn 0.3s ease;
  
  background-color: ${props => 
    props.type === 'success' ? colors.lightGreen : 
    props.type === 'error' ? colors.lightRed : 
    colors.lightGray};
  
  color: ${props => 
    props.type === 'success' ? colors.green : 
    props.type === 'error' ? colors.red : 
    colors.darkGray};
  
  @keyframes slideIn {
    from {
      transform: translateX(100%);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }
`;

const BookingManagement = () => {
  const [bookings, setBookings] = useState([]);
  const [bikes, setBikes] = useState([]);
  const [users, setUsers] = useState([]);
  const [revenueSummary, setRevenueSummary] = useState({
    day: { totalRevenue: 0, bookings: 0, completedBookings: 0 },
    week: { totalRevenue: 0, bookings: 0, completedBookings: 0 },
    month: { totalRevenue: 0, bookings: 0, completedBookings: 0 }
  });
  const [bookingSummary, setBookingSummary] = useState({});
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [userPopupPosition, setUserPopupPosition] = useState({ x: 0, y: 0 });
  const [viewMode, setViewMode] = useState('list');
  const [filters, setFilters] = useState({
    bike: '',
    status: '',
    payment: '',
    startDate: '',
    endDate: ''
  });
  const [filteredBookings, setFilteredBookings] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [isAdmin, setIsAdmin] = useState(true);
  const [showAllBikesView, setShowAllBikesView] = useState(true);
  const [updating, setUpdating] = useState(false);
  
  // Authentication state
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);
  
  // Notification state
  const [notification, setNotification] = useState(null);
  
  // Function to show a notification
  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
    
    // Auto-hide notification after 5 seconds
    setTimeout(() => {
      setNotification(null);
    }, 5000);
  };

  // Authentication check effect
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (user) {
        setUser(user);
        setIsAuthenticated(true);
        setAuthLoading(false);
      } else {
        setUser(null);
        setIsAuthenticated(false);
        setAuthLoading(false);
        showNotification('Please log in to access booking management', 'error');
      }
    });

    return () => unsubscribe();
  }, []);
  
  // Mock data for filters and pagination
  const [statusFilter, setStatusFilter] = useState('all');
  const [bikeFilter, setBikeFilter] = useState('all');
  const [bookingTypeFilter, setBookingTypeFilter] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [startDateFilter, setStartDateFilter] = useState('');
  const [endDateFilter, setEndDateFilter] = useState('');
  const [activeFilters, setActiveFilters] = useState([]);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  
  // Pagination constants
  const indexOfLastBooking = currentPage * itemsPerPage;
  const indexOfFirstBooking = indexOfLastBooking - itemsPerPage;
  const currentBookings = filteredBookings.slice(indexOfFirstBooking, indexOfLastBooking);
  
  const locales = {
    'en-US': require('date-fns/locale/en-US')
  };
  
  const localizer = dateFnsLocalizer({
    format,
    parse: parseISO,
    startOfWeek: () => startOfWeek(new Date()),
    getDay: (date) => date.getDay(),
    locales,
  });

  const handleViewAllBikes = () => {
    setShowAllBikesView(true);
  };

  // Function to clear filters
  const clearFilters = () => {
    setStatusFilter('all');
    setBikeFilter('all');
    setBookingTypeFilter('all');
    setSearchTerm('');
    setStartDateFilter('');
    setEndDateFilter('');
    setActiveFilters([]);
  };

  // Function to remove a filter
  const removeFilter = (type) => {
    switch(type) {
      case 'status': setStatusFilter('all'); break;
      case 'bike': setBikeFilter('all'); break;
      case 'type': setBookingTypeFilter('all'); break;
      case 'search': setSearchTerm(''); break;
      case 'date': 
        setStartDateFilter('');
        setEndDateFilter('');
        break;
      default: break;
    }
    setActiveFilters(activeFilters.filter(f => f.type !== type));
  };

  // Function to format bookings for calendar
  const formatBookingsForCalendar = () => {
    return bookings.map(booking => ({
      title: `${booking.bikeName} - ${booking.fullName || booking.userName}`,
      start: booking.startDate,
      end: booking.endDate,
      booking: booking,
      status: booking.status
    }));
  };

  // Event style getter for calendar
  const eventStyleGetter = (event) => {
    let style = {
      backgroundColor: colors.pineGreen
    };
    
    if (event.status === 'CONFIRMED') {
      style.backgroundColor = colors.green;
    } else if (event.status === 'PENDING') {
  // Function to check payment status for a booking
  const checkPaymentStatus = async (bookingId) => {
    try {
      // Query the payments collection for a payment matching this booking ID
      const paymentsRef = collection(db, 'payments');
      const q = query(paymentsRef, where('bookingId', '==', bookingId));
      const snapshot = await getDocs(q);
      
      if (snapshot.empty) {
        return 'unpaid';
      }
      
      // Check all payments for this booking (there should typically be just one)
      let paymentStatus = 'unpaid';
      snapshot.forEach((doc) => {
        const payment = doc.data();
        if (payment.status === 'CONFIRMED') {
          paymentStatus = 'paid';
        }
      });
      
      return paymentStatus;
    } catch (error) {
      console.error('Error checking payment status:', error);
      return 'error';
    }
  };

  // Update payment status in booking
  const updatePaymentStatusInBooking = async (bookingId) => {
    try {
      const paymentStatus = await checkPaymentStatus(bookingId);
      if (paymentStatus === 'error') {
        return; // Skip update if there was an error
      }
      
      // Update the booking's payment status
      await updateBooking(bookingId, { paymentStatus });
      
      // Update local state
      setBookings(prevBookings => 
        prevBookings.map(booking => 
          booking.id === bookingId ? {...booking, paymentStatus} : booking
        )
      );
      
      setFilteredBookings(prevFilteredBookings => 
        prevFilteredBookings.map(booking => 
          booking.id === bookingId ? {...booking, paymentStatus} : booking
        )
      );
      
      // If the selected booking is the one being updated, update it too
      if (selectedBooking && selectedBooking.id === bookingId) {
        setSelectedBooking({...selectedBooking, paymentStatus});
      }
      
    } catch (error) {
      console.error('Error updating payment status in booking:', error);
    }
  };

  // Function to view booking details
  const handleViewDetails = async (booking) => {
    setSelectedBooking(booking);
    setShowDetailsModal(true);
    
    // Check if payment status is current
    await updatePaymentStatusInBooking(booking.id);
  };

  // Function to handle user click
  const handleUserClick = (booking, e) => {
    e.stopPropagation();
    const rect = e.target.getBoundingClientRect();
    setUserPopupPosition({
      x: rect.left,
      y: rect.bottom + 5
    });
    setSelectedUser({
      fullName: booking.fullName || booking.userName,
      email: booking.userEmail,
      phone: booking.userPhone,
      userId: booking.userId,
      booking
    });
  };

  // Function to handle status changes
  const handleStatusChange = async (bookingId, newStatus) => {
    try {
      setUpdating(true);
      
      // Update booking status in the database
      const updatedBooking = await updateBooking(bookingId, {
        status: newStatus,
        updatedAt: new Date()
      });
      
      // If status is changing to COMPLETED or CANCELLED, also update the bike availability
      if (newStatus === 'COMPLETED' || newStatus === 'CANCELLED') {
        const booking = bookings.find(b => b.id === bookingId);
        if (booking && booking.bikeId) {
          // Update bike status - mark as available when booking is completed or cancelled
          await updateBikeStatus(booking.bikeId, {
            isAvailable: true,
            isInUse: false
          });
        }
      } else if (newStatus === 'CONFIRMED') {
        // When confirming, mark the bike as unavailable and in use
        const booking = bookings.find(b => b.id === bookingId);
        if (booking && booking.bikeId) {
          await updateBikeStatus(booking.bikeId, {
            isAvailable: false,  // Explicitly set as not available
            isInUse: true
          });
        }
      }
      
      // Update the bookings state with the updated booking
      setBookings(prevBookings => 
        prevBookings.map(booking => 
          booking.id === bookingId ? {...booking, status: newStatus} : booking
        )
      );
      
      // Update filtered bookings
      setFilteredBookings(prevFilteredBookings => 
        prevFilteredBookings.map(booking => 
          booking.id === bookingId ? {...booking, status: newStatus} : booking
        )
      );
      
      // If the selected booking is the one being updated, update it too
      if (selectedBooking && selectedBooking.id === bookingId) {
        setSelectedBooking({...selectedBooking, status: newStatus});
      }
      
      // Check and update payment status after changing booking status
      await updatePaymentStatusInBooking(bookingId);
      
      // Show success notification
      const booking = bookings.find(b => b.id === bookingId);
      const bookingName = booking?.bikeName || 'Booking';
      
      let statusText;
      switch(newStatus) {
        case 'CONFIRMED': statusText = 'confirmed'; break;
        case 'COMPLETED': statusText = 'completed'; break;
        case 'CANCELLED': statusText = 'cancelled'; break;
        default: statusText = newStatus.toLowerCase();
      }
      
      showNotification(`${bookingName} successfully ${statusText}!`, 'success');
      
    } catch (error) {
      showNotification(`Error updating booking: ${error.message}`, 'error');
    } finally {
      setUpdating(false);
    }
  };
  
  // Main data loading effect
  useEffect(() => {
    if (authLoading) {
      return;
    }

    if (!isAuthenticated || !user) {
      return;
    }
    
    // Load bookings, bikes, and revenue data
    const loadData = async () => {
      try {
        setIsLoading(true);
        
        // Load bikes and users in parallel
        const [bikesData, usersData] = await Promise.all([
          getBikes(),
          getUsers()
        ]);
        
        setBikes(bikesData);
        
        // Load bookings once
        await loadBookingsOnce(bikesData, usersData);
        
        // Load revenue data
        await loadRevenueData();
      } catch (error) {
        setIsLoading(false);
        if (error.code === 'permission-denied') {
          showNotification('Permission denied. Please ensure you are logged in.', 'error');
        } else {
          showNotification('Error loading booking data: ' + error.message, 'error');
        }
      }
    };

    loadData();
  }, [selectedBooking, isAuthenticated, authLoading, user]);
  
  // Load revenue data
  const loadRevenueData = async () => {
    try {
      const [dayRevenue, weekRevenue, monthRevenue] = await Promise.all([
        getRevenueByPeriod('day'),
        getRevenueByPeriod('week'),
        getRevenueByPeriod('month')
      ]);
      
      setRevenueSummary({
        day: dayRevenue,
        week: weekRevenue,
        month: monthRevenue
      });
    } catch (error) {
      showNotification('Error loading revenue data: ' + error.message, 'error');
    }
  };
  
  // Helper function to load bookings without real-time updates
  const loadBookingsOnce = async (bikesData, usersData) => {
    try {
      setIsLoading(true);
      
      // Get bookings from main collection
      const mainBookingsCollection = collection(db, "bookings");
      const mainSnapshot = await getDocs(mainBookingsCollection);
      
      const mainBookings = mainSnapshot.docs.map(doc => {
        const data = doc.data();
        return {
          id: doc.id,
          ...data,
          startDate: data.startDate?.toDate ? data.startDate.toDate() : 
                    data.startDate ? new Date(data.startDate) : null,
          endDate: data.endDate?.toDate ? data.endDate.toDate() : 
                  data.endDate ? new Date(data.endDate) : null,
          createdAt: data.createdAt?.toDate ? data.createdAt.toDate() : 
                    data.createdAt ? new Date(data.createdAt) : new Date(),
          source: 'main'
        };
      });

      // Get bookings from bike subcollections
      const bikeBookings = [];
      
      for (const bike of bikesData) {
        try {
          const bikeBookingsRef = collection(db, `bikes/${bike.id}/bookings`);
          const bikeBookingsSnapshot = await getDocs(bikeBookingsRef);
          
          if (bikeBookingsSnapshot.docs.length > 0) {
            bikeBookingsSnapshot.docs.forEach(doc => {
              const data = doc.data();
              bikeBookings.push({
                id: doc.id,
                ...data,
                startDate: data.startDate?.toDate ? data.startDate.toDate() : 
                          data.startDate ? new Date(data.startDate) : null,
                endDate: data.endDate?.toDate ? data.endDate.toDate() : 
                        data.endDate ? new Date(data.endDate) : null,
                createdAt: data.createdAt?.toDate ? data.createdAt.toDate() : 
                          data.createdAt ? new Date(data.createdAt) : new Date(),
                source: `bike-${bike.id}`,
                bikeName: data.bikeName || bike.name,
                bikeType: data.bikeType || bike.type
              });
            });
          }
        } catch (error) {
          // Silent error handling for individual bike collections
        }
      }

      // Get bookings from user subcollections
      const userBookings = [];
      
      for (const user of usersData) {
        try {
          const userBookingsRef = collection(db, `users/${user.id}/bookings`);
          const userBookingsSnapshot = await getDocs(userBookingsRef);
          
          if (userBookingsSnapshot.docs.length > 0) {
            userBookingsSnapshot.docs.forEach(doc => {
              const data = doc.data();
              userBookings.push({
                id: doc.id,
                ...data,
                startDate: data.startDate?.toDate ? data.startDate.toDate() : 
                          data.startDate ? new Date(data.startDate) : null,
                endDate: data.endDate?.toDate ? data.endDate.toDate() : 
                        data.endDate ? new Date(data.endDate) : null,
                createdAt: data.createdAt?.toDate ? data.createdAt.toDate() : 
                          data.createdAt ? new Date(data.createdAt) : new Date(),
                source: `user-${user.id}`,
                fullName: data.fullName || user.fullName || user.displayName,
                userEmail: data.userEmail || user.email
              });
            });
          }
        } catch (error) {
          // Silent error handling for individual user collections
        }
      }

      // Also try collectionGroup query as fallback
      let collectionGroupBookings = [];
      try {
        const bookingsGroupRef = collectionGroup(db, "bookings");
        const groupSnapshot = await getDocs(bookingsGroupRef);
        
        collectionGroupBookings = groupSnapshot.docs.map(doc => {
          const data = doc.data();
          return {
            id: doc.id,
            ...data,
            startDate: data.startDate?.toDate ? data.startDate.toDate() : 
                      data.startDate ? new Date(data.startDate) : null,
            endDate: data.endDate?.toDate ? data.endDate.toDate() : 
                    data.endDate ? new Date(data.endDate) : null,
            createdAt: data.createdAt?.toDate ? data.createdAt.toDate() : 
                      data.createdAt ? new Date(data.createdAt) : new Date(),
            source: 'collectionGroup',
            documentPath: doc.ref.path
          };
        });
      } catch (error) {
        // Silent error handling for collectionGroup query
      }

      // Merge all bookings and remove duplicates
      const allBookings = [...mainBookings];
      
      // Add bike subcollection bookings that aren't already in main collection
      bikeBookings.forEach(booking => {
        if (!allBookings.some(existing => existing.id === booking.id)) {
          allBookings.push(booking);
        }
      });
      
      // Add user subcollection bookings that aren't already in main collection or bike collections
      userBookings.forEach(booking => {
        if (!allBookings.some(existing => existing.id === booking.id)) {
          allBookings.push(booking);
        }
      });
      
      // Add any additional bookings from collectionGroup that we might have missed
      collectionGroupBookings.forEach(booking => {
        if (!allBookings.some(existing => existing.id === booking.id)) {
          allBookings.push(booking);
        }
      });
      
      // Enhance booking data with bike and user information
      const enhancedBookings = allBookings.map(booking => {
        let enhancedBooking = { ...booking };
        
        // Add bike information if missing
        if (!booking.bikeName && booking.bikeId) {
          const matchingBike = bikesData.find(bike => bike.id === booking.bikeId);
          if (matchingBike) {
            enhancedBooking = {
              ...enhancedBooking,
              bikeName: matchingBike.name || 'Unknown Bike',
              bikeType: matchingBike.type || '',
              bikeImageUrl: matchingBike.imageUrl || ''
            };
          }
        }
        
        // Add user fullName if missing
        if (!booking.fullName && booking.userId) {
          const matchingUser = usersData.find(user => user.id === booking.userId);
          if (matchingUser) {
            enhancedBooking = {
              ...enhancedBooking,
              fullName: matchingUser.fullName || matchingUser.displayName || booking.userName,
              userEmail: matchingUser.email,
              userPhone: matchingUser.phoneNumber
            };
          }
        }
        
        return enhancedBooking;
      });
      
      // Sort by creation date (newest first)
      enhancedBookings.sort((a, b) => {
        const aDate = a.createdAt || new Date(0);
        const bDate = b.createdAt || new Date(0);
        return bDate - aDate;
      });
      
      // Check payment status for all bookings
      for (const booking of enhancedBookings) {
        try {
          if (!booking.paymentStatus) {
            const paymentStatus = await checkPaymentStatus(booking.id);
            if (paymentStatus !== 'error') {
              booking.paymentStatus = paymentStatus;
              
              // Update the booking record with payment status if it's not already set
              await updateBooking(booking.id, { paymentStatus });
            }
          }
        } catch (error) {
          console.error(`Error checking payment for booking ${booking.id}:`, error);
        }
      }
      
      setBookings(enhancedBookings);
      setFilteredBookings(enhancedBookings);
      setIsLoading(false);
      
    } catch (error) {
      setIsLoading(false);
      showNotification('Error loading bookings: ' + error.message, 'error');
    }
  };
  
  // Apply filters and search whenever filter states change
  useEffect(() => {
    // Skip filtering if bookings are currently being updated
    if (isLoading) {
      return;
    }
    
    let result = [...bookings];
    
    // Filter by status
    if (statusFilter !== 'all') {
      result = result.filter(booking => booking.status === statusFilter);
    }
    
    // Filter by bike
    if (bikeFilter !== 'all') {
      result = result.filter(booking => booking.bikeId === bikeFilter);
    }
    
    // Filter by booking type
    if (bookingTypeFilter !== 'all') {
      result = result.filter(booking => 
        bookingTypeFilter === 'hourly' ? booking.isHourly : !booking.isHourly
      );
    }
    
    // Filter by search term
    if (searchTerm) {
      const lowercasedSearchTerm = searchTerm.toLowerCase();
      result = result.filter(booking => 
        (booking.fullName && booking.fullName.toLowerCase().includes(lowercasedSearchTerm)) ||
        (booking.userName && booking.userName.toLowerCase().includes(lowercasedSearchTerm)) ||
        (booking.bikeName && booking.bikeName.toLowerCase().includes(lowercasedSearchTerm)) ||
        (booking.id && booking.id.toLowerCase().includes(lowercasedSearchTerm))
      );
    }
    
    // Filter by date range
    if (startDateFilter && endDateFilter) {
      const startDate = new Date(startDateFilter);
      const endDate = new Date(endDateFilter);
      endDate.setHours(23, 59, 59); // Set to end of day
      
      result = result.filter(booking => {
        if (!booking.startDate) return false;
        const bookingDate = new Date(booking.startDate);
        return bookingDate >= startDate && bookingDate <= endDate;
      });
    }
    
    // Update active filters
    const newActiveFilters = [];
    
    if (statusFilter !== 'all') {
      newActiveFilters.push({ type: 'status', label: `Status: ${statusFilter}` });
    }
    
    if (bikeFilter !== 'all') {
      const bikeName = bikes.find(bike => bike.id === bikeFilter)?.name || bikeFilter;
      newActiveFilters.push({ type: 'bike', label: `Bike: ${bikeName}` });
    }
    
    if (bookingTypeFilter !== 'all') {
      newActiveFilters.push({ 
        type: 'type', 
        label: `Type: ${bookingTypeFilter === 'hourly' ? 'Hourly' : 'Daily'}` 
      });
    }
    
    if (searchTerm) {
      newActiveFilters.push({ type: 'search', label: `Search: ${searchTerm}` });
    }
    
    if (startDateFilter && endDateFilter) {
      newActiveFilters.push({ 
        type: 'date', 
        label: `Dates: ${startDateFilter} to ${endDateFilter}` 
      });
    }
    
    setActiveFilters(newActiveFilters);
    setFilteredBookings(result);
    setTotalPages(Math.ceil(result.length / itemsPerPage));
    setCurrentPage(1);
    
  }, [statusFilter, bikeFilter, bookingTypeFilter, searchTerm, startDateFilter, endDateFilter, bookings, bikes]);

  // Return loading state or actual UI
  
  // Show loading state while checking authentication
  if (authLoading) {
    return (
      <Container>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '50vh',
          flexDirection: 'column',
          gap: '20px'
        }}>
          <div style={{
            width: '40px',
            height: '40px',
            border: `4px solid ${colors.lightGray}`,
            borderTop: `4px solid ${colors.pineGreen}`,
            borderRadius: '50%',
            animation: 'spin 1s linear infinite'
          }}></div>
          <div style={{ color: colors.mediumGray, fontSize: '16px' }}>
            Checking authentication...
          </div>
          <style>
            {`
              @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
              }
            `}
          </style>
        </div>
      </Container>
    );
  }

  // Show error state if not authenticated
  if (!isAuthenticated) {
    return (
      <Container>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '50vh',
          flexDirection: 'column',
          gap: '20px',
          textAlign: 'center'
        }}>
          <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke={colors.red} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M9 12l2 2 4-4"/>
            <path d="M21 12c.552 0 1-.449 1-1s-.448-1-1-1-1 .449-1 1 .448 1 1 1z"/>
            <path d="M3 12c.552 0 1-.449 1-1s-.448-1-1-1-1 .449-1 1 .448 1 1 1z"/>
            <path d="M12 21c.552 0 1-.449 1-1s-.448-1-1-1-1 .449-1 1 .448 1 1 1z"/>
            <path d="M12 3c.552 0 1-.449 1-1s-.448-1-1-1-1 .449-1 1 .448 1 1 1z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
          <div>
            <div style={{ 
              fontSize: '24px', 
              fontWeight: '600', 
              color: colors.darkGray,
              marginBottom: '10px' 
            }}>
              Authentication Required
            </div>
            <div style={{ 
              fontSize: '16px', 
              color: colors.mediumGray,
              maxWidth: '400px',
              lineHeight: '1.5'
            }}>
              You need to be logged in to access booking management. 
              Please return to the dashboard and ensure you're properly authenticated.
            </div>
          </div>
        </div>
      </Container>
    );
  }

  return (
    <Container>
      <PageHeader>
        <PageTitle>Booking Management</PageTitle>
        {isAdmin && (
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
        )}
      </PageHeader>
      
      {/* Debug Information - Remove this section completely */}
      
      {/* Revenue Summary - Admin Only */}
      {isAdmin && (
        <RevenueSummaryContainer>
          <RevenueCard period="day">
            <RevenueAmount>₱{revenueSummary?.day?.totalRevenue?.toFixed(2) || '0.00'}</RevenueAmount>
            <RevenuePeriod>Today's Revenue</RevenuePeriod>
            <BookingCount>{revenueSummary?.day?.completedBookings || 0} completed bookings</BookingCount>
          </RevenueCard>
          <RevenueCard period="week">
            <RevenueAmount>₱{revenueSummary?.week?.totalRevenue?.toFixed(2) || '0.00'}</RevenueAmount>
            <RevenuePeriod>This Week's Revenue</RevenuePeriod>
            <BookingCount>{revenueSummary?.week?.completedBookings || 0} completed bookings</BookingCount>
          </RevenueCard>
          <RevenueCard period="month">
            <RevenueAmount>₱{revenueSummary?.month?.totalRevenue?.toFixed(2) || '0.00'}</RevenueAmount>
            <RevenuePeriod>This Month's Revenue</RevenuePeriod>
            <BookingCount>{revenueSummary?.month?.completedBookings || 0} completed bookings</BookingCount>
          </RevenueCard>
        </RevenueSummaryContainer>
      )}
      
      {/* Booking Summary - Show for both admin and regular users */}
      <BookingSummarySection>
        <BookingSummaryTitle>
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
          {isAdmin ? 'Booking Overview' : 'Your Bookings'}
        </BookingSummaryTitle>
        <BookingSummaryGrid>
          <SummaryCard>
            <SummaryLabel>Total Bookings</SummaryLabel>
            <SummaryValue>{bookings.length}</SummaryValue>
            <SummarySubtext>{isAdmin ? 'All-time bookings' : 'All your bookings'}</SummarySubtext>
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
      
      {/* View Toggle for Admin Users */}
      {isAdmin && (
        <ViewSwitchContainer>
          <ViewSwitchButton 
            active={viewMode === 'list'} 
            onClick={() => setViewMode('list')}
          >
            List View
          </ViewSwitchButton>
          <ViewSwitchButton 
            active={viewMode === 'calendar'} 
            onClick={() => setViewMode('calendar')}
          >
            Calendar View
          </ViewSwitchButton>
        </ViewSwitchContainer>
      )}
      
      {/* Calendar View - Admin Only */}
      {isAdmin && viewMode === 'calendar' && (
        <CalendarContainer>
          <Calendar
            localizer={localizer}
            events={formatBookingsForCalendar()}
            startAccessor="start"
            endAccessor="end"
            style={{ height: 650 }}
            eventPropGetter={eventStyleGetter}
            components={{
              event: EventTooltip,
              toolbar: CalendarToolbar
            }}
            views={['month', 'week', 'day', 'agenda']}
            defaultView="week"
            step={60}
            timeslots={2}
            selectable
            onSelectEvent={(event) => handleViewDetails(event.booking)}
            dayPropGetter={date => {
              const today = new Date();
              return {
                style: {
                  backgroundColor: 
                    date.getDate() === today.getDate() &&
                    date.getMonth() === today.getMonth() &&
                    date.getFullYear() === today.getFullYear()
                      ? '#f8f9ff'
                      : undefined,
                }
              };
            }}
            dayLayoutAlgorithm="no-overlap"
            popup
            tooltipAccessor={null}
            formats={{
              timeGutterFormat: (date, culture, localizer) =>
                localizer.format(date, 'h a', culture),
              dayFormat: (date, culture, localizer) =>
                localizer.format(date, 'ddd dd', culture),
            }}
          />
        </CalendarContainer>
      )}
      
      {/* Only show the list view when in list mode */}
      {viewMode === 'list' && (
        <>
          {/* Filters Section */}
          <FiltersSection>
            <BookingSummaryTitle>
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
              </svg>
              Filters & Search
            </BookingSummaryTitle>
            
            <FiltersGrid>
              <div>
                <FilterSelect 
                  value={statusFilter} 
                  onChange={e => setStatusFilter(e.target.value)}
                >
                  <option value="all">All Statuses</option>
                  <option value="PENDING">Pending</option>
                  <option value="CONFIRMED">Confirmed</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="CANCELLED">Cancelled</option>
                </FilterSelect>
              </div>
              
              <div>
                <FilterSelect 
                  value={bikeFilter} 
                  onChange={e => setBikeFilter(e.target.value)}
                >
                  <option value="all">All Bikes</option>
                  {bikes.map(bike => (
                    <option key={bike.id} value={bike.id}>{bike.name}</option>
                  ))}
                </FilterSelect>
              </div>
              
              <div>
                <FilterSelect 
                  value={bookingTypeFilter} 
                  onChange={e => setBookingTypeFilter(e.target.value)}
                >
                  <option value="all">All Types</option>
                  <option value="hourly">Hourly Bookings</option>
                  <option value="daily">Daily Bookings</option>
                </FilterSelect>
              </div>
              
              <div>
                <FilterInput 
                  type="text"
                  placeholder="Search by name or ID"
                  value={searchTerm}
                  onChange={e => setSearchTerm(e.target.value)}
                />
              </div>
              
              <div>
                <FilterInput 
                  type="date"
                  value={startDateFilter}
                  onChange={e => setStartDateFilter(e.target.value)}
                  placeholder="Start Date"
                />
              </div>
              
              <div>
                <FilterInput 
                  type="date"
                  value={endDateFilter}
                  onChange={e => setEndDateFilter(e.target.value)}
                  placeholder="End Date"
                />
              </div>
              
              {/* Clear Filters Button */}
              <FilterButton onClick={clearFilters}>
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
                Clear Filters
              </FilterButton>
            </FiltersGrid>
            
            {/* Display active filters as tags */}
            {activeFilters.length > 0 && (
              <ActiveFiltersContainer>
                {activeFilters.map((filter, index) => (
                  <FilterTag key={index}>
                    {filter.label}
                    <button onClick={() => removeFilter(filter.type)}>
                      <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                      </svg>
                    </button>
                  </FilterTag>
                ))}
              </ActiveFiltersContainer>
            )}
          </FiltersSection>
          
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
            
            {isLoading ? (
              <EmptyState>
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'center',
                  gap: '10px',
                  color: colors.pineGreen 
                }}>
                  <div style={{
                    width: '20px',
                    height: '20px',
                    border: `3px solid ${colors.lightGray}`,
                    borderTop: `3px solid ${colors.pineGreen}`,
                    borderRadius: '50%',
                    animation: 'spin 1s linear infinite'
                  }}></div>
                  <span>Loading bookings...</span>
                </div>
                <style>
                  {`
                    @keyframes spin {
                      0% { transform: rotate(0deg); }
                      100% { transform: rotate(360deg); }
                    }
                  `}
                </style>
              </EmptyState>
            ) : currentBookings.length === 0 ? (
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
                <TableRow key={booking.id} status={booking.status} onClick={() => handleViewDetails(booking)} style={{ cursor: 'pointer' }}>
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
                      <BookingInfoValue bold>
                        <UserNameLink onClick={(e) => handleUserClick(booking, e)}>
                          {booking.fullName || booking.userName || (booking.userId ? `User ID: ${booking.userId.substring(0, 8)}...` : 'Unknown User')}
                        </UserNameLink>
                      </BookingInfoValue>
                    </BookingInfoCard>
                  </TableCell>
                  
                  <TableCell label="Booking Period:">
                    <BookingInfoCard>
                      <BookingInfoLabel>Dates</BookingInfoLabel>
                      <BookingInfoValue>
                        {booking.startDate ? (
                          <>
                            {format(booking.startDate, 'MMM d, yyyy')}
                            {booking.isHourly && (
                              <>
                                <br />
                                {format(booking.startDate, 'h:mm a')} - 
                                {booking.endDate && format(booking.endDate, 'h:mm a')}
                              </>
                            )}
                          </>
                        ) : 'N/A'}
                      </BookingInfoValue>
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
                      <ViewDetailsButton onClick={(e) => {
                        e.stopPropagation(); // Prevent row click event from firing
                        handleViewDetails(booking);
                      }}>
                        Details
                      </ViewDetailsButton>
                      
                      {booking.status === 'PENDING' && (
                        <ActionButton 
                          confirm
                          disabled={updating}
                          onClick={(e) => {
                            e.stopPropagation(); // Prevent row click event from firing
                            handleStatusChange(booking.id, 'CONFIRMED');
                          }}
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="20 6 9 17 4 12"></polyline>
                          </svg>
                          Confirm
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
              
              {Array.from({ length: totalPages }).map((_, index) => {
                // Show first, last, and pages around current page
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
                  // Show ellipsis for breaks
                  (index === 1 && currentPage > 3) ||
                  (index === totalPages - 2 && currentPage < totalPages - 2)
                ) {
                  return <span key={index} style={{ margin: '0 5px' }}>...</span>;
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
        </>
      )}

      {/* User Info Popup */}
      {selectedUser && (
        <UserInfoPopup 
          className="user-info-popup"
          x={userPopupPosition.x} 
          y={userPopupPosition.y}
        >
          <UserInfoHeader>
            <h4>User Details</h4>
            {selectedUser.loading && (
              <div style={{ 
                display: 'inline-block', 
                marginLeft: '10px',
                width: '16px',
                height: '16px',
                border: '2px solid rgba(29, 60, 52, 0.3)',
                borderTopColor: colors.pineGreen,
                borderRadius: '50%',
                animation: 'spin 1s linear infinite'
              }} />
            )}
            <style>
              {`
                @keyframes spin {
                  to { transform: rotate(360deg); }
                }
              `}
            </style>
          </UserInfoHeader>
          
          {selectedUser.profileUrl && (
            <div style={{ textAlign: 'center', marginBottom: '15px' }}>
              <img 
                src={selectedUser.profileUrl} 
                alt={selectedUser.fullName} 
                style={{
                  width: '60px',
                  height: '60px',
                  borderRadius: '50%',
                  objectFit: 'cover',
                  border: `2px solid ${colors.pineGreen}`
                }}
              />
            </div>
          )}
          
          <UserInfoRow>
            <UserInfoLabel>Name:</UserInfoLabel>
            <UserInfoValue bold>{selectedUser.fullName}</UserInfoValue>
          </UserInfoRow>
          
          <UserInfoRow>
            <UserInfoLabel>Email:</UserInfoLabel>
            <UserInfoValue>
              <a 
                href={`mailto:${selectedUser.email}`} 
                style={{ color: colors.pineGreen, textDecoration: 'none' }}
              >
                {selectedUser.email}
              </a>
            </UserInfoValue>
          </UserInfoRow>
          
          <UserInfoRow>
            <UserInfoLabel>Phone:</UserInfoLabel>
            <UserInfoValue>
              <a 
                href={`tel:${selectedUser.phone}`} 
                style={{ color: colors.pineGreen, textDecoration: 'none' }}
              >
                {selectedUser.phone}
              </a>
            </UserInfoValue>
          </UserInfoRow>
          
          <UserInfoRow>
            <UserInfoLabel>User ID:</UserInfoLabel>
            <UserInfoValue>{selectedUser.userId || 'N/A'}</UserInfoValue>
          </UserInfoRow>
          
          {selectedUser.address && (
            <UserInfoRow>
              <UserInfoLabel>Address:</UserInfoLabel>
              <UserInfoValue>{selectedUser.address}</UserInfoValue>
            </UserInfoRow>
          )}
          
          {selectedUser.role && (
            <UserInfoRow>
              <UserInfoLabel>Role:</UserInfoLabel>
              <UserInfoValue>{selectedUser.role}</UserInfoValue>
            </UserInfoRow>
          )}
          
          {selectedUser.membershipTier && (
            <UserInfoRow>
              <UserInfoLabel>Membership:</UserInfoLabel>
              <UserInfoValue>{selectedUser.membershipTier}</UserInfoValue>
            </UserInfoRow>
          )}
          
          {selectedUser.joinDate && (
            <UserInfoRow>
              <UserInfoLabel>Joined:</UserInfoLabel>
              <UserInfoValue>{selectedUser.joinDate}</UserInfoValue>
            </UserInfoRow>
          )}
          
          {selectedUser.emergencyContact && (
            <UserInfoRow>
              <UserInfoLabel>Emergency:</UserInfoLabel>
              <UserInfoValue>{selectedUser.emergencyContact}</UserInfoValue>
            </UserInfoRow>
          )}
          
          {/* View Booking button */}
          <button
            onClick={() => {
              setSelectedBooking(selectedUser.booking);
              setShowDetailsModal(true);
              setSelectedUser(null);
            }}
            style={{
              backgroundColor: colors.pineGreen,
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              padding: '8px 12px',
              marginTop: '15px',
              cursor: 'pointer',
              fontSize: '13px',
              width: '100%',
              transition: 'background-color 0.2s'
            }}
          >
            View Full Booking
          </button>
        </UserInfoPopup>
      )}

      {/* Booking Details Modal */}
      {showDetailsModal && selectedBooking && (
        <ModalWrapper>
          <h3>Booking Details</h3>
          
          <div style={{ padding: '0 30px 30px' }}>
            <div style={{ 
              display: 'grid', 
              gridTemplateColumns: '1fr 1fr', 
              gap: '30px', 
              marginBottom: '25px',
              marginTop: '20px',
              background: 'white',
              borderRadius: '12px',
              padding: '25px',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.05)'
            }}>
              <div>
                <h4 style={{ 
                  fontSize: '18px', 
                  margin: '0 0 5px 0', 
                  color: colors.darkGray,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px'
                }}>
                  {selectedBooking.bikeImageUrl && (
                    <BikeImage src={selectedBooking.bikeImageUrl} alt={selectedBooking.bikeName} 
                      style={{ width: '60px', height: '60px', borderRadius: '12px', marginRight: '0' }}
                    />
                  )}
                  {selectedBooking.bikeName || 'Unknown Bike'}
                </h4>
                <BookingTypeTag isHourly={selectedBooking.isHourly} style={{ marginTop: '12px' }}>
                  {selectedBooking.isHourly ? 'Hourly Rental' : 'Daily Rental'}
                </BookingTypeTag>
              </div>
              
              <div>
                <BookingInfoLabel>Bike ID</BookingInfoLabel>
                <BookingInfoValue>{selectedBooking.bikeId || 'N/A'}</BookingInfoValue>
                
                <div style={{ marginTop: '10px' }}>
                  <BookingInfoLabel>Bike Type</BookingInfoLabel>
                  <BookingInfoValue>{selectedBooking.bikeType || 'N/A'}</BookingInfoValue>
                </div>
              </div>
            </div>
            
            <div style={{ 
              background: 'white',
              borderRadius: '12px',
              padding: '25px',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.05)',
              marginBottom: '25px',
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '20px'
            }}>
              <div>
                <BookingInfoLabel>Name</BookingInfoLabel>
                <BookingInfoValue bold>
                  {selectedBooking.fullName || selectedBooking.userName || 'Unknown User'}
                </BookingInfoValue>
              </div>
              
              <div>
                <BookingInfoLabel>User ID</BookingInfoLabel>
                <BookingInfoValue>{selectedBooking.userId || 'N/A'}</BookingInfoValue>
              </div>
              
              <div>
                <BookingInfoLabel>Email</BookingInfoLabel>
                <BookingInfoValue>
                  <a href={`mailto:${selectedBooking.userEmail}`} style={{ color: colors.pineGreen }}>
                    {selectedBooking.userEmail || 'N/A'}
                  </a>
                </BookingInfoValue>
              </div>
              
              <div>
                <BookingInfoLabel>Phone</BookingInfoLabel>
                <BookingInfoValue>
                  <a href={`tel:${selectedBooking.userPhone}`} style={{ color: colors.pineGreen }}>
                    {selectedBooking.userPhone || 'N/A'}
                  </a>
                </BookingInfoValue>
              </div>
            </div>
          
            <div style={{ 
              background: '#f9f9f9', 
              padding: '25px', 
              borderRadius: '12px',
              display: 'grid',
              gridTemplateColumns: '1fr 1fr 1fr',
              gap: '20px',
              marginBottom: '25px',
              boxShadow: 'inset 0 2px 10px rgba(0, 0, 0, 0.04)',
              border: '1px solid #f0f0f0'
            }}>
              <div style={{ 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: 'flex-start',
              }}>
                <BookingInfoLabel>Booking Status</BookingInfoLabel>
                <StatusBadge status={selectedBooking.status} style={{ 
                  marginTop: '8px',
                  boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)'
                }}>
                  {selectedBooking.status}
                </StatusBadge>
              </div>
              
              <div style={{ 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: 'flex-start',
              }}>
                <BookingInfoLabel>Payment Status</BookingInfoLabel>
                <PaymentBadge status={selectedBooking.paymentStatus} style={{ 
                  marginTop: '8px',
                  boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)'
                }}>
                  {selectedBooking.paymentStatus || 'unpaid'}
                </PaymentBadge>
              </div>
              
              <div style={{ 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: 'flex-start',
              }}>
                <BookingInfoLabel>Total Amount</BookingInfoLabel>
                <BookingPrice style={{ 
                  marginTop: '8px', 
                  fontSize: '22px',
                  background: colors.pineGreen,
                  color: 'white',
                  padding: '8px 15px',
                  borderRadius: '10px',
                  boxShadow: '0 2px 8px rgba(0, 0, 0, 0.12)'
                }}>
                  ₱{parseFloat(selectedBooking.totalPrice).toFixed(2)}
                </BookingPrice>
              </div>
            </div>
            
            <div style={{ 
              display: 'grid', 
              gridTemplateColumns: '1fr 1fr', 
              gap: '30px', 
              marginBottom: '30px'
            }}>
              <div style={{ 
                background: 'white',
                padding: '25px',
                borderRadius: '12px',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.05)'
              }}>
                <h3 style={{ 
                  fontSize: '18px', 
                  marginTop: '0',
                  marginBottom: '20px', 
                  color: colors.darkGray,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px'
                }}>
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={colors.pineGreen} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                    <line x1="16" y1="2" x2="16" y2="6"></line>
                    <line x1="8" y1="2" x2="8" y2="6"></line>
                    <line x1="3" y1="10" x2="21" y2="10"></line>
                  </svg>
                  Booking Period
                </h3>
                
                <div style={{ 
                  background: '#f9f9f9',
                  padding: '20px',
                  borderRadius: '10px',
                  marginBottom: '10px',
                  border: '1px solid #f0f0f0'
                }}>
                  <BookingInfoLabel>Date</BookingInfoLabel>
                  <BookingInfoValue style={{ fontSize: '16px', fontWeight: '600' }}>
                    {selectedBooking.startDate ? format(selectedBooking.startDate, 'MMMM d, yyyy') : 'N/A'}
                  </BookingInfoValue>
                </div>
                
                {selectedBooking.isHourly && (
                  <div style={{ 
                    background: '#f9f9f9',
                    padding: '20px',
                    borderRadius: '10px',
                    marginBottom: '10px',
                    border: '1px solid #f0f0f0'
                  }}>
                    <BookingInfoLabel>Time</BookingInfoLabel>
                    <BookingInfoValue style={{ fontSize: '16px', fontWeight: '600' }}>
                      {selectedBooking.startDate && format(selectedBooking.startDate, 'h:mm a')} - 
                      {selectedBooking.endDate && format(selectedBooking.endDate, 'h:mm a')}
                    </BookingInfoValue>
                  </div>
                )}
                
                <div style={{ 
                  display: 'flex',
                  justifyContent: 'center',
                  marginTop: '20px'
                }}>
                  <BookingDuration style={{ 
                    marginTop: '5px', 
                    padding: '10px 20px',
                    fontSize: '15px',
                    fontWeight: '600',
                    boxShadow: '0 4px 10px rgba(33, 150, 243, 0.15)'
                  }}>
                    {calculateBookingDuration(selectedBooking)}
                  </BookingDuration>
                </div>
              </div>
              
              <div style={{ 
                background: 'white',
                padding: '25px',
                borderRadius: '12px',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.05)'
              }}>
                <h3 style={{ 
                  fontSize: '18px', 
                  marginTop: '0',
                  marginBottom: '20px', 
                  color: colors.darkGray,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px'
                }}>
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={colors.pineGreen} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                    <line x1="16" y1="13" x2="8" y2="13"></line>
                    <line x1="16" y1="17" x2="8" y2="17"></line>
                    <polyline points="10 9 9 9 8 9"></polyline>
                  </svg>
                  Additional Information
                </h3>
                
                <div style={{ 
                  background: '#f9f9f9',
                  padding: '20px',
                  borderRadius: '10px',
                  marginBottom: '10px',
                  border: '1px solid #f0f0f0'
                }}>
                  <BookingInfoLabel>Booking ID</BookingInfoLabel>
                  <BookingInfoValue>{selectedBooking.id || 'N/A'}</BookingInfoValue>
                </div>
                
                <div style={{ 
                  background: '#f9f9f9',
                  padding: '20px',
                  borderRadius: '10px',
                  marginBottom: '10px',
                  border: '1px solid #f0f0f0'
                }}>
                  <BookingInfoLabel>Created At</BookingInfoLabel>
                  <BookingInfoValue>
                    {selectedBooking.createdAt ? format(selectedBooking.createdAt, 'MMMM d, yyyy h:mm a') : 'N/A'}
                  </BookingInfoValue>
                </div>
                
                <div style={{ 
                  background: '#f9f9f9',
                  padding: '20px',
                  borderRadius: '10px',
                  border: '1px solid #f0f0f0'
                }}>
                  <BookingInfoLabel>Additional Notes</BookingInfoLabel>
                  <BookingInfoValue>
                    {selectedBooking.notes || 'No additional notes'}
                  </BookingInfoValue>
                </div>
              </div>
            </div>
            
            {/* Actions */}
            <div style={{ 
              borderTop: '1px solid #eee',
              paddingTop: '25px',
              display: 'flex',
              justifyContent: 'flex-end',
              gap: '15px'
            }}>
              {selectedBooking.status === 'PENDING' && (
                <ActionButton 
                  confirm
                  disabled={updating}
                  onClick={() => handleStatusChange(selectedBooking.id, 'CONFIRMED')}
                  style={{ 
                    padding: '12px 20px', 
                    borderRadius: '10px',
                    boxShadow: '0 4px 10px rgba(76, 175, 80, 0.2)'
                  }}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  Confirm Booking
                </ActionButton>
              )}
              
              {selectedBooking.status === 'CONFIRMED' && (
                <ActionButton 
                  complete
                  disabled={updating}
                  onClick={() => handleStatusChange(selectedBooking.id, 'COMPLETED')}
                  style={{ 
                    padding: '12px 20px', 
                    borderRadius: '10px',
                    boxShadow: '0 4px 10px rgba(33, 150, 243, 0.2)'
                  }}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                  </svg>
                  Complete Booking
                </ActionButton>
              )}
              
              {(selectedBooking.status === 'PENDING' || selectedBooking.status === 'CONFIRMED') && (
                <ActionButton 
                  cancel
                  disabled={updating}
                  onClick={() => handleStatusChange(selectedBooking.id, 'CANCELLED')}
                  style={{ 
                    padding: '12px 20px', 
                    borderRadius: '10px',
                    boxShadow: '0 4px 10px rgba(211, 47, 47, 0.2)'
                  }}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                  Cancel Booking
                </ActionButton>
              )}
              
              <button
                onClick={() => setShowDetailsModal(false)}
                style={{
                  padding: '12px 20px',
                  border: '1px solid #ddd',
                  borderRadius: '10px',
                  background: 'white',
                  color: colors.darkGray,
                  cursor: 'pointer',
                  fontWeight: '500',
                  fontSize: '15px',
                  transition: 'all 0.2s',
                  boxShadow: '0 2px 5px rgba(0, 0, 0, 0.05)'
                }}
                onMouseOver={(e) => {
                  e.currentTarget.style.backgroundColor = '#f5f5f5';
                }}
                onMouseOut={(e) => {
                  e.currentTarget.style.backgroundColor = 'white';
                }}
              >
                Close
              </button>
            </div>
          </div>
        </ModalWrapper>
      )}

      {/* Notification */}
      {notification && (
        <Notification type={notification.type}>
          {notification.type === 'success' ? (
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
          ) : (
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
          )}
          <span>{notification.message}</span>
        </Notification>
      )}
    </Container>
  );
};

// Custom calendar toolbar component
const CalendarToolbar = (toolbar) => {
  const goToBack = () => {
    toolbar.onNavigate('PREV');
  };
  
  const goToNext = () => {
    toolbar.onNavigate('NEXT');
  };
  
  const goToCurrent = () => {
    toolbar.onNavigate('TODAY');
  };

  const label = () => {
    const date = toolbar.date;
    return format(date, 'MMMM yyyy');
  };

  const viewButtons = () => {
    const views = toolbar.views;
    const view = toolbar.view;
    
    return (
      <div style={{ display: 'flex', gap: '8px' }}>
        {views.map(name => (
          <button
            key={name}
            type="button"
            onClick={() => toolbar.onView(name)}
            style={{
              padding: '8px 12px',
              backgroundColor: view === name ? colors.pineGreen : 'white',
              color: view === name ? 'white' : colors.darkGray,
              border: `1px solid ${view === name ? colors.pineGreen : '#ddd'}`,
              borderRadius: '6px',
              cursor: 'pointer',
              fontWeight: view === name ? '600' : '400',
              fontSize: '13px'
            }}
          >
            {name.charAt(0).toUpperCase() + name.slice(1)}
          </button>
        ))}
      </div>
    );
  };

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'space-between', 
      alignItems: 'center',
      marginBottom: '25px',
      flexWrap: 'wrap',
      gap: '15px'
    }}>
      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
        <ViewToggleButton 
          onClick={goToCurrent}
          style={{ padding: '8px 15px', fontSize: '13px' }}
        >
          Today
        </ViewToggleButton>
        <div style={{ display: 'flex', gap: '8px' }}>
          <ViewToggleButton 
            onClick={goToBack}
            style={{ padding: '8px 12px', minWidth: '40px' }}
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
          </ViewToggleButton>
          <ViewToggleButton 
            onClick={goToNext}
            style={{ padding: '8px 12px', minWidth: '40px' }}
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </ViewToggleButton>
        </div>
      </div>
      
      <div style={{ 
        fontSize: '20px', 
        fontWeight: '600',
        color: colors.darkGray,
        padding: '0 15px'
      }}>
        {label()}
      </div>
      
      {viewButtons()}
    </div>
  );
};

// Event tooltip component
const EventTooltip = ({ event }) => {
  return (
    <div>
      <strong>{event.title}</strong>
      <p>{event.booking.isHourly ? 'Hourly Rental' : 'Daily Rental'}</p>
    </div>
  );
};

export default BookingManagement; 