ALTER TABLE api_keys
    ADD COLUMN mask VARCHAR(255) NOT NULL;

ALTER TABLE api_keys
    RENAME COLUMN api_key TO api_key_hash;
