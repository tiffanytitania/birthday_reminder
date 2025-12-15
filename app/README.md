================================================================================================================================================================
BIRTHDAY REMINDER APP
================================================================================

FUTURE ENHANCEMENTS
-------------------
Planned Features:
- Push Notifications via Firebase Cloud Messaging
- Birthday Sharing via deep links
- Photo Gallery untuk setiap anggota
- Event Calendar untuk acara komunitas
- Gift Wishlist untuk setiap anggota
- Birthday Countdown Widget
- Dark Mode support
- Multiple Languages (i18n)
- Cloud Backup untuk data lokal
- Birthday Reminders via WhatsApp integration

Under Consideration:
- Zodiac sign calculator
- Age calculator widget
- Birthday statistics charts
- Social media integration
- Birthday party planner


LICENSE
-------
This project is created for educational purposes.
(c) 2025 Birthday Reminder Team. All rights reserved.


CONTACT
-------
Untuk pertanyaan atau bantuan, hubungi salah satu anggota tim:
- Felicia Annabel Ruriyanto - NIM: 00000096911
- Tiffany Titania Sunarga   - NIM: 00000087499
- Sakura Nadira             - NIM: 00000082171
- Frissa Budiansyah         - NIM: 00000087470


ACKNOWLEDGMENTS
---------------
Special thanks to:
- Firebase (Backend infrastructure & Realtime Database)
- ImageKit.io (CDN & Image optimization)
- Google (Android SDK, Material Design, Location Services, Maps API)
- ZenQuotes API (Inspirational quotes)
- TensorFlow (Machine Learning framework)
- Android Development Community (Libraries & support)
- Material Design Team (UI/UX guidelines)
- Binus University (Educational support)


PROJECT STATS
-------------
* Lines of Code: 10,000+
* Kotlin Files: 50+
* XML Layouts: 40+
* Features: 30+
* Dependencies: 25+
* Development Time: 3 months
* Team Members: 4

DESKRIPSI APLIKASI
------------------
Birthday Reminder adalah aplikasi mobile berbasis Android yang dirancang
untuk membantu komunitas atau organisasi mengelola dan mengingat ulang tahun
anggotanya. Aplikasi ini menyediakan sistem pengingat otomatis, fitur
messaging internal, AI sentiment analysis, location sharing, dan panel admin
lengkap untuk manajemen data anggota.


FITUR UTAMA
-----------
1. FITUR ULANG TAHUN
    * Kalender Ulang Tahun Interaktif dengan dot indicator
    * Upcoming Birthdays (hari ini, minggu ini, bulan ini)
    * Notifikasi Otomatis (H-0, H-1, H-3, H-7) dengan waktu presisi
    * CRUD Birthday Data (Admin only)
    * Search & Filter anggota

2. DIREKTORI ANGGOTA
    * Member Directory - Daftar lengkap anggota
    * Member Detail dengan informasi lengkap
    * Quick Contact (WhatsApp, Call, SMS, Email)
    * Profile Photos dengan ImageKit CDN
    * Role Badge (Admin/Member)

3. SISTEM MESSAGING INTERNAL
    * Kirim Ucapan antar anggota
    * Template Ucapan (10+ pilihan)
    * AI Sentiment Analysis (TensorFlow Lite)
    * Location Sharing dengan Google Maps
    * Inbox & Sent Messages
    * Unread Badge
    * Message History lengkap

4. STATISTIK KOMUNITAS
    * Total Members
    * Birthdays Today/This Week/This Month
    * Average Age
    * Oldest & Youngest Member

5. PANEL ADMIN (Admin Only)
    * Community Management (nama, deskripsi, banner)
    * User Management (edit role, hapus user)
    * Announcement System (broadcast ke semua)
    * Export Data (CSV/Text format)

6. PENGATURAN
    * Notification Settings dengan waktu presisi (jam & menit)
    * Profile Management lengkap
    * Upload Foto Profil (Camera/Gallery)

7. FITUR TAMBAHAN
    * Quotes & Inspirasi dari ZenQuotes API
    * Google Maps Integration
    * ImageKit CDN untuk optimasi gambar
    * Geocoding (koordinat ke nama lokasi)
    * Role-Based Access Control
    * Auto Login dengan persistent session



ANGGOTA KELOMPOK
----------------
1. Felicia Annabel Ruriyanto   - NIM: 00000096911
2. Tiffany Titania Sunarga     - NIM: 00000087499
3. Sakura Nadira               - NIM: 00000082171
4. Frissa Budiansyah           - NIM: 00000087470


TEKNOLOGI YANG DIGUNAKAN
------------------------
* Language: Kotlin 1.9.0
* UI Framework: Android Jetpack, View Binding
* Design: Material Design 3
* Backend: Firebase Realtime Database (Asia Southeast 1)
* CDN: ImageKit.io
* Local Storage: SharedPreferences
* Background Task: WorkManager
* Machine Learning: TensorFlow Lite 2.14.0

