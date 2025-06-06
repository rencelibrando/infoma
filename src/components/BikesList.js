// src/components/BikesList.js
import React, { useEffect, useState, useMemo } from 'react';
import { getBikes, deleteBike as deleteBikeService, updateBikesWithHardwareIds, toggleBikeLock, subscribeToBikes } from '../services/bikeService';
import BikeQRCode from './BikeQRCode';
import BikeDetailsDialog from './BikeDetailsDialog';
import AddBike from './AddBike';
import styled from 'styled-components';
import QRCode from 'qrcode';
import { useNavigate } from 'react-router-dom';
import { useDataContext } from '../context/DataContext';
import { FiPlus, FiSearch, FiFilter, FiChevronLeft, FiChevronRight, FiEdit2, FiTrash2, FiEye, FiPrinter } from 'react-icons/fi';

// Pine green and gray theme colors
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
  lightRed: '#ffebee'
};

const Container = styled.div`
  padding: 20px;
`;

const Title = styled.h2`
  margin-bottom: 20px;
  color: ${colors.darkGray};
`;

const TableContainer = styled.div`
  overflow-x: auto;
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
`;

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  background-color: ${colors.white};
`;

const TableHeader = styled.th`
  text-align: left;
  padding: 12px;
  background-color: ${colors.lightGray};
  border-bottom: 1px solid #ddd;
  color: ${colors.darkGray};
`;

const TableRow = styled.tr`
  &:nth-child(even) {
    background-color: ${colors.lightGray};
  }
  &:hover {
    background-color: rgba(29, 60, 52, 0.05);
    cursor: pointer;
  }
`;

const TableCell = styled.td`
  padding: 12px;
  border-bottom: 1px solid #eee;
  color: ${colors.darkGray};
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
`;

const Button = styled.button`
  padding: 8px 12px;
  margin-right: 5px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  background-color: ${props => 
    props.danger ? colors.danger : 
    props.edit ? colors.lightPineGreen : 
    props.success ? colors.success :
    props.locked ? colors.warning : colors.pineGreen};
  color: white;
  transition: all 0.3s ease;
  font-size: 12px;
  
  &:hover {
    opacity: 0.85;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const LoadingMessage = styled.div`
  text-align: center;
  padding: 40px;
  color: ${colors.mediumGray};
`;

const ErrorMessage = styled.div`
  text-align: center;
  padding: 20px;
  color: ${colors.danger};
  background-color: #ffebee;
  border-radius: 4px;
  margin: 20px 0;
`;

const SearchContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
  min-width: 0;
  
  @media (max-width: 768px) {
    flex-wrap: wrap;
    gap: 10px;
  }
  
  @media (max-width: 576px) {
    flex-direction: column;
    align-items: stretch;
  }
`;

const SearchInput = styled.input`
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  flex: 1;
  font-size: 14px;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 2px rgba(29, 60, 52, 0.1);
  }
`;

const Select = styled.select`
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: ${colors.white};
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
`;

const StatusContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

const StatusBadge = styled.span`
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  background-color: ${props => {
    if (props.isInUse) return '#ffebee';
    if (props.isAvailable) return '#e8f5e9';
    return '#fff3cd';
  }};
  color: ${props => {
    if (props.isInUse) return '#b71c1c';
    if (props.isAvailable) return '#2e7d32';
    return '#856404';
  }};
`;

const LockStatus = styled.span`
  font-size: 12px;
  color: ${props => props.locked ? colors.warning : colors.mediumGray};
  display: flex;
  align-items: center;
  gap: 5px;
  
  &:before {
    content: "${props => props.locked ? '🔒' : '🔓'}";
  }
`;

const ActionBar = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 20px;
  flex-wrap: wrap;
  
  @media (max-width: 768px) {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
`;

const QRCodeIcon = styled.div`
  margin-right: 5px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: ${colors.pineGreen};
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &:hover {
    transform: scale(1.1);
  }
`;

const Modal = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

const ModalContent = styled.div`
  background-color: ${colors.white};
  padding: 20px;
  border-radius: 8px;
  position: relative;
  max-width: 90%;
  max-height: 90%;
  overflow: auto;
`;

