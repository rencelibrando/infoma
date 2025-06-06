// setup-admin.js
// Script to add admin users to the Firebase admins collection
// Run this script to set up admin users who can delete bikes

const admin = require('firebase-admin');
const readline = require('readline');

// Initialize Firebase Admin SDK
const serviceAccount = {
  // You'll need to replace these with your actual Firebase Admin SDK credentials
  // Get them from: Firebase Console > Project Settings > Service Accounts > Generate New Private Key
  "type": "service_account",
  "project_id": "bike-rental-bc5bd",
  "private_key_id": "your_private_key_id",
  "private_key": "your_private_key",
  "client_email": "your_client_email",
  "client_id": "your_client_id",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "your_cert_url"
};

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://bike-rental-bc5bd-default-rtdb.asia-southeast1.firebasedatabase.app"
});

const db = admin.firestore();

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

async function addAdminUser() {
  return new Promise((resolve) => {
    rl.question('Enter the email address of the user to make admin: ', async (email) => {
      try {
        // Get user by email
        const userRecord = await admin.auth().getUserByEmail(email);
        
        // Add user to admins collection
        await db.collection('admins').doc(userRecord.uid).set({
          email: email,
          role: 'admin',
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          createdBy: 'setup-script'
        });
        
        console.log(`✅ Successfully added ${email} as admin with UID: ${userRecord.uid}`);
        
        // Set custom claims (optional, for compatibility with existing auth logic)
        await admin.auth().setCustomUserClaims(userRecord.uid, { admin: true });
        console.log(`✅ Set admin custom claims for ${email}`);
        
        resolve();
      } catch (error) {
        console.error('❌ Error adding admin user:', error.message);
        resolve();
      }
    });
  });
}

async function listAdmins() {
  try {
    const adminsSnapshot = await db.collection('admins').get();
    console.log('\n📋 Current Admin Users:');
    console.log('========================');
    
    if (adminsSnapshot.empty) {
      console.log('No admin users found.');
    } else {
      adminsSnapshot.forEach(doc => {
        const data = doc.data();
        console.log(`- ${data.email} (UID: ${doc.id})`);
      });
    }
  } catch (error) {
    console.error('❌ Error listing admins:', error.message);
  }
}

async function main() {
  console.log('🚀 Firebase Admin Setup Tool');
  console.log('=============================');
  console.log('This tool helps you set up admin users who can delete bikes.');
  console.log('\nNOTE: Make sure to update the serviceAccount credentials in this file first!\n');
  
  await listAdmins();
  
  const shouldAdd = await new Promise((resolve) => {
    rl.question('\nDo you want to add a new admin user? (y/n): ', (answer) => {
      resolve(answer.toLowerCase() === 'y' || answer.toLowerCase() === 'yes');
    });
  });
  
  if (shouldAdd) {
    await addAdminUser();
    await listAdmins();
  }
  
  console.log('\n✅ Done!');
  process.exit(0);
}

// Handle errors
process.on('unhandledRejection', (error) => {
  console.error('❌ Unhandled error:', error);
  process.exit(1);
});

main().catch(console.error); 