import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { updateBike, setupAdminUser } from '../services/bikeService';
import { checkQRCodeCollisions, migrateBikesWithMaintenanceStatus } from '../services/migrationService';
import MaintenanceManager from './MaintenanceManager';

// Styled components
const Form = styled.form`
  background-color: white;
  padding: 25px;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 700px;
  margin: 0 auto;
`;

const FormTitle = styled.h2`
  margin-bottom: 24px;
  color: #333;
  font-size: 24px;
  text-align: center;
`;

const FormGroup = styled.div`
  margin-bottom: 20px;
`;

const FormRow = styled.div`
  display: flex;
  gap: 16px;
  
  @media (max-width: 768px) {
    flex-direction: column;
    gap: 12px;
  }
`;

const FormColumn = styled.div`
  flex: 1;
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #555;
`;

const Input = styled.input`
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  &:focus {
    border-color: #4CAF50;
    outline: none;
  }
  
  /* Special styling for coordinate inputs */
  ${props => props.type === 'number' && props.name?.includes('latitude') && `
    &:valid {
      border-color: #4CAF50;
    }
    &:invalid {
      border-color: #f44336;
    }
  `}
  
  ${props => props.type === 'number' && props.name?.includes('longitude') && `
    &:valid {
      border-color: #4CAF50;
    }
    &:invalid {
      border-color: #f44336;
    }
  `}
`;

const Textarea = styled.textarea`
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  min-height: 100px;
  resize: vertical;
  &:focus {
    border-color: #4CAF50;
    outline: none;
  }
`;

const Select = styled.select`
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  &:focus {
    border-color: #4CAF50;
    outline: none;
  }
`;

const ImagePreview = styled.div`
  margin-top: 10px;
  img {
    max-width: 100%;
    max-height: 200px;
    border-radius: 4px;
  }
`;

const ButtonGroup = styled.div`
  display: flex;
  justify-content: space-between;
  margin-top: 30px;
`;

const Button = styled.button`
  padding: 10px 20px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
`;

const PrimaryButton = styled(Button)`
  background-color: #4CAF50;
  color: white;
  border: none;
  &:hover {
    background-color: #388E3C;
  }
  &:disabled {
    background-color: #A5D6A7;
    cursor: not-allowed;
  }
`;

const SecondaryButton = styled(Button)`
  background-color: transparent;
  color: #555;
  border: 1px solid #ddd;
  &:hover {
    background-color: #f5f5f5;
  }
`;

const ErrorMessage = styled.div`
  color: #D32F2F;
  margin-top: 15px;
  font-size: 14px;
  text-align: center;
`;

const SuccessMessage = styled.div`
  color: #388E3C;
  margin-top: 15px;
  font-size: 14px;
  text-align: center;
`;

const ValidationMessage = styled.div`
  padding: 8px 12px;
  border-radius: 4px;
  margin-top: 8px;
  font-size: 14px;
  
  ${props => {
    switch (props.type) {
      case 'warning':
        return `
          background-color: #fff3e0;
          color: #ef6c00;
          border: 1px solid #ffb74d;
        `;
      case 'error':
        return `
          background-color: #ffebee;
          color: #d32f2f;
          border: 1px solid #ef5350;
        `;
      case 'success':
        return `
          background-color: #e8f5e9;
          color: #2e7d32;
          border: 1px solid #66bb6a;
        `;
      default:
        return `
          background-color: #e3f2fd;
          color: #1565c0;
          border: 1px solid #42a5f5;
        `;
    }
  }}
`;

const CoordinateSection = styled.div`
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  margin-bottom: 20px;
`;

const SectionTitle = styled.h4`
  margin: 0 0 12px 0;
  color: #495057;
  font-size: 16px;
  font-weight: 600;
`;

const CoordinateHelp = styled.div`
  font-size: 12px;
  color: #6c757d;
  margin-top: 8px;
  line-height: 1.4;
`;

const AdminSetupSection = styled.div`
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  margin-top: 20px;
`;

const AdminSetupTitle = styled.h4`
  margin: 0 0 12px 0;
  color: #495057;
  font-size: 16px;
  font-weight: 600;
`;

