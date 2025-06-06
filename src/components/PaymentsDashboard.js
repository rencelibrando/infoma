import React, { useState, useEffect } from 'react';
import { collection, getDocs, doc, updateDoc, orderBy, query, onSnapshot, setDoc, getDoc, where } from 'firebase/firestore';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { db, storage } from '../firebase';
import { useAuth } from '../context/AuthContext';
import styled from 'styled-components';

// Black and white theme colors to match BookingManagement
const colors = {
  primary: '#000000',
  secondary: '#333333',
  tertiary: '#666666',
  quaternary: '#999999',
  lightGray: '#f8f9fa',
  mediumGray: '#e9ecef',
  darkGray: '#495057',
  white: '#ffffff',
  success: '#28a745',
  warning: '#ffc107',
  danger: '#dc3545',
  info: '#17a2b8',
  border: '#dee2e6'
};

const Container = styled.div`
  padding: 16px;
  background-color: ${colors.white};
  min-height: 100vh;
`;

const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid ${colors.border};
`;

const Title = styled.h1`
  color: ${colors.primary};
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: -0.025em;
`;

// Updated styled components for payment settings with black-and-white theme
const PaymentSettingsSection = styled.div`
  background: ${colors.white};
  border: 2px solid ${colors.border};
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.06);
`;

const SettingsTitle = styled.h2`
  color: ${colors.primary};
  margin: 0 0 16px 0;
  font-size: 1.5rem;
  font-weight: 600;
  letter-spacing: -0.025em;
`;

const SettingsGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: start;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

const SettingCard = styled.div`
  background: ${colors.lightGray};
  border: 1px solid ${colors.border};
  border-radius: 8px;
  padding: 16px;
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.06);
  }
`;

const SettingLabel = styled.label`
  display: block;
  font-weight: 600;
  color: ${colors.secondary};
  margin-bottom: 6px;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
`;

const SettingInput = styled.input`
  width: 100%;
  padding: 8px 12px;
  border: 2px solid ${colors.border};
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
  background-color: ${colors.white};
  color: ${colors.primary};
  transition: all 0.2s ease;

  &:focus {
    outline: none;
    border-color: ${colors.primary};
    box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
  }

  &::placeholder {
    color: ${colors.quaternary};
  }
`;

const FileInputWrapper = styled.div`
  position: relative;
  display: inline-block;
  width: 100%;
`;

const FileInput = styled.input`
  display: none;
`;

const FileInputLabel = styled.label`
  display: block;
  width: 100%;
  padding: 12px;
  border: 2px dashed ${colors.border};
  border-radius: 6px;
  text-align: center;
  cursor: pointer;
  background-color: ${colors.white};
  color: ${colors.secondary};
  font-weight: 500;
  transition: all 0.2s ease;

  &:hover {
    border-color: ${colors.primary};
    background-color: ${colors.lightGray};
    color: ${colors.primary};
  }
`;

const CurrentQRPreview = styled.div`
  margin-top: 12px;
  text-align: center;
`;

const QRImage = styled.img`
  max-width: 150px;
  height: auto;
  border-radius: 6px;
  border: 2px solid ${colors.border};
  transition: all 0.2s ease;

  &:hover {
    border-color: ${colors.primary};
  }
`;

const SaveButton = styled.button`
  background-color: ${colors.primary};
  color: ${colors.white};
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 12px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;

  &:hover {
    background-color: ${colors.secondary};
    transform: translateY(-1px);
    box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.06);
  }

  &:disabled {
    background-color: ${colors.quaternary};
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }
`;

const SuccessMessage = styled.div`
  background-color: ${colors.success};
  color: ${colors.white};
  padding: 8px 12px;
  border-radius: 6px;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 500;
`;

const ErrorMessage = styled.div`
  background-color: ${colors.danger};
  color: ${colors.white};
  padding: 8px 12px;
  border-radius: 6px;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 500;
`;

const LoadingSpinner = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 40vh;
  font-size: 16px;
  color: ${colors.secondary};
`;

