-- Existing and service accounts remain without password credentials until explicitly provisioned.
ALTER TABLE app_user ADD COLUMN password_hash text;
