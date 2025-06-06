import React, { useState, useEffect, useMemo, useCallback } from 'react';
import styled from 'styled-components';
import { useDataContext } from '../context/DataContext';
import { collection, getDocs, query, orderBy, limit } from 'firebase/firestore';
import { db } from '../firebase';

// Pine green and gray theme colors
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  accent: '#FF8C00',
  warning: '#FFC107'
};

const Container = styled.div`
  padding: 20px;
`;

const Title = styled.h2`
  margin-bottom: 20px;
  color: ${colors.darkGray};
`;

const ReviewsContainer = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  padding: 20px;
  margin-bottom: 20px;
`;

const ReviewsHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
`;

const ReviewsTitle = styled.h3`
  color: ${colors.darkGray};
  font-size: 18px;
  margin: 0;
`;

const FilterContainer = styled.div`
  display: flex;
  gap: 10px;
`;

const Select = styled.select`
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: ${colors.white};
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
`;

const SearchInput = styled.input`
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  flex: 1;
  min-width: 200px;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
    box-shadow: 0 0 0 2px rgba(29, 60, 52, 0.1);
  }
`;

const ReviewsList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 15px;
`;

const ReviewCard = styled.div`
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 15px;
  transition: all 0.2s ease;
  
  &:hover {
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  }
`;

const ReviewHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
`;

const BikeInfo = styled.div`
  font-weight: 500;
  color: ${colors.darkGray};
`;

const UserInfo = styled.div`
  font-size: 14px;
  color: ${colors.mediumGray};
`;

const Rating = styled.div`
  display: flex;
  align-items: center;
  color: ${colors.warning};
  font-weight: 600;
`;

const ReviewText = styled.p`
  color: ${colors.darkGray};
  margin: 10px 0;
  line-height: 1.5;
`;

const ReviewDate = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  text-align: right;
`;

const LoadingMessage = styled.div`
  text-align: center;
  padding: 40px;
  color: ${colors.mediumGray};
`;

const NoDataMessage = styled.div`
  text-align: center;
  padding: 30px;
  color: ${colors.mediumGray};
  font-style: italic;
`;

const Pagination = styled.div`
  display: flex;
  justify-content: center;
  gap: 5px;
  margin-top: 20px;
