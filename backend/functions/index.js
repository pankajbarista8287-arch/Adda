/**
 * ADDA Backend Cloud Functions (Firebase Functions v2)
 * Deploy via `firebase deploy --only functions`
 */
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();
// Configure named database ID from firebase_applet_config.xml when deploying:
const DATABASE_ID = process.env.FIRESTORE_DATABASE_ID || "ai-studio-android-adda-5ef52753-d5a7-4725-8c85-0adb20f4fde9";

/**
 * Server-Side Payment Gateway (Razorpay / UPI) Verification & Coin Credit
 * Ensures users can never forge or self-credit wallet balances.
 */
exports.verifyPaymentAndCreditCoins = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }
  const { orderId, razorpayOrderId, razorpayPaymentId, razorpaySignature } = request.data || {};
  const secret = process.env.RAZORPAY_KEY_SECRET;
  if (!secret) {
    throw new HttpsError("failed-precondition", "RAZORPAY_KEY_SECRET is not configured on server.");
  }
  const expectedSig = crypto
    .createHmac("sha256", secret)
    .update(`${razorpayOrderId}|${razorpayPaymentId}`)
    .digest("hex");

  if (expectedSig !== razorpaySignature) {
    throw new HttpsError("permission-denied", "Invalid payment signature.");
  }

  const orderRef = db.collection("recharge_orders").doc(orderId);
  await db.runTransaction(async (tx) => {
    const orderSnap = await tx.get(orderRef);
    if (!orderSnap.exists) throw new HttpsError("not-found", "Order not found.");
    const order = orderSnap.data();
    if (order.status !== "PENDING") {
      throw new HttpsError("already-exists", "Order already processed.");
    }
    const walletRef = db.collection("wallets").doc(order.userId);
    const walletSnap = await tx.get(walletRef);
    const currentCoins = walletSnap.exists ? walletSnap.get("coins") || 0 : 0;
    const currentInr = walletSnap.exists ? walletSnap.get("totalRechargedInr") || 0 : 0;

    tx.update(orderRef, {
      status: "VERIFIED",
      verifiedBy: "SERVER_RAZORPAY",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.update(walletRef, {
      coins: currentCoins + order.coinsAmount,
      totalRechargedInr: currentInr + order.amountInr,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.set(db.collection("transactions").doc(), {
      senderId: "ADDA_SYSTEM",
      senderName: "ADDA Gateway",
      receiverId: order.userId,
      receiverName: order.userName || "",
      type: "RECHARGE_CREDIT",
      amountCoins: order.coinsAmount,
      amountDiamonds: 0,
      giftId: order.packageId,
      giftName: order.packageLabel || "",
      quantity: 1,
      contextId: orderId,
      note: `Verified INR ${order.amountInr} (${razorpayPaymentId})`,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return { success: true };
});

/**
 * Automated Mission Coin Payout when a user completes & claims a daily/weekly mission.
 */
exports.onMissionClaimed = onDocumentUpdated("users/{userId}/missions/{missionId}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (!before.claimed && after.claimed && after.rewardCoins > 0) {
    const walletRef = db.collection("wallets").doc(event.params.userId);
    await walletRef.update({
      coins: admin.firestore.FieldValue.increment(after.rewardCoins),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }
});
