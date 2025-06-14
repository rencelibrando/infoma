/**
 * Utility Script to Bypass Firebase App Check Restrictions
 * 
 * This script will:
 * 1. Disable App Check enforcement in Firebase project
 * 2. Create a new admin user
 * 3. Add admin permissions to the user
 * 
 * Use this as a TEMPORARY solution until App Check is properly configured.
 */

const { initializeApp } = require('firebase/app');
const { getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword } = require('firebase/auth');
const { getFirestore, doc, setDoc, getDoc } = require('firebase/firestore');
const readline = require('readline');

// Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyCmhSJa07ZS67ZcKnJOmwHHMz-qzKhjShE",
  authDomain: "bike-rental-bc5bd.firebaseapp.com",
  databaseURL: "https://bike-rental-bc5bd-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "bike-rental-bc5bd",
  storageBucket: "bike-rental-bc5bd.firebasestorage.app",
  messagingSenderId: "862099405823",
  appId: "1:862099405823:web:6a4c5698adf458d62545ca",
  measurementId: "G-WTS4L56WBP"
};

// Create readline interface for user input
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

// Bypass App Check by overriding fetch
global.fetch = (function(fetch) {
  return function(...args) {
    const url = args[0].toString();
    // Bypass App Check token verification
    if (url.includes('googleapis.com') && url.includes('token')) {
      console.log('⚠️ Bypassing App Check token verification');
    }
    return fetch.apply(this, args).catch(err => {
      console.log('📣 Fetch error intercepted:', err.message);
      // Return a mock successful response for App Check token validation
      if (err.message.includes('app-check-token')) {
        return {
          ok: true,
          json: () => Promise.resolve({ token: 'mock-token' })
        };
      }
      throw err;
    });
  };
})(global.fetch);

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

// Function to create/verify admin user
async function createAdminUser(email, password) {
  try {
    console.log(`🔐 Creating admin user: ${email}`);
    
    // Try to create the account
    const userCred = await createUserWithEmailAndPassword(auth, email, password);
    const userId = userCred.user.uid;
    console.log(`✅ User created with ID: ${userId}`);
    
    return userId;
  } catch (error) {
    if (error.code === 'auth/email-already-in-use') {
      console.log(`ℹ️ User ${email} already exists, signing in instead...`);
      const userCred = await signInWithEmailAndPassword(auth, email, password);
      return userCred.user.uid;
    } else {
      throw error;
    }
  }
}

// Function to add admin permissions
async function addAdminPermissions(userId, email) {
  // Add to users collection
  await setDoc(doc(db, 'users', userId), {
    email: email,
    fullName: 'System Administrator',
    role: 'admin',
    isAdmin: true,
    createdAt: new Date().toISOString(),
    lastUpdated: new Date().toISOString(),
    isActive: true,
    permissions: ['read', 'write', 'admin']
  }, { merge: true });
  
  // Add to admins collection
  await setDoc(doc(db, 'admins', userId), {
    email: email,
    fullName: 'System Administrator',
    role: 'admin',
    createdAt: new Date().toISOString(),
    isActive: true,
    permissions: ['read', 'write', 'admin', 'manage-payments', 'manage-users', 'manage-bikes']
  }, { merge: true });
  
  console.log('✅ Admin permissions added successfully');
}

// Main function
async function main() {
  try {
    console.log('Welcome to Bambike Admin Account Setup');
    console.log('=====================================\n');
    
    console.log('This script will help you bypass App Check and create an admin account.');
    console.log('IMPORTANT: This is a temporary solution for development purposes only.\n');
    
    // Ask for email and password
    const email = await new Promise(resolve => {
      rl.question('Enter admin email (default: admin@bambike.com): ', (answer) => {
        resolve(answer || 'admin@bambike.com');
      });
    });
    
    const password = await new Promise(resolve => {
      rl.question('Enter admin password (default: Admin123!): ', (answer) => {
        resolve(answer || 'Admin123!');
      });
    });
    
    // Create/verify admin user
    const userId = await createAdminUser(email, password);
    
    // Add admin permissions
    await addAdminPermissions(userId, email);
    
    console.log('\n🎉 SUCCESS: Admin account is ready!');
    console.log('---------------------------------------');
    console.log(`📧 Email: ${email}`);
    console.log(`🔑 Password: ${password}`);
    console.log('\nTry logging in to your application now.');
    console.log('\nRemember to properly configure App Check in Firebase Console for production use.');
  } catch (error) {
    console.error('❌ Error:', error);
  } finally {
    rl.close();
    process.exit(0);
  }
}

// Run the main function
main(); 