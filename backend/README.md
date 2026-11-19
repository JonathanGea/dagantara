# Dagantara Backend

Prasyarat menjalankan proyek adalah JDK 21 dan Maven 3.6.3 atau lebih baru.

Jalankan dari root proyek.

```bash
cd backend
mvn spring-boot:run
```

Cek aplikasi di terminal lain.

```bash
curl --fail http://localhost:8080/actuator/health
```

Respons yang diharapkan adalah `{"status":"UP"}`.

Untuk membuat JAR, jalankan `mvn clean package` dari folder `backend`.
