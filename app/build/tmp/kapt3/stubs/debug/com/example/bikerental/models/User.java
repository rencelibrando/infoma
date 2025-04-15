package com.example.bikerental.models;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

/**
 * User model representing a user in the application.
 * This model is used for Firestore deserialization and must have default values for all properties.
 */
@com.google.firebase.database.IgnoreExtraProperties()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0002\bK\n\u0002\u0010\b\n\u0000\n\u0002\u0010$\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B\u0089\u0002\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\n\u0012\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\b\u0012\n\b\u0002\u0010\u0014\u001a\u0004\u0018\u00010\b\u0012\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0016\u001a\u00020\n\u0012\b\b\u0002\u0010\u0017\u001a\u00020\b\u0012\n\b\u0002\u0010\u0018\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0019\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u001a\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u001b\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u001cJ\t\u0010:\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010;\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010<\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010=\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010>\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010?\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0010\u0010@\u001a\u0004\u0018\u00010\bH\u00c6\u0003\u00a2\u0006\u0002\u0010/J\u0010\u0010A\u001a\u0004\u0018\u00010\bH\u00c6\u0003\u00a2\u0006\u0002\u0010/J\u000b\u0010B\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010C\u001a\u00020\nH\u00c6\u0003J\t\u0010D\u001a\u00020\bH\u00c6\u0003J\t\u0010E\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010F\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010G\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010H\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010I\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010J\u001a\u00020\u0003H\u00c6\u0003J\t\u0010K\u001a\u00020\u0003H\u00c6\u0003J\t\u0010L\u001a\u00020\bH\u00c6\u0003J\t\u0010M\u001a\u00020\nH\u00c6\u0003J\u000b\u0010N\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010O\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010P\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0092\u0002\u0010Q\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00032\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\n2\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\b2\n\b\u0002\u0010\u0014\u001a\u0004\u0018\u00010\b2\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0016\u001a\u00020\n2\b\b\u0002\u0010\u0017\u001a\u00020\b2\n\b\u0002\u0010\u0018\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0019\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u001a\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u001b\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001\u00a2\u0006\u0002\u0010RJ\u0013\u0010S\u001a\u00020\n2\b\u0010T\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010U\u001a\u00020VH\u00d6\u0001J\u0016\u0010W\u001a\u0010\u0012\u0004\u0012\u00020\u0003\u0012\u0006\u0012\u0004\u0018\u00010\u00010XH\u0007J\t\u0010Y\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u0019\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u0013\u0010\u001a\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u001eR\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010!R\u0013\u0010\u000e\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u001eR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u001eR\u0013\u0010\u0011\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u001eR\u0013\u0010\r\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u001eR\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\u001eR\u0013\u0010\f\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u001eR\u0013\u0010\u0010\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010\u001eR\u0011\u0010\u0016\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010*R\u0013\u0010\u0002\u001a\u00020\u00038\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\u001eR\u001e\u0010\t\u001a\u00020\n8\u0007@\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\t\u0010*\"\u0004\b,\u0010-R\u0015\u0010\u0013\u001a\u0004\u0018\u00010\b\u00a2\u0006\n\n\u0002\u00100\u001a\u0004\b.\u0010/R\u0011\u0010\u0017\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u0010!R\u0011\u0010\u0006\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b2\u0010\u001eR\u0013\u0010\u000b\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u0010\u001eR\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u0010\u001eR\u0013\u0010\u0018\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u0010\u001eR\u0013\u0010\u0012\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b6\u0010\u001eR\u0013\u0010\u001b\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u0010\u001eR\u0015\u0010\u0014\u001a\u0004\u0018\u00010\b\u00a2\u0006\n\n\u0002\u00100\u001a\u0004\b8\u0010/R\u0013\u0010\u0015\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u0010\u001e\u00a8\u0006Z"}, d2 = {"Lcom/example/bikerental/models/User;", "", "id", "", "email", "fullName", "phoneNumber", "createdAt", "", "isEmailVerified", "", "profilePictureUrl", "givenName", "familyName", "displayName", "provider", "googleId", "facebookId", "twitterId", "lastSignInTime", "verificationSentAt", "verificationToken", "hasCompletedAppVerification", "lastUpdated", "street", "barangay", "city", "verificationMethod", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/String;ZJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getBarangay", "()Ljava/lang/String;", "getCity", "getCreatedAt", "()J", "getDisplayName", "getEmail", "getFacebookId", "getFamilyName", "getFullName", "getGivenName", "getGoogleId", "getHasCompletedAppVerification", "()Z", "getId", "setEmailVerified", "(Z)V", "getLastSignInTime", "()Ljava/lang/Long;", "Ljava/lang/Long;", "getLastUpdated", "getPhoneNumber", "getProfilePictureUrl", "getProvider", "getStreet", "getTwitterId", "getVerificationMethod", "getVerificationSentAt", "getVerificationToken", "component1", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component18", "component19", "component2", "component20", "component21", "component22", "component23", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/String;ZJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/example/bikerental/models/User;", "equals", "other", "hashCode", "", "toMap", "", "toString", "app_debug"})
public final class User {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String id = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String email = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String fullName = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String phoneNumber = null;
    private final long createdAt = 0L;
    private boolean isEmailVerified;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String profilePictureUrl = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String givenName = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String familyName = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String displayName = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String provider = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String googleId = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String facebookId = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String twitterId = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long lastSignInTime = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long verificationSentAt = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String verificationToken = null;
    private final boolean hasCompletedAppVerification = false;
    private final long lastUpdated = 0L;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String street = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String barangay = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String city = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String verificationMethod = null;
    
    public User(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String fullName, @org.jetbrains.annotations.NotNull()
    java.lang.String phoneNumber, long createdAt, boolean isEmailVerified, @org.jetbrains.annotations.Nullable()
    java.lang.String profilePictureUrl, @org.jetbrains.annotations.Nullable()
    java.lang.String givenName, @org.jetbrains.annotations.Nullable()
    java.lang.String familyName, @org.jetbrains.annotations.Nullable()
    java.lang.String displayName, @org.jetbrains.annotations.Nullable()
    java.lang.String provider, @org.jetbrains.annotations.Nullable()
    java.lang.String googleId, @org.jetbrains.annotations.Nullable()
    java.lang.String facebookId, @org.jetbrains.annotations.Nullable()
    java.lang.String twitterId, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastSignInTime, @org.jetbrains.annotations.Nullable()
    java.lang.Long verificationSentAt, @org.jetbrains.annotations.Nullable()
    java.lang.String verificationToken, boolean hasCompletedAppVerification, long lastUpdated, @org.jetbrains.annotations.Nullable()
    java.lang.String street, @org.jetbrains.annotations.Nullable()
    java.lang.String barangay, @org.jetbrains.annotations.Nullable()
    java.lang.String city, @org.jetbrains.annotations.Nullable()
    java.lang.String verificationMethod) {
        super();
    }
    
    @com.google.firebase.firestore.DocumentId()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getEmail() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFullName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPhoneNumber() {
        return null;
    }
    
    public final long getCreatedAt() {
        return 0L;
    }
    
    @com.google.firebase.firestore.PropertyName(value = "isEmailVerified")
    public final boolean isEmailVerified() {
        return false;
    }
    
    @com.google.firebase.firestore.PropertyName(value = "isEmailVerified")
    public final void setEmailVerified(boolean p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getProfilePictureUrl() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getGivenName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getFamilyName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDisplayName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getProvider() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getGoogleId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getFacebookId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getTwitterId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getLastSignInTime() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getVerificationSentAt() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getVerificationToken() {
        return null;
    }
    
    public final boolean getHasCompletedAppVerification() {
        return false;
    }
    
    public final long getLastUpdated() {
        return 0L;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getStreet() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getBarangay() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCity() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getVerificationMethod() {
        return null;
    }
    
    /**
     * Convert user to a map for Firestore updates
     */
    @com.google.firebase.firestore.Exclude()
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.lang.Object> toMap() {
        return null;
    }
    
    public User() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component10() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component11() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component12() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component13() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component14() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component15() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component16() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component17() {
        return null;
    }
    
    public final boolean component18() {
        return false;
    }
    
    public final long component19() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component20() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component21() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component22() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component23() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    public final long component5() {
        return 0L;
    }
    
    public final boolean component6() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.bikerental.models.User copy(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String fullName, @org.jetbrains.annotations.NotNull()
    java.lang.String phoneNumber, long createdAt, boolean isEmailVerified, @org.jetbrains.annotations.Nullable()
    java.lang.String profilePictureUrl, @org.jetbrains.annotations.Nullable()
    java.lang.String givenName, @org.jetbrains.annotations.Nullable()
    java.lang.String familyName, @org.jetbrains.annotations.Nullable()
    java.lang.String displayName, @org.jetbrains.annotations.Nullable()
    java.lang.String provider, @org.jetbrains.annotations.Nullable()
    java.lang.String googleId, @org.jetbrains.annotations.Nullable()
    java.lang.String facebookId, @org.jetbrains.annotations.Nullable()
    java.lang.String twitterId, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastSignInTime, @org.jetbrains.annotations.Nullable()
    java.lang.Long verificationSentAt, @org.jetbrains.annotations.Nullable()
    java.lang.String verificationToken, boolean hasCompletedAppVerification, long lastUpdated, @org.jetbrains.annotations.Nullable()
    java.lang.String street, @org.jetbrains.annotations.Nullable()
    java.lang.String barangay, @org.jetbrains.annotations.Nullable()
    java.lang.String city, @org.jetbrains.annotations.Nullable()
    java.lang.String verificationMethod) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}