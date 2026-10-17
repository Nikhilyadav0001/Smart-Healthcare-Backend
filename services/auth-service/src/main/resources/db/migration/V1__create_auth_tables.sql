CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(320) NOT NULL UNIQUE,
                       password_hash VARCHAR(255),
                       status VARCHAR(32) NOT NULL,
                       email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                       locked_until TIMESTAMP WITH TIME ZONE,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE user_roles (
                            user_id UUID NOT NULL,
                            role VARCHAR(100) NOT NULL,

                            PRIMARY KEY (user_id, role),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE
);

CREATE TABLE user_identities (
                                 id UUID PRIMARY KEY,
                                 user_id UUID NOT NULL,
                                 provider VARCHAR(32) NOT NULL,
                                 provider_subject VARCHAR(255) NOT NULL,
                                 email VARCHAR(320),
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                 CONSTRAINT fk_user_identities_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT uk_user_identity_provider_subject
                                     UNIQUE (provider, provider_subject)
);

CREATE TABLE verification_tokens (
                                     id UUID PRIMARY KEY,
                                     user_id UUID NOT NULL,
                                     token_hash VARCHAR(255) NOT NULL UNIQUE,
                                     type VARCHAR(32) NOT NULL,
                                     expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     used_at TIMESTAMP WITH TIME ZONE,

                                     CONSTRAINT fk_verification_tokens_user
                                         FOREIGN KEY (user_id)
                                             REFERENCES users(id)
                                             ON DELETE CASCADE
);

CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY,
                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(150) NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(32) NOT NULL,
                               attempts INTEGER NOT NULL DEFAULT 0,
                               available_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               published_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE processed_events (
                                  event_id UUID PRIMARY KEY,
                                  event_type VARCHAR(150) NOT NULL,
                                  processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE used_refresh_tokens (
                                     id UUID PRIMARY KEY,
                                     token_value VARCHAR(2000) NOT NULL UNIQUE,
                                     authorization_id VARCHAR(255),
                                     used_at TIMESTAMP WITH TIME ZONE NOT NULL
);