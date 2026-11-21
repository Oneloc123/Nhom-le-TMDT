# Customer return and refund workflow

## Scope

This feature adds a first-class return/refund case tied to the signed-in customer, an order and one order item. The support ticket remains a separate conversation channel; the return/refund case and its history are stored independently, so a ticketing outage does not block a valid case from being created.

## Customer policy implemented in this change

- Requests must be sent no later than 7 days after the relevant delivery/completion timestamp. The deadline is stored on the request as `return_deadline_at` and the request records policy version `PK-RR-2026-11`.
- Customers can request a return-and-refund or refund-only. Requested quantity and value are calculated from the order-item snapshot on the server.
- A customer can request only the quantity not already included in another non-rejected/non-withdrawn case.
- Defective, damaged, wrong-item and missing-item claims require at least one photo. Images are JPEG/PNG only, at most 5 MiB each; a case allows at most 4 images on initial submission and 8 in total.
- Custom-design order items (those linked to a stored design file) can only be submitted for defective, damaged, wrong-item or missing-item reasons, with evidence. This is a project workflow policy, not a statement of statutory rights.
- The customer can withdraw a case while it is still submitted or under review, record return shipment details after approval, upload additional evidence while review is open, and view the case history/status/refund reference.
- Every state transition is written to `return_refund_request_events`; refund attempts are written to `return_refund_transactions`.

## Status lifecycle

- `SUBMITTED` -> `UNDER_REVIEW` -> `REJECTED` or `AWAITING_RETURN_SHIPMENT`.
- Return-and-refund: `AWAITING_RETURN_SHIPMENT` -> `RETURN_SHIPPED` -> `RETURN_RECEIVED` -> `REFUND_PROCESSING`.
- Refund-only: after review and approval, the case enters `REFUND_PROCESSING`.
- `REFUND_PROCESSING` -> `REFUNDED` or `REFUND_FAILED`; a failed attempt can be retried from `REFUND_FAILED` to `REFUND_PROCESSING`.
- `REJECTED`, `WITHDRAWN` and `REFUNDED` are terminal states.

## Admin review and refund settlement

The Admin workspace has a **Đổi trả / Hoàn tiền** page with filtering, case details, evidence, timeline, approve/reject actions, return-received action, and refund-attempt recording.

**Important:** recording `REFUNDED` does not move money. The operator must execute the transfer in the bank/payment provider outside this application, then record the transfer method and its reference. Failed attempts must be recorded as failed with an explanation. The current implementation does not invoke a bank or payment gateway refund API.

## Evidence storage

Evidence is stored outside the web root under `PK_UPLOAD_DIR`; if unset, the storage directory defaults to `~/.printkraft-uploads`. Configure `PK_UPLOAD_DIR` to a private writable directory outside the deployed application. The upload handler validates actual image decoding, JPEG/PNG format, a 5 MiB file limit and a 20 megapixel limit. Evidence is delivered through authenticated Customer/Admin endpoints and is not exposed via a public static URL.

## Database deployment

1. Back up the target MariaDB database and verify the selected schema.
2. Ensure the existing application support schema (`support_tickets` and `support_ticket_messages`) has already been applied; these tables are prerequisites of the existing SupportDao as well as the optional linked conversation.
3. Apply `migrate_customer_returns_refunds.sql` to the existing application database. The migration is additive and creates the request, evidence, status-event and refund-transaction tables.
4. Verify the four new tables with the postflight query at the end of the migration.
5. Deploy the WAR and verify Customer and Admin flows against a test database before production use.

Do not run the migration before selecting the intended database. No refund is automatically issued by deploying this migration.

## Checks

Unit coverage is provided in `ReturnRefundPolicyTest` for delivery deadlines, reason validation, evidence requirements, quantity/value calculations and allowed state transitions. Build and database integration still need to run in the target environment; this change does not claim an end-to-end MariaDB/Tomcat payment test.
