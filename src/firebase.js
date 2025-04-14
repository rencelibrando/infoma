// src/firebase.js
import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getAuth } from "firebase/auth";

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


const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const storage = getStorage(app);
const auth = getAuth(app);

export { db, storage, auth };