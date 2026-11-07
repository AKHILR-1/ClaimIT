const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Cloud Function triggered when a new Item is created.
 * Broadcasts FCM push notifications to all verified users in the same organization (excluding uploader).
 */
exports.onItemCreated = functions.database
  .ref("/organizations/{orgId}/items/{itemId}")
  .onCreate(async (snapshot, context) => {
    const itemData = snapshot.val();
    if (!itemData) return null;

    const orgId = context.params.orgId;
    const authorUid = itemData.userId;
    const itemType = itemData.type || "FOUND";
    const title = itemData.title || "Lost & Found Item";
    const category = itemData.category || "Item";

    // 1. Fetch user FCM tokens in the same organization
    const fcmTokensSnap = await admin.database().ref("/fcmTokens").once("value");
    const tokens = [];

    if (fcmTokensSnap.exists()) {
      fcmTokensSnap.forEach((userSnap) => {
        const uid = userSnap.key;
        if (uid !== authorUid) {
          userSnap.forEach((deviceSnap) => {
            const tokenVal = deviceSnap.child("token").val();
            const tokenOrg = deviceSnap.child("organizationId").val();
            if (tokenVal && tokenOrg === orgId) {
              tokens.push(tokenVal);
            }
          });
        }
      });
    }

    if (tokens.length === 0) {
      console.log("No recipient device tokens found for org:", orgId);
      return null;
    }

    // 2. Build FCM Multicast Payload
    const notificationTitle = itemType === "FOUND" ? "New Found Item" : "New Lost Item";
    const notificationBody = `A new ${category} item '${title}' was reported in your college campus.`;

    const message = {
      notification: {
        title: notificationTitle,
        body: notificationBody,
      },
      data: {
        itemId: context.params.itemId,
        type: itemType === "FOUND" ? "NEW_FOUND_ITEM" : "NEW_LOST_ITEM",
        organizationId: orgId,
      },
      tokens: tokens,
    };

    try {
      const response = await admin.messaging().sendEachForMulticast(message);
      console.log(`Successfully sent ${response.successCount} multicast messages.`);
    } catch (error) {
      console.error("Error broadcasting FCM message:", error);
    }
    return null;
  });

/**
 * Cloud Function triggered when a new Claim request is created.
 * Sends FCM push notification to the item uploader.
 */
exports.onClaimCreated = functions.database
  .ref("/claims/{claimId}")
  .onCreate(async (snapshot, context) => {
    const claim = snapshot.val();
    if (!claim) return null;

    const uploaderUid = claim.uploaderUid || claim.itemOwnerOrFinderId;
    if (!uploaderUid) return null;

    // Fetch uploader tokens
    const tokensSnap = await admin.database().ref(`/fcmTokens/${uploaderUid}`).once("value");
    if (!tokensSnap.exists()) return null;

    const tokens = [];
    tokensSnap.forEach((child) => {
      const token = child.child("token").val();
      if (token) tokens.push(token);
    });

    if (tokens.length === 0) return null;

    const message = {
      notification: {
        title: "New Claim Request",
        body: `Someone submitted an ownership claim for your item '${claim.itemTitle}'.`,
      },
      data: {
        claimId: context.params.claimId,
        itemId: claim.itemId || "",
        type: "NEW_CLAIM_REQUEST",
      },
      tokens: tokens,
    };

    try {
      await admin.messaging().sendEachForMulticast(message);
    } catch (error) {
      console.error("Error sending claim creation FCM:", error);
    }
    return null;
  });

/**
 * Cloud Function triggered when a Claim status updates.
 * Sends FCM notifications to claimant and uploader.
 */
