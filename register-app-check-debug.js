// Script to register App Check debug token
// Run this script before starting your app in development mode

const { initializeApp } = require("firebase/app");
const { initializeAppCheck, ReCaptchaV3Provider } = require("firebase/app-check");

// Your web app's Firebase configuration from firebase.js
const firebaseConfig = {
  apiKey: "AIzaSyCmhSJa07ZS67ZcKnJOmwHHMz-qzKhjShE",
  authDomain: "bike-rental-bc5bd.firebaseapp.com",
  databaseURL: "https://bike-rental-bc5bd-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "bike-rental-bc5bd",
  storageBucket: "bike-rental-bc5bd.firebasestorage.app",
  messagingSenderId: "862099405823",
  appId: "1:862099405823:web:6a4c5698adf458d62545ca", // Using the web app ID from firebase apps:list
  measurementId: "G-WTS4L56WBP"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Enable debug mode - this will output the debug token in the console
// Use global for Node.js environment
global.FIREBASE_APPCHECK_DEBUG_TOKEN = true;

// Initialize App Check
const appCheck = initializeAppCheck(app, {
  provider: new ReCaptchaV3Provider("6LfqjBsrAAAAAMs93cei_7rFTn2hXKLPvL-sEKFr"),
  isTokenAutoRefreshEnabled: true
});

console.log("App Check debug token registration complete!");
console.log("Please copy the debug token from the error message in your console");
console.log("Then register it in the Firebase Console under Project Settings > App Check");
console.log("Instructions: https://firebase.google.com/docs/app-check/web/debug-provider"); 