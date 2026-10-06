# School onboarding and lifecycle

The backend supports verified signup, school profile and branding uploads, independent review, branch opening,
and suspension. JSON bodies, responses, query parameters, and Bruno variables use `snake_case`.
School profile updates use JSON; logo and cover uploads use multipart form data.

## Local walkthrough

1. Run `docker compose up -d --wait`, then `./gradlew bootRun` with JDK 25. SMTP is available on
   `localhost:1025`; read verification mail at [Mailpit](http://localhost:8025).
2. Open the repository's `bruno` folder in Bruno and select **Local**. Set `auth_email`, `auth_display_name`,
   and the `auth_password` secret, then run **Authentication → Register**. It saves the access and refresh
   tokens in memory. Registration creates an active, unverified person without school or platform grants.
3. Run **Email verification → Send verification email**. Open its email link and click **Verify email**.
   Alternatively copy the link's token into the `verification_token` secret and run **Confirm email**.
   Use one confirmation method: the token is single-use. Verification is read from the database, so the
   existing access token can immediately register a school without logging in again.
4. Set `school_display_name`, an unused `workspace_slug`, and absolute paths in `logo_file` and `cover_file`.
   In **School onboarding**, register the school, get the checklist, update the profile, upload both images,
   configure the default branch location, and claim the workspace. Registration automatically grants the
   creator an accepted `school_owner` role and creates a draft default branch in the same transaction.
5. Refresh the checklist. When `missing_fields` is empty and `can_submit` is true, submit for review.
6. An independent super admin selects the school, reads **School review → Reviews**, and chooses **Approve**,
   **Request changes**, or **Reject**. A changes request requires a reason; the owner edits and resubmits.
   Each resubmission creates a new `review_id`. The reviewer must reload and examine the new snapshot.
7. After approval, run **Branches and workspaces → Open branch** for the default branch. Resolve its workspace,
   then invite instructors and students through the existing **Members** requests.
8. Additional branches start as drafts. Give each its own complete location and unique workspace before
   opening it. They do not require another school review. The Add branch request selects the new `branch_id`;
   change `workspace_slug` before claiming a name for it.

**Admin authentication prerequisite:** review, suspension, reinstatement, and the admin directory require a
super-admin grant, enrolled MFA, and a session whose signed claim confirms MFA. This repository's ordinary
password login deliberately returns `MFA_REQUIRED` for enrolled accounts; an MFA enrollment/challenge/login
API is still missing. Set `admin_access_token` only from a trusted MFA-capable authentication flow. The new
review APIs enforce this requirement; there is no development bypass. Integration tests provision the
reviewer and sign its MFA claim inside the test fixture only.

Bruno preserves the owner's `access_token` while review requests use `admin_access_token`. Approval,
changes requested, and rejection are alternative actions. The collection is a manual stateful workflow,
not a script to run every request in order. Successful mutations capture school, branch, image and review IDs.

## State rules

| Current status | Action | Result and conditions |
|---|---|---|
| No school | Register | Verified active person; creates `draft`, owner grant and default branch |
| `draft` / `changes_requested` | Submit | Both images ready, default branch address/province/coordinates and workspace complete; creates `pending_review` and snapshot |
| `pending_review` | Edit/upload/rename workspace | Refused; profile, default branch and workspace are locked |
| `pending_review` | Approve | Independent MFA-verified super admin; becomes `approved` |
| `pending_review` | Request changes | Nonblank reason; becomes `changes_requested`, owner can edit and resubmit |
| `pending_review` | Reject | Nonblank reason; becomes `rejected`, terminal for this school record |
| `approved` | Open branch | Complete location and current workspace; branch becomes `active` |
| `approved` | Suspend | MFA-verified super admin and reason; becomes `suspended` |
| `suspended` | Reinstate | MFA-verified super admin; restores `approved` and clears suspension reason |

A reviewer cannot be the creator or hold any school-scoped grant, including pending or historical grants.
The request includes the current pending `review_id`, preventing a decision based on an earlier submission.
The database records the decision, reviewer, reason and immutable submitted snapshot atomically.
Rejection cannot be reversed with the reinstatement endpoint. An owner can still read their school summaries
and decision reasons through `/api/me/schools`, including when school access is suspended.

Approved identity fields (`display_name`, `legal_name`, `registration_no`) remain fixed. Description, contact
information and branding can be edited after approval. Profile PUT replaces all editable profile fields:
omitted optional fields become null. Branch profile PUT also supplies a complete location.

Workspace names use 3–63 lowercase letters/digits/hyphens, with no leading/trailing or repeated hyphens.
Reserved names are refused. A name is unique forever: renaming retires the old row, then inserts a new name.
Retired aliases resolve to the current canonical name only while the school is approved and the branch active.
Suspension makes every workspace return 404, blocks members' school access, and blocks image delivery.
Reinstatement restores visibility for branches that were already active.

## API reference

All paths below begin with `/api`. Protected requests use a bearer access token. JSON responses use the usual
`data` / `error` envelope. The verification browser page returns HTML; image downloads return binary bytes.

| Method | Path | Purpose |
|---|---|---|
| POST | `/me/email-verification` | Send/resend a link for the authenticated unverified person |
| POST | `/auth/verify-email` | Public JSON body `{ "token": "…" }`; confirm email |
| GET | `/auth/verify-email?token=…` | Public browser confirmation form; GET does not consume the token |
| POST | `/auth/verify-email` | Public form-urlencoded `token`; confirms and returns HTML |
| POST | `/schools` | Register school profile; 201 |
| GET | `/me/schools?page=0&size=20` | Creator's school status summaries |
| GET | `/schools/{school_id}` | School profile; approved profiles are public |
| PUT | `/schools/{school_id}/profile` | Replace editable school profile |
| GET | `/schools/{school_id}/onboarding` | Owner/editor checklist and default branch |
| POST | `/schools/{school_id}/logo` | Multipart `file`; process and attach logo; 201 |
| POST | `/schools/{school_id}/cover` | Multipart `file`; process and attach cover; 201 |
| GET | `/schools/{school_id}/media/{asset_id}/content` | Authorized image bytes, including public approved branding |
| PUT | `/schools/{school_id}/default-branch` | Configure default branch profile and location |
| POST | `/schools/{school_id}/submit` | Validate completeness and submit snapshot |
| GET | `/schools/{school_id}/reviews` | Owner/editor or reviewer reads history; not public |
| GET | `/admin/schools?status=pending_review&page=0&size=20` | MFA-protected directory; status is optional |
| POST | `/admin/schools/{school_id}/review` | `{ "review_id": "…", "decision": "approved\|changes_requested\|rejected", "reason": "…" }` |
| POST | `/admin/schools/{school_id}/suspend` | `{ "reason": "…" }` |
| POST | `/admin/schools/{school_id}/reinstate` | Restore suspended school |
| GET | `/schools/{school_id}/branches` | List branches visible to caller |
| POST | `/schools/{school_id}/branches` | Create additional draft branch after school approval; 201 |
| PUT | `/schools/{school_id}/branches/{branch_id}` | Replace branch profile/location |
| POST | `/schools/{school_id}/branches/{branch_id}/open` | Open complete branch in approved school |
| PUT | `/schools/{school_id}/branches/{branch_id}/workspace` | Claim/rename with `{ "slug": "example-school" }` |
| GET | `/workspaces/{slug}` | Public resolution to `school_id`, `branch_id`, `canonical_slug` |

Example school profile:

```json
{
  "display_name": "Example School",
  "legal_name": null,
  "registration_no": null,
  "description": "A community school in Phnom Penh.",
  "contact_email": "school@example.com",
  "contact_phone": null
}
```

Example branch profile:

```json
{
  "name": "Main branch",
  "address_line": "12 Example Street",
  "province": "Phnom Penh",
  "district": "Daun Penh",
  "country_code": "KH",
  "latitude": 11.5564,
  "longitude": 104.9282,
  "timezone": "Asia/Phnom_Penh"
}
```

Upload example (set the shell variables locally):

```bash
curl --fail-with-body -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "file=@/absolute/path/logo.png" \
  "http://localhost:8080/api/schools/$SCHOOL_ID/logo"
```

The response includes `id`, `school_id`, `purpose`, `status`, `mime_type`, `byte_size`, `width`, `height`,
`sha256`, and `content_url`. Clients never submit storage keys, processing status, dimensions or hashes.

## Email delivery

| Environment variable | Local default | Deployment setting |
|---|---|---|
| `SMTP_HOST` | `localhost` | SMTP provider hostname |
| `SMTP_PORT` | `1025` | Provider's submission port, commonly 587 |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | empty | Provider credentials |
| `SMTP_AUTH` | `false` | `true` for authenticated SMTP |
| `SMTP_STARTTLS` | `false` | `true` enables and requires STARTTLS |
| `VERIFICATION_FROM` | `no-reply@localhost` | Provider-verified sender address |
| `VERIFICATION_URL` | `http://localhost:8080/api/auth/verify-email` | Public HTTPS confirmation URL |

The backend supplies the confirmation page, so no frontend is required for local verification. If pointing
`VERIFICATION_URL` at a frontend instead, it must read `token` and POST it to `/api/auth/verify-email` after
user confirmation. SMTP calls have five-second connection/read/write timeouts and run outside database
transactions. The local Mailpit service captures messages; it does not deliver them to real inboxes.

Tokens contain 256 random bits, are stored only as SHA-256 hashes, expire after 24 hours, and become unusable
when consumed or superseded. Resends have a 60-second per-account cooldown. Failed SMTP sends return 503
and invalidate the newly created token so the user can retry. Opening an email link does not verify the
account until the confirmation button is submitted, avoiding consumption by ordinary link scanners.
Do not log verification URL query strings at your reverse proxy, or log raw tokens. The response includes
no token; the recipient gets it only by email. Edge throttling is still needed for public authentication APIs.

## Image processing and storage

The synchronous pipeline validates actual JPEG/PNG contents, checks dimensions before decoding, and re-encodes
the image to strip metadata and trailing payload. It allows at most 10 MiB input, 8192 pixels per side and
12 million total pixels. Canonical output is capped at 20 MiB. GIF, SVG, WebP and AVIF are not accepted by this
pipeline. It is image validation and normalization, not a malware-scanning service.

Processing follows `pending → processing → ready`; a seeded service identity alone records trusted metadata
and completes processing. A successfully stored canonical object is attached to the school in the same
transaction that marks it ready. Object I/O occurs outside transactions. Finalization reacquires the school
lock and rechecks edit access, so a concurrent submission cannot change the reviewed branding. Failures
attempt to remove the new object and mark its row failed; the previous attached image stays intact.

Keep the storage bucket private. Image endpoints check access on every request and send `Cache-Control:
no-store`; they do not expose presigned URLs that remain usable after suspension. Downloads also verify the
stored size and SHA-256. Bytes already downloaded by a client cannot be recalled. Previously uploaded ready
assets remain stored; there is no retention sweep yet. A process crash between storage and database finalization
can leave a processing row or orphan object requiring reconciliation. Cleanup failures need operational attention.

## Schema and verification

`V1_0_12__SCHOOL_ONBOARDING.sql` adds email verification tokens, the image processing service identity, review
read permissions, and profile/contact fields in the existing review snapshot. Existing school, branch,
workspace, media and review guards remain authoritative. User-facing mutations lock the school row before
checking lifecycle status, serializing submission, review, profile edits, branch edits and media finalization.

Use a disposable database: these integration tests migrate it and create test accounts, schools and reviews.
The onboarding suite sends only to local SMTP and uses a dedicated `krob-krong-onboarding-it` bucket, whose
objects it deletes after the suite. Do not point the suite at production or share its bucket with application data.

```bash
docker compose up -d --wait
# Wait for http://localhost:8025/api/v1/info to respond before SMTP tests.
docker compose exec -T postgres createdb -U postgres krob_krong_onboarding_test
ONBOARDING_IT=true \
ONBOARDING_TEST_DB_URL=jdbc:postgresql://localhost:5432/krob_krong_onboarding_test \
  ./gradlew test --tests '*SchoolOnboardingIT' --tests '*SchoolImageProcessorTest'
docker compose exec -T postgres dropdb -U postgres krob_krong_onboarding_test
```

Tests cover real migrations and transaction guards, SMTP send/confirmation, one-time and concurrent token use,
expiry/resend/failure handling, real RustFS image storage, format validation, lifecycle locks, incomplete
submission, changes/resubmission/stale decisions, reviewer independence/MFA, terminal rejection, branch opening,
workspace reservation/retirement, cross-school denials, suspension and reinstatement. HTTP handlers run through
MockMvc with the application's security filters; these are not a deployed-server or Bruno UI execution test.
