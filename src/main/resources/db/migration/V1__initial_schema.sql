CREATE TABLE users (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_projects_owner
        FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE environments (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    name TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_env_project
        FOREIGN KEY (project_id) REFERENCES projects(id),

    CONSTRAINT uq_env_project_name
        UNIQUE (project_id, name)
);

CREATE TABLE api_keys (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    api_key TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_api_env
        FOREIGN KEY (environment_id) REFERENCES environments(id)
);

CREATE TABLE config_entries
(
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    config_key TEXT NOT NULL,
    config_value TEXT NOT NULL,
    config_type TEXT NOT NULL CHECK (config_type IN ('int', 'float', 'bool', 'string')),
    active_from TIMESTAMP,
    active_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_config_env
        FOREIGN KEY (environment_id) REFERENCES environments(id),

    CONSTRAINT uq_config_env_key
            UNIQUE (environment_id, config_key)
);
