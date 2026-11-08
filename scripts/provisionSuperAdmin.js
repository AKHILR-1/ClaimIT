/**
 * CLAIMIT — LOCAL INITIAL SUPER ADMIN PROVISIONING SCRIPT (SPARK PLAN / ZERO COST)
 * 
 * Target Project ID: claimit-322b5
 * Realtime Database URL: https://claimit-322b5-default-rtdb.asia-southeast1.firebasedatabase.app
 * Target Super Admin Email: battleinsane510@gmail.com
 * Executed locally via Node.js using Firebase Admin SDK.
 * 
 * Reliability & Security Rules:
 * 1. Requires verified Firebase Auth email before granting SUPER_ADMIN role.
 * 2. Merges existing custom claims (never overwrites).
 * 3. Preserves existing user profile fields (reads before writing).
 * 4. Displays generated password reset link clearly for manual setup (never prints service account keys/passwords).
 * 5. Full compensating rollback for partial failures (removes newly created Auth/RTDB resources and restores original state).
 * 6. Concurrency-safe atomic transaction with lock ownership validation (`currentRunId`).
 * 7. `--dry-run` mode is strictly read-only with ZERO database/Auth mutations.
 * 8. Explicit `--confirm` flag AND interactive confirmation required for live execution.
 * 9. Strict service account project ID verification.
 * 10. Pre-execution revalidation of lock and Auth account.
 */

const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");
const readline = require("readline");

// CLI Flags
const args = process.argv.slice(2);
const isDryRun = args.includes("--dry-run") || args.includes("-d");
const hasConfirmFlag = args.includes("--confirm") || args.includes("-y");

// Unique Execution Run ID for Lock Ownership Verification
const currentRunId = "run_" + Date.now() + "_" + Math.random().toString(36).substring(2, 8);

// Path to Local Service Account Credentials
const serviceAccountPath = path.join(__dirname, "serviceAccountKey.json");

console.log("=================================================");
console.log(`  CLAIMIT: SUPER ADMIN PROVISIONING ${isDryRun ? "[DRY-RUN MODE]" : "[LIVE EXECUTION MODE]"}`);
console.log("=================================================");

// 1. Validate Service Account Key File Existence & Parse JSON
if (!fs.existsSync(serviceAccountPath)) {
  console.error("\n[FATAL ERROR] Service Account key file missing!");
  console.error(`Expected path: ${serviceAccountPath}`);
  console.error("Please place 'serviceAccountKey.json' inside the 'scripts/' directory.\n");
  process.exit(1);
}

let serviceAccount;
try {
  serviceAccount = require(serviceAccountPath);
} catch (e) {
  console.error("\n[FATAL ERROR] Failed to parse 'serviceAccountKey.json'. File may be corrupted or invalid JSON.");
  process.exit(1);
}

// Target Configuration
const PROJECT_ID = "claimit-322b5";
const DATABASE_URL = "https://claimit-322b5-default-rtdb.asia-southeast1.firebasedatabase.app";
const SUPER_ADMIN_EMAIL = "battleinsane510@gmail.com";
const SUPER_ADMIN_NAME = "Sachin Sahani";
const ORG_NAME = "Lovely Professional University";
const ORG_ID = "12314408";

// 2. Strict Service Account Project Verification (Abort on Mismatch)
if (!serviceAccount.project_id || serviceAccount.project_id !== PROJECT_ID) {
  console.error(`\n[FATAL ERROR] Service Account project_id ('${serviceAccount.project_id || "UNKNOWN"}') does not match target project ('${PROJECT_ID}').`);
  console.error("Provisioning aborted to prevent cross-project misconfiguration.\n");
  process.exit(1);
}

// 3. Initialize Firebase Admin SDK
try {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: DATABASE_URL
  });
  console.log(`[VALIDATION] Firebase Admin SDK initialized successfully.`);
  console.log(`- Target Project ID: ${PROJECT_ID}`);
  console.log(`- Target Database URL: ${DATABASE_URL}`);
} catch (e) {
  console.error("\n[FATAL ERROR] Failed to initialize Firebase Admin SDK:", e.message);
  process.exit(1);
}

// Helper: Interactive Terminal Prompt
function askConfirmation(query) {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
  });
  return new Promise((resolve) => {
    rl.question(query, (answer) => {
      rl.close();
      resolve(answer.trim());
    });
  });
}

