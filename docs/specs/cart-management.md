# Cart Management

> **Status — Draf awal.** Bahas keputusan pembuka lewat prompt di [Progres Modul](../planning/progress.md#cart-management). Implementasi mengikuti spesifikasi yang sudah disetujui.

## Problem Statement

Pembeli perlu meninjau produk dan jumlah yang dipilih sebelum checkout. Toko perlu mengetahui keranjang mana yang terkait dengan kunjungan pembeli.

## Solution

Rumuskan cara keranjang menyimpan pilihan pembeli dan mengikuti kondisi produk, varian, dan stok toko.

## User Stories

Isi daftar cerita pengguna setelah pelaku, kebutuhan, dan batas versi pertama disepakati.

## Implementation Decisions

**Keputusan pembuka.** Bagaimana toko mengenali keranjang pembeli ketika ia kembali membuka URL toko?

**Aturan identitas.** Cart melayani pengunjung tanpa login dan setiap keranjang tamu dimiliki satu tenant. Ikuti [CM-01 dan CM-02](customer-management.md#implementation-decisions).

**Rujukan pembahasan.** [DEP-02](../planning/development-dependencies.md#open-decisions--blockers)

## Testing Decisions

Tetapkan kondisi, tindakan, dan hasil yang harus terlihat setelah perilaku disepakati. Gunakan Bruno untuk penerimaan kontrak HTTP bila termasuk dalam cakupan.

## Out of Scope

Catat fitur dan variasi alur yang ditunda setelah cakupan versi pertama disepakati.
