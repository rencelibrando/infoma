// src/components/BikesList.js
import React, { useEffect, useState } from 'react';
import { getBikes, deleteBike } from '../services/bikeService';
import styled from 'styled-components';

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  background-color: white;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
`;

const TableHeader = styled.th`
  text-align: left;
  padding: 12px;
  background-color: #f2f2f2;
  border-bottom: 1px solid #ddd;
`;

const TableRow = styled.tr`
  &:nth-child(even) {
    background-color: #f9f9f9;
  }
  &:hover {
    background-color: #f5f5f5;
  }
`;

const TableCell = styled.td`
  padding: 12px;
  border-bottom: 1px solid #eee;
`;

const Button = styled.button`
  padding: 6px 12px;
  margin-right: 5px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  background-color: ${props => props.danger ? '#ff4444' : props.edit ? '#2196F3' : '#4CAF50'};
  color: white;
  transition: all 0.3s ease;
  
  &:hover {
    opacity: 0.85;
    transform: translateY(-2px);
    box-shadow: 0 3px 8px rgba(0, 0, 0, 0.15);
  }
`;

const BikesList = ({ onEditBike }) => {
  const [bikes, setBikes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchBikes = async () => {
      try {
        setLoading(true);
        const bikeData = await getBikes();
        setBikes(bikeData);
      } catch (err) {
        setError('Failed to fetch bikes: ' + err.message);
      } finally {
        setLoading(false);
      }
    };
    
    fetchBikes();
  }, []);

  const handleDelete = async (bikeId) => {
    if (window.confirm('Are you sure you want to delete this bike?')) {
      try {
        await deleteBike(bikeId);
        setBikes(bikes.filter(bike => bike.id !== bikeId));
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

  if (loading) return <div>Loading bikes...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;

  return (
    <div>
      <h2>Manage Bikes</h2>
      {bikes.length === 0 ? (
        <p>No bikes available. Add some bikes to get started.</p>
      ) : (
        <Table>
          <thead>
            <tr>
              <TableHeader>Image</TableHeader>
              <TableHeader>Name</TableHeader>
              <TableHeader>Type</TableHeader>
              <TableHeader>Price</TableHeader>
              <TableHeader>Status</TableHeader>
              <TableHeader>Actions</TableHeader>
            </tr>
          </thead>
          <tbody>
            {bikes.map((bike) => (
              <TableRow key={bike.id}>
                <TableCell>
                  <img 
                    src={bike.imageUrl} 
                    alt={bike.name} 
                    style={{ width: '60px', height: '60px', objectFit: 'cover', borderRadius: '4px' }} 
                  />
                </TableCell>
                <TableCell>{bike.name}</TableCell>
                <TableCell>{bike.type}</TableCell>
                <TableCell>{bike.price}</TableCell>
                <TableCell>
                  {bike.isAvailable ? 
                    <span style={{ color: 'green' }}>Available</span> : 
                    <span style={{ color: 'red' }}>Not Available</span>
                  }
                </TableCell>
                <TableCell>
                  <Button edit onClick={() => handleEdit(bike)}>Edit</Button>
                  <Button danger onClick={() => handleDelete(bike.id)}>Delete</Button>
                </TableCell>
              </TableRow>
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
};

export default BikesList;