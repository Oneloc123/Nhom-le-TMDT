# Admin operations and integration acceptance

This checklist covers the Admin operational and finance changes. Automated unit tests are safe for normal CI. Database integration tests are opt-in and must run only against a disposable schema.

## 1. Prepare a dedicated integration database

1. Back up any data first. Create or restore a dedicated schema named with the suffix _test or _integration, for example printkraft_test.
2. Set PK_DB_HOST, PK_DB_PORT, PK_DB_NAME=printkraft_test, PK_DB_USER and PK_DB_PASSWORD in the test process. Never commit credentials.
3. Apply the relevant schema scripts in the order documented in docs/ADMIN_STABILITY.md; then run verify_admin_schema.sql and save its output.
4. Use disposable fixture IDs only. The evidence check requires an existing evidence row and three IDs:
   - pk.adminIntegrationEvidenceId: evidence attached to a fixture return/refund request.
   - pk.adminIntegrationOwnerUserId: the customer who owns that request.
   - pk.adminIntegrationOtherUserId: a different customer.
5. The notification durability test also needs pk.adminIntegrationUserId, an existing disposable user in that schema.

## 2. Automated checks

Normal CI runs ./mvnw --batch-mode --no-transfer-progress clean verify, parses all Admin ES modules with Node, and runs the Workshop UI smoke tests. Policy unit tests cover valid and invalid transitions, SLA calculations, seller verification reasons, manual-refund inputs, Admin role decisions, and CSRF token matching.

The database integration tests are disabled by default. Enable them only for the dedicated schema by running Maven with the system property pk.adminIntegration=true and the fixture IDs listed above. Set PK_DB_NAME to a schema ending in _test or _integration before running:

    ./mvnw --batch-mode -Dpk.adminIntegration=true -Dpk.adminIntegrationUserId=9001 -Dpk.adminIntegrationEvidenceId=1201 -Dpk.adminIntegrationOwnerUserId=9002 -Dpk.adminIntegrationOtherUserId=9003 test

Replace all example IDs with disposable fixture IDs. The durability test creates a uniquely tagged notification, reads it through a separate JDBC connection after the write connection has closed, then deletes only that tagged row. It asserts that the owner can resolve evidence metadata, a different customer cannot, and the Admin DAO can resolve the same evidence for its protected endpoint. Tests skip if the database name is not suffixed _test or _integration.

## 3. Manual role and CSRF matrix

Use an active Admin session and a normal Customer session. Do not use production accounts to test destructive transitions.

| Scenario | Request | Expected result |
|---|---|---|
| Anonymous browser | GET /admin/shipments | Redirect to login |
| Anonymous API client | GET /admin/api/shipments | Unauthenticated JSON response; no data |
| Customer session | GET /admin/api/shipments | HTTP 403 |
| Active Admin session | GET /admin/api/shipments | HTTP 200 when migrations are applied |
| Active Admin, no CSRF token | POST /admin/api/shipments/{id}/update | HTTP 403; no row/history/notification change |
| Active Admin, wrong CSRF token | Same POST with invalid X-CSRF-Token | HTTP 403; no mutation |
| Active Admin, matching CSRF token | Valid POST with matching session token | Request reaches validation and commits only if the transition is valid |
| Locked/deleted account | Repeat an Admin request after changing account state | Session invalidated or denied; no privileged read/mutation |

The ticket JSP POST must include csrfToken, and Admin SPA POST requests must send the session token via X-CSRF-Token. CSRF failure must not create a ticket event, shipment event, audit record or notification.

## 4. Business transition matrix

### Shipment
- PREPARING to SHIPPED to IN_TRANSIT to DELIVERED is allowed; FAILED may move back to SHIPPED/IN_TRANSIT after a real retry. DELIVERED and RETURNED cannot reopen to another status.
- Attempt PREPARING to DELIVERED, DELIVERED to IN_TRANSIT, or an unknown status. The API must return HTTP 400 and must not write shipment, order, event or notification changes.
- Attempt SHIPPED, IN_TRANSIT or DELIVERED with a blank carrier or tracking number; the API must reject it. A valid change must persist carrier, tracking_number, shipped_at/delivered_at, aggregate orders.shipping_status, one shipment_events row and a customer notification in the same transaction.

### Seller verification
- From PENDING, test APPROVE, REJECT and REQUEST_INFO, each with a reason of at least three characters.
- Blank/too-long reasons and unsupported actions must return HTTP 400. A second decision after VERIFIED or REJECTED must be rejected.
- Each successful decision writes seller_verification_events. APPROVE sets VERIFIED, REJECT sets REJECTED, REQUEST_INFO leaves the shop PENDING; the owner receives a notification explaining the result and reason.

### Support tickets and SLA
- Create tickets with HIGH, NORMAL and LOW priority. First-response deadlines should be about 4, 24 and 72 hours from creation.
- Move OPEN/IN_PROGRESS to WAITING_CUSTOMER; check sla_remaining_seconds and sla_paused_at are saved. A customer reply resumes the remaining SLA if an Admin first response is still pending.
- Send a public Admin reply; check first_response_at is recorded, the first-response SLA stops, the message is persisted and the customer receives a notification.
- Move a ticket to RESOLVED/CLOSED, then reopen within seven days. A new first-response window must be assigned. Confirm each status/priority/assignee change writes support_ticket_events, and SLA filters OVERDUE, DUE_SOON, PAUSED, ON_TRACK and RESPONDED return the expected queue.

### Manual refunds
- Only a payment in REFUND_REQUIRED may be completed using the manual-refund action. PAID, PENDING, FAILED or already REFUNDED payments must be rejected.
- SUCCESS requires the operator to confirm an actual external transfer first, enter a supported method, a 3–120-character reference, a 3–1000-character note, and the full payment amount. Partial refunds through this Admin action are not supported.
- On success, check admin_manual_refunds, payment status REFUNDED, any eligible cancelled-order aggregate status, a RECONCILED event and customer notification are committed together. Submit the same request again; duplicate success must be rejected.
- Record a failed attempt with method and reason. Check the FAILED ledger record and FOLLOW_UP reconciliation event are saved, the customer is notified, and the original REFUND_REQUIRED obligation remains open so it can be retried.
- Existing return/refund requests enforce the same required note/reference/amount rules. An external transfer is never initiated by this application.

## 5. Durable data and evidence-download authorization

- After creating a notification, shipment event, seller-verification decision, ticket event/message or refund record, close the current page/session and reconnect. The row must remain in the database and render after a new read.
- With an Admin session, request GET /admin/api/return-refund-evidence/{evidenceId}. The evidence must be resolved through ReturnRefundEvidenceStore; raw storage keys and file-system paths must never be accepted from the browser.
- A Customer must receive 403/404 from the Admin evidence route. On GET /return-refunds/evidence?evidenceId={evidenceId}, the owning Customer can download/view evidence; a different Customer must receive 404. Test both with the dedicated fixture IDs above.
- Inspect the evidence storage location to confirm it is outside the public web root. Never expose a direct static URL to evidence files.
- Trigger a database error during an update in the integration schema. Confirm the shipment/payment/ticket state, audit event and notification either all commit or all roll back; partial event/notification writes are not accepted.

## 6. Release sign-off

Record the deployed commit SHA, WAR build result, migration version, verify_admin_schema.sql output, CI run URL, Admin role/CSRF checks, transition tests, notification/persistence checks and evidence authorization checks. Do not mark an environment accepted based only on source inspection; attach output captured from the target environment.
