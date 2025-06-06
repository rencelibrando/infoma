// add-admin-permissions.js - Add admin user to admins collection
const { initializeApp } = require('firebase/app');
const { getAuth, signInWithEmailAndPassword } = require('firebase/auth');
const { getFirestore, doc, setDoc, getDoc } = require('firebase/firestore');

const firebaseConfig = {
  apiKey: "AIzaSyCmhSJa07ZS67ZcKnJOmwHHMz-qzKhjShE",
  authDomain: "bike-rental-bc5bd.firebaseapp.com",
  databaseURL: "https://bike-rental-bc5bd-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "bike-rental-bc5bd",
  storageBucket: "bike-rental-bc5bd.firebasestorage.app",
  messagingSenderId: "862099405823",
  appId: "1:862099405823:android:4c6ad9bdb7c6515e2545ca",
  measurementId: "G-WTS4L56WBP"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

async function addAdminPermissions() {
  const adminEmail = 'admin@bambike.com';
  const adminPassword = 'Admin123!';
  
  try {
    console.log('🔐 Signing in as admin...');
    
    // Sign in as admin
    const userCredential = await signInWithEmailAndPassword(auth, adminEmail, adminPassword);
    const user = userCredential.user;
    
    console.log('✅ Admin signed in:', user.uid);
    
    // Check if admin document already exists
    const adminDocRef = doc(db, 'admins', user.uid);
    const adminDoc = await getDoc(adminDocRef);
    
    if (adminDoc.exists()) {
      console.log('ℹ️  Admin permissions already exist for this user');
      console.log('📄 Existing admin data:', adminDoc.data());
    } else {
      // Create admin document in admins collection
      await setDoc(adminDocRef, {
        email: adminEmail,
        fullName: 'System Administrator',
        role: 'admin',
        createdAt: new Date(),
        isActive: true,
        permissions: ['read', 'write', 'admin', 'manage-payments', 'manage-users', 'manage-bikes']
      });
      
      console.log('✅ Admin document created in admins collection');
    }
    
    // Update user document with admin role
    const userDocRef = doc(db, 'users', user.uid);
    const userDoc = await getDoc(userDocRef);
    
    if (userDoc.exists()) {
      console.log('📄 User document exists, updating with admin role...');
      await setDoc(userDocRef, {
        ...userDoc.data(),
        role: 'admin',
        isAdmin: true,
        updatedAt: new Date()
      }, { merge: true });
      console.log('✅ User document updated with admin role');
    } else {
      console.log('📄 Creating user document...');
      await setDoc(userDocRef, {
        email: adminEmail,
        fullName: 'System Administrator',
        role: 'admin',
        isAdmin: true,
        createdAt: new Date(),
        isActive: true,
        permissions: ['read', 'write', 'admin']
      });
      console.log('✅ User document created');
    }
    
    console.log('');
    console.log('🎉 ADMIN PERMISSIONS CONFIGURED SUCCESSFULLY!');
    console.log('');
    console.log('📧 Admin Email:', adminEmail);
    console.log('🔑 Password:', adminPassword);
    console.log('🆔 User ID:', user.uid);
    console.log('');
    console.log('The admin can now:');
    console.log('• View all payments in the dashboard');
    console.log('• Approve/reject payment submissions');
    console.log('• Manage users and bookings');
    console.log('• Access all admin features');
    
  } catch (error) {
    console.error('❌ Error setting up admin permissions:', error.message);
    console.log('');
    console.log('🛠️  Please try:');
    console.log('1. Make sure the admin account exists (run create-admin.js first)');
    console.log('2. Check Firebase security rules');
    console.log('3. Verify Firebase configuration');
  }
  
  process.exit(0);
}

// Run the script
addAdminPermissions(); 