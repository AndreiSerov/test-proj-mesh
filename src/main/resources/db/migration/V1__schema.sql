CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    date_of_birth DATE         NOT NULL,
    password_hash VARCHAR(100) NOT NULL
);

CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT         NOT NULL UNIQUE REFERENCES users (id),
    balance         NUMERIC(19, 2) NOT NULL CONSTRAINT balance_non_negative CHECK (balance >= 0),
    initial_deposit NUMERIC(19, 2) NOT NULL CHECK (initial_deposit > 0)
);

CREATE TABLE emails (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT       NOT NULL REFERENCES users (id),
    value   VARCHAR(255) NOT NULL UNIQUE
);
CREATE INDEX emails_user_id_idx ON emails (user_id);

CREATE TABLE phones (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT      NOT NULL REFERENCES users (id),
    value   VARCHAR(15) NOT NULL UNIQUE
);
CREATE INDEX phones_user_id_idx ON phones (user_id);

CREATE TABLE transfers (
    id           BIGSERIAL PRIMARY KEY,
    from_user_id BIGINT         NOT NULL REFERENCES users (id),
    to_user_id   BIGINT         NOT NULL REFERENCES users (id),
    amount       NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CHECK (from_user_id <> to_user_id)
);
