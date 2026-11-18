# Product Management

## Catalog

Setiap tenant memiliki katalog produk sendiri.

## Kategori Produk

Satu produk dapat masuk ke beberapa kategori, dengan tepat satu **Primary Category**.

## Media Produk

Media dimiliki oleh produk. Relasi antara **Product Media** dan **Product Option Values** bersifat many-to-many: satu media dapat terkait dengan beberapa nilai opsi, dan satu nilai opsi dapat terkait dengan beberapa media.

## Diagram Relasi

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
