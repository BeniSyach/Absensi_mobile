# 📱 Absensi App - Android (Kotlin + Jetpack Compose)

Aplikasi mobile untuk absensi pegawai yang terintegrasi dengan backend Spring Boot.

## Fitur

- Login JWT — token disimpan aman via Jetpack DataStore
- Registrasi mandiri — pegawai baru bisa daftar (menunggu approval admin)
- Absen Masuk & Pulang — wajib foto selfie + lokasi GPS
- Deteksi Mock Location — cek `Location.isFromMockProvider()` / `isMock` (API 31+) dan flag global Developer Options
- CameraX — ambil foto selfie langsung dari aplikasi (kamera depan default)
- Kompresi foto otomatis — resize ke maks 1024px, kualitas 80% sebelum upload
- Dashboard — status absen hari ini, info shift, profil ringkas
- Riwayat absensi — list absen masuk & pulang per periode
- Profil & Ganti Password — edit data diri, upload foto profil

## Arsitektur

```
MVVM + Repository Pattern + Hilt DI

ui/
├── screen/
│   ├── login/      -> LoginScreen, RegistrasiScreen, AuthViewModel
│   ├── home/       -> HomeScreen (dashboard), HomeViewModel
│   ├── absen/      -> AbsenScreen, AbsenViewModel
│   ├── profil/     -> ProfilScreen, GantiPasswordScreen, ProfilViewModel
│   └── riwayat/    -> RiwayatScreen, RiwayatViewModel
├── component/      -> CameraCaptureScreen (CameraX)
├── navigation/     -> NavRoutes, AbsensiNavGraph
└── theme/          -> Color, Theme, Type (Material3)

data/
├── api/            -> AbsensiApi (Retrofit), AuthInterceptor
├── local/          -> TokenManager (DataStore)
├── model/          -> Request & Response DTO (kotlinx.serialization)
└── repository/     -> AuthRepository, AbsensiRepository, UserRepository

util/
└── LocationHelper  -> FusedLocationProvider + deteksi mock location

di/
├── NetworkModule   -> Retrofit, OkHttp, Interceptor
└── AppModule       -> TokenManager, LocationHelper
```

## Setup

### 1. Konfigurasi Base URL API

Edit `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "BASE_URL_DEBUG", "\"http://10.0.2.2:8080/\"")    // emulator -> localhost backend
buildConfigField("String", "BASE_URL_RELEASE", "\"https://api.absensi.go.id/\"")
```

`10.0.2.2` adalah alias localhost host machine dari Android Emulator.
Untuk device fisik, gunakan IP LAN backend, contoh `http://192.168.1.10:8080/`.

### 2. Build & Run

```bash
./gradlew assembleDebug
```

Atau langsung Run di Android Studio (pilih device/emulator).

## Alur Deteksi Mock Location

1. Saat user menekan "Absen Masuk/Pulang", `LocationHelper.getCurrentLocation()` mengambil GPS dengan `PRIORITY_HIGH_ACCURACY`
2. Cek `location.isMock` (API 31+) atau `location.isFromMockProvider` (API < 31)
3. Cek juga provider lain di sistem yang mengindikasikan fake GPS aktif (`isMockLocationEnabledGlobally()`)
4. Semua info ini (`isMockLocation`, `locationProvider`, `akurasiGps`) dikirim ke backend
5. Backend melakukan validasi final (radius kantor, kecepatan perpindahan, dsb) — Android hanya memberi sinyal awal, keputusan akhir selalu di server

## Alur Absen

```
User tekan "Absen Masuk"
  -> Minta izin Lokasi + Kamera (jika belum)
  -> Ambil lokasi GPS otomatis
  -> User foto selfie (CameraX, kamera depan)
  -> Foto dikompres (maks 1024px, 80% JPEG)
  -> POST multipart ke /api/v1/absensi/masuk
      - part "foto" = file gambar
      - part "data" = JSON {lokasi: {...}}
  -> Tampilkan hasil (status, jarak dari kantor, peringatan jika mock terdeteksi)
```

## Dependency Utama

| Library | Fungsi |
|---|---|
| Jetpack Compose + Material3 | UI |
| Hilt | Dependency Injection |
| Retrofit + OkHttp + kotlinx.serialization | Networking |
| DataStore + Security Crypto | Simpan token JWT |
| Play Services Location (Fused) | GPS & deteksi mock location |
| CameraX | Ambil foto selfie |
| Coil | Load gambar dari URL |
| Accompanist Permissions | Handle runtime permission |

## Catatan Produksi

1. Ganti `BASE_URL_RELEASE` ke domain HTTPS produksi
2. Tambahkan dropdown daftar OPD (saat ini input ID manual di registrasi) — buat endpoint publik `/api/v1/opd` di backend
3. Tambahkan retry/queue lokal (Room) jika ingin mendukung absen saat offline lalu sync saat online
4. Pertimbangkan ProGuard/R8 minify untuk release (sudah disiapkan rule dasar)
5. Untuk produksi pemerintahan, pertimbangkan SafetyNet/Play Integrity API sebagai lapisan tambahan deteksi root/mock