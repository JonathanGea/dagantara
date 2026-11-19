# Product Management

## Module Overview

Mengelola katalog produk per tenant (**Independent Catalog**), hubungan produk–kategori, dan asosiasi media produk dengan nilai opsi. Produk berada dalam katalog tenant pemiliknya.

## Admin UI Structure

Referensi struktur navigasi Tenant Admin:

```text
Tenant Admin
└── Catalog
    ├── Products
    ├── Categories
    ├── Brands
    ├── Collections
    └── Option Library
```

Nama submenu di atas mengikuti referensi navigasi. Spesifikasi fungsi yang sudah disepakati saat ini:

| Menu | Submenu / halaman | Fitur dan aksi |
| --- | --- | --- |
| Catalog | Products | Menghubungkan produk dengan beberapa kategori dan memilih satu Primary Category. |
| Catalog | Products | Menghubungkan media produk ke nilai opsi produk. |

## Functional Specification

| ID | Perilaku |
| --- | --- |
| **PM-01** | Setiap tenant memiliki katalog produknya sendiri. Produk terhubung ke tenant pemilik. |
| **PM-02** | Produk dapat terhubung ke beberapa kategori dan memiliki tepat satu Primary Category. |
| **PM-03** | Media produk dimiliki oleh produk terkait. |
| **PM-04** | Satu media dapat dihubungkan dengan beberapa Product Option Values; satu Product Option Value dapat mempunyai beberapa media. |

## Business Rules

- **PM-BR-01:** katalog produk independen untuk setiap tenant.
- **PM-BR-02:** kategori produk bersifat many-to-many melalui `product_categories`; satu produk memiliki satu Primary Category.
- **PM-BR-03:** media merupakan bagian dari produk, bukan bagian dari pustaka nilai opsi.
- **PM-BR-04:** hubungan Product Media ↔ Product Option Values bersifat many-to-many melalui `media_option_values`.

## Data Model

```mermaid
erDiagram
    TENANTS {
        uuid id PK
    }

    PRODUCTS {
        uuid id PK
        uuid tenant_id FK
    }

    CATEGORIES {
        uuid id PK
    }

    PRODUCT_CATEGORIES {
        uuid product_id PK, FK
        uuid category_id PK, FK
    }

    PRODUCT_MEDIA {
        uuid id PK
        uuid product_id FK
    }

    PRODUCT_OPTION_VALUES {
        uuid id PK
        uuid product_id FK
    }

    MEDIA_OPTION_VALUES {
        uuid media_id PK, FK
        uuid option_value_id PK, FK
    }

    TENANTS ||--o{ PRODUCTS : owns
    PRODUCTS ||--o{ PRODUCT_CATEGORIES : categorized_as
    CATEGORIES ||--o{ PRODUCT_CATEGORIES : groups
    PRODUCTS ||--o{ PRODUCT_MEDIA : has
    PRODUCTS ||--o{ PRODUCT_OPTION_VALUES : has
    PRODUCT_MEDIA ||--o{ MEDIA_OPTION_VALUES : links
    PRODUCT_OPTION_VALUES ||--o{ MEDIA_OPTION_VALUES : links
```

### Penjelasan tabel

| Tabel | Fungsi |
| --- | --- |
| `tenants` | Tenant pemilik katalog produk. |
| `products` | Data produk yang dimiliki tenant melalui `tenant_id`. |
| `categories` | Data kategori produk. |
| `product_categories` | Tabel penghubung produk dengan kategori. |
| `product_media` | Media yang dimiliki suatu produk. |
| `product_option_values` | Nilai opsi yang terkait dengan produk. |
| `media_option_values` | Tabel penghubung media produk dengan nilai opsi. |

### Penjelasan relasi

| Relasi | Makna |
| --- | --- |
| `tenants → products` | Setiap tenant memiliki katalog produknya sendiri. |
| `products ↔ categories` | Many-to-many melalui `product_categories`. Satu produk memiliki satu Primary Category dari kategori yang dipilih. |
| `products → product_media` | Satu produk dapat memiliki beberapa media. |
| `products → product_option_values` | Produk memiliki nilai opsi yang dapat dikaitkan dengan medianya. |
| `product_media ↔ product_option_values` | Many-to-many melalui `media_option_values`; media dapat ditautkan ke beberapa nilai opsi dan sebaliknya. |

**Implementation dependency — PM-02:** lokasi penyimpanan penanda Primary Category belum ditentukan dalam model yang disepakati. Jangan menetapkan kolom atau constraint baru untuk itu sebelum ada keputusan skema.

## Acceptance Scenarios

| ID | Kondisi | Tindakan | Hasil yang diharapkan |
| --- | --- | --- | --- |
| **PM-AT-01** | Terdapat tenant A dan tenant B. | Mengaitkan produk ke katalog tenant A. | Produk mempunyai kepemilikan tenant A dan tidak menjadi entri katalog bersama lintas tenant. |
| **PM-AT-02** | Satu produk memiliki dua kategori. | Menetapkan satu kategori sebagai Primary Category. | Kedua kategori tetap terhubung dan tepat satu menjadi Primary Category. Implementasi penyimpanannya mengikuti keputusan skema PM-02. |
| **PM-AT-03** | Satu produk memiliki media M1 dan nilai opsi V1, V2. | Mengaitkan M1 dengan V1 dan V2. | M1 mempunyai dua asosiasi nilai opsi. |
| **PM-AT-04** | Nilai opsi V1 berkaitan dengan media M1 dan M2 milik produk yang sama. | Mengaitkan V1 dengan kedua media. | V1 mempunyai dua asosiasi media tanpa memindahkan kepemilikan media dari produk. |
