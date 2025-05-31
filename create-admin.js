// create-admin.js - Run this script to create an admin account
const { initializeApp } = require('firebase/app');
const { getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword } = require('firebase/auth');
const { getFirestore, doc, setDoc } = require('firebase/firestore');

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

async function createAdminAccount() {
  const adminEmail = 'admin@bambike.com';
  const adminPassword = 'Admin123!';
  
  try {
    console.log('🔐 Creating admin account...');
    
    // Create the user account
    const userCredential = await createUserWithEmailAndPassword(auth, adminEmail, adminPassword);
    const user = userCredential.user;
    
    console.log('✅ User account created:', user.uid);
    
    // Create admin user document in Firestore
    await setDoc(doc(db, 'users', user.uid), {
      email: adminEmail,
      fullName: 'System Administrator',
      role: 'admin',
      createdAt: new Date(),
      isActive: true,
      permissions: ['read', 'write', 'admin']
    });
    
    console.log('✅ Admin user document created in Firestore');
    console.log('');
    console.log('🎉 ADMIN ACCOUNT CREATED SUCCESSFULLY!');
    console.log('');
    console.log('📧 Email:', adminEmail);
    console.log('🔑 Password:', adminPassword);
    console.log('');
    console.log('You can now use these credentials to log in to your application!');
    
  } catch (error) {
    if (error.code === 'auth/email-already-in-use') {
      console.log('ℹ️  Admin account already exists!');
      console.log('📧 Email:', adminEmail);
      console.log('🔑 Password:', adminPassword);
      console.log('');
      console.log('Try logging in with these credentials.');
    } else {
      console.error('❌ Error creating admin account:', error.message);
      console.log('');
      console.log('🛠️  Alternative options:');
      console.log('1. Go to Firebase Console → Authentication → Users');
      console.log('2. Manually create a user with email/password');
      console.log('3. Or try these test credentials if they exist:');
      console.log('   Email: admin@test.com');
      console.log('   Password: password123');
    }
  }
  
  process.exit(0);
}

// Run the script
createAdminAccount(); 