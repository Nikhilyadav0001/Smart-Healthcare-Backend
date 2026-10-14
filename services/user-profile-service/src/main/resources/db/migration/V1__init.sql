CREATE TABLE profiles (
                          user_id            CHAR(36)     NOT NULL PRIMARY KEY,
                          email              VARCHAR(255) NOT NULL,
                          full_name          VARCHAR(150),
                          date_of_birth      DATE,
                          gender             VARCHAR(20),
                          phone              VARCHAR(30),
                          assigned_doctor_id CHAR(36),
                          created_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE outbox_events (
                               id           CHAR(36)     NOT NULL PRIMARY KEY,
                               topic        VARCHAR(100) NOT NULL,
                               event_key    VARCHAR(100) NOT NULL,
                               payload      TEXT         NOT NULL,
                               created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               published_at TIMESTAMP(6) NULL,
                               INDEX idx_outbox_unpublished (published_at, created_at)
);

CREATE TABLE processed_events (
                                  event_id     CHAR(36)     NOT NULL PRIMARY KEY,
                                  processed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);