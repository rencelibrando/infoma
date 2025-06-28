import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { auth, db } from '../firebase';
import { updatePassword, updateProfile, EmailAuthProvider, reauthenticateWithCredential } from 'firebase/auth';
import { doc, getDoc, updateDoc } from 'firebase/firestore';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { ref as dbRef, set as dbSet, get as dbGet, onValue, off } from 'firebase/database';
import { setDoc } from 'firebase/firestore';
import { storage, realtimeDb } from '../firebase';
import { FiUser, FiMail, FiLock, FiSave, FiX, FiEye, FiEyeOff, FiCamera, FiUpload, FiSettings, FiMapPin, FiDollarSign, FiChevronDown, FiChevronRight } from 'react-icons/fi';

// Pine green and gray theme colors
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  red: '#d32f2f',
  green: '#4caf50',
  blue: '#2196f3'
};

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
  z-index: 2000;
  padding: 20px;
`;

const ModalContent = styled.div`
  background-color: ${colors.white};
  border-radius: 12px;
  padding: 30px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  position: relative;
`;

const CloseButton = styled.button`
  position: absolute;
  top: 15px;
  right: 15px;
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: ${colors.mediumGray};
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
  
  &:hover {
    background-color: ${colors.lightGray};
    color: ${colors.red};
  }
`;

const Title = styled.h2`
  color: ${colors.darkGray};
  margin-bottom: 30px;
  font-size: 24px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
`;

const Section = styled.div`
  margin-bottom: 20px;
  background-color: ${colors.white};
  border-radius: 8px;
  border: 1px solid ${colors.lightGray};
  overflow: hidden;
`;

const SectionHeader = styled.div`
  padding: 15px 20px;
  background-color: ${colors.lightGray};
  border-bottom: ${props => props.expanded ? `1px solid ${colors.lightGray}` : 'none'};
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.3s ease;
  
  &:hover {
    background-color: #e8e8e8;
  }
`;

const SectionTitle = styled.h3`
  color: ${colors.pineGreen};
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const SectionContent = styled.div`
  padding: ${props => props.expanded ? '20px' : '0'};
  max-height: ${props => props.expanded ? '1000px' : '0'};
  overflow: hidden;
  transition: all 0.3s ease;
`;

const ChevronIcon = styled.div`
  transform: ${props => props.expanded ? 'rotate(180deg)' : 'rotate(0deg)'};
  transition: transform 0.3s ease;
  color: ${colors.mediumGray};
  display: flex;
  align-items: center;
`;

const FormGroup = styled.div`
  margin-bottom: 20px;
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  color: ${colors.darkGray};
  font-weight: 500;
  font-size: 14px;
`;

const Input = styled.input`
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  box-sizing: border-box;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 3px rgba(29, 60, 52, 0.1);
  }
  
  &:disabled {
    background-color: #f5f5f5;
    color: ${colors.mediumGray};
    cursor: not-allowed;
  }
`;

const PasswordInputContainer = styled.div`
  position: relative;
`;

const PasswordToggle = styled.button`
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: ${colors.mediumGray};
  font-size: 16px;
  padding: 4px;
  
  &:hover {
    color: ${colors.pineGreen};
  }
`;

const Button = styled.button`
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  
  ${props => props.primary && `
    background-color: ${colors.pineGreen};
    color: ${colors.white};
    
    &:hover {
      background-color: ${colors.lightPineGreen};
      transform: translateY(-1px);
    }
  `}
  
  ${props => props.secondary && `
    background-color: ${colors.lightGray};
    color: ${colors.darkGray};
    
    &:hover {
      background-color: #e0e0e0;
    }
  `}
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 15px;
  justify-content: flex-end;
  margin-top: 30px;
`;

