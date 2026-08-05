# Setup Firebase dan Midtrans GO-WIN

Proyek ini menghasilkan dua aplikasi Android terpisah yang memakai satu proyek
Firebase `gowin-1f4e0`.

| Modul | Package Android | Fungsi |
| --- | --- | --- |
| `app` | `com.panjirai0110.gowin` | Aplikasi penumpang |
| `admin` | `com.panjirai0110.admin` | Panel operasional/admin |

## 1. Konfigurasi Android di Firebase

1. Daftarkan kedua package Android di Firebase Console.
2. Simpan file konfigurasi sesuai modul:
   - `app/google-services.json`
   - `admin/google-services.json`
3. Aktifkan provider **Email/Password** dan **Google** pada Firebase
   Authentication.
4. Tambahkan fingerprint debug aplikasi penumpang:
   - SHA-1:
     `AF:A2:EA:E6:CC:96:50:84:33:5C:98:F0:88:12:B6:76:F7:C5:D0:69`
   - SHA-256:
     `5B:1B:F7:1D:49:F3:16:85:75:4F:B9:7F:80:B9:4F:EB:4E:EC:5B:92:30:84:98:F1:5F:6C:6E:F7:B9:2A:34:21`
5. Unduh ulang `google-services.json` aplikasi penumpang setelah menambahkan
   SHA. Pastikan file tersebut mempunyai Web OAuth client agar Login Google
   menghasilkan resource `default_web_client_id`.

Firestore menggunakan database `(default)` di region `asia-southeast2`
(Jakarta).

## 2. Akses admin

Admin ditentukan oleh dokumen marker:

```text
admins/{uid}
```

Nama dokumen harus sama dengan Firebase Authentication UID akun admin. Dokumen
boleh kosong. Client tidak dapat membuat marker ini karena `firestore.rules`
menolak semua write ke koleksi `admins`.

## 3. Transfer manual

Transfer manual dapat diuji tanpa Midtrans. Aplikasi menampilkan rekening
sandbox/fiktif dan tidak boleh dipakai untuk mentransfer uang sungguhan.

Alurnya:

1. Penumpang memilih **Transfer Manual** dan mengonfirmasi transfer pengujian.
2. Booking berstatus `pending_verification` dan kursi langsung dikunci.
3. Admin membuka bagian **Verifikasi Transfer Manual**.
4. **Setujui** menerbitkan tiket digital.
5. **Tolak** mengubah status transaksi dan melepaskan kursi secara atomik.

Deploy security rules sebelum menguji alur ini:

```powershell
firebase deploy --only firestore:rules
```

## 4. Midtrans Sandbox

Server Key Midtrans tidak boleh disimpan di aplikasi Android, Git, `.env`, atau
file konfigurasi publik. Backend pembayaran memakai **Cloudflare Workers Free**
agar proyek Firebase tetap dapat menggunakan paket Spark.

1. Gunakan akun dan environment **Midtrans Sandbox**.
   Ambil Server Key dari **Sandbox > Settings > Access Keys**, bukan dari
   environment Production. Gunakan sumber halaman Access Keys sebagai acuan;
   jangan menentukan environment hanya dari pola awalan key.
2. Deploy Firestore Rules dari root proyek:

```powershell
firebase deploy --only firestore:rules
```

3. Buat akun Cloudflare Free, lalu masuk melalui Wrangler:

```powershell
npx wrangler login
```

4. Instal dependency Worker:

```powershell
Set-Location worker
npm install
npm run check
npm test
```

5. Simpan credential secara interaktif sebagai encrypted secrets. Jangan
   menuliskan nilainya di source code atau command:

```powershell
npx wrangler secret put MIDTRANS_SERVER_KEY
npx wrangler secret put FIREBASE_WEB_API_KEY
npx wrangler secret put FIREBASE_ADMIN_EMAIL
npx wrangler secret put FIREBASE_ADMIN_PASSWORD
```

`FIREBASE_WEB_API_KEY` dapat dilihat pada Firebase Project Settings atau
`app/google-services.json`. Gunakan akun Firebase Authentication yang UID-nya
sudah terdaftar pada dokumen `admins/{uid}` untuk dua secret akun admin.

6. Deploy Worker:

```powershell
npm run deploy
```

Wrangler akan menampilkan URL seperti:

```text
https://gowin-midtrans.<subdomain-akun>.workers.dev
```

7. URL Worker produksi sandbox sudah menjadi nilai default pada build aplikasi.
   Untuk menggantinya pada environment lokal, tambahkan override berikut ke
   `local.properties`:

```properties
MIDTRANS_BACKEND_URL=https://gowin-midtrans.<subdomain-akun>.workers.dev
```

8. Pastikan endpoint health dapat dibuka:

```text
https://gowin-midtrans.<subdomain-akun>.workers.dev/health
```

9. Di dashboard Midtrans Sandbox, atur Payment Notification URL ke:

```text
https://gowin-midtrans.<subdomain-akun>.workers.dev/webhook
```

