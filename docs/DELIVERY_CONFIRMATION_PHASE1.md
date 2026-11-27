# Delivery confirmation and dispute workflow — Phase 1

## Scope

This branch implements Phase 1 only. It does not call any carrier API, process carrier webhooks, run a 72-hour auto-completion job, reconcile COD, or change payout/settlement behavior.

The implementation reuses the existing `shipments`, `shipment_events`, `orders`, `order_status_history`, customer tracking page, Admin SPA/API, and notification service.

## Business rules

1. A shipment remains in the existing shipping state machine: `PREPARING`, `SHIPPED`, `IN_TRANSIT`, `DELIVERED`, `FAILED`, `RETURNED`.
2. When Admin manually changes a shipment from a non-delivered status to `DELIVERED`, the system sets `receipt_status='WAITING'`. This is a manual delivery declaration, not carrier-confirmed proof.
3. The customer can confirm only a shipment owned by their order, only after it is `DELIVERED`, and only while its receipt status is `WAITING`.
4. The customer can report `NOT_RECEIVED`, `DAMAGED`, `MISSING`, `WRONG_ITEM`, or `OTHER` while receipt confirmation is waiting. An active dispute sets the receipt status to `DISPUTED`.
5. Disputes have the states `OPEN`, `UNDER_REVIEW`, `RESOLVED`, and `REJECTED`. Every transition writes `delivery_dispute_events` and a shipment timeline event. Customer notifications are written in the same transaction.
6. An `OPEN` or `UNDER_REVIEW` dispute blocks both customer receipt confirmation and order completion.
7. When Admin resolves or rejects a dispute, the related shipment returns to `WAITING` for customer confirmation. This does not automatically complete the order.
8. A physical order can become `COMPLETED` only after all its shipments are `DELIVERED` and receipt state is `CONFIRMED` or `NOT_REQUIRED`, and no active dispute remains. Existing orders with no shipment records retain their previous completion path.
9. Old already-delivered shipments are marked `NOT_REQUIRED` by migration so historical orders do not unexpectedly require new customer actions.

## Database migration

Back up the target MariaDB database first, select the existing PrintKraft schema, then execute:

```sql
SOURCE migrate_delivery_confirmation_phase1.sql;
```

The migration is additive. It adds receipt-confirmation fields to `shipments` and creates:

- `delivery_disputes`
- `delivery_dispute_events`

It does not change existing order/payment rows or drop existing data. Verify the postflight table list and the `shipments.receipt_status` column before deploying the WAR.

## Customer workflow

Open `/order-tracking?code={orderCode}`.

For each shipment at `WAITING`, the customer can:

- Confirm receipt.
- Report not received.
- Report damaged, missing, or wrong item.
- Describe another delivery issue.

The customer POST actions are handled by `TrackingServlet` and are protected by the existing login and CSRF filters. The DAO independently checks that the shipment belongs to the authenticated customer.

## Admin workflow

Open `/admin/delivery-disputes`.

Admin can filter by status, open a dispute detail, move an `OPEN` case to `UNDER_REVIEW`, then resolve or reject it with a required note. The Admin API is:

- `GET /admin/api/delivery-disputes?status=OPEN&q=...`
- `GET /admin/api/delivery-disputes/{id}`
- `POST /admin/api/delivery-disputes/{id}/action` with `action=REVIEW|RESOLVE|REJECT` and `note`

The existing Admin authentication and CSRF checks apply to these paths.

## Acceptance tests

### Database and compatibility

- Run the migration twice in a development database; the second run must be safe.
- Verify old `DELIVERED` shipment rows are `NOT_REQUIRED`.
- Verify shipment events and existing order/payment rows remain unchanged by the migration.

### Shipment transition

- Change a test shipment from `IN_TRANSIT` to `DELIVERED`; verify receipt state becomes `WAITING`.
- Verify shipment event and customer notification are recorded.
- Attempt to mark an already-delivered shipment as in transit; the existing ShipmentPolicy must reject it.

### Customer confirmation

- The owner confirms a `WAITING` shipment; verify `CONFIRMED`, timestamp, actor, and shipment timeline event.
- A different customer attempts to confirm the shipment; request must fail.
- Confirming a shipment that is not delivered or is already confirmed must fail.
- For an order with multiple shipments, confirm only one; the order must not complete until the other eligible shipments are also confirmed.

### Customer dispute

- Submit each supported issue type with a valid description; verify a dispute, dispute event, shipment event, and notification.
- Submit an issue for an unrelated shipment or an already-confirmed shipment; request must fail.
- Attempt a second active dispute for the same shipment; request must fail.
- Verify an active dispute prevents customer confirmation and both Admin/customer order completion.

### Admin review

- Verify unauthenticated and non-Admin access is rejected by existing filters.
- Verify Admin can read the queue and detail.
- Verify `OPEN -> UNDER_REVIEW -> RESOLVED` and `OPEN -> REJECTED` transitions.
- Verify invalid transitions and short/missing notes are rejected.
- Verify resolving/rejecting resets the shipment to `WAITING`, retains all history, and notifies the customer.
- Verify the order remains incomplete until the customer confirms all shipments.

### Regression

- Verify existing order tracking, public share links, shipment updates, return/refund pages, and payment flows remain functional.
- Verify return/refund history remains separate from delivery-dispute history.
- Run `mvn test` and `mvn package` against the repository's configured Java 21 environment before merging.

## Current implementation boundary

- Carrier delivery status is still entered manually in Admin in this phase.
- Complaint evidence is currently text-based; evidence file uploads can be added in a subsequent iteration if required.
- The three-day auto-completion policy and all financial hold/release/COD reconciliation features are intentionally deferred to Phases 2 and 3.
