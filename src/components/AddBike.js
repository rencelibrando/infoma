// src/components/AddBike.js
import React, { useState } from 'react';
import { uploadBike } from '../services/bikeService';
import styled from 'styled-components';

const Form = styled.form`
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
`;

const FormGroup = styled.div`
  margin-bottom: 20px;
`;

const Label = styled.label`
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
`;

const Input = styled.input`
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
`;

const Textarea = styled.textarea`
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  min-height: 100px;
`;

const Button = styled.button`
  padding: 10px 15px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  &:disabled {
    background-color: #cccccc;
    cursor: not-allowed;
  }
`;

const ImagePreview = styled.div`
  width: 100%;
  height: 200px;
  border: 2px dashed #ddd;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  overflow: hidden;
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
    <div>
      <h2>Add New Bike</h2>
      
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {success && <p style={{ color: 'green' }}>{success}</p>}
      
      <Form onSubmit={handleSubmit}>
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
        
        <FormGroup>
          <Label>Description</Label>
          <Textarea
            name="description"
            value={bike.description}
            onChange={handleChange}
            placeholder="Describe the bike"
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
        
        <FormGroup>
          <Label>Location</Label>
          <Button 
            type="button" 
            onClick={handleGetLocation}
            style={{ marginBottom: '10px', backgroundColor: '#2196F3' }}
          >
            Get Current Location
          </Button>
          <p>
            Latitude: {bike.latitude}, Longitude: {bike.longitude}
          </p>
        </FormGroup>
        
        <Button type="submit" disabled={loading}>
          {loading ? 'Uploading...' : 'Add Bike'}
        </Button>
      </Form>
    </div>
  );
};

export default AddBike;