const Alert = styled.div`
  padding: 12px 15px;
  border-radius: 6px;
  margin-bottom: 20px;
  font-size: 14px;
  
  ${props => props.type === 'success' && `
    background-color: #e8f5e9;
    color: ${colors.green};
    border: 1px solid ${colors.green};
  `}
  
  ${props => props.type === 'error' && `
    background-color: #ffebee;
    color: ${colors.red};
    border: 1px solid ${colors.red};
  `}
`;

const ProfilePictureContainer = styled.div`
  position: relative;
  margin-bottom: 20px;
`;

const ProfilePicture = styled.div`
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: ${props => props.imageUrl ? `url(${props.imageUrl})` : `linear-gradient(135deg, ${colors.pineGreen}, ${colors.lightPineGreen})`};
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 48px;
  font-weight: 600;
  text-transform: uppercase;
  border: 4px solid ${colors.white};
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  position: relative;
  overflow: hidden;
`;

const ProfilePictureOverlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  cursor: pointer;
  border-radius: 50%;
  
  &:hover {
    opacity: 1;
  }
`;

const CameraIcon = styled.div`
  font-size: 24px;
  color: white;
`;

const HiddenFileInput = styled.input`
  display: none;
`;

const UploadButton = styled.button`
  background-color: ${colors.pineGreen};
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
  
  &:hover {
    background-color: ${colors.lightPineGreen};
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const UploadProgress = styled.div`
  margin-top: 10px;
  color: ${colors.mediumGray};
  font-size: 12px;
  text-align: center;
`;

const SettingsGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

const ToggleSwitch = styled.label`
  position: relative;
  display: inline-block;
  width: 60px;
  height: 34px;
  
  input {
    opacity: 0;
    width: 0;
    height: 0;
  }
  
  span {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #ccc;
    transition: .4s;
    border-radius: 34px;
  }
  
  span:before {
    position: absolute;
    content: "";
    height: 26px;
    width: 26px;
    left: 4px;
    bottom: 4px;
    background-color: white;
    transition: .4s;
    border-radius: 50%;
  }
  
  input:checked + span {
    background-color: ${colors.pineGreen};
  }
  
  input:focus + span {
    box-shadow: 0 0 1px ${colors.pineGreen};
  }
  
  input:checked + span:before {
    transform: translateX(26px);
  }
`;

const ToggleContainer = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 0;
`;

const ToggleInfo = styled.div`
  flex: 1;
  margin-right: 15px;
`;

const ToggleTitle = styled.div`
  font-weight: 600;
  color: ${colors.darkGray};
  margin-bottom: 4px;
`;

const ToggleDescription = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  line-height: 1.4;
`;

const FileUploadArea = styled.div`
  border: 2px dashed rgba(29, 60, 52, 0.2);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: ${colors.white};
  
  &:hover {
    border-color: ${colors.pineGreen};
    background: ${colors.lightGray};
  }
`;

const QRPreview = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
`;

const QRImage = styled.img`
  max-width: 120px;
  border-radius: 8px;
  border: 2px solid rgba(29, 60, 52, 0.1);
  transition: all 0.3s ease;
  
  &:hover {
    transform: scale(1.05);
    border-color: ${colors.pineGreen};
  }
`;

const StatusIndicator = styled.div`
  display: flex;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
  
  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: 6px;
    background-color: ${props => props.active ? colors.green : colors.red};
  }
