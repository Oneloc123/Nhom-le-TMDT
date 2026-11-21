# Admin stability and route audit

## Canonical routes

All SPA routes except support tickets are handled by `AdminRouterServlet` and render `/WEB-INF/admin/index.jsp`. The exact `/admin/users` legacy servlet and its old JSP have been removed, so the SPA and API own the user-management URL.

| Menu URL | Page entry | Data/API route |
|---|---|---|
| `/admin` | `AdminRouterServlet` → `index.jsp` | `/admin/api/dashboard` |
| `/admin/users` | `AdminRouterServlet` → `index.jsp` | `/admin/api/users` |
| `/admin/sellers` | `AdminRouterServlet` → `index.jsp` | `/admin/api/sellers` |
| `/admin/products` | `AdminRouterServlet` → `index.jsp` | `/admin/api/products` |
| `/admin/orders` | `AdminRouterServlet` → `index.jsp` | `/admin/api/orders` |
| `/admin/orders?view=quotes` | `AdminRouterServlet` → `index.jsp` | `/admin/api/quotes` |
| `/admin/statistics` | `AdminRouterServlet` → `index.jsp` | `/admin/api/statistics` |
| `/admin/custom-prints` | SPA | `/admin/api/custom-prints` |
| `/admin/order-changes` | SPA | `/admin/api/order-changes` |
| `/admin/reviews` | SPA | `/admin/api/reviews` |
| `/admin/reconciliation` | SPA | `/admin/api/reconciliation` |
| `/admin/tickets` | `AdminTicketServlet` → `/WEB-INF/admin/tickets.jsp` | `SupportDao`; separate ticket UI intentionally retained |
| `/admin/returns` | `AdminRouterServlet` → `index.jsp` | `/admin/api/return-refunds` |

The Admin API is mapped under `/admin/api/*`. Existing authentication and CSRF enforcement must remain enabled for mutations.

## Database rollout

1. Back up the target database. On the same connection that will run migration, execute `SELECT VERSION() AS server_version, DATABASE() AS selected_database;` and confirm the target.
2. For an existing database, apply `migrate_20261117_persistent_notifications_support_attachments.sql`; do not import the complete bootstrap schema over a database in use.
3. Run read-only `verify_admin_schema.sql` and retain its output. `CREATE TABLE IF NOT EXISTS` does not repair an existing table with a wrong definition.
4. Run `migrate_customer_returns_refunds.sql` separately for return/refund workflows. The custom-print migration in `docs/CUSTOM_PRINT_WORKFLOW_MIGRATION_EXISTING_MARIADB_10_4.sql` has staged preflight requirements; follow the document and stop on a failed preflight.
5. Deploy the WAR and verify each route in the table using an authenticated Admin session.

## Known schema boundaries

- Statistics hides custom-print, order-change, review, or reconciliation queues until the corresponding business table and audit log exist.
- Custom-print requests require the separately staged custom-print workflow migration; follow its preflight and run stages one by one.
- Admin review of custom-print requests is an audit workflow only; quote pricing and quote responses remain the workshop's responsibility.
- Rejection of an order-change request is allowed only while the request is `PENDING`; Admin review does not silently apply changes to orders.
- Payment reconciliation records a note/outcome/reference only. It does not change payment/order status and does not execute a refund.
- Repository inspection is not proof that a database migration has run. Keep the output from the target database as rollout evidence.


## Admin business queues

After backup and verifying the selected schema, run `migrate_admin_business_queues.sql`. It creates `order_change_requests` when absent and audit tables for custom-print review, order-change review, review moderation, and payment reconciliation. It does not change existing order/payment states.

Custom-print requests require `docs/CUSTOM_PRINT_WORKFLOW_MIGRATION_EXISTING_MARIADB_10_4.sql`, which is a staged migration with required preflight checks. Read the instructions and stop on a failed stage. After the migrations, run `verify_admin_schema.sql` and confirm the new queue cards appear in Statistics.

Payment reconciliation in Admin only records an operator's outcome, note and optional provider reference. It does not mark a payment as paid/refunded, change the order status, or initiate a transfer.


## Admin operations and acceptance tests

Run `migrate_admin_operations_sla_manual_refunds.sql` only after taking a backup and selecting the intended database. It adds shipment-event linkage, seller verification decisions, manual-refund ledger, support-ticket audit events and SLA fields. Run `verify_admin_schema.sql` afterwards and keep the result with the release evidence.

See [ADMIN_OPERATIONS_ACCEPTANCE.md](ADMIN_OPERATIONS_ACCEPTANCE.md) for the role/CSRF matrix, shipment transitions, seller decision reasons, support SLA cases, manual refund reconciliation and evidence-download authorization. Database integration tests are opt-in and must use a disposable schema ending in `_test` or `_integration`; never run them against production.
