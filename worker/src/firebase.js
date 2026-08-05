import { AppError } from "./errors.js";

let cachedAdminSession = null;
let adminSessionRequest = null;

function requireEnvironment(env, name) {
  const value = env[name];
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new AppError(
      503,
      "backend-not-configured",
      `Secret backend ${name} belum dikonfigurasi.`
    );
  }
  return value.trim();
}

async function firebaseAuthRequest(env, endpoint, payload) {
  const apiKey = requireEnvironment(env, "FIREBASE_WEB_API_KEY");
  let response;
  try {
    response = await fetch(
      `https://identitytoolkit.googleapis.com/v1/${endpoint}?key=${
        encodeURIComponent(apiKey)
      }`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      }
    );
  } catch {
    throw new AppError(
      503,
      "firebase-unavailable",
      "Firebase Authentication tidak dapat dihubungi."
    );
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const reason = String(data?.error?.message || "");
    if (
      reason.includes("INVALID_ID_TOKEN") ||
      reason.includes("TOKEN_EXPIRED") ||
      reason.includes("USER_DISABLED") ||
      reason.includes("USER_NOT_FOUND")
    ) {
      throw new AppError(
        401,
        "unauthenticated",
        "Sesi login telah berakhir. Silakan masuk kembali."
      );
    }
    throw new AppError(
      503,
      "firebase-auth-error",
      "Firebase Authentication belum dapat memproses permintaan."
    );
  }
  return data;
}

export async function authenticateFirebaseUser(request, env) {
  const authorization = request.headers.get("authorization") || "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);
  if (!match) {
    throw new AppError(
      401,
      "unauthenticated",
      "Silakan login terlebih dahulu."
    );
  }

  const data = await firebaseAuthRequest(env, "accounts:lookup", {
    idToken: match[1]
  });
  const user = Array.isArray(data.users) ? data.users[0] : null;
  if (!user || typeof user.localId !== "string" || user.localId.length === 0) {
    throw new AppError(
      401,
      "unauthenticated",
      "Sesi login tidak valid."
    );
  }
  return {
    uid: user.localId,
    email: typeof user.email === "string" ? user.email : "",
    name:
      typeof user.displayName === "string" && user.displayName.trim().length > 0
        ? user.displayName.trim()
        : String(user.email || "").split("@")[0].trim()
  };
}

async function signInAdmin(env) {
  const email = requireEnvironment(env, "FIREBASE_ADMIN_EMAIL");
  const password = requireEnvironment(env, "FIREBASE_ADMIN_PASSWORD");
  let data;
  try {
    data = await firebaseAuthRequest(
      env,
      "accounts:signInWithPassword",
      {
        email,
        password,
        returnSecureToken: true
      }
    );
  } catch {
    throw new AppError(
      503,
      "admin-auth-error",
      "Akun backend Firebase belum dikonfigurasi dengan benar."
    );
  }
  if (
    typeof data.idToken !== "string" ||
    typeof data.refreshToken !== "string"
  ) {
    throw new AppError(
      503,
      "admin-auth-error",
      "Akun backend Firebase tidak dapat digunakan."
    );
  }
  return {
    idToken: data.idToken,
    refreshToken: data.refreshToken,
    expiresAt:
      Date.now() + Math.max(300, Number(data.expiresIn) || 3600) * 1000
  };
}

async function refreshAdmin(env, refreshToken) {
  const apiKey = requireEnvironment(env, "FIREBASE_WEB_API_KEY");
  let response;
  try {
    response = await fetch(
      `https://securetoken.googleapis.com/v1/token?key=${
        encodeURIComponent(apiKey)
      }`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded"
        },
        body: new URLSearchParams({
          grant_type: "refresh_token",
          refresh_token: refreshToken
        })
      }
    );
  } catch {
    return signInAdmin(env);
  }
  const data = await response.json().catch(() => ({}));
  if (
    !response.ok ||
    typeof data.id_token !== "string" ||
    typeof data.refresh_token !== "string"
  ) {
    return signInAdmin(env);
  }
  return {
    idToken: data.id_token,
    refreshToken: data.refresh_token,
    expiresAt:
      Date.now() + Math.max(300, Number(data.expires_in) || 3600) * 1000
  };
}

