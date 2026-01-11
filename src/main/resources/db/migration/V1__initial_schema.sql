CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    email         TEXT      NOT NULL UNIQUE,
    username      TEXT      NOT NULL UNIQUE,
    password_hash TEXT      NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL
);

CREATE TABLE projects
(
    id         UUID PRIMARY KEY,
    owner_id   UUID      NOT NULL,
    name       TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_projects_name
        UNIQUE (name),

    CONSTRAINT fk_projects_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id)
            ON DELETE RESTRICT -- force explicit deletion
);

CREATE TABLE environments
(
    id         UUID PRIMARY KEY,
    project_id UUID      NOT NULL,
    name       TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_env_project
        FOREIGN KEY (project_id)
            REFERENCES projects (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_env_project_name
        UNIQUE (project_id, name)
);

CREATE TABLE api_keys
(
    id             UUID PRIMARY KEY,
    environment_id UUID      NOT NULL,
    api_key        TEXT      NOT NULL UNIQUE,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,

    CONSTRAINT fk_api_env
        FOREIGN KEY (environment_id) REFERENCES environments (id)
);

-- ==== CONFIG ENTRIES ====
CREATE TABLE config_entries
(
    id             UUID PRIMARY KEY,
    environment_id UUID                      NOT NULL,
    config_key     TEXT                      NOT NULL,
    config_value   TEXT                      NOT NULL,
    config_type    TEXT                      NOT NULL,
    active_from    TIMESTAMP WITH TIME ZONE,
    active_until   TIMESTAMP WITH TIME ZONE,
    created_by     UUID,
    created_at     TIMESTAMP WITH TIME ZONE  NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE  NOT NULL,

    CONSTRAINT fk_config_env
        FOREIGN KEY (environment_id)
            REFERENCES environments (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_config_created_by
        FOREIGN KEY (created_by)
            REFERENCES users (id),

    CONSTRAINT uq_config_env_key
        UNIQUE (environment_id, config_key),

    CONSTRAINT chk_config_type_valid
        CHECK (config_type IN ('BOOLEAN', 'STRING', 'NUMBER', 'JSON')),

    CONSTRAINT chk_config_entry_valid_window
        CHECK (
            active_from IS NULL
                OR active_until IS NULL
                OR active_from <= active_until
            )
);


CREATE INDEX idx_config_entry_key
    ON config_entries (config_key);


CREATE TABLE config_entry_history
(
    id                 UUID PRIMARY KEY,
    config_entry_id    UUID      NOT NULL,
    environment_id     UUID      NOT NULL,
    changed_by         UUID      NOT NULL,
    changed_at         TIMESTAMP NOT NULL,
    change_description TEXT,
    config_snapshot    TEXT      NOT NULL, -- this should become JSONB at some point. Made it TEXT to simplify testing with H2

    CONSTRAINT fk_history_entry
        FOREIGN KEY (config_entry_id) REFERENCES config_entries (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_history_env
        FOREIGN KEY (environment_id) REFERENCES environments (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_history_user
        FOREIGN KEY (changed_by) REFERENCES users (id)
);
