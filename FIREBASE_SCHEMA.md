# Firebase Schema for Bike Rental App

This document outlines the Firebase Realtime Database and Firestore structure for the bike rental application.

## Firebase Realtime Database Structure

The Realtime Database is used for real-time data that changes frequently during rides.

```json
{
  "bikes": {
    "bike_001": {
      "latitude": 14.5890,
      "longitude": 120.9760,
      "isAvailable": true,
      "isInUse": false,
      "currentRider": "",
      "lastUpdated": "2024-01-15T10:30:00Z",
      "batteryLevel": 85
    }
  },
  "rides": {
    "ride_001": {
      "id": "ride_001",
      "bikeId": "bike_001",
      "userId": "user_123",
      "startTime": 1705312200000,
      "endTime": 1705315800000,
      "startLocation": {
        "latitude": 14.5890,
        "longitude": 120.9760,
        "timestamp": 1705312200000
      },
      "endLocation": {
        "latitude": 14.5920,
        "longitude": 120.9780,
        "timestamp": 1705315800000
      },
      "path": [
        {
          "latitude": 14.5890,
          "longitude": 120.9760,
          "timestamp": 1705312200000
        },
        {
          "latitude": 14.5900,
          "longitude": 120.9770,
          "timestamp": 1705312260000
        }
      ],
      "status": "active|completed|cancelled",
      "cost": 15.75
    }
  },
  "liveTracking": {
    "ride_001": {
      "currentLocation": {
        "latitude": 14.5905,
        "longitude": 120.9775,
        "timestamp": 1705312320000,
        "speed": 12.5,
        "heading": 45.0
      },
      "lastUpdate": 1705312320000
    }
  }
}
```

## Firestore Collections Structure

Firestore is used for structured data and user records.

### Collection: `bikes`

```json
{
  "bikes/bike_001": {
    "id": "bike_001",
    "name": "Red Mountain Bike",
    "type": "mountain",
    "price": 12.0,
    "priceUnit": "hour",
    "imageUrl": "https://storage.googleapis.com/...",
    "description": "High-quality mountain bike perfect for city rides",
    "isAvailable": true,
    "isInUse": false,
    "currentRider": "",
    "location": {
      "latitude": 14.5890,
      "longitude": 120.9760
    },
    "station": "Intramuros Station",
    "batteryLevel": 85,
    "lastMaintenance": "2024-01-10T09:00:00Z",
    "totalRides": 245,
    "totalDistance": 1250.5,
    "qrCode": "bike_001:ABC123",
    "features": ["GPS", "Electric Assist", "Phone Holder"],
    "specifications": {
      "maxSpeed": 25,
      "range": 50,
      "weight": 22.5
    },
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
}
```

### Collection: `users`

```json
{
  "users/user_123": {
    "uid": "user_123",
    "email": "user@example.com",
    "displayName": "John Doe",
    "phoneNumber": "+639123456789",
    "profileImageUrl": "https://storage.googleapis.com/...",
    "membershipType": "standard|premium",
    "totalRides": 25,
    "totalDistance": 125.5,
    "totalSpent": 350.75,
    "createdAt": "2024-01-01T00:00:00Z",
    "lastActiveAt": "2024-01-15T10:30:00Z",
    "preferences": {
      "preferredBikeType": "mountain",
      "notifications": {
        "rideReminders": true,
        "promotions": false,
        "maintenance": true
      }
    },
    "paymentMethods": [
      {
        "id": "pm_001",
        "type": "card",
        "last4": "1234",
        "brand": "visa",
        "isDefault": true
      }
    ]
  }
}
```

### SubCollection: `users/{userId}/rideHistory`

```json
{
  "users/user_123/rideHistory/ride_001": {
    "id": "ride_001",
    "bikeId": "bike_001",
    "bikeName": "Red Mountain Bike",
    "startTime": 1705312200000,
    "endTime": 1705315800000,
    "duration": 3600000,
    "distance": 5.2,
    "cost": 15.75,
    "startLocation": {
      "latitude": 14.5890,
      "longitude": 120.9760,
      "address": "Intramuros, Manila"
    },
    "endLocation": {
      "latitude": 14.5920,
      "longitude": 120.9780,
      "address": "Rizal Park, Manila"
    },
    "status": "completed",
    "rating": 5,
    "review": "Great bike, smooth ride!",
    "paymentStatus": "paid",
    "paymentMethod": "pm_001"
  }
}
```