exports.onClaimStatusUpdated = functions.database
  .ref("/claims/{claimId}/status")
  .onUpdate(async (change, context) => {
    const newStatus = change.after.val();
    const claimSnap = await admin.database().ref(`/claims/${context.params.claimId}`).once("value");
    if (!claimSnap.exists()) return null;

    const claim = claimSnap.val();
    const claimantUid = claim.claimantUid || claim.claimantId;

    if (!claimantUid) return null;

    const tokensSnap = await admin.database().ref(`/fcmTokens/${claimantUid}`).once("value");
    if (!tokensSnap.exists()) return null;

    const tokens = [];
    tokensSnap.forEach((child) => {
      const token = child.child("token").val();
      if (token) tokens.push(token);
    });

    if (tokens.length === 0) return null;

    let title = "Claim Status Update";
    let body = `Your claim for '${claim.itemTitle}' status changed to ${newStatus}.`;

    if (newStatus === "APPROVED") {
      title = "Your Claim Has Been Approved";
      body = `The uploader approved your claim for '${claim.itemTitle}'. Arrange a safe handover.`;
    } else if (newStatus === "REJECTED") {
      title = "Claim Decision Update";
      body = `Your claim for '${claim.itemTitle}' was rejected by the uploader.`;
    } else if (newStatus === "RETURNED") {
      title = "Item Successfully Returned";
      body = `The handover for '${claim.itemTitle}' has been confirmed returned. Thank you for using ClaimIT!`;
    }

    const message = {
      notification: { title, body },
      data: {
        claimId: context.params.claimId,
        itemId: claim.itemId || "",
        type: `CLAIM_${newStatus}`,
      },
      tokens: tokens,
    };

    try {
      await admin.messaging().sendEachForMulticast(message);
    } catch (error) {
      console.error("Error sending claim status update FCM:", error);
    }
    return null;
  });

/**
 * Cloud Function triggered when a new Student Moderation Report is created.
 * Sends FCM notification to the administrator.
 */
exports.onModerationReportCreated = functions.database
  .ref("/moderationReports/{reportId}")
  .onCreate(async (snapshot, context) => {
    const report = snapshot.val();
    if (!report) return null;

    // Fetch admin tokens (assuming admin uid = user_admin_1 or query users with role ADMIN)
    const adminUid = "user_admin_1";
    const tokensSnap = await admin.database().ref(`/fcmTokens/${adminUid}`).once("value");
    if (!tokensSnap.exists()) return null;

    const tokens = [];
    tokensSnap.forEach((child) => {
      const token = child.child("token").val();
      if (token) tokens.push(token);
    });

    if (tokens.length === 0) return null;

    const message = {
      notification: {
        title: "New Moderation Report",
        body: `Report [${report.reportReference || "REF"}] submitted for item '${report.itemName || report.targetTitleOrName}'.`,
      },
      data: {
        reportId: context.params.reportId,
        itemId: report.itemId || report.targetId || "",
        type: "MODERATION_REPORT_SUBMITTED",
      },
      tokens: tokens,
    };

    try {
      await admin.messaging().sendEachForMulticast(message);
    } catch (error) {
      console.error("Error sending moderation report FCM:", error);
    }
    return null;
  });

/**
 * Cloud Function triggered when an Admin Audit action is created (e.g. Listing Removal).
 * Sends FCM notification to item owner.
 */
exports.onModerationAuditCreated = functions.database
  .ref("/moderationAudit/{auditId}")
  .onCreate(async (snapshot, context) => {
    const audit = snapshot.val();
    if (!audit || audit.action !== "REMOVED" || !audit.itemId) return null;

    // Fetch item owner
    const itemSnap = await admin.database().ref(`/items/${audit.itemId}`).once("value");
    if (!itemSnap.exists()) return null;

    const ownerUid = itemSnap.child("userId").val() || itemSnap.child("ownerUid").val();
    if (!ownerUid) return null;

    const tokensSnap = await admin.database().ref(`/fcmTokens/${ownerUid}`).once("value");
    if (!tokensSnap.exists()) return null;

    const tokens = [];
    tokensSnap.forEach((child) => {
      const token = child.child("token").val();
      if (token) tokens.push(token);
    });

    if (tokens.length === 0) return null;

    const itemName = itemSnap.child("title").val() || "Listing";
    const message = {
      notification: {
        title: "Your Listing Has Been Removed",
        body: `Your ClaimIT listing '${itemName}' has been removed by an administrator. Reason: ${audit.reason || "Policy Violation"}.`,
      },
      data: {
        itemId: audit.itemId,
        type: "LISTING_REMOVED_BY_ADMIN",
      },
      tokens: tokens,
    };

    try {
      await admin.messaging().sendEachForMulticast(message);
    } catch (error) {
      console.error("Error sending listing removal FCM:", error);
    }
    return null;
  });
