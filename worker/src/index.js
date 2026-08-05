import {
  authenticateFirebaseUser,
  commitWrites,
  createWrite,
  decodeFields,
  deleteWrite,
  documentName,
  getDocument,
  getUserProfileName,
  updateWrite,
  value
} from "./firebase.js";
import { AppError, jsonResponse, readJson } from "./errors.js";
import {
  createCharge,
  createSnapTransaction,
  getTransactionStatus,
  normalizeMidtransStatus,
  PAYMENT_METHODS,
  paymentInstructions,
  verifyMidtransSignature
} from "./midtrans.js";

const ADMIN_FEE = 2500;

function requireString(input, fieldName, maxLength = 120) {
  if (typeof input !== "string" || input.trim().length === 0) {
    throw new AppError(400, "invalid-argument", `${fieldName} wajib diisi.`);
  }
  return input.trim().slice(0, maxLength);
}

function requireDocumentPart(input, fieldName, maxLength = 120) {
  const value = requireString(input, fieldName, maxLength);
  if (!/^[A-Za-z0-9_-]+$/.test(value)) {
    throw new AppError(
      400,
      "invalid-argument",
      `${fieldName} tidak valid.`
    );
  }
  return value;
}

function bookingId() {
  return crypto.randomUUID().replaceAll("-", "");
}

function dateReservationId(scheduleId, seatNumber, travelDate) {
  return `${scheduleId}_${travelDate}_${seatNumber}`;
}

function legacyReservationId(scheduleId, seatNumber) {
  return `${scheduleId}_${seatNumber}`;
}

function bookingFields(booking) {
  return {
    id: value.string(booking.id),
    bookingCode: value.string(booking.bookingCode),
    userId: value.string(booking.userId),
    userName: value.string(booking.userName),
    userEmail: value.string(booking.userEmail),
    scheduleId: value.string(booking.scheduleId),
    seatNumber: value.string(booking.seatNumber),
    routeFrom: value.string(booking.routeFrom),
    routeTo: value.string(booking.routeTo),
    departureTime: value.string(booking.departureTime),
    vehicleName: value.string(booking.vehicleName),
    travelDate: value.integer(booking.travelDate),
    ticketPrice: value.integer(booking.ticketPrice),
    adminFee: value.integer(booking.adminFee),
    totalAmount: value.integer(booking.totalAmount),
    paymentMethod: value.string(booking.paymentMethod),
    paymentStatus: value.string(booking.paymentStatus),
    status: value.string(booking.status),
    paymentReference: value.string(booking.paymentReference),
    midtransTransactionId: value.string(""),
    midtransTransactionStatus: value.string(""),
    midtransQrString: value.string(""),
    midtransQrUrl: value.string(""),
    midtransDeeplinkUrl: value.string(""),
    virtualAccountNumber: value.string(""),
    virtualAccountBank: value.string(""),
    paymentExpiresAt: value.integer(0),
    createdAt: value.integer(booking.createdAt),
    updatedAt: value.integer(booking.updatedAt)
  };
}

function reservationFields(booking) {
  return {
    scheduleId: value.string(booking.scheduleId),
    seatNumber: value.string(booking.seatNumber),
    bookingId: value.string(booking.id),
    userId: value.string(booking.userId),
    travelDate: value.integer(booking.travelDate),
    createdAt: value.integer(booking.createdAt)
  };
}