const StatsContainer = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
`;

const StatCard = styled.div`
  background: ${colors.white};
  border: 2px solid ${colors.border};
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  transition: all 0.2s ease;

  &:hover {
    border-color: ${colors.primary};
    transform: translateY(-2px);
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  }
`;

const StatValue = styled.div`
  font-size: 1.8rem;
  font-weight: 700;
  color: ${colors.primary};
  margin-bottom: 4px;
`;

const StatLabel = styled.div`
  font-size: 0.8rem;
  font-weight: 600;
  color: ${colors.secondary};
  text-transform: uppercase;
  letter-spacing: 0.05em;
`;

const FilterContainer = styled.div`
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
`;

const FilterSelect = styled.select`
  padding: 6px 12px;
  border: 2px solid ${colors.border};
  border-radius: 6px;
  background-color: ${colors.white};
  color: ${colors.primary};
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;

  &:focus {
    outline: none;
    border-color: ${colors.primary};
    box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
  }
`;

const SearchInput = styled.input`
  padding: 6px 12px;
  border: 2px solid ${colors.border};
  border-radius: 6px;
  background-color: ${colors.white};
  color: ${colors.primary};
  font-weight: 500;
  min-width: 200px;
  transition: all 0.2s ease;

  &:focus {
    outline: none;
    border-color: ${colors.primary};
    box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
  }

  &::placeholder {
    color: ${colors.quaternary};
  }
`;

const PaymentsTable = styled.div`
  background: ${colors.white};
  border: 2px solid ${colors.border};
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.06);
`;

const TableHeader = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr 1fr 120px;
  background: ${colors.lightGray};
  border-bottom: 2px solid ${colors.border};
  font-weight: 700;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: ${colors.secondary};
  
  @media (max-width: 1200px) {
    display: none;
  }
`;

const HeaderCell = styled.div`
  padding: 12px 16px;
  border-right: 1px solid ${colors.border};
  
  &:last-child {
    border-right: none;
  }
`;

const TableRow = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr 1fr 120px;
  border-bottom: 1px solid ${colors.border};
  transition: all 0.2s ease;
  
  &:hover {
    background-color: ${colors.lightGray};
  }
  
  &:last-child {
    border-bottom: none;
  }
  
  @media (max-width: 1200px) {
    display: block;
    padding: 16px;
    border-bottom: 2px solid ${colors.border};
    
    &:hover {
      background-color: ${colors.lightGray};
    }
  }
`;

const TableCell = styled.div`
  padding: 12px 16px;
  border-right: 1px solid ${colors.border};
  display: flex;
  align-items: center;
  
  &:last-child {
    border-right: none;
  }
  
  @media (max-width: 1200px) {
    border-right: none;
    padding: 6px 0;
    display: block;
    
    &:before {
      content: "${props => props.label}: ";
      font-weight: 700;
      color: ${colors.secondary};
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      display: inline-block;
      min-width: 100px;
    }
    
    &:last-child:before {
      content: "Actions: ";
    }
  }
`;

const PaymentInfoCard = styled.div`
  width: 100%;
  
  @media (max-width: 1200px) {
    display: inline-block;
    margin-left: 8px;
  }
`;

const PaymentInfoLabel = styled.div`
  font-size: 0.7rem;
  font-weight: 600;
  color: ${colors.tertiary};
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 2px;
  
  @media (max-width: 1200px) {
    display: none;
  }
`;

const PaymentInfoValue = styled.div`
  font-size: 0.85rem;
  font-weight: 600;
  color: ${colors.primary};
  word-break: break-word;
`;

const DetailLabel = styled.div`
  font-size: 0.7rem;
  font-weight: 600;
  color: ${colors.tertiary};
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
  margin-top: 6px;
`;

const StatusBadge = styled.span`
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  text-align: center;
  border: 2px solid;
  background-color: ${props => {
    switch (props.status) {
      case 'PENDING': return colors.warning;
      case 'CONFIRMED': return colors.success;
      case 'REJECTED': return colors.danger;
      default: return colors.quaternary;
    }
  }};
  color: ${colors.white};
  border-color: ${props => {
    switch (props.status) {
      case 'PENDING': return colors.warning;
      case 'CONFIRMED': return colors.success;
      case 'REJECTED': return colors.danger;
      default: return colors.quaternary;
    }
  }};
