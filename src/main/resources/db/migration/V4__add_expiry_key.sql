ALTER TABLE api_keys ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE NULL;
CREATE INDEX idx_api_keys_expires_at ON api_keys(expires_at)
