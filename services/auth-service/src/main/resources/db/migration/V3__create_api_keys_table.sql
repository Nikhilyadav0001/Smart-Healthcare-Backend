CREATE TABLE api_keys (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    roles VARCHAR(255) NOT NULL DEFAULT 'API_CLIENT,SERVICE',
    created_by VARCHAR(320),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_api_keys_active ON api_keys(active);
