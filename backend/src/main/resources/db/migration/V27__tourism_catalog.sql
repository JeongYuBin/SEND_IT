CREATE TABLE tourism_catalog (
    mode varchar(16) NOT NULL,
    content_id varchar(80) NOT NULL,
    content_type_id varchar(16),
    name text NOT NULL,
    category text,
    address text,
    latitude double precision NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude double precision NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    image_url text,
    event_start_date date,
    event_end_date date,
    geom geometry(Point,4326) GENERATED ALWAYS AS
        (ST_SetSRID(ST_MakePoint(longitude,latitude),4326)) STORED,
    PRIMARY KEY (mode, content_id)
);
CREATE INDEX tourism_catalog_geom_idx ON tourism_catalog USING gist(geom);
CREATE TABLE tourism_catalog_sync (
    mode varchar(16) PRIMARY KEY,
    last_success timestamptz,
    period date,
    last_error boolean NOT NULL DEFAULT false
);
INSERT INTO tourism_catalog_sync(mode) VALUES ('nearby'), ('festival');
