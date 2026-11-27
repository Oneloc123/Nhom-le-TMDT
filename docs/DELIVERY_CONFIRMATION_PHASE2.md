# Delivery confirmation and GHN synchronization — Phase 2

## Scope

This phase extends Phase 1 with GHN carrier status synchronization and automated receipt confirmation. It does not create GHN shipping orders, print GHN labels, transfer COD, book carrier pickups, calculate final carrier charges, release workshop payouts, or post financial ledger entries. Those are separate carrier-fulfilment / Phase 3 tasks.

The existing GHN integration in this repository calculates shipping fees. Phase 2 adds:
- GHN order-status webhook at `POST /webhooks/ghn`
- webhook idempotency and an audit table
- GHN order-detail polling as a fallback
- in-app shipment notifications
- one receipt reminder approximately 24 hours before the 72-hour deadline
- automatic receipt confirmation after 72 hours, only when GHN has authenticated delivery status and no active dispute exists
- Admin, Workshop and customer tracking metadata for carrier / receipt status

## Database prerequisites

Apply both migrations in order to a backed-up MariaDB schema:

1. `migrate_delivery_confirmation_phase1.sql`
2. `migrate_delivery_confirmation_phase2.sql`

The Phase 2 migration is additive. It extends `shipments` with carrier/provider synchronization fields and creates `carrier_webhook_events`. It does not alter payments, COD balances, settlement entries, workshop balances or payout rows.

The migration marks existing carrier labels matching GHN / Giao Hàng Nhanh as `carrier_provider='GHN'`. Other providers remain `MANUAL` and are not auto-confirmed.

## Runtime environment

Set these environment variables in the Tomcat service/runtime environment. Do not put real credentials in source control, `integrations.properties`, the WAR or GitHub commits.

| Variable | Required | Purpose |
|---|---|---|
| `GHN_ENV` | Yes | `staging` or `production`; defaults to staging in code |
| `GHN_TOKEN` | Yes | GHN API token for the selected environment |
| `GHN_SHOP_ID` | Yes | Numeric GHN shop ID |
| `GHN_WEBHOOK_SECRET` | Yes | Strong, random callback secret configured in GHN's custom-header setting |

Staging and Production use separate GHN accounts/configurations. Configure and verify Staging first, then repeat the webhook and token setup for Production.

## GHN webhook setup

In the GHN Developer Portal:

1. Open Webhook configuration and select the Order tab.
2. Set the callback URL to the publicly reachable HTTPS address:
   `https://<your-domain>/webhooks/ghn`
3. Add the custom header:
   `X-PrintKraft-GHN-Webhook-Token: <the-same-value-as-GHN_WEBHOOK_SECRET>`
4. Enable at least the `switch_status` event. `create` is also accepted. Optional `cod`, `update_cod`, `update_fee`, `update_weight`, `update_payment_type`, and `update_partial_return` events are retained for future reconciliation, but are not financially posted in Phase 2.
5. Enable POD permission when you want GHN to include the Proof-of-Delivery URL.
6. Confirm the selected Shop ID matches `GHN_SHOP_ID`.

The webhook authenticates the custom header using a constant-time comparison and checks the payload's `ShopID`. It accepts POST JSON, caps the body at 64 KB, and returns a 2xx acknowledgement for a processed or duplicate event. Unmatched tracking codes are audited but acknowledged to prevent an endless retry queue. Transient server errors return 5xx so GHN can retry.

GHN documents idempotency as `OrderCode + Type + Time`; the application hashes this tuple into a unique event key. Its callback configuration changes may take time to become active; check the GHN portal before diagnosing an absent callback.

## Linking an app shipment to GHN

The shipment must have:

- carrier label `GHN` or `Giao Hàng Nhanh`
- a real GHN tracking `OrderCode` in the tracking-number field
- `carrier_provider='GHN'`

New Workshop shipments and Admin shipment updates identify GHN by the carrier label. The migration backfills existing rows with matching labels. For existing shipments that use another label but are in fact GHN, update the carrier field through the Admin shipment editor to a GHN label and save.

Do not enter the PrintKraft order code in the tracking-number field unless that exact value is the GHN OrderCode. The webhook is matched against GHN's `OrderCode`, not PrintKraft's `orders.order_code`.

## Fallback polling

A container-managed listener polls up to 80 active GHN shipments every 15 minutes using the GHN Order Info endpoint:

`GET /shiip/public-api/v2/shipping-order/detail?order_code=<GHN OrderCode>`

The call uses `Token` and `ShopId` headers. The application checks that the returned `order_code` matches the shipment's tracking number before applying the response. Poll failures are logged without writing secrets or the tracking number to application logs; the next scheduled pass retries after the polling interval.

Webhook is the primary signal. Polling is a fallback for missed callback delivery and is not a substitute for correct GHN account configuration.

