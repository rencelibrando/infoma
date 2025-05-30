# QR Code Bike Unlock Implementation Guide

## 🎯 Overview

This implementation provides a complete QR code scanning solution for unlocking bikes in your bike rental app. The system searches for bikes by their `hardwareId` field in Firestore and provides atomic unlocking with proper error handling.

## 🏗️ Architecture

### Key Components

1. **BikeViewModel** - Handles QR validation and bike unlocking logic
2. **QRScannerDialog** - Camera-based QR code scanning UI
3. **BikeUnlockDialog** - Loading dialog during unlock process
4. **QRCodeHelper** - Utility for QR code validation and generation
5. **BikeSetupService** - Service for setting up test data

### Data Flow

```
User scans QR → Validate format → Search Firestore → Atomic unlock → Start ride tracking
```

## 🔧 Setup Instructions

### 1. Update Your Firestore Data

Your bikes need a `hardwareId` field. If you have existing bikes without this field, you can use the setup service:

```kotlin
// In your activity or fragment
val bikeSetupService = BikeSetupService()

// Add hardware IDs to existing bikes
lifecycleScope.launch {
    val result = bikeSetupService.addHardwareIdsToExistingBikes()
    result.onSuccess { count ->
        Log.d("Setup", "Added hardware IDs to $count bikes")
    }.onFailure { error ->
        Log.e("Setup", "Error: ${error.message}")
    }
}
```

### 2. Create Test Bikes (Optional)

```kotlin
// Create sample bikes with hardware IDs for testing
lifecycleScope.launch {
    val result = bikeSetupService.createSampleBikesWithHardwareIds()
    result.onSuccess { bikes ->
        Log.d("Setup", "Created ${bikes.size} test bikes")
        bikes.forEach { bike ->
            Log.d("Test", "Bike: ${bike.id} -> QR Code: ${bike.hardwareId}")
        }
    }
}
```

### 3. Get Test QR Codes

```kotlin
// Print test QR codes to logcat for manual testing
val bikeSetupService = BikeSetupService()
bikeSetupService.printTestQRCodes()
```

## 📱 Usage

### Basic Integration

The MapTab already includes the complete QR scanning flow:

1. User taps "START RIDE" button
2. StartRideDialog appears with "Scan QR Code" option
3. QRScannerDialog opens for camera scanning
4. BikeUnlockDialog shows during unlock process
5. Ride starts automatically on successful unlock

### Manual Integration

If you want to integrate QR scanning elsewhere:

```kotlin
@Composable
fun YourScreen() {
    val bikeViewModel: BikeViewModel = viewModel()
    val isUnlockingBike by bikeViewModel.isUnlockingBike.collectAsState()
    val unlockError by bikeViewModel.unlockError.collectAsState()
    val unlockSuccess by bikeViewModel.unlockSuccess.collectAsState()
    
    var showQRScanner by remember { mutableStateOf(false) }
    
    // Your UI
    Button(onClick = { showQRScanner = true }) {
        Text("Scan QR Code")
    }
    
    // QR Scanner
    QRScannerDialog(
        isVisible = showQRScanner,
        onDismiss = { showQRScanner = false },
        onQRCodeScanned = { qrCode ->
            showQRScanner = false
            // Get user location and unlock bike
            bikeViewModel.validateQRCodeAndUnlockBike(qrCode, userLocation)
        }
    )
    
    // Unlock loading dialog
    BikeUnlockDialog(isVisible = isUnlockingBike)
    
    // Handle results
    LaunchedEffect(unlockSuccess) {
        if (unlockSuccess) {
            // Navigate to ride screen
            bikeViewModel.resetUnlockStates()
        }
    }
    
    unlockError?.let { error ->
        // Show error to user
        Text("Error: $error", color = Color.Red)
    }
}
```

## 🧪 Testing

### 1. Generate Test QR Codes

```kotlin
val qrCodes = QRCodeHelper.generateSampleHardwareIds()
qrCodes.forEach { (bikeId, hardwareId) ->
    println("Bike: $bikeId")
    println("QR Code: $hardwareId")
    println("---")
}
```

### 2. Manual Testing

You can manually test by typing QR codes in the scanner. Valid formats:
- `bike_001:ABC123` (bike ID with verification code)
- `bike_001` (direct bike ID)
- Any 6+ character string

### 3. Create Physical QR Codes

Use any QR code generator to create physical codes with the hardware IDs:
- https://qr-code-generator.com/
- Use the `hardwareId` value as the QR code content

### 4. Test Error Cases