async function createBooking(env, user, payload) {
  const scheduleId = requireDocumentPart(payload.scheduleId, "Jadwal");
  const seatNumber = requireDocumentPart(payload.seatNumber, "Kursi", 8);
  const paymentMethod = requireString(
    payload.paymentMethod,
    "Metode pembayaran",
    40
  );
  const paymentConfiguration = PAYMENT_METHODS[paymentMethod];
  if (!paymentConfiguration) {
    throw new AppError(
      400,
      "invalid-argument",
      "Metode Midtrans tidak didukung."
    );
  }

  const travelDate = Number(payload.travelDate);
  const earliest = Date.now() - 24 * 60 * 60 * 1000;
  const latest = Date.now() + 366 * 24 * 60 * 60 * 1000;
  if (
    !Number.isSafeInteger(travelDate) ||
    travelDate < earliest ||
    travelDate > latest
  ) {
    throw new AppError(
      400,
      "invalid-argument",
      "Tanggal perjalanan tidak valid."
    );
  }

  const scheduleDocument = await getDocument(
    env,
    "schedules",
    scheduleId
  );
  if (!scheduleDocument) {
    throw new AppError(404, "not-found", "Jadwal tidak ditemukan.");
  }
  const schedule = decodeFields(scheduleDocument);
  const ticketPrice = Number(schedule.price);
  if (!Number.isSafeInteger(ticketPrice) || ticketPrice <= 0) {
    throw new AppError(
      422,
      "failed-precondition",
      "Harga jadwal tidak valid."
    );
  }

  const id = bookingId();
  const now = Date.now();
  const booking = {
    id,
    bookingCode: `GW${id.slice(0, 8).toUpperCase()}`,
    userId: user.uid,
    userName: user.name,
    userEmail: user.email,
    scheduleId,
    seatNumber,
    routeFrom: String(schedule.from || ""),
    routeTo: String(schedule.to || ""),
    departureTime: String(schedule.time || ""),
    vehicleName: String(schedule.vehicleName || "Hiace Premio"),
    travelDate,
    ticketPrice,
    adminFee: ADMIN_FEE,
    totalAmount: ticketPrice + ADMIN_FEE,
    paymentMethod,
    paymentStatus: "pending",
    status: "pending_payment",
    paymentReference: id,
    createdAt: now,
    updatedAt: now
  };

  const oldReservationId = legacyReservationId(scheduleId, seatNumber);
  const oldReservation = await getDocument(
    env,
    "seat_reservations",
    oldReservationId
  );
  if (oldReservation) {
    throw new AppError(
      409,
      "already-exists",
      "Kursi masih tercatat pada format reservasi lama. Buka aplikasi admin untuk melakukan migrasi otomatis."
    );
  }
  const reservationId = reservationIdForBooking(booking);
  await commitWrites(env, [
    createWrite(
      documentName(env, "bookings", id),
      bookingFields(booking)
    ),
    createWrite(
      documentName(env, "seat_reservations", reservationId),
      reservationFields(booking)
    )
  ]);
  return { booking, paymentConfiguration, reservationId };
}

async function updateInstructions(env, booking, instructions, status) {
  await commitWrites(env, [
    updateWrite(documentName(env, "bookings", booking.id), {
      midtransTransactionId: value.string(
        instructions.midtransTransactionId
      ),
      midtransTransactionStatus: value.string(status),
      midtransQrString: value.string(instructions.midtransQrString),
      midtransQrUrl: value.string(instructions.midtransQrUrl),
      midtransDeeplinkUrl: value.string(
        instructions.midtransDeeplinkUrl
      ),
      virtualAccountNumber: value.string(
        instructions.virtualAccountNumber
      ),
      virtualAccountBank: value.string(instructions.virtualAccountBank),
      paymentExpiresAt: value.integer(instructions.paymentExpiresAt),
      paymentReference: value.string(
        instructions.midtransTransactionId || booking.id
      ),
      updatedAt: value.integer(Date.now())
    })
  ]);
}

async function failBookingAndRelease(env, booking, reservationId) {
  try {
    const reservation = await getDocument(
      env,
      "seat_reservations",
      reservationId
    );
    const writes = [
      updateWrite(documentName(env, "bookings", booking.id), {
        status: value.string("rejected"),
        paymentStatus: value.string("failed"),
        updatedAt: value.integer(Date.now())
      })
    ];
    if (
      reservation &&
      decodeFields(reservation).bookingId === booking.id
    ) {
      writes.push(
        deleteWrite(
          documentName(env, "seat_reservations", reservationId)
        )
      );
    }
    await commitWrites(env, writes);
  } catch (error) {
    console.error("Failed to release booking after payment error", {
      bookingId: booking.id,
      error: error instanceof Error ? error.message : "unknown"
    });
  }
}

