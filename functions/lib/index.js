"use strict";
/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.updateUserBlockStatus = exports.deleteUser = void 0;
const https_1 = require("firebase-functions/v2/https");
const logger = __importStar(require("firebase-functions/logger"));
const app_1 = require("firebase-admin/app");
const auth_1 = require("firebase-admin/auth");
const firestore_1 = require("firebase-admin/firestore");
// Initialize Firebase Admin SDK
(0, app_1.initializeApp)();
// Function to delete a user from Firebase Authentication
exports.deleteUser = (0, https_1.onCall)({ cors: true }, async (request) => {
    const { data, auth } = request;
    // Check if the user is authenticated
    if (!auth) {
        throw new https_1.HttpsError("unauthenticated", "User must be authenticated");
    }
    // Check if the user has admin privileges
    const db = (0, firestore_1.getFirestore)();
    const userDoc = await db.collection('users').doc(auth.uid).get();
    const userData = userDoc.data();
    // More flexible admin check - case insensitive and multiple fields
    const isAdmin = userData && (
        userData.role?.toLowerCase() === 'admin' ||
        userData.isAdmin === true ||
        userData.isAdmin === 'true' ||
        userData.role?.toLowerCase() === 'administrator'
    );
    if (!isAdmin) {
        throw new https_1.HttpsError("permission-denied", "Only admins can delete users");
    }
    const { userId } = data;
    if (!userId) {
        throw new https_1.HttpsError("invalid-argument", "User ID is required");
    }
    try {
        // Delete user from Firebase Authentication
        const authService = (0, auth_1.getAuth)();
        await authService.deleteUser(userId);
        logger.info(`User ${userId} deleted from Authentication by admin ${auth.uid}`);
        return { success: true, message: "User deleted successfully" };
    }
    catch (error) {
        logger.error("Error deleting user from Authentication:", error);
        if (error.code === 'auth/user-not-found') {
            // User doesn't exist in Authentication, but that's okay
            logger.info(`User ${userId} not found in Authentication, continuing...`);
            return { success: true, message: "User deleted successfully" };
        }
        throw new https_1.HttpsError("internal", `Failed to delete user: ${error.message}`);
    }
});
// Function to update user block status (optional, can be done client-side too)
exports.updateUserBlockStatus = (0, https_1.onCall)({ cors: true }, async (request) => {
    const { data, auth } = request;
    if (!auth) {
        throw new https_1.HttpsError("unauthenticated", "User must be authenticated");
    }
    const db = (0, firestore_1.getFirestore)();
    const userDoc = await db.collection('users').doc(auth.uid).get();
    const userData = userDoc.data();
    // More flexible admin check - case insensitive and multiple fields
    const isAdmin = userData && (
        userData.role?.toLowerCase() === 'admin' ||
        userData.isAdmin === true ||
        userData.isAdmin === 'true' ||
        userData.role?.toLowerCase() === 'administrator'
    );
    if (!isAdmin) {
        throw new https_1.HttpsError("permission-denied", "Only admins can block/unblock users");
    }
    const { userId, isBlocked } = data;
    if (!userId || typeof isBlocked !== 'boolean') {
        throw new https_1.HttpsError("invalid-argument", "User ID and block status are required");
    }
    try {
        // Update user's custom claims in Authentication
        const authService = (0, auth_1.getAuth)();
        await authService.setCustomUserClaims(userId, {
            blocked: isBlocked
        });
        logger.info(`User ${userId} ${isBlocked ? 'blocked' : 'unblocked'} by admin ${auth.uid}`);
        return { success: true, message: `User ${isBlocked ? 'blocked' : 'unblocked'} successfully` };
    }
    catch (error) {
        logger.error("Error updating user block status:", error);
        if (error.code === 'auth/user-not-found') {
            throw new https_1.HttpsError("not-found", "User not found in Authentication");
        }
        throw new https_1.HttpsError("internal", `Failed to update user status: ${error.message}`);
    }
});
// Start writing functions
// https://firebase.google.com/docs/functions/typescript
// export const helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
