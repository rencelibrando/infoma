import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { format } from 'date-fns';
import { getMessages, updateMessageStatus, sendResponse, getFaqs, addFaq, updateFaq, deleteFaq } from '../services/supportService';

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
  warning: '#FFA000',
  error: '#F44336'
};

// SVG Icons
const IconMessage = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M20 4H4C2.9 4 2.01 4.9 2.01 6L2 18C2 19.1 2.9 20 4 20H20C21.1 20 22 19.1 22 18V6C22 4.9 21.1 4 20 4ZM20 18H4V8L12 13L20 8V18ZM12 11L4 6H20L12 11Z" fill="currentColor"/>
  </svg>
);

const IconFaq = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM12 20C7.59 20 4 16.41 4 12C4 7.59 7.59 4 12 4C16.41 4 20 7.59 20 12C20 16.41 16.41 20 12 20ZM12 16C12.83 16 13.5 15.33 13.5 14.5C13.5 13.67 12.83 13 12 13C11.17 13 10.5 13.67 10.5 14.5C10.5 15.33 11.17 16 12 16ZM11 12H13V7H11V12Z" fill="currentColor"/>
  </svg>
);

const IconClock = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M11.99 2C6.47 2 2 6.48 2 12C2 17.52 6.47 22 11.99 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 11.99 2ZM12 20C7.58 20 4 16.42 4 12C4 7.58 7.58 4 12 4C16.42 4 20 7.58 20 12C20 16.42 16.42 20 12 20ZM12.5 7H11V13L16.25 16.15L17 14.92L12.5 12.25V7Z" fill="currentColor"/>
  </svg>
);

const IconAdd = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M19 13H13V19H11V13H5V11H11V5H13V11H19V13Z" fill="currentColor"/>
  </svg>
);

const IconEdit = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M3 17.25V21H6.75L17.81 9.94L14.06 6.19L3 17.25ZM20.71 7.04C21.1 6.65 21.1 6.02 20.71 5.63L18.37 3.29C17.98 2.9 17.35 2.9 16.96 3.29L15.13 5.12L18.88 8.87L20.71 7.04Z" fill="currentColor"/>
  </svg>
);

const IconDelete = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M6 19C6 20.1 6.9 21 8 21H16C17.1 21 18 20.1 18 19V7H6V19ZM19 4H15.5L14.5 3H9.5L8.5 4H5V6H19V4Z" fill="currentColor"/>
  </svg>
);

const IconSave = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M9 16.2L4.8 12L3.4 13.4L9 19L21 7L19.6 5.6L9 16.2Z" fill="currentColor"/>
  </svg>
);

const Container = styled.div`
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
`;

const Title = styled.h2`
  margin-bottom: 24px;
  color: ${colors.darkGray};
  font-size: 24px;
  position: relative;
  padding-bottom: 10px;
  
  &:after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 40px;
    height: 3px;
    background-color: ${colors.pineGreen};
  }
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

const Button = styled.button`
  padding: 10px 16px;
  border: none;
  border-radius: 4px;
  background-color: ${props => props.secondary ? colors.lightGray : props.danger ? '#d32f2f' : colors.pineGreen};
  color: ${props => props.secondary ? colors.darkGray : colors.white};
  cursor: pointer;
  font-weight: ${props => props.bold ? 'bold' : 'normal'};
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
  
  &:hover {
    opacity: 0.9;
    transform: translateY(-1px);
    box-shadow: 0 2px 5px rgba(0,0,0,0.1);
  }
  
  &:active {
    transform: translateY(0);
    box-shadow: none;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }
`;

const MessagesContainer = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin-top: 20px;
  overflow: hidden;
`;

const MessageItem = styled.div`
  padding: 18px;
  border-bottom: 1px solid ${colors.lightGray};
  display: flex;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background-color: rgba(29, 60, 52, 0.05);
    transform: translateY(-2px);
    box-shadow: 0 2px 5px rgba(0,0,0,0.05);
  }
  
  &:last-child {
    border-bottom: none;
  }
`;

const MessageAvatar = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: ${colors.lightGray};
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: ${colors.mediumGray};
  margin-right: 15px;
  flex-shrink: 0;
  overflow: hidden;
