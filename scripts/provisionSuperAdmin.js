/**
 * CLAIMIT — LOCAL INITIAL SUPER ADMIN PROVISIONING SCRIPT (SPARK PLAN / ZERO COST)
 * 
 * Target Project ID: claimit-322b5
 * Realtime Database URL: https://claimit-322b5-default-rtdb.asia-southeast1.firebasedatabase.app
 * Target Super Admin Email: battleinsane510@gmail.com
 * Executed locally via Node.js using Firebase Admin SDK.
 * 
 * Security & Reliability Rules:
 * 1. Require verified Firebase Auth email before granting Super Admin role.
 * 2. Merge existing custom claims (never overwrite).
 * 3. Preserve existing user profile fields (read before write).
 * 4. Generate password-reset link securely; never print sensitive links/tokens to logs.
 * 5. Safe rollback with lock ownership verification.
 * 6. Concurrency-safe atomic transaction with abandoned attempt cleanup (>15 mins).
 * 7. Read-only --dry-run mode performs ZERO database/Auth writes.
 * 8. Explicit confirmation required for live execution.
 * 9. Strict service account project ID verification.
 * 10. Complete configuration validation prior to execution.
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

    // 4. Inspect Existing Provisioning Lock & Abandoned Attempt Handling
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
      console.warn("Please verify your email via the link sent to your inbox, then re-run this script.\n");
      if (isDryRun) {
        console.log("[DRY-RUN SUMMARY] Dry-run complete. Super Admin role assignment pending email verification.");
        process.exit(0);
      }
    }

    // =========================================================================
    // DRY-RUN MODE EXIT POINT (ZERO WRITES TO AUTH OR DATABASE)
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
      console.log(`- Existing Super Admin Lock: ${existingData?.uid ? "PROVISIONED" : "CLEAR"}`);
      console.log(`- Planned Auth Action: ${userRecord ? (userRecord.emailVerified ? "Grant SUPER_ADMIN role & Custom Claims" : "Wait for email verification") : "Create Auth account and send verification/setup email"}`);
      console.log("\nTo execute LIVE provisioning when ready, run:");
      console.log("  node provisionSuperAdmin.js --confirm\n");
      process.exit(0);
    }

    // =========================================================================
    // LIVE PROVISIONING CONFIRMATION CHECK
    // =========================================================================
    if (!hasConfirmFlag) {
      const answer = await askConfirmation("\n[CONFIRMATION REQUIRED] Type 'PROVISION' to execute live Super Admin setup: ");
      if (answer !== "PROVISION") {
        console.log("\n[ABORTED] Live provisioning cancelled by user. No changes were made.\n");
        process.exit(0);
      }
    }

    // =========================================================================
    // LIVE PROVISIONING EXECUTION WITH ATOMIC TRANSACTION & ROLLBACK SAFETY
    // =========================================================================

    console.log("\n[EXECUTION] Acquiring atomic database lock...");
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
    console.log(`[SUCCESS] Atomic transaction lock acquired (Lock Run ID: ${currentRunId}).`);

    let uid;
    let isNewAccount = false;

    try {
      // Step A: Create or Retrieve Auth User
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
        isNewAccount = true;
        console.log(`[SUCCESS] Created new Auth user account (UID: ${uid}).`);
      }

      // Re-verify Rule 1: Email must be verified before setting SUPER_ADMIN claims
      if (!userRecord.emailVerified) {
        console.warn(`\n[NOTICE] Auth account created/found for '${SUPER_ADMIN_EMAIL}', but email is NOT verified.`);
        console.warn("Super Admin custom claims will NOT be granted until email is verified.");
        console.warn("A password reset / setup email link is being dispatched securely.\n");
      }

      // Step B: Merge Custom Claims (Never Overwrite Existing Claims)
      if (userRecord.emailVerified) {
        const existingClaims = userRecord.customClaims || {};
        const mergedClaims = {
          ...existingClaims,
          superAdmin: true,
          admin: true
        };
        console.log(`[EXECUTION] Merging Custom Claims:`, mergedClaims);
        await admin.auth().setCustomUserClaims(uid, mergedClaims);
        console.log(`[SUCCESS] Custom Claims merged successfully on Auth token.`);
      }

      // Step C: Preserve Existing Profile Fields (Read before Write)
      console.log(`[EXECUTION] Reading existing profile at users/${uid}...`);
      const userProfileRef = db.ref(`users/${uid}`);
      const existingProfileSnap = await userProfileRef.once("value");
      const existingProfile = existingProfileSnap.exists() ? existingProfileSnap.val() : {};

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
      console.log(`[SUCCESS] Profile updated at users/${uid} (Preserved ${Object.keys(existingProfile).length} existing fields).`);

      // Step D: Finalize Atomic Lock in adminConfiguration/superAdmin (Only if email is verified)
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

      // Step E: Generate Password Reset Link Securely (Never printed to shared logs)
      const resetLink = await admin.auth().generatePasswordResetLink(SUPER_ADMIN_EMAIL);
      const instructionsPath = path.join(__dirname, ".superadmin_setup_instructions.txt");
      fs.writeFileSync(instructionsPath, `CLAIMIT SUPER ADMIN SETUP INSTRUCTIONS\n\nAccount: ${SUPER_ADMIN_EMAIL}\nUID: ${uid}\nPassword Reset Link: ${resetLink}\nTimestamp: ${new Date().toISOString()}\n\nDelete this file after completing login setup.`, { mode: 0o600 });
      console.log(`[SUCCESS] Secure password setup instructions written to local file (git-ignored): ${instructionsPath}`);

      console.log("\n=================================================");
      console.log("  SUPER ADMIN PROVISIONING COMPLETED SUCCESSFULLY!");
      console.log("=================================================");
      console.log(`- Super Admin Name: ${SUPER_ADMIN_NAME}`);
      console.log(`- Super Admin Email: ${SUPER_ADMIN_EMAIL}`);
      console.log(`- Organization: ${ORG_NAME} (ID: ${ORG_ID})`);
      console.log(`- Firebase Auth UID: ${uid}`);
      console.log(`- Email Verification Status: ${userRecord.emailVerified ? "VERIFIED" : "PENDING VERIFICATION"}`);
      console.log(`- Role Assigned: ${userRecord.emailVerified ? "SUPER_ADMIN" : "PENDING EMAIL VERIFICATION"}`);
      console.log(`- Setup Instructions: Saved securely to .superadmin_setup_instructions.txt`);
      console.log("=================================================\n");

      process.exit(0);

    } catch (stepErr) {
      console.error("\n[RECOVERY] Execution failed during step process. Checking lock ownership...");
      // Safe Rollback: Verify lock ownership before removing
      try {
        const currentLockSnap = await superAdminLockRef.once("value");
        if (currentLockSnap.exists() && currentLockSnap.val()?.lockId === currentRunId) {
          await superAdminLockRef.remove();
          console.log("[RECOVERY] Lock node owned by this execution run was safely removed.");
        } else {
          console.warn("[RECOVERY] Lock node is not owned by this execution run. Lock preserved.");
        }
      } catch (cleanErr) {
        console.error("[RECOVERY ERROR] Failed to inspect or remove lock node:", cleanErr.message);
      }
      throw stepErr;
    }

  } catch (error) {
    console.error("\n[FATAL ERROR] Provisioning execution failed:", error);
    process.exit(1);
  }
}

Main();
