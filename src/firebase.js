// src/firebase.js
import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getAuth } from "firebase/auth";
import { getDatabase } from "firebase/database";
// Import App Check at the top level
import { initializeAppCheck, ReCaptchaV3Provider, CustomProvider } from "firebase/app-check";

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

// Initialize Firebase first
const app = initializeApp(firebaseConfig);

// Temporarily skip App Check initialization for development
let appCheck;

// DEVELOPMENT ONLY: Skip App Check to allow login
// We'll pass a placeholder value for appCheck so other code doesn't break
appCheck = { 
  getToken: () => Promise.resolve({ 
    token: 'dev-token-mock',
    expireTimeMillis: Date.now() + 3600000
  })
};

console.log('⚠️ WARNING: App Check is DISABLED. This should only be used in development.');

// Initialize Firestore, Storage, Auth, and Realtime Database
const db = getFirestore(app);
const storage = getStorage(app);
const auth = getAuth(app);
const realtimeDb = getDatabase(app);

export { db, storage, auth, realtimeDb, appCheck };