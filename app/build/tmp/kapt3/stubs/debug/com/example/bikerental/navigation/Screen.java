package com.example.bikerental.navigation;

/**
 * Sealed class representing all navigation destinations in the app.
 * This centralizes route definitions and prevents hardcoded strings.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0013\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0010\u0007\b\t\n\u000b\f\r\u000e\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016B\u000f\b\u0004\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u0082\u0001\u0010\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f !\"#$%&\u00a8\u0006\'"}, d2 = {"Lcom/example/bikerental/navigation/Screen;", "", "route", "", "(Ljava/lang/String;)V", "getRoute", "()Ljava/lang/String;", "AccessAccount", "BikeDetails", "BikeList", "BikeUpload", "BookingDetails", "Bookings", "ChangePassword", "EditProfile", "EmailVerification", "GoogleVerification", "Help", "Home", "Initial", "Profile", "SignIn", "SignUp", "Lcom/example/bikerental/navigation/Screen$AccessAccount;", "Lcom/example/bikerental/navigation/Screen$BikeDetails;", "Lcom/example/bikerental/navigation/Screen$BikeList;", "Lcom/example/bikerental/navigation/Screen$BikeUpload;", "Lcom/example/bikerental/navigation/Screen$BookingDetails;", "Lcom/example/bikerental/navigation/Screen$Bookings;", "Lcom/example/bikerental/navigation/Screen$ChangePassword;", "Lcom/example/bikerental/navigation/Screen$EditProfile;", "Lcom/example/bikerental/navigation/Screen$EmailVerification;", "Lcom/example/bikerental/navigation/Screen$GoogleVerification;", "Lcom/example/bikerental/navigation/Screen$Help;", "Lcom/example/bikerental/navigation/Screen$Home;", "Lcom/example/bikerental/navigation/Screen$Initial;", "Lcom/example/bikerental/navigation/Screen$Profile;", "Lcom/example/bikerental/navigation/Screen$SignIn;", "Lcom/example/bikerental/navigation/Screen$SignUp;", "app_debug"})
public abstract class Screen {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String route = null;
    
    private Screen(java.lang.String route) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRoute() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$AccessAccount;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class AccessAccount extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.AccessAccount INSTANCE = null;
        
        private AccessAccount() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0004\u00a8\u0006\u0006"}, d2 = {"Lcom/example/bikerental/navigation/Screen$BikeDetails;", "Lcom/example/bikerental/navigation/Screen;", "()V", "createRoute", "", "bikeId", "app_debug"})
    public static final class BikeDetails extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.BikeDetails INSTANCE = null;
        
        private BikeDetails() {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String createRoute(@org.jetbrains.annotations.NotNull()
        java.lang.String bikeId) {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$BikeList;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class BikeList extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.BikeList INSTANCE = null;
        
        private BikeList() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$BikeUpload;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class BikeUpload extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.BikeUpload INSTANCE = null;
        
        private BikeUpload() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0004\u00a8\u0006\u0006"}, d2 = {"Lcom/example/bikerental/navigation/Screen$BookingDetails;", "Lcom/example/bikerental/navigation/Screen;", "()V", "createRoute", "", "bookingId", "app_debug"})
    public static final class BookingDetails extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.BookingDetails INSTANCE = null;
        
        private BookingDetails() {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String createRoute(@org.jetbrains.annotations.NotNull()
        java.lang.String bookingId) {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$Bookings;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class Bookings extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.Bookings INSTANCE = null;
        
        private Bookings() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$ChangePassword;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class ChangePassword extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.ChangePassword INSTANCE = null;
        
        private ChangePassword() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$EditProfile;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class EditProfile extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.EditProfile INSTANCE = null;
        
        private EditProfile() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$EmailVerification;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class EmailVerification extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.EmailVerification INSTANCE = null;
        
        private EmailVerification() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$GoogleVerification;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class GoogleVerification extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.GoogleVerification INSTANCE = null;
        
        private GoogleVerification() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$Help;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class Help extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.Help INSTANCE = null;
        
        private Help() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$Home;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class Home extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.Home INSTANCE = null;
        
        private Home() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$Initial;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class Initial extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.Initial INSTANCE = null;
        
        private Initial() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$Profile;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class Profile extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.Profile INSTANCE = null;
        
        private Profile() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$SignIn;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class SignIn extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.SignIn INSTANCE = null;
        
        private SignIn() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/bikerental/navigation/Screen$SignUp;", "Lcom/example/bikerental/navigation/Screen;", "()V", "app_debug"})
    public static final class SignUp extends com.example.bikerental.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.bikerental.navigation.Screen.SignUp INSTANCE = null;
        
        private SignUp() {
        }
    }
}