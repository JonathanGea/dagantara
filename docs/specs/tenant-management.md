# Tenant Management

> **Status — Draf awal.** Bahas keputusan pembuka lewat prompt di [Progres Modul](../planning/progress.md#tenant-management). Implementasi mengikuti spesifikasi yang sudah disetujui.

## Problem Statement

Operator platform perlu menangani tenant dan alamat tokonya sepanjang siklus hidup tenant. Alur onboarding dan pemetaan URL awal sudah dijelaskan di User Management.

## Solution

Rumuskan siklus hidup tenant dan domain, termasuk dampak perubahan status pada akses toko publik.

## User Stories

Isi daftar cerita pengguna setelah pelaku, kebutuhan, dan batas versi pertama disepakati.

## Implementation Decisions

**Keputusan pembuka.** Saat tenant dinonaktifkan, apa yang harus terjadi pada URL toko publiknya?

**Rujukan pembahasan.** [DEP-01](../planning/development-dependencies.md#open-decisions--blockers)

## Testing Decisions

Tetapkan kondisi, tindakan, dan hasil yang harus terlihat setelah perilaku disepakati. Gunakan Bruno untuk penerimaan kontrak HTTP bila termasuk dalam cakupan.

## Out of Scope

Catat fitur dan variasi alur yang ditunda setelah cakupan versi pertama disepakati.
