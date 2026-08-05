import assert from "node:assert/strict";
import test from "node:test";
import {
  createCharge,
  createSnapTransaction,
  normalizeMidtransStatus,
  paymentInstructions,
  PAYMENT_METHODS,
  SNAP_ENABLED_PAYMENTS,
  userFacingMidtransMessage
} from "../src/midtrans.js";

const TEST_ENV = Object.freeze({
  MIDTRANS_SERVER_KEY: "sandbox-test-key"
});

const TEST_BOOKING = Object.freeze({
  id: "booking123",
  scheduleId: "schedule-one",
  totalAmount: 77_500,
  ticketPrice: 75_000,
  adminFee: 2_500,
  routeFrom: "Bandung",
  routeTo: "Garut",
  userName: "Panji",
  userEmail: "panji@example.test"
});

test("settlement issues a paid ticket", () => {
  assert.deepEqual(normalizeMidtransStatus("settlement", ""), {
    status: "paid",
    paymentStatus: "paid",
    releaseSeat: false
  });
});

test("expire releases the reserved seat", () => {
  assert.deepEqual(normalizeMidtransStatus("expire", ""), {
    status: "expired",
    paymentStatus: "expired",
    releaseSeat: true
  });
});

test("QRIS response is reduced to safe client instructions", () => {
  const instructions = paymentInstructions({
    transaction_id: "transaction-id",
    qr_string: "000201010212",
    expiry_time: "2026-07-30 18:30:00",
    actions: [
      {
        name: "generate-qr-code",
        url: "https://api.sandbox.midtrans.com/qr"
      }
    ]
  });
  assert.equal(instructions.midtransTransactionId, "transaction-id");
  assert.equal(instructions.midtransQrString, "000201010212");
  assert.equal(
    instructions.midtransQrUrl,
    "https://api.sandbox.midtrans.com/qr"
  );
  assert.equal("serverKey" in instructions, false);
});

test("virtual account response extracts bank and number", () => {
  const instructions = paymentInstructions({
    transaction_id: "transaction-id",
    va_numbers: [{ bank: "bca", va_number: "1234567890" }]
  });
  assert.equal(instructions.virtualAccountBank, "bca");
  assert.equal(instructions.virtualAccountNumber, "1234567890");
});

test("virtual account charge uses a sandbox-friendly BNI channel", () => {
  assert.deepEqual(PAYMENT_METHODS.midtrans_virtual_account, {
    payment_type: "bank_transfer",
    bank_transfer: { bank: "bni" }
  });
});

test("Snap fallback restricts checkout to the selected channel", async () => {
  const originalFetch = globalThis.fetch;
  let requestBody;
  globalThis.fetch = async (_url, init) => {
    requestBody = JSON.parse(String(init.body));
    return Response.json({
      token: "sandbox-snap-token",
      redirect_url:
        "https://app.sandbox.midtrans.com/snap/v4/redirection/token"
    });
  };
  try {
    const result = await createSnapTransaction(
      TEST_ENV,
      TEST_BOOKING,
      "midtrans_virtual_account",
      "https://worker.example/webhook"
    );
    assert.equal(
      result.redirectUrl,
      "https://app.sandbox.midtrans.com/snap/v4/redirection/token"
    );
    assert.deepEqual(
      requestBody.enabled_payments,
      SNAP_ENABLED_PAYMENTS.midtrans_virtual_account
    );
    assert.equal(requestBody.transaction_details.gross_amount, 77_500);
    assert.equal(
      requestBody.callbacks.finish,
      "https://worker.example/finish?bookingId=booking123"
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("Core API status 402 is eligible for Snap fallback", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () =>
    Response.json({
      status_code: "402",
      status_message: "Payment channel is not activated."
    });
  try {
    await assert.rejects(
      createCharge(
        TEST_ENV,
        TEST_BOOKING,
        PAYMENT_METHODS.midtrans_qris,
        "https://worker.example/webhook"
      ),
      (error) => error.code === "midtrans-channel-unavailable"
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("inactive payment channel is explained in Indonesian", () => {
  assert.equal(
    userFacingMidtransMessage("Payment channel is not activated."),
    "Metode pembayaran belum aktif pada Midtrans Sandbox."
  );
});
