const { initializeApp } = require("firebase/app");
const { getFirestore, collection, getDocs, doc, getDoc, query, where } = require("firebase/firestore");

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

async function testPaymentRetrieval() {
  try {
    console.log('🔍 Testing payment retrieval from web app...');
    
    // 1. Check if any payments exist
    console.log('\n1. Checking all payments...');
    const allPayments = await getDocs(collection(db, 'payments'));
    console.log(`Total payments found: ${allPayments.size}`);
    
    if (allPayments.size === 0) {
      console.log('❌ No payments found in the collection');
    } else {
      console.log('✅ Payments exist, listing all:');
      allPayments.forEach((doc) => {
        console.log(`  - ID: ${doc.id}`);
        console.log(`  - Data:`, doc.data());
        console.log('  ---');
      });
    }
    
    // 2. Search for specific payment ID from mobile logs
    console.log('\n2. Searching for specific payment: 5606b6d2-e80e-426c-a608-bd20a82352fd');
    const specificDoc = await getDoc(doc(db, 'payments', '5606b6d2-e80e-426c-a608-bd20a82352fd'));
    if (specificDoc.exists()) {
      console.log('✅ Specific payment found:', specificDoc.data());
    } else {
      console.log('❌ Specific payment not found');
    }
    
    // 3. Search by user ID
    console.log('\n3. Searching by user ID: g7cVdPQdAIhwqmS3TA2XYYywKp63');
    const userQuery = query(
      collection(db, 'payments'),
      where('userId', '==', 'g7cVdPQdAIhwqmS3TA2XYYywKp63')
    );
    const userResults = await getDocs(userQuery);
    console.log(`Found ${userResults.size} payments for this user`);
    userResults.forEach((doc) => {
      console.log(`  - Payment: ${doc.id}`, doc.data());
    });
    
    // 4. Search by amount
    console.log('\n4. Searching by amount: 200');
    const amountQuery = query(
      collection(db, 'payments'),
      where('amount', '==', 200.0)
    );
    const amountResults = await getDocs(amountQuery);
    console.log(`Found ${amountResults.size} payments with amount 200`);
    amountResults.forEach((doc) => {
      console.log(`  - Payment: ${doc.id}`, doc.data());
    });
    
    // 5. Search for recent payments (last hour)
    console.log('\n5. Searching for recent payments...');
    const recentTime = new Date(Date.now() - 60 * 60 * 1000); // 1 hour ago
    console.log('Looking for payments after:', recentTime);
    
    const recentPayments = await getDocs(collection(db, 'payments'));
    const recentList = [];
    recentPayments.forEach((doc) => {
      const data = doc.data();
      if (data.createdAt && data.createdAt.toDate() > recentTime) {
        recentList.push({ id: doc.id, ...data });
      }
    });
    
    console.log(`Found ${recentList.length} recent payments`);
    recentList.forEach((payment) => {
      console.log(`  - Recent: ${payment.id}`, payment);
    });
    
    console.log('\n✅ Test completed');
    
  } catch (error) {
    console.error('❌ Test failed:', error);
    console.error('Error details:', {
      code: error.code,
      message: error.message,
      stack: error.stack
    });
  }
}

// Run the test
testPaymentRetrieval().then(() => {
  console.log('\n🎉 Test completed! The web dashboard should now be able to display payments.');
  console.log('\n🌐 Access the dashboard at: http://localhost:3000');
  console.log('📊 Check the browser console for payment loading logs.');
}); 