```kotlin
// Test invalid QR code
bikeViewModel.validateQRCodeAndUnlockBike("invalid", userLocation)

// Test bike not found
bikeViewModel.validateQRCodeAndUnlockBike("nonexistent:123456", userLocation)

// Test bike already in use (need to manually set isInUse=true in Firestore)
```

## 🔒 Security Features

### Atomic Transactions
- Uses Firestore transactions to prevent race conditions
- Multiple users cannot unlock the same bike simultaneously
- Bike status is checked and updated atomically

### Validation
- QR code format validation before database queries
- User authentication checks
- Active ride prevention (one ride per user)
- Bike availability verification

### Error Handling
- Comprehensive error messages for different failure scenarios
- Automatic state cleanup on errors
- Network error resilience

## 🛠️ Customization

### QR Code Formats

Modify `QRCodeHelper.isValidQRCodeFormat()` to support your specific QR code formats:

```kotlin
fun isValidQRCodeFormat(qrCode: String): Boolean {
    return when {
        qrCode.matches(Regex("^BIKE-\\d{3}-[A-Z0-9]{6}$")) -> true // Custom format
        qrCode.startsWith("bike_") && qrCode.length >= 6 -> true
        // Add your validation rules
        else -> false
    }
}
```

### Hardware ID Generation

Customize `QRCodeHelper.generateHardwareId()` for your hardware ID format:

```kotlin
fun generateHardwareId(bikeId: String): String {
    val timestamp = System.currentTimeMillis().toString().takeLast(6)
    val random = (1000..9999).random()
    return "BIKE-${bikeId.takeLast(3)}-$timestamp$random"
}
```

### Error Messages

Update error messages in `BikeViewModel.validateQRCodeAndUnlockBike()`:

```kotlin
if (matchingBike == null) {
    _unlockError.value = "QR code not recognized. Please scan the code on your assigned bike."
    return@launch
}
```

## 📊 Monitoring

### Logging

The implementation includes comprehensive logging:
- QR code validation attempts
- Firestore query results
- Transaction success/failure
- Error details

### Metrics to Track

Consider tracking these metrics:
- QR scan success rate
- Average unlock time
- Failed unlock reasons
- User retry patterns

## 🚀 Production Considerations

### Performance
- Firestore queries are indexed on `hardwareId`
- Transactions are kept minimal for speed
- Camera permissions are handled gracefully

### Scaling
- Consider adding `hardwareId` to Firestore indexes
- Implement QR code caching for frequently scanned bikes
- Add retry logic for network failures

### Security
- Consider adding QR code expiration
- Implement bike location verification
- Add audit logging for unlock attempts

## 📝 Firebase Rules

Add these Firestore security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Bikes collection
    match /bikes/{bikeId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null 
        && (resource == null || resource.data.currentRider == "" || resource.data.currentRider == request.auth.uid);
    }
  }
}
```

## 🐛 Common Issues

### QR Scanner Not Working
- Check camera permissions
- Ensure device has a camera
- Test with good lighting conditions

### Bike Not Found
- Verify `hardwareId` field exists in Firestore
- Check QR code format is valid
- Ensure bike document exists

### Transaction Failures
- Check network connectivity
- Verify Firestore rules allow writes
- Ensure user is authenticated

### State Issues
- Call `bikeViewModel.resetUnlockStates()` after handling errors
- Check for active rides before allowing new scans

## 📚 API Reference

### BikeViewModel Methods

```kotlin
// Main unlock method
fun validateQRCodeAndUnlockBike(qrCode: String, userLocation: LatLng)

// QR scanner state management
fun showQRScanner()
fun hideQRScanner()
fun onQRCodeScanned(qrCode: String, userLocation: LatLng)

// State cleanup
fun resetUnlockStates()
fun resetQRScanningError()
```

### QRCodeHelper Methods

```kotlin
// Validation
fun isValidQRCodeFormat(qrCode: String): Boolean
fun extractBikeIdFromQRCode(qrCode: String): String?

// Generation
fun generateHardwareId(bikeId: String): String
fun generateSampleHardwareIds(): List<Pair<String, String>>
```

### BikeSetupService Methods

```kotlin
// Data setup
suspend fun addHardwareIdsToExistingBikes(): Result<Int>
suspend fun createSampleBikesWithHardwareIds(): Result<List<Bike>>

// Verification
suspend fun verifyBikeHardwareIds(): Result<List<String>>
fun printTestQRCodes()
```

This implementation provides a robust, secure, and user-friendly QR code scanning solution for your bike rental app. The atomic transaction system ensures data consistency, while comprehensive error handling provides a smooth user experience. 