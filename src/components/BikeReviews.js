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

// Auto-fetch function to get reviews directly from Firestore
const fetchReviewsDirectlyFromFirestore = async (bikesData) => {
  console.log('Auto-fetching reviews from Firestore...');
  let allReviews = [];
  
  // First try the top-level collection
  try {
    const topLevelQuery = query(
      collection(db, 'reviews'),
      limit(50)
    );
    const topSnapshot = await getDocs(topLevelQuery);
    const topReviews = topSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      source: 'top-level'
    }));
    console.log(`Found ${topReviews.length} reviews in top-level collection`);
    allReviews = [...allReviews, ...topReviews];
  } catch (err) {
    console.error('Error fetching top-level reviews:', err);
  }
  
  // Now try to get reviews from each bike's subcollection
  if (bikesData && bikesData.length > 0) {
    console.log(`Checking reviews for ${bikesData.length} bikes...`);
    
    const bikeReviewPromises = bikesData.map(async (bike) => {
      try {
        console.log(`Checking reviews for bike: ${bike.id} (${bike.name || 'unnamed'})`);
        const subQuery = query(
          collection(db, `bikes/${bike.id}/reviews`),
          limit(10)
        );
        const subSnapshot = await getDocs(subQuery);
        const subReviews = subSnapshot.docs.map(doc => {
          const data = doc.data();
          return {
            id: doc.id,
            bikeId: bike.id, // Explicitly add bikeId from parent path
            ...data,
            source: 'subcollection'
          };
        });
        
        console.log(`Found ${subReviews.length} reviews for bike ${bike.id}`);
        return subReviews;
      } catch (err) {
        console.error(`Error fetching reviews for bike ${bike.id}:`, err);
        return [];
      }
    });
    
    try {
      const bikeReviewsResults = await Promise.all(bikeReviewPromises);
      const allBikeReviews = bikeReviewsResults.flat();
      console.log(`Found total of ${allBikeReviews.length} reviews across all bike subcollections`);
      allReviews = [...allReviews, ...allBikeReviews];
    } catch (err) {
      console.error('Error processing bike review promises:', err);
    }
  } else {
    console.warn('No bikes data available to check for reviews');
  }
  
  console.log('All reviews found:', allReviews);
  return allReviews;
};