* Libraries:
    - UI & Image:
        * Glide 4.16.0 (image loading & caching)
        * Material Calendar View 1.4.3
        * Material Components
        * RecyclerView & CoordinatorLayout

    - Networking:
        * Retrofit 2.9.0 (REST API)
        * Moshi 1.15.0 (JSON parsing)
        * OkHttp 4.12.0 (HTTP client)

    - Firebase:
        * Firebase Realtime Database 20.3.1
        * Firebase Storage 20.3.0
        * Firebase Auth 23.1.0
        * Firebase Analytics 21.3.0

    - Machine Learning:
        * TensorFlow Lite 2.14.0
        * TensorFlow Lite Support 0.4.4
        * TensorFlow Lite Metadata 0.4.4
        * TensorFlow Lite GPU 2.14.0

    - Location:
        * Google Play Services Location 21.3.0

    - Background Processing:
        * WorkManager 2.8.1
        * Coroutines (Core & Android) 1.8.0

    - Lifecycle & ViewModel:
        * Lifecycle ViewModel KTX 2.7.0
        * Lifecycle LiveData KTX 2.7.0
        * Fragment KTX 1.6.2


REQUIREMENTS
------------
* Android Studio: Ladybug (2024.2.1) atau lebih baru
* Min SDK: 24 (Android 7.0 Nougat)
* Target SDK: 34 (Android 14)
* Compile SDK: 36
* Kotlin: 1.9.0
* Gradle: 8.7.3
* Java: 17


CARA INSTALASI
--------------
1. Clone repository atau download source code
2. Buka project di Android Studio
3. Setup Firebase:
    - Download google-services.json dari Firebase Console
    - Letakkan di folder app/
    - Pastikan Database URL di FirebaseConfig.kt sudah benar
4. (Optional) Setup ImageKit di ImageKitConfig.kt
5. Wait for Gradle sync
6. Clean Project (Build -> Clean Project)
7. Rebuild Project (Build -> Rebuild Project)
8. Build & Run project


DEMO ACCOUNT
------------
Untuk testing, gunakan akun berikut:

Admin:
Username: admin
Password: admin123

Privileges:
- Full CRUD access
- User management
- Community settings
- Export data
- Send announcements

Member:
Bisa register sendiri via Register Activity

Privileges:
- View birthdays
- Send greetings
- View directory
- Edit own profile



STRUKTUR PROJECT
----------------
app/src/main/java/com/example/birthday_reminder/
│
├── auth/                           Authentication & User Management
│   └── UserManager.kt
│
├── data/                           Data Layer
│   ├── api/                        API Services
│   │   ├── QuoteApiService.kt
│   │   └── RetrofitInstance.kt
│   ├── model/                      Data Models
│   │   ├── Birthday.kt
│   │   ├── Member.kt
│   │   ├── Message.kt
│   │   ├── Quote.kt
│   │   └── NotificationSettings.kt
│   └── repository/                 Repositories
│       ├── BirthdayRepository.kt
│       └── StatisticsRepository.kt
│
├── messaging/                      Internal Messaging System
│   └── MessageManager.kt
│
├── ml/                             Machine Learning
│   └── SentimentAnalyzer.kt
│
├── settings/                       Settings Management
│   └── NotificationSettingsManager.kt
│
├── ui/                             UI Layer
│   ├── adapter/                    RecyclerView Adapters
│   │   ├── BirthdayListAdapter.kt
│   │   ├── MemberAdapter.kt
│   │   ├── MessageAdapter.kt
│   │   ├── QuoteAdapter.kt
│   │   └── UserManagementAdapter.kt
│   └── viewmodel/                  ViewModels
│       ├── BirthdayViewModel.kt
│       └── StatisticsViewModel.kt
│
├── utils/                          Helper Classes
│   ├── ExportManager.kt
│   ├── FirebaseConfig.kt
│   ├── ImageKitConfig.kt
│   ├── ImageKitManager.kt
│   ├── LocationHelper.kt
│   ├── NotificationHelper.kt
│   └── TestHelper.kt
│
├── worker/                         Background Workers
│   └── BirthdayWorker.kt
│
├── Activities
│   ├── MainActivity.kt
│   ├── LoginActivity.kt
│   └── RegisterActivity.kt
│
└── Fragments
├── HomeFragment.kt
├── UpcomingBirthdaysFragment.kt
├── AddBirthdayFragment.kt
├── StatisticsFragment.kt
├── MoreFragment.kt
├── ProfileFragment.kt
├── MemberDirectoryFragment.kt
├── MemberDetailFragment.kt
├── SendGreetingFragment.kt
├── MessagingFragment.kt
├── HistoryFragment.kt
├── NotificationSettingsFragment.kt
├── AdminPanelFragment.kt
├── UserManagementFragment.kt
└── ExportDataFragment.kt


CARA MENGGUNAKAN APLIKASI
--------------------------

MENGELOLA ULANG TAHUN (Admin Only)
Menambah:
1. Login sebagai Admin
2. Tap "Tambah Ulang Tahun" di bottom navigation
3. Klik FAB (+) di kanan bawah
4. Isi nama dan tanggal lahir
5. Klik Simpan

Edit:
1. Di halaman Tambah Ulang Tahun
2. Klik icon Edit pada item
3. Ubah data
4. Klik Update

