import React, { useState } from 'react';
import styled from 'styled-components';

const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  accent: '#FF8C00',
  success: '#4CAF50',
  danger: '#d32f2f',
  warning: '#FFC107',
  info: '#2196F3',
  gold: '#FFD700'
};

const Overlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
`;

const DialogContainer = styled.div`
  background: ${colors.white};
  border-radius: 20px;
  padding: 30px;
  max-width: 500px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
  
  @keyframes slideUp {
    from {
      transform: translateY(50px);
      opacity: 0;
    }
    to {
      transform: translateY(0);
      opacity: 1;
    }
  }
`;

const Header = styled.div`
  text-align: center;
  margin-bottom: 25px;
`;

const Title = styled.h2`
  color: ${colors.darkGray};
  margin: 0 0 10px 0;
  font-size: 24px;
  font-weight: 600;
`;

const Subtitle = styled.p`
  color: ${colors.mediumGray};
  margin: 0;
  font-size: 16px;
`;

const StatsContainer = styled.div`
  background: linear-gradient(135deg, ${colors.lightGray} 0%, #e8f5e8 100%);
  border-radius: 15px;
  padding: 20px;
  margin-bottom: 25px;
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
`;

const StatItem = styled.div`
  text-align: center;
`;

const StatValue = styled.div`
  font-size: 24px;
  font-weight: bold;
  color: ${props => props.color || colors.pineGreen};
  margin-bottom: 5px;
`;

const StatLabel = styled.div`
  font-size: 12px;
  color: ${colors.mediumGray};
  font-weight: 500;
`;

const RatingSection = styled.div`
  margin-bottom: 25px;
`;

const SectionTitle = styled.h3`
  color: ${colors.darkGray};
  margin: 0 0 15px 0;
  font-size: 18px;
  font-weight: 600;
`;

const StarRating = styled.div`
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 15px;
`;

const Star = styled.button`
  background: none;
  border: none;
  font-size: 32px;
  cursor: pointer;
  color: ${props => props.filled ? colors.gold : colors.lightGray};
  transition: all 0.2s ease;
  
  &:hover {
    transform: scale(1.1);
    color: ${colors.gold};
  }
`;

const RatingLabel = styled.div`
  text-align: center;
  color: ${colors.mediumGray};
  font-size: 14px;
  margin-bottom: 20px;
`;

const FeedbackSection = styled.div`
  margin-bottom: 25px;
`;

const TextArea = styled.textarea`
  width: 100%;
  min-height: 100px;
  padding: 15px;
  border: 2px solid ${colors.lightGray};
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  transition: border-color 0.2s ease;
  
  &:focus {
    outline: none;
    border-color: ${colors.pineGreen};
  }
  
  &::placeholder {
    color: ${colors.mediumGray};
  }
`;

const TagsSection = styled.div`
  margin-bottom: 25px;
`;

const TagsContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
`;

const Tag = styled.button`
  background: ${props => props.selected ? colors.pineGreen : colors.lightGray};
  color: ${props => props.selected ? colors.white : colors.darkGray};
  border: none;
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
`;

const ButtonsContainer = styled.div`
  display: flex;
  gap: 15px;
  margin-top: 30px;
`;

const Button = styled.button`
  flex: 1;
  padding: 15px 20px;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  
  ${props => {
    if (props.variant === 'primary') {
      return `
        background: linear-gradient(135deg, ${colors.pineGreen} 0%, ${colors.lightPineGreen} 100%);
        color: ${colors.white};
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 25px rgba(29, 60, 52, 0.3);
        }
      `;
    } else {
      return `
        background: ${colors.lightGray};
        color: ${colors.darkGray};
        &:hover {
          background: ${colors.mediumGray};
          color: ${colors.white};
        }
      `;
    }
  }}
`;

const AchievementBadge = styled.div`
  background: linear-gradient(135deg, ${colors.gold} 0%, #ffa000 100%);
  color: ${colors.white};
  padding: 10px 15px;
  border-radius: 25px;
  text-align: center;
  margin-bottom: 20px;
  font-weight: bold;
  box-shadow: 0 4px 15px rgba(255, 193, 7, 0.3);
`;

