package com.example.bikerental.viewmodels;

import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.example.bikerental.models.Bike;
import com.example.bikerental.models.BikeLocation;
import com.example.bikerental.models.BikeRide;
import com.example.bikerental.models.TrackableBike;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import kotlinx.coroutines.flow.StateFlow;
import java.util.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u009a\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0014\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0016\u00100\u001a\u0002012\u0006\u00102\u001a\u00020\f2\u0006\u00103\u001a\u00020\fJ\u0006\u00104\u001a\u000205J\b\u00106\u001a\u000205H\u0002J\u000e\u00107\u001a\u0002052\u0006\u00108\u001a\u00020\fJ\b\u00109\u001a\u000205H\u0002J\u0006\u0010:\u001a\u000205J\b\u0010;\u001a\u000205H\u0014J\u0010\u0010<\u001a\u0002052\u0006\u0010=\u001a\u00020\u0007H\u0002J\u000e\u0010>\u001a\u0002052\u0006\u0010?\u001a\u00020\u0004J\u0016\u0010@\u001a\u0002052\u0006\u0010?\u001a\u00020\u00042\u0006\u0010A\u001a\u00020\fJ\u000e\u0010B\u001a\u0002052\u0006\u0010?\u001a\u00020\u0004J\u0006\u0010C\u001a\u000205J\u0016\u0010D\u001a\u0002052\u0006\u0010?\u001a\u00020\u00042\u0006\u0010E\u001a\u00020\fJX\u0010F\u001a\u0002052\u0006\u0010G\u001a\u00020\u00042\u0006\u0010H\u001a\u00020\u00042\u0006\u0010I\u001a\u00020J2\u0006\u0010K\u001a\u00020L2\u0006\u0010M\u001a\u00020\f2\u0006\u0010N\u001a\u00020\u00042\f\u0010O\u001a\b\u0012\u0004\u0012\u0002050P2\u0012\u0010Q\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u0002050RR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0005\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000b\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\f0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00040\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0012\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\n0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0013\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0019\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0016R\u0010\u0010\u001b\u001a\u0004\u0018\u00010\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u001d\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\f0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0016R\u0010\u0010\u001f\u001a\u0004\u0018\u00010\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001d\u0010 \u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\t0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u0016R\u000e\u0010\"\u001a\u00020#X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010$\u001a\u00020%X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010&\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00040\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u0016R\u000e\u0010(\u001a\u00020)X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010*\u001a\b\u0012\u0004\u0012\u00020\u00110\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010\u0016R\u000e\u0010+\u001a\u00020#X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010,\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\n0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010\u0016R\u000e\u0010.\u001a\u00020/X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006S"}, d2 = {"Lcom/example/bikerental/viewmodels/BikeViewModel;", "Landroidx/lifecycle/ViewModel;", "()V", "TAG", "", "_activeRide", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/example/bikerental/models/BikeRide;", "_availableBikes", "", "Lcom/example/bikerental/models/TrackableBike;", "_bikeLocation", "Lcom/google/android/gms/maps/model/LatLng;", "_bikes", "Lcom/example/bikerental/models/Bike;", "_error", "_isLoading", "", "_selectedBike", "activeRide", "Lkotlinx/coroutines/flow/StateFlow;", "getActiveRide", "()Lkotlinx/coroutines/flow/StateFlow;", "auth", "Lcom/google/firebase/auth/FirebaseAuth;", "availableBikes", "getAvailableBikes", "availableBikesListener", "Lcom/google/firebase/database/ValueEventListener;", "bikeLocation", "getBikeLocation", "bikeLocationListener", "bikes", "getBikes", "bikesRef", "Lcom/google/firebase/database/DatabaseReference;", "database", "Lcom/google/firebase/database/FirebaseDatabase;", "error", "getError", "firestore", "Lcom/google/firebase/firestore/FirebaseFirestore;", "isLoading", "ridesRef", "selectedBike", "getSelectedBike", "storage", "Lcom/google/firebase/storage/FirebaseStorage;", "calculateDistance", "", "start", "end", "cancelRide", "", "checkForActiveRide", "endRide", "finalLocation", "fetchAllBikes", "fetchBikesFromFirestore", "onCleared", "saveRideToFirestore", "ride", "selectBike", "bikeId", "startRide", "userLocation", "startTrackingBike", "stopTrackingBike", "updateBikeLocation", "newLocation", "uploadBike", "name", "type", "price", "", "imageUri", "Landroid/net/Uri;", "location", "description", "onSuccess", "Lkotlin/Function0;", "onError", "Lkotlin/Function1;", "app_debug"})
public final class BikeViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String TAG = "BikeViewModel";
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.auth.FirebaseAuth auth = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.firestore.FirebaseFirestore firestore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.database.FirebaseDatabase database = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.storage.FirebaseStorage storage = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.database.DatabaseReference bikesRef = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.database.DatabaseReference ridesRef = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.example.bikerental.models.Bike>> _bikes = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.bikerental.models.Bike>> bikes = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _error = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> error = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.example.bikerental.models.TrackableBike>> _availableBikes = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.bikerental.models.TrackableBike>> availableBikes = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.bikerental.models.TrackableBike> _selectedBike = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.bikerental.models.TrackableBike> selectedBike = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.bikerental.models.BikeRide> _activeRide = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.bikerental.models.BikeRide> activeRide = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.google.android.gms.maps.model.LatLng> _bikeLocation = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.google.android.gms.maps.model.LatLng> bikeLocation = null;
    @org.jetbrains.annotations.Nullable()
    private com.google.firebase.database.ValueEventListener bikeLocationListener;
    @org.jetbrains.annotations.Nullable()
    private com.google.firebase.database.ValueEventListener availableBikesListener;
    
    public BikeViewModel() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.bikerental.models.Bike>> getBikes() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoading() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getError() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.bikerental.models.TrackableBike>> getAvailableBikes() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.bikerental.models.TrackableBike> getSelectedBike() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.bikerental.models.BikeRide> getActiveRide() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.google.android.gms.maps.model.LatLng> getBikeLocation() {
        return null;
    }
    
    public final void fetchBikesFromFirestore() {
    }
    
    public final void uploadBike(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.lang.String type, double price, @org.jetbrains.annotations.NotNull()
    android.net.Uri imageUri, @org.jetbrains.annotations.NotNull()
    com.google.android.gms.maps.model.LatLng location, @org.jetbrains.annotations.NotNull()
    java.lang.String description, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSuccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onError) {
    }
    
    private final void fetchAllBikes() {
    }
    
    private final void checkForActiveRide() {
    }
    
    public final void selectBike(@org.jetbrains.annotations.NotNull()
    java.lang.String bikeId) {
    }
    
    public final void startTrackingBike(@org.jetbrains.annotations.NotNull()
    java.lang.String bikeId) {
    }
    
    public final void stopTrackingBike() {
    }
    
    public final void startRide(@org.jetbrains.annotations.NotNull()
    java.lang.String bikeId, @org.jetbrains.annotations.NotNull()
    com.google.android.gms.maps.model.LatLng userLocation) {
    }
    
    public final void updateBikeLocation(@org.jetbrains.annotations.NotNull()
    java.lang.String bikeId, @org.jetbrains.annotations.NotNull()
    com.google.android.gms.maps.model.LatLng newLocation) {
    }
    
    public final void endRide(@org.jetbrains.annotations.NotNull()
    com.google.android.gms.maps.model.LatLng finalLocation) {
    }
    
    private final void saveRideToFirestore(com.example.bikerental.models.BikeRide ride) {
    }
    
    public final void cancelRide() {
    }
    
    public final float calculateDistance(@org.jetbrains.annotations.NotNull()
    com.google.android.gms.maps.model.LatLng start, @org.jetbrains.annotations.NotNull()
    com.google.android.gms.maps.model.LatLng end) {
        return 0.0F;
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
}