Hapus:
1. Di halaman Tambah Ulang Tahun
2. Klik icon Delete pada item
3. Konfirmasi hapus

MENGATUR NOTIFIKASI
1. Buka More -> Pengaturan Notifikasi
2. Toggle Aktifkan Notifikasi
3. Pilih kapan notifikasi dikirim:
    - H-0: Hari ini ulang tahun
    - H-1: Besok ulang tahun
    - H-3: 3 hari lagi
    - H-7: Seminggu lagi
4. Klik Ubah Waktu Notifikasi
5. Pilih jam & menit (contoh: 07:30)
6. Klik Simpan
7. (Optional) Klik Test Notifikasi untuk cek

Note: Notifikasi dikirim otomatis setiap hari pada waktu yang ditentukan.

MENGIRIM UCAPAN
Via Internal Messaging:
1. Buka More -> Ucapan & Quotes
2. Klik card "Kirim Ucapan"
3. Pilih penerima dari dropdown
4. Klik Pilih Template atau tulis manual
5. (Optional) Klik Get Location untuk share lokasi
6. Klik Kirim

Via External App:
1. Buka More -> Direktori Anggota
2. Pilih anggota
3. Klik icon WhatsApp/Call/SMS

MENGELOLA PROFIL
1. Buka More -> Profil Saya
2. Klik Edit Profil
3. Update: Nama lengkap, Tanggal lahir, Nomor HP
4. Klik Simpan

Upload Foto Profil:
1. Di halaman Profil, klik icon Camera
2. Pilih: Camera (foto baru), Gallery (dari galeri), Remove (hapus)
3. Foto akan di-upload ke ImageKit CDN

PANEL ADMIN (Admin Only)
Kelola Komunitas:
1. Login sebagai Admin
2. Buka More -> Panel Admin
3. Edit nama & deskripsi komunitas
4. Klik Upload Banner untuk ganti banner
5. Klik Simpan Data Komunitas

User Management:
1. Klik Kelola User
2. Lihat daftar semua user dengan role
3. Klik Edit Role untuk ubah Admin/Member
4. Klik Delete untuk hapus user

Kirim Pengumuman:
1. Klik Kirim Pengumuman
2. Isi judul dan pesan
3. Klik Kirim (broadcast ke semua anggota)

Export Data:
1. Klik Ekspor Data
2. Pilih: Daftar Ulang Tahun atau Daftar Kontak
3. Pilih format: CSV atau Text
4. Klik Bagikan -> pilih app tujuan



DATABASE STRUCTURE
------------------
birthdays/
- birthday_id_1/
    - name: "John Doe"
    - date: "15/5/1995"
    - phone: "08123456789"
    - email: "john@example.com"
    - role: "member"
    - createdAt: 1234567890
    - updatedAt: 1234567890

community_info/
- name: "Birthday Reminder Community"
- description: "Komunitas pengingat ulang tahun"
- bannerUrl: "https://ik.imagekit.io/.../banner.jpg"
- updatedBy: "admin"
- updatedAt: 1234567890

announcements/
- announcement_id_1/
    - title: "Welcome!"
    - message: "Selamat datang di Birthday Reminder"
    - sender: "admin"
    - timestamp: 1234567890
    - type: "announcement"

users/
- username/
    - photoUrl: "https://ik.imagekit.io/.../profile.jpg"


DESIGN GUIDELINES
-----------------
* Primary Color: Purple (#9C27B0)
* Accent Color: Pink (#E91E63)
* Background: Light Gray (#F5F5F5)
* Success: Green (#4CAF50)
* Error: Red (#F44336)
* Font: Comic Sans MS (custom font)
* Icon Style: Material Icons with emoji
* UI Pattern: Bottom Navigation + Fragment-based


TROUBLESHOOTING
---------------
NOTIFIKASI TIDAK MUNCUL:
1. Cek Permission:
    - Settings -> Apps -> Birthday Reminder -> Permissions
    - Enable "Notifications"
2. Disable Battery Optimization:
    - Settings -> Battery -> Battery Optimization
    - Find "Birthday Reminder" -> Don't optimize
3. Reset Notifikasi:
    - Clear app data
    - Login ulang
    - Setup notifikasi lagi

FORCE CLOSE SAAT UPDATE DATA:
Penyebab: URL Firebase Database tidak konsisten
Solusi:
1. Pastikan semua file menggunakan URL yang sama
2. Gunakan FirebaseConfig.getDatabaseReference()
3. Clean & Rebuild project

FOTO PROFIL TIDAK MUNCUL:
1. Cek ImageKit Config:
    - Pastikan PUBLIC_KEY & PRIVATE_KEY benar
    - Cek URL_ENDPOINT sudah sesuai
2. Cek Internet Connection
3. Clear Glide Cache

LOCATION TIDAK BISA DIAMBIL:
1. Enable GPS: Settings -> Location -> ON
2. Grant Permission: Allow "Location" permission
3. Cek Google Play Services: Update jika perlu

WORKMANAGER TIDAK JALAN:
1. Cek Logcat dengan tag: BirthdayWorker
2. Restart app
3. Re-schedule notification di Notification Settings