`;

const AvatarImage = styled.img`
  width: 100%;
  height: 100%;
  object-fit: cover;
`;

const MessageContent = styled.div`
  flex: 1;
`;

const MessageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
`;

const UserName = styled.div`
  font-weight: 500;
  color: ${colors.darkGray};
`;

const MessageDate = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
`;

const MessagePreview = styled.div`
  color: ${colors.mediumGray};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 700px;
`;

const StatusBadge = styled.span`
  display: inline-block;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  margin-left: 10px;
  background-color: ${props => {
    switch (props.status) {
      case 'new': return 'rgba(244, 67, 54, 0.1)';
      case 'in-progress': return 'rgba(255, 160, 0, 0.1)';
      case 'resolved': return 'rgba(76, 175, 80, 0.1)';
      default: return 'rgba(0, 0, 0, 0.05)';
    }
  }};
  color: ${props => {
    switch (props.status) {
      case 'new': return colors.error;
      case 'in-progress': return colors.warning;
      case 'resolved': return colors.success;
      default: return colors.mediumGray;
    }
  }};
`;

const Modal = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
`;

const ModalContent = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 20px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
`;

const ModalHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid ${colors.lightGray};
`;

const ModalTitle = styled.h3`
  margin: 0;
  color: ${colors.darkGray};
`;

const CloseButton = styled.button`
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: ${colors.mediumGray};
  
  &:hover {
    color: ${colors.darkGray};
  }
`;

const MessageDetails = styled.div`
  margin-bottom: 30px;
`;

const UserInfo = styled.div`
  display: flex;
  margin-bottom: 20px;
  padding: 15px;
  background-color: ${colors.lightGray};
  border-radius: 8px;
`;

const UserDetails = styled.div`
  margin-left: 15px;
`;

const UserInfoItem = styled.div`
  margin-bottom: 5px;
  
  strong {
    color: ${colors.darkGray};
    margin-right: 5px;
  }
`;

const FullMessage = styled.div`
  padding: 15px;
  border: 1px solid ${colors.lightGray};
  border-radius: 8px;
  line-height: 1.6;
  color: ${colors.darkGray};
  margin-bottom: 20px;
`;

const ResponseSection = styled.div`
  margin-top: 20px;
`;

const ResponseTextarea = styled.textarea`
  width: 100%;
  padding: 15px;
  border: 1px solid ${colors.lightGray};
  border-radius: 8px;
  min-height: 150px;
  resize: vertical;
  margin-bottom: 15px;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 2px rgba(29, 60, 52, 0.1);
  }
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 20px;
`;

const LoadingMessage = styled.div`
  text-align: center;
  padding: 20px;
  font-style: italic;
  color: ${colors.mediumGray};
`;

const LoadingSpinner = styled.div`
  border: 3px solid ${colors.lightGray};
  border-top: 3px solid ${colors.pineGreen};
  border-radius: 50%;
  width: 24px;
  height: 24px;
  animation: spin 1s linear infinite;
  margin-right: 10px;
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 60px 20px;
  color: ${colors.mediumGray};
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
`;

const EmptyStateIcon = styled.div`
  width: 60px;
  height: 60px;
  background-color: ${colors.lightGray};
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  
  svg {
    width: 30px;
    height: 30px;
    color: ${colors.mediumGray};
  }
`;

const TabsContainer = styled.div`
  display: flex;
  margin-bottom: 30px;
  border-bottom: 1px solid ${colors.lightGray};
  overflow-x: auto;
  scrollbar-width: none;
  
  &::-webkit-scrollbar {
    display: none;
  }
  
  @media (max-width: 576px) {
    padding-bottom: 5px;
  }
`;

const Tab = styled.div`
  padding: 12px 20px;
  cursor: pointer;
  font-weight: ${props => props.active ? 'bold' : 'normal'};
  color: ${props => props.active ? colors.pineGreen : colors.mediumGray};
  border-bottom: 2px solid ${props => props.active ? colors.pineGreen : 'transparent'};
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  transition: all 0.2s ease;
  
  &:hover {
    color: ${props => props.active ? colors.pineGreen : colors.darkGray};
  }
`;

const FAQContainer = styled.div`
  margin-top: 20px;
`;

