// src/components/AddBike.js
import React, { useState } from 'react';
import { uploadBike } from '../services/bikeService';
import styled from 'styled-components';

const Container = styled.div`
  padding: 20px;
`;

const Title = styled.h2`
  color: #333;
  margin-bottom: 24px;
  font-weight: 600;
  position: relative;
  
  &:after {
    content: '';
    position: absolute;
    bottom: -8px;
    left: 0;
    width: 60px;
    height: 3px;
    background-color: #4CAF50;
  }
`;

const Form = styled.form`
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  
  &:hover {
    box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  }
`;

const FormSection = styled.div`
  margin-bottom: 30px;
`;

const SectionTitle = styled.h3`
  font-size: 1.1rem;
  color: #555;
  margin-bottom: 15px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
`;

const FormGroup = styled.div`
  margin-bottom: 20px;
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #444;
`;

const Input = styled.input`
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
  transition: border 0.2s ease, box-shadow 0.2s ease;
  
  &:focus {
    outline: none;
    border-color: #4CAF50;
    box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2);
  }
`;

const Textarea = styled.textarea`
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  min-height: 120px;
  font-size: 16px;
  transition: border 0.2s ease, box-shadow 0.2s ease;
  
  &:focus {
    outline: none;
    border-color: #4CAF50;
    box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2);
  }
`;

const Button = styled.button`
  padding: 12px 20px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: background-color 0.2s ease, transform 0.1s ease;
  
  &:hover {
    background-color: #43a047;
    transform: translateY(-1px);
  }
  
  &:active {
    transform: translateY(1px);
  }
  
  &:disabled {
    background-color: #cccccc;
    cursor: not-allowed;
    transform: none;
  }
`;

const LocationButton = styled(Button)`
  background-color: #2196F3;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  
  &:hover {
    background-color: #1e88e5;
  }
`;

const ImagePreview = styled.div`
  width: 100%;
  height: 220px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  overflow: hidden;
  background-color: #f9f9f9;
  transition: border-color 0.2s ease;
  
  &:hover {
    border-color: #4CAF50;
  }
`;

const LocationInfo = styled.div`
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 6px;
  margin-top: 10px;
`;

const Message = styled.div`
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  
  &:before {
    content: '';
    margin-right: 10px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
  }
`;

const ErrorMessage = styled(Message)`
  background-color: #ffebee;
  color: #d32f2f;
  
  &:before {
    background-color: #d32f2f;
  }
`;

const SuccessMessage = styled(Message)`
  background-color: #e8f5e9;
  color: #388e3c;
  
  &:before {
    background-color: #388e3c;
  }
`;

const AddBike = ({ onSuccess }) => {
  const [bike, setBike] = useState({
    name: '',
    type: '',
    price: '',
    description: '',
    latitude: 0,
    longitude: 0
  });
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleChange = (e) => {
    setBike({
      ...bike,
      [e.target.name]: e.target.value
    });
  };

  const handleImageChange = (e) => {
    if (e.target.files[0]) {
      setImageFile(e.target.files[0]);
      
      // Create an image preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
      };
      reader.readAsDataURL(e.target.files[0]);
    }
  };

  const handleGetLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setBike({
            ...bike,
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          });
        },
        (error) => {
          setError(`Geolocation error: ${error.message}`);
        }
      );
    } else {
      setError("Geolocation is not supported by this browser.");
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    setLoading(true);
    setError('');
    
    try {
      await uploadBike(bike, imageFile);
      setSuccess("Bike added successfully!");
      setBike({
        name: '',
        type: '',
        price: '',
        description: '',
        latitude: 0,
        longitude: 0
      });
      setImageFile(null);
      setImagePreview(null);
      
      // Notify parent component of success
      if (onSuccess) {
        setTimeout(() => {
          onSuccess();
        }, 1000);
      }
    } catch (err) {
      setError(`Error adding bike: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const validateForm = () => {
    if (!bike.name.trim()) {
      setError("Name is required");
      return false;
    }
    if (!bike.type.trim()) {
      setError("Type is required");
      return false;
    }
    if (!bike.price.trim() || isNaN(parseFloat(bike.price))) {
      setError("Valid price is required");
      return false;
    }
    if (!imageFile) {
      setError("Image is required");
      return false;
    }
    if (bike.latitude === 0 && bike.longitude === 0) {
      setError("Location is required");
      return false;
    }
    return true;
  };

  return (
    <Container>
      <Title>Add New Bike</Title>
      
      {error && <ErrorMessage>{error}</ErrorMessage>}
      {success && <SuccessMessage>{success}</SuccessMessage>}
      
      <Form onSubmit={handleSubmit}>
        <FormSection>
          <SectionTitle>Basic Information</SectionTitle>
          <FormGroup>
            <Label>Bike Name</Label>
            <Input
              type="text"
              name="name"
              value={bike.name}
              onChange={handleChange}
              placeholder="Enter bike name"
            />
          </FormGroup>
          
          <FormGroup>
            <Label>Bike Type</Label>
            <Input
              type="text"
              name="type"
              value={bike.type}
              onChange={handleChange}
              placeholder="Mountain, Road, City, etc."
            />
          </FormGroup>
          
          <FormGroup>
            <Label>Price per Hour (₱)</Label>
            <Input
              type="number"
              name="price"
              value={bike.price}
              onChange={handleChange}
              placeholder="Price in Philippine Peso"
              step="0.01"
            />
          </FormGroup>
        </FormSection>
        
        <FormSection>
          <SectionTitle>Details & Media</SectionTitle>
          <FormGroup>
            <Label>Description</Label>
            <Textarea
              name="description"
              value={bike.description}
              onChange={handleChange}
              placeholder="Describe the bike features, conditions, and any special notes"
            />
          </FormGroup>
          
          <FormGroup>
            <Label>Bike Image</Label>
            <ImagePreview>
              {imagePreview ? (
                <img 
                  src={imagePreview} 
                  alt="Preview" 
                  style={{ width: '100%', height: '100%', objectFit: 'contain' }} 
                />
              ) : (
                <p>Select an image to preview</p>
              )}
            </ImagePreview>
            <Input
              type="file"
              accept="image/*"
              onChange={handleImageChange}
            />
          </FormGroup>
        </FormSection>
        
        <FormSection>
          <SectionTitle>Location Information</SectionTitle>
          <FormGroup>
            <Label>Bike Location</Label>
            <LocationButton 
              type="button" 
              onClick={handleGetLocation}
            >
              📍 Get Current Location
            </LocationButton>
            
            <LocationInfo>
              <p>
                <strong>Latitude:</strong> {bike.latitude.toFixed(6)}
                <br />
                <strong>Longitude:</strong> {bike.longitude.toFixed(6)}
              </p>
            </LocationInfo>
          </FormGroup>
        </FormSection>
        
        <Button type="submit" disabled={loading}>
          {loading ? 'Uploading...' : 'Add Bike'}
        </Button>
      </Form>
    </Container>
  );
};

export default AddBike;