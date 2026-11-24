# Glosarium Dagantara

## Pemilik Tenant

Akun pertama yang ditetapkan saat provisioning tenant. Setiap tenant memiliki satu Pemilik Tenant dengan role aktif Admin Tenant. Pemilik berwenang memberi dan mencabut role Admin Tenant untuk akun lain dalam tenant tersebut.

## Varian Produk

Kombinasi nilai opsi yang disediakan untuk suatu produk, misalnya Kaos Basic warna Hitam ukuran M. Produk tanpa opsi memiliki satu varian bawaan. Inventory menggunakan UUID varian untuk merujuk unit yang dicatat stoknya.

## SKU

Kode unik dalam satu tenant untuk mengidentifikasi varian produk. Petugas memakai SKU untuk memilih varian saat mencatat barang masuk.

## Penerimaan Barang

Pencatatan masuk satu kiriman ke gudang yang berisi satu atau lebih SKU dan jumlah per SKU. Konfirmasi penerimaan menambah saldo barang pada gudang itu.

## Saldo Stok

Jumlah unit hasil transaksi stok yang sudah dikonfirmasi untuk satu varian di satu gudang. Total per produk atau opsi dihitung dari saldo variannya.
