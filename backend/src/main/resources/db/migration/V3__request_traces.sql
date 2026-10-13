CREATE TABLE request_traces (
    id BIGSERIAL PRIMARY KEY,
    trace_id VARCHAR(36) NOT NULL,
    endpoint VARCHAR(150) NOT NULL,
    api_ms DOUBLE PRECISION NOT NULL,
    business_ms DOUBLE PRECISION NOT NULL,
    db_ms DOUBLE PRECISION NOT NULL,
    total_ms DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_request_traces_created_at ON request_traces (created_at);
CREATE INDEX idx_request_traces_endpoint ON request_traces (endpoint);
