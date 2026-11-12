# Workshop implementation audit

This audit is based on the repository code and documentation inspected for the
`feature/workshop-enterprise-redesign` branch. It is a source-level audit, not a
claim that the deployed Tomcat application or production database has been
runtime-tested.

## Current implementation

| Area | Already present | Remaining work |
|---|---|---|
| Existing Workshop portal | Shop-scoped dashboard, profile/capabilities, product offers and moderation-aware edits, B2B quotes, custom-print pricing, orders, production updates, reviews | Exercise each read/write API against real data; verify empty/error/loading states, pagination/filter combinations, and deployed JSP/static asset paths |
| Messages | `OrderMessageDao` and Workshop API already provide per-order thread listing, thread retrieval/read marking, send, ownership checks, notification, message-length and rate limits | This branch exposes the existing API in a Workshop page; verify unread counts, multi-shop accounts, unauthorized order IDs, and customer/workshop round trips |
| Change requests | Customer order-change workflow, Workshop list/respond API, ownership checks, pending-state checks, response note, price delta and cancellation fee already exist | This branch exposes the workflow in a Workshop page; test each request type and production-stage policy, customer confirmation, withdrawal, expiry, and concurrent responses |
| 3D files | Customer upload currently accepts OBJ only (100 MiB file limit); private random-key storage, SHA-256 metadata, server-side analysis and authorized Workshop download endpoints exist | No Workshop upload/replacement action for revisions; no design version comparison/history; do not advertise STL/3MF/STEP support until upload/parser/security tests exist |
| Capacity and production | Shop-level max build dimensions, max custom-print quantity, capabilities, lead time, offer/pricing-profile production days, production progress and optional ETA exist | No per-machine records, machine availability, shift calendar, capacity reservation, queue, overlap detection or rescheduling workflow |
| Finance | Existing payment/order state is maintained by the central checkout/payment workflow | Seller payout, deposit, commission, refund ownership and reconciliation rules are not sufficiently defined; do not add seller finance totals yet |
| Verification | Unit tests exist for production workflow; an opt-in read-only runtime schema audit exists | Add API/service/UI integration coverage, role and cross-shop negative tests, and a real Tomcat + MariaDB smoke test using the target schema |

## Changes made on this branch

- Added Workshop navigation and page rendering for `/workshop/messages` and
  `/workshop/change-requests`, reusing the existing API/DAO workflows rather
  than introducing duplicate backend tables or endpoints.
- Added message thread list, message history, unread indicator, and send form.
- Added change-request status filter, request details, and accept/reject form
  with price adjustment, cancellation fee and response note.
- Added responsive styles for the new communication screens.
- Added both routes to the Workshop page allow-list.
- Added a JUnit route regression test.
- Updated `docs/WORKSHOP_PORTAL.md` to describe the current UI, file and
  production-capacity boundaries.

## Safety and compatibility constraints

- No database schema or data changes are part of this UI integration.
- The UI uses the existing CSRF-protected Workshop API. Backend ownership
  checks remain authoritative; hiding a control is not treated as access control.
- Workshop users must not confirm payments, issue refunds, or mutate global
  order/payment/shipping status.
- Stored model files remain private. Download authorization is based on the
  authenticated owner, selected shop and request/order relationship.
- Finance work is deferred until central payment and seller settlement rules
  are approved.
- Production scheduling requires an explicit machine/shift/capacity model
  before adding tables or reservation logic.

## Recommended implementation order

1. Run the existing test suite and build on the project's supported Java 21
   environment; fix any baseline failures before adding more functionality.
2. Run Workshop API smoke tests with an active shop owner and a second shop
   account. Verify successful access and cross-shop denial.
3. Add integration tests for messages and change requests, including workflow
   transitions, rate limits, notifications and customer confirmation.
4. Define the supported 3D formats and file lifecycle (upload, replacement,
   revision, retention, deletion, download authorization) before expanding
   upload support.
5. Define printer/machine, technology, build volume, shift, maintenance,
   capacity unit, queue priority and rescheduling rules before implementing
   production scheduling.
6. Agree central payment, commission, deposit, refund and settlement rules
   before building financial dashboards or reconciliation.
7. Complete end-to-end tests across customer, workshop and admin roles.

## Verification status

The GitHub source/API operations in this branch have been inspected and the
new changes were committed to this feature branch. A live application test
against the group's Tomcat instance and MariaDB database has not been run as
part of this audit. Do not treat source inspection or GitHub commit success as
a substitute for that test.
