# Checkout Management

> **Status — Draf awal.** Bahas keputusan pembuka lewat prompt di [Progres Modul](../planning/progress.md#checkout-management). Implementasi mengikuti spesifikasi yang sudah disetujui.

## Problem Statement

Pembeli perlu memastikan detail pembelian sebelum pilihan di keranjang menjadi pesanan.

## Solution

Rumuskan informasi dan validasi yang diperlukan agar pembeli dapat mengirim pesanan dari keranjangnya.

## User Stories

Isi daftar cerita pengguna setelah pelaku, kebutuhan, dan batas versi pertama disepakati.

## Implementation Decisions

**Keputusan pembuka.** Data apa yang wajib diisi pembeli sebelum bisa membuat pesanan?

**Aturan identitas.** Checkout menerima keranjang tamu dari toko yang sama dan meneruskan identitas serta kontak tamu untuk disimpan pada pesanan. Ikuti [CM-01 sampai CM-03](customer-management.md#implementation-decisions).

**Rujukan pembahasan.** [DEP-05 dan DEP-07](../planning/development-dependencies.md#open-decisions--blockers)

## Testing Decisions

Tetapkan kondisi, tindakan, dan hasil yang harus terlihat setelah perilaku disepakati. Gunakan Bruno untuk penerimaan kontrak HTTP bila termasuk dalam cakupan.

## Out of Scope

Catat fitur dan variasi alur yang ditunda setelah cakupan versi pertama disepakati.