const FAQItem = styled.div`
  margin-bottom: 15px;
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  transition: all 0.2s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 3px 15px rgba(0, 0, 0, 0.08);
  }
`;

const FAQHeader = styled.div`
  padding: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  background-color: ${props => props.isOpen ? colors.lightGray : colors.white};
  transition: background-color 0.2s ease;
  
  &:hover {
    background-color: ${colors.lightGray};
  }
`;

const FAQTitle = styled.div`
  font-weight: 500;
  color: ${colors.darkGray};
`;

const FAQContent = styled.div`
  padding: ${props => props.isOpen ? '18px' : '0'};
  height: ${props => props.isOpen ? 'auto' : '0'};
  overflow: hidden;
  transition: all 0.3s ease;
  border-top: ${props => props.isOpen ? `1px solid ${colors.lightGray}` : 'none'};
  line-height: 1.6;
`;

const FAQActions = styled.div`
  display: flex;
  gap: 12px;
  align-items: center;
`;

const IconButton = styled.button`
  background: none;
  border: none;
  color: ${colors.mediumGray};
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  transition: all 0.2s ease;
  
  &:hover {
    color: ${colors.darkGray};
    background-color: rgba(0, 0, 0, 0.05);
  }
`;

const FAQEditor = styled.div`
  margin-top: 20px;
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  padding: 20px;
  margin-bottom: 20px;
`;

const FormTitle = styled.h3`
  margin-bottom: 20px;
  color: ${colors.darkGray};
  font-size: 18px;
  position: relative;
  padding-bottom: 10px;
  
  &:after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 30px;
    height: 2px;
    background-color: ${colors.pineGreen};
  }
`;

const OperatingHoursContainer = styled.div`
  margin-top: 20px;
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  padding: 24px;
  position: relative;
`;

const OperatingHoursTable = styled.table`
  width: 100%;
  border-collapse: collapse;
  margin-top: 15px;
`;

const OperatingHoursRow = styled.tr`
  &:nth-child(even) {
    background-color: ${colors.lightGray};
  }
  
  transition: background-color 0.2s ease;
  
  &:hover {
    background-color: rgba(29, 60, 52, 0.05);
  }
`;

const OperatingHoursCell = styled.td`
  padding: 12px;
  border-bottom: 1px solid ${colors.lightGray};
`;

const OperatingHoursHead = styled.th`
  padding: 12px;
  text-align: left;
  font-weight: 500;
  border-bottom: 2px solid ${colors.lightGray};
  color: ${colors.darkGray};
`;

const TimeInput = styled.input`
  width: 100px;
  padding: 8px;
  border: 1px solid ${colors.lightGray};
  border-radius: 4px;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
`;

const Input = styled.input`
  display: block;
  width: 100%;
  padding: 12px 15px;
  margin-bottom: 15px;
  border: 1px solid ${colors.lightGray};
  border-radius: 4px;
  font-size: 14px;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 2px rgba(29, 60, 52, 0.1);
  }
`;

const Textarea = styled.textarea`
  display: block;
  width: 100%;
  padding: 12px 15px;
  margin-bottom: 15px;
  border: 1px solid ${colors.lightGray};
  border-radius: 4px;
  font-size: 14px;
  min-height: 120px;
  resize: vertical;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 2px rgba(29, 60, 52, 0.1);
  }
`;

// Helper function to get user initials
const getUserInitials = (name) => {
  if (!name) return '?';
  return name.split(' ').map(n => n[0]).join('').toUpperCase();
};

