import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { updateBike } from '../services/bikeService';
import { checkQRCodeCollisions } from '../services/migrationService';

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

const EditBike = ({ bike, onCancel, onSuccess }) => {
  const [formData, setFormData] = useState({
    name: '',
    type: '',
    price: '',
    description: '',
    qrCode: '',
    hardwareId: '',
    isAvailable: true,
    latitude: 0,
    longitude: 0
  });
  
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [qrCodeValidation, setQrCodeValidation] = useState({ status: '', message: '' });
  const [hardwareIdValidation, setHardwareIdValidation] = useState({ status: '', message: '' });
  
  // Initialize form with bike data
  useEffect(() => {
    if (bike) {
      setFormData({
        name: bike.name || '',
        type: bike.type || '',
        price: bike.priceValue?.toString() || '',
        description: bike.description || '',
        qrCode: bike.qrCode || '',
        hardwareId: bike.hardwareId || '',
        isAvailable: bike.isAvailable !== undefined ? bike.isAvailable : true,
        latitude: bike.latitude || 0,
        longitude: bike.longitude || 0
      });
      setImagePreview(bike.imageUrl || '');
    }
  }, [bike]);
  
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
      setImageFile(file);
      
      // Create preview URL
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };
  
  const validateForm = () => {
    if (!formData.name) return 'Bike name is required';
    if (!formData.type) return 'Bike type is required';
    if (!formData.price) return 'Price is required';
    if (isNaN(parseFloat(formData.price))) return 'Price must be a number';
    
    // Validate QR code and hardware ID
    if (!formData.qrCode.trim() && !formData.hardwareId.trim()) {
      return 'Either QR Code or Hardware ID is required';
    }
    
    // Check for validation errors
    if (qrCodeValidation.status === 'error') {
      return 'QR Code validation failed. Please fix the issue.';
    }
    
    if (hardwareIdValidation.status === 'error') {
      return 'Hardware ID validation failed. Please fix the issue.';
    }
    
    return '';
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
      // If the bike is being marked as available, we need to ensure it's locked
      if (formData.isAvailable && !bike.isLocked) {
        const confirmLock = window.confirm(
          "This bike is currently unlocked. To mark it as available, it must be locked. Do you want to lock this bike now?"
        );
        
        if (!confirmLock) {
          setError("A bike must be locked to be available. Please try again.");
          setLoading(false);
          return;
        }
        
        // We'll add isLocked: true to the update data
        await updateBike(bike.id, {...formData, isLocked: true}, imageFile);
      } else {
        await updateBike(bike.id, formData, imageFile);
      }
      
      setSuccess('Bike updated successfully!');
      setTimeout(() => {
        if (onSuccess) onSuccess();
      }, 1500);
    } catch (err) {
      setError('Failed to update bike. Please try again.');
      console.error(err);
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
      
      <FormGroup>
        <Label>
          <Input
            type="checkbox"
            name="isAvailable"
            checked={formData.isAvailable}
            onChange={handleInputChange}
          />
          Available for Rent
        </Label>
      </FormGroup>
      
      {error && <ErrorMessage>{error}</ErrorMessage>}
      {success && <SuccessMessage>{success}</SuccessMessage>}
      
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