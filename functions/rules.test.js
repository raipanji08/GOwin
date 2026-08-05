"use strict";

const fs = require("fs");
const path = require("path");
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} = require("@firebase/rules-unit-testing");
const {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  runTransaction,
  setDoc,
  updateDoc,
  where
} = require("firebase/firestore");
const { after, before, beforeEach, test } = require("node:test");

const PROJECT_ID = "gowin-rules-test";
const USER_ID = "user-one";
const OTHER_USER_ID = "user-two";
const ADMIN_ID = "admin-one";
const SCHEDULE_ID = "schedule-one";
let testEnvironment;

before(async () => {
  testEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(
        path.resolve(__dirname, "..", "firestore.rules"),
        "utf8"
      )
    }
  });
});

beforeEach(async () => {
  await testEnvironment.clearFirestore();
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "schedules", SCHEDULE_ID), {
      id: "",
      from: "Bandung",
      to: "Garut",
      time: "09:00",
      price: 75000,
      vehicleName: "Hiace Premio"
    });
    await setDoc(doc(context.firestore(), "admins", ADMIN_ID), {
      role: "admin"
    });
  });
});

after(async () => {
  await testEnvironment.cleanup();
});

function manualBooking(bookingId, seatNumber = "B3") {
  const now = Date.now();
  return {
    id: bookingId,
    bookingCode: `GW${bookingId.toUpperCase()}`,
    userId: USER_ID,
    userName: "Pengguna Uji",
    userEmail: "user@gowin.test",
    scheduleId: SCHEDULE_ID,
    seatNumber,
    routeFrom: "Bandung",
    routeTo: "Garut",
    departureTime: "09:00",
    vehicleName: "Hiace Premio",
    travelDate: now,
    ticketPrice: 75000,
    adminFee: 2500,
    totalAmount: 77500,
    paymentMethod: "manual_transfer",
    paymentStatus: "verification",
    status: "pending_verification",
    paymentReference: `MANUAL-${now}`,
    createdAt: now,
    updatedAt: now
  };
}

async function createManualBooking(
  database,
  bookingId = "booking-one",
  seatNumber = "B3",
  mutateBooking = (value) => value
) {
  const booking = mutateBooking(manualBooking(bookingId, seatNumber));
  const bookingRef = doc(database, "bookings", bookingId);
  const reservationRef = doc(
    database,
    "seat_reservations",
    `${SCHEDULE_ID}_${booking.travelDate}_${seatNumber}`
  );
  return runTransaction(database, async (transaction) => {
    transaction.set(reservationRef, {
      scheduleId: SCHEDULE_ID,
      seatNumber,
      bookingId,
      userId: USER_ID,
      travelDate: booking.travelDate,
      createdAt: booking.createdAt
    });
    transaction.set(bookingRef, booking);
  });
}

function onlineBooking(bookingId, seatNumber = "C1") {
  const now = Date.now();
  return {
    ...manualBooking(bookingId, seatNumber),
    paymentMethod: "midtrans_qris",
    paymentStatus: "pending",
    status: "pending_payment",
    paymentReference: bookingId,
    midtransTransactionId: "",
    midtransTransactionStatus: "",
    midtransQrString: "",
    midtransQrUrl: "",
    midtransDeeplinkUrl: "",
    virtualAccountNumber: "",
    virtualAccountBank: "",
    paymentExpiresAt: 0,
    createdAt: now,
    updatedAt: now
  };
}

async function createOnlineBooking(
  database,
  bookingId = "booking-online",
  seatNumber = "C1"
) {
  const booking = onlineBooking(bookingId, seatNumber);
  const bookingRef = doc(database, "bookings", bookingId);
  const reservationRef = doc(
    database,
    "seat_reservations",
    `${SCHEDULE_ID}_${booking.travelDate}_${seatNumber}`
  );
  return runTransaction(database, async (transaction) => {
    transaction.set(reservationRef, {
      scheduleId: SCHEDULE_ID,
      seatNumber,
      bookingId,
      userId: USER_ID,
      travelDate: booking.travelDate,
      createdAt: booking.createdAt
    });
    transaction.set(bookingRef, booking);
  });
}

test("owner can atomically create a valid manual-transfer booking", async () => {
  const database = testEnvironment
    .authenticatedContext(USER_ID, { email: "user@gowin.test" })
    .firestore();

  await assertSucceeds(createManualBooking(database));
});

test("the same seat can be reserved on different travel dates only", async () => {
  const database = testEnvironment
    .authenticatedContext(USER_ID, { email: "user@gowin.test" })
    .firestore();
  const firstDate = Date.now();
  const secondDate = firstDate + 86_400_000;

  await assertSucceeds(
    createManualBooking(database, "booking-day-one", "B3", (booking) => ({
      ...booking,
      travelDate: firstDate
    }))
  );
  await assertFails(
    createManualBooking(database, "booking-day-one-copy", "B3", (booking) => ({
      ...booking,
      travelDate: firstDate
    }))
  );
  await assertSucceeds(
    createManualBooking(database, "booking-day-two", "B3", (booking) => ({
      ...booking,
      travelDate: secondDate
    }))
  );
});

test("client cannot forge schedule price or paid status", async () => {
  const database = testEnvironment
    .authenticatedContext(USER_ID, { email: "user@gowin.test" })
    .firestore();

  await assertFails(
    createManualBooking(
      database,
      "forged-price",
      "A1",
      (booking) => ({
        ...booking,
        ticketPrice: 1000,
        totalAmount: 3500
      })
    )
  );
  await assertFails(
    createManualBooking(
      database,
      "forged-status",
      "A2",
      (booking) => ({
        ...booking,
        paymentStatus: "paid",
        status: "paid"
      })
    )
  );
});

