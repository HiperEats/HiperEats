-- Stores and the scooters of their fixed fleets (HE-29).
-- A store holds at most fleet_capacity scooters (BR13); the rule is checked by the application,
-- which locks the store row while it registers a scooter.

CREATE TABLE stores (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name           TEXT    NOT NULL,
    fleet_capacity INTEGER NOT NULL CHECK (fleet_capacity >= 0)
);

-- code identifies the scooter on the platform and in its telemetry (scooterCode).
CREATE TABLE scooters (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(32) NOT NULL CONSTRAINT scooters_code_unique UNIQUE,
    type     VARCHAR(20) NOT NULL CHECK (type IN ('STANDARD', 'LONG_RANGE')),
    state    VARCHAR(20) NOT NULL
             CHECK (state IN ('INACTIVE', 'AVAILABLE', 'RESERVED', 'IN_USE', 'MAINTENANCE', 'RETIRED')),
    store_id BIGINT      NOT NULL REFERENCES stores (id)
);

CREATE INDEX scooters_store_id_idx ON scooters (store_id);