const CloseButton = styled.button`
  position: absolute;
  top: 10px;
  right: 10px;
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: ${colors.darkGray};
  
  &:hover {
    color: ${colors.danger};
  }
`;

const SmallQRCode = styled.div`
  display: inline-block;
  cursor: pointer;
  transition: transform 0.2s ease;
  
  &:hover {
    transform: scale(1.1);
  }
`;

const HardwareIdCell = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
`;

const RefreshButton = styled.button`
  padding: 10px 15px;
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
  flex-shrink: 0;
  
  &:hover {
    opacity: 0.9;
  }
  
  @media (max-width: 576px) {
    justify-content: center;
  }
`;

const AddNewBikeButton = styled.button`
  padding: 12px 20px;
  background-color: ${colors.success};
  color: ${colors.white};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
  white-space: nowrap;
  flex-shrink: 0;
  
  &:hover {
    background-color: #45a049;
    transform: translateY(-1px);
  }
  
  @media (max-width: 768px) {
    justify-content: center;
  }
`;

const AddBikeModal = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
`;

const AddBikeModalContent = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  position: relative;
  max-width: 800px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
`;

const ModalCloseButton = styled.button`
  position: absolute;
  top: 15px;
  right: 15px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: ${colors.darkGray};
  z-index: 1001;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
  
  &:hover {
    background-color: ${colors.lightGray};
    color: ${colors.danger};
  }
`;

const RefreshIndicator = styled.div`
  position: fixed;
  top: 10px;
  right: 10px;
  background-color: ${colors.pineGreen};
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  z-index: 1000;
  animation: fadeOut 2s forwards;
  animation-delay: 1s;
  
  @keyframes fadeOut {
    to {
      opacity: 0;
      visibility: hidden;
    }
  }
`;

const LastUpdateTime = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  text-align: right;
  margin-bottom: 5px;
`;

const Header = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
`;

const AddButton = styled.button`
  padding: 8px 12px;
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: all 0.3s ease;
  
  &:hover {
    opacity: 0.9;
  }
`;

const StatsContainer = styled.div`
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
`;

const StatCard = styled.div`
  background-color: ${colors.white};
  padding: 10px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  flex: 1;
`;

const StatValue = styled.div`
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 5px;
`;

const StatLabel = styled.div`
  font-size: 14px;
  color: ${colors.mediumGray};
`;

const ControlsContainer = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
`;

const FilterContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
`;

const FilterButton = styled.button`
  padding: 8px 12px;
  background-color: ${props => props.active ? colors.pineGreen : colors.lightGray};
  color: ${props => props.active ? colors.white : colors.darkGray};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    opacity: 0.9;
  }
`;

const FilterCount = styled.span`
  padding: 2px 4px;
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  border-radius: 4px;
  font-size: 12px;
`;

const SortSelect = styled.select`
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: ${colors.white};
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
`;

const FilterPanel = styled.div`
  background-color: ${colors.white};
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
`;

const FilterSection = styled.div`
  margin-bottom: 20px;
`;

const FilterTitle = styled.h3`
  margin-bottom: 10px;
  color: ${colors.darkGray};
`;

const TypeFilters = styled.div`
  display: flex;
  gap: 10px;
`;

const TypeFilter = styled.button`
  padding: 8px 12px;
  background-color: ${props => props.active ? colors.pineGreen : colors.lightGray};
  color: ${props => props.active ? colors.white : colors.darkGray};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    opacity: 0.9;
  }
`;

const ResultsInfo = styled.div`
  margin-bottom: 20px;
  color: ${colors.mediumGray};
`;

const NoDataContainer = styled.div`
  text-align: center;
  padding: 40px;
  color: ${colors.mediumGray};
`;

const BikesGrid = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
`;

const BikeCard = styled.div`
  background-color: ${colors.white};
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  flex: 1;
  min-width: 250px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  }
`;

const BikeCardHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
`;

const BikeCardTitle = styled.h3`
  font-size: 18px;
  font-weight: 500;
  color: ${colors.darkGray};
`;

const BikeActions = styled.div`
  display: flex;
  gap: 5px;
