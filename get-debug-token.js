/**
 * Firebase App Check Debug Token Generator
 * 
 * This script generates a UUID v4 debug token for Firebase App Check.
 * Run this script with Node.js to get a debug token.
 */

// Function to generate a proper UUID v4
function generateUUIDv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// Generate a valid UUID v4 debug token
const debugToken = generateUUIDv4();

console.log('\n===== FIREBASE APP CHECK DEBUG TOKEN (UUID v4) =====\n');
console.log(debugToken);
console.log('\n===================================================');
console.log('\nInstructions:');
console.log('1. Copy this UUID v4 debug token');
console.log('2. Go to Firebase Console > Project Settings > App Check');
console.log('3. Scroll down to "Debug tokens" section');
console.log('4. Add this token for your web app: 1:862099405823:web:6a4c5698adf458d62545ca');
console.log('5. Click "Save" and try your app again\n');

// Additional helpful information
console.log('App ID information:');
console.log('Web App ID (BambikeAdmin): 1:862099405823:web:6a4c5698adf458d62545ca');
console.log('Make sure to use this exact app ID in your firebase.js configuration\n'); 