Worker membaca harga dan rute langsung dari Firestore, mengunci kursi secara
atomik, lalu mencoba membuat transaksi melalui Midtrans Core API sandbox. Token
Firebase pengguna diverifikasi oleh Firebase Authentication sebelum transaksi
dibuat.

Sebagian akun Sandbox dapat mengaktifkan channel di **Snap Preferences**, tetapi
tetap menerima status `402` saat channel yang sama dipanggil melalui Core API.
Kasus tersebut merupakan perbedaan akses produk/channel di sisi Midtrans, bukan
kesalahan CORS atau format request aplikasi. Worker menanganinya otomatis:

1. Core API tetap menjadi jalur utama.
2. Jika Midtrans mengembalikan `402`, Worker membuat transaksi Snap Sandbox
   dengan metode yang dipilih pengguna saja.
3. Aplikasi membuka halaman pembayaran resmi
   `https://app.sandbox.midtrans.com/` di dalam WebView aman dengan header dan
   tombol cek status GO-WIN.
4. Webhook, polling status, penguncian kursi, dan penerbitan tiket tetap memakai
   alur backend yang sama.

Pemetaan fallback Snap saat ini:

- **QRIS** → `other_qris`
- **E-Wallet** → `gopay`
- **Virtual Account** → `bni_va`

Fallback ini tidak memerlukan upgrade Firebase Blaze maupun paket berbayar
Cloudflare. Semua API Midtrans tetap dipanggil dari Worker; Server Key tidak
pernah dikirim ke aplikasi Android.

Endpoint `/webhook` memverifikasi signature SHA-512 sebelum memperbarui status
pembayaran. Endpoint `/status` melakukan verifikasi tambahan melalui Get Status
API saat aplikasi melakukan polling atau pengguna menekan tombol **Cek Status
Pembayaran**. Status juga berubah secara real-time melalui listener Firestore.

Worker mengirim `X-Override-Notification` ke URL webhook miliknya sendiri.
Konfigurasi Payment Notification URL pada dashboard tetap disarankan sebagai
konfigurasi cadangan.

Semua transaksi pada environment ini adalah transaksi pengujian.
Merchant ID dan Client Key tidak perlu ditanam di aplikasi karena integrasi
tidak memakai JavaScript SDK atau tokenisasi kartu.

### Tampilan Snap di aplikasi

Halaman Snap tetap dikelola Midtrans agar proses pembayaran aman. Aplikasi
GO-WIN menyembunyikan hanya aksi Snap yang duplikat (`Check status`, `Cek
status`, dan `Download QRIS`) dan menyajikan tombol native **UNDUH QRIS** serta
**CEK STATUS PEMBAYARAN** di bagian bawah. Tombol unduh native mengambil gambar
QRIS yang sedang ditampilkan Snap dan menyimpannya melalui Download Manager
Android; alur pembayaran, QRIS, Virtual Account, dan instruksi Snap tidak
dimodifikasi.

Untuk menyamakan branding resmi halaman Snap, atur **Sandbox > Settings > Snap
Preferences > Theme and Logo** sebagai berikut:

- Font: `Poppins`
- Header color: `#FFFFFF`
- Button color: `#2563EB`
- Input dan button radius: `10px`
- Language: Indonesian

Pengaturan ini berlaku global untuk Snap merchant. Midtrans mendukung
personalisasi font, warna, dan roundness, tetapi tidak menyediakan opsi resmi
untuk menghapus tombol aksi internal secara selektif. Jangan menyembunyikan
aksi pembayaran seperti **Pay now** karena dapat mengganggu alur pembayaran.

### Uji end-to-end Sandbox

Gunakan simulator resmi Midtrans untuk menyelesaikan transaksi uji:

- Virtual Account:
  `https://simulator.sandbox.midtrans.com/bni/va/index`
- QRIS: buka `https://simulator.sandbox.midtrans.com/`, pilih
  **QRIS (openAPI & non openAPI)**, lalu masukkan URL gambar QR dari halaman
  Snap.

Hasil yang benar adalah status simulator berubah menjadi paid/success, lalu
aplikasi otomatis berpindah ke tiket digital melalui webhook atau polling.
Jangan membayar referensi Sandbox dengan uang sungguhan.

## 5. Verifikasi lokal

Build dua APK:

```powershell
.\gradlew.bat :app:assembleDebug :admin:assembleDebug
```

Jalankan unit test dan lint:

```powershell
.\gradlew.bat testDebugUnitTest :app:lintDebug :admin:lintDebug
npm --prefix worker run check
npm --prefix worker test
```

Validasi rules dengan Firestore Emulator:

```powershell
firebase emulators:exec --only firestore --project gowin-rules-test `
  "npm --prefix functions run test:rules"
```

Jika Java default masih versi lama, jalankan Firebase CLI dengan JDK 17 pada
`PATH`.

## Catatan migrasi

Booking baru memakai ID unik di `bookings/{bookingId}`. Penguncian kursi
disimpan terpisah di:

```text
seat_reservations/{scheduleId}_{seatNumber}
```

Jika database masih memiliki booking dari versi lama, buat dokumen reservation
yang sesuai sebelum rules baru digunakan agar kursi lama tetap dianggap terisi.