`;

const ScreenshotContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

const ScreenshotImage = styled.img`
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
  border: 2px solid ${colors.border};
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: ${colors.primary};
    transform: scale(1.05);
  }
`;

const ActionButtons = styled.div`
  display: flex;
  gap: 6px;
  flex-direction: column;

  @media (max-width: 1200px) {
    flex-direction: row;
    justify-content: flex-start;
    margin-left: 8px;
  }
`;

const ActionButton = styled.button`
  padding: 6px 12px;
  border: 2px solid;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  
  ${props => props.variant === 'confirm' ? `
    background-color: ${colors.success};
    color: ${colors.white};
    border-color: ${colors.success};
    
    &:hover {
      background-color: transparent;
      color: ${colors.success};
    }
  ` : `
    background-color: ${colors.danger};
    color: ${colors.white};
    border-color: ${colors.danger};
    
    &:hover {
      background-color: transparent;
      color: ${colors.danger};
    }
  `}

  &:disabled {
    background-color: ${colors.quaternary};
    color: ${colors.white};
    border-color: ${colors.quaternary};
    cursor: not-allowed;
    
    &:hover {
      background-color: ${colors.quaternary};
      color: ${colors.white};
    }
  }
`;

const Modal = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
`;

const ModalContent = styled.div`
  background: ${colors.white};
  padding: 20px;
  border-radius: 8px;
  max-width: 80vw;
  max-height: 80vh;
  position: relative;
`;

const CloseButton = styled.button`
  position: absolute;
  top: 8px;
  right: 8px;
  background: ${colors.danger};
  color: ${colors.white};
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  cursor: pointer;
  font-weight: 700;
  font-size: 14px;
  
  &:hover {
    background: ${colors.secondary};
  }
`;

const ModalImage = styled.img`
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 6px;
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 40px 16px;
  color: ${colors.tertiary};
  font-size: 16px;
  font-weight: 500;
  background: ${colors.lightGray};
  border-radius: 8px;
  border: 2px dashed ${colors.border};
`;

// Add new styled components for modern card layout
const PaymentCard = styled.div`
  background: ${colors.white};
  border: 2px solid ${props => props.expanded ? colors.primary : colors.border};
  border-radius: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  
  &:hover {
    border-color: ${colors.primary};
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
`;

const PaymentCardHeader = styled.div`
  padding: 16px 20px;
  display: grid;
  grid-template-columns: auto 1fr auto auto;
  gap: 16px;
  align-items: center;
  background: ${props => props.expanded ? colors.lightGray : 'transparent'};
`;

const PaymentIcon = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: ${props => {
    switch (props.status) {
      case 'PENDING': return `linear-gradient(135deg, ${colors.warning}, #f59e0b)`;
      case 'CONFIRMED': return `linear-gradient(135deg, ${colors.success}, #10b981)`;
      case 'REJECTED': return `linear-gradient(135deg, ${colors.danger}, #ef4444)`;
      default: return `linear-gradient(135deg, ${colors.primary}, ${colors.secondary})`;
    }
  }};
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 18px;
`;

const PaymentMainInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

const PaymentTitle = styled.div`
  font-size: 16px;
  font-weight: 600;
  color: ${colors.primary};
`;

const PaymentSubtitle = styled.div`
  font-size: 13px;
  color: ${colors.tertiary};
  display: flex;
  gap: 12px;
  align-items: center;
`;

const PaymentAmount = styled.div`
  font-size: 20px;
  font-weight: 700;
  color: ${colors.primary};
  text-align: right;
`;

const ExpandButton = styled.div`
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: ${colors.lightGray};
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  transform: ${props => props.expanded ? 'rotate(180deg)' : 'rotate(0deg)'};
  
  &:hover {
    background: ${colors.primary};
    color: white;
  }
`;

const PaymentDetails = styled.div`
  padding: 0 20px 20px 20px;
  background: ${colors.lightGray};
  border-top: 1px solid ${colors.border};
  display: ${props => props.expanded ? 'block' : 'none'};
`;

const DetailGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

const DetailItem = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

const DetailItemLabel = styled.div`
  font-size: 11px;
  font-weight: 600;
  color: ${colors.tertiary};
  text-transform: uppercase;
  letter-spacing: 0.05em;
