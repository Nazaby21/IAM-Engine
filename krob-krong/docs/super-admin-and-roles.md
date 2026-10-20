# Super admin and role assignment

People get permissions through grants in the `user_role` table. There are two kinds:

- **Platform grant:** `super_admin`, which has no school (`school_id` is null) and applies everywhere.
- **School grant:** `school_owner`, `school_admin`, `instructor` or `student` in one school. The last three can
  also be limited to one branch.

Roles, their permissions and who may grant what are seed data
([V1_0_10__SEED_DATA.sql](../src/main/resources/db/migration/V1_0_10__SEED_DATA.sql)). The application checks
grant rules; the `user_role_guard` trigger
([V1_0_8__WRITE_GUARDS.sql](../src/main/resources/db/migration/V1_0_8__WRITE_GUARDS.sql)) enforces data rules
and writes every grant, acceptance and revocation to `audit_event`. Grants are never deleted, only revoked, and
are read from the database on every request, so a change takes effect without signing in again.

Registering a school already gives its creator an accepted `school_owner` grant (see
[School onboarding](onboarding.md)). Everything else follows the steps below.

## Before you start

Start the stack, run the app with `./gradlew bootRun`, and open a database shell:

```bash
docker compose up -d --wait
```

```bash
docker compose exec postgres psql -U postgres -d krob_krong
```

The SQL below uses psql variables (`\set`, `:'name'`), so run it in psql.

## Turn on MFA for an account

Platform roles require multi-factor authentication. Each super admin enrolls an authenticator app that supports
TOTP codes (for example Google Authenticator, Microsoft Authenticator, 1Password or Bitwarden). In Bruno, use the
**MFA** folder.

1. Sign in normally to get an access token (**Authentication → Register** or **Login**).
2. Start enrollment. The response has the `secret` and an `otpauth_uri`; add either to the authenticator app. The
   secret is shown only here, and calling this again replaces an enrollment that isn't active yet.

   ```bash
   curl --fail-with-body -X POST -H "Authorization: Bearer $ACCESS_TOKEN" \
     http://localhost:8080/api/me/mfa/totp
   ```

3. Activate it with the current 6-digit code from the app. The response has `mfa_enabled_at`.

   ```bash
   curl --fail-with-body -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" \
     -d '{"code":"123456"}' http://localhost:8080/api/me/mfa/totp/activate
   ```

From then on, `POST /api/auth/login` needs `mfa_code` next to the email and password:

| Situation | Result |
|---|---|
| No `mfa_code` | 403 `MFA_REQUIRED` |
| Wrong code, or a code that was already used | 401 `INVALID_MFA_CODE` |
| Five wrong codes in a row | The factor locks for 15 minutes: 429 `MFA_LOCKED` |
| Current code | Tokens whose `mfa` claim is `true` |
| `POST /api/auth/refresh` | Keeps the session's MFA status. Sessions started before MFA was turned on get 403 `MFA_REQUIRED` |
| Access tokens issued before MFA was turned on | Valid until they expire, with `mfa: false` |

