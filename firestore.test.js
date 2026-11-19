const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const ADMIN_UID = "akki_admin_uid_001";
const ADMIN_EMAIL = "pankajbarista8287@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function futureDate() {
  return new Date(Date.now() + 120000);
}

function buildUserPayload(uid, username, now = pastDate()) {
  return {
    userId: uid,
    addaCode: "1234567",
    username,
    bio: "ADDA Voice & Gaming Lover",
    age: 22,
    gender: "OTHER",
    avatarUrl: "",
    avatarPreset: "ROYAL_LOTUS",
    level: 1,
    xp: 0,
    giftSendLevel: 1,
    giftReceiveLevel: 1,
    vipTier: "NONE",
    frameStyle: "DEFAULT",
    nameColorHex: "#F8F6FF",
    familyId: "",
    agencyId: "",
    hostStatus: "NONE",
    verified: false,
    banned: false,
    online: true,
    followersCount: 0,
    followingCount: 0,
    friendsCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function buildWalletPayload(uid, now = pastDate()) {
  return {
    userId: uid,
    coins: 100,
    diamonds: 0,
    totalSpentCoins: 0,
    totalEarnedDiamonds: 0,
    totalRechargedInr: 0,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any ADDA collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").where("banned", "==", false).get());
  await assertFails(unauth.collection("wallets").doc(ALICE_UID).get());
  await assertFails(unauth.collection("voice_rooms").where("active", "==", true).get());
  await assertFails(unauth.collection("gifts").where("active", "==", true).get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set(buildUserPayload(ALICE_UID, "Unauth"))
  );
});

test("User profile, private PII, notifications, and missions enforce strict ownership and bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  // Create Alice's user profile + private + mission
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildUserPayload(ALICE_UID, "AliceStar", now))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phone: "+919876543210",
      preferredLang: "hi",
      securityPinSet: false,
      twoFactorEnabled: false,
      privacyDiscoverable: true,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("missions").doc("daily_login").set({
      userId: ALICE_UID,
      missionKey: "DAILY_LOGIN",
      title: "Daily Login Reward",
      progress: 1,
      target: 1,
      rewardCoins: 50,
      rewardXp: 40,
      completed: true,
      claimed: false,
      updatedAt: now,
    })
  );

  // Partial profile & private updates
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      username: "AliceRoyal",
      bio: "Hindi Singer",
      age: 23,
      gender: "FEMALE",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("missions").doc("daily_login").update({
      progress: 1,
      completed: true,
      claimed: true,
      updatedAt: pastDate(1000),
    })
  );

  // Directory query matching Kotlin repository
  await assertSucceeds(aliceDb.collection("users").where("banned", "==", false).get());
  // Unfiltered directory query fails
  await assertFails(aliceDb.collection("users").get());

  // Cross-user PII read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("account").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("missions").get());

  // Bob cannot ban/verify Alice or inject @DocumentId id or future timestamp
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      banned: true,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      verified: true,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Future tamper",
      updatedAt: futureDate(),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      id: BOB_UID,
      ...buildUserPayload(BOB_UID, "BobWithId", now),
    })
  );
});

