CREATE TABLE IF NOT EXISTS event (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    location VARCHAR(255),
    date DATE,
    description VARCHAR(2000),
    einlass TIME,
    beginn TIME,
    price VARCHAR(255),
    misc VARCHAR(4000),
    source_url VARCHAR(1000),
    event_hash VARCHAR(255) NOT NULL UNIQUE,
    thumbnail VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS scrape_run (
    id BIGSERIAL PRIMARY KEY,
    scraper_name VARCHAR(255),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    added INT NOT NULL DEFAULT 0,
    updated INT NOT NULL DEFAULT 0,
    errors INT NOT NULL DEFAULT 0,
    message VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_event_date ON event(date);
