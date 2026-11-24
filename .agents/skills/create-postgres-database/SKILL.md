---
name: create-postgres-database
description: Buat atau siapkan database PostgreSQL lokal Dagantara di Windows. Gunakan untuk menyiapkan instance pengembangan, database dagantara, dan akun aplikasi.
---

# Membuat database PostgreSQL Dagantara

Jalankan skrip dari root repo untuk menyiapkan instance lokal dengan PostgreSQL 16 yang sudah terpasang. Skrip menggunakan kredensial Windows terenkripsi di luar repo dan memeriksa koneksi sebagai `dagantara_app`.

```powershell
$result = & .\.agents\skills\create-postgres-database\scripts\create-database.ps1
$result
```

Default-nya adalah `127.0.0.1:5433` dengan data di `%LOCALAPPDATA%\Dagantara\postgresql-16`. Skrip memakai ulang instance tersebut, menyalakannya bila berhenti, dan mengisi role atau database yang masih belum ada. Gunakan `-PgBin` jika lokasi binary PostgreSQL 16 berbeda. Untuk instance tambahan yang memang diminta, gunakan nama direktori lokal dan port yang berbeda, misalnya `-StateName Dagantara-Test -Port 15433`. Periksa konflik port sebelum memilih instance tambahan.

Jika pengguna memilih instance PostgreSQL yang sudah ada, gunakan kredensial admin yang sah pada instance tersebut. Ikuti penyiapan database dan konfigurasi koneksi di [README backend](../../../backend/README.md#development), lalu verifikasi koneksi dengan akun aplikasi. Jika instance tersebut meminta password yang tidak tersedia, instance lokal terpisah adalah opsi ketika targetnya belum ditetapkan. Pertahankan konfigurasi, kredensial, dan data instance yang sudah ada.

Laporkan URL JDBC, nama akun aplikasi, status verifikasi, dan lokasi berkas kredensial tanpa menampilkan password. Untuk menjalankan backend, muat nilai yang dikembalikan skrip ke terminal yang sama.

```powershell
$db = Import-Clixml -LiteralPath $result.CredentialPath
$env:SPRING_DATASOURCE_URL = $result.JdbcUrl
$env:SPRING_DATASOURCE_USERNAME = $db.UserName
$env:SPRING_DATASOURCE_PASSWORD = $db.GetNetworkCredential().Password
```

Jika backend juga diminta berjalan, ikuti panduan [spring-boot-run](../spring-boot-run/SKILL.md) dan cek `/actuator/health/db` dengan pengaturan Actuator lokal di README backend. Untuk membuat tabel atau migrasi, gunakan [spring-boot-database](../spring-boot-database/SKILL.md).
