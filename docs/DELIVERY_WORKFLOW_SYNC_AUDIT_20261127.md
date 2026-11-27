# Delivery workflow synchronization audit — 2026-11-27

Branch: `feature/delivery-confirmation-phase1-20261127`

## Canonical ownership

- `shipments.status` is the state of an individual parcel.
- Workshop owns handoff for its own parcel and may declare delivery only for non-GHN/manual carriers.
- GHN webhook/polling is the authority for carrier-confirmed GHN delivery. A GHN OrderCode must be the actual code issued by GHN, not the PrintKraft order code.
- `shipments.receipt_status` tracks the customer's receipt confirmation/dispute independently from carrier status.
- `orders.shipping_status` is an aggregate over all shipments and uses the shared Vietnamese labels defined in `OrderStatus`.
- `orders.status` is the order lifecycle; Workshop must not mark the entire order shipped until all order items are handed off.

## Changes in this branch

1. Workshop shipment handoff is scoped to the exact `order_id + shop_id`; it no longer reuses a legacy shipment with no shop owner.
2. Workshop cannot overwrite carrier/tracking data after a shipment has been handed off or reached a terminal state. A retry of a failed shipment is allowed, but active disputes block it.
3. Starting a retry clears stale carrier proof, deadline/reminder, and receipt-confirmation data so the parcel does not inherit the previous delivery cycle.
4. Workshop delivery declaration is scoped to eligible shipments owned by that workshop, and is blocked for GHN shipments. Manual delivery does not create GHN proof or a 72-hour deadline.
5. Workshop updates only its own order-item rows. The order-level `SHIPPED` status is set only when all items have been handed off.
6. GHN carrier labels require an actual GHN OrderCode at Workshop handoff; the default PrintKraft tracking code cannot be used for GHN.
7. Admin cannot manually declare a GHN shipment delivered. Status restarts clear stale receipt fields, and shipment changes are blocked while a dispute is active.
8. Admin, Workshop, and GHN use the same aggregate shipping labels: preparing, in transit, delivered, failed, and returned.

## Manual acceptance tests

Use a backed-up development database with Phase 1 and Phase 2 migrations applied.

### Workshop ownership and handoff

- [ ] A Workshop cannot access/update another shop's shipment.
- [ ] A shipment already `SHIPPED`, `IN_TRANSIT`, `DELIVERED`, or `RETURNED` cannot be handed off again or have its tracking code replaced from Workshop.
- [ ] A `FAILED` manual shipment may be retried when no active dispute exists; receipt/proof/deadline fields are reset.
- [ ] A shipment with an `OPEN` or `UNDER_REVIEW` dispute cannot be restarted.
- [ ] When one shop ships a multi-shop order, only its order-item rows change. The overall order remains processing until all order items have been handed off.
- [ ] GHN handoff with no tracking code is rejected. GHN handoff with a PrintKraft fallback code is never silently generated.

### Manual delivery

- [ ] A Workshop can declare delivered only its eligible manual-carrier parcel(s).
- [ ] GHN shipments cannot be declared delivered by Workshop or Admin.
- [ ] A manual delivery sets shipment status to `DELIVERED` and receipt to `WAITING`; it does not set carrier proof/deadline.
- [ ] Each changed shipment gets its own timeline event. The aggregate order shipping status is derived from every shipment, not a hard-coded value.

### GHN synchronization

- [ ] A callback with wrong token or ShopID is rejected.
- [ ] A callback OrderCode must exactly match a GHN shipment tracking number.
- [ ] Duplicate `OrderCode + Type + Time` is acknowledged without duplicate state/events/notifications.
- [ ] An older callback cannot overwrite a newer carrier update.
- [ ] A valid GHN `delivered` event sets carrier proof, `WAITING`, and the deadline based on carrier event time + 72 hours.
- [ ] GHN polling uses the actual GHN OrderCode and rejects mismatched order-info responses.
- [ ] Manual/legacy delivery without GHN proof cannot be auto-confirmed.

### Customer and completion

- [ ] Customer can confirm or dispute only a delivered shipment owned by the customer's order and in receipt state `WAITING`.
- [ ] A GHN shipment with manually declared `DELIVERED` but no `carrier_delivery_verified_at` cannot show receipt/dispute actions and backend rejects both POST actions; manual carriers retain their existing flow.
- [ ] Confirming one parcel does not complete an order with other parcels awaiting delivery/confirmation.
- [ ] Any active delivery dispute blocks order completion and automatic confirmation.
- [ ] Resolving/rejecting a dispute restores the shipment to `WAITING`; the customer can confirm afterward.
- [ ] The order completes only when all delivered shipments are `CONFIRMED` or legacy `NOT_REQUIRED`, and no active dispute exists.

## Build verification

CI runs `./mvnw --batch-mode --no-transfer-progress clean verify`, Workshop/Admin JS syntax checks, and Workshop UI smoke tests. Confirm the latest run on this branch is green before deploying.
