import { AppError } from "./errors.js";

const MIDTRANS_SANDBOX_BASE = "https://api.sandbox.midtrans.com/v2";
const MIDTRANS_SNAP_SANDBOX_URL =
  "https://app.sandbox.midtrans.com/snap/v1/transactions";

export const PAYMENT_METHODS = Object.freeze({
  midtrans_qris: {
    payment_type: "qris",
    qris: { acquirer: "gopay" }
  },
  midtrans_ewallet: {
    payment_type: "gopay",
    gopay: { enable_callback: false }
  },
  midtrans_virtual_account: {
    payment_type: "bank_transfer",
    bank_transfer: { bank: "bni" }
  }
});

export const SNAP_ENABLED_PAYMENTS = Object.freeze({
  midtrans_qris: ["other_qris"],
  midtrans_ewallet: ["gopay"],
  midtrans_virtual_account: ["bni_va"]
});

function serverKey(env) {
  if (
    typeof env.MIDTRANS_SERVER_KEY !== "string" ||
    env.MIDTRANS_SERVER_KEY.trim().length === 0
  ) {
    throw new AppError(
      503,
      "backend-not-configured",
      "Secret Midtrans belum dikonfigurasi."
    );
  }
  return env.MIDTRANS_SERVER_KEY.trim();
}

function authorization(env) {
  return `Basic ${btoa(`${serverKey(env)}:`)}`;
}

export function userFacingMidtransMessage(message) {
  const normalized = String(message || "").trim();
  if (normalized.toLowerCase().includes("payment channel is not activated")) {
    return "Metode pembayaran belum aktif pada Midtrans Sandbox.";
  }
  return normalized || "Midtrans belum dapat memproses pembayaran.";
}

async function midtransRequest(env, url, init) {
  let response;
  try {
    response = await fetch(url, {
      ...init,
      headers: {
        Authorization: authorization(env),
        Accept: "application/json",
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...(init.headers || {})
      }
    });
  } catch {
    throw new AppError(
      503,
      "midtrans-unavailable",
      "Layanan pembayaran Midtrans tidak dapat dihubungi."
    );
  }
  const data = await response.json().catch(() => ({}));
  const midtransStatusCode = Number(data.status_code);
  if (
    !response.ok ||
    (Number.isFinite(midtransStatusCode) && midtransStatusCode >= 400)
  ) {
    const statusMessage =
      typeof data.status_message === "string"
        ? userFacingMidtransMessage(data.status_message.slice(0, 180))
        : "Midtrans belum dapat memproses pembayaran.";
    throw new AppError(
      response.status >= 500 ? 503 : 422,
      midtransStatusCode === 402
        ? "midtrans-channel-unavailable"
        : midtransStatusCode === 404
          ? "midtrans-transaction-not-found"
          : "midtrans-error",
      statusMessage
    );
  }
  return data;
}

async function midtransFetch(env, path, init) {
  return midtransRequest(
    env,
    `${MIDTRANS_SANDBOX_BASE}${path}`,
    init
  );
}

export function normalizeMidtransStatus(transactionStatus, fraudStatus) {
  if (
    transactionStatus === "settlement" ||
    (transactionStatus === "capture" && fraudStatus === "accept")
  ) {
    return { status: "paid", paymentStatus: "paid", releaseSeat: false };
  }
  if (transactionStatus === "pending" || transactionStatus === "authorize") {
    return {
      status: "pending_payment",
      paymentStatus: "pending",
      releaseSeat: false
    };
  }
  if (transactionStatus === "expire") {
    return { status: "expired", paymentStatus: "expired", releaseSeat: true };
  }
  if (transactionStatus === "cancel") {
    return { status: "cancelled", paymentStatus: "failed", releaseSeat: true };
  }
  if (transactionStatus === "deny" || transactionStatus === "failure") {
    return { status: "rejected", paymentStatus: "failed", releaseSeat: true };
  }
  if (
    transactionStatus === "refund" ||
    transactionStatus === "chargeback"
  ) {
    return {
      status: "cancelled",
      paymentStatus: "refunded",
      releaseSeat: true
    };
  }
  if (
    transactionStatus === "partial_refund" ||
    transactionStatus === "partial_chargeback"
  ) {
    return {
      status: "paid",
      paymentStatus: "refunded",
      releaseSeat: false
    };
  }
  return {
    status: "pending_payment",
    paymentStatus: "pending",
    releaseSeat: false
  };
}

function actionUrl(midtransResponse, actionName) {
  if (!Array.isArray(midtransResponse.actions)) return "";
  const action = midtransResponse.actions.find(
    (candidate) =>
      candidate &&
      candidate.name === actionName &&
      typeof candidate.url === "string"
  );
  return action?.url || "";
}

function parseExpiry(value) {
  if (typeof value !== "string" || value.trim().length === 0) {
    return Date.now() + 30 * 60 * 1000;
  }
  const normalized = value.includes("T")
    ? value
    : `${value.trim().replace(" ", "T")}+07:00`;
  const timestamp = Date.parse(normalized);
  return Number.isFinite(timestamp)
    ? timestamp
    : Date.now() + 30 * 60 * 1000;
}