Codes change every 30 seconds, and one step of clock drift either way is accepted. The secret is stored encrypted
with `krob-krong.security.mfa.encryption-key`. Set a long random value per deployment; changing it makes every
enrolled authenticator unusable, so each person would need a [reset](#reset-a-lost-authenticator).

## Set up the first super admin

1. Register the account and [turn on MFA](#turn-on-mfa-for-an-account) for it. Verifying the email is recommended
   but not required for platform roles.
2. In psql, create the bootstrap grant:

   ```sql
   \set email 'admin@example.com'
   INSERT INTO user_role (user_id, role_id, source)
   SELECT u.id, r.id, 'bootstrap'
   FROM app_user u JOIN role r ON r.code = 'super_admin'
   WHERE lower(u.email) = lower(:'email');
   ```

3. Check the result:

   ```sql
   SELECT u.email, r.code, ur.source, ur.accepted_at, ur.valid_to
   FROM user_role ur
   JOIN app_user u ON u.id = ur.user_id
   JOIN role r ON r.id = ur.role_id
   WHERE r.kind = 'platform' AND ur.revoked_at IS NULL;
   ```

4. Sign in with a code. In Bruno, set `admin_email`, the `admin_password` secret and `mfa_code`, then run
   **Authentication → Admin login**. It stores `admin_access_token`, which the **School review** requests use.

The trigger accepts a `bootstrap` grant only when all of these hold:

| Rule | Error if broken |
|---|---|
| The account is a person, not a service account | `service accounts cannot hold roles` |
| `mfa_enabled_at` is set | `platform roles require MFA` |
| `school_id` is null | `role super_admin must be assigned at platform level` |
| No accepted, currently valid platform grant exists | `bootstrap is only for the first platform admin` |

The grant is accepted immediately, has no `granted_by`, and is audited as `bootstrapped`. You don't need to set
`app.actor_id` for it. The admin endpoints (`GET /api/admin/schools`, review, suspend, reinstate) need the grant,
MFA turned on and a token with `mfa: true`; otherwise they return 403 `MFA_REQUIRED`.

## Add more super admins

Bootstrap is refused once a super admin exists. After that, an existing super admin invites new ones through the
API, signed in with an authenticator code. Inviting needs a session with `mfa: true`, even for an account that has
MFA turned on.

1. The invitee registers and [turns on MFA](#turn-on-mfa-for-an-account). The database refuses platform roles for
   accounts without MFA (400 `platform roles require MFA`).
2. Find the `super_admin` role id with `GET /api/roles?kind=platform` (Bruno: **Roles → List roles**).
3. The inviting super admin sends the invitation (Bruno: **Members → Invite super admin**, which uses
   `admin_access_token`). The response is 201 with `status: "pending"`; its `id` is the `user_role_id`.

   ```bash
   curl --fail-with-body -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" -H "Content-Type: application/json" \
     -d "{\"user_id\":\"$USER_ID\",\"role_id\":\"$SUPER_ADMIN_ROLE_ID\"}" \
     http://localhost:8080/api/admin/members/invite
   ```

4. The invitee signs in with their code and accepts: `GET /api/me/invitations`, then
   `POST /api/me/invitations/{user_role_id}/accept` (Bruno: **My invitations**). The grant gives no permissions
   until then.

`valid_to` works as it does for school roles; `branch_id` is refused. School roles sent to this endpoint, and
platform roles sent to the school invite endpoint, return 400 `ROLE_NOT_GRANTABLE_HERE`.

A super admin can drop their own grant, or another super admin's, with `DELETE /api/user-roles/{user_role_id}`.

## Reset a lost authenticator

Confirm the person's identity first: after a reset, anyone with their password can enroll a new authenticator.

```sql
UPDATE app_user SET mfa_enabled_at = NULL WHERE lower(email) = lower('admin@example.com');
```

The person can then sign in with their password alone and [turn on MFA](#turn-on-mfa-for-an-account) again, which
replaces the old secret. Their roles stay, but the admin endpoints refuse them until MFA is back on.

## Assign school roles to a user

This works through the API today. The caller must hold a role that may grant the target role in that school:

| Granter | Can grant | Branch-limited grants? |
|---|---|---|
| `super_admin` | `school_owner` in any school, and `super_admin` (see above) | — |
| `school_owner` | `school_owner`, `school_admin`, `instructor`, `student` | `school_admin`, `instructor`, `student` only |
| `school_admin` | `instructor`, `student` | yes |

All paths begin with `/api`. Set `ACCESS_TOKEN`, `SCHOOL_ID` and the other shell variables first.

1. **Get the user's id.** There is no user search endpoint. The person sees `user_id` in their register or
   login response, or an operator can run `SELECT id FROM app_user WHERE lower(email) = lower('…');`.
2. **Find the role id.** List only the roles you may grant there (Bruno: **Roles → Grantable roles**). Add
   `&branch_id=…` to list roles you can limit to that branch.

   ```bash
   curl --fail-with-body -H "Authorization: Bearer $ACCESS_TOKEN" \
     "http://localhost:8080/api/roles/grantable?school_id=$SCHOOL_ID"
   ```

3. **Invite the user** (Bruno: **Members → Invite member**). Optionally add `"branch_id"` to limit the role to
   one branch and `"valid_to"` (ISO-8601 instant) to make it expire. The response is 201 with
   `status: "pending"`; its `id` is the `user_role_id`.

   ```bash
   curl --fail-with-body -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" \
     -d "{\"user_id\":\"$USER_ID\",\"role_id\":\"$ROLE_ID\"}" \
     "http://localhost:8080/api/schools/$SCHOOL_ID/members/invite"
   ```

4. **The invitee accepts** with their own token (Bruno: **My invitations**). `GET /me/invitations` lists pending
   invitations; accepting turns the grant `active`.

   ```bash
   curl --fail-with-body -X POST -H "Authorization: Bearer $INVITEE_TOKEN" \
     "http://localhost:8080/api/me/invitations/$USER_ROLE_ID/accept"
   ```

5. **Check.** `GET /schools/{school_id}/members/{user_id}/roles` lists the user's grants in the school, and the
   invitee can call `GET /permissions/effective?school_id=…` to see what they can now do.

### Rules

- **School status:** while a school is `draft`, `changes_requested` or `pending_review`, only `school_owner` and
  `school_admin` can be invited. `instructor` and `student` need an `approved` school. Nobody can be granted a
  role in a `suspended` or `rejected` school, and grants held there cannot be used to grant.
- **Branches:** a branch-limited granter must set `branch_id` to their own branch, and that branch must be
  `active`.
- **One grant at a time:** a user can't hold or be invited to the same role in the same school and branch twice.
- **No self-grants**, and service accounts can't hold roles.
- **Pending gives nothing:** an invitation grants no permissions until it is accepted.
- **Changing a grant:** grants can't be edited. To change the role, branch or end date, revoke the grant and
  send a new invitation.

## Revoke a role

```bash
curl --fail-with-body -X DELETE -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" \
  -d '{"reason":"Left the school"}' \
  "http://localhost:8080/api/user-roles/$USER_ROLE_ID"
```

The holder can drop their own grant; anyone else must be allowed to grant that role there. The body is optional.
This also cancels a pending invitation. Revocation ends the grant now and keeps it in history as `revoked`.
A school must keep at least one accepted owner, so add another owner before removing the last one.

## Errors

| Code | HTTP | Meaning |
|---|---|---|
| `ROLE_GRANT_NOT_ALLOWED` | 403 | Your roles don't allow granting or revoking this role here, or you tried to grant yourself |
| `ROLE_ALREADY_HELD` | 409 | The user already holds or has been invited to this role here |
| `ROLE_NOT_GRANTABLE_HERE` | 400 | A platform role sent to a school endpoint or the reverse, or a branch on a platform role |
| `USER_NOT_FOUND` / `ROLE_NOT_FOUND` | 404 | Unknown `user_id` or `role_id` |
| `INVITATION_NOT_FOUND` | 404 | Unknown, revoked or someone else's invitation |
| `INVITATION_ALREADY_ACCEPTED` | 400 | The invitation was already accepted |
| `BAD_REQUEST` | 400 | A database rule refused it; the message says which, e.g. `instructor can only be invited after the school is approved` |
| `MFA_REQUIRED` | 403 | Sign-in needs `mfa_code`, or an admin endpoint was called without an MFA-verified token |
| `INVALID_MFA_CODE` | 401 | The code is wrong, already used, or the account has no active authenticator |
| `MFA_LOCKED` | 429 | Too many wrong codes; wait for the lockout to end |
| `MFA_ALREADY_ENABLED` | 409 | Enrollment or activation for an account that already has MFA on |
| `MFA_ENROLLMENT_NOT_FOUND` | 404 | Activation before starting enrollment |
