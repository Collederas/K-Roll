CREATE TABLE config_versions (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    version TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    contract_hash TEXT NOT NULL,
    notes TEXT
);

ALTER TABLE config_versions
    ADD CONSTRAINT uq_config_versions_env_version
    UNIQUE (environment_id, version);

ALTER TABLE config_versions
    ADD CONSTRAINT fk_config_versions_environment
    FOREIGN KEY (environment_id)
    REFERENCES environments(id)
    ON DELETE RESTRICT;

CREATE INDEX idx_config_versions_environment
    ON config_versions (environment_id);

CREATE INDEX idx_config_versions_created_at
    ON config_versions (created_at DESC);


CREATE TABLE config_snapshots (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    version TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    contract_hash TEXT NOT NULL,
    snapshot_json TEXT NOT NULL
);

ALTER TABLE config_snapshots
    ADD CONSTRAINT uq_config_snapshots_env_version
    UNIQUE (environment_id, version);

ALTER TABLE config_snapshots
    ADD CONSTRAINT fk_config_snapshots_environment
    FOREIGN KEY (environment_id)
    REFERENCES environments(id)
    ON DELETE RESTRICT;

CREATE INDEX idx_config_snapshots_environment
    ON config_snapshots (environment_id);

-- Fast diff / fetch by environment + version
CREATE INDEX idx_config_snapshots_env_version
    ON config_snapshots (environment_id, version);

-- time-ordered access (history views, audits)
CREATE INDEX idx_config_snapshots_created_at
    ON config_snapshots (created_at DESC);


CREATE TABLE active_versions (
    environment_id UUID PRIMARY KEY,
    version TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID
);

ALTER TABLE active_versions
    ADD CONSTRAINT fk_active_versions_environment
    FOREIGN KEY (environment_id)
    REFERENCES environments(id)
    ON DELETE CASCADE;

ALTER TABLE active_versions
    ADD CONSTRAINT fk_active_versions_version
    FOREIGN KEY (environment_id, version)
    REFERENCES config_versions(environment_id, version)
    ON DELETE RESTRICT;