`;

const ActionButton = styled.button`
  padding: 5px 8px;
  background-color: transparent;
  color: ${props => props.danger ? colors.danger : colors.pineGreen};
  border: 1px solid ${props => props.danger ? colors.danger : colors.pineGreen};
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 5px;
  
  &:hover {
    background-color: ${props => props.danger ? colors.danger : colors.pineGreen};
    color: ${colors.white};
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const DeleteButton = styled.button`
  background-color: transparent;
  color: ${colors.danger};
  border: none;
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
  transition: all 0.3s ease;
  
  &:hover {
    background-color: ${colors.lightGray};
    color: ${colors.danger};
  }
`;

const BikeImage = styled.img`
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
  margin-bottom: 10px;
`;

const BikeInfo = styled.div`
  margin-top: 10px;
`;

const InfoRow = styled.div`
  margin-bottom: 5px;
`;

const InfoLabel = styled.span`
  font-size: 14px;
  font-weight: 500;
  color: ${colors.darkGray};
`;

const InfoValue = styled.span`
  font-size: 14px;
  color: ${colors.mediumGray};
`;

const PaginationContainer = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20px;
`;

const PaginationButton = styled.button`
  padding: 8px 12px;
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    opacity: 0.9;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const PageNumbers = styled.div`
  display: flex;
  gap: 5px;
`;

const PageNumber = styled.button`
  padding: 8px 12px;
  background-color: ${props => props.active ? colors.pineGreen : colors.lightGray};
  color: ${props => props.active ? colors.white : colors.darkGray};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    opacity: 0.9;
  }
`;

const DialogOverlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

const DeleteDialog = styled.div`
  background-color: ${colors.white};
  padding: 20px;
  border-radius: 8px;
  max-width: 400px;
  width: 100%;
`;

const DialogHeader = styled.div`
  margin-bottom: 20px;
`;

const DialogTitle = styled.h3`
  font-size: 18px;
  font-weight: 500;
  color: ${colors.darkGray};
`;

const DialogContent = styled.div`
  margin-bottom: 20px;
`;

const DialogActions = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: 10px;
`;

const DialogButton = styled.button`
  padding: 8px 12px;
  background-color: ${props => props.secondary ? colors.lightGray : props.danger ? colors.danger : colors.pineGreen};
  color: ${props => props.secondary ? colors.darkGray : colors.white};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    opacity: 0.9;
  }
`;

const LoadingContainer = styled.div`
  text-align: center;
  padding: 40px;
  color: ${colors.mediumGray};
`;

const ErrorContainer = styled.div`
  text-align: center;
  padding: 20px;
  background-color: ${colors.lightRed};
  border-radius: 8px;
  margin-bottom: 20px;
  
  button {
    margin-top: 10px;
    padding: 8px 16px;
    background-color: ${colors.danger};
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    
    &:hover {
      opacity: 0.9;
    }
  }
`;

const QRCodeModal = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

const QRCodeModalContent = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  position: relative;
  max-width: 90%;
  max-height: 90%;
  overflow: auto;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
`;

const QRCodeCloseButton = styled.button`
  position: absolute;
  top: 15px;
  right: 15px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: ${colors.darkGray};
  z-index: 1001;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
  
  &:hover {
    background-color: ${colors.lightGray};
    color: ${colors.danger};
  }
`;