test("owner query must be scoped to their own userId", async () => {
  const database = testEnvironment
    .authenticatedContext(USER_ID, { email: "user@gowin.test" })
    .firestore();
  await assertSucceeds(createManualBooking(database));

  await assertSucceeds(
    getDocs(
      query(
        collection(database, "bookings"),
        where("userId", "==", USER_ID)
      )
    )
  );
  await assertFails(getDocs(collection(database, "bookings")));
});

test("admin can approve or reject manual transfer but user cannot", async () => {
  const userDatabase = testEnvironment
    .authenticatedContext(USER_ID, { email: "user@gowin.test" })
    .firestore();
  const adminDatabase = testEnvironment
    .authenticatedContext(ADMIN_ID, { email: "admin@gowin.test" })
    .firestore();
  await assertSucceeds(createManualBooking(userDatabase));

  await assertFails(
    updateDoc(doc(userDatabase, "bookings", "booking-one"), {
      status: "paid",
      paymentStatus: "paid",
      paymentReference: "forged",
      updatedAt: Date.now()
    })
  );

  await assertSucceeds(
    updateDoc(doc(adminDatabase, "bookings", "booking-one"), {
      status: "paid",
      paymentStatus: "paid",
      paymentReference: "verified-by-admin",
      updatedAt: Date.now()
    })
  );

  const storedBooking = (
    await getDoc(doc(adminDatabase, "bookings", "booking-one"))
  ).data();
  await assertSucceeds(
    runTransaction(adminDatabase, async (transaction) => {
      const bookingRef = doc(adminDatabase, "bookings", "booking-one");
      const reservationRef = doc(
        adminDatabase,
        "seat_reservations",
        `${SCHEDULE_ID}_${storedBooking.travelDate}_B3`
      );
      transaction.update(bookingRef, {
        status: "rejected",
        paymentStatus: "failed",
        paymentReference: "rejected-by-admin",
        updatedAt: Date.now()
      });
      transaction.delete(reservationRef);
    })
  );
});

test("only admin backend can create and enrich an online booking", async () => {
  const userDatabase = testEnvironment
    .authenticatedContext(USER_ID, { email: "user@gowin.test" })
    .firestore();
  const adminDatabase = testEnvironment
    .authenticatedContext(ADMIN_ID, { email: "admin@gowin.test" })
    .firestore();

  await assertFails(
    createOnlineBooking(userDatabase, "forged-online", "C2")
  );
  await assertSucceeds(createOnlineBooking(adminDatabase));

  await assertFails(
    updateDoc(doc(userDatabase, "bookings", "booking-online"), {
      midtransTransactionId: "forged",
      midtransTransactionStatus: "settlement",
      updatedAt: Date.now()
    })
  );
  await assertSucceeds(
    updateDoc(doc(adminDatabase, "bookings", "booking-online"), {
      midtransTransactionId: "sandbox-transaction",
      midtransTransactionStatus: "pending",
      midtransQrString: "000201010212",
      paymentExpiresAt: Date.now() + 1_800_000,
      paymentReference: "sandbox-transaction",
      updatedAt: Date.now()
    })
  );
});

test("profile is visible only to its owner and an authorized admin", async () => {
  const ownerDatabase = testEnvironment
    .authenticatedContext(USER_ID, { email: "user@gowin.test" })
    .firestore();
  const otherDatabase = testEnvironment
    .authenticatedContext(OTHER_USER_ID, { email: "other@gowin.test" })
    .firestore();
  const adminDatabase = testEnvironment
    .authenticatedContext(ADMIN_ID, { email: "admin@gowin.test" })
    .firestore();
  const profileRef = doc(ownerDatabase, "user_profiles", USER_ID);

  await assertSucceeds(
    setDoc(profileRef, {
      userId: USER_ID,
      name: "Pengguna Uji",
      email: "user@gowin.test",
      phone: "081234567890",
      gender: "male",
      onboardingStatus: "completed",
      updatedAt: Date.now()
    })
  );
  await assertSucceeds(getDoc(doc(adminDatabase, "user_profiles", USER_ID)));
  await assertFails(getDoc(doc(otherDatabase, "user_profiles", USER_ID)));
  await assertFails(deleteDoc(doc(otherDatabase, "user_profiles", USER_ID)));
});

test("new users can skip optional profile completion safely", async () => {
  const ownerDatabase = testEnvironment
    .authenticatedContext(USER_ID, { email: "user@gowin.test" })
    .firestore();

  await assertSucceeds(
    setDoc(doc(ownerDatabase, "user_profiles", USER_ID), {
      userId: USER_ID,
      name: "",
      email: "user@gowin.test",
      phone: "",
      gender: "",
      onboardingStatus: "skipped",
      updatedAt: Date.now()
    })
  );
});

test("admin cannot save malformed schedule data", async () => {
  const adminDatabase = testEnvironment
    .authenticatedContext(ADMIN_ID, { email: "admin@gowin.test" })
    .firestore();

  await assertFails(
    setDoc(doc(adminDatabase, "schedules", "invalid-time"), {
      id: "",
      from: "Bandung",
      to: "Garut",
      time: "29.00",
      price: 75000,
      vehicleName: "Hiace Premio"
    })
  );
});

test("profile rejects malformed phone number", async () => {
  const ownerDatabase = testEnvironment
    .authenticatedContext(USER_ID, { email: "user@gowin.test" })
    .firestore();

  await assertFails(
    setDoc(doc(ownerDatabase, "user_profiles", USER_ID), {
      userId: USER_ID,
      name: "Pengguna Uji",
      email: "user@gowin.test",
      phone: "nomor-invalid",
      gender: "male",
      onboardingStatus: "completed",
      updatedAt: Date.now()
    })
  );
});
