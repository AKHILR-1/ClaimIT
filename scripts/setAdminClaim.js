
const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

const adminEmail = process.env.ADMIN_EMAIL;

if (!adminEmail) {
    throw new Error("ADMIN_EMAIL is missing.");
}

if (serviceAccount.project_id !== "claimit-322b5") {
    throw new Error("Wrong Firebase project.");
}

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

async function assignAdmin() {
    const user = await admin.auth().getUserByEmail(adminEmail);

    const existingClaims = user.customClaims || {};

    await admin.auth().setCustomUserClaims(user.uid, {
        ...existingClaims,
        admin: true,
        role: "SUPER_ADMIN"
    });

    console.log("SUPER ADMIN CLAIM ASSIGNED SUCCESSFULLY.");
    console.log("Firebase UID found: true");
    console.log("Admin claim: true");
}

assignAdmin()
    .then(() => process.exit(0))
    .catch(error => {
        console.error("Admin assignment failed:", error.message);
        process.exit(1);
    });
