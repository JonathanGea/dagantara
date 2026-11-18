# User Management

## Database — Entity Relationship Diagram

Skema final pada level tabel terdiri dari **6 tabel**: `tenants`, `tenant_domains`, `users`, `roles`, `permissions`, dan `role_permissions`.

```mermaid
erDiagram
    TENANTS {
        uuid id PK
        varchar code UK
        varchar name
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    TENANT_DOMAINS {
        uuid id PK
        uuid tenant_id FK
        varchar hostname UK
        varchar domain_kind
        varchar status
    }

    USERS {
        uuid id PK
        uuid tenant_id FK "nullable for platform"
        uuid role_id FK
        varchar username
        varchar email
        varchar full_name
        varchar password_hash
        varchar account_type
        varchar status
        uuid created_by FK "nullable"
        timestamp created_at
        timestamp updated_at
    }

    ROLES {
        uuid id PK
        uuid tenant_id FK "nullable for system"
        varchar code
        varchar name
        varchar scope
        boolean is_system
        timestamp created_at
    }

    PERMISSIONS {
        uuid id PK
        varchar code UK
        varchar module
        text description
    }

    ROLE_PERMISSIONS {
        uuid role_id PK,FK
        uuid permission_id PK,FK
    }

    TENANTS ||--o{ TENANT_DOMAINS : owns
    TENANTS |o--o{ USERS : contains
    TENANTS |o--o{ ROLES : defines
    ROLES ||--o{ USERS : assigned_to
    ROLES ||--o{ ROLE_PERMISSIONS : grants
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : used_by
    USERS |o--o{ USERS : created_by
```

**Keterangan:** relasi `TENANTS → USERS` dan `TENANTS → ROLES` bersifat opsional di sisi user/role karena akun platform dan role sistem dapat memiliki `tenant_id = NULL`.

### Penjelasan tabel

| Tabel | Fungsi |
| --- | --- |
| `tenants` | Menyimpan tenant yang menggunakan Dagantara. |
| `tenant_domains` | Menyimpan hostname/domain yang terhubung dengan tenant. |
| `users` | Menyimpan akun pengguna, tenant pengguna, dan role aktif pengguna. |
| `roles` | Menyimpan role yang dapat ditetapkan kepada pengguna. |
| `permissions` | Menyimpan kode permission yang digunakan untuk otorisasi. |
| `role_permissions` | Menghubungkan role dengan permission yang dimilikinya. |

### Penjelasan relasi

| Relasi | Makna |
| --- | --- |
| `tenants → tenant_domains` | Satu tenant dapat memiliki beberapa domain. |
| `tenants → users` | Satu tenant dapat memiliki banyak pengguna. Akun platform tidak memiliki `tenant_id`. |
| `tenants → roles` | Tenant dapat memiliki role tenant; role sistem dapat memiliki `tenant_id = NULL`. |
| `roles → users` | Setiap pengguna memiliki satu role aktif melalui `users.role_id`. |
| `roles ↔ permissions` | Many-to-many melalui `role_permissions` dengan pasangan `(role_id, permission_id)` sebagai primary key. |
| `users → users` | `users.created_by` mereferensikan pengguna pembuat akun. |

## Business Rules & Validation

- Pengguna platform memiliki `users.tenant_id = NULL`; pengguna tenant terhubung ke satu tenant.
- Setiap pengguna memiliki satu `users.role_id`.
- Permission diturunkan dari role melalui `role_permissions`. Akses khusus diatur dengan membuat role dan menetapkannya kepada pengguna yang sesuai.
- Pengguna yang sedang login dan memiliki permission `users.create` dapat membuat pengguna secara langsung.
- Role harus sesuai dengan scope dan tenant pengguna. Data tenant diakses dalam cakupan tenant terkait.
- Setiap pasangan `(role_id, permission_id)` pada `role_permissions` bersifat unik.

## Tenant Admin — Menu & Features

```text
Tenant Admin
└── User Management
    ├── Users
    └── Roles
```

| Menu | Submenu | Fitur / aksi |
| --- | --- | --- |
| User Management | Users | Membuat pengguna, menetapkan role pengguna |
| User Management | Roles | Membuat role, menetapkan permission pada role |

## Permissions

Permission diberikan melalui role. Permission `users.create` diperlukan untuk membuat pengguna.

## Workflow

### Membuat Pengguna

```mermaid
flowchart TD
    A[Pengguna login] --> B{Memiliki permission users.create?}
    B -- Ya --> C[Isi data pengguna dan pilih role]
    C --> D[Validasi role dan tenant]
    D --> E[Simpan pengguna]
    B -- Tidak --> F[Akses ditolak]
```

### Mengelola Role

```mermaid
flowchart LR
    A[Buat role] --> B[Pilih permissions]
    B --> C[Simpan role_permissions]
    C --> D[Tetapkan role ke pengguna]
```
