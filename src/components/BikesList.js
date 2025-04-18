// src/components/BikesList.js
import React, { useEffect, useState } from 'react';
import { getBikes, deleteBike, updateBikesWithHardwareIds, toggleBikeLock, subscribeToBikes } from '../services/bikeService';
import BikeQRCode from './BikeQRCode';
import styled from 'styled-components';
import { QRCodeSVG } from 'qrcode.react';
import { useNavigate } from 'react-router-dom';

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
  margin-bottom: 20px;
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
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 5px;
  
  &:hover {
    opacity: 0.9;
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
  const [bikes, setBikes] = useState([]);
  const [filteredBikes, setFilteredBikes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');
  const [availabilityFilter, setAvailabilityFilter] = useState('all');
  const [selectedBike, setSelectedBike] = useState(null);
  const [showQRModal, setShowQRModal] = useState(false);
  const [generatingIds, setGeneratingIds] = useState(false);
  const [processingBikeAction, setProcessingBikeAction] = useState(null);
  const [lastUpdateTime, setLastUpdateTime] = useState(new Date());
  const [showUpdateIndicator, setShowUpdateIndicator] = useState(false);
  const navigate = useNavigate();

  // Set up real-time listener for bikes
  useEffect(() => {
    setLoading(true);
    
    // Initial data fetch
    const initialFetch = async () => {
      try {
        const bikeData = await getBikes();
        setBikes(bikeData);
        setFilteredBikes(bikeData);
        setLastUpdateTime(new Date());
      } catch (err) {
        setError('Failed to fetch bikes: ' + err.message);
      } finally {
        setLoading(false);
      }
    };
    
    initialFetch();
    
    // Set up real-time listener
    const unsubscribe = subscribeToBikes((updatedBikes) => {
      setBikes(updatedBikes);
      setLastUpdateTime(new Date());
      setShowUpdateIndicator(true);
      
      // Hide update indicator after 3 seconds
      setTimeout(() => {
        setShowUpdateIndicator(false);
      }, 3000);
    });
    
    // Cleanup listener on component unmount
    return () => {
      unsubscribe();
    };
  }, []);

  // Apply filters whenever bikes, searchTerm, or filters change
  useEffect(() => {
    // Apply filters
    const filtered = bikes.filter(bike => {
      const matchesSearch = 
        searchTerm === '' || 
        bike.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        bike.type?.toLowerCase().includes(searchTerm.toLowerCase()) ||
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
  }, [searchTerm, typeFilter, availabilityFilter, bikes]);

  const handleDelete = async (bikeId) => {
    if (window.confirm('Are you sure you want to delete this bike?')) {
      try {
        await deleteBike(bikeId);
        // The real-time listener will update the list automatically
      } catch (err) {
        alert('Error deleting bike: ' + err.message);
      }
    }
  };
  
  const handleEdit = (bike) => {
    if (onEditBike) {
      onEditBike(bike);
    }
  };

  const handleShowQR = (bike) => {
    setSelectedBike(bike);
    setShowQRModal(true);
  };

  const handleCloseQR = () => {
    setShowQRModal(false);
    setSelectedBike(null);
  };

  const handleGenerateHardwareIds = async () => {
    try {
      setGeneratingIds(true);
      const updatedBikes = await updateBikesWithHardwareIds();
      setBikes(updatedBikes);
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
      
      // Immediately refresh the bike data
      const updatedBikes = await getBikes();
      setBikes(updatedBikes);
      
      // Show confirmation message
      alert(`Bike ${bike.name} has been ${newLockState ? 'locked' : 'unlocked'}.${!newLockState && !bike.isInUse ? ' The bike is now unavailable.' : ''}`);
      
      // Get the updated bike
      const updatedBike = updatedBikes.find(b => b.id === bike.id);
      
      console.log(`After toggle - Bike ${bike.id}:`, {
        isLocked: updatedBike.isLocked,
        isAvailable: updatedBike.isAvailable,
        isInUse: updatedBike.isInUse
      });
      
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

  // Identify unique bike types for filter dropdown
  const bikeTypes = [...new Set(bikes.map(bike => bike.type))];

  if (loading) return <LoadingMessage>Loading bikes...</LoadingMessage>;
  if (error) return <ErrorMessage>{error}</ErrorMessage>;

  return (
    <Container>
      <Title>Manage Bikes</Title>
      
      {showUpdateIndicator && (
        <RefreshIndicator>
          <span>🔄</span> Bike data updated
        </RefreshIndicator>
      )}
      
      <LastUpdateTime>
        Last updated: {lastUpdateTime.toLocaleTimeString()}
      </LastUpdateTime>
      
      <ActionBar>
        <SearchContainer>
          <SearchInput
            type="text"
            placeholder="Search bikes by name, type, or hardware ID..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
          
          <Select 
            value={typeFilter} 
            onChange={e => setTypeFilter(e.target.value)}
          >
            <option value="all">All Types</option>
            {bikeTypes.map(type => (
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
                <TableHeader>Hardware ID</TableHeader>
                <TableHeader>Status</TableHeader>
                <TableHeader>QR Code</TableHeader>
                <TableHeader>Actions</TableHeader>
              </tr>
            </thead>
            <tbody>
              {filteredBikes.map((bike) => (
                <TableRow key={bike.id}>
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
                    <HardwareIdCell>
                      {bike.hardwareId ? (
                        <>
                          <span>{bike.hardwareId}</span>
                          <SmallQRCode onClick={() => handleShowQR(bike)}>
                            <QRCodeSVG 
                              value={JSON.stringify({bikeId: bike.id, hardwareId: bike.hardwareId})}
                              size={32}
                              bgColor={colors.white}
                              fgColor={colors.pineGreen}
                            />
                          </SmallQRCode>
                        </>
                      ) : (
                        "Not assigned"
                      )}
                    </HardwareIdCell>
                  </TableCell>
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
                  <TableCell>
                    <QRCodeIcon onClick={() => handleShowQR(bike)}>
                      <span role="img" aria-label="QR Code">📱</span>
                    </QRCodeIcon>
                  </TableCell>
                  <TableCell>
                    <ButtonGroup>
                      {!bike.isInUse && (
                        <Button 
                          locked={bike.isLocked}
                          onClick={() => handleToggleLock(bike)}
                          disabled={processingBikeAction === bike.id}
                        >
                          {processingBikeAction === bike.id
                            ? 'Processing...'
                            : bike.isLocked
                              ? 'Unlock'
                              : 'Lock'}
                        </Button>
                      )}
                      
                      {bike.isAvailable && bike.isLocked && (
                        <Button 
                          success 
                          onClick={() => handleStartRide(bike)}
                        >
                          Start Ride
                        </Button>
                      )}
                      
                      <Button edit onClick={() => handleEdit(bike)}>Edit</Button>
                      <Button danger onClick={() => handleDelete(bike.id)}>Delete</Button>
                    </ButtonGroup>
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
    </Container>
  );
};

export default BikesList;