test("Wallet anti-fraud: users cannot inflate coins, and atomic gift send works across sender and receiver", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildUserPayload(ALICE_UID, "Alice", now))
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set(buildUserPayload(BOB_UID, "Bob", now))
  );
  await assertSucceeds(
    aliceDb.collection("wallets").doc(ALICE_UID).set(buildWalletPayload(ALICE_UID, now))
  );
  await assertSucceeds(
    bobDb.collection("wallets").doc(BOB_UID).set(buildWalletPayload(BOB_UID, now))
  );

  // Anti-fraud: Alice cannot self-credit coins or start with 99999 coins
  await assertFails(
    aliceDb.collection("wallets").doc(ALICE_UID).update({
      coins: 5000,
      totalSpentCoins: 0,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    bobDb.collection("wallets").doc("evil_wallet").set({
      ...buildWalletPayload(BOB_UID, now),
      coins: 99999,
    })
  );

  // Unfiltered wallet list denied for non-admin
  await assertFails(aliceDb.collection("wallets").get());
  await assertFails(aliceDb.collection("wallets").doc(BOB_UID).get());

  // Atomic gift send flow (mirrors sendGiftAtomic)
  const batch = aliceDb.batch();
  batch.update(aliceDb.collection("wallets").doc(ALICE_UID), {
    coins: 75,
    totalSpentCoins: 25,
    updatedAt: pastDate(1000),
  });
  batch.update(aliceDb.collection("wallets").doc(BOB_UID), {
    diamonds: 12,
    totalEarnedDiamonds: 12,
    updatedAt: pastDate(1000),
  });
  batch.set(aliceDb.collection("transactions").doc("tx_gift_1"), {
    senderId: ALICE_UID,
    senderName: "Alice",
    receiverId: BOB_UID,
    receiverName: "Bob",
    type: "GIFT_SEND",
    amountCoins: 25,
    amountDiamonds: 12,
    giftId: "gift_heart",
    giftName: "Neon Heart",
    quantity: 1,
    contextId: "room_1",
    note: "ROOM Gift: Neon Heart x1",
    createdAt: pastDate(1000),
  });
  batch.set(aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_1"), {
    userId: BOB_UID,
    type: "GIFT_RECEIVED",
    title: "Gift Received!",
    body: "Alice sent you Neon Heart x1 (+12 Diamonds)",
    read: false,
    targetId: "room_1",
    createdAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  // Query transactions as sender & receiver
  await assertSucceeds(
    aliceDb.collection("transactions").where("senderId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("transactions").where("receiverId", "==", BOB_UID).get()
  );
  // Unfiltered transaction query denied
  await assertFails(aliceDb.collection("transactions").get());

  // Bob can mark notification read, cannot tamper body
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_1").update({
      read: true,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_1").update({
      body: "Tampered",
    })
  );
});

test("Recharge orders remain PENDING for clients; AKKI_ADMIN verifies and credits coins", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("wallets").doc(ALICE_UID).set(buildWalletPayload(ALICE_UID, now))
  );

  // Client cannot create an already-VERIFIED recharge order
  await assertFails(
    aliceDb.collection("recharge_orders").doc("order_bad").set({
      userId: ALICE_UID,
      userName: "Alice",
      packageId: "pkg_49",
      packageLabel: "Popular Pack",
      amountInr: 49,
      coinsAmount: 550,
      paymentMethod: "UPI",
      utrReference: "UTR123456",
      status: "VERIFIED",
      verifiedBy: "SELF",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid PENDING recharge order
  await assertSucceeds(
    aliceDb.collection("recharge_orders").doc("order_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      packageId: "pkg_49",
      packageLabel: "Popular Pack",
      amountInr: 49,
      coinsAmount: 550,
      paymentMethod: "UPI",
      utrReference: "UTR987654321",
      status: "PENDING",
      verifiedBy: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Client cannot verify own order
  await assertFails(
    aliceDb.collection("recharge_orders").doc("order_1").update({
      status: "VERIFIED",
      verifiedBy: "AKKI_ADMIN",
      updatedAt: pastDate(1000),
    })
  );

  // Non-admin cannot register in /admins/{uid}
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      adminUid: ALICE_UID,
      adminCode: "AKKI_ADMIN",
      email: "fake@example.com",
      role: "SUPER_ADMIN",
      createdAt: now,
    })
  );

  // Authorized bootstrapped admin registers AKKI_ADMIN and approves order
  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      adminUid: ADMIN_UID,
      adminCode: "AKKI_ADMIN",
      email: ADMIN_EMAIL,
      role: "SUPER_ADMIN",
      createdAt: now,
    })
  );
  await assertSucceeds(adminDb.collection("recharge_orders").get());

  const approveBatch = adminDb.batch();
  approveBatch.update(adminDb.collection("recharge_orders").doc("order_1"), {
    status: "VERIFIED",
    verifiedBy: `AKKI_ADMIN (${ADMIN_UID})`,
    updatedAt: pastDate(500),
  });
  approveBatch.update(adminDb.collection("wallets").doc(ALICE_UID), {
    coins: 650,
    totalRechargedInr: 49,
    updatedAt: pastDate(500),
  });
  approveBatch.set(adminDb.collection("transactions").doc("tx_recharge_1"), {
    senderId: ADMIN_UID,
    senderName: "AKKI_ADMIN",
    receiverId: ALICE_UID,
    receiverName: "Alice",
    type: "RECHARGE_CREDIT",
    amountCoins: 550,
    amountDiamonds: 0,
    giftId: "pkg_49",
    giftName: "Popular Pack",
    quantity: 1,
    contextId: "order_1",
    note: "Verified INR 49 (UTR987654321)",
    createdAt: pastDate(500),
  });
  await assertSucceeds(approveBatch.commit());
});