const CustomerSupportMessages = () => {
  const [messages, setMessages] = useState([]);
  const [filteredMessages, setFilteredMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [selectedMessage, setSelectedMessage] = useState(null);
  const [responseText, setResponseText] = useState('');
  
  // New state for tabs, FAQs, and operating hours
  const [activeTab, setActiveTab] = useState('messages');
  const [faqs, setFaqs] = useState([]);
  const [operatingHours, setOperatingHours] = useState([]);
  const [editingFAQ, setEditingFAQ] = useState(null);
  const [newFAQ, setNewFAQ] = useState({ question: '', answer: '' });
  const [openFAQs, setOpenFAQs] = useState({});
  const [editingHours, setEditingHours] = useState(false);
  const [tempOperatingHours, setTempOperatingHours] = useState([]);
  const [loadingOperation, setLoadingOperation] = useState({
    messages: false,
    faqs: false,
    saveResponse: false
  });

  // Fetch messages from Firebase
  useEffect(() => {
    const fetchMessages = async () => {
      setLoading(true);
      try {
        const messagesData = await getMessages();
        setMessages(messagesData);
        setFilteredMessages(messagesData);
      } catch (error) {
        console.error('Error fetching messages:', error);
      } finally {
        setLoading(false);
      }
    };
    
    // Fetch FAQs
    const fetchFaqs = async () => {
      setLoadingOperation(prev => ({ ...prev, faqs: true }));
      try {
        const faqData = await getFaqs();
        setFaqs(faqData);
      } catch (error) {
        console.error('Error fetching FAQs:', error);
      } finally {
        setLoadingOperation(prev => ({ ...prev, faqs: false }));
      }
    };
    
    fetchMessages();
    fetchFaqs();
    
    // For now, we'll keep using mock operating hours
    setOperatingHours([
      { day: 'Monday', open: '09:00', close: '18:00', closed: false },
      { day: 'Tuesday', open: '09:00', close: '18:00', closed: false },
      { day: 'Wednesday', open: '09:00', close: '18:00', closed: false },
      { day: 'Thursday', open: '09:00', close: '18:00', closed: false },
      { day: 'Friday', open: '09:00', close: '20:00', closed: false },
      { day: 'Saturday', open: '10:00', close: '16:00', closed: false },
      { day: 'Sunday', open: '10:00', close: '14:00', closed: true }
    ]);
    setTempOperatingHours([...operatingHours]);
  }, []);

  // Filter messages based on search term and status
  useEffect(() => {
    let result = messages;
    
    if (searchTerm) {
      const lowercasedSearch = searchTerm.toLowerCase();
      result = result.filter(message => 
        message.userName.toLowerCase().includes(lowercasedSearch) ||
        message.subject.toLowerCase().includes(lowercasedSearch) ||
        message.message.toLowerCase().includes(lowercasedSearch)
      );
    }
    
    if (statusFilter !== 'all') {
      result = result.filter(message => message.status === statusFilter);
    }
    
    setFilteredMessages(result);
  }, [searchTerm, statusFilter, messages]);

  const handleMessageClick = (message) => {
    setSelectedMessage(message);
  };

  const handleCloseModal = () => {
    setSelectedMessage(null);
    setResponseText('');
  };

  const handleSubmitResponse = async () => {
    if (!responseText.trim()) return;
    
    setLoadingOperation(prev => ({ ...prev, saveResponse: true }));
    try {
      await sendResponse(selectedMessage.id, responseText);
      
      // Update messages list
      const updatedMessages = messages.map(msg => 
        msg.id === selectedMessage.id 
          ? {...msg, response: responseText, status: 'resolved'} 
          : msg
      );
      setMessages(updatedMessages);
      
      handleCloseModal();
    } catch (error) {
      console.error('Error sending response:', error);
      alert('Failed to send response. Please try again.');
    } finally {
      setLoadingOperation(prev => ({ ...prev, saveResponse: false }));
    }
  };
  
  const handleStatusChange = async (messageId, newStatus) => {
    try {
      await updateMessageStatus(messageId, newStatus);
      
      // Update messages list
      const updatedMessages = messages.map(msg => 
        msg.id === messageId ? {...msg, status: newStatus} : msg
      );
      setMessages(updatedMessages);
      
      if (selectedMessage && selectedMessage.id === messageId) {
        setSelectedMessage({...selectedMessage, status: newStatus});
      }
    } catch (error) {
      console.error('Error updating message status:', error);
      alert('Failed to update status. Please try again.');
    }
  };
  
  // FAQ functions
  const toggleFAQ = (id) => {
    setOpenFAQs(prev => ({
      ...prev,
      [id]: !prev[id]
    }));
  };
  
  const handleEditFAQ = (faq) => {
    setEditingFAQ(faq);
  };
  
  const handleDeleteFAQ = async (id) => {
    if (window.confirm('Are you sure you want to delete this FAQ?')) {
      try {
        await deleteFaq(id);
        setFaqs(faqs.filter(faq => faq.id !== id));
      } catch (error) {
        console.error('Error deleting FAQ:', error);
        alert('Failed to delete FAQ. Please try again.');
      }
    }
  };
  
  const handleSaveFAQ = async () => {
    if (!editingFAQ.question || !editingFAQ.answer) {
      alert('Please fill in both question and answer fields.');
      return;
    }
    
    try {
      if (editingFAQ.id) {
        // Update existing FAQ
        await updateFaq(editingFAQ.id, {
          question: editingFAQ.question,
          answer: editingFAQ.answer
        });
        
        setFaqs(faqs.map(faq => 
          faq.id === editingFAQ.id ? editingFAQ : faq
        ));
      } else {
        // Add new FAQ
        const newFaqData = await addFaq({
          question: editingFAQ.question,
          answer: editingFAQ.answer
        });
        
        setFaqs([...faqs, newFaqData]);
      }
      
      setEditingFAQ(null);
      setNewFAQ({ question: '', answer: '' });
    } catch (error) {
      console.error('Error saving FAQ:', error);
      alert('Failed to save FAQ. Please try again.');
    }
  };
  
  const handleCancelFAQ = () => {
    setEditingFAQ(null);
    setNewFAQ({ question: '', answer: '' });
  };
  
  // Operating hours functions
  const handleEditHours = () => {
    setEditingHours(true);
    setTempOperatingHours([...operatingHours]);
  };
  
  const handleSaveHours = () => {
    // This will be implemented when backend is ready
    setOperatingHours([...tempOperatingHours]);
    setEditingHours(false);
  };
  
  const handleCancelHours = () => {
    setTempOperatingHours([...operatingHours]);
    setEditingHours(false);
  };
  
  const handleHoursChange = (index, field, value) => {
    const updatedHours = [...tempOperatingHours];
    
    if (field === 'closed') {
      updatedHours[index].closed = !updatedHours[index].closed;
    } else {
      updatedHours[index][field] = value;
    }
    
    setTempOperatingHours(updatedHours);
  };

  return (
    <Container>
      <Title>Customer Support</Title>
      
      <TabsContainer>
        <Tab 
          active={activeTab === 'messages'} 
          onClick={() => setActiveTab('messages')}
        >
          <IconMessage /> Messages
        </Tab>
        <Tab 
          active={activeTab === 'faqs'} 
          onClick={() => setActiveTab('faqs')}
        >
          <IconFaq /> FAQs
        </Tab>
        <Tab 
          active={activeTab === 'hours'} 
          onClick={() => setActiveTab('hours')}
        >
          <IconClock /> Operating Hours
        </Tab>
      </TabsContainer>
      
      {activeTab === 'messages' && (
        <>
          <SearchContainer>
            <SearchInput 
              type="text"
              placeholder="Search messages..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <Select 
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="all">All statuses</option>
              <option value="new">New</option>
              <option value="in-progress">In Progress</option>
              <option value="resolved">Resolved</option>
            </Select>
          </SearchContainer>
          
          <MessagesContainer>
            {loading ? (
              <LoadingMessage>
                <div style={{ display: 'flex', alignItems: 'center' }}>
                  <LoadingSpinner />
                  Loading messages...
                </div>
              </LoadingMessage>
            ) : filteredMessages.length === 0 ? (
              <EmptyState>
                <EmptyStateIcon>
                  <IconMessage />
                </EmptyStateIcon>
                <div>No messages found</div>
                <div style={{ fontSize: '14px', maxWidth: '400px' }}>
                  {searchTerm || statusFilter !== 'all' ? 
                    'Try adjusting your search or filter criteria' : 
                    'When customers send support messages, they will appear here'}
                </div>
              </EmptyState>
            ) : (
              filteredMessages.map(message => (
                <MessageItem key={message.id} onClick={() => handleMessageClick(message)}>
                  <MessageAvatar>
                    {message.userAvatar ? (
                      <AvatarImage src={message.userAvatar} alt={message.userName} />
                    ) : (
                      getUserInitials(message.userName)
                    )}
                  </MessageAvatar>
                  <MessageContent>
                    <MessageHeader>
                      <UserName>
                        {message.userName}
                        <StatusBadge status={message.status}>
                          {message.status === 'in-progress' ? 'In Progress' : message.status}
                        </StatusBadge>
                      </UserName>
                      <MessageDate>{format(message.dateCreated, 'MMM d, yyyy HH:mm')}</MessageDate>
                    </MessageHeader>
                    <div><strong>{message.subject}</strong></div>
                    <MessagePreview>{message.message}</MessagePreview>
                  </MessageContent>
                </MessageItem>
              ))
            )}
          </MessagesContainer>
          
          {selectedMessage && (
            <Modal>
              <ModalContent>
                <ModalHeader>
                  <ModalTitle>{selectedMessage.subject}</ModalTitle>
                  <CloseButton onClick={handleCloseModal}>&times;</CloseButton>
                </ModalHeader>
                
                <MessageDetails>
                  <UserInfo>
                    <MessageAvatar>
                      {selectedMessage.userAvatar ? (
                        <AvatarImage src={selectedMessage.userAvatar} alt={selectedMessage.userName} />
                      ) : (
                        getUserInitials(selectedMessage.userName)
                      )}
                    </MessageAvatar>
                    <UserDetails>
                      <UserInfoItem>
                        <strong>Name:</strong> {selectedMessage.userName}
                      </UserInfoItem>
                      <UserInfoItem>
                        <strong>Email:</strong> {selectedMessage.userEmail}
                      </UserInfoItem>
                      <UserInfoItem>
                        <strong>Phone:</strong> {selectedMessage.userPhone}
                      </UserInfoItem>
                      <UserInfoItem>
                        <strong>Date:</strong> {format(selectedMessage.dateCreated, 'MMM d, yyyy HH:mm')}
                      </UserInfoItem>
                      <UserInfoItem>
                        <strong>Status:</strong> 
                        <Select 
                          value={selectedMessage.status}
                          onChange={(e) => handleStatusChange(selectedMessage.id, e.target.value)}
                          style={{marginLeft: '10px', padding: '4px 8px'}}
                        >
                          <option value="new">New</option>
                          <option value="in-progress">In Progress</option>
                          <option value="resolved">Resolved</option>
                        </Select>
                      </UserInfoItem>
                    </UserDetails>
                  </UserInfo>
                  
                  <FullMessage>
                    {selectedMessage.message}
                  </FullMessage>
                  
                  <ResponseSection>
                    <FormTitle>Reply to this message</FormTitle>
                    <ResponseTextarea 
                      value={responseText}
                      onChange={(e) => setResponseText(e.target.value)}
                      placeholder="Type your response here..."
                    />
                    <ButtonGroup>
                      <Button secondary onClick={handleCloseModal}>Cancel</Button>
                      <Button onClick={handleSubmitResponse}>
                        <IconSave /> Send Response
                      </Button>
                    </ButtonGroup>
                  </ResponseSection>
                </MessageDetails>
              </ModalContent>
            </Modal>
          )}
        </>
      )}
      
      {activeTab === 'faqs' && (
        <FAQContainer>
          <Button onClick={() => setEditingFAQ({ id: null, question: '', answer: '' })}>
            <IconAdd /> Add New FAQ
          </Button>
          
          {editingFAQ && (
            <FAQEditor>
              <FormTitle>{editingFAQ.id ? 'Edit FAQ' : 'Add New FAQ'}</FormTitle>
              <Input 
                placeholder="Question"
                value={editingFAQ.question}
                onChange={(e) => setEditingFAQ({...editingFAQ, question: e.target.value})}
              />
              <Textarea 
                placeholder="Answer"
                value={editingFAQ.answer}
                onChange={(e) => setEditingFAQ({...editingFAQ, answer: e.target.value})}
              />
              <ButtonGroup>
                <Button secondary onClick={handleCancelFAQ}>Cancel</Button>
                <Button onClick={handleSaveFAQ}>
                  <IconSave /> Save
                </Button>
              </ButtonGroup>
            </FAQEditor>
          )}
          
          {faqs.length === 0 ? (
            <EmptyState>
              <EmptyStateIcon>
                <IconFaq />
              </EmptyStateIcon>
              <div>No FAQs found</div>
              <div style={{ fontSize: '14px' }}>Add one to get started</div>
            </EmptyState>
          ) : (
            faqs.map(faq => (
              <FAQItem key={faq.id}>
                <FAQHeader 
                  isOpen={openFAQs[faq.id]} 
                  onClick={() => toggleFAQ(faq.id)}
                >
                  <FAQTitle>{faq.question}</FAQTitle>
                  <FAQActions onClick={e => e.stopPropagation()}>
                    <IconButton onClick={() => handleEditFAQ(faq)}>
                      <IconEdit />
                    </IconButton>
                    <IconButton onClick={() => handleDeleteFAQ(faq.id)}>
                      <IconDelete />
                    </IconButton>
                    {openFAQs[faq.id] ? '▲' : '▼'}
                  </FAQActions>
                </FAQHeader>
                <FAQContent isOpen={openFAQs[faq.id]}>
                  {faq.answer}
                </FAQContent>
              </FAQItem>
            ))
          )}
        </FAQContainer>
      )}
      
      {activeTab === 'hours' && (
        <OperatingHoursContainer>
          <FormTitle>Operating Hours</FormTitle>
          
          {!editingHours ? (
            <>
              <OperatingHoursTable>
                <thead>
                  <tr>
                    <OperatingHoursHead>Day</OperatingHoursHead>
                    <OperatingHoursHead>Open</OperatingHoursHead>
                    <OperatingHoursHead>Close</OperatingHoursHead>
                    <OperatingHoursHead>Status</OperatingHoursHead>
                  </tr>
                </thead>
                <tbody>
                  {operatingHours.map(hours => (
                    <OperatingHoursRow key={hours.day}>
                      <OperatingHoursCell>{hours.day}</OperatingHoursCell>
                      <OperatingHoursCell>{hours.closed ? '-' : hours.open}</OperatingHoursCell>
                      <OperatingHoursCell>{hours.closed ? '-' : hours.close}</OperatingHoursCell>
                      <OperatingHoursCell>
                        <StatusBadge status={hours.closed ? 'closed' : 'open'}>
                          {hours.closed ? 'Closed' : 'Open'}
                        </StatusBadge>
                      </OperatingHoursCell>
                    </OperatingHoursRow>
                  ))}
                </tbody>
              </OperatingHoursTable>
              <Button 
                style={{ marginTop: '20px' }} 
                onClick={handleEditHours}
              >
                <IconEdit /> Edit Hours
              </Button>
            </>
          ) : (
            <>
              <OperatingHoursTable>
                <thead>
                  <tr>
                    <OperatingHoursHead>Day</OperatingHoursHead>
                    <OperatingHoursHead>Open</OperatingHoursHead>
                    <OperatingHoursHead>Close</OperatingHoursHead>
                    <OperatingHoursHead>Closed</OperatingHoursHead>
                  </tr>
                </thead>
                <tbody>
                  {tempOperatingHours.map((hours, index) => (
                    <OperatingHoursRow key={hours.day}>
                      <OperatingHoursCell>{hours.day}</OperatingHoursCell>
                      <OperatingHoursCell>
                        <TimeInput 
                          type="time" 
                          value={hours.open}
                          onChange={(e) => handleHoursChange(index, 'open', e.target.value)}
                          disabled={hours.closed}
                        />
                      </OperatingHoursCell>
                      <OperatingHoursCell>
                        <TimeInput 
                          type="time" 
                          value={hours.close}
                          onChange={(e) => handleHoursChange(index, 'close', e.target.value)}
                          disabled={hours.closed}
                        />
                      </OperatingHoursCell>
                      <OperatingHoursCell>
                        <input 
                          type="checkbox" 
                          checked={hours.closed}
                          onChange={() => handleHoursChange(index, 'closed')}
                        />
                      </OperatingHoursCell>
                    </OperatingHoursRow>
                  ))}
                </tbody>
              </OperatingHoursTable>
              <ButtonGroup style={{ marginTop: '20px' }}>
                <Button secondary onClick={handleCancelHours}>Cancel</Button>
                <Button onClick={handleSaveHours}>
                  <IconSave /> Save Changes
                </Button>
              </ButtonGroup>
            </>
          )}
        </OperatingHoursContainer>
      )}
    </Container>
  );
};

export default CustomerSupportMessages; 