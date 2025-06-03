// src/firebase.js
import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getAuth } from "firebase/auth";
import { getDatabase } from "firebase/database";

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

// Initialize Firebase first
const app = initializeApp(firebaseConfig);

// Initialize Firestore, Storage, Auth, and Realtime Database
const db = getFirestore(app);
const storage = getStorage(app);
const auth = getAuth(app);
const realtimeDb = getDatabase(app);

// Only initialize App Check in production environment
// This allows local development to work without reCAPTCHA verification
if (process.env.NODE_ENV === 'production') {
  import('firebase/app-check').then(({ initializeAppCheck, ReCaptchaV3Provider }) => {
    initializeAppCheck(app, {
      provider: new ReCaptchaV3Provider('6LfqjBsrAAAAAMs93cei_7rFTn2hXKLPvL-sEKFr'),
      isTokenAutoRefreshEnabled: true
    });
    console.log('Firebase App Check initialized for production');
  }).catch(error => {
    console.error('Error initializing App Check:', error);
  });
} else {
  console.log('Firebase App Check disabled for development environment');
}

export { db, storage, auth, realtimeDb };