const RideRatingDialog = ({ 
  rideData, 
  onSubmitRating, 
  onClose,
  achievements = []
}) => {
  const [rating, setRating] = useState(0);
  const [feedback, setFeedback] = useState('');
  const [selectedTags, setSelectedTags] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const ratingLabels = {
    1: 'Poor',
    2: 'Fair', 
    3: 'Good',
    4: 'Very Good',
    5: 'Excellent'
  };

  const feedbackTags = [
    'Smooth ride',
    'Great bike condition',
    'Easy to find',
    'Good location',
    'Fast unlock',
    'Comfortable seat',
    'Scenic route',
    'Good exercise',
    'Bike needs maintenance',
    'Hard to unlock',
    'Poor bike condition',
    'Difficult location'
  ];

  const formatTime = (milliseconds) => {
    const totalSeconds = Math.floor(milliseconds / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
  };

  const formatDistance = (meters) => {
    if (meters < 1000) {
      return `${Math.round(meters)}m`;
    }
    return `${(meters / 1000).toFixed(2)}km`;
  };

  const handleStarClick = (starRating) => {
    setRating(starRating);
  };

  const handleTagClick = (tag) => {
    setSelectedTags(prev => 
      prev.includes(tag) 
        ? prev.filter(t => t !== tag)
        : [...prev, tag]
    );
  };

  const handleSubmit = async () => {
    if (rating === 0) {
      alert('Please provide a rating before submitting.');
      return;
    }

    setIsSubmitting(true);
    
    try {
      await onSubmitRating({
        rating,
        feedback,
        tags: selectedTags,
        rideId: rideData.id,
        timestamp: new Date()
      });
      
      onClose();
    } catch (error) {
      console.error('Error submitting rating:', error);
      alert('Failed to submit rating. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!rideData) return null;

  const duration = rideData.endTime 
    ? (rideData.endTime.toDate?.()?.getTime() || rideData.endTime) - (rideData.startTime.toDate?.()?.getTime() || rideData.startTime)
    : Date.now() - (rideData.startTime.toDate?.()?.getTime() || rideData.startTime);
  
  const distance = rideData.totalDistance || 0;
  const averageSpeed = duration > 0 ? (distance / 1000) / (duration / 3600000) : 0;
  const calories = Math.round((distance / 1000) * 50); // Rough calculation

  return (
    <Overlay onClick={(e) => e.target === e.currentTarget && onClose()}>
      <DialogContainer>
        <Header>
          <Title>🎉 Ride Complete!</Title>
          <Subtitle>How was your ride experience?</Subtitle>
        </Header>

        {/* Achievements */}
        {achievements.length > 0 && (
          <div>
            {achievements.map((achievement, index) => (
              <AchievementBadge key={index}>
                🏆 {achievement}
              </AchievementBadge>
            ))}
          </div>
        )}

        {/* Ride Statistics */}
        <StatsContainer>
          <StatsGrid>
            <StatItem>
              <StatValue color={colors.info}>{formatTime(duration)}</StatValue>
              <StatLabel>Duration</StatLabel>
            </StatItem>
            <StatItem>
              <StatValue color={colors.success}>{formatDistance(distance)}</StatValue>
              <StatLabel>Distance</StatLabel>
            </StatItem>
            <StatItem>
              <StatValue color={colors.purple}>{averageSpeed.toFixed(1)} km/h</StatValue>
              <StatLabel>Avg Speed</StatLabel>
            </StatItem>
            <StatItem>
              <StatValue color={colors.accent}>{calories}</StatValue>
              <StatLabel>Calories</StatLabel>
            </StatItem>
          </StatsGrid>
        </StatsContainer>

        {/* Rating Section */}
        <RatingSection>
          <SectionTitle>Rate Your Ride</SectionTitle>
          <StarRating>
            {[1, 2, 3, 4, 5].map(star => (
              <Star
                key={star}
                filled={star <= rating}
                onClick={() => handleStarClick(star)}
              >
                ★
              </Star>
            ))}
          </StarRating>
          {rating > 0 && (
            <RatingLabel>{ratingLabels[rating]}</RatingLabel>
          )}
        </RatingSection>

        {/* Quick Tags */}
        <TagsSection>
          <SectionTitle>Quick Feedback</SectionTitle>
          <TagsContainer>
            {feedbackTags.map(tag => (
              <Tag
                key={tag}
                selected={selectedTags.includes(tag)}
                onClick={() => handleTagClick(tag)}
              >
                {tag}
              </Tag>
            ))}
          </TagsContainer>
        </TagsSection>

        {/* Detailed Feedback */}
        <FeedbackSection>
          <SectionTitle>Additional Comments (Optional)</SectionTitle>
          <TextArea
            value={feedback}
            onChange={(e) => setFeedback(e.target.value)}
            placeholder="Tell us more about your ride experience..."
            maxLength={500}
          />
          <div style={{ 
            textAlign: 'right', 
            fontSize: '12px', 
            color: colors.mediumGray,
            marginTop: '5px'
          }}>
            {feedback.length}/500
          </div>
        </FeedbackSection>

        {/* Action Buttons */}
        <ButtonsContainer>
          <Button onClick={onClose}>
            Skip
          </Button>
          <Button 
            variant="primary" 
            onClick={handleSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Submitting...' : 'Submit Rating'}
          </Button>
        </ButtonsContainer>
      </DialogContainer>
    </Overlay>
  );
};

export default RideRatingDialog; 