const BikesList = ({ onEditBike }) => {
  // Get data from context
  const { 
    bikes, 
    loading, 
    error: bikesError
  } = useDataContext();

  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTypes, setSelectedTypes] = useState([]);
  const [sortBy, setSortBy] = useState('newest');
  const [currentPage, setCurrentPage] = useState(1);
  const [bikesPerPage] = useState(10);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [bikeToDelete, setBikeToDelete] = useState(null);
  const [filterOpen, setFilterOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [qrCodeModalOpen, setQrCodeModalOpen] = useState(false);
  const [selectedBikeForQR, setSelectedBikeForQR] = useState(null);
  const [operationError, setOperationError] = useState(null);
  const [addBikeModalOpen, setAddBikeModalOpen] = useState(false);

  const navigate = useNavigate();

  // Get unique bike types for filtering
  const bikeTypes = useMemo(() => {
    if (!bikes || !Array.isArray(bikes)) return [];
    const types = [...new Set(bikes.map(bike => bike.type).filter(Boolean))];
    return types.sort();
  }, [bikes]);

  // Helper function to get bike status based on boolean fields
  const getBikeStatus = (bike) => {
    if (bike.isInUse) {
      return {
        text: 'In Use',
        isInUse: true,
        isAvailable: false
      };
    }
    if (bike.isAvailable) {
      return {
        text: 'Available',
        isInUse: false,
        isAvailable: true
      };
    }
    return {
      text: 'Maintenance',
      isInUse: false,
      isAvailable: false
    };
  };

  // Apply filters to bikes
  const filteredBikes = useMemo(() => {
    if (!bikes || !Array.isArray(bikes)) return [];

    let filtered = bikes.filter(bike => {
      const searchFields = [
        bike.name || '',
        bike.type || '',
        bike.qrCode || '',
        bike.hardwareId || ''
      ].map(field => field.toLowerCase());
      
      const searchMatch = !searchTerm || 
        searchFields.some(field => field.includes(searchTerm.toLowerCase()));
      
      const typeMatch = selectedTypes.length === 0 || 
        selectedTypes.includes(bike.type);
      
      return searchMatch && typeMatch;
    });

    // Sort filtered bikes
    filtered.sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return (a.name || '').localeCompare(b.name || '');
        case 'type':
          return (a.type || '').localeCompare(b.type || '');
        case 'status':
          const statusA = getBikeStatus(a).text;
          const statusB = getBikeStatus(b).text;
          return statusA.localeCompare(statusB);
        case 'newest':
        default:
          return (b.dateAdded || 0) - (a.dateAdded || 0);
      }
    });

    return filtered;
  }, [bikes, searchTerm, selectedTypes, sortBy]);

  // Pagination
  const totalPages = Math.ceil(filteredBikes.length / bikesPerPage);
  const currentBikes = filteredBikes.slice(
    (currentPage - 1) * bikesPerPage,
    currentPage * bikesPerPage
  );

  // Handle page change
  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  // Handle bike type filter
  const handleTypeFilter = (type) => {
    setSelectedTypes(prev => 
      prev.includes(type) 
        ? prev.filter(t => t !== type)
        : [...prev, type]
    );
    setCurrentPage(1); // Reset to first page when filtering
  };

  // Format date for display
  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  // Calculate bike statistics
  const stats = useMemo(() => {
    if (!bikes || !Array.isArray(bikes)) {
      return {
        total: 0,
        available: 0,
        inUse: 0,
        maintenance: 0
      };
    }

    return {
      total: bikes.length,
      available: bikes.filter(bike => bike.isAvailable).length,
      inUse: bikes.filter(bike => bike.isInUse).length,
      maintenance: bikes.filter(bike => !bike.isAvailable && !bike.isInUse).length
    };
  }, [bikes]);

  // Handle bike deletion
  const handleDeleteBike = async () => {
    if (!bikeToDelete) return;
    
    setIsDeleting(true);
    setOperationError(null);
    try {
      await deleteBikeService(bikeToDelete.id);
      setDeleteDialogOpen(false);
      setBikeToDelete(null);
      // Note: The context will automatically update via real-time listeners
    } catch (error) {
      console.error('Error deleting bike:', error);
      setOperationError(error.message || 'Failed to delete bike');
    } finally {
      setIsDeleting(false);
    }
  };

  const openDeleteDialog = (bike) => {
    setBikeToDelete(bike);
    setDeleteDialogOpen(true);
  };

  // Handle bike card click to show QR code
  const handleBikeCardClick = (bike, event) => {
    // Prevent opening QR modal when clicking on action buttons
    if (event.target.closest('button')) {
      return;
    }
    setSelectedBikeForQR(bike);
    setQrCodeModalOpen(true);
  };

  // Handle QR code modal close
  const handleQRModalClose = () => {
    setQrCodeModalOpen(false);
    setSelectedBikeForQR(null);
  };

  // Handle add bike success
  const handleAddBikeSuccess = () => {
    setAddBikeModalOpen(false);
    // The context will automatically update via real-time listeners
  };

  // Handle print QR code
  const handlePrintQRCode = async (bike) => {
    const qrCode = bike.qrCode || bike.hardwareId || 'No QR Code';
    const bikeName = bike.name || 'Unnamed Bike';
    
    try {
      // Generate QR code as high-resolution data URL using the npm package
      const url = await QRCode.toDataURL(qrCode, {
        width: 400, // Higher resolution for better print quality
        height: 400,
        color: {
          dark: '#000000', // Pure black for best print contrast
          light: '#FFFFFF'
        },
        margin: 1, // Minimal margin
        errorCorrectionLevel: 'H' // High error correction for print reliability
      });

      // Create minimal print window with only the QR code
      const printWindow = window.open('', '_blank', 'width=600,height=600');
      const printContent = `
        <!DOCTYPE html>
        <html>
        <head>
          <title>QR Code - ${bikeName}</title>
          <style>
            * {
              margin: 0;
              padding: 0;
              box-sizing: border-box;
            }
            html, body {
              width: 100%;
              height: 100%;
              background: white;
            }
            body {
              display: flex;
              align-items: center;
              justify-content: center;
              font-family: Arial, sans-serif;
            }
            .qr-print-container {
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              min-height: 100vh;
              padding: 20px;
            }
            .qr-image {
              max-width: 100%;
              max-height: 100%;
              width: auto;
              height: auto;
              display: block;
            }
            .bike-label {
              font-size: 12px;
              color: #333;
              margin-top: 10px;
              text-align: center;
              font-weight: normal;
            }
            @media print {
              html, body {
                width: 100%;
                height: 100%;
                margin: 0;
                padding: 0;
                background: white !important;
                -webkit-print-color-adjust: exact;
                color-adjust: exact;
              }
              .qr-print-container {
                min-height: 100vh;
                padding: 0;
                display: flex;
                align-items: center;
                justify-content: center;
              }
              .qr-image {
                max-width: 90vmin;
                max-height: 90vmin;
                width: auto;
                height: auto;
              }
              .bike-label {
                font-size: 10px;
                margin-top: 8px;
              }
              @page {
                margin: 0.5in;
                size: auto;
              }
            }
          </style>
        </head>
        <body>
          <div class="qr-print-container">
            <img src="${url}" alt="QR Code for ${bikeName}" class="qr-image" />
            <div class="bike-label">${bikeName}</div>
          </div>
          <script>
            window.onload = function() {
              setTimeout(() => {
                window.print();
                setTimeout(() => {
                  window.close();
                }, 1000);
              }, 300);
            };
          </script>
        </body>
        </html>
      `;
      
      printWindow.document.write(printContent);
      printWindow.document.close();
    } catch (error) {
      console.error('QR Code generation error:', error);
      alert('Failed to generate QR code for printing. Please try again.');
    }
  };

  // Handle print multiple QR codes
  const handlePrintAllQRCodes = async () => {
    // Show loading message
    const loadingMessage = document.createElement('div');
    loadingMessage.style.cssText = `
      position: fixed;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      z-index: 10000;
      text-align: center;
    `;
    loadingMessage.innerHTML = `
      <div style="margin-bottom: 10px;">Generating QR codes...</div>
      <div style="width: 20px; height: 20px; border: 2px solid #f3f3f3; border-top: 2px solid #1D3C34; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto;"></div>
      <style>@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }</style>
    `;
    document.body.appendChild(loadingMessage);

    try {
      // Generate all QR codes as data URLs using the npm package
      const qrDataPromises = filteredBikes.map(async (bike, index) => {
        const qrCode = bike.qrCode || bike.hardwareId || 'No QR Code';
        const bikeName = bike.name || 'Unnamed Bike';

        try {
          const url = await QRCode.toDataURL(qrCode, {
            width: 300, // Good resolution for multi-page printing
            height: 300,
            color: {
              dark: '#000000',
              light: '#FFFFFF'
            },
            margin: 1,
            errorCorrectionLevel: 'H'
          });

          return {
            bikeName,
            qrCode,
            qrImageUrl: url,
            hasError: false
          };
        } catch (error) {
          console.error('QR Code generation error for bike', index, ':', error);
          return {
            bikeName,
            qrCode,
            qrImageUrl: null,
            hasError: true
          };
        }
      });

      // Wait for all QR codes to be generated
      const qrData = await Promise.all(qrDataPromises);
      
      // Remove loading message
      document.body.removeChild(loadingMessage);

      // Create bike cards HTML - minimal layout with just QR codes
      const bikeCards = qrData.map((data) => {
        if (data.hasError) {
          return `
            <div class="qr-page">
              <div class="error-message">
                <div class="bike-label">${data.bikeName}</div>
                <div class="error-text">QR Code generation failed</div>
                <div class="fallback-code">${data.qrCode}</div>
              </div>
            </div>
          `;
        } else {
          return `
            <div class="qr-page">
              <div class="qr-container">
                <img src="${data.qrImageUrl}" alt="QR Code for ${data.bikeName}" class="qr-image" />
                <div class="bike-label">${data.bikeName}</div>
              </div>
            </div>
          `;
        }
      }).join('');

      // Create and open print window with minimal styling
      const printWindow = window.open('', '_blank');
      const printContent = `
        <!DOCTYPE html>
        <html>
        <head>
          <title>All QR Codes - Bambike Admin</title>
          <style>
            * {
              margin: 0;
              padding: 0;
              box-sizing: border-box;
            }
            html, body {
              width: 100%;
              height: 100%;
              background: white;
              font-family: Arial, sans-serif;
            }
            .qr-page {
              width: 100%;
              min-height: 100vh;
              display: flex;
              align-items: center;
              justify-content: center;
              page-break-after: always;
              padding: 20px;
            }
            .qr-page:last-child {
              page-break-after: avoid;
            }
            .qr-container {
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              text-align: center;
            }
            .qr-image {
              max-width: 100%;
              max-height: 80vh;
              width: auto;
              height: auto;
              display: block;
            }
            .bike-label {
              font-size: 14px;
              color: #333;
              margin-top: 15px;
              font-weight: normal;
            }
            .error-message {
              text-align: center;
              color: #666;
            }
            .error-text {
              font-size: 16px;
              margin: 10px 0;
              color: #d32f2f;
            }
            .fallback-code {
              font-family: 'Courier New', monospace;
              font-size: 12px;
              padding: 10px;
              background: #f5f5f5;
              border: 1px solid #ddd;
              margin-top: 10px;
              word-break: break-all;
            }
            @media print {
              html, body {
                width: 100%;
                height: 100%;
                margin: 0;
                padding: 0;
                background: white !important;
                -webkit-print-color-adjust: exact;
                color-adjust: exact;
              }
              .qr-page {
                padding: 0;
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
              }
              .qr-image {
                max-width: 90vmin;
                max-height: 90vmin;
                width: auto;
                height: auto;
              }
              .bike-label {
                font-size: 12px;
                margin-top: 10px;
              }
              @page {
                margin: 0.5in;
                size: auto;
              }
            }
          </style>
        </head>
        <body>
          ${bikeCards}
          <script>
            window.onload = function() { 
              setTimeout(() => {
                window.print();
                setTimeout(() => {
                  window.close();
                }, 1000);
              }, 500); 
            };
          </script>
        </body>
        </html>
      `;

      printWindow.document.write(printContent);
      printWindow.document.close();

    } catch (error) {
      // Remove loading message if still present
      if (document.body.contains(loadingMessage)) {
        document.body.removeChild(loadingMessage);
      }
      console.error('Error generating QR codes:', error);
      alert('Error generating QR codes. Please try again.');
    }
  };

  return (
    <Container>
      <Header>
        <Title>Bike Management</Title>
        <div style={{ display: 'flex', gap: '10px' }}>
          <RefreshButton onClick={handlePrintAllQRCodes}>
            <FiPrinter size={16} />
            Print All QR Codes
          </RefreshButton>
          <AddButton onClick={() => setAddBikeModalOpen(true)}>
            <FiPlus size={20} />
            Add New Bike
          </AddButton>
        </div>
      </Header>

      {/* Operation Error */}
      {operationError && (
        <ErrorContainer>
          <div>Error: {operationError}</div>
          <button onClick={() => setOperationError(null)}>Dismiss</button>
        </ErrorContainer>
      )}

      {/* Statistics Cards */}
      <StatsContainer>
        <StatCard>
          <StatValue>{stats.total}</StatValue>
          <StatLabel>Total Bikes</StatLabel>
        </StatCard>
        <StatCard available>
          <StatValue>{stats.available}</StatValue>
          <StatLabel>Available</StatLabel>
        </StatCard>
        <StatCard inUse>
          <StatValue>{stats.inUse}</StatValue>
          <StatLabel>In Use</StatLabel>
        </StatCard>
        <StatCard maintenance>
          <StatValue>{stats.maintenance}</StatValue>
          <StatLabel>Maintenance</StatLabel>
        </StatCard>
      </StatsContainer>

      {/* Controls */}
      <ControlsContainer>
        <SearchContainer>
          <FiSearch />
          <SearchInput
            type="text"
            placeholder="Search bikes by name, type, QR code, or hardware ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </SearchContainer>

        <FilterContainer>
          <FilterButton
            active={filterOpen}
            onClick={() => setFilterOpen(!filterOpen)}
          >
            <FiFilter />
            Filters
            {selectedTypes.length > 0 && (
              <FilterCount>{selectedTypes.length}</FilterCount>
            )}
          </FilterButton>

          <SortSelect value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="newest">Newest First</option>
            <option value="name">Name A-Z</option>
            <option value="type">Type</option>
            <option value="status">Status</option>
          </SortSelect>
        </FilterContainer>
      </ControlsContainer>

      {/* Filter Panel */}
      {filterOpen && (
        <FilterPanel>
          <FilterSection>
            <FilterTitle>Bike Types</FilterTitle>
            <TypeFilters>
              {bikeTypes.map(type => (
                <TypeFilter
                  key={type}
                  active={selectedTypes.includes(type)}
                  onClick={() => handleTypeFilter(type)}
                >
                  {type}
                </TypeFilter>
              ))}
            </TypeFilters>
          </FilterSection>
        </FilterPanel>
      )}

      {/* Loading State */}
      {loading && (
        <LoadingContainer>
          <div>Loading bikes...</div>
        </LoadingContainer>
      )}

      {/* Error State */}
      {bikesError && (
        <ErrorContainer>
          <div>Error loading bikes: {bikesError}</div>
          <button onClick={() => {}}>Retry</button>
        </ErrorContainer>
      )}

      {/* Bikes List */}
      {!loading && !bikesError && (
        <>
          <ResultsInfo>
            Showing {currentBikes.length} of {filteredBikes.length} bikes
            {searchTerm && ` matching "${searchTerm}"`}
            {selectedTypes.length > 0 && ` in ${selectedTypes.join(', ')}`}
          </ResultsInfo>

          {currentBikes.length === 0 ? (
            <NoDataContainer>
              <div>No bikes found matching your criteria.</div>
            </NoDataContainer>
          ) : (
            <BikesGrid>
              {currentBikes.map(bike => {
                const bikeStatus = getBikeStatus(bike);
                return (
                  <BikeCard 
                    key={bike.id}
                    onClick={(e) => handleBikeCardClick(bike, e)}
                    title="Click to view QR code"
                  >
                    <BikeCardHeader>
                      <BikeCardTitle>{bike.name || 'Unnamed Bike'}</BikeCardTitle>
                      <BikeActions>
                        <ActionButton
                          onClick={(e) => {
                            e.stopPropagation();
                            handleBikeCardClick(bike, e);
                          }}
                          title="View QR Code"
                        >
                          <FiEye />
                        </ActionButton>
                        <ActionButton
                          onClick={(e) => {
                            e.stopPropagation();
                            handlePrintQRCode(bike);
                          }}
                          title="Print QR Code"
                        >
                          <FiPrinter />
                        </ActionButton>
                        <ActionButton
                          onClick={(e) => {
                            e.stopPropagation();
                            onEditBike ? onEditBike(bike) : console.warn('No edit handler provided');
                          }}
                          title="Edit Bike"
                        >
                          <FiEdit2 />
                        </ActionButton>
                        <ActionButton
                          onClick={(e) => {
                            e.stopPropagation();
                            openDeleteDialog(bike);
                          }}
                          title="Delete Bike"
                          danger
                        >
                          <FiTrash2 />
                        </ActionButton>
                      </BikeActions>
                    </BikeCardHeader>

                    <BikeImage
                      src={bike.imageUrl || 'https://via.placeholder.com/300x200?text=No+Image'}
                      alt={bike.name || 'Bike'}
                      onError={(e) => {
                        e.target.src = 'https://via.placeholder.com/300x200?text=No+Image';
                      }}
                    />

                    <BikeInfo>
                      <InfoRow>
                        <InfoLabel>Type:</InfoLabel>
                        <InfoValue>{bike.type || 'N/A'}</InfoValue>
                      </InfoRow>
                      <InfoRow>
                        <InfoLabel>Status:</InfoLabel>
                        <StatusBadge 
                          isInUse={bikeStatus.isInUse}
                          isAvailable={bikeStatus.isAvailable}
                        >
                          {bikeStatus.text}
                        </StatusBadge>
                      </InfoRow>
                      <InfoRow>
                        <InfoLabel>QR Code:</InfoLabel>
                        <InfoValue>{bike.qrCode || 'N/A'}</InfoValue>
                      </InfoRow>
                      <InfoRow>
                        <InfoLabel>Hardware ID:</InfoLabel>
                        <InfoValue>{bike.hardwareId || 'N/A'}</InfoValue>
                      </InfoRow>
                      <InfoRow>
                        <InfoLabel>Date Added:</InfoLabel>
                        <InfoValue>{formatDate(bike.dateAdded)}</InfoValue>
                      </InfoRow>
                    </BikeInfo>
                  </BikeCard>
                );
              })}
            </BikesGrid>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <PaginationContainer>
              <PaginationButton
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 1}
              >
                <FiChevronLeft />
                Previous
              </PaginationButton>

              <PageNumbers>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                  <PageNumber
                    key={page}
                    active={page === currentPage}
                    onClick={() => handlePageChange(page)}
                  >
                    {page}
                  </PageNumber>
                ))}
              </PageNumbers>

              <PaginationButton
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages}
              >
                Next
                <FiChevronRight />
              </PaginationButton>
            </PaginationContainer>
          )}
        </>
      )}

      {/* QR Code Modal */}
      {qrCodeModalOpen && selectedBikeForQR && (
        <QRCodeModal onClick={handleQRModalClose}>
          <QRCodeModalContent onClick={(e) => e.stopPropagation()}>
            <QRCodeCloseButton onClick={handleQRModalClose}>
              ×
            </QRCodeCloseButton>
            <BikeQRCode bike={selectedBikeForQR} />
          </QRCodeModalContent>
        </QRCodeModal>
      )}

      {/* Delete Confirmation Dialog */}
      {deleteDialogOpen && (
        <DialogOverlay onClick={() => setDeleteDialogOpen(false)}>
          <DeleteDialog onClick={(e) => e.stopPropagation()}>
            <DialogHeader>
              <DialogTitle>Confirm Deletion</DialogTitle>
            </DialogHeader>
            <DialogContent>
              <p>Are you sure you want to delete "{bikeToDelete?.name || 'this bike'}"?</p>
              <p>This action cannot be undone.</p>
            </DialogContent>
            <DialogActions>
              <DialogButton
                secondary
                onClick={() => setDeleteDialogOpen(false)}
                disabled={isDeleting}
              >
                Cancel
              </DialogButton>
              <DialogButton
                danger
                onClick={handleDeleteBike}
                disabled={isDeleting}
              >
                {isDeleting ? 'Deleting...' : 'Delete'}
              </DialogButton>
            </DialogActions>
          </DeleteDialog>
        </DialogOverlay>
      )}

      {/* Add Bike Modal */}
      {addBikeModalOpen && (
        <AddBikeModal onClick={() => setAddBikeModalOpen(false)}>
          <AddBikeModalContent onClick={(e) => e.stopPropagation()}>
            <ModalCloseButton onClick={() => setAddBikeModalOpen(false)}>
              ×
            </ModalCloseButton>
            <AddBike onSuccess={handleAddBikeSuccess} />
          </AddBikeModalContent>
        </AddBikeModal>
      )}
    </Container>
  );
};

export default BikesList;