export function paymentInstructions(midtransResponse) {
  const virtualAccount = Array.isArray(midtransResponse.va_numbers)
    ? midtransResponse.va_numbers.find(
        (candidate) =>
          candidate && typeof candidate.va_number === "string"
      )
    : null;
  return {
    midtransTransactionId:
      typeof midtransResponse.transaction_id === "string"
        ? midtransResponse.transaction_id
        : "",
    midtransQrString:
      typeof midtransResponse.qr_string === "string"
        ? midtransResponse.qr_string
        : "",
    midtransQrUrl:
      actionUrl(midtransResponse, "generate-qr-code") ||
      actionUrl(midtransResponse, "generate-qr-code-v2"),
    midtransDeeplinkUrl: actionUrl(
      midtransResponse,
      "deeplink-redirect"
    ),
    virtualAccountNumber:
      virtualAccount?.va_number ||
      (
        typeof midtransResponse.permata_va_number === "string"
          ? midtransResponse.permata_va_number
          : ""
      ),
    virtualAccountBank:
      virtualAccount?.bank ||
      (
        typeof midtransResponse.payment_type === "string"
          ? midtransResponse.payment_type
          : ""
      ),
    paymentExpiresAt: parseExpiry(midtransResponse.expiry_time)
  };
}

export async function createCharge(
  env,
  booking,
  paymentConfiguration,
  notificationUrl
) {
  return midtransFetch(env, "/charge", {
    method: "POST",
    headers: {
      "X-Override-Notification": notificationUrl
    },
    body: JSON.stringify({
      ...paymentConfiguration,
      transaction_details: {
        order_id: booking.id,
        gross_amount: booking.totalAmount
      },
      item_details: [
        {
          id: booking.scheduleId.slice(0, 50),
          price: booking.ticketPrice,
          quantity: 1,
          name: `${booking.routeFrom} - ${booking.routeTo}`.slice(0, 50)
        },
        {
          id: "ADMIN_FEE",
          price: booking.adminFee,
          quantity: 1,
          name: "Biaya admin GO-WIN"
        }
      ],
      customer_details: {
        first_name: booking.userName.slice(0, 50),
        email: booking.userEmail
      },
      custom_expiry: {
        expiry_duration: 30,
        unit: "minute"
      }
    })
  });
}

export async function createSnapTransaction(
  env,
  booking,
  paymentMethod,
  notificationUrl
) {
  const enabledPayments = SNAP_ENABLED_PAYMENTS[paymentMethod];
  if (!enabledPayments) {
    throw new AppError(
      400,
      "invalid-argument",
      "Metode pembayaran Snap tidak didukung."
    );
  }
  const finishUrl = new URL(
    `/finish?bookingId=${encodeURIComponent(booking.id)}`,
    notificationUrl
  ).toString();
  const response = await midtransRequest(env, MIDTRANS_SNAP_SANDBOX_URL, {
    method: "POST",
    headers: {
      "X-Override-Notification": notificationUrl
    },
    body: JSON.stringify({
      transaction_details: {
        order_id: booking.id,
        gross_amount: booking.totalAmount
      },
      item_details: [
        {
          id: booking.scheduleId.slice(0, 50),
          price: booking.ticketPrice,
          quantity: 1,
          name: `${booking.routeFrom} - ${booking.routeTo}`.slice(0, 50)
        },
        {
          id: "ADMIN_FEE",
          price: booking.adminFee,
          quantity: 1,
          name: "Biaya admin GO-WIN"
        }
      ],
      customer_details: {
        first_name: booking.userName.slice(0, 50),
        email: booking.userEmail
      },
      enabled_payments: enabledPayments,
      callbacks: {
        finish: finishUrl,
        error: finishUrl
      },
      expiry: {
        duration: 30,
        unit: "minute"
      }
    })
  });
  if (
    typeof response.token !== "string" ||
    typeof response.redirect_url !== "string" ||
    !response.redirect_url.startsWith(
      "https://app.sandbox.midtrans.com/"
    )
  ) {
    throw new AppError(
      502,
      "incomplete-response",
      "Respons checkout Midtrans Snap tidak lengkap."
    );
  }
  return {
    token: response.token,
    redirectUrl: response.redirect_url
  };
}

export async function getTransactionStatus(env, orderId) {
  return midtransFetch(
    env,
    `/${encodeURIComponent(orderId)}/status`,
    { method: "GET" }
  );
}

export async function verifyMidtransSignature(env, notification) {
  const orderId = String(notification.order_id || "");
  const statusCode = String(notification.status_code || "");
  const grossAmount = String(notification.gross_amount || "");
  const signature = String(notification.signature_key || "").toLowerCase();
  if (!orderId || !/^[a-f0-9]{128}$/.test(signature)) return false;

  const source = new TextEncoder().encode(
    `${orderId}${statusCode}${grossAmount}${serverKey(env)}`
  );
  const digest = await crypto.subtle.digest("SHA-512", source);
  const expected = Array.from(new Uint8Array(digest))
    .map((item) => item.toString(16).padStart(2, "0"))
    .join("");
  if (signature.length !== expected.length) return false;
  let difference = 0;
  for (let index = 0; index < signature.length; index += 1) {
    difference |= signature.charCodeAt(index) ^ expected.charCodeAt(index);
  }
  return difference === 0;
}
