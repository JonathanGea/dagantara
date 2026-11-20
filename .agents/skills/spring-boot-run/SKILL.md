---
name: spring-boot-run
description: Gunakan saat diminta menjalankan atau memeriksa backend Spring Boot Dagantara secara lokal. Bukan untuk deployment atau implementasi fitur.
---

# Menjalankan Backend Dagantara

Skill ini menjalankan backend, bukan hanya menjelaskan perintahnya. Gunakan `backend/README.md` untuk konfigurasi terbaru dan `backend/pom.xml` untuk target Java. Tidak ada ID Functional Specification atau Acceptance Scenario modul yang otomatis tercakup.

## Windows PowerShell

1. Pastikan repository dan branch yang diminta, lalu baca `AGENTS.md`, `backend/AGENTS.md`, dan `backend/README.md`.
2. Dari root repository, jalankan `& .\.agents\skills\spring-boot-run\scripts\run-backend.ps1`. Gunakan `-Port <nomor>` hanya bila port lokal yang berbeda memang diminta. Skrip memakai `JAVA_HOME` sesuai target `pom.xml`, membangun JAR dengan Maven Wrapper, menjalankan proses Java tersembunyi, dan menunggu respons health `UP`.
3. Bila port sudah dipakai, skrip hanya menganggap backend telah berjalan jika prosesnya berasal dari folder backend ini dan health `UP`; proses lain tidak dihentikan. Baca pesan kesalahan dan log di `backend/target/run-backend.*.log` bila startup gagal. Jangan mencetak rahasia dari log.
4. Laporkan hasil skrip, PID, URL health, dan keterbatasan verifikasi. Health `UP` tidak membuktikan fitur domain atau test suite lulus.

## Shell POSIX

Gunakan `./mvnw spring-boot:run` dari folder `backend`, biarkan proses tetap hidup, lalu verifikasi `http://localhost:8080/actuator/health`. Periksa port sebelum memulai agar tidak membuka instance kedua.