async function getAdminToken(env, forceRefresh = false) {
  if (
    !forceRefresh &&
    cachedAdminSession &&
    cachedAdminSession.expiresAt > Date.now() + 60_000
  ) {
    return cachedAdminSession.idToken;
  }
  if (!adminSessionRequest) {
    adminSessionRequest = (
      cachedAdminSession?.refreshToken
        ? refreshAdmin(env, cachedAdminSession.refreshToken)
        : signInAdmin(env)
    )
      .then((session) => {
        cachedAdminSession = session;
        return session;
      })
      .finally(() => {
        adminSessionRequest = null;
      });
  }
  return (await adminSessionRequest).idToken;
}

function firestoreBase(env) {
  const projectId = requireEnvironment(env, "FIREBASE_PROJECT_ID");
  return `https://firestore.googleapis.com/v1/projects/${
    encodeURIComponent(projectId)
  }/databases/(default)/documents`;
}

export function documentName(env, collection, documentId) {
  const projectId = requireEnvironment(env, "FIREBASE_PROJECT_ID");
  return `projects/${projectId}/databases/(default)/documents/${
    collection
  }/${documentId}`;
}

async function firestoreRequest(
  env,
  path,
  { method = "GET", body = undefined } = {},
  retry = true
) {
  const token = await getAdminToken(env);
  let response;
  try {
    response = await fetch(`${firestoreBase(env)}${path}`, {
      method,
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "application/json",
        ...(body ? { "Content-Type": "application/json" } : {})
      },
      body: body ? JSON.stringify(body) : undefined
    });
  } catch {
    throw new AppError(
      503,
      "firestore-unavailable",
      "Database pembayaran tidak dapat dihubungi."
    );
  }

  if (response.status === 401 && retry) {
    cachedAdminSession = null;
    await getAdminToken(env, true);
    return firestoreRequest(env, path, { method, body }, false);
  }
  return response;
}

export async function getDocument(env, collection, documentId) {
  const response = await firestoreRequest(
    env,
    `/${encodeURIComponent(collection)}/${encodeURIComponent(documentId)}`
  );
  if (response.status === 404) return null;
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw firestoreError(response.status, data);
  }
  return data;
}

export async function getUserProfileName(env, userId) {
  const document = await getDocument(env, "user_profiles", userId);
  if (!document) return "";

  const name = decodeFields(document).name;
  return typeof name === "string" ? name.trim().slice(0, 80) : "";
}

export async function commitWrites(env, writes) {
  const response = await firestoreRequest(
    env,
    ":commit",
    {
      method: "POST",
      body: { writes }
    }
  );
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw firestoreError(response.status, data);
  }
  return data;
}

function firestoreError(status, data) {
  const firestoreStatus = String(data?.error?.status || "");
  if (status === 409 || firestoreStatus === "ALREADY_EXISTS") {
    return new AppError(
      409,
      "already-exists",
      "Kursi sudah dipesan oleh pengguna lain."
    );
  }
  if (status === 403 || firestoreStatus === "PERMISSION_DENIED") {
    return new AppError(
      503,
      "backend-permission-denied",
      "Izin akun backend Firebase belum dikonfigurasi."
    );
  }
  return new AppError(
    503,
    "firestore-error",
    "Database pembayaran belum dapat memproses permintaan."
  );
}

export const value = Object.freeze({
  string: (input) => ({ stringValue: String(input) }),
  integer: (input) => ({
    integerValue: String(Math.trunc(Number(input)))
  }),
  boolean: (input) => ({ booleanValue: Boolean(input) })
});

export function decodeValue(input) {
  if (!input || typeof input !== "object") return null;
  if ("stringValue" in input) return String(input.stringValue);
  if ("integerValue" in input) return Number(input.integerValue);
  if ("doubleValue" in input) return Number(input.doubleValue);
  if ("booleanValue" in input) return Boolean(input.booleanValue);
  if ("nullValue" in input) return null;
  return null;
}

export function decodeFields(document) {
  return Object.fromEntries(
    Object.entries(document?.fields || {}).map(([key, item]) => [
      key,
      decodeValue(item)
    ])
  );
}

export function createWrite(name, fields) {
  return {
    update: { name, fields },
    currentDocument: { exists: false }
  };
}

export function updateWrite(name, fields) {
  return {
    update: { name, fields },
    updateMask: { fieldPaths: Object.keys(fields) },
    currentDocument: { exists: true }
  };
}

export function deleteWrite(name) {
  return {
    delete: name,
    currentDocument: { exists: true }
  };
}
