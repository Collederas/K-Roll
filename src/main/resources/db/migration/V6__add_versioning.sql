--  ==== VERSIONS
CREATE TABLE config_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    environment_id UUID NOT NULL,

    -- for machines
    version_sequence BIGINT NOT NULL,
    -- for humans
    version_label TEXT NOT NULL,

    -- Integrity
    contract_hash TEXT NOT NULL,
    parent_hash TEXT,

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    change_log TEXT
);

ALTER TABLE config_versions
    ADD CONSTRAINT uq_config_versions_env_version
    UNIQUE (environment_id, version_sequence);

ALTER TABLE config_versions
    ADD CONSTRAINT uq_config_versions_env_label
    UNIQUE (environment_id, version_label);

ALTER TABLE config_versions
    ADD CONSTRAINT fk_config_versions_environment
    FOREIGN KEY (environment_id)
    REFERENCES environments(id)
    ON DELETE RESTRICT;

-- optimizes "ORDER BY version_sequence DESC" queries automatically.
CREATE INDEX idx_config_versions_env_seq_desc
    ON config_versions (environment_id, version_sequence DESC);

-- for UI showing activity across ALL projects.
CREATE INDEX idx_config_versions_created_at
    ON config_versions (created_at DESC);



--  ==== SNAPSHOTS
CREATE TABLE config_snapshots (
    version_id UUID PRIMARY KEY REFERENCES config_versions(id),
    created_at TIMESTAMPTZ NOT NULL,
    snapshot_json TEXT NOT NULL,
    diff_payload TEXT
);

ALTER TABLE config_snapshots
    ADD CONSTRAINT fk_snapshots_version
    FOREIGN KEY (version_id)
    REFERENCES config_versions(id)
    ON DELETE CASCADE;




--  ==== POINTER TO ACTIVE VERSION
CREATE TABLE active_versions (
    environment_id UUID PRIMARY KEY,

    -- the pointer
    active_version_id UUID,

    draft_json JSONB,
    draft_updated_at TIMESTAMPTZ,
    draft_updated_by UUID,

    published_at TIMESTAMPTZ,
    published_by UUID
);

ALTER TABLE active_versions
    ADD CONSTRAINT fk_active_env
    FOREIGN KEY (environment_id)
    REFERENCES environments(id)
    ON DELETE CASCADE;

-- you cannot delete a version even if it’s inactive but historically referenced
ALTER TABLE active_versions
    ADD CONSTRAINT fk_active_version_ptr
    FOREIGN KEY (active_version_id)
    REFERENCES config_versions(id)
    ON DELETE RESTRICT;

ALTER TABLE active_versions
    ADD CONSTRAINT check_draft_json_valid
    CHECK (draft_json IS NULL OR draft_json::jsonb IS NOT NULL);

-- publish exists iff there is an active version pointer
ALTER TABLE active_versions
    ADD CONSTRAINT check_publish_state_consistent
    CHECK (
        (active_version_id IS NOT NULL AND published_at IS NOT NULL)
     OR (active_version_id IS NULL     AND published_at IS NULL)
    );
