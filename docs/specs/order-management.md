# Order Management

> **Status — Draf awal.** Bahas keputusan pembuka lewat prompt di [Progres Modul](../planning/progress.md#order-management). Implementasi mengikuti spesifikasi yang sudah disetujui.

## Problem Statement

Pembeli dan pengelola toko perlu mengetahui status pesanan dan tindakan yang tersedia setelah checkout.

## Solution

Rumuskan siklus hidup pesanan serta riwayat tindakan yang perlu diketahui pembeli dan pengelola.

## User Stories

Isi daftar cerita pengguna setelah pelaku, kebutuhan, dan batas versi pertama disepakati.

## Implementation Decisions

**Keputusan pembuka.** Apa status awal pesanan ketika pembeli berhasil checkout?

**Aturan identitas.** Setiap pesanan menyimpan identitas dan kontak yang diberikan ketika tamu checkout. Ikuti [CM-03](customer-management.md#implementation-decisions).

**Rujukan pembahasan.** [DEP-06](../planning/development-dependencies.md#open-decisions--blockers)

## Testing Decisions

Tetapkan kondisi, tindakan, dan hasil yang harus terlihat setelah perilaku disepakati. Gunakan Bruno untuk penerimaan kontrak HTTP bila termasuk dalam cakupan.

## Out of Scope

Catat fitur dan variasi alur yang ditunda setelah cakupan versi pertama disepakati.