async function applyMidtransStatus(env, orderId, midtransStatus) {
  const bookingDocument = await getDocument(env, "bookings", orderId);
  if (!bookingDocument) {
    throw new AppError(404, "not-found", "Pemesanan tidak ditemukan.");
  }
  const booking = decodeFields(bookingDocument);
  if (
    Number(midtransStatus.gross_amount) !== Number(booking.totalAmount)
  ) {
    throw new AppError(
      400,
      "amount-mismatch",
      "Nominal pembayaran tidak sesuai."
    );
  }

  const normalized = normalizeMidtransStatus(
    String(midtransStatus.transaction_status || ""),
    String(midtransStatus.fraud_status || "")
  );
  const statusUpdate =
    booking.status === "paid" && normalized.status !== "paid"
      ? {
          status: "paid",
          paymentStatus: "paid",
          releaseSeat: false
        }
      : normalized;
  const writes = [
    updateWrite(documentName(env, "bookings", orderId), {
      status: value.string(statusUpdate.status),
      paymentStatus: value.string(statusUpdate.paymentStatus),
      paymentReference: value.string(
        String(midtransStatus.transaction_id || orderId)
      ),
      midtransTransactionStatus: value.string(
        String(midtransStatus.transaction_status || "")
      ),
      updatedAt: value.integer(Date.now())
    })
  ];

  if (statusUpdate.releaseSeat) {
    const reservationIds = [
      dateReservationId(
        booking.scheduleId,
        booking.seatNumber,
        Number(booking.travelDate)
      ),
      legacyReservationId(booking.scheduleId, booking.seatNumber)
    ];
    for (const candidateId of reservationIds) {
      const reservation = await getDocument(
        env,
        "seat_reservations",
        candidateId
      );
      if (
        reservation &&
        decodeFields(reservation).bookingId === orderId
      ) {
        writes.push(
          deleteWrite(
            documentName(env, "seat_reservations", candidateId)
          )
        );
      }
    }
  }
  await commitWrites(env, writes);
  return {
    status: statusUpdate.status,
    paymentStatus: statusUpdate.paymentStatus
  };
}

function reservationIdForBooking(booking) {
  return dateReservationId(
    booking.scheduleId,
    booking.seatNumber,
    booking.travelDate
  );
}

async function handleCreate(request, env) {
  const user = await authenticateFirebaseUser(request, env);
  const payload = await readJson(request);
  const profileName = await getUserProfileName(env, user.uid);
  const { booking, paymentConfiguration, reservationId } =
    await createBooking(
      env,
      {
        ...user,
        name: profileName || user.name
      },
      payload
    );
  let charge;
  try {
    const notificationUrl = new URL("/webhook", request.url).toString();
    charge = await createCharge(
      env,
      booking,
      paymentConfiguration,
      notificationUrl
    );
  } catch (error) {
    if (
      error instanceof AppError &&
      error.code === "midtrans-channel-unavailable"
    ) {
      try {
        const notificationUrl =
          new URL("/webhook", request.url).toString();
        const snap = await createSnapTransaction(
          env,
          booking,
          booking.paymentMethod,
          notificationUrl
        );
        const instructions = {
          midtransTransactionId: booking.id,
          midtransQrString: "",
          midtransQrUrl: "",
          midtransDeeplinkUrl: snap.redirectUrl,
          virtualAccountNumber: "",
          virtualAccountBank: "",
          paymentExpiresAt: Date.now() + 30 * 60 * 1000
        };
        await updateInstructions(
          env,
          booking,
          instructions,
          "snap_pending"
        );
        return jsonResponse({
          bookingId: booking.id,
          bookingCode: booking.bookingCode,
          paymentMethod: booking.paymentMethod,
          transactionStatus: "snap_pending",
          ...instructions
        });
      } catch (snapError) {
        await failBookingAndRelease(env, booking, reservationId);
        throw snapError;
      }
    }
    await failBookingAndRelease(env, booking, reservationId);
    throw error;
  }

  if (
    typeof charge.transaction_id !== "string" ||
    typeof charge.transaction_status !== "string"
  ) {
    console.error("Incomplete Midtrans charge response", {
      statusCode: String(charge.status_code || ""),
      statusMessage: String(charge.status_message || "").slice(0, 180),
      paymentType: String(charge.payment_type || ""),
      responseFields: Object.keys(charge).sort()
    });
    await failBookingAndRelease(env, booking, reservationId);
    throw new AppError(
      502,
      "incomplete-response",
      typeof charge.status_message === "string" &&
        charge.status_message.trim().length > 0
        ? charge.status_message.slice(0, 180)
        : "Respons pembayaran Midtrans tidak lengkap."
    );
  }
  const instructions = paymentInstructions(charge);
  await updateInstructions(
    env,
    booking,
    instructions,
    charge.transaction_status
  );

  return jsonResponse({
    bookingId: booking.id,
    bookingCode: booking.bookingCode,
    paymentMethod: booking.paymentMethod,
    transactionStatus: String(charge.transaction_status),
    ...instructions
  });
}

