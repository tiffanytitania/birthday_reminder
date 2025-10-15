================================================================================
BIRTHDAY REMINDER APP
================================================================================

DESKRIPSI APLIKASI
------------------
Birthday Reminder adalah aplikasi mobile berbasis Android yang dirancang
untuk membantu komunitas atau organisasi mengelola dan mengingat ulang tahun
anggotanya. Aplikasi ini menyediakan sistem pengingat otomatis, fitur
messaging internal, dan panel admin lengkap untuk manajemen data anggota.


FITUR UTAMA
-----------
* Kalender Ulang Tahun Interaktif
* Notifikasi Otomatis (H-0, H-1, H-3, H-7)
* Direktori Anggota dengan Informasi Kontak
* Quick Contact (WhatsApp, Call, SMS)
* Sistem Messaging Internal
* Template Ucapan (10+ pilihan)
* Riwayat Pesan
* Statistik Komunitas
* Pengaturan Notifikasi Custom
* Role Management (Admin/Member)
* Member Management
* CRUD Birthday Data
* Kelola Data Komunitas
* Kirim Pengumuman Massal
* Export Data ke CSV/Text


ANGGOTA KELOMPOK
----------------
1. Felicia Annabel Ruriyanto   - NIM: 00000096911
2. Tiffany Titania Sunarga     - NIM: 00000087499
3. Sakura Nadira               - NIM: 00000082171
4. Frissa Budiansyah           - NIM: 00000087470


TEKNOLOGI YANG DIGUNAKAN
------------------------
* Language: Kotlin
* UI Framework: Android Jetpack, View Binding
* Design: Material Design 3
* Local Storage: SharedPreferences
* Background Task: WorkManager
* Libraries:
    - Glide (Image loading)
    - Retrofit + Moshi (API networking)
    - Material Calendar View
    - Gson (JSON parsing)
    - Coroutines (Async programming)


REQUIREMENTS
------------
* Android Studio: Ladybug (2024.2.1) atau lebih baru
* Min SDK: 24 (Android 7.0 Nougat)
* Target SDK: 34 (Android 14)
* Kotlin: 1.9.0
* Gradle: 8.7.3

CARA INSTALASI
--------------
1. Clone repository atau download source code
2. Buka project di Android Studio
3. Wait for Gradle sync
4. Build & Run project


DEMO ACCOUNT
------------
Untuk testing, gunakan akun berikut:

Admin:
Username: admin
Password: admin123

Member:
Username: member
Password: member123


STRUKTUR PROJECT
----------------
app/src/main/java/com/example/birthday_reminder/
- auth/                   (Authentication & User Management)
- data/                   (Data Models & API)
- messaging/              (Internal Messaging System)
- settings/               (Notification Settings)
- ui/adapter/             (RecyclerView Adapters)
- utils/                  (Helper Classes)
- worker/                 (Background Workers)
- MainActivity.kt
- LoginActivity.kt
- RegisterActivity.kt
- [Fragments]             (UI Fragments)


CARA MENGGUNAKAN NOTIFIKASI
----------------------------
1. Buka More -> Pengaturan Notifikasi
2. Toggle Aktifkan Notifikasi
3. Pilih kapan notifikasi dikirim:
    - H-0: Hari ini ulang tahun
    - H-1: Besok ulang tahun
    - H-3: 3 hari lagi
    - H-7: Seminggu lagi
4. Atur Jam Notifikasi (default: 08:00)
5. Klik Simpan

Note: Notifikasi akan dikirim setiap hari pada waktu yang ditentukan.


CARA KIRIM UCAPAN
-----------------
Via Internal Messaging:
1. Buka More -> Ucapan & Quotes
2. Klik Kirim Ucapan
3. Pilih anggota dari dropdown
4. Klik Pilih Template atau tulis manual
5. Klik Kirim

Via External App:
1. Buka Direktori Anggota
2. Pilih anggota
3. Klik icon WhatsApp/SMS/Call


EXPORT DATA (Admin Only)
-------------------------
1. Login as Admin
2. Buka More -> Panel Admin
3. Klik Ekspor Data
4. Pilih tipe data:
    - Daftar Ulang Tahun (nama, tanggal, usia, kontak)
    - Daftar Kontak (nama, HP, email)
5. Pilih format: CSV atau Text
6. Klik Bagikan -> Pilih app tujuan


TROUBLESHOOTING
---------------
Notifikasi Tidak Muncul:
1. Cek permission notifikasi di Settings
2. Disable battery optimization
3. Clear app data dan login ulang

Crash Saat Buka Fragment:
1. Pastikan dependencies sudah sync
2. Clean & Rebuild project
3. Invalidate cache di Android Studio

DATABASE STRUCTURE
------------------------------
birthdays/
- id1/
    - name: "John Doe"
    - date: "15/05/1995"
    - phone: "08123456789"
    - email: "john@example.com"
    - role: "member"

community_info/
- name: "Birthday Reminder Community"
- description: "Komunitas pengingat ulang tahun"
- updatedBy: "admin"
- updatedAt: 1234567890

announcements/
- id1/
    - title: "Welcome!"
    - message: "Selamat datang"
    - sender: "admin"
    - timestamp: 1234567890


DESIGN GUIDELINES
-----------------
* Primary Color: Purple (#9C27B0)
* Accent Color: Pink (#E91E63)
* Font: Comic Sans MS (custom font)
* Icon Style: Material Icons with emoji
* UI Pattern: Bottom Navigation + Fragment-based


FUTURE ENHANCEMENTS
-------------------
* Push Notifications
* Reminder Sharing - Share birthdays via link

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
- Firebase untuk backend infrastructure
- Material Design untuk UI components
- ZenQuotes API untuk inspirational quotes
- Android Development Community


================================================================================
Made with Love by Birthday Reminder Team!
================================================================================