const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_govind";
const OWNER_UID = "owner_marco";
const CUSTOMER_UID = "customer_maya";
const OTHER_UID = "customer_liam";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowMinus(ms = 2000) {
  return new Date(Date.now() - ms);
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

test("Unauthenticated users are blocked from all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("restaurants").where("approved", "==", true).where("status", "==", "active").get());
  await assertFails(unauthDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(unauthDb.collection("bookings").where("customerId", "==", CUSTOMER_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(CUSTOMER_UID).set({
      uid: CUSTOMER_UID,
      name: "Guest",
      email: "guest@example.com",
      phone: "",
      photoUrl: "",
      role: "customer",
      status: "active",
      createdAt: nowMinus(),
      updatedAt: nowMinus(),
    })
  );
});

test("User profile isolation, self-escalation block, and email spoofing guard", async () => {
  const mayaDb = testEnv.authenticatedContext(CUSTOMER_UID, {
    email: "maya@example.com",
    email_verified: true,
  }).firestore();

  // Valid customer profile create
  await assertSucceeds(
    mayaDb.collection("users").doc(CUSTOMER_UID).set({
      uid: CUSTOMER_UID,
      name: "Maya Patel",
      email: "maya@example.com",
      phone: "+1 555-0101",
      photoUrl: "",
      role: "customer",
      status: "active",
      createdAt: nowMinus(),
      updatedAt: nowMinus(),
    })
  );

  // Reject self-escalation to admin on create
  await assertFails(
    mayaDb.collection("users").doc(CUSTOMER_UID + "_esc").set({
      uid: CUSTOMER_UID + "_esc",
      name: "Hacker",
      email: "maya@example.com",
      phone: "",
      photoUrl: "",
      role: "admin",
      status: "active",
      createdAt: nowMinus(),
      updatedAt: nowMinus(),
    })
  );

  // Reject customer self-updating role
  await assertFails(
    mayaDb.collection("users").doc(CUSTOMER_UID).update({
      role: "admin",
      updatedAt: nowMinus(500),
    })
  );

  // Reject listing all users (PII protection)
  await assertFails(mayaDb.collection("users").get());

  // Email spoofing check: admin email with email_verified=false must NOT be admin
  const spoofDb = testEnv.authenticatedContext("spoof_1", {
    email: "govindsinghpratap123@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("users").get());

  // Bootstrapped verified admin succeeds
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "govindsinghpratap123@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("users").doc(CUSTOMER_UID).update({
      role: "owner",
      updatedAt: nowMinus(500),
    })
  );
});

test("Restaurants: only approved & active visible to customers, unfiltered list blocked, owner restrictions enforced", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("restaurants").doc("rest_active").set({
      ownerId: OWNER_UID,
      name: "Osteria Brava",
      description: "Wood-fired pasta and hearth dishes",
      cuisines: ["Italian"],
      city: "San Francisco",
      address: "410 Valencia St",
      phone: "+1 415-555-0112",
      email: "hello@osteriabrava.com",
      priceRange: "$$$",
      dietaryOptions: ["veg", "gluten-free"],
      openingHours: "11:30 AM - 10:30 PM",
      holidayClosures: [],
      timeSlots: ["18:00", "19:30", "21:00"],
      logoUrl: "",
      coverPhotoUrl: "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4",
      galleryUrls: [],
      ratingAvg: 4.8,
      reviewCount: 42,
      status: "active",
      featured: true,
      approved: true,
      createdAt: nowMinus(10000),
      updatedAt: nowMinus(5000),
    });
    await db.collection("restaurants").doc("rest_suspended").set({
      ownerId: OWNER_UID,
      name: "Hidden Kitchen",
      description: "Suspended listing",
      cuisines: ["Grill"],
      city: "San Francisco",
      address: "99 Pine St",
      phone: "+1 415-555-0199",
      email: "hidden@example.com",
      priceRange: "$$",
      dietaryOptions: [],
      openingHours: "12:00 PM - 9:00 PM",
      holidayClosures: [],
      timeSlots: ["18:00"],
      logoUrl: "",
      coverPhotoUrl: "",
      galleryUrls: [],
      ratingAvg: 4.0,
      reviewCount: 1,
      status: "suspended",
      featured: false,
      approved: true,
      createdAt: nowMinus(10000),
      updatedAt: nowMinus(5000),
    });
  });

  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  // Aligned filtered query succeeds
  await assertSucceeds(
    customerDb.collection("restaurants")
      .where("approved", "==", true)
      .where("status", "==", "active")
      .get()
  );
  // Unfiltered query fails
  await assertFails(customerDb.collection("restaurants").get());
  // Direct get of suspended restaurant fails for customer
  await assertFails(customerDb.collection("restaurants").doc("rest_suspended").get());

  // Owner can query their own restaurants & update allowed profile fields
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertSucceeds(ownerDb.collection("restaurants").where("ownerId", "==", OWNER_UID).get());
  await assertSucceeds(
    ownerDb.collection("restaurants").doc("rest_active").update({
      openingHours: "11:00 AM - 11:00 PM",
      updatedAt: nowMinus(500),
    })
  );
  // Owner cannot self-approve or feature
  await assertFails(
    ownerDb.collection("restaurants").doc("rest_suspended").update({
      status: "active",
      featured: true,
      updatedAt: nowMinus(500),
    })
  );
});

