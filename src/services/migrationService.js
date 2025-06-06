import { db } from '../firebase';
import { collection, getDocs, doc, updateDoc, query, where, writeBatch } from "firebase/firestore";

/**
 * Migration service to help transition from hardwareId to qrCode field structure
 */

/**
 * Migrate existing bikes from hardwareId to qrCode field
 * This ensures backward compatibility while moving to the new field structure
 */
export const migrateBikesToQRCodeField = async () => {
  try {
    console.log('Starting migration of bikes to qrCode field...');
    
    const bikesCollection = collection(db, "bikes");
    const snapshot = await getDocs(bikesCollection);
    
    const migrations = [];
    let migratedCount = 0;
    let skippedCount = 0;
    
    for (const docSnapshot of snapshot.docs) {
      const bike = { id: docSnapshot.id, ...docSnapshot.data() };
      const updateData = {};
      
      // If bike has hardwareId but no qrCode, copy hardwareId to qrCode
      if (bike.hardwareId && !bike.qrCode) {
        updateData.qrCode = bike.hardwareId;
        console.log(`Migrating bike ${bike.id}: hardwareId -> qrCode (${bike.hardwareId})`);
        migratedCount++;
      } else if (bike.qrCode) {
        console.log(`Skipping bike ${bike.id}: already has qrCode (${bike.qrCode})`);
        skippedCount++;
        continue;
      } else {
        console.log(`Skipping bike ${bike.id}: no hardwareId to migrate`);
        skippedCount++;
        continue;
      }
      
      // Add migration timestamp
      updateData.migratedAt = new Date();
      updateData.lastUpdated = new Date();
      
      // Only update if there are changes
      if (Object.keys(updateData).length > 0) {
        const bikeRef = doc(db, "bikes", bike.id);
        migrations.push(updateDoc(bikeRef, updateData));
      }
    }
    
    // Execute all migrations
    await Promise.all(migrations);
    
    const result = {
      totalBikes: snapshot.docs.length,
      migratedCount,
      skippedCount,
      success: true
    };
    
    console.log('Migration completed:', result);
    return result;
    
  } catch (error) {
    console.error('Error during migration:', error);
    return {
      success: false,
      error: error.message
    };
  }
};

/**
 * Validate that all bikes have proper QR codes
 */
export const validateBikeQRCodes = async () => {
  try {
    console.log('Validating bike QR codes...');
    
    const bikesCollection = collection(db, "bikes");
    const snapshot = await getDocs(bikesCollection);
    
    const validation = {
      totalBikes: snapshot.docs.length,
      validQRCodes: 0,
      validHardwareIds: 0,
      missingBoth: 0,
      duplicateQRCodes: [],
      issues: []
    };
    
    const qrCodeMap = new Map();
    const hardwareIdMap = new Map();
    
    for (const docSnapshot of snapshot.docs) {
      const bike = { id: docSnapshot.id, ...docSnapshot.data() };
      
      // Check qrCode
      if (bike.qrCode) {
        validation.validQRCodes++;
        
        // Check for duplicates
        if (qrCodeMap.has(bike.qrCode)) {
          validation.duplicateQRCodes.push({
            qrCode: bike.qrCode,
            bikes: [qrCodeMap.get(bike.qrCode), bike.id]
          });
        } else {
          qrCodeMap.set(bike.qrCode, bike.id);
        }
      }
      
      // Check hardwareId
      if (bike.hardwareId) {
        validation.validHardwareIds++;
        
        // Check for duplicates
        if (hardwareIdMap.has(bike.hardwareId)) {
          validation.issues.push({
            type: 'duplicate_hardwareId',
            hardwareId: bike.hardwareId,
            bikes: [hardwareIdMap.get(bike.hardwareId), bike.id]
          });
        } else {
          hardwareIdMap.set(bike.hardwareId, bike.id);
        }
      }
      
      // Check if missing both
      if (!bike.qrCode && !bike.hardwareId) {
        validation.missingBoth++;
        validation.issues.push({
          type: 'missing_identifiers',
          bikeId: bike.id,
          bikeName: bike.name
        });
      }
      
      // Check if qrCode and hardwareId are different (potential issue)
      if (bike.qrCode && bike.hardwareId && bike.qrCode !== bike.hardwareId) {
        validation.issues.push({
          type: 'identifier_mismatch',
          bikeId: bike.id,
          qrCode: bike.qrCode,
          hardwareId: bike.hardwareId
        });
      }
    }
    
    console.log('Validation completed:', validation);
    return validation;
    
  } catch (error) {
    console.error('Error during validation:', error);
    return {
      success: false,
      error: error.message
    };
  }
};

/**
 * Check for QR code collisions across the database
 */
export const checkQRCodeCollisions = async (qrCode) => {
  try {
    // Check qrCode field
    const qrCodeQuery = query(collection(db, "bikes"), where("qrCode", "==", qrCode));
    const qrCodeSnapshot = await getDocs(qrCodeQuery);
    
    // Check hardwareId field for backward compatibility
    const hardwareIdQuery = query(collection(db, "bikes"), where("hardwareId", "==", qrCode));
    const hardwareIdSnapshot = await getDocs(hardwareIdQuery);
    
    const collisions = [];
    
    qrCodeSnapshot.docs.forEach(doc => {
      collisions.push({
        bikeId: doc.id,
        field: 'qrCode',
        value: qrCode,
        data: doc.data()
      });
    });
    
    hardwareIdSnapshot.docs.forEach(doc => {
      collisions.push({
        bikeId: doc.id,
        field: 'hardwareId', 
        value: qrCode,
        data: doc.data()
      });
    });
    
    return {
      hasCollisions: collisions.length > 0,
      collisions
    };
    
  } catch (error) {
    console.error('Error checking QR code collisions:', error);
    return {
      hasCollisions: false,
      error: error.message
    };
  }
};

