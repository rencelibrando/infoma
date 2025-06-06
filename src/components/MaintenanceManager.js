import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { updateBikeMaintenanceStatus, addMaintenanceLogEntry, getBikeMaintenanceHistory } from '../services/bikeService';
import { doc, updateDoc } from 'firebase/firestore';
import { db } from '../firebase';

const MaintenanceSection = styled.div`
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  margin-top: 20px;
`;

const MaintenanceTitle = styled.h4`
  margin: 0 0 16px 0;
  color: #495057;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const MaintenanceForm = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

const FormGroup = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

const Label = styled.label`
  font-size: 14px;
  font-weight: 500;
  color: #333;
`;

const Select = styled.select`
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  background-color: white;
  &:focus {
    border-color: #4CAF50;
    outline: none;
  }
`;

const TextArea = styled.textarea`
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-height: 80px;
  resize: vertical;
  &:focus {
    border-color: #4CAF50;
    outline: none;
  }
`;

const CheckboxGroup = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
`;

const Checkbox = styled.input`
  width: 16px;
  height: 16px;
  cursor: pointer;
`;

const CheckboxLabel = styled.label`
  font-size: 14px;
  font-weight: 500;
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const WarningText = styled.div`
  font-size: 12px;
  color: #ef6c00;
  margin-top: 4px;
  font-style: italic;
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 8px;
  margin-top: 12px;
`;

const Button = styled.button`
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
`;

const PrimaryButton = styled(Button)`
  background-color: #4CAF50;
  color: white;
  &:hover {
    background-color: #388E3C;
  }
  &:disabled {
    background-color: #A5D6A7;
    cursor: not-allowed;
  }
`;

const SecondaryButton = styled(Button)`
  background-color: #6c757d;
  color: white;
  &:hover {
    background-color: #5a6268;
  }
`;

const StatusBadge = styled.span`
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  ${props => {
    switch (props.status) {
      case 'operational':
        return 'background-color: #d1edff; color: #0c63e4;';
      case 'maintenance':
        return 'background-color: #fff3e0; color: #ef6c00;';
      case 'repair':
        return 'background-color: #ffebee; color: #d32f2f;';
      case 'out-of-service':
        return 'background-color: #f3e5f5; color: #7b1fa2;';
      default:
        return 'background-color: #f5f5f5; color: #333;';
    }
  }}
`;

const MaintenanceHistory = styled.div`
  margin-top: 16px;
  border-top: 1px solid #e9ecef;
  padding-top: 16px;
`;

const HistoryItem = styled.div`
  padding: 8px 0;
  border-bottom: 1px solid #f1f3f4;
  font-size: 12px;
  color: #6c757d;
`;

const ErrorMessage = styled.div`
  color: #D32F2F;
  margin-top: 8px;
  font-size: 12px;
`;

const SuccessMessage = styled.div`
  color: #388E3C;
  margin-top: 8px;
  font-size: 12px;
