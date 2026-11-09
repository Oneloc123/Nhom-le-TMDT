# Workshop Portal

## Access and account rules

- A user signs in with the existing account. Workshop is represented by ownership of a `print_shops` row (`owner_user_id`); the application does not add a parallel `WORKSHOP` login role.
- A signed-in active customer without an owned shop opens `/workshop/apply` and submits a shop application. The server creates the shop as `verification_status = 'PENDING'`, attaches the authenticated user, and leaves account role and status unchanged.
- An Admin reviews the application in Admin → Seller / Xưởng in. Only a verified, active shop is listed to marketplace customers. After approval, the owner can open `/workshop/dashboard`; login routes an account with an owned shop there.
- Customer accounts cannot access Admin. Admin accounts cannot use the Workshop portal. Every Workshop page/API request refreshes the current account status from the database, and every DAO query checks `shop_id` against the owner ID from the session. A `shopId` query parameter is only a selector and never grants ownership.
- All Workshop POST requests require the existing session CSRF token. The API uses prepared statements and validates IDs, filters, lengths, prices, stock states, and production stage transitions on the server.

## Pages

- `/workshop/apply` — apply for a shop.
- `/workshop/dashboard` — shop scoped metrics, recent orders and quote requests; no estimated or invented revenue.
- `/workshop/profile` — shop name/contact/address, service capabilities, supported file formats, maximum build size, hours, lead time and service notes.
- `/workshop/products` — shop offers and prices, product creation, edits and pause/resume for the shop-specific offer.
- `/workshop/orders` — shop-assigned order lines, details and global order history; add production progress to `production_updates`.
- `/workshop/quotes` — requests specifically addressed to the shop; a quote amount or close reason is saved to the quote row and shown to the customer.
- `/workshop/reviews` — read-only published reviews for the shop.

All pages can be loaded directly or refreshed. Workshop APIs are under `/workshop/api/*` and return JSON. They are protected by `AuthFilter` just like pages.

## Business rules

- `products.is_active` remains independent from `products.moderation_status`. New Workshop products start with `is_active = 1` and `moderation_status = 'PENDING_REVIEW'`; editing shared product content returns moderation to pending without changing `is_active`. A paused `shop_products.is_active` offer is never automatically reactivated by review changes.
- Sellers edit `shop_products` for their own price, material, printing method, minimum quantity, stock state and production days. Shared product content can only be changed if the product is not offered by another shop; such an edit is sent for Admin review again.
- A seller does not change global order/payment/shipping statuses, confirm payment, or issue refunds. Production progress is recorded per owned `order_items` line only after the order is confirmed and the existing payment state is paid or COD. The accepted sequence is enforced by `WorkshopWorkflow` and the DAO transaction.
- Seller quote responses are allowed only for pending quote requests with `preferred_shop_id` set to that seller's shop. There is no automatic price calculation from model files.
- Catalog and checkout queries now require a verified, active shop and an approved, active product/offer.
- No revenue metric is shown because the existing payment records do not define a reliable seller payout/revenue calculation. No deposit or commission feature is included.

## Files and stored model

The portal uses existing tables `users`, `print_shops`, `products`, `shop_products`, `categories`, `orders`, `order_items`, `order_status_history`, `payments`, `production_updates`, `quote_requests`, `reviews`, `addresses`, and `design_files`. It adds shop profile capability fields and a seller response note to the existing tables; it does not add financial tables or a migration runner.

For database setup, see [DATABASE_SETUP.md](DATABASE_SETUP.md). It explains how to inspect the target and run the repeatable schema file manually. No SQL that changes schema or data was run. Earlier read-only smoke checks found `print_shops.contact_email` missing; the current schema file now adds missing Workshop fields when run manually.

## File storage limits

The current codebase has no authenticated file storage service or upload endpoint. The portal displays the stored `design_files.original_filename` to an authorized shop but does not expose `storage_url` as a link or download route. Product images use the existing `product_images` table: an owner may set the primary image URL only when the product is offered by that shop alone; no file is uploaded. Shop logos can also be set to a validated HTTP/HTTPS URL. Add secure upload/download only after the project has an explicit storage location, file size/type policy, and private authorization contract.

## Run and verify

1. Apply the schema changes manually only after following [DATABASE_SETUP.md](DATABASE_SETUP.md), or use the full safe SQL once on a confirmed empty new database.
2. Configure Tomcat with valid `PK_DB_HOST`, `PK_DB_PORT`, `PK_DB_NAME`, `PK_DB_USER`, and `PK_DB_PASSWORD`. Do not store credentials in Git.
3. Build/deploy the WAR. Sign in with an active customer account and visit `/workshop/apply`.
4. Approve the new shop through the existing Admin Seller / Xưởng in screen. Sign in again; the owner should land in Workshop.
5. Verify the shop profile in `/stores`, create a product and ensure it remains hidden until Admin approval, then test its offer and checkout visibility. Test quote responses from both seller and customer pages.
6. Verify order detail access with an order line assigned to the shop and try a different shop ID; the API must not return another shop's data. Test a valid/invalid production stage with the order's actual payment status.

## Optional demo shops

`printkraft_sample_data.sql` includes five development-only 3D workshop accounts and shop profiles along with demo rows for every table. Four shops are marked verified for marketplace testing and one is pending for the Admin queue. It creates normal `CUSTOMER` accounts, never a parallel Workshop role. Run it manually only after the schema file and only in a development database. The seed is additive; existing rows and passwords are not overwritten. All five demo accounts use `Print3D-Demo-2026!`. The script also inserts explicitly marked demo order/payment/review rows and does not create real payment transactions or uploaded design files. Do not use these credentials or data in production.

Unit tests and WAR packaging results for this code change are recorded in `ADMIN_CHANGELOG.md`. Runtime verification against Tomcat and the real DB is separate and has not been claimed unless explicitly recorded there.