test("Partner restaurant request workflow & admin approval", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "govindsinghpratap123@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    ownerDb.collection("restaurantRequests").doc("req_1").set({
      ownerId: OWNER_UID,
      ownerName: "Chef Marco",
      restaurantName: "Brava Trattoria",
      cuisines: ["Italian", "Pizza"],
      address: "221 Baker St",
      city: "London",
      phone: "+44 20 7946 0999",
      email: "marco@brava.co.uk",
      licenseNumber: "LIC-98231",
      description: "Artisan pasta and wood-fired sourdough",
      logoUrl: "",
      coverPhotoUrl: "https://images.unsplash.com/photo-1555396273-367ea4eb4db5",
      galleryUrls: [],
      status: "pending",
      adminNote: "",
      createdAt: nowMinus(5000),
      updatedAt: nowMinus(5000),
    })
  );

  // Owner cannot self-approve
  await assertFails(
    ownerDb.collection("restaurantRequests").doc("req_1").update({
      status: "approved",
      updatedAt: nowMinus(500),
    })
  );

  // Admin can approve
  await assertSucceeds(
    adminDb.collection("restaurantRequests").doc("req_1").update({
      status: "approved",
      adminNote: "Welcome to TableTrail!",
      updatedAt: nowMinus(500),
    })
  );
});

test("Bookings: creation, owner transitions, terminal state locking, and cross-customer isolation", async () => {
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();

  const created = nowMinus(5000);
  await assertSucceeds(
    customerDb.collection("bookings").doc("book_1").set({
      restaurantId: "rest_active",
      restaurantName: "Osteria Brava",
      restaurantImageUrl: "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4",
      ownerId: OWNER_UID,
      customerId: CUSTOMER_UID,
      customerName: "Maya Patel",
      customerPhone: "+1 555-0101",
      date: "2026-11-20",
      timeSlot: "19:30",
      bookingEpochMillis: Date.now() + 86400000,
      partySize: 4,
      tableLabel: "4-Top Window",
      specialRequests: "Corner table please",
      estimatedSpend: 160.0,
      status: "pending",
      reviewed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Other customer cannot read or modify Maya's booking
  await assertFails(otherDb.collection("bookings").doc("book_1").get());
  await assertFails(otherDb.collection("bookings").where("customerId", "==", CUSTOMER_UID).get());

  // Customer cannot self-confirm booking
  await assertFails(
    customerDb.collection("bookings").doc("book_1").update({
      status: "confirmed",
      updatedAt: nowMinus(1000),
    })
  );

  // Owner confirms and then completes booking
  await assertSucceeds(
    ownerDb.collection("bookings").doc("book_1").update({
      status: "confirmed",
      updatedAt: nowMinus(900),
    })
  );
  await assertSucceeds(
    ownerDb.collection("bookings").doc("book_1").update({
      status: "completed",
      updatedAt: nowMinus(800),
    })
  );

  // Terminal state lock: owner cannot move completed booking back to cancelled
  await assertFails(
    ownerDb.collection("bookings").doc("book_1").update({
      status: "cancelled",
      updatedAt: nowMinus(700),
    })
  );

  // Customer can mark reviewed=true on completed booking
  await assertSucceeds(
    customerDb.collection("bookings").doc("book_1").update({
      reviewed: true,
      updatedAt: nowMinus(600),
    })
  );
});

test("Reviews, Favorites, Notifications, and ActivityLogs isolation", async () => {
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "govindsinghpratap123@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    customerDb.collection("reviews").doc("rev_1").set({
      restaurantId: "rest_active",
      ownerId: OWNER_UID,
      bookingId: "book_1",
      customerId: CUSTOMER_UID,
      customerName: "Maya Patel",
      rating: 5,
      comment: "Incredible handmade pasta and warm service!",
      ownerReply: "",
      status: "visible",
      createdAt: nowMinus(3000),
      updatedAt: nowMinus(3000),
    })
  );

  // Owner can reply to review
  await assertSucceeds(
    ownerDb.collection("reviews").doc("rev_1").update({
      ownerReply: "Thank you Maya, see you soon!",
      updatedAt: nowMinus(1000),
    })
  );

  // Other user cannot overwrite review
  await assertFails(
    otherDb.collection("reviews").doc("rev_1").update({
      comment: "Hacked",
      updatedAt: nowMinus(500),
    })
  );

  // Favorites scoped to userId
  await assertSucceeds(
    customerDb.collection("favorites").doc(`${CUSTOMER_UID}_rest_active`).set({
      userId: CUSTOMER_UID,
      restaurantId: "rest_active",
      restaurantName: "Osteria Brava",
      restaurantCity: "San Francisco",
      restaurantCoverUrl: "",
      restaurantRating: 4.8,
      createdAt: nowMinus(1000),
    })
  );
  await assertFails(otherDb.collection("favorites").doc(`${CUSTOMER_UID}_rest_active`).delete());

  // ActivityLogs restricted to admin
  await assertFails(customerDb.collection("activityLogs").get());
  await assertSucceeds(
    adminDb.collection("activityLogs").doc("log_1").set({
      adminId: ADMIN_UID,
      adminEmail: "govindsinghpratap123@gmail.com",
      action: "APPROVE_REQUEST",
      targetType: "request",
      targetId: "req_1",
      details: "Approved Brava Trattoria",
      createdAt: nowMinus(500),
    })
  );
});