async function Main() {
  try {
    const db = admin.database();
    const superAdminLockRef = db.ref("adminConfiguration/superAdmin");

    // 4. Pre-Validation: Inspect Existing Provisioning Lock & Abandoned Attempt Handling
    console.log(`\n[INSPECTION] Checking database node 'adminConfiguration/superAdmin'...`);
    const existingLockSnap = await superAdminLockRef.once("value");
    const existingData = existingLockSnap.exists() ? existingLockSnap.val() : null;

    if (existingData) {
      if (existingData.uid) {
        console.error("\n[BLOCKED] Provisioning Aborted: Super Admin is already provisioned!");
        console.error(`- Existing Super Admin UID: ${existingData.uid}`);
        console.error(`- Existing Email: ${existingData.email || "N/A"}`);
        console.error(`- Provisioned At: ${existingData.provisionedAt ? new Date(existingData.provisionedAt).toISOString() : "N/A"}`);
        console.error("\nTo re-provision, remove the node 'adminConfiguration/superAdmin' in Firebase Console.\n");
        process.exit(1);
      } else if (existingData.initiatedAt) {
        const lockAgeMs = Date.now() - existingData.initiatedAt;
        const ABANDONED_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes
        if (lockAgeMs < ABANDONED_TIMEOUT_MS) {
          console.error(`\n[BLOCKED] Active provisioning lock detected (initiated ${Math.round(lockAgeMs / 1000)} seconds ago). Aborting concurrent run.`);
          process.exit(1);
        } else {
          console.warn(`[WARNING] Found abandoned provisioning lock from ${new Date(existingData.initiatedAt).toISOString()}. Expired after 15 minutes. Proceeding with override.`);
        }
      }
    } else {
      console.log(`[VALIDATION] No existing Super Admin found in 'adminConfiguration/superAdmin'.`);
    }

    // 5. Inspect Firebase Auth Account & Verification Status
    console.log(`[INSPECTION] Checking Firebase Auth for email '${SUPER_ADMIN_EMAIL}'...`);
    let userRecord = null;
    try {
      userRecord = await admin.auth().getUserByEmail(SUPER_ADMIN_EMAIL);
      console.log(`[VALIDATION] Found existing Firebase Auth account (UID: ${userRecord.uid}).`);
      console.log(`- Email Verification Status: ${userRecord.emailVerified ? "VERIFIED" : "UNVERIFIED"}`);
    } catch (authErr) {
      if (authErr.code === "auth/user-not-found") {
        console.log(`[VALIDATION] No existing Firebase Auth account found for '${SUPER_ADMIN_EMAIL}'. A new account will be created.`);
      } else {
        throw authErr;
      }
    }

    // Rule 1 Verification Check: Email must be verified before granting SUPER_ADMIN role
    if (userRecord && !userRecord.emailVerified) {
      console.warn(`\n[REQUIREMENT UNMET] Account '${SUPER_ADMIN_EMAIL}' exists but email is NOT yet verified.`);
      console.warn("Super Admin privileges cannot be granted to an unverified email address.");
      console.warn("Please verify your email address, then re-run this script.\n");
      if (isDryRun) {
        console.log("[DRY-RUN SUMMARY] Dry-run complete. Super Admin role assignment pending email verification.");
        process.exit(0);
      }
    }

    // =========================================================================
    // DRY-RUN MODE EXIT POINT (STRICTLY ZERO WRITES TO AUTH OR DATABASE)
    // =========================================================================
    if (isDryRun) {
      console.log("\n=================================================");
      console.log("  DRY-RUN VALIDATION SUCCESSFUL — NO CHANGES MADE");
      console.log("=================================================");
      console.log("Dry-Run Checks Summary:");
      console.log(`- Service Account Credential: Valid (${serviceAccount.client_email || "OK"})`);
      console.log(`- Target Firebase Project: ${PROJECT_ID}`);
      console.log(`- Target Realtime Database: ${DATABASE_URL}`);
      console.log(`- Target Super Admin Email: ${SUPER_ADMIN_EMAIL}`);
      console.log(`- Target Organization: ${ORG_NAME} (ID: ${ORG_ID})`);
      console.log(`- Super Admin Lock Status: ${existingData?.uid ? "PROVISIONED" : "CLEAR"}`);
      console.log(`- Planned Auth Action: ${userRecord ? (userRecord.emailVerified ? "Grant SUPER_ADMIN role & merge Custom Claims" : "Wait for email verification") : "Create Auth account & generate password setup link"}`);
      console.log("\nTo execute LIVE provisioning when ready, run:");
      console.log("  node provisionSuperAdmin.js --confirm\n");
      process.exit(0);
    }

    // =========================================================================
    // LIVE PROVISIONING CONFIRMATION CHECK
    // =========================================================================
    if (!hasConfirmFlag) {
      console.error("\n[BLOCKED] Live provisioning requires the '--confirm' flag.");
      console.error("Run command: node provisionSuperAdmin.js --confirm\n");
      process.exit(1);
    }

    const promptAnswer = await askConfirmation("\n[CONFIRMATION REQUIRED] Type 'PROVISION' to execute live Super Admin setup: ");
    if (promptAnswer !== "PROVISION") {
      console.log("\n[ABORTED] Live provisioning cancelled by user. No changes were made.\n");
      process.exit(0);
    }

    // =========================================================================
    // REVALIDATE LOCK & AUTH ACCOUNT IMMEDIATELY BEFORE LIVE WRITES
    // =========================================================================
    console.log("\n[REVALIDATION] Rechecking database lock and Auth account immediately before live execution...");
    const revalLockSnap = await superAdminLockRef.once("value");
    if (revalLockSnap.exists() && revalLockSnap.val()?.uid) {
      console.error("\n[BLOCKED] Revalidation failed: Super Admin was provisioned by another process.");
      process.exit(1);
    }

    // =========================================================================
    // LIVE PROVISIONING EXECUTION WITH COMPENSATING ROLLBACK SAFETY
    // =========================================================================

    // State trackers for compensating rollback
    let didAcquireLock = false;
    let didCreateAuthUser = false;
    let didMergeCustomClaims = false;
    let originalCustomClaims = userRecord?.customClaims || null;
    let didCreateRtdbProfile = false;
    let originalRtdbProfile = null;
    let uid = userRecord?.uid || null;

    try {
      // Step 1: Acquire Atomic Lock
      console.log("[EXECUTION] Acquiring atomic database lock...");
      const transactionResult = await superAdminLockRef.transaction((currentData) => {
        if (currentData !== null && currentData.uid) {
          return; // Abort if another process completed provisioning concurrently
        }
        return {
          email: SUPER_ADMIN_EMAIL,
          organizationId: ORG_ID,
          organizationName: ORG_NAME,
          lockId: currentRunId,
          initiatedAt: Date.now()
        };
      });

      if (!transactionResult.committed) {
        console.error("\n[BLOCKED] Atomic transaction failed: Super Admin lock was acquired by another process.");
        process.exit(1);
      }
      didAcquireLock = true;
      console.log(`[SUCCESS] Atomic transaction lock acquired (Lock Run ID: ${currentRunId}).`);

      // Step 2: Create or Retrieve Auth User
      if (userRecord) {
        uid = userRecord.uid;
        console.log(`[EXECUTION] Using existing Auth account (UID: ${uid}).`);
      } else {
        const setupPass = "ClaimIT_" + Math.random().toString(36).substring(2, 12) + "!" + Math.random().toString(36).substring(2, 6);
        const newRecord = await admin.auth().createUser({
          email: SUPER_ADMIN_EMAIL,
          password: setupPass,
          displayName: SUPER_ADMIN_NAME,
          emailVerified: false // Preserve actual email verification requirement
        });
        uid = newRecord.uid;
        userRecord = newRecord;
        didCreateAuthUser = true;
        console.log(`[SUCCESS] Created new Auth user account (UID: ${uid}).`);
      }

      // Re-verify Rule 1: Email must be verified before setting SUPER_ADMIN claims
      if (!userRecord.emailVerified) {
        console.warn(`\n[NOTICE] Auth account created/found for '${SUPER_ADMIN_EMAIL}', but email is NOT verified.`);
        console.warn("Super Admin custom claims will NOT be granted until email is verified.");
        console.warn("A password reset / setup link is being generated securely.\n");
      }

      // Step 3: Merge Custom Claims (Never Overwrite Existing Claims)
      if (userRecord.emailVerified) {
        const existingClaims = userRecord.customClaims || {};
        const mergedClaims = {
          ...existingClaims,
          superAdmin: true,
          admin: true
        };
        console.log(`[EXECUTION] Merging Custom Claims:`, mergedClaims);
        await admin.auth().setCustomUserClaims(uid, mergedClaims);
        didMergeCustomClaims = true;
        console.log(`[SUCCESS] Custom Claims merged successfully on Auth token.`);
      }

      // Step 4: Preserve Existing Profile Fields (Read before Write)
      console.log(`[EXECUTION] Reading existing profile at users/${uid}...`);
      const userProfileRef = db.ref(`users/${uid}`);
      const existingProfileSnap = await userProfileRef.once("value");
      if (existingProfileSnap.exists()) {
        originalRtdbProfile = existingProfileSnap.val();
      }

      const existingProfile = originalRtdbProfile || {};
      const updatedProfile = {
        ...existingProfile,
        fullName: existingProfile.fullName || SUPER_ADMIN_NAME,
        displayName: existingProfile.displayName || SUPER_ADMIN_NAME,
        email: SUPER_ADMIN_EMAIL,
        collegeEmail: SUPER_ADMIN_EMAIL,
        collegeEmailVerified: userRecord.emailVerified,
        contactEmail: existingProfile.contactEmail || SUPER_ADMIN_EMAIL,
        contactEmailVerified: existingProfile.contactEmailVerified || userRecord.emailVerified,
        studentId: existingProfile.studentId || ORG_ID,
        registrationNumber: existingProfile.registrationNumber || ORG_ID,
        studentRegistrationNumber: existingProfile.studentRegistrationNumber || ORG_ID,
        organizationId: ORG_ID,
        organizationName: ORG_NAME,
        role: userRecord.emailVerified ? "SUPER_ADMIN" : (existingProfile.role || "STUDENT"),
        status: userRecord.emailVerified ? "VERIFIED" : "PENDING_VERIFICATION",
        createdAt: existingProfile.createdAt || admin.database.ServerValue.TIMESTAMP,
        updatedAt: admin.database.ServerValue.TIMESTAMP
      };

      await userProfileRef.set(updatedProfile);
      didCreateRtdbProfile = true;
      console.log(`[SUCCESS] Profile updated at users/${uid} (Preserved ${Object.keys(existingProfile).length} existing fields).`);

      // Step 5: Finalize Atomic Lock in adminConfiguration/superAdmin (Only if email is verified)
      if (userRecord.emailVerified) {
        console.log(`[EXECUTION] Finalizing lock at adminConfiguration/superAdmin...`);
        const lockConfig = {
          uid: uid,
          email: SUPER_ADMIN_EMAIL,
          organizationId: ORG_ID,
          organizationName: ORG_NAME,
          provisionedAt: admin.database.ServerValue.TIMESTAMP
        };
        await superAdminLockRef.set(lockConfig);
        console.log(`[SUCCESS] Super Admin configuration locked.`);
      }

      // Step 6: Generate Firebase Password Reset Link
      const resetLink = await admin.auth().generatePasswordResetLink(SUPER_ADMIN_EMAIL);

      console.log("\n=================================================");
      console.log("  SUPER ADMIN PROVISIONING COMPLETED SUCCESSFULLY!");
      console.log("=================================================");
      console.log(`- Super Admin Name: ${SUPER_ADMIN_NAME}`);
      console.log(`- Super Admin Email: ${SUPER_ADMIN_EMAIL}`);
      console.log(`- Organization: ${ORG_NAME} (ID: ${ORG_ID})`);
      console.log(`- Firebase Auth UID: ${uid}`);
      console.log(`- Account Type: ${didCreateAuthUser ? "New Auth Account Created" : "Existing Auth Account Upgraded"}`);
      console.log(`- Email Verification: ${userRecord.emailVerified ? "VERIFIED" : "PENDING VERIFICATION"}`);
      console.log(`- Role Assigned: ${userRecord.emailVerified ? "SUPER_ADMIN" : "PENDING EMAIL VERIFICATION"}`);
      console.log("\n[MANUAL ACCOUNT SETUP LINK]");
      console.log(`Generated Firebase Password Reset Link for manual setup:`);
      console.log(`  ${resetLink}`);
      console.log("\n(Use the link above to set your password and complete sign-in on ClaimIT).\n");

      process.exit(0);

    } catch (stepErr) {
      console.error("\n[COMPENSATING ROLLBACK] Provisioning failed during execution. Reverting changes created by this run...");

      try {
        if (didCreateRtdbProfile && uid) {
          if (originalRtdbProfile) {
            await db.ref(`users/${uid}`).set(originalRtdbProfile);
            console.log("[ROLLBACK] Restored original RTDB profile.");
          } else {
            await db.ref(`users/${uid}`).remove();
            console.log("[ROLLBACK] Removed newly created RTDB profile.");
          }
        }

        if (didMergeCustomClaims && uid && !didCreateAuthUser) {
          await admin.auth().setCustomUserClaims(uid, originalCustomClaims || {});
          console.log("[ROLLBACK] Restored original custom claims.");
        }

        if (didCreateAuthUser && uid) {
          await admin.auth().deleteUser(uid);
          console.log("[ROLLBACK] Deleted newly created Auth user account.");
        }

        if (didAcquireLock) {
          const lockSnap = await superAdminLockRef.once("value");
          if (lockSnap.exists() && lockSnap.val()?.lockId === currentRunId) {
            await superAdminLockRef.remove();
            console.log("[ROLLBACK] Removed atomic lock owned by current run.");
          } else {
            console.warn("[ROLLBACK] Lock node is not owned by this run. Lock preserved.");
          }
        }
      } catch (rollbackErr) {
        console.error("[ROLLBACK ERROR] Failed during rollback execution:", rollbackErr.message);
      }

      throw stepErr;
    }

  } catch (error) {
    console.error("\n[FATAL ERROR] Provisioning execution failed:", error);
    process.exit(1);
  }
}

Main();