`;

const MaintenanceManager = ({ bike, onUpdate }) => {
  const [maintenanceStatus, setMaintenanceStatus] = useState(bike?.maintenanceStatus || 'operational');
  const [maintenanceNotes, setMaintenanceNotes] = useState(bike?.maintenanceNotes || '');
  const [isAvailable, setIsAvailable] = useState(bike?.isAvailable || false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showHistory, setShowHistory] = useState(false);
  const [maintenanceHistory, setMaintenanceHistory] = useState([]);

  useEffect(() => {
    if (bike) {
      setMaintenanceStatus(bike.maintenanceStatus || 'operational');
      setMaintenanceNotes(bike.maintenanceNotes || '');
      setIsAvailable(bike.isAvailable || false);
    }
  }, [bike]);

  const handleStatusChange = async () => {
    if (!bike) return;
    
    setLoading(true);
    setError('');
    setSuccess('');
    
    try {
      const oldStatus = bike.maintenanceStatus || 'operational';
      
      // Update maintenance status
      const updatedBike = await updateBikeMaintenanceStatus(bike.id, {
        status: maintenanceStatus,
        notes: maintenanceNotes
      });
      
      // Add maintenance log entry
      await addMaintenanceLogEntry(bike.id, {
        action: 'status_change',
        oldStatus: oldStatus,
        newStatus: maintenanceStatus,
        notes: maintenanceNotes,
        description: `Status changed from ${oldStatus} to ${maintenanceStatus}`
      });
      
      setSuccess('Maintenance status updated successfully');
      
      // Notify parent component
      if (onUpdate) {
        onUpdate(updatedBike);
      }
      
    } catch (err) {
      console.error('Error updating maintenance status:', err);
      setError(`Failed to update maintenance status: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleAvailabilityChange = async () => {
    if (!bike) return;
    
    // Warn if trying to make a non-operational bike available
    if (isAvailable && maintenanceStatus !== 'operational') {
      const shouldProceed = window.confirm(
        `This bike has maintenance status: "${maintenanceStatus}"\n\n` +
        'Making it available while not operational may cause conflicts with rentals. ' +
        'Consider setting maintenance status to "operational" first.\n\n' +
        'Do you want to proceed anyway?'
      );
      
      if (!shouldProceed) {
        setIsAvailable(bike.isAvailable || false); // Reset to current state
        return;
      }
    }
    
    setLoading(true);
    setError('');
    setSuccess('');
    
    try {
      // Update bike availability using direct Firestore update
      await updateDoc(doc(db, 'bikes', bike.id), {
        isAvailable: isAvailable,
        lastUpdated: new Date()
      });
      
      setSuccess(`Bike ${isAvailable ? 'marked as available' : 'marked as unavailable'}`);
      
      // Create updated bike object for parent component
      const updatedBike = {
        ...bike,
        isAvailable: isAvailable,
        lastUpdated: new Date()
      };
      
      // Notify parent component
      if (onUpdate) {
        onUpdate(updatedBike);
      }
      
    } catch (err) {
      console.error('Error updating bike availability:', err);
      setError(`Failed to update availability: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const loadMaintenanceHistory = async () => {
    try {
      const history = await getBikeMaintenanceHistory(bike.id);
      setMaintenanceHistory(history);
      setShowHistory(true);
    } catch (err) {
      console.error('Error loading maintenance history:', err);
      setError('Failed to load maintenance history');
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'operational': return '✅';
      case 'maintenance': return '🔧';
      case 'repair': return '🔨';
      case 'out-of-service': return '❌';
      default: return '❓';
    }
  };

  if (!bike) return null;

  return (
    <MaintenanceSection>
      <MaintenanceTitle>
        {getStatusIcon(bike.maintenanceStatus)} Maintenance Management
      </MaintenanceTitle>
      
      <div style={{ marginBottom: '12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '8px' }}>
          <div>
            <strong>Maintenance Status:</strong>{' '}
            <StatusBadge status={bike.maintenanceStatus}>
              {bike.maintenanceStatus || 'operational'}
            </StatusBadge>
          </div>
          <div>
            <strong>Available:</strong>{' '}
            <StatusBadge status={bike.isAvailable ? 'operational' : 'out-of-service'}>
              {bike.isAvailable ? 'YES' : 'NO'}
            </StatusBadge>
          </div>
        </div>
        {bike.maintenanceLastUpdated && (
          <div style={{ fontSize: '12px', color: '#6c757d' }}>
            Last updated: {new Date(bike.maintenanceLastUpdated.seconds ? 
              bike.maintenanceLastUpdated.seconds * 1000 : 
              bike.maintenanceLastUpdated).toLocaleString()}
          </div>
        )}
      </div>

      <MaintenanceForm>
        <FormGroup>
          <Label>Update Status:</Label>
          <Select
            value={maintenanceStatus}
            onChange={(e) => setMaintenanceStatus(e.target.value)}
            disabled={loading}
          >
            <option value="operational">✅ Operational</option>
            <option value="maintenance">🔧 Under Maintenance</option>
            <option value="repair">🔨 Needs Repair</option>
            <option value="out-of-service">❌ Out of Service</option>
          </Select>
        </FormGroup>

        <FormGroup>
          <Label>Maintenance Notes:</Label>
          <TextArea
            value={maintenanceNotes}
            onChange={(e) => setMaintenanceNotes(e.target.value)}
            placeholder="Enter maintenance notes, issues, or comments..."
            disabled={loading}
          />
        </FormGroup>

        <FormGroup>
          <Label>Bike Availability:</Label>
          <CheckboxGroup>
            <CheckboxLabel>
              <Checkbox
                type="checkbox"
                checked={isAvailable}
                onChange={(e) => setIsAvailable(e.target.checked)}
                disabled={loading}
              />
              Available for Rent
            </CheckboxLabel>
          </CheckboxGroup>
          {isAvailable && maintenanceStatus !== 'operational' && (
            <WarningText>
              ⚠️ Warning: This bike is marked as available but has maintenance status "{maintenanceStatus}". 
              This may cause conflicts with the rental system.
            </WarningText>
          )}
          {!isAvailable && maintenanceStatus === 'operational' && (
            <WarningText>
              ℹ️ This bike is operational but marked as unavailable. It won't appear in the rental system.
            </WarningText>
          )}
        </FormGroup>

        <ButtonGroup>
          <PrimaryButton 
            onClick={handleStatusChange}
            disabled={loading || maintenanceStatus === bike.maintenanceStatus}
          >
            {loading ? 'Updating...' : 'Update Status'}
          </PrimaryButton>
          
          <PrimaryButton 
            onClick={handleAvailabilityChange}
            disabled={loading || isAvailable === bike.isAvailable}
          >
            {loading ? 'Updating...' : 'Update Availability'}
          </PrimaryButton>
          
          <SecondaryButton onClick={loadMaintenanceHistory}>
            View History
          </SecondaryButton>
        </ButtonGroup>

        {error && <ErrorMessage>{error}</ErrorMessage>}
        {success && <SuccessMessage>{success}</SuccessMessage>}
      </MaintenanceForm>

      {showHistory && (
        <MaintenanceHistory>
          <h5>Maintenance History</h5>
          {maintenanceHistory.length === 0 ? (
            <div style={{ color: '#6c757d', fontStyle: 'italic' }}>No maintenance history available</div>
          ) : (
            maintenanceHistory.map((entry, index) => (
              <HistoryItem key={index}>
                <strong>{entry.action || 'Status Change'}</strong> - {entry.description || `${entry.oldStatus} → ${entry.newStatus}`}
                <br />
                <span>{new Date(entry.timestamp.seconds ? entry.timestamp.seconds * 1000 : entry.timestamp).toLocaleString()}</span>
                {entry.adminEmail && <span> by {entry.adminEmail}</span>}
                {entry.notes && <div style={{ marginTop: '4px', fontStyle: 'italic' }}>{entry.notes}</div>}
              </HistoryItem>
            ))
          )}
        </MaintenanceHistory>
      )}
    </MaintenanceSection>
  );
};

export default MaintenanceManager; 