### Collection: `bookings`

```json
{
  "bookings/booking_001": {
    "id": "booking_001",
    "userId": "user_123",
    "bikeId": "bike_001",
    "status": "pending|confirmed|cancelled|completed",
    "startTime": 1705312200000,
    "endTime": 1705315800000,
    "totalCost": 15.75,
    "createdAt": 1705310000000,
    "updatedAt": 1705312000000,
    "notes": "Pick up at main station"
  }
}
```

### SubCollection: `bikes/{bikeId}/reviews`

```json
{
  "bikes/bike_001/reviews/review_001": {
    "id": "review_001",
    "userId": "user_123",
    "userName": "John Doe",
    "rating": 5,
    "comment": "Excellent bike, very smooth ride!",
    "createdAt": "2024-01-15T12:00:00Z",
    "rideId": "ride_001",
    "helpful": 3,
    "reported": false
  }
}
```

### Collection: `stations`

```json
{
  "stations/station_001": {
    "id": "station_001",
    "name": "Intramuros Station",
    "address": "Intramuros, Manila, Philippines",
    "location": {
      "latitude": 14.5890,
      "longitude": 120.9760
    },
    "capacity": 20,
    "availableBikes": 15,
    "status": "active|maintenance|inactive",
    "amenities": ["Parking", "Restroom", "WiFi"],
    "operatingHours": {
      "open": "06:00",
      "close": "22:00"
    },
    "contactInfo": {
      "phone": "+639123456789",
      "email": "intramuros@bikerental.com"
    }
  }
}
```

### Collection: `admin`

```json
{
  "admin/dashboard": {
    "totalBikes": 150,
    "activeBikes": 120,
    "totalUsers": 5000,
    "activeRides": 25,
    "todayRevenue": 1250.50,
    "monthlyRevenue": 35000.75,
    "maintenanceAlerts": 3,
    "lastUpdated": "2024-01-15T10:30:00Z"
  },
  "admin/settings": {
    "pricePerHour": 12.0,
    "maxRideDuration": 14400000,
    "maintenanceInterval": 2592000000,
    "features": {
      "realTimeTracking": true,
      "pushNotifications": true,
      "paymentGateway": true
    }
  }
}
```

## Data Relationships

1. **Users ↔ Rides**: One-to-many (user can have multiple rides)
2. **Bikes ↔ Rides**: One-to-many (bike can have multiple rides over time)
3. **Bikes ↔ Reviews**: One-to-many (bike can have multiple reviews)
4. **Users ↔ Reviews**: One-to-many (user can write multiple reviews)
5. **Stations ↔ Bikes**: One-to-many (station can have multiple bikes)
6. **Users ↔ Bookings**: One-to-many (user can have multiple bookings)

## Indexing Strategy

### Firestore Composite Indexes

1. **Bikes Collection**:
   - `isAvailable` (ascending) + `location` (geo point)
   - `type` (ascending) + `isAvailable` (ascending)
   - `price` (ascending) + `isAvailable` (ascending)

2. **Rides Collection**:
   - `userId` (ascending) + `startTime` (descending)
   - `bikeId` (ascending) + `startTime` (descending)
   - `status` (ascending) + `startTime` (descending)

3. **Reviews Collection**:
   - `bikeId` (ascending) + `createdAt` (descending)
   - `userId` (ascending) + `createdAt` (descending)

### Realtime Database Indexes

```json
{
  "rules": {
    "rides": {
      ".indexOn": ["userId", "bikeId", "status", "startTime"]
    },
    "bikes": {
      ".indexOn": ["isAvailable", "isInUse", "currentRider"]
    }
  }
}
```

## Security Considerations

1. **User Data**: Only accessible by the user themselves and admin
2. **Bike Data**: Read-only for users, write access for admin and system
3. **Ride Data**: Only accessible by the rider and admin
4. **Real-time Location**: Only accessible during active rides
5. **Payment Info**: Encrypted and tokenized, never stored in plain text
6. **QR Codes**: Generated with time-based tokens for security

## Performance Optimizations

1. **Denormalization**: Store frequently accessed data (like bike name) in ride documents
2. **Caching**: Use Firestore offline persistence for better performance
3. **Pagination**: Implement cursor-based pagination for large collections
4. **Real-time Updates**: Use Realtime Database for live tracking, Firestore for persistent data
5. **Image Optimization**: Store multiple sizes for different use cases
6. **Location Queries**: Use geohash for efficient location-based queries 