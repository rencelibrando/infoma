#!/usr/bin/env node

/**
 * Deploy only Firestore rules script
 * This script deploys only the firestore.rules without affecting hosting or functions
 */

const { exec } = require('child_process');
const path = require('path');

console.log('🔧 Deploying Firestore rules only...');
console.log('This will update your Firebase security rules to allow dashboard access.');

// Change to the correct directory
process.chdir(__dirname);

// Deploy only firestore rules
const command = 'firebase deploy --only firestore:rules';

console.log(`Running: ${command}`);

exec(command, (error, stdout, stderr) => {
  if (error) {
    console.error('❌ Error deploying rules:', error);
    console.error('Make sure you are logged in to Firebase and have the correct project selected.');
    console.error('Run: firebase login && firebase use <your-project-id>');
    return;
  }

  if (stderr) {
    console.warn('⚠️ Warning:', stderr);
  }

  console.log('✅ Firestore rules deployed successfully!');
  console.log(stdout);
  console.log('\n🎉 Your dashboard should now be able to access Firebase data.');
  console.log('Try refreshing your admin dashboard to see if the data loads properly.');
}); 