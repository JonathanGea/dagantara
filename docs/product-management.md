# Product Management

## Business Rules & Validation

- Setiap tenant mengelola katalog produknya sendiri (**Independent Catalog**).
- Satu produk dapat terhubung ke beberapa kategori dan memiliki satu **Primary Category**.
- Media dimiliki oleh produk.
- **Product Media** dan **Product Option Values** memiliki relasi many-to-many: satu media dapat terhubung ke beberapa nilai opsi dan satu nilai opsi dapat terhubung ke beberapa media.

## Tenant Admin — Menu & Features

| Area | Fitur dan aksi |
| --- | --- |
| Produk | Menentukan kategori produk dan Primary Category |
| Media Produk | Mengaitkan media produk dengan nilai opsi produk |

## Database — Entity Relationship Diagram

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
