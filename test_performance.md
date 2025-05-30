# Performance Testing Guide

## Quick Testing Commands

### 1. Monitor Performance Logs
```bash
# Monitor startup performance
adb logcat | grep -E "(PerformanceMonitor|MainActivity|AuthViewModel)"

# Monitor for warnings/errors
adb logcat | grep -E "(WARNING|ERROR)" | grep -E "(PerformanceMonitor|Authentication)"
```

### 2. Test App Startup Performance
```bash
# Cold start test
adb shell am force-stop com.example.bikerental
adb shell am start -n com.example.bikerental/.MainActivity

# Check startup time in logs
adb logcat | grep "PerformanceMonitor.*total_startup"
```

### 3. Test Authentication Scenarios

#### Scenario A: New User (No Auth)
1. Clear app data: `adb shell pm clear com.example.bikerental`
2. Launch app
3. **Expected**: Should go to login screen immediately (within 1 second)

#### Scenario B: Logged-in User
1. Login normally first
2. Force close: `adb shell am force-stop com.example.bikerental`
3. Relaunch: `adb shell am start -n com.example.bikerental/.MainActivity`
4. **Expected**: Should navigate to Home screen within 3 seconds

#### Scenario C: User Needs Email Verification
1. Create account but don't verify email
2. Force close and relaunch
3. **Expected**: Should navigate to Email Verification screen immediately

## Success Criteria

### Startup Performance
- ✅ **Total startup time**: < 2 seconds (was 2-4 seconds)
- ✅ **Auth initialization**: < 3 seconds (was sometimes infinite)
- ✅ **Splash screen**: 500ms (was 800ms+)

### Authentication Flow
- ✅ **No infinite loading**: App always reaches a final state within 10 seconds
- ✅ **Proper navigation**: Logged-in users go directly to Home
- ✅ **Error handling**: Network/auth errors show proper error messages

### Log Verification
Look for these success indicators in logs:
```
PerformanceMonitor: Completed total_startup in XXXms
PerformanceMonitor: Completed auth_initialization in XXXms
MainActivity: User authenticated, navigating to Home
```

Look for these warning indicators (should be rare):
```
PerformanceMonitor: WARNING: Auth initialization took XXXms (>3s)
PerformanceMonitor: WARNING: Total startup took XXXms (>5s)
```

## Performance Regression Detection

If you see these patterns, there may be a regression:
- Startup time > 5 seconds consistently
- Auth initialization > 5 seconds
- App stuck on splash screen or loading screen
- Navigation not working after authentication

## Advanced Testing

### Memory Testing
```bash
# Monitor memory usage during startup
adb shell dumpsys meminfo com.example.bikerental
```

### Network Testing
1. Test with airplane mode on/off
2. Test with slow network connection
3. Verify app handles offline scenarios gracefully

### Device Testing
- Test on different Android versions
- Test on low-end devices
- Test on devices with low memory

## Benchmark Results (Expected)

### Before Optimization:
- Cold start: 2-4 seconds
- Auth check: Sometimes infinite
- Navigation: Often failed or delayed

### After Optimization:
- Cold start: 1-2 seconds (50% improvement)
- Auth check: 1-3 seconds (always completes)
- Navigation: Immediate after auth resolves 