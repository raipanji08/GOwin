import assert from "node:assert/strict";
import test from "node:test";
import worker from "../src/index.js";

const ENV = Object.freeze({
  FIREBASE_PROJECT_ID: "gowin-1f4e0",
  FIREBASE_WEB_API_KEY: "firebase-web-api-key",
  FIREBASE_ADMIN_EMAIL: "backend-admin@example.test",
  FIREBASE_ADMIN_PASSWORD: "backend-password",
  MIDTRANS_SERVER_KEY: "SB-Mid-server-test",
  MIDTRANS_ENVIRONMENT: "sandbox"
});

test("health endpoint does not expose configuration secrets", async () => {
  const response = await worker.fetch(
    new Request("https://gowin-midtrans.example/health"),
    ENV
  );
  assert.equal(response.status, 200);
  const body = await response.text();
  assert.equal(body.includes(ENV.MIDTRANS_SERVER_KEY), false);
  assert.equal(body.includes(ENV.FIREBASE_ADMIN_PASSWORD), false);
});

test("create endpoint rejects a request without Firebase login", async () => {
  const response = await worker.fetch(
    new Request("https://gowin-midtrans.example/create", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: "{}"
    }),
    ENV
  );
  assert.equal(response.status, 401);
  const body = await response.json();
  assert.equal(body.error.code, "unauthenticated");
});

test("create endpoint verifies user and returns safe QRIS instructions", async () => {
  const originalFetch = globalThis.fetch;
  const commitBodies = [];
  let midtransAuthorization = "";
  globalThis.fetch = async (input, init = {}) => {
    const url = String(input);
    if (url.includes("accounts:lookup")) {
      return Response.json({
        users: [
          {
            localId: "passenger-uid",
            email: "passenger@example.test",
            displayName: "Panji"
          }
        ]
      });
    }
    if (url.includes("accounts:signInWithPassword")) {
      return Response.json({
        idToken: "admin-id-token",
        refreshToken: "admin-refresh-token",
        expiresIn: "3600"
      });
    }
    if (
      url.includes("firestore.googleapis.com") &&
      url.includes("/user_profiles/passenger-uid")
    ) {
      return Response.json({
        name:
          "projects/gowin-1f4e0/databases/(default)/documents/" +
          "user_profiles/passenger-uid",
        fields: {
          name: { stringValue: "Panji Rai" }
        }
      });
    }
    if (
      url.includes("firestore.googleapis.com") &&
      url.includes("/schedules/schedule-one")
    ) {
      return Response.json({
        name:
          "projects/gowin-1f4e0/databases/(default)/documents/" +
          "schedules/schedule-one",
        fields: {
          from: { stringValue: "Bandung" },
          to: { stringValue: "Garut" },
          time: { stringValue: "09:00" },
          price: { integerValue: "75000" },
          vehicleName: { stringValue: "Hiace Premio" }
        }
      });
    }
    if (
      url.includes("firestore.googleapis.com") &&
      url.includes("/seat_reservations/schedule-one_B3")
    ) {
      return new Response("", { status: 404 });
    }
    if (
      url.includes("firestore.googleapis.com") &&
      url.endsWith("/documents:commit")
    ) {
      commitBodies.push(JSON.parse(String(init.body)));
      return Response.json({ writeResults: [] });
    }
    if (url === "https://api.sandbox.midtrans.com/v2/charge") {
      midtransAuthorization = String(init.headers.Authorization || "");
      return Response.json({
        transaction_id: "sandbox-transaction",
        transaction_status: "pending",
        qr_string: "000201010212",
        expiry_time: "2026-07-30 23:59:00",
        actions: [
          {
            name: "generate-qr-code",
            url: "https://api.sandbox.midtrans.com/qr"
          }
        ]
      });
    }
    throw new Error(`Unexpected fetch: ${url}`);
  };

  try {
    const response = await worker.fetch(
      new Request("https://gowin-midtrans.example/create", {
        method: "POST",
        headers: {
          Authorization: "Bearer passenger-id-token",
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          scheduleId: "schedule-one",
          seatNumber: "B3",
          travelDate: Date.now() + 86_400_000,
          paymentMethod: "midtrans_qris"
        })
      }),
      ENV
    );
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.equal(body.paymentMethod, "midtrans_qris");
    assert.equal(body.midtransQrString, "000201010212");
    assert.equal("serverKey" in body, false);
    assert.equal(JSON.stringify(body).includes(ENV.MIDTRANS_SERVER_KEY), false);
    assert.match(midtransAuthorization, /^Basic\s/);
    assert.equal(commitBodies.length, 2);
    assert.equal(commitBodies[0].writes.length, 2);
    assert.equal(JSON.stringify(commitBodies[0]).includes("Panji Rai"), true);
    assert.equal(commitBodies[1].writes.length, 1);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("status endpoint synchronizes the authenticated user's transaction", async () => {
  const originalFetch = globalThis.fetch;
  let statusRequestCount = 0;
  let commitCount = 0;
  globalThis.fetch = async (input) => {
    const url = String(input);
    if (url.includes("accounts:lookup")) {
      return Response.json({
        users: [{ localId: "passenger-uid", email: "passenger@example.test" }]
      });
    }
    if (url.includes("accounts:signInWithPassword")) {
      return Response.json({
        idToken: "admin-id-token",
        refreshToken: "admin-refresh-token",
        expiresIn: "3600"
      });
    }
    if (
      url.includes("firestore.googleapis.com") &&
      url.includes("/bookings/status-booking")
    ) {
      return Response.json({
        fields: {
          userId: { stringValue: "passenger-uid" },
          scheduleId: { stringValue: "schedule-one" },
          seatNumber: { stringValue: "B3" },
          totalAmount: { integerValue: "77500" },
          status: { stringValue: "pending_payment" },
          paymentStatus: { stringValue: "pending" },
          midtransTransactionStatus: { stringValue: "snap_pending" }
        }
      });
    }
    if (url.endsWith("/v2/status-booking/status")) {
      statusRequestCount += 1;
      return Response.json({
        transaction_id: "sandbox-transaction",
        transaction_status: "pending",
        gross_amount: "77500.00"
      });
    }
    if (
      url.includes("firestore.googleapis.com") &&
      url.endsWith("/documents:commit")
    ) {
      commitCount += 1;
      return Response.json({ writeResults: [] });
    }
    throw new Error(`Unexpected fetch: ${url}`);
  };

  try {
    const response = await worker.fetch(
      new Request("https://gowin-midtrans.example/status", {
        method: "POST",
        headers: {
          Authorization: "Bearer passenger-id-token",
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ bookingId: "status-booking" })
      }),
      ENV
    );
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), {
      status: "pending_payment",
      paymentStatus: "pending"
    });
    assert.equal(statusRequestCount, 1);
    assert.equal(commitCount, 1);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
