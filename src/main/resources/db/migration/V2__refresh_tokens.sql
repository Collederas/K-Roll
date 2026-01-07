CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY,
    owner_id   UUID      NOT NULL,
    token      TEXT      NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_token_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
);