test("Voice rooms, 8-16 seats, chat, PK battles, and queries align with Kotlin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  // Create Voice Room + seats + member in batch
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("voice_rooms").doc("room_1"), {
    title: "Mumbai Midnight Lounge",
    topic: "Hindi Acoustic",
    ownerId: ALICE_UID,
    ownerName: "Alice",
    roomType: "PUBLIC",
    roomPin: "",
    seatCount: 8,
    themeId: "ROYAL_STAGE",
    announcement: "Welcome to ADDA!",
    level: 1,
    xp: 0,
    onlineCount: 1,
    totalGiftCoins: 0,
    pkActive: false,
    moderatorUids: [],
    active: true,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("voice_rooms").doc("room_1").collection("seats").doc("seat_0"), {
    roomId: "room_1",
    seatIndex: 0,
    occupantId: ALICE_UID,
    occupantName: "Alice",
    occupantVip: "NONE",
    muted: false,
    locked: false,
    speaking: false,
    speakingLevel: 0,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("voice_rooms").doc("room_1").collection("members").doc(ALICE_UID), {
    roomId: "room_1",
    userId: ALICE_UID,
    username: "Alice",
    vipTier: "NONE",
    role: "OWNER",
    muted: false,
    joinedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Bob queries active rooms, seats, messages, joins room & sends chat
  await assertSucceeds(aliceDb.collection("voice_rooms").where("active", "==", true).get());
  await assertFails(bobDb.collection("voice_rooms").get());
  await assertSucceeds(
    bobDb.collection("voice_rooms").doc("room_1").collection("seats").where("roomId", "==", "room_1").get()
  );
  await assertSucceeds(
    bobDb.collection("voice_rooms").doc("room_1").collection("messages").doc("msg_1").set({
      roomId: "room_1",
      senderId: BOB_UID,
      senderName: "Bob",
      senderVip: "NONE",
      msgType: "CHAT",
      text: "Namaste sabko!",
      createdAt: pastDate(1000),
    })
  );

  // Bob cannot spoof Alice's senderId in room chat or change room announcement
  await assertFails(
    bobDb.collection("voice_rooms").doc("room_1").collection("messages").doc("msg_spoof").set({
      roomId: "room_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      senderVip: "NONE",
      msgType: "CHAT",
      text: "Spoofed!",
      createdAt: pastDate(1000),
    })
  );
  await assertFails(
    bobDb.collection("voice_rooms").doc("room_1").update({
      announcement: "Hijacked!",
      updatedAt: pastDate(1000),
    })
  );

  // Owner updates announcement & theme
  await assertSucceeds(
    aliceDb.collection("voice_rooms").doc("room_1").update({
      announcement: "PK Battle Starting!",
      themeId: "CYBER_NEON",
      updatedAt: pastDate(1000),
    })
  );

  // PK Battle start + gift score + finish
  await assertSucceeds(
    aliceDb.collection("pk_battles").doc("pk_1").set({
      roomId: "room_1",
      redTeamName: "Red Lions",
      blueTeamName: "Blue Royals",
      redCaptainId: ALICE_UID,
      blueCaptainId: ALICE_UID,
      redScore: 0,
      blueScore: 0,
      status: "ACTIVE",
      winnerTeam: "NONE",
      durationSec: 300,
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("pk_battles").doc("pk_1").update({
      redScore: 150,
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("pk_battles").where("status", "in", ["ACTIVE", "FINISHED"]).get()
  );
});

test("ADDA Groups, Families, Agencies, Friendships, Private Chats & Reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const now = pastDate(5000);

  // ADDA Group create + moderation
  await assertSucceeds(
    aliceDb.collection("adda_groups").doc("adda_1").set({
      name: "Delhi Shayari ADDA",
      description: "Poetry and music",
      rules: "Be respectful",
      avatarKey: "ROYAL_LOTUS",
      ownerId: ALICE_UID,
      ownerName: "Alice",
      publicAdda: true,
      themeId: "ROYAL_STAGE",
      level: 1,
      xp: 0,
      memberCount: 1,
      seatCount: 8,
      coAdminUids: [],
      moderatorUids: [],
      mutedUids: [],
      bannedUids: [],
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("adda_groups").where("publicAdda", "==", true).get());
  await assertSucceeds(aliceDb.collection("adda_groups").where("ownerId", "==", ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("adda_groups").doc("adda_1").update({
      moderatorUids: [BOB_UID],
      updatedAt: pastDate(1000),
    })
  );
  // Bob cannot add co-admin before being promoted unless mod; once mod, Bob can mute
  await assertSucceeds(
    bobDb.collection("adda_groups").doc("adda_1").update({
      mutedUids: ["spammer_1"],
      updatedAt: pastDate(500),
    })
  );

  // Family create & join
  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").set({
      name: "Royal Rajputs",
      motto: "Unity & Honor",
      crestKey: "ROYAL_CREST",
      ownerId: ALICE_UID,
      ownerName: "Alice",
      adminUids: [],
      memberCount: 1,
      level: 1,
      xp: 0,
      totalGiftPoints: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("families").where("memberCount", ">=", 1).get());

  // Agency create & host apply
  await assertSucceeds(
    aliceDb.collection("agencies").doc("ag_1").set({
      name: "Starlight Talent India",
      description: "Top hosts",
      ownerId: ALICE_UID,
      ownerName: "Alice",
      managerUids: [],
      hostCount: 0,
      totalDiamonds: 0,
      commissionPercent: 15,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("agencies").where("hostCount", ">=", 0).get());

  // Friend request -> Accept -> Friendship + Private Chat
  await assertSucceeds(
    aliceDb.collection("friend_requests").doc("req_1").set({
      fromUid: ALICE_UID,
      fromName: "Alice",
      toUid: BOB_UID,
      toName: "Bob",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("friend_requests").where("toUid", "==", BOB_UID).get());
  const sortedIds = [ALICE_UID, BOB_UID].sort();
  const chatId = `${sortedIds[0]}_${sortedIds[1]}`;
  const acceptBatch = bobDb.batch();
  acceptBatch.update(bobDb.collection("friend_requests").doc("req_1"), {
    status: "ACCEPTED",
    updatedAt: pastDate(1000),
  });
  acceptBatch.set(bobDb.collection("friendships").doc(chatId), {
    userIds: sortedIds,
    userNames: ["Alice", "Bob"],
    createdAt: pastDate(1000),
  });
  await assertSucceeds(acceptBatch.commit());
  await assertSucceeds(bobDb.collection("friendships").where("userIds", "array-contains", BOB_UID).get());

  // Private chat create + subsequent update + messages
  const msgBatch1 = aliceDb.batch();
  msgBatch1.set(aliceDb.collection("chats").doc(chatId), {
    participantIds: sortedIds,
    participantNames: ["Alice", "Bob"],
    lastMessage: "Namaste Bob!",
    lastSenderId: ALICE_UID,
    updatedAt: pastDate(1000),
  });
  msgBatch1.set(aliceDb.collection("chats").doc(chatId).collection("messages").doc("pm_1"), {
    chatId,
    participantIds: sortedIds,
    senderId: ALICE_UID,
    senderName: "Alice",
    text: "Namaste Bob!",
    msgType: "TEXT",
    durationSec: 0,
    createdAt: pastDate(1000),
  });
  await assertSucceeds(msgBatch1.commit());

  const msgBatch2 = bobDb.batch();
  msgBatch2.update(bobDb.collection("chats").doc(chatId), {
    lastMessage: "Voice Note (8s)",
    lastSenderId: BOB_UID,
    updatedAt: pastDate(500),
  });
  msgBatch2.set(bobDb.collection("chats").doc(chatId).collection("messages").doc("pm_2"), {
    chatId,
    participantIds: sortedIds,
    senderId: BOB_UID,
    senderName: "Bob",
    text: "Voice Note (8s)",
    msgType: "VOICE",
    durationSec: 8,
    createdAt: pastDate(500),
  });
  await assertSucceeds(msgBatch2.commit());
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").where("participantIds", "array-contains", BOB_UID).get()
  );

  // Reports, Events & Announcements
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetId: BOB_UID,
      targetType: "USER",
      reason: "Spam link in voice room",
      status: "OPEN",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("reports").doc("rep_1").get());
  await assertSucceeds(adminDb.collection("reports").get());
  await assertSucceeds(
    adminDb.collection("events").doc("ev_diwali").set({
      title: "Diwali Mega Voice Fest",
      titleHi: "दिवाली मेगा वॉइस फेस्ट",
      description: "Send gifts & win Royal Crown!",
      eventType: "FESTIVAL",
      rewardCoins: 500,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("events").where("active", "==", true).get());
  await assertFails(
    aliceDb.collection("announcements").doc("ann_spoof").set({
      title: "Fake Admin",
      body: "Spoof",
      createdBy: "Fake",
      active: true,
      createdAt: now,
    })
  );
});