`;

const DetailItemValue = styled.div`
  font-size: 14px;
  font-weight: 500;
  color: ${colors.primary};
  word-break: break-all;
`;

const PaymentsDashboard = () => {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [filteredPayments, setFilteredPayments] = useState([]);
  const [selectedImage, setSelectedImage] = useState(null);
  const [processingId, setProcessingId] = useState(null);
  
  // New state for expanded payment details
  const [expandedPayment, setExpandedPayment] = useState(null);
  
  // Settings states
  const [businessName, setBusinessName] = useState('');
  const [gcashNumber, setGCashNumber] = useState('');
  const [qrFile, setQrFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [currentQRUrl, setCurrentQRUrl] = useState('');

  const { user, isAuthenticated, loading: authLoading } = useAuth();

  // Load payment settings from Firestore
  useEffect(() => {
    const loadPaymentSettings = async () => {
      try {
        const settingsDoc = await getDoc(doc(db, 'settings', 'payment'));
        if (settingsDoc.exists()) {
          setBusinessName(settingsDoc.data().businessName);
          setGCashNumber(settingsDoc.data().gcashNumber);
          setCurrentQRUrl(settingsDoc.data().qrCodeUrl);
        } else {
          // Create default settings if they don't exist
          const defaultSettings = {
            gcashNumber: '09123456789',
            businessName: 'Bambike Cycles',
            qrCodeUrl: ''
          };
          await setDoc(doc(db, 'settings', 'payment'), defaultSettings);
          setBusinessName(defaultSettings.businessName);
          setGCashNumber(defaultSettings.gcashNumber);
          setCurrentQRUrl(defaultSettings.qrCodeUrl);
        }
      } catch (error) {
        console.error('Error loading payment settings:', error);
      }
    };

    if (isAuthenticated) {
      loadPaymentSettings();
    }
  }, [isAuthenticated]);

  // Load payments from Firestore with real-time updates
  const loadPayments = async () => {
    try {
      setLoading(true);
      
      // First, let's try a simple query without any authentication filters
      const paymentsRef = collection(db, 'payments');
      
      // Try to get all payments (this will be limited by Firebase rules)
      const snapshot = await getDocs(paymentsRef);
      
      if (snapshot.empty) {
        setPayments([]);
        setLoading(false);
        return;
      }
      
      const paymentsData = [];
      
      snapshot.forEach((doc) => {
        const data = doc.data();
        
        // Validate required fields
        const requiredFields = ['userId', 'amount', 'status', 'createdAt'];
        const missingFields = requiredFields.filter(field => !data.hasOwnProperty(field));
        
        if (missingFields.length > 0) {
          console.warn(`Document ${doc.id} missing fields:`, missingFields);
        }
        
        paymentsData.push({
          id: doc.id,
          ...data,
          // Ensure amount is a number
          amount: typeof data.amount === 'number' ? data.amount : parseFloat(data.amount) || 0
        });
      });
      
      setPayments(paymentsData);
      setLoading(false);
      
    } catch (error) {
      console.error('Error loading payments:', error);
      setLoading(false);
    }
  };

  // Load payments on component mount
  useEffect(() => {
    if (isAuthenticated) {
      loadPayments();
    }
  }, [isAuthenticated]);

  // Filter payments based on status and search term
  useEffect(() => {
    let filtered = payments;

    // Filter by status
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(payment => payment.status === statusFilter);
    }

    // Filter by search term
    if (searchTerm) {
      filtered = filtered.filter(payment =>
        payment.mobileNumber?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        payment.referenceNumber?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        payment.bikeType?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    setFilteredPayments(filtered);
  }, [payments, statusFilter, searchTerm]);

  // Calculate statistics
  const stats = {
    total: payments.length,
    pending: payments.filter(p => p.status === 'PENDING').length,
    confirmed: payments.filter(p => p.status === 'CONFIRMED').length,
    rejected: payments.filter(p => p.status === 'REJECTED').length
  };

  const handlePaymentAction = async (paymentId, newStatus) => {
    setProcessingId(paymentId);
    try {
      const paymentRef = doc(db, 'payments', paymentId);
      await updateDoc(paymentRef, {
        status: newStatus,
        processedAt: new Date(),
        processedBy: user?.email || 'admin' // Use actual authenticated user email
      });
      
      // Reload payments to reflect the update
      loadPayments();
    } catch (error) {
      console.error('Error updating payment:', error);
      alert('Failed to update payment status. Please try again.');
    }
    setProcessingId(null);
  };

  // Handle payment settings update
  const handleSettingsUpdate = async () => {
    // Validate input fields
    if (!gcashNumber || !businessName) {
      setError({ 
        type: 'error', 
        text: 'Please fill in both GCash number and Business name before updating.' 
      });
      return;
    }

    // Validate GCash number format (basic validation)
    if (!/^09\d{9}$/.test(gcashNumber)) {
      setError({ 
        type: 'error', 
        text: 'Please enter a valid GCash number (format: 09XXXXXXXXX).' 
      });
      return;
    }

    setIsUploading(true);
    setError({ type: '', text: '' });

    try {
      let updatedSettings = { 
        gcashNumber: gcashNumber.trim(),
        businessName: businessName.trim(),
        qrCodeUrl: currentQRUrl || ''
      };

      // Upload new QR code if provided
      if (qrFile) {
        const qrRef = ref(storage, `payment-settings/qr-code-${Date.now()}.jpg`);
        const uploadResult = await uploadBytes(qrRef, qrFile);
        const downloadURL = await getDownloadURL(qrRef);
        updatedSettings.qrCodeUrl = downloadURL;
      }

      // Update settings in Firestore
      await setDoc(doc(db, 'settings', 'payment'), updatedSettings, { merge: true });
      
      setBusinessName(updatedSettings.businessName);
      setGCashNumber(updatedSettings.gcashNumber);
      setCurrentQRUrl(updatedSettings.qrCodeUrl);
      setQrFile(null);
      
      // Reset file input
      const fileInput = document.getElementById('qr-upload');
      if (fileInput) {
        fileInput.value = '';
      }
      
      setError({ type: 'success', text: 'Payment settings updated successfully!' });
      
      // Clear message after 5 seconds
      setTimeout(() => setError({ type: '', text: '' }), 5000);
    } catch (error) {
      console.error('Error updating payment settings:', error);
      let errorMessage = 'Failed to update payment settings. ';
      
      if (error.code === 'permission-denied') {
        errorMessage += 'Permission denied. Please check your access rights.';
      } else if (error.code === 'unavailable') {
        errorMessage += 'Service temporarily unavailable. Please try again later.';
      } else {
        errorMessage += `Error: ${error.message}`;
      }
      
      setError({ type: 'error', text: errorMessage });
    }
    
    setIsUploading(false);
  };

  const handleQRFileChange = (event) => {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
      if (!validTypes.includes(file.type)) {
        setError({ 
          type: 'error', 
          text: 'Please select a valid image file (JPEG, PNG, or GIF).' 
        });
        event.target.value = ''; // Clear the input
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        setError({ 
          type: 'error', 
          text: 'File size must be less than 5MB.' 
        });
        event.target.value = ''; // Clear the input
        return;
      }

      setQrFile(file);
      setError({ type: '', text: '' }); // Clear any previous errors
    }
  };

  const handleGCashNumberChange = (e) => {
    const value = e.target.value;
    // Only allow numbers and ensure it starts with 09
    if (value === '' || /^09\d{0,9}$/.test(value)) {
      setGCashNumber(value);
    }
  };

  const handleBusinessNameChange = (e) => {
    const value = e.target.value;
    // Limit business name to 50 characters
    if (value.length <= 50) {
      setBusinessName(value);
    }
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    
    // Handle different timestamp formats
    if (timestamp.seconds) {
      // Firestore Timestamp object
      return new Date(timestamp.seconds * 1000).toLocaleString();
    } else if (timestamp instanceof Date) {
      // JavaScript Date object
      return timestamp.toLocaleString();
    } else if (typeof timestamp === 'number') {
      // Unix timestamp
      return new Date(timestamp).toLocaleString();
    } else {
      return 'Invalid Date';
    }
  };

  const formatAmount = (amount) => {
    return `₱${parseFloat(amount).toFixed(2)}`;
  };

  // Show loading while authentication is being checked
  if (authLoading) {
    return (
      <Container>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '50vh' 
        }}>
          <div>Loading authentication...</div>
        </div>
      </Container>
    );
  }

  // Show login prompt if not authenticated
  if (!isAuthenticated) {
    return (
      <Container>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '50vh',
          flexDirection: 'column'
        }}>
          <h2>Please log in to access the Payments Dashboard</h2>
          <p>You need to be authenticated to view payment information.</p>
        </div>
      </Container>
    );
  }

  if (loading) {
    return (
      <LoadingSpinner>
        Loading payments...
      </LoadingSpinner>
    );
  }

  return (
    <Container>
      <PageHeader>
        <Title>Payments Dashboard</Title>
      </PageHeader>

      {/* Payment Settings Section */}
      <PaymentSettingsSection>
        <SettingsTitle>Payment Settings Management</SettingsTitle>
        <SettingsGrid>
          <SettingCard>
            <SettingLabel>GCash Mobile Number</SettingLabel>
            <SettingInput
              type="text"
              value={gcashNumber}
              onChange={handleGCashNumberChange}
              placeholder="09123456789"
            />
            
            <SettingLabel style={{ marginTop: '15px' }}>Business Name</SettingLabel>
            <SettingInput
              type="text"
              value={businessName}
              onChange={handleBusinessNameChange}
              placeholder="Bambike Cycles"
            />
            
            <SaveButton
              onClick={handleSettingsUpdate}
              disabled={isUploading}
            >
              {isUploading ? 'Updating...' : 'Update Settings'}
            </SaveButton>
            
            {error && error.text && (
              error.type === 'success' ? (
                <SuccessMessage>{error.text}</SuccessMessage>
              ) : (
                <ErrorMessage>{error.text}</ErrorMessage>
              )
            )}
          </SettingCard>

          <SettingCard>
            <SettingLabel>QR Code Image</SettingLabel>
            <FileInputWrapper>
              <FileInput
                id="qr-upload"
                type="file"
                accept="image/*"
                onChange={handleQRFileChange}
              />
              <FileInputLabel htmlFor="qr-upload">
                {qrFile ? `Selected: ${qrFile.name}` : 'Click to upload new QR code'}
              </FileInputLabel>
            </FileInputWrapper>
            
            {currentQRUrl && (
              <CurrentQRPreview>
                <DetailLabel>Current QR Code:</DetailLabel>
                <QRImage 
                  src={currentQRUrl} 
                  alt="Current QR Code"
                  onClick={() => setSelectedImage(currentQRUrl)}
                />
              </CurrentQRPreview>
            )}
          </SettingCard>
        </SettingsGrid>
      </PaymentSettingsSection>

      {/* Statistics Cards */}
      <StatsContainer>
        <StatCard type="total">
          <StatValue>{stats.total}</StatValue>
          <StatLabel>Total Payments</StatLabel>
        </StatCard>
        <StatCard type="pending">
          <StatValue>{stats.pending}</StatValue>
          <StatLabel>Pending Review</StatLabel>
        </StatCard>
        <StatCard type="confirmed">
          <StatValue>{stats.confirmed}</StatValue>
          <StatLabel>Confirmed</StatLabel>
        </StatCard>
        <StatCard type="rejected">
          <StatValue>{stats.rejected}</StatValue>
          <StatLabel>Rejected</StatLabel>
        </StatCard>
      </StatsContainer>

      {/* Filters */}
      <FilterContainer>
        <FilterSelect
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">All Payments</option>
          <option value="PENDING">Pending</option>
          <option value="CONFIRMED">Confirmed</option>
          <option value="REJECTED">Rejected</option>
        </FilterSelect>
        <SearchInput
          type="text"
          placeholder="Search by mobile, reference, or bike type..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </FilterContainer>

      {/* Payments List */}
      {filteredPayments.length === 0 ? (
        <EmptyState>
          {loading ? 'Loading payments...' : 'No payments found.'}
        </EmptyState>
      ) : (
        <div>
          {filteredPayments.map(payment => (
            <PaymentCard
              key={payment.id}
              expanded={expandedPayment === payment.id}
              onClick={() => setExpandedPayment(expandedPayment === payment.id ? null : payment.id)}
            >
              <PaymentCardHeader expanded={expandedPayment === payment.id}>
                <PaymentIcon status={payment.status}>
                  {payment.status === 'PENDING' ? '⏳' : payment.status === 'CONFIRMED' ? '✓' : '✗'}
                </PaymentIcon>
                
                <PaymentMainInfo>
                  <PaymentTitle>{payment.bikeType} • {payment.duration}</PaymentTitle>
                  <PaymentSubtitle>
                    <span>#{payment.id.slice(-8)}</span>
                    <span>•</span>
                    <span>{payment.mobileNumber}</span>
                    <span>•</span>
                    <span>{formatDate(payment.createdAt)}</span>
                  </PaymentSubtitle>
                </PaymentMainInfo>
                
                <PaymentAmount>{formatAmount(payment.amount)}</PaymentAmount>
                
                <ExpandButton expanded={expandedPayment === payment.id}>
                  ↓
                </ExpandButton>
              </PaymentCardHeader>
              
              <PaymentDetails expanded={expandedPayment === payment.id}>
                <DetailGrid>
                  <DetailItem>
                    <DetailItemLabel>Payment ID</DetailItemLabel>
                    <DetailItemValue>{payment.id}</DetailItemValue>
                  </DetailItem>
                  
                  <DetailItem>
                    <DetailItemLabel>Reference Number</DetailItemLabel>
                    <DetailItemValue>{payment.referenceNumber}</DetailItemValue>
                  </DetailItem>
                  
                  <DetailItem>
                    <DetailItemLabel>Mobile Number</DetailItemLabel>
                    <DetailItemValue>{payment.mobileNumber}</DetailItemValue>
                  </DetailItem>
                  
                  <DetailItem>
                    <DetailItemLabel>Status</DetailItemLabel>
                    <StatusBadge status={payment.status}>{payment.status}</StatusBadge>
                  </DetailItem>
                  
                  {payment.processedAt && (
                    <DetailItem>
                      <DetailItemLabel>Processed Date</DetailItemLabel>
                      <DetailItemValue>{formatDate(payment.processedAt)}</DetailItemValue>
                    </DetailItem>
                  )}
                  
                  {payment.processedBy && (
                    <DetailItem>
                      <DetailItemLabel>Processed By</DetailItemLabel>
                      <DetailItemValue>{payment.processedBy}</DetailItemValue>
                    </DetailItem>
                  )}
                </DetailGrid>
                
                {payment.screenshotUrl && (
                  <DetailItem style={{ marginBottom: '16px' }}>
                    <DetailItemLabel>Payment Screenshot</DetailItemLabel>
                    <ScreenshotImage
                      src={payment.screenshotUrl}
                      alt="Payment screenshot"
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedImage(payment.screenshotUrl);
                      }}
                      style={{ width: '80px', height: '80px', marginTop: '8px' }}
                    />
                  </DetailItem>
                )}
                
                {payment.status === 'PENDING' && (
                  <ActionButtons>
                    <ActionButton
                      variant="confirm"
                      onClick={(e) => {
                        e.stopPropagation();
                        handlePaymentAction(payment.id, 'CONFIRMED');
                      }}
                      disabled={processingId === payment.id}
                    >
                      {processingId === payment.id ? 'Processing...' : 'Confirm Payment'}
                    </ActionButton>
                    <ActionButton
                      variant="reject"
                      onClick={(e) => {
                        e.stopPropagation();
                        handlePaymentAction(payment.id, 'REJECTED');
                      }}
                      disabled={processingId === payment.id}
                    >
                      {processingId === payment.id ? 'Processing...' : 'Reject Payment'}
                    </ActionButton>
                  </ActionButtons>
                )}
              </PaymentDetails>
            </PaymentCard>
          ))}
        </div>
      )}

      {/* Image Modal */}
      {selectedImage && (
        <Modal onClick={() => setSelectedImage(null)}>
          <ModalContent onClick={(e) => e.stopPropagation()}>
            <CloseButton onClick={() => setSelectedImage(null)}>×</CloseButton>
            <ModalImage
              src={selectedImage}
              alt="Payment screenshot"
            />
          </ModalContent>
        </Modal>
      )}
    </Container>
  );
};

export default PaymentsDashboard; 