// create-simple-admin.js - Create admin with simple password
const { initializeApp } = require('firebase/app');
const { getAuth, createUserWithEmailAndPassword } = require('firebase/auth');
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

async function createSimpleAdminAccount() {
  const adminEmail = 'admin@test.com';
  const adminPassword = 'admin123';
  
  try {
    console.log('🔐 Creating simple admin account...');
    
    // Create the user account
    const userCredential = await createUserWithEmailAndPassword(auth, adminEmail, adminPassword);
    const user = userCredential.user;
    
    console.log('✅ User account created:', user.uid);
    
    // Create admin user document in Firestore
    await setDoc(doc(db, 'users', user.uid), {
      email: adminEmail,
      fullName: 'Test Administrator',
      role: 'admin',
      createdAt: new Date(),
      isActive: true,
      permissions: ['read', 'write', 'admin']
    });
    
    console.log('✅ Admin user document created in Firestore');
    console.log('');
    console.log('🎉 SIMPLE ADMIN ACCOUNT CREATED!');
    console.log('');
    console.log('📧 Email:', adminEmail);
    console.log('🔑 Password:', adminPassword);
    console.log('');
    console.log('You can now use these credentials to log in!');
    
  } catch (error) {
    if (error.code === 'auth/email-already-in-use') {
      console.log('ℹ️  This admin account already exists!');
      console.log('📧 Email:', adminEmail);
      console.log('🔑 Password:', adminPassword);
      console.log('');
      console.log('Try logging in with these credentials.');
    } else {
      console.error('❌ Error creating admin account:', error.message);
      
      // If it's a weak password error, suggest alternatives
      if (error.code === 'auth/weak-password') {
        console.log('');
        console.log('🔒 The password "admin123" might be too weak.');
        console.log('Try these stronger alternatives:');
        console.log('  Email: admin@bambike.com');
        console.log('  Password: Admin123!');
        console.log('');
        console.log('Or manually create an account in Firebase Console with a strong password.');
      }
    }
  }
  
  process.exit(0);
}

// Run the script
createSimpleAdminAccount(); 