const AdminSetupText = styled.div`
  margin-bottom: 12px;
  font-size: 14px;
  color: #6c757d;
`;

const AdminSetupButton = styled.button`
  padding: 10px 20px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: #4CAF50;
  color: white;
  border: none;
  &:hover {
    background-color: #388E3C;
  }
  &:disabled {
    background-color: #A5D6A7;
    cursor: not-allowed;
  }
`;

const EditBike = ({ bike, onCancel, onSuccess }) => {
  const [formData, setFormData] = useState({
    name: bike?.name || '',
    type: bike?.type || '',
    qrCode: bike?.qrCode || '',
    hardwareId: bike?.hardwareId || '',
    price: bike?.priceValue || '',
    latitude: bike?.latitude || '',
    longitude: bike?.longitude || '',
    description: bike?.description || '',
    imageFile: null
  });

  const [imagePreview, setImagePreview] = useState(bike?.imageUrl || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [qrCodeValidation, setQrCodeValidation] = useState({ status: '', message: '' });
  const [hardwareIdValidation, setHardwareIdValidation] = useState({ status: '', message: '' });
  const [coordinateValidation, setCoordinateValidation] = useState({ status: '', message: '' });
  const [showAdminSetup, setShowAdminSetup] = useState(false);
  const [adminSetupLoading, setAdminSetupLoading] = useState(false);
  const [currentBike, setCurrentBike] = useState(bike); // Track current bike data for maintenance manager
  const [migrationLoading, setMigrationLoading] = useState(false);
  const [migrationMessage, setMigrationMessage] = useState('');
  
  // Initialize form with bike data
  useEffect(() => {
    if (bike) {
      console.log('Initializing EditBike form with bike data:', bike);
      setFormData({
        name: bike.name || '',
        type: bike.type || '',
        price: bike.priceValue?.toString() || '',
        description: bike.description || '',
        qrCode: bike.qrCode || '',
        hardwareId: bike.hardwareId || '',
        latitude: bike.latitude !== undefined ? bike.latitude : '',
        longitude: bike.longitude !== undefined ? bike.longitude : ''
      });
      setImagePreview(bike.imageUrl || '');
      
      // Clear any previous validation messages
      setQrCodeValidation({ status: '', message: '' });
      setHardwareIdValidation({ status: '', message: '' });
      setCoordinateValidation({ status: '', message: '' });
      setError('');
      setSuccess('');
    }
  }, [bike]);
  
  // Update form data when currentBike changes (from maintenance updates)
  useEffect(() => {
    if (currentBike && currentBike !== bike) {
      console.log('CurrentBike changed, updating form data:', {
        oldBike: bike,
        newBike: currentBike,
        maintenanceStatus: currentBike.maintenanceStatus
      });
      
      // Clear any previous success/error messages when bike data changes
      setSuccess('');
      // Don't clear error in case it's relevant
    }
  }, [currentBike, bike]);
  
  const validateCoordinates = (lat, lng) => {
    // Allow empty coordinates
    if (!lat && !lng) {
      setCoordinateValidation({ status: '', message: '' });
      return true;
    }
    
    // If one coordinate is provided, both should be provided
    if ((lat && !lng) || (!lat && lng)) {
      setCoordinateValidation({
        status: 'error',
        message: 'Both latitude and longitude must be provided together.'
      });
      return false;
    }
    
    const latitude = parseFloat(lat);
    const longitude = parseFloat(lng);
    
    // Check if values are valid numbers
    if (isNaN(latitude) || isNaN(longitude)) {
      setCoordinateValidation({
        status: 'error',
        message: 'Latitude and longitude must be valid numbers.'
      });
      return false;
    }
    
    // Check latitude range (-90 to 90)
    if (latitude < -90 || latitude > 90) {
      setCoordinateValidation({
        status: 'error',
        message: 'Latitude must be between -90 and 90 degrees.'
      });
      return false;
    }
    
    // Check longitude range (-180 to 180)
    if (longitude < -180 || longitude > 180) {
      setCoordinateValidation({
        status: 'error',
        message: 'Longitude must be between -180 and 180 degrees.'
      });
      return false;
    }
    
    // Check for Philippines region (optional warning)
    const isInPhilippines = (
      latitude >= 4.0 && latitude <= 21.0 &&
      longitude >= 116.0 && longitude <= 127.0
    );
    
    if (!isInPhilippines) {
      setCoordinateValidation({
        status: 'warning',
        message: 'Coordinates appear to be outside the Philippines region. Please verify the location is correct.'
      });
    } else {
      setCoordinateValidation({
        status: 'success',
        message: 'Valid coordinates within Philippines region.'
      });
    }
    
    return true;
  };
  
  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });

    // Clear validation messages when field is empty
    if (name === 'qrCode' && !value.trim()) {
      setQrCodeValidation({ status: '', message: '' });
    }
    if (name === 'hardwareId' && !value.trim()) {
      setHardwareIdValidation({ status: '', message: '' });
    }
    
    // Validate coordinates on change
    if (name === 'latitude' || name === 'longitude') {
      const lat = name === 'latitude' ? value : formData.latitude;
      const lng = name === 'longitude' ? value : formData.longitude;
      
      // Only validate if both fields have non-zero values
      if ((lat && lat !== '0') || (lng && lng !== '0')) {
        validateCoordinates(lat, lng);
      } else {
        setCoordinateValidation({ status: '', message: '' });
      }
    }
  };

  const validateQRCode = async (qrCode) => {
    if (!qrCode.trim()) {
      setQrCodeValidation({ status: '', message: '' });
      return;
    }

    // Skip validation if the QR code hasn't changed
    if (qrCode === bike.qrCode) {
      setQrCodeValidation({ status: 'success', message: 'Current QR code.' });
      return;
    }

    try {
      const collision = await checkQRCodeCollisions(qrCode);
      if (collision.hasCollisions) {
        // Check if the collision is with the current bike
        const isCurrentBike = collision.collisions.some(c => c.bikeId === bike.id);
        if (isCurrentBike && collision.collisions.length === 1) {
          setQrCodeValidation({ status: 'success', message: 'Current QR code.' });
        } else {
          setQrCodeValidation({
            status: 'error',
            message: `QR code "${qrCode}" is already in use by another bike.`
          });
        }
      } else {
        setQrCodeValidation({
          status: 'success',
          message: 'QR code is available.'
        });
      }
    } catch (error) {
      setQrCodeValidation({
        status: 'error',
        message: 'Error validating QR code.'
      });
    }
  };

  const validateHardwareId = async (hardwareId) => {
    if (!hardwareId.trim()) {
      setHardwareIdValidation({ status: '', message: '' });
      return;
    }

    // Skip validation if the hardware ID hasn't changed
    if (hardwareId === bike.hardwareId) {
      setHardwareIdValidation({ status: 'success', message: 'Current hardware ID.' });
      return;
    }

    try {
      const collision = await checkQRCodeCollisions(hardwareId);
      if (collision.hasCollisions) {
        // Check if the collision is with the current bike
        const isCurrentBike = collision.collisions.some(c => c.bikeId === bike.id);
        if (isCurrentBike && collision.collisions.length === 1) {
          setHardwareIdValidation({ status: 'success', message: 'Current hardware ID.' });
        } else {
          setHardwareIdValidation({
            status: 'error',
            message: `Hardware ID "${hardwareId}" is already in use by another bike.`
          });
        }
      } else {
        setHardwareIdValidation({
          status: 'success',
          message: 'Hardware ID is available.'
        });
      }
    } catch (error) {
      setHardwareIdValidation({
        status: 'error',
        message: 'Error validating hardware ID.'
      });
    }
  };

  const handleQRCodeBlur = (e) => {
    validateQRCode(e.target.value);
  };

  const handleHardwareIdBlur = (e) => {
    validateHardwareId(e.target.value);
  };
  
  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setFormData({
        ...formData,
        imageFile: file
      });
      
      // Create preview URL
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };
  
  const validateForm = () => {
    console.log('=== FORM VALIDATION DEBUG ===');
    console.log('Form data:', formData);
    console.log('Bike data:', bike);
    console.log('Current bike data:', currentBike);
    
    // Check required text fields
    if (!formData.name?.trim()) {
      console.log('Validation failed: Bike name is missing');
      return 'Bike name is required';
    }
    if (!formData.type?.trim()) {
      console.log('Validation failed: Bike type is missing or empty');
      console.log('formData.type value:', formData.type);
      return 'Bike type is required';
    }
    if (!formData.description?.trim()) {
      console.log('Validation failed: Description is missing');
      return 'Description is required';
    }
    
    // Check price
    const price = parseFloat(formData.price);
    if (!formData.price || isNaN(price) || price <= 0) {
      console.log('Validation failed: Invalid price');
      return 'Valid price is required';
    }
    
    // Check identifiers - at least one is required
    if (!formData.qrCode?.trim() && !formData.hardwareId?.trim()) {
      console.log('Validation failed: No identifiers provided');
      return 'At least one identifier (QR Code or Hardware ID) is required';
    }
    
    // Check coordinates if provided
    if (formData.latitude !== '' && formData.longitude !== '') {
      const lat = parseFloat(formData.latitude);
      const lng = parseFloat(formData.longitude);
      
      if (isNaN(lat) || lat < -90 || lat > 90) {
        console.log('Validation failed: Invalid latitude');
        return 'Valid latitude (-90 to 90) is required';
      }
      if (isNaN(lng) || lng < -180 || lng > 180) {
        console.log('Validation failed: Invalid longitude');
        return 'Valid longitude (-180 to 180) is required';
      }
    }
    
    console.log('Form validation passed successfully');
    console.log('=============================');
    return null;
  };

  const handleAdminSetup = async () => {
    setAdminSetupLoading(true);
    setError('');
    
    try {
      await setupAdminUser();
      setSuccess('Admin access granted successfully! You can now update bikes.');
      setShowAdminSetup(false);
    } catch (err) {
      console.error('Admin setup error:', err);
      setError(`Failed to setup admin access: ${err.message}`);
    } finally {
      setAdminSetupLoading(false);
    }
  };

  const handleMaintenanceMigration = async () => {
    setMigrationLoading(true);
    setMigrationMessage('');
    setError('');
    
    try {
      const result = await migrateBikesWithMaintenanceStatus();
      
      if (result.success) {
        if (result.updated > 0) {
          setMigrationMessage(`✅ Successfully migrated ${result.updated} bikes with maintenance status`);
        } else {
          setMigrationMessage('ℹ️ All bikes already have maintenance status - no migration needed');
        }
      } else {
        setError(`Migration failed: ${result.error}`);
      }
    } catch (err) {
      console.error('Migration error:', err);
      setError(`Migration failed: ${err.message}`);
    } finally {
      setMigrationLoading(false);
    }
  };
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const validationError = validateForm();
    if (validationError) {
      setError(validationError);
      return;
    }
    
    setLoading(true);
    setError('');
    
    try {
      // Prepare form data with proper type conversion and validation
      const updateData = {
        name: formData.name,
        type: formData.type,
        price: formData.price,
        description: formData.description,
        qrCode: formData.qrCode,
        hardwareId: formData.hardwareId
      };

      // Handle coordinates carefully
      if (formData.latitude !== '' && formData.longitude !== '') {
        const lat = parseFloat(formData.latitude);
        const lng = parseFloat(formData.longitude);
        
        // Only include coordinates if they are valid numbers
        if (!isNaN(lat) && !isNaN(lng)) {
          updateData.latitude = lat;
          updateData.longitude = lng;
        }
      }

      console.log('=== BIKE UPDATE DEBUG ===');
      console.log('Original bike data:', bike);
      console.log('Current bike data:', currentBike);
      console.log('Form data:', formData);
      console.log('Update data being sent:', updateData);
      console.log('Image file:', formData.imageFile);
      console.log('Maintenance status should be preserved:', bike?.maintenanceStatus || 'operational');
      console.log('Availability status should be preserved:', bike?.isAvailable);
      console.log('========================');
      
      const result = await updateBike(bike.id, updateData, formData.imageFile);
      console.log('Update result:', result);
      
      setSuccess('Bike updated successfully!');
      setError('');
      
      // Update current bike state for maintenance manager
      setCurrentBike(result);
      
      if (onSuccess) {
        onSuccess(result);
      }
    } catch (err) {
      console.error('=== BIKE UPDATE ERROR ===');
      console.error('Error object:', err);
      console.error('Error message:', err.message);
      console.error('Error stack:', err.stack);
      console.error('========================');
      
      // Provide more specific error messages
      let errorMessage = 'Failed to update bike. Please try again.';
      
      if (err.message) {
        if (err.message.includes('identifier') || err.message.includes('QR') || err.message.includes('Hardware')) {
          errorMessage = 'QR Code or Hardware ID validation failed. Please check for duplicates.';
        } else if (err.message.includes('permission') || err.message.includes('Access denied') || err.message.includes('Administrator')) {
          errorMessage = 'You do not have permission to update bikes. Please check your admin status.';
          setShowAdminSetup(true); // Show admin setup option
        } else if (err.message.includes('coordinates') || err.message.includes('latitude') || err.message.includes('longitude')) {
          errorMessage = 'Invalid coordinates provided. Please check the latitude and longitude values.';
        } else if (err.message.includes('network') || err.message.includes('offline') || err.message.includes('connection')) {
          errorMessage = 'Network error. Please check your internet connection and try again.';
        } else if (err.message.includes('auth') || err.message.includes('authentication')) {
          errorMessage = 'Authentication error. Please refresh the page and try again.';
        } else {
          errorMessage = `Update failed: ${err.message}`;
        }
      }
      
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <Form onSubmit={handleSubmit}>
      <FormTitle>Edit Bike</FormTitle>
      
      <FormGroup>
        <Label htmlFor="image">Bike Image</Label>
        <Input 
          type="file" 
          id="image" 
          accept="image/*" 
          onChange={handleImageChange}
        />
        {imagePreview && (
          <ImagePreview>
            <img src={imagePreview} alt="Bike preview" />
          </ImagePreview>
        )}
      </FormGroup>
      
      <FormGroup>
        <Label htmlFor="name">Bike Name *</Label>
        <Input
          type="text"
          id="name"
          name="name"
          value={formData.name}
          onChange={handleInputChange}
          placeholder="Enter bike name"
          required
        />
      </FormGroup>
      
      <FormGroup>
        <Label htmlFor="type">Bike Type *</Label>
        <Select
          id="type"
          name="type"
          value={formData.type}
          onChange={handleInputChange}
          required
        >
          <option value="">Select bike type</option>
          <option value="Mountain Bike">Mountain Bike</option>
          <option value="Road Bike">Road Bike</option>
          <option value="Hybrid Bike">Hybrid Bike</option>
          <option value="Electric Bike">Electric Bike</option>
          <option value="BMX">BMX</option>
          <option value="Folding Bike">Folding Bike</option>
        </Select>
      </FormGroup>

      <FormGroup>
        <Label htmlFor="qrCode">QR Code *</Label>
        <Input
          type="text"
          id="qrCode"
          name="qrCode"
          value={formData.qrCode}
          onChange={handleInputChange}
          onBlur={handleQRCodeBlur}
          placeholder="Enter QR code (preferred identifier)"
        />
        {qrCodeValidation.message && (
          <ValidationMessage type={qrCodeValidation.status}>
            {qrCodeValidation.message}
          </ValidationMessage>
        )}
      </FormGroup>
      
      <FormGroup>
        <Label htmlFor="hardwareId">Hardware ID (Legacy)</Label>
        <Input
          type="text"
          id="hardwareId"
          name="hardwareId"
          value={formData.hardwareId}
          onChange={handleInputChange}
          onBlur={handleHardwareIdBlur}
          placeholder="Enter hardware ID (for backward compatibility)"
        />
        {hardwareIdValidation.message && (
          <ValidationMessage type={hardwareIdValidation.status}>
            {hardwareIdValidation.message}
          </ValidationMessage>
        )}
        <ValidationMessage type="info">
          <small>
            * At least one identifier (QR Code or Hardware ID) is required.<br />
            QR Code is the preferred identifier for bikes.
          </small>
        </ValidationMessage>
      </FormGroup>
      
      <FormGroup>
        <Label htmlFor="price">Price per Hour (₱) *</Label>
        <Input
          type="number"
          id="price"
          name="price"
          value={formData.price}
          onChange={handleInputChange}
          placeholder="Enter price per hour"
          min="0"
          step="0.01"
          required
        />
      </FormGroup>
      
      <FormGroup>
        <Label htmlFor="description">Description</Label>
        <Textarea
          id="description"
          name="description"
          value={formData.description}
          onChange={handleInputChange}
          placeholder="Enter bike description"
        />
      </FormGroup>
      
      <CoordinateSection>
        <SectionTitle>📍 Location Coordinates</SectionTitle>
        <FormRow>
          <FormColumn>
            <Label htmlFor="latitude">Latitude</Label>
            <Input
              type="number"
              id="latitude"
              name="latitude"
              value={formData.latitude}
              onChange={handleInputChange}
              placeholder="e.g., 14.5995"
              step="any"
              min="-90"
              max="90"
            />
          </FormColumn>
          <FormColumn>
            <Label htmlFor="longitude">Longitude</Label>
            <Input
              type="number"
              id="longitude"
              name="longitude"
              value={formData.longitude}
              onChange={handleInputChange}
              placeholder="e.g., 120.9842"
              step="any"
              min="-180"
              max="180"
            />
          </FormColumn>
        </FormRow>
        
        {coordinateValidation.message && (
          <ValidationMessage type={coordinateValidation.status}>
            {coordinateValidation.message}
          </ValidationMessage>
        )}
        
        <CoordinateHelp>
          <strong>Tips:</strong> 
          • Use decimal degrees format (e.g., 14.5995, 120.9842 for Manila)
          • Latitude range: -90 to 90 degrees
          • Longitude range: -180 to 180 degrees
          • You can get coordinates from Google Maps by right-clicking on a location
          • <strong>Note:</strong> Updating coordinates will preserve the bike's current maintenance status and availability settings
        </CoordinateHelp>
      </CoordinateSection>
      
      {error && <ErrorMessage>{error}</ErrorMessage>}
      {success && <SuccessMessage>{success}</SuccessMessage>}
      
      {showAdminSetup && (
        <AdminSetupSection>
          <AdminSetupTitle>🔧 Admin Access Required</AdminSetupTitle>
          <AdminSetupText>
            It looks like you need admin privileges to update bikes. You can grant yourself admin access if you're the application administrator.
          </AdminSetupText>
          <AdminSetupButton 
            type="button" 
            onClick={handleAdminSetup}
            disabled={adminSetupLoading}
          >
            {adminSetupLoading ? 'Setting up admin access...' : 'Grant Admin Access'}
          </AdminSetupButton>
        </AdminSetupSection>
      )}
      
      {/* Migration Section */}
      <AdminSetupSection>
        <AdminSetupTitle>🔄 Database Migration</AdminSetupTitle>
        <AdminSetupText>
          If this is your first time using the maintenance management feature, you may need to migrate existing bikes to include maintenance status fields.
        </AdminSetupText>
        <AdminSetupButton 
          type="button" 
          onClick={handleMaintenanceMigration}
          disabled={migrationLoading}
        >
          {migrationLoading ? 'Migrating bikes...' : 'Migrate Bikes for Maintenance'}
        </AdminSetupButton>
        {migrationMessage && (
          <div style={{ marginTop: '10px', fontSize: '14px', color: '#2e7d32' }}>
            {migrationMessage}
          </div>
        )}
      </AdminSetupSection>
      
      {/* Maintenance Management Section */}
      <MaintenanceManager 
        bike={currentBike} 
        onUpdate={(updatedBike) => {
          setCurrentBike(updatedBike);
          // You can also call onSuccess here if you want to refresh the parent component
          if (onSuccess) {
            onSuccess(updatedBike);
          }
        }} 
      />
      
      <ButtonGroup>
        <SecondaryButton type="button" onClick={onCancel}>
          Cancel
        </SecondaryButton>
        <PrimaryButton type="submit" disabled={loading}>
          {loading ? 'Updating...' : 'Update Bike'}
        </PrimaryButton>
      </ButtonGroup>
    </Form>
  );
};

export default EditBike; 