-- Accounts of the people who use the platform (HE-28). The email is the login name, stored in
-- lower case by the application; the password is stored only as a hash.
CREATE TABLE user_accounts (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    CONSTRAINT user_accounts_email_unique UNIQUE (email),
    CONSTRAINT user_accounts_email_lower_case CHECK (email = lower(email)),
    CONSTRAINT user_accounts_role_known
        CHECK (role IN ('COURIER', 'TECHNICIAN', 'OPERATOR', 'CHAIN_MANAGER', 'ADMIN'))
);
