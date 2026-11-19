-- Table used by the /locate endpoint.
CREATE TABLE IF NOT EXISTS cell_towers (
    mcc INTEGER,
    mnc INTEGER,
    tac INTEGER,
    cid BIGINT,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION
);

-- Composite B-tree index on the exact lookup key, so /locate does an index
-- lookup instead of scanning the whole table. Not UNIQUE because the
-- crowd-sourced source data can contain duplicate cells (the query uses LIMIT 1).
CREATE INDEX IF NOT EXISTS idx_cell_towers_lookup
    ON cell_towers (mcc, mnc, tac, cid);