`;

const PageButton = styled.button`
  padding: 5px 10px;
  border: 1px solid ${props => props.active ? colors.pineGreen : '#ddd'};
  background-color: ${props => props.active ? colors.pineGreen : colors.white};
  color: ${props => props.active ? colors.white : colors.darkGray};
  border-radius: 4px;
  cursor: pointer;
  
  &:hover {
    background-color: ${props => props.active ? colors.pineGreen : colors.lightGray};
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const RefreshIndicator = styled.div`
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 20px;
  text-align: center;
`;

const LastUpdateTime = styled.div`
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 20px;
  text-align: center;
`;

const FiltersContainer = styled.div`
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
`;

const StatsContainer = styled.div`
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
`;

const StatCard = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  padding: 10px;
  flex: 1;
`;

const StatValue = styled.div`
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 5px;
`;

const StatLabel = styled.div`
  font-size: 14px;
  color: ${colors.mediumGray};
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 30px;
  color: ${colors.mediumGray};
  font-style: italic;
`;

const EmptyIcon = styled.span`
  font-size: 24px;
  margin-bottom: 10px;
`;

const EmptyText = styled.div`
  font-size: 16px;
`;

const Button = styled.button`
  padding: 10px 20px;
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  
  &:hover {
    background-color: ${colors.lightPineGreen};
  }
`;

const ErrorMessage = styled.div`
  text-align: center;
  padding: 20px;
  color: #d32f2f;
  background-color: #ffebee;
  border-radius: 4px;
  margin: 20px 0;
`;

const Avatar = styled.div`
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: ${colors.pineGreen};
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 10px;
`;

const UserName = styled.div`
  font-weight: 500;
  color: ${colors.darkGray};
`;

const StarRating = styled.div`
  display: flex;
  color: ${colors.warning};
`;

const Star = styled.span`
  color: ${props => props.filled ? colors.warning : '#e0e0e0'};
  margin: 0 1px;
`;

const BikeName = styled.div`
  font-weight: 500;
  color: ${colors.pineGreen};
  margin: 10px 0;
`;

const ReviewFooter = styled.div`
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
  font-size: 12px;
`;

const DateInfo = styled.div`
  color: ${colors.mediumGray};
`;

const PageNumber = styled.button`
  padding: 5px 10px;
  border: 1px solid ${props => props.active ? colors.pineGreen : '#ddd'};
  background-color: ${props => props.active ? colors.pineGreen : colors.white};
  color: ${props => props.active ? colors.white : colors.darkGray};
  border-radius: 4px;
  cursor: pointer;
  
  &:hover {
    background-color: ${props => props.active ? colors.pineGreen : colors.lightGray};
  }
`;

// Fetch reviews directly from Firestore for immediate loading
const fetchReviewsDirectlyFromFirestore = async (bikesData) => {
  try {
    const allReviews = [];
    
    // First, try to get reviews from the top-level reviews collection
    const reviewsRef = collection(db, 'reviews');
    const q = query(reviewsRef, orderBy('timestamp', 'desc'), limit(100));
    const snapshot = await getDocs(q);
    const topReviews = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      source: 'top-level'
    }));
    
    allReviews.push(...topReviews);
    
    // Then get reviews from bike subcollections
    const allBikeReviews = [];
    for (const bike of bikesData) {
      if (bike.id) {
        try {
          const bikeReviewsRef = collection(db, 'bikes', bike.id, 'reviews');
          const bikeQuery = query(bikeReviewsRef, orderBy('timestamp', 'desc'), limit(20));
          const bikeSnapshot = await getDocs(bikeQuery);
          const subReviews = bikeSnapshot.docs.map(doc => ({
            id: `${bike.id}_${doc.id}`,
            bikeId: bike.id,
            ...doc.data(),
            source: 'subcollection'
          }));
          
          allBikeReviews.push(...subReviews);
        } catch (error) {
          console.error(`Error fetching reviews for bike ${bike.id}:`, error);
        }
      }
    }
    
    allReviews.push(...allBikeReviews);
    
    return allReviews;
  } catch (error) {
    console.error('Error in fetchReviewsDirectlyFromFirestore:', error);
    return [];
  }
};

const BikeReviews = () => {
  const { bikes, reviews: contextReviews, loading: contextLoading } = useDataContext();
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  // Filtering and pagination states
  const [searchTerm, setSearchTerm] = useState('');
  const [bikeFilter, setBikeFilter] = useState('all');
  const [ratingFilter, setRatingFilter] = useState('all');
  const [sortBy, setSortBy] = useState('newest');
  const [currentPage, setCurrentPage] = useState(1);
  const reviewsPerPage = 10;
  
  // Detail dialog state
  const [selectedReview, setSelectedReview] = useState(null);
  const [showDetailDialog, setShowDetailDialog] = useState(false);

  // Fetch reviews directly from Firestore on mount
  useEffect(() => {
    const fetchReviews = async () => {
      if (bikes && bikes.length > 0) {
        setLoading(true);
        const directReviews = await fetchReviewsDirectlyFromFirestore(bikes);
        setReviews(directReviews);
        setLoading(false);
      }
    };
    
    fetchReviews();
  }, [bikes]);

  // Fall back to context reviews if no direct reviews
  useEffect(() => {
    // If we don't have direct reviews but have context reviews, use those
    if ((!reviews || reviews.length === 0) && contextReviews && contextReviews.length > 0) {
      setReviews(contextReviews);
    }
  }, [contextReviews, reviews]);

  // Get filtered and sorted reviews
  const filteredReviews = useMemo(() => {
    if (!reviews || reviews.length === 0) return [];
    
    let filtered = reviews;

    // Filter by search term
    if (searchTerm) {
      filtered = filtered.filter(review =>
        review.comment?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        review.userName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        getBikeName(review.bikeId)?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Filter by bike
    if (bikeFilter !== 'all') {
      filtered = filtered.filter(review => review.bikeId === bikeFilter);
    }

    // Filter by rating
    if (ratingFilter !== 'all') {
      filtered = filtered.filter(review => {
        const rating = Number(review.rating);
        return rating === Number(ratingFilter);
      });
    }

    // Sort reviews
    filtered.sort((a, b) => {
      switch (sortBy) {
        case 'oldest':
          return (a.timestamp?.seconds || 0) - (b.timestamp?.seconds || 0);
        case 'highestRating':
          return Number(b.rating || 0) - Number(a.rating || 0);
        case 'lowestRating':
          return Number(a.rating || 0) - Number(b.rating || 0);
        case 'newest':
        default:
          return (b.timestamp?.seconds || 0) - (a.timestamp?.seconds || 0);
      }
    });

    return filtered;
  }, [reviews, searchTerm, bikeFilter, ratingFilter, sortBy, bikes]);

  // Pagination
  const totalPages = Math.ceil(filteredReviews.length / reviewsPerPage);
  const currentReviews = filteredReviews.slice(
    (currentPage - 1) * reviewsPerPage,
    currentPage * reviewsPerPage
  );

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'Unknown date';
    const date = timestamp.seconds 
      ? new Date(timestamp.seconds * 1000) 
      : new Date(timestamp);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleRefresh = async () => {
    if (bikes && bikes.length > 0) {
      setLoading(true);
      const directReviews = await fetchReviewsDirectlyFromFirestore(bikes);
      setReviews(directReviews);
      setLoading(false);
    }
  };

  const getBikeName = (bikeId) => {
    if (!bikeId || !bikes) return 'Unknown Bike';
    const bike = bikes.find(b => b.id === bikeId);
    return bike ? bike.name : `Bike ${bikeId}`;
  };

  // Calculate statistics
  const totalReviews = reviews.length;
  const averageRating = reviews.length > 0 
    ? (reviews.reduce((sum, review) => sum + Number(review.rating || 0), 0) / reviews.length).toFixed(1)
    : '0.0';
  const highRatingCount = reviews.filter(review => Number(review.rating || 0) >= 4).length;
  const uniqueBikeIds = [...new Set(reviews.map(review => review.bikeId).filter(Boolean))];

  // Show appropriate message when no data (after all hooks)
  if (!reviews || reviews.length === 0) {
    if (contextLoading || loading) {
      return <LoadingMessage>Loading reviews...</LoadingMessage>;
    }
    return <NoDataMessage>No review data available.</NoDataMessage>;
  }

  return (
    <Container>
      <Title>Bike Reviews</Title>
      
      {/* Statistics */}
      <StatsContainer>
        <StatCard>
          <StatValue>{totalReviews}</StatValue>
          <StatLabel>Total Reviews</StatLabel>
        </StatCard>
        
        <StatCard>
          <StatValue>{averageRating}</StatValue>
          <StatLabel>Average Rating</StatLabel>
        </StatCard>
        
        <StatCard>
          <StatValue>{highRatingCount}</StatValue>
          <StatLabel>4+ Star Reviews</StatLabel>
        </StatCard>
        
        <StatCard>
          <StatValue>{uniqueBikeIds.length}</StatValue>
          <StatLabel>Bikes Reviewed</StatLabel>
        </StatCard>
      </StatsContainer>
      
      <ReviewsContainer>
        <ReviewsHeader>
          <ReviewsTitle>All Reviews</ReviewsTitle>
          
          <FilterContainer>
            <SearchInput 
              type="text"
              placeholder="Search reviews..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            
            <Select 
              value={bikeFilter}
              onChange={(e) => setBikeFilter(e.target.value)}
            >
              <option value="all">All Bikes</option>
              {uniqueBikeIds.map(bikeId => (
                <option key={bikeId} value={bikeId}>
                  {getBikeName(bikeId)}
                </option>
              ))}
            </Select>
            
            <Select 
              value={ratingFilter}
              onChange={(e) => setRatingFilter(e.target.value)}
            >
              <option value="all">All Ratings</option>
              <option value="5">5 Stars</option>
              <option value="4">4 Stars</option>
              <option value="3">3 Stars</option>
              <option value="2">2 Stars</option>
              <option value="1">1 Star</option>
            </Select>
            
            <Select 
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
            >
              <option value="newest">Newest First</option>
              <option value="oldest">Oldest First</option>
              <option value="highestRating">Highest Rating</option>
              <option value="lowestRating">Lowest Rating</option>
            </Select>
          </FilterContainer>
        </ReviewsHeader>
        
        {contextLoading || loading ? (
          <LoadingMessage>
            <div style={{ 
              display: 'inline-block',
              border: '4px solid #f3f3f3',
              borderTop: '4px solid #1D3C34',
              borderRadius: '50%',
              width: '30px',
              height: '30px',
              animation: 'spin 2s linear infinite',
              marginRight: '10px'
            }} />
            Loading reviews...
            <style>{`
              @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
              }
            `}</style>
          </LoadingMessage>
        ) : error ? (
          <ErrorMessage>
            Error loading reviews: {error}
          </ErrorMessage>
        ) : (!reviews || reviews.length === 0) ? (
          <NoDataMessage>
            No review data available. Reviews might not be loaded yet.
          </NoDataMessage>
        ) : filteredReviews.length === 0 ? (
          <NoDataMessage>
            {searchTerm || bikeFilter !== 'all' || ratingFilter !== 'all' 
              ? 'No reviews match your search criteria' 
              : 'No reviews available yet'}
          </NoDataMessage>
        ) : (
          <>
            <ReviewsList>
              {currentReviews.map(review => (
                <ReviewCard key={review.id} onClick={() => {
                  setSelectedReview(review);
                  setShowDetailDialog(true);
                }}>
                  <ReviewHeader>
                    <BikeInfo>
                      {review.bikeId ? getBikeName(review.bikeId) : "Unknown Bike"}
                    </BikeInfo>
                    <Rating>
                      {review.rating !== undefined ? Number(review.rating).toFixed(1) : '?'} <span role="img" aria-label="star">⭐</span>
                    </Rating>
                  </ReviewHeader>
                  
                  <UserInfo>By {review.userName || 'Anonymous User'}</UserInfo>
                  
                  <ReviewText>
                    {review.comment ? 
                      (review.comment.length > 150 
                        ? `${review.comment.substring(0, 150)}...` 
                        : review.comment)
                      : 'No comment provided'
                    }
                  </ReviewText>
                  
                  <ReviewDate>
                    {review.timestamp ? formatDate(review.timestamp) : 'Unknown date'}
                  </ReviewDate>
                  
                  {/* Show review details for debugging */}
                  <div style={{ fontSize: '10px', color: '#999', marginTop: '5px' }}>
                    ID: {review.id?.substring(0, 8) || 'unknown'}...
                    {review.bikeId && <span> | Bike: {review.bikeId?.substring(0, 8)}...</span>}
                    {review.source && <span> | Source: {review.source}</span>}
                    <div>Fields: {Object.keys(review).join(', ')}</div>
                  </div>
                </ReviewCard>
              ))}
            </ReviewsList>
            
            {totalPages > 1 && (
              <Pagination>
                <PageButton 
                  onClick={() => handlePageChange(1)} 
                  disabled={currentPage === 1}
                >
                  &laquo;
                </PageButton>
                
                <PageButton 
                  onClick={() => handlePageChange(currentPage - 1)} 
                  disabled={currentPage === 1}
                >
                  &lsaquo;
                </PageButton>
                
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  // Show 5 pages around current page
                  let pageNum;
                  if (totalPages <= 5) {
                    pageNum = i + 1;
                  } else if (currentPage <= 3) {
                    pageNum = i + 1;
                  } else if (currentPage >= totalPages - 2) {
                    pageNum = totalPages - 4 + i;
                  } else {
                    pageNum = currentPage - 2 + i;
                  }
                  
                  return (
                    <PageButton 
                      key={pageNum}
                      onClick={() => handlePageChange(pageNum)} 
                      active={currentPage === pageNum}
                    >
                      {pageNum}
                    </PageButton>
                  );
                })}
                
                <PageButton 
                  onClick={() => handlePageChange(currentPage + 1)} 
                  disabled={currentPage === totalPages}
                >
                  &rsaquo;
                </PageButton>
                
                <PageButton 
                  onClick={() => handlePageChange(totalPages)} 
                  disabled={currentPage === totalPages}
                >
                  &raquo;
                </PageButton>
              </Pagination>
            )}
          </>
        )}
      </ReviewsContainer>
      
      {/* Review Detail Dialog */}
      {showDetailDialog && selectedReview && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white',
            borderRadius: '8px',
            padding: '20px',
            maxWidth: '600px',
            width: '90%',
            maxHeight: '80vh',
            overflow: 'auto'
          }}>
            <h3 style={{ marginTop: 0 }}>Review Details</h3>
            <p><strong>Bike:</strong> {getBikeName(selectedReview.bikeId || '')}</p>
            <p><strong>Rating:</strong> {selectedReview.rating !== undefined ? Number(selectedReview.rating).toFixed(1) : '?'} ⭐</p>
            <p><strong>User:</strong> {selectedReview.userName || 'Anonymous'}</p>
            <p><strong>Date:</strong> {formatDate(selectedReview.timestamp)}</p>
            <p><strong>Comment:</strong></p>
            <div style={{ 
              padding: '10px', 
              backgroundColor: '#f5f5f5', 
              borderRadius: '4px',
              marginBottom: '20px'
            }}>
              {selectedReview.comment || 'No comment provided'}
            </div>
            
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button onClick={() => setShowDetailDialog(false)}>Close</Button>
            </div>
          </div>
        </div>
      )}
    </Container>
  );
};

export default BikeReviews; 