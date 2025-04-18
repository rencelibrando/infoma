import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { db } from '../firebase';
import { collection, getDocs, query, orderBy, collectionGroup } from 'firebase/firestore';

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

const BikeReviews = () => {
  const [reviews, setReviews] = useState([]);
  const [filteredReviews, setFilteredReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [bikeFilter, setBikeFilter] = useState('all');
  const [ratingFilter, setRatingFilter] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [bikes, setBikes] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const reviewsPerPage = 10;

  // Fetch reviews and bikes data
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        
        // Fetch bikes data for dropdown
        const bikesSnapshot = await getDocs(collection(db, 'bikes'));
        const bikesData = bikesSnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        setBikes(bikesData);
        
        try {
          // Fetch all reviews using collectionGroup to get subcollections
          const reviewsQuery = query(
            collectionGroup(db, 'reviews')
            // Temporarily remove orderBy to check if that's causing issues
            // orderBy('timestamp', 'desc')
          );
          
          const reviewsSnapshot = await getDocs(reviewsQuery);
          
          const reviewsData = reviewsSnapshot.docs.map(doc => {
            const data = doc.data();
            const parentPath = doc.ref.parent.parent.id;
            
            return {
              id: doc.id,
              ...data,
              // Ensure bikeId is set either from the document or from parent path
              bikeId: data.bikeId || parentPath,
              // Convert timestamp to Date if it exists, otherwise use current date
              createdAt: data.timestamp ? 
                (typeof data.timestamp === 'number' ? new Date(data.timestamp) : data.timestamp.toDate()) 
                : new Date(),
              // Make sure we have a comment field
              comment: data.comment || ''
            };
          });
          
          setReviews(reviewsData);
          setFilteredReviews(reviewsData);
        } catch (reviewErr) {
          console.error('Error in reviews query:', reviewErr);
          setReviews([]);
          setFilteredReviews([]);
          setError('Failed to load reviews: ' + reviewErr.message);
        }
      } catch (err) {
        console.error('Error fetching data:', err);
        setError('Failed to load data: ' + err.message);
      } finally {
        setLoading(false);
      }
    };
    
    fetchData();
  }, []);

  // Apply filters when reviews or filter values change
  useEffect(() => {
    try {
      filterReviews();
    } catch (err) {
      console.error('Error applying filters:', err);
      setError('Error applying filters: ' + err.message);
    }
  }, [reviews, bikeFilter, ratingFilter, searchTerm]);

  const filterReviews = () => {
    if (!reviews || reviews.length === 0) {
      setFilteredReviews([]);
      return;
    }
    
    let filtered = [...reviews];
    
    // Apply bike filter
    if (bikeFilter !== 'all') {
      filtered = filtered.filter(review => review.bikeId === bikeFilter);
    }
    
    // Apply rating filter
    if (ratingFilter !== 'all') {
      filtered = filtered.filter(review => review.rating === parseInt(ratingFilter));
    }
    
    // Apply search
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(review => 
        (review.comment && review.comment.toLowerCase().includes(search)) ||
        (review.userName && review.userName.toLowerCase().includes(search))
      );
    }
    
    setFilteredReviews(filtered);
    setCurrentPage(1); // Reset to first page when filters change
  };

  // Get current page of reviews
  const indexOfLastReview = currentPage * reviewsPerPage;
  const indexOfFirstReview = indexOfLastReview - reviewsPerPage;
  const currentReviews = filteredReviews.slice(indexOfFirstReview, indexOfLastReview);
  const totalPages = Math.ceil(filteredReviews.length / reviewsPerPage);

  // Function to get bike name by ID
  const getBikeName = (bikeId) => {
    if (!bikeId) return 'Unknown Bike';
    const bike = bikes.find(b => b.id === bikeId);
    return bike ? bike.name : 'Unknown Bike';
  };

  // Format date for display
  const formatDate = (date) => {
    if (!date) return 'Unknown Date';
    try {
      return date.toLocaleDateString('en-US', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (err) {
      console.error('Error formatting date:', err);
      return 'Invalid Date';
    }
  };

  // Generate star rating display
  const renderStars = (rating) => {
    if (!rating || isNaN(rating)) return '☆☆☆☆☆';
    const ratingNum = parseInt(rating);
    if (ratingNum < 1 || ratingNum > 5) return '☆☆☆☆☆';
    return '★'.repeat(ratingNum) + '☆'.repeat(5 - ratingNum);
  };

  if (loading) {
    return <LoadingMessage>Loading reviews...</LoadingMessage>;
  }

  if (error) {
    return <NoDataMessage>{error}</NoDataMessage>;
  }

  return (
    <Container>
      <Title>Bike Reviews</Title>
      
      <ReviewsContainer>
        <ReviewsHeader>
          <ReviewsTitle>Customer Reviews ({filteredReviews.length})</ReviewsTitle>
          
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
              {bikes.map(bike => (
                <option key={bike.id} value={bike.id}>
                  {bike.name}
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
          </FilterContainer>
        </ReviewsHeader>
        
        {currentReviews.length === 0 ? (
          <NoDataMessage>No reviews found matching your criteria</NoDataMessage>
        ) : (
          <ReviewsList>
            {currentReviews.map(review => (
              <ReviewCard key={review.id}>
                <ReviewHeader>
                  <BikeInfo>{getBikeName(review.bikeId)}</BikeInfo>
                  <Rating>{renderStars(review.rating)}</Rating>
                </ReviewHeader>
                
                <UserInfo>Review by {review.userName || review.userId || 'Anonymous'}</UserInfo>
                <ReviewText>{review.comment || 'No comment provided'}</ReviewText>
                <ReviewDate>{formatDate(review.createdAt)}</ReviewDate>
              </ReviewCard>
            ))}
          </ReviewsList>
        )}
        
        {totalPages > 1 && (
          <Pagination>
            <PageButton 
              onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
              disabled={currentPage === 1}
            >
              Previous
            </PageButton>
            
            {[...Array(totalPages)].map((_, index) => (
              <PageButton 
                key={index} 
                active={currentPage === index + 1}
                onClick={() => setCurrentPage(index + 1)}
              >
                {index + 1}
              </PageButton>
            ))}
            
            <PageButton 
              onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
              disabled={currentPage === totalPages}
            >
              Next
            </PageButton>
          </Pagination>
        )}
      </ReviewsContainer>
    </Container>
  );
};

export default BikeReviews; 