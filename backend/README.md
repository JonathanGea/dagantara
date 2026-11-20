# Dagantara Backend

Prasyarat menjalankan proyek adalah JDK 25 (LTS). Proyek menyertakan Maven Wrapper yang menjalankan Maven 3.9.9, sehingga instalasi Maven terpisah tidak diperlukan.

Pastikan `JAVA_HOME` menunjuk ke JDK 25. Jika `java -version` masih menunjukkan versi lain di PowerShell, dahulukan JDK dari `JAVA_HOME` untuk sesi terminal tersebut:

```powershell
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

Jalankan dari root proyek.

```bash
cd backend
./mvnw spring-boot:run
```

Di PowerShell, gunakan `cd backend` lalu `.\mvnw.cmd spring-boot:run`.

Cek aplikasi di terminal lain.

```bash
curl --fail http://localhost:8080/actuator/health
```

Respons yang diharapkan adalah `{"status":"UP"}`.

Untuk membuat JAR, jalankan `./mvnw clean package` dari folder `backend` (atau `.\mvnw.cmd clean package` di PowerShell).
