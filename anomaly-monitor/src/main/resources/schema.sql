CREATE TABLE IF NOT EXISTS anomaly_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    reading_value DOUBLE NOT NULL,
    mean DOUBLE NOT NULL,
    std_dev DOUBLE NOT NULL,
    z_score DOUBLE NOT NULL,
    detected_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_anomaly_event_detected_at ON anomaly_event (detected_at DESC);