const BikeReviews = () => {
  // Get data from context
  const { 
    reviews: contextReviews, 
    loading: contextLoading, 
    lastUpdateTime,
    showUpdateIndicator,
    stats,
    bikes
  } = useDataContext();
  
  // Local state for reviews that combines context data and direct fetched data
  const [reviews, setReviews] = useState([]);
  
  // Add debug logs to check reviews from context
  useEffect(() => {
    console.log('Reviews from context:', contextReviews);
    console.log('Context loading state:', contextLoading);
  }, [contextReviews, contextLoading]);

  const [searchTerm, setSearchTerm] = useState('');
  const [bikeFilter, setBikeFilter] = useState('all');
  const [ratingFilter, setRatingFilter] = useState('all');
  const [sortBy, setSortBy] = useState('newest');
  const [currentPage, setCurrentPage] = useState(1);
  const [reviewsPerPage] = useState(10);
  const [showDetailDialog, setShowDetailDialog] = useState(false);
  const [selectedReview, setSelectedReview] = useState(null);
  const [error, setError] = useState(null);
  // Add loading state back for UX purposes
  const [loading, setLoading] = useState(false);
  
  // Auto-fetch reviews if context doesn't have them
  useEffect(() => {
    // If we already have reviews from context, use those
    if (contextReviews && Array.isArray(contextReviews) && contextReviews.length > 0) {
      console.log('Using reviews from context:', contextReviews.length);
      setReviews(contextReviews);
      return;
    }
    
    // Otherwise, check if we should fetch them directly
    if ((!contextReviews || contextReviews.length === 0) && !contextLoading && bikes) {
      console.log('No reviews in context, fetching directly from Firestore');
      
      setLoading(true);
      fetchReviewsDirectlyFromFirestore(bikes)
        .then(directReviews => {
          console.log(`Fetched ${directReviews.length} reviews directly`);
          setReviews(directReviews);
          setError(null);
        })
        .catch(err => {
          console.error('Error in direct fetch:', err);
          setError('Failed to fetch reviews: ' + err.message);
        })
        .finally(() => {
          setLoading(false);
        });
    }
  }, [contextReviews, contextLoading, bikes]);
  
  // Force initial data load from context if needed
  useEffect(() => {
    if (!reviews || reviews.length === 0) {
      console.log('No reviews found, might need to trigger a refresh');
    }
  }, [reviews]);
  
  // Get unique bike IDs for filtering - modified to only use automatic reviews
  const uniqueBikeIds = useMemo(() => {
    if (!reviews || !Array.isArray(reviews)) return [];
    return [...new Set(reviews.map(review => review.bikeId).filter(Boolean))];
  }, [reviews]);
  
  // Apply filters and sorting to reviews - simplified to only use automatic reviews
  const filteredReviews = useMemo(() => {
    // Guard against missing reviews data
    if (!reviews || !Array.isArray(reviews)) {
      console.log("No reviews data available");
      return [];
    }
    
    console.log(`Filtering ${reviews.length} reviews with filters:`, { searchTerm, bikeFilter, ratingFilter, sortBy });
    
    const filtered = reviews.filter(review => {
      // Filter by search term (in comment or username)
      if (searchTerm && !(
        (review.comment && review.comment.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (review.userName && review.userName.toLowerCase().includes(searchTerm.toLowerCase()))
      )) {
        return false;
      }
      
      // Filter by bike (only if bikeId is present and filter is active)
      if (bikeFilter !== 'all' && review.bikeId && review.bikeId !== bikeFilter) {
        return false;
      }
      
      // Filter by rating
      if (ratingFilter !== 'all' && review.rating !== undefined) {
        const minRating = parseInt(ratingFilter);
        if (review.rating < minRating || review.rating >= minRating + 1) {
          return false;
        }
      }
      
      return true;
    }).sort((a, b) => {
      // Apply sorting, with fallbacks for missing fields
      switch (sortBy) {
        case 'newest':
          return (b.timestamp || 0) - (a.timestamp || 0);
        case 'oldest':
          return (a.timestamp || 0) - (b.timestamp || 0);
        case 'highestRating':
          return (b.rating || 0) - (a.rating || 0);
        case 'lowestRating':
          return (a.rating || 0) - (b.rating || 0);
        default:
          return 0;
      }
    });
    
    console.log(`Filtered down to ${filtered.length} reviews`);
    return filtered;
  }, [reviews, searchTerm, bikeFilter, ratingFilter, sortBy]);
  
  // Pagination logic
  const indexOfLastReview = currentPage * reviewsPerPage;
  const indexOfFirstReview = indexOfLastReview - reviewsPerPage;
  const currentReviews = filteredReviews.slice(indexOfFirstReview, indexOfLastReview);
  
  // Add log to check if currentReviews has items
  useEffect(() => {
    console.log(`Current page ${currentPage} shows ${currentReviews.length} reviews`);
    console.log('Current reviews:', currentReviews);
  }, [currentReviews, currentPage]);
  
  const totalPages = Math.ceil(filteredReviews.length / reviewsPerPage);
  
  // Update debug info to remove manual fetch references
  const debugInfo = useMemo(() => {
    return {
      totalReviews: reviews?.length || 0,
      reviewsInCurrentPage: filteredReviews.slice(indexOfFirstReview, indexOfLastReview).length,
      uniqueBikeIds: uniqueBikeIds?.length || 0,
      hasSubcollectionReviews: reviews?.some(r => r.bikeId && r.id) || false
    };
  }, [reviews, uniqueBikeIds, filteredReviews, indexOfFirstReview, indexOfLastReview]);
  
  // Function to get bike name by ID - using bikes from context
  const getBikeName = useCallback((bikeId) => {
    if (!bikes) return `Bike ${bikeId.substring(0, 6)}`;
    
    const bike = bikes.find(b => b.id === bikeId);
    return bike ? bike.name : `Bike ${bikeId.substring(0, 6)}`;
  }, [bikes]);
  
  const handlePageChange = (page) => {
    setCurrentPage(page);
    // Scroll to top of reviews section
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  };
  
  // Format date for display
  const formatDate = (timestamp) => {
    if (!timestamp) return 'Unknown date';
    const date = new Date(timestamp);
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };
  
  // Add a function to manually trigger refresh
  const handleRefresh = async () => {
    setLoading(true);
    try {
      const directReviews = await fetchReviewsDirectlyFromFirestore(bikes);
      console.log(`Refreshed and found ${directReviews.length} reviews`);
      setReviews(directReviews);
      setError(null);
    } catch (err) {
      console.error('Error in refresh:', err);
      setError('Failed to refresh reviews: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container>
      <Title>Bike Reviews</Title>
      
      {showUpdateIndicator && (
        <RefreshIndicator>
          <span role="img" aria-label="refresh">🔄</span> Data refreshed
        </RefreshIndicator>
      )}
      
      <StatsContainer>
        <StatCard>
          <StatValue>{stats?.totalReviews || reviews.length || 0}</StatValue>
          <StatLabel>Total Reviews</StatLabel>
        </StatCard>
        
        <StatCard>
          <StatValue>
            {stats?.averageRating !== undefined && typeof stats.averageRating === 'number' 
              ? stats.averageRating.toFixed(1) 
              : '0.0'}
            <span role="img" aria-label="star" style={{ marginLeft: '5px', color: colors.warning }}>⭐</span>
          </StatValue>
          <StatLabel>Average Rating</StatLabel>
        </StatCard>
        
        <StatCard>
          <StatValue>{uniqueBikeIds.length}</StatValue>
          <StatLabel>Bikes Reviewed</StatLabel>
        </StatCard>
      </StatsContainer>
      
      {/* Enhanced debug information */}
      <div style={{ 
        margin: '10px 0', 
        padding: '10px', 
        backgroundColor: '#f8f9fa', 
        borderRadius: '4px',
        fontSize: '12px',
        color: '#666'
      }}>
        <strong>Debug Info:</strong>
        <ul style={{ margin: '5px 0', paddingLeft: '20px' }}>
          <li>Reviews in Context: {contextReviews?.length || 0}</li>
          <li>Reviews in Local State: {reviews?.length || 0}</li>
          <li>Reviews in Current Page: {currentReviews?.length || 0}</li>
          <li>Unique Bike IDs: {uniqueBikeIds?.length || 0}</li>
          <li>Has Subcollection Reviews: {reviews?.some(r => r.bikeId && r.id) || false}</li>
          <li>Current Filters: {searchTerm ? `Search: ${searchTerm}, ` : ''}Bike: {bikeFilter}, Rating: {ratingFilter}</li>
          <li>Context Loading: {contextLoading ? 'Yes' : 'No'}</li>
          <li>Local Loading: {loading ? 'Yes' : 'No'}</li>
        </ul>
        
        {/* Add refresh button to force direct fetch */}
        <div style={{ marginTop: '10px' }}>
          <button 
            onClick={handleRefresh} 
            disabled={loading}
            style={{
              padding: '5px 10px',
              backgroundColor: colors.pineGreen,
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: loading ? 'wait' : 'pointer'
            }}
          >
            {loading ? 'Loading...' : 'Refresh Reviews from Firestore'}
          </button>
        </div>
      </div>
      
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
                      {review.rating !== undefined ? review.rating.toFixed(1) : '?'} <span role="img" aria-label="star">⭐</span>
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
            <p><strong>Rating:</strong> {selectedReview.rating} ⭐</p>
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