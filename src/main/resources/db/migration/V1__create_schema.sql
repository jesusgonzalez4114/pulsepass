CREATE TABLE venues (
                        id       BIGSERIAL PRIMARY KEY,
                        code     VARCHAR(20) NOT NULL UNIQUE,
                        name     VARCHAR(150) NOT NULL,
                        city     VARCHAR(100) NOT NULL,
                        address  VARCHAR(200),
                        capacity INTEGER NOT NULL,
                        active   BOOLEAN NOT NULL DEFAULT TRUE,
                        CONSTRAINT chk_venue_capacity CHECK (capacity > 0)
);

CREATE TABLE events (
                        id            BIGSERIAL PRIMARY KEY,
                        event_code    VARCHAR(30) NOT NULL UNIQUE,
                        name          VARCHAR(150) NOT NULL,
                        description   TEXT,
                        category      VARCHAR(30) NOT NULL,
                        status        VARCHAR(30) NOT NULL,
                        event_date    TIMESTAMP NOT NULL,
                        minimum_age   INTEGER,
                        venue_id      BIGINT NOT NULL,
                        CONSTRAINT fk_event_venue
                            FOREIGN KEY (venue_id) REFERENCES venues(id),
                        CONSTRAINT chk_event_category
                            CHECK (category IN ('MUSIC', 'SPORTS', 'TECHNOLOGY', 'EDUCATION', 'CULTURE', 'ENTERTAINMENT')),
                        CONSTRAINT chk_event_status
                            CHECK (status IN ('DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED'))
);

CREATE TABLE artists (
                         id          BIGSERIAL PRIMARY KEY,
                         stage_name  VARCHAR(150) NOT NULL UNIQUE,
                         country     VARCHAR(100),
                         genre       VARCHAR(100),
                         active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE event_artists (
                               event_id   BIGINT NOT NULL,
                               artist_id  BIGINT NOT NULL,
                               PRIMARY KEY (event_id, artist_id),
                               CONSTRAINT fk_ea_event
                                   FOREIGN KEY (event_id) REFERENCES events(id),
                               CONSTRAINT fk_ea_artist
                                   FOREIGN KEY (artist_id) REFERENCES artists(id)
);

CREATE TABLE users (
                       id        BIGSERIAL PRIMARY KEY,
                       username  VARCHAR(50) NOT NULL UNIQUE,
                       email     VARCHAR(150) NOT NULL UNIQUE,
                       active    BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE user_profiles (
                               id          BIGSERIAL PRIMARY KEY,
                               user_id     BIGINT NOT NULL UNIQUE,
                               first_name  VARCHAR(100),
                               last_name   VARCHAR(100),
                               phone       VARCHAR(30),
                               city        VARCHAR(100),
                               birth_date  DATE,
                               CONSTRAINT fk_profile_user
                                   FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE tickets (
                         id             BIGSERIAL PRIMARY KEY,
                         ticket_code    VARCHAR(30) NOT NULL UNIQUE,
                         type           VARCHAR(30) NOT NULL,
                         status         VARCHAR(30) NOT NULL,
                         price          NUMERIC(10,2) NOT NULL,
                         purchase_date  TIMESTAMP,
                         user_id        BIGINT NOT NULL,
                         event_id       BIGINT NOT NULL,
                         CONSTRAINT fk_ticket_user
                             FOREIGN KEY (user_id) REFERENCES users(id),
                         CONSTRAINT fk_ticket_event
                             FOREIGN KEY (event_id) REFERENCES events(id),
                         CONSTRAINT chk_ticket_price CHECK (price >= 0),
                         CONSTRAINT chk_ticket_type
                             CHECK (type IN ('GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT')),
                         CONSTRAINT chk_ticket_status
                             CHECK (status IN ('RESERVED', 'PAID', 'CANCELLED', 'USED'))
);

CREATE INDEX idx_events_venue      ON events(venue_id);
CREATE INDEX idx_events_status     ON events(status);
CREATE INDEX idx_events_date       ON events(event_date);
CREATE INDEX idx_tickets_user      ON tickets(user_id);
CREATE INDEX idx_tickets_event     ON tickets(event_id);
CREATE INDEX idx_tickets_status    ON tickets(status);