## Receipt reminder and 72-hour automation

The 72-hour window starts from the GHN-authenticated `delivered` event time (or the GHN order-detail `updated_date` observed by fallback polling), not from a Workshop or Admin manual status change.

- At approximately 48 hours, one in-app reminder asks the customer to confirm the parcel or open a dispute.
- At or after 72 hours, the scheduler attempts automatic receipt confirmation.
- Auto-confirmation requires `carrier_provider='GHN'`, a saved `carrier_delivery_verified_at`, `receipt_status='WAITING'`, an expired `receipt_deadline_at`, an order that can still complete, and no active `OPEN` / `UNDER_REVIEW` delivery dispute.
- Auto-confirmation updates shipment receipt fields, writes shipment and order history, and notifies the customer.
- The order becomes `COMPLETED` only when every shipment is either `CONFIRMED` or legacy `NOT_REQUIRED` and no active dispute exists.
- Non-GHN/manual shipments never enter the automatic-confirmation queue.

A dispute prevents auto-confirmation. When Admin resolves/rejects a dispute, the existing Phase 1 workflow restores the shipment to `WAITING`; where GHN delivery proof exists, the 72-hour confirmation window is restarted from the resolution step.

## What Phase 2 does not do

- It does not call GHN Create Order, book a pickup, or generate a shipping label. The existing `GhnShippingService.quote()` is still only a shipping-fee preview.
- It does not process money collected by GHN, credit a COD balance, deduct carrier fees, reconcile COD transfers, hold/release workshop balances, calculate loss compensation, or pay sellers.
- It does not auto-confirm carriers other than GHN.
- It stores COD/fee callbacks as shipment-linked audit events only. Financial posting is deferred to Phase 3.

## Acceptance checklist

### Setup and security

- Run both migrations on a backed-up test schema; check the new columns/indexes and `carrier_webhook_events`.
- Missing or incorrect `GHN_WEBHOOK_SECRET` returns 503/401; incorrect `ShopID` returns 403.
- Invalid JSON, empty payload and payload larger than 64 KB are rejected.
- No webhook mutation is accepted through GET.
- Confirm the callback is served over HTTPS and the secret is absent from logs, JSP, Git and the WAR.

### Webhook processing

- Send a valid GHN `switch_status` payload; verify shipment status, event audit, timeline event and customer notification.
- Replay the exact same `OrderCode + Type + Time`; verify the second request is acknowledged as duplicate and does not create another shipment event/notification.
- Send an older event after a newer event; verify the old status is ignored.
- Send events for another Shop ID / an unknown OrderCode; verify no shipment is mutated.
- Verify final GHN states `delivered`, `returned`, `cancel`, `exception`, `lost`, `damage`, and `scrap` cannot be reopened by later callbacks.
- Verify a late authenticated GHN `delivered` event enriches a manually declared DELIVERED shipment with proof/deadline without resetting an already confirmed or disputed receipt.
- Verify COD/fee/weight callbacks link to the correct shipment audit record and do not create financial ledger entries.

### Polling fallback

- A GHN shipment in a non-terminal state is selected after the 15-minute interval.
- The lookup uses GHN Order Info with Token/ShopId and the actual GHN OrderCode.
- A mismatched returned OrderCode is rejected.
- Repeated poll responses are deduplicated against webhook events when they share the same event timestamp.
- A lookup timeout or GHN non-200 response does not alter shipment status.

### Reminder and automatic confirmation

- GHN delivered event sets `carrier_delivery_verified_at` and deadline to event time + 72 hours.
- Verify one reminder is generated when the remaining window is approximately 24 hours.
- Before the deadline, status remains WAITING and the order is not completed.
- At/after the deadline, no dispute + verified GHN proof auto-confirms the shipment.
- A customer-confirmed, disputed, returned, failed-final, cancelled-order or non-GHN shipment is never auto-confirmed by the scheduler.
- One dispute on any shipment blocks auto-confirmation / order completion for the order.
- For multi-shop orders, order completion waits until all shipment receipts are confirmed or legacy-not-required.

### Regression and build

- Run `./mvnw --batch-mode --no-transfer-progress clean verify` with Java 21.
- Run `node --check src/main/webapp/js/order-tracking.js`, `node --check src/main/webapp/workshop-ui/workshop.js`, and the existing Workshop/Admin UI smoke tests.
- Re-test customer tracking, public tracking share, workshop shipment handoff, Admin shipment update, delivery dispute queue and return/refund flows.

## Operational notes

The periodic listener runs inside the Tomcat web application. In a multi-instance deployment, every instance would run the same scheduled tasks; database row locks and conditional updates protect state transitions, but running multiple schedulers can still create unnecessary GHN polling traffic. Run one application instance for this phase, or move these scheduled tasks to a dedicated single worker before horizontal scaling.