`;

const AdminProfile = ({ isOpen, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [showPasswords, setShowPasswords] = useState({});
  
  // Profile data
  const [profileData, setProfileData] = useState({
    displayName: '',
    email: '',
    phoneNumber: '',
    role: 'admin',
    photoURL: ''
  });
  
  // Password change data
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  // Payment settings data
  const [paymentSettings, setPaymentSettings] = useState({
    gcashNumber: '',
    businessName: '',
    qrCodeUrl: ''
  });

  // Tracking settings data
  const [trackingSettings, setTrackingSettings] = useState({
    locationRestrictionEnabled: true
  });

  // Loading states for settings
  const [savingPaymentSettings, setSavingPaymentSettings] = useState(false);
  const [savingTrackingSettings, setSavingTrackingSettings] = useState(false);
  const [uploadingQR, setUploadingQR] = useState(false);

  // Collapsible sections state
  const [expandedSections, setExpandedSections] = useState({
    profile: true,      // Profile info expanded by default
    picture: false,     // Picture collapsed by default
    password: false,    // Password collapsed by default
    payments: false,    // Payment settings collapsed by default
    tracking: false     // Tracking settings collapsed by default
  });

  useEffect(() => {
    if (isOpen && auth.currentUser) {
      loadProfileData();
      loadPaymentSettings();
      loadTrackingSettings();
    }
  }, [isOpen]);

  const loadProfileData = async () => {
    try {
      const user = auth.currentUser;
      if (!user) return;

      // Load from Firebase Auth
      setProfileData(prev => ({
        ...prev,
        displayName: user.displayName || '',
        email: user.email || '',
        photoURL: user.photoURL || ''
      }));

      // Load additional data from Firestore
      try {
        const userDoc = await getDoc(doc(db, 'users', user.uid));
        if (userDoc.exists()) {
          const userData = userDoc.data();
          setProfileData(prev => ({
            ...prev,
            phoneNumber: userData.phoneNumber || '',
            role: userData.role || 'admin'
          }));
        }
      } catch (error) {
        console.log('Could not load additional profile data:', error.message);
      }
    } catch (error) {
      setMessage({ type: 'error', text: 'Failed to load profile data' });
    }
  };

  const handleProfileUpdate = async () => {
    if (!auth.currentUser) return;
    
    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      // Update Firebase Auth profile
      await updateProfile(auth.currentUser, {
        displayName: profileData.displayName,
        photoURL: profileData.photoURL
      });

      // Update Firestore document
      const userRef = doc(db, 'users', auth.currentUser.uid);
      await updateDoc(userRef, {
        displayName: profileData.displayName,
        phoneNumber: profileData.phoneNumber,
        role: profileData.role,
        photoURL: profileData.photoURL,
        lastUpdated: new Date()
      });

      setMessage({ type: 'success', text: 'Profile updated successfully!' });
    } catch (error) {
      console.error('Error updating profile:', error);
      setMessage({ type: 'error', text: error.message });
    } finally {
      setLoading(false);
    }
  };

  const handleImageUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      setMessage({ type: 'error', text: 'Please select a valid image file' });
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setMessage({ type: 'error', text: 'Image size must be less than 5MB' });
      return;
    }

    setUploadingImage(true);
    setMessage({ type: '', text: '' });

    try {
      // Create a reference to the file in Firebase Storage
      const storageRef = ref(storage, `profile-pictures/${auth.currentUser.uid}/${Date.now()}_${file.name}`);
      
      // Upload the file
      await uploadBytes(storageRef, file);
      
      // Get the download URL
      const downloadURL = await getDownloadURL(storageRef);
      
      // Update profile data state
      setProfileData(prev => ({
        ...prev,
        photoURL: downloadURL
      }));

      setMessage({ type: 'success', text: 'Profile picture uploaded successfully! Click "Update Profile" to save.' });
    } catch (error) {
      console.error('Error uploading image:', error);
      setMessage({ type: 'error', text: 'Failed to upload image. Please try again.' });
    } finally {
      setUploadingImage(false);
    }
  };

  const getInitials = () => {
    if (profileData.displayName) {
      return profileData.displayName
        .split(' ')
        .map(name => name.charAt(0))
        .slice(0, 2)
        .join('');
    }
    if (profileData.email) {
      return profileData.email.charAt(0).toUpperCase();
    }
    return 'A';
  };

  const loadPaymentSettings = async () => {
    try {
      const settingsDoc = await getDoc(doc(db, 'settings', 'payment'));
      if (settingsDoc.exists()) {
        const data = settingsDoc.data();
        setPaymentSettings({
          gcashNumber: data.gcashNumber || '',
          businessName: data.businessName || '',
          qrCodeUrl: data.qrCodeUrl || ''
        });
      }
    } catch (error) {
      console.error('Error loading payment settings:', error);
    }
  };

  const loadTrackingSettings = async () => {
    try {
      const settingsRef = dbRef(realtimeDb, 'app_config');
      const snapshot = await dbGet(settingsRef);
      if (snapshot.exists()) {
        const data = snapshot.val();
        setTrackingSettings({
          locationRestrictionEnabled: data.locationRestrictionEnabled !== false
        });
      }
    } catch (error) {
      console.error('Error loading tracking settings:', error);
    }
  };

  const handlePaymentSettingsUpdate = async () => {
    setSavingPaymentSettings(true);
    setMessage({ type: '', text: '' });

    try {
      await setDoc(doc(db, 'settings', 'payment'), {
        ...paymentSettings,
        updatedAt: new Date(),
        updatedBy: auth.currentUser.uid
      }, { merge: true });

      setMessage({ type: 'success', text: 'Payment settings updated successfully!' });
    } catch (error) {
      console.error('Error updating payment settings:', error);
      setMessage({ type: 'error', text: 'Failed to update payment settings' });
    } finally {
      setSavingPaymentSettings(false);
    }
  };

  const handleTrackingSettingsUpdate = async () => {
    setSavingTrackingSettings(true);
    setMessage({ type: '', text: '' });

    try {
      // Save to Realtime Database
      const rtdbRef = dbRef(realtimeDb, 'app_config/locationRestrictionEnabled');
      await dbSet(rtdbRef, trackingSettings.locationRestrictionEnabled);

      // Also save to Firestore for backup
      await setDoc(doc(db, 'app_config', 'settings'), {
        locationRestrictionEnabled: trackingSettings.locationRestrictionEnabled,
        updatedAt: new Date(),
        updatedBy: auth.currentUser.uid
      }, { merge: true });

      setMessage({ type: 'success', text: 'Tracking settings updated successfully!' });
    } catch (error) {
      console.error('Error updating tracking settings:', error);
      setMessage({ type: 'error', text: 'Failed to update tracking settings' });
    } finally {
      setSavingTrackingSettings(false);
    }
  };

  const handleQRUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setMessage({ type: 'error', text: 'Please select a valid image file' });
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setMessage({ type: 'error', text: 'Image size must be less than 5MB' });
      return;
    }

    setUploadingQR(true);
    setMessage({ type: '', text: '' });

    try {
      const qrRef = ref(storage, `payment-settings/qr-code-${Date.now()}.jpg`);
      await uploadBytes(qrRef, file);
      const downloadURL = await getDownloadURL(qrRef);
      
      setPaymentSettings(prev => ({
        ...prev,
        qrCodeUrl: downloadURL
      }));

      setMessage({ type: 'success', text: 'QR code uploaded successfully! Click "Update Payment Settings" to save.' });
    } catch (error) {
      console.error('Error uploading QR code:', error);
      setMessage({ type: 'error', text: 'Failed to upload QR code' });
    } finally {
      setUploadingQR(false);
    }
  };

  const handlePasswordChange = async () => {
    if (!auth.currentUser) return;
    
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setMessage({ type: 'error', text: 'New passwords do not match' });
      return;
    }

    if (passwordData.newPassword.length < 6) {
      setMessage({ type: 'error', text: 'New password must be at least 6 characters' });
      return;
    }

    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      // Reauthenticate user first
      const credential = EmailAuthProvider.credential(
        auth.currentUser.email,
        passwordData.currentPassword
      );
      await reauthenticateWithCredential(auth.currentUser, credential);

      // Update password
      await updatePassword(auth.currentUser, passwordData.newPassword);

      setMessage({ type: 'success', text: 'Password changed successfully!' });
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });
    } catch (error) {
      console.error('Error changing password:', error);
      let errorMessage = 'Failed to change password';
      if (error.code === 'auth/wrong-password') {
        errorMessage = 'Current password is incorrect';
      } else if (error.code === 'auth/weak-password') {
        errorMessage = 'New password is too weak';
      }
      setMessage({ type: 'error', text: errorMessage });
    } finally {
      setLoading(false);
    }
  };

  const togglePasswordVisibility = (field) => {
    setShowPasswords(prev => ({
      ...prev,
      [field]: !prev[field]
    }));
  };

  const toggleSection = (sectionKey) => {
    setExpandedSections(prev => ({
      ...prev,
      [sectionKey]: !prev[sectionKey]
    }));
  };

  const handleClose = () => {
    setMessage({ type: '', text: '' });
    setPasswordData({
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    });
    // Reset any unsaved changes to settings
    if (isOpen) {
      loadPaymentSettings();
      loadTrackingSettings();
    }
    onClose();
  };

  if (!isOpen) return null;

  return (
    <Modal onClick={handleClose}>
      <ModalContent onClick={(e) => e.stopPropagation()}>
        <CloseButton onClick={handleClose}>
          <FiX />
        </CloseButton>

        <Title>
          <FiUser />
          Admin Profile & Settings
        </Title>

        {message.text && (
          <Alert type={message.type}>
            {message.text}
          </Alert>
        )}

        {/* Profile Picture Section */}
        <Section>
          <SectionHeader 
            expanded={expandedSections.picture} 
            onClick={() => toggleSection('picture')}
          >
            <SectionTitle>
              <FiCamera />
              Profile Picture
            </SectionTitle>
            <ChevronIcon expanded={expandedSections.picture}>
              <FiChevronDown />
            </ChevronIcon>
          </SectionHeader>
          
          <SectionContent expanded={expandedSections.picture}>
            <div style={{ textAlign: 'center', marginBottom: '20px' }}>
              <ProfilePictureContainer>
                <ProfilePicture imageUrl={profileData.photoURL}>
                  {!profileData.photoURL && getInitials()}
                  <ProfilePictureOverlay onClick={() => document.getElementById('profile-picture-input').click()}>
                    <CameraIcon>
                      <FiCamera />
                    </CameraIcon>
                  </ProfilePictureOverlay>
                </ProfilePicture>
              </ProfilePictureContainer>

              <HiddenFileInput
                id="profile-picture-input"
                type="file"
                accept="image/*"
                onChange={handleImageUpload}
                disabled={uploadingImage}
              />

              <UploadButton 
                onClick={() => document.getElementById('profile-picture-input').click()}
                disabled={uploadingImage}
              >
                <FiUpload />
                {uploadingImage ? 'Uploading...' : 'Choose New Picture'}
              </UploadButton>

              {uploadingImage && (
                <UploadProgress>
                  Uploading your profile picture...
                </UploadProgress>
              )}
            </div>
          </SectionContent>
        </Section>

        {/* Profile Information Section */}
        <Section>
          <SectionHeader 
            expanded={expandedSections.profile} 
            onClick={() => toggleSection('profile')}
          >
            <SectionTitle>
              <FiUser />
              Profile Information
            </SectionTitle>
            <ChevronIcon expanded={expandedSections.profile}>
              <FiChevronDown />
            </ChevronIcon>
          </SectionHeader>
          
          <SectionContent expanded={expandedSections.profile}>
            <FormGroup>
              <Label>Display Name</Label>
              <Input
                type="text"
                value={profileData.displayName}
                onChange={(e) => setProfileData(prev => ({
                  ...prev,
                  displayName: e.target.value
                }))}
                placeholder="Enter your display name"
              />
            </FormGroup>

            <FormGroup>
              <Label>Email Address</Label>
              <Input
                type="email"
                value={profileData.email}
                disabled
                placeholder="Email cannot be changed"
              />
            </FormGroup>

            <FormGroup>
              <Label>Phone Number</Label>
              <Input
                type="tel"
                value={profileData.phoneNumber}
                onChange={(e) => setProfileData(prev => ({
                  ...prev,
                  phoneNumber: e.target.value
                }))}
                placeholder="Enter your phone number"
              />
            </FormGroup>

            <FormGroup>
              <Label>Role</Label>
              <Input
                type="text"
                value={profileData.role}
                disabled
                placeholder="Role cannot be changed"
              />
            </FormGroup>

            <Button 
              primary 
              onClick={handleProfileUpdate} 
              disabled={loading}
            >
              <FiSave />
              Update Profile
            </Button>
          </SectionContent>
        </Section>

        {/* Password Change Section */}
        <Section>
          <SectionHeader 
            expanded={expandedSections.password} 
            onClick={() => toggleSection('password')}
          >
            <SectionTitle>
              <FiLock />
              Change Password
            </SectionTitle>
            <ChevronIcon expanded={expandedSections.password}>
              <FiChevronDown />
            </ChevronIcon>
          </SectionHeader>

          <SectionContent expanded={expandedSections.password}>
            <FormGroup>
              <Label>Current Password</Label>
              <PasswordInputContainer>
                <Input
                  type={showPasswords.current ? "text" : "password"}
                  value={passwordData.currentPassword}
                  onChange={(e) => setPasswordData(prev => ({
                    ...prev,
                    currentPassword: e.target.value
                  }))}
                  placeholder="Enter current password"
                />
                <PasswordToggle 
                  type="button"
                  onClick={() => togglePasswordVisibility('current')}
                >
                  {showPasswords.current ? <FiEyeOff /> : <FiEye />}
                </PasswordToggle>
              </PasswordInputContainer>
            </FormGroup>

            <FormGroup>
              <Label>New Password</Label>
              <PasswordInputContainer>
                <Input
                  type={showPasswords.new ? "text" : "password"}
                  value={passwordData.newPassword}
                  onChange={(e) => setPasswordData(prev => ({
                    ...prev,
                    newPassword: e.target.value
                  }))}
                  placeholder="Enter new password (min 6 characters)"
                />
                <PasswordToggle 
                  type="button"
                  onClick={() => togglePasswordVisibility('new')}
                >
                  {showPasswords.new ? <FiEyeOff /> : <FiEye />}
                </PasswordToggle>
              </PasswordInputContainer>
            </FormGroup>

            <FormGroup>
              <Label>Confirm New Password</Label>
              <PasswordInputContainer>
                <Input
                  type={showPasswords.confirm ? "text" : "password"}
                  value={passwordData.confirmPassword}
                  onChange={(e) => setPasswordData(prev => ({
                    ...prev,
                    confirmPassword: e.target.value
                  }))}
                  placeholder="Confirm new password"
                />
                <PasswordToggle 
                  type="button"
                  onClick={() => togglePasswordVisibility('confirm')}
                >
                  {showPasswords.confirm ? <FiEyeOff /> : <FiEye />}
                </PasswordToggle>
              </PasswordInputContainer>
            </FormGroup>

            <Button 
              primary 
              onClick={handlePasswordChange} 
              disabled={loading || !passwordData.currentPassword || !passwordData.newPassword}
            >
              <FiLock />
              Change Password
            </Button>
          </SectionContent>
        </Section>

        {/* Payment Settings Section */}
        <Section>
          <SectionHeader 
            expanded={expandedSections.payments} 
            onClick={() => toggleSection('payments')}
          >
            <SectionTitle>
              <FiDollarSign />
              Payment Settings
            </SectionTitle>
            <ChevronIcon expanded={expandedSections.payments}>
              <FiChevronDown />
            </ChevronIcon>
          </SectionHeader>

          <SectionContent expanded={expandedSections.payments}>
            <SettingsGrid>
              <FormGroup>
                <Label>Business Name</Label>
                <Input
                  type="text"
                  value={paymentSettings.businessName}
                  onChange={(e) => setPaymentSettings(prev => ({
                    ...prev,
                    businessName: e.target.value
                  }))}
                  placeholder="Enter business name"
                />
              </FormGroup>

              <FormGroup>
                <Label>GCash Number</Label>
                <Input
                  type="text"
                  value={paymentSettings.gcashNumber}
                  onChange={(e) => setPaymentSettings(prev => ({
                    ...prev,
                    gcashNumber: e.target.value
                  }))}
                  placeholder="Enter GCash number"
                />
              </FormGroup>
            </SettingsGrid>

            <FormGroup>
              <Label>Payment QR Code</Label>
              <FileUploadArea onClick={() => document.getElementById('qr-upload').click()}>
                <FiUpload size={24} color={colors.pineGreen} />
                <p style={{ margin: '8px 0 4px', fontWeight: '600' }}>
                  {uploadingQR ? 'Uploading...' : 'Click to upload QR code'}
                </p>
                <p style={{ margin: 0, fontSize: '12px', color: colors.mediumGray }}>
                  PNG, JPG up to 5MB
                </p>
              </FileUploadArea>
              
              <HiddenFileInput
                id="qr-upload"
                type="file"
                accept="image/*"
                onChange={handleQRUpload}
                disabled={uploadingQR}
              />

              {paymentSettings.qrCodeUrl && (
                <QRPreview>
                  <QRImage src={paymentSettings.qrCodeUrl} alt="Payment QR Code" />
                </QRPreview>
              )}
            </FormGroup>

            <Button 
              primary 
              onClick={handlePaymentSettingsUpdate} 
              disabled={savingPaymentSettings}
            >
              <FiSave />
              {savingPaymentSettings ? 'Saving...' : 'Update Payment Settings'}
            </Button>
          </SectionContent>
        </Section>

        {/* Location Tracking Settings Section */}
        <Section>
          <SectionHeader 
            expanded={expandedSections.tracking} 
            onClick={() => toggleSection('tracking')}
          >
            <SectionTitle>
              <FiMapPin />
              Location Tracking Settings
            </SectionTitle>
            <ChevronIcon expanded={expandedSections.tracking}>
              <FiChevronDown />
            </ChevronIcon>
          </SectionHeader>

          <SectionContent expanded={expandedSections.tracking}>
            <ToggleContainer>
              <ToggleInfo>
                <ToggleTitle>Location Restriction</ToggleTitle>
                <ToggleDescription>
                  When enabled, users must be within the Intramuros area to start rides.
                  Disabling will remove the location boundary restriction for all users.
                </ToggleDescription>
                {trackingSettings.locationRestrictionEnabled ? (
                  <StatusIndicator active={true}>
                    <span />
                    <em>Location restriction is currently enabled</em>
                  </StatusIndicator>
                ) : (
                  <StatusIndicator active={false}>
                    <span />
                    <em>Location restriction is currently disabled</em>
                  </StatusIndicator>
                )}
              </ToggleInfo>
              <ToggleSwitch>
                <input 
                  type="checkbox" 
                  checked={trackingSettings.locationRestrictionEnabled}
                  onChange={(e) => setTrackingSettings(prev => ({
                    ...prev,
                    locationRestrictionEnabled: e.target.checked
                  }))}
                />
                <span></span>
              </ToggleSwitch>
            </ToggleContainer>

            <Button 
              primary 
              onClick={handleTrackingSettingsUpdate} 
              disabled={savingTrackingSettings}
            >
              <FiSave />
              {savingTrackingSettings ? 'Saving...' : 'Update Tracking Settings'}
            </Button>
          </SectionContent>
        </Section>

        <ButtonGroup>
          <Button secondary onClick={handleClose}>
            Close
          </Button>
        </ButtonGroup>
      </ModalContent>
    </Modal>
  );
};

export default AdminProfile; 