async function handleStatus(request, env) {
  const user = await authenticateFirebaseUser(request, env);
  const payload = await readJson(request);
  const id = requireDocumentPart(payload.bookingId, "ID pemesanan", 80);
  const bookingDocument = await getDocument(env, "bookings", id);
  if (!bookingDocument) {
    throw new AppError(404, "not-found", "Pemesanan tidak ditemukan.");
  }
  const booking = decodeFields(bookingDocument);
  if (booking.userId !== user.uid) {
    throw new AppError(
      403,
      "permission-denied",
      "Anda tidak dapat memeriksa transaksi ini."
    );
  }
  let status;
  try {
    status = await getTransactionStatus(env, id);
  } catch (error) {
    if (
      error instanceof AppError &&
      error.code === "midtrans-transaction-not-found" &&
      booking.midtransTransactionStatus === "snap_pending"
    ) {
      // A Snap token does not always create a Midtrans transaction until the
      // customer selects a channel.  Keep a fresh token pending, but do not
      // leave an expired token locking its seat forever.
      if (
        Number.isFinite(Number(booking.paymentExpiresAt)) &&
        Number(booking.paymentExpiresAt) > 0 &&
        Number(booking.paymentExpiresAt) <= Date.now()
      ) {
        return jsonResponse(
          await applyMidtransStatus(env, id, {
            transaction_status: "expire",
            gross_amount: booking.totalAmount
          })
        );
      }
      return jsonResponse({
        status: booking.status,
        paymentStatus: booking.paymentStatus
      });
    }
    throw error;
  }
  return jsonResponse(await applyMidtransStatus(env, id, status));
}

async function handleWebhook(request, env) {
  const notification = await readJson(request);
  if (!(await verifyMidtransSignature(env, notification))) {
    throw new AppError(401, "invalid-signature", "Signature tidak valid.");
  }
  const orderId = requireDocumentPart(
    notification.order_id,
    "ID pemesanan",
    80
  );
  await applyMidtransStatus(env, orderId, notification);
  return new Response("OK", {
    status: 200,
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff"
    }
  });
}

async function route(request, env) {
  const url = new URL(request.url);
  if (request.method === "OPTIONS") {
    return jsonResponse({});
  }
  if (request.method === "GET" && url.pathname === "/health") {
    return jsonResponse({
      service: "gowin-midtrans",
      environment: "sandbox",
      status: "ok"
    });
  }
  if (request.method === "GET" && url.pathname === "/finish") {
    return new Response(
      `<!doctype html><html lang="id"><head><meta charset="utf-8">` +
        `<meta name="viewport" content="width=device-width,initial-scale=1">` +
        `<title>GO-WIN</title></head><body style="font-family:sans-serif;` +
        `text-align:center;padding:48px 20px;color:#1f2937">` +
        `<h2>Pembayaran sedang diproses</h2>` +
        `<p>Kembali ke aplikasi GO-WIN untuk melihat status terbaru.</p>` +
        `</body></html>`,
      {
        status: 200,
        headers: {
          "Content-Type": "text/html; charset=utf-8",
          "Cache-Control": "no-store",
          "Content-Security-Policy":
            "default-src 'none'; style-src 'unsafe-inline'",
          "X-Content-Type-Options": "nosniff",
          "Referrer-Policy": "no-referrer"
        }
      }
    );
  }
  if (request.method !== "POST") {
    throw new AppError(405, "method-not-allowed", "Method tidak didukung.");
  }
  if (url.pathname === "/create") return handleCreate(request, env);
  if (url.pathname === "/status") return handleStatus(request, env);
  if (url.pathname === "/webhook") return handleWebhook(request, env);
  throw new AppError(404, "not-found", "Endpoint tidak ditemukan.");
}

export default {
  async fetch(request, env) {
    try {
      return await route(request, env);
    } catch (error) {
      if (error instanceof AppError) {
        return jsonResponse(
          { error: { code: error.code, message: error.message } },
          error.status
        );
      }
      console.error("Unhandled worker error", {
        error: error instanceof Error ? error.message : "unknown"
      });
      return jsonResponse(
        {
          error: {
            code: "internal",
            message: "Terjadi kesalahan pada backend pembayaran."
          }
        },
        500
      );
    }
  }
};