/**
 * Generate report of current QR code status
 */
export const generateQRCodeReport = async () => {
  try {
    const validation = await validateBikeQRCodes();
    
    const report = {
      timestamp: new Date().toISOString(),
      summary: {
        totalBikes: validation.totalBikes,
        bikesWithQRCode: validation.validQRCodes,
        bikesWithHardwareId: validation.validHardwareIds,
        bikesWithoutIdentifiers: validation.missingBoth,
        duplicateQRCodes: validation.duplicateQRCodes.length,
        totalIssues: validation.issues.length
      },
      details: validation,
      recommendations: []
    };
    
    // Generate recommendations
    if (validation.missingBoth > 0) {
      report.recommendations.push(
        `${validation.missingBoth} bikes are missing both QR code and hardware ID. Run migration to fix.`
      );
    }
    
    if (validation.duplicateQRCodes.length > 0) {
      report.recommendations.push(
        `${validation.duplicateQRCodes.length} duplicate QR codes found. These need to be resolved manually.`
      );
    }
    
    const bikesWithOnlyHardwareId = validation.validHardwareIds - validation.validQRCodes;
    if (bikesWithOnlyHardwareId > 0) {
      report.recommendations.push(
        `${bikesWithOnlyHardwareId} bikes have only hardwareId. Consider running migration to copy to qrCode field.`
      );
    }
    
    if (validation.issues.length === 0) {
      report.recommendations.push('All bikes have proper QR code configuration!');
    }
    
    return report;
    
  } catch (error) {
    console.error('Error generating QR code report:', error);
    return {
      success: false,
      error: error.message
    };
  }
};

// Function to migrate existing bikes to include maintenance status
export const migrateBikesWithMaintenanceStatus = async () => {
  try {
    console.log('Starting bike maintenance status migration...');
    
    // Get all bikes
    const bikesRef = collection(db, 'bikes');
    const snapshot = await getDocs(bikesRef);
    
    if (snapshot.empty) {
      console.log('No bikes found to migrate');
      return { success: true, updated: 0 };
    }
    
    let updatedCount = 0;
    const batch = writeBatch(db);
    
    snapshot.docs.forEach(doc => {
      const bikeData = doc.data();
      
      // Check if bike already has maintenance status
      if (!bikeData.maintenanceStatus) {
        const bikeRef = doc.ref;
        
        // Default all bikes to operational status during migration
        // This ensures that availability is not controlled by maintenance status
        // unless explicitly set by an admin
        const initialStatus = 'operational';
        
        const updateData = {
          maintenanceStatus: initialStatus,
          maintenanceNotes: '',
          maintenanceLastUpdated: new Date(),
          maintenanceUpdatedBy: 'system_migration'
        };
        
        batch.update(bikeRef, updateData);
        updatedCount++;
        
        console.log(`Queued bike ${doc.id} for maintenance status migration (status: ${initialStatus})`);
      }
    });
    
    if (updatedCount > 0) {
      await batch.commit();
      console.log(`✅ Successfully migrated ${updatedCount} bikes with maintenance status`);
    } else {
      console.log('All bikes already have maintenance status - no migration needed');
    }
    
    return { success: true, updated: updatedCount };
    
  } catch (error) {
    console.error('Error migrating bikes with maintenance status:', error);
    return { success: false, error: error.message };
  }
};

// Function to fix bikes that were incorrectly set to maintenance status during migration
export const fixIncorrectMaintenanceStatus = async () => {
  try {
    console.log('Starting fix for incorrect maintenance status...');
    
    // Get all bikes with maintenance status but no maintenance notes
    const bikesRef = collection(db, 'bikes');
    const q = query(bikesRef, where('maintenanceStatus', '!=', 'operational'));
    const snapshot = await getDocs(q);
    
    if (snapshot.empty) {
      console.log('No bikes with non-operational status found');
      return { success: true, fixed: 0 };
    }
    
    let fixedCount = 0;
    const batch = writeBatch(db);
    
    snapshot.docs.forEach(doc => {
      const bikeData = doc.data();
      
      // If the bike has maintenance status but was set by system migration and has no notes,
      // it was probably incorrectly migrated
      if (bikeData.maintenanceUpdatedBy === 'system_migration' && 
          (!bikeData.maintenanceNotes || bikeData.maintenanceNotes.trim() === '')) {
        
        const bikeRef = doc.ref;
        const updateData = {
          maintenanceStatus: 'operational',
          maintenanceNotes: '',
          maintenanceLastUpdated: new Date(),
          maintenanceUpdatedBy: 'system_fix'
        };
        
        batch.update(bikeRef, updateData);
        fixedCount++;
        
        console.log(`Queued bike ${doc.id} for maintenance status fix (${bikeData.maintenanceStatus} -> operational)`);
      }
    });
    
    if (fixedCount > 0) {
      await batch.commit();
      console.log(`✅ Successfully fixed ${fixedCount} bikes with incorrect maintenance status`);
    } else {
      console.log('No bikes needed fixing - all maintenance statuses appear correct');
    }
    
    return { success: true, fixed: fixedCount };
    
  } catch (error) {
    console.error('Error fixing incorrect maintenance status:', error);
    return { success: false, error: error.message };
  }
}; 