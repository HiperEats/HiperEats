-- An admin can deactivate an account (HE-36): it can no longer log in, and its data stays.
ALTER TABLE user_accounts ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
