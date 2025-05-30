// src/components/BikesList.js
import React, { useEffect, useState, useMemo } from 'react';
import { getBikes, deleteBike, updateBikesWithHardwareIds, toggleBikeLock, subscribeToBikes } from '../services/bikeService';
import BikeQRCode from './BikeQRCode';
import BikeDetailsDialog from './BikeDetailsDialog';
import AddBike from './AddBike';
import styled from 'styled-components';
import { QRCodeSVG } from 'qrcode.react';
import { useNavigate } from 'react-router-dom';
import { useDataContext } from '../context/DataContext';

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
  warning: '#FFC107'
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
  background-color: ${props => 
    props.isInUse ? '#ffebee' : 
    props.isAvailable ? '#e8f5e9' : 
    '#fff3cd'};
  color: ${props => 
    props.isInUse ? '#b71c1c' : 
    props.isAvailable ? '#2e7d32' : 
    '#856404'};
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

const DebugInfo = styled.pre`
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  margin-top: 10px;
  white-space: pre-wrap;
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

const BikesList = ({ onEditBike }) => {
  // Get data from context instead of component state
  const { 
    bikes, 
    loading: contextLoading, 
    lastUpdateTime, 
    showUpdateIndicator,
    bikeTypes 
  } = useDataContext();
  
  // Keep local component state for UI elements
  const [filteredBikes, setFilteredBikes] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');
  const [availabilityFilter, setAvailabilityFilter] = useState('all');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);
  const [bikeToDelete, setBikeToDelete] = useState(null);
  const [selectedBike, setSelectedBike] = useState(null);
  const [showBikeDetails, setShowBikeDetails] = useState(false);
  const [availableTypes, setAvailableTypes] = useState([]);
  const [showQRModal, setShowQRModal] = useState(false);
  const [showBikeDetailsDialog, setShowBikeDetailsDialog] = useState(false);
  const [generatingIds, setGeneratingIds] = useState(false);
  const [processingBikeAction, setProcessingBikeAction] = useState(null);
  const [showAddBikeModal, setShowAddBikeModal] = useState(false);
  const navigate = useNavigate();

  // Update available types from context
  useEffect(() => {
    if (bikeTypes && bikeTypes.length > 0) {
      setAvailableTypes(bikeTypes);
    }
  }, [bikeTypes]);

  // Apply filters whenever bikes, searchTerm, or filters change
  useEffect(() => {
    setLoading(true);
    
    // Apply filters
    const filtered = (bikes || []).filter(bike => {
      const matchesSearch = 
        searchTerm === '' || 
        bike.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        bike.type?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (bike.qrCode && bike.qrCode.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (bike.hardwareId && bike.hardwareId.toLowerCase().includes(searchTerm.toLowerCase()));
        
      const matchesType = 
        typeFilter === 'all' || 
        bike.type?.toLowerCase() === typeFilter.toLowerCase();
        
      const matchesAvailability = 
        availabilityFilter === 'all' || 
        (availabilityFilter === 'available' && bike.isAvailable) ||
        (availabilityFilter === 'unavailable' && !bike.isAvailable);
        
      return matchesSearch && matchesType && matchesAvailability;
    });
    
    setFilteredBikes(filtered);
    setLoading(false);
  }, [searchTerm, typeFilter, availabilityFilter, bikes]);

  const handleDelete = async (bikeId) => {
    try {
      await deleteBike(bikeId);
      // The real-time listener will update the list automatically
    } catch (err) {
      alert('Error deleting bike: ' + err.message);
    }
  };
  
  const handleEdit = (bike) => {
    if (onEditBike) {
      onEditBike(bike);
    }
    setShowBikeDetailsDialog(false);
  };

  const handleShowQR = (bike, e) => {
    if (e) e.stopPropagation(); // Prevent row click event
    setSelectedBike(bike);
    setShowQRModal(true);
  };

  const handleCloseQR = () => {
    setShowQRModal(false);
    setSelectedBike(null);
  };

  const handleRowClick = (bike) => {
    setSelectedBike(bike);
    setShowBikeDetailsDialog(true);
  };

  const handleCloseDetailsDialog = () => {
    setShowBikeDetailsDialog(false);
    setSelectedBike(null);
  };

  const handleGenerateHardwareIds = async () => {
    try {
      setGeneratingIds(true);
      const updatedBikes = await updateBikesWithHardwareIds();
      // No need to call setBikes since data context will update automatically
      alert("Hardware IDs have been generated for all bikes!");
    } catch (error) {
      alert("Error generating hardware IDs: " + error.message);
    } finally {
      setGeneratingIds(false);
    }
  };

  // Enhanced version of handleToggleLock with debug info
  const handleToggleLock = async (bike) => {
    try {
      setProcessingBikeAction(bike.id);
      const newLockState = !bike.isLocked;
      
      // Prevent locking a bike that's in use
      if (newLockState && bike.isInUse) {
        alert("Cannot lock a bike that is currently in use.");
        setProcessingBikeAction(null);
        return;
      }
      
      // Warn user if they're unlocking a bike that's not in use
      if (!newLockState && !bike.isInUse) {
        if (!window.confirm("Unlocking this bike will mark it as UNAVAILABLE. Do you want to continue?")) {
          setProcessingBikeAction(null);
          return;
        }
      }
      
      console.log(`Before toggle - Bike ${bike.id}:`, {
        isLocked: bike.isLocked,
        isAvailable: bike.isAvailable,
        isInUse: bike.isInUse
      });
      
      await toggleBikeLock(bike.id, newLockState);
      
      // No need to fetch bikes manually - context will update
      // Only show confirmation message
      alert(`Bike ${bike.name} has been ${newLockState ? 'locked' : 'unlocked'}.${!newLockState && !bike.isInUse ? ' The bike is now unavailable.' : ''}`);
      
      // Log the change for debugging
      console.log(`After toggle - Requested lock state: ${newLockState}`);
      
    } catch (error) {
      console.error('Error toggling bike lock:', error);
      alert(`Error toggling bike lock: ${error.message}`);
    } finally {
      setProcessingBikeAction(null);
    }
  };

  const handleStartRide = (bike) => {
    navigate(`/ride/${bike.id}`);
  };

  const handleOpenAddBike = () => {
    setShowAddBikeModal(true);
  };

  const handleCloseAddBike = () => {
    setShowAddBikeModal(false);
  };

  const handleAddBikeSuccess = () => {
    setShowAddBikeModal(false);
    // The DataContext will automatically update the bikes list
  };

  // Show loading state from context
  if (contextLoading && !bikes) {
    return <LoadingMessage>Loading bikes...</LoadingMessage>;
  }

  return (
    <Container>
      <Title>Manage Bikes</Title>
      
      {showUpdateIndicator && (
        <RefreshIndicator>
          <span>🔄</span> Data updated
        </RefreshIndicator>
      )}
      
      <LastUpdateTime>
        Last updated: {lastUpdateTime.toLocaleTimeString()}
      </LastUpdateTime>
      
      <ActionBar>
        <AddNewBikeButton onClick={handleOpenAddBike}>
          <span>+</span>
          Add New Bike
        </AddNewBikeButton>
        <SearchContainer>
          <SearchInput
            type="text"
            placeholder="Search bikes by name, type, QR code, or hardware ID..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
          
          <Select 
            value={typeFilter} 
            onChange={e => setTypeFilter(e.target.value)}
          >
            <option value="all">All Types</option>
            {availableTypes.map(type => (
              <option key={type} value={type.toLowerCase()}>
                {type}
              </option>
            ))}
          </Select>
          
          <Select 
            value={availabilityFilter} 
            onChange={e => setAvailabilityFilter(e.target.value)}
          >
            <option value="all">All Status</option>
            <option value="available">Available</option>
            <option value="unavailable">Not Available</option>
          </Select>

          <RefreshButton onClick={handleGenerateHardwareIds} disabled={generatingIds}>
            {generatingIds ? 'Generating...' : 'Generate Missing Hardware IDs'}
          </RefreshButton>
        </SearchContainer>
      </ActionBar>
      
      {filteredBikes.length === 0 ? (
        <p>No bikes available. Add some bikes to get started.</p>
      ) : (
        <TableContainer>
          <Table>
            <thead>
              <tr>
                <TableHeader>Image</TableHeader>
                <TableHeader>Name</TableHeader>
                <TableHeader>Type</TableHeader>
                <TableHeader>Price</TableHeader>
                <TableHeader>Status</TableHeader>
              </tr>
            </thead>
            <tbody>
              {filteredBikes.map((bike) => (
                <TableRow key={bike.id} onClick={() => handleRowClick(bike)}>
                  <TableCell>
                    <img 
                      src={bike.imageUrl} 
                      alt={bike.name} 
                      style={{ width: '50px', height: '50px', borderRadius: '4px', objectFit: 'cover' }} 
                    />
                  </TableCell>
                  <TableCell>{bike.name}</TableCell>
                  <TableCell>{bike.type}</TableCell>
                  <TableCell>${parseFloat(bike.priceValue || 0).toFixed(2)}</TableCell>
                  <TableCell>
                    <StatusContainer>
                      <StatusBadge 
                        isInUse={bike.isInUse}
                        isAvailable={bike.isAvailable} 
                        isLocked={bike.isLocked}
                      >
                        {bike.isInUse 
                          ? 'In Use' 
                          : bike.isAvailable
                            ? 'Available' 
                            : 'Unavailable'}
                      </StatusBadge>
                      {(bike.isAvailable || bike.isInUse) && (
                        <LockStatus locked={bike.isLocked}>
                          {bike.isLocked ? 'Locked' : 'Unlocked'}
                        </LockStatus>
                      )}
                    </StatusContainer>
                  </TableCell>
                </TableRow>
              ))}
            </tbody>
          </Table>
        </TableContainer>
      )}

      {showQRModal && selectedBike && (
        <Modal onClick={handleCloseQR}>
          <ModalContent onClick={e => e.stopPropagation()}>
            <CloseButton onClick={handleCloseQR}>×</CloseButton>
            <BikeQRCode bike={selectedBike} />
          </ModalContent>
        </Modal>
      )}

      {showBikeDetailsDialog && selectedBike && (
        <BikeDetailsDialog
          bike={selectedBike}
          onClose={handleCloseDetailsDialog}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onToggleLock={handleToggleLock}
          onStartRide={handleStartRide}
          processingBikeAction={processingBikeAction}
        />
      )}

      {showAddBikeModal && (
        <AddBikeModal onClick={handleCloseAddBike}>
          <AddBikeModalContent onClick={e => e.stopPropagation()}>
            <ModalCloseButton onClick={handleCloseAddBike}>×</ModalCloseButton>
            <AddBike onSuccess={handleAddBikeSuccess} />
          </AddBikeModalContent>
        </AddBikeModal>
      )}
    </Container>
  );
};

export default BikesList;