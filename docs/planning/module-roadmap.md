# Module Roadmap & Ownership

This planning document proposes module scope and priorities. See [Module Progress](progress.md) for spec and implementation status.

## Module Overview

The matrix records module ownership and proposed priorities. Module specifications define requirements, navigation, data models, and acceptance criteria.

## Priority Definitions

- **P0:** proposed MVP scope required to operate a tenant storefront and process an online order.
- **P1:** post-MVP improvements.
- **P2:** later-stage or conditional capabilities.

## Module Matrix

| No. | Module | Surface | Priority | Scope / notes |
| --- | --- | --- | --- | --- |
| 1 | User Management | Platform Admin / Tenant Admin | P0 | Identity, roles, permissions, and tenant-scoped access |
| 2 | Product Management | Tenant Admin | P0 | Independent catalog, variants, categories, and media |
| 3 | Tenant Management | Platform Admin | P0 | Tenant lifecycle, domain mapping, isolation |
| 4 | Store Settings | Tenant Admin | P0 | Store identity, contact, basic operational settings |
| 5 | Storefront Management | Tenant Admin | P0/P1 | P0 basic layout/navigation; P1 advanced customization |
| 6 | Inventory Management | Tenant Admin | P0 | Stock receipts, corrections, and variant availability in [v1](../specs/inventory-management.md). Reservation rules are tracked in [DEP-03](development-dependencies.md#open-decisions--blockers). |
| 7 | Product Catalog / Discovery | Storefront | P0 | Public listing and detail are available. [Discovery v1](../specs/product-discovery.md) adds name search and one category filter. Collections remain a future discussion. |
| 8 | Customer Management | Tenant Admin / Storefront | P0 | Customer identity, guest/registered decisions |
| 9 | Cart Management | Storefront | P0 | Cart items and quantity |
| 10 | Checkout Management | Storefront | P0 | Checkout validation and order submission |
| 11 | Order Management | Tenant Admin / Storefront | P0 | Order lifecycle and history |
| 12 | Payment Management | Tenant Admin / Storefront | P0 | Manual transfer and admin verification |
| 13 | Shipping Management | Tenant Admin / Storefront | P0 | Shipping fees, fulfillment, tracking |
| 14 | Address & Location Management | Tenant Admin / Storefront | P0 | Customer addresses and shipping origins; may be subfeatures |
| 15 | Pricing & Promotion | Tenant Admin | P1 | Discounts, vouchers, campaign rules |
| 16 | Notification Management | Tenant Admin / Storefront | P1 | Transaction notifications |
| 17 | Reporting & Analytics | Platform Admin / Tenant Admin | P1 | Tenant-scoped and platform-wide metrics |
| 18 | Tax Management | Tenant Admin | P1 | Tax configuration and calculations |
| 19 | Platform Configuration | Platform Admin | P1 | Global configuration |
| 20 | Return & Refund | Tenant Admin / Storefront | P2 | Returns and refunds |
| 21 | POS Management | Tenant Admin | P2 | Offline sales, shared inventory |
| 22 | Review & Rating | Tenant Admin / Storefront | P2 | Product reviews |
| 23 | Audit & Activity Log | Platform Admin / Tenant Admin | P2 | Activity traceability |
| 24 | Subscription & Billing | Platform Admin / Tenant Admin | P2 conditional | Only if SaaS monetization is adopted |

## Ownership & Boundaries

- **Platform Admin:** manages cross-tenant platform operations. Cross-tenant access requires explicit authorization.
- **Tenant Admin:** manages only the resolved tenant's resources.
- **Storefront:** customer-facing public and authenticated flows, resolved from the tenant's domain/context.
- Cart and Checkout are primarily storefront flows, not automatically Tenant Admin menus.
- Address & Location may be organized under Customer and Shipping; do not create a standalone service/table merely because it appears in this roadmap.
- Storefront Management is **one module** with basic (P0) and advanced (P1) phases, not two technical modules.
- Product Catalog reads the tenant's published product data; it does not create a second independent product catalog.
- Product/variant stock reservations belong to Inventory, and payment proof/verification belongs to Payment.

## Known Decisions & Dependencies

- Each tenant has an independent catalog; tenant isolation is mandatory.
- Initial payment uses manual bank transfer with tenant bank account and admin verification; payment gateway is not an MVP requirement.
- Online commerce comes before POS.
- Tenant-specific storefront domains are supported in the architecture; the exact tenant/domain lifecycle needs a dedicated specification.
- Subscription & Billing remains conditional on a future SaaS pricing decision.
- Guest checkout, tax handling, delivery rate calculation, promotion rules and several lifecycle details remain open for dedicated module discussions.

For technical sequencing and safe parallel work, see [Development Dependencies & Parallel Workstreams](development-dependencies.md).

## Suggested Discussion Order

Tenant Management → Store Settings → Inventory Management → Product Catalog / Discovery → Customer → Cart → Checkout → Order → Payment → Shipping. See [Module Progress](progress.md) for the next discussion. Implementation order follows technical dependencies and approved scope.
