package com.example.monitor.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("anomaly_event")
public class AnomalyEventEntity {

    @Id
    private Long id;
    private String deviceId;
    @Column("reading_value")
    private double value;
    private double mean;
    private double stdDev;
    private double zScore;
    private Instant detectedAt;

    public AnomalyEventEntity() {
    }

    public AnomalyEventEntity(String deviceId, double value, double mean, double stdDev,
                               double zScore, Instant detectedAt) {
        this.deviceId = deviceId;
        this.value = value;
        this.mean = mean;
        this.stdDev = stdDev;
        this.zScore = zScore;
        this.detectedAt = detectedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public double getValue() {
        return value;
    }

    public double getMean() {
        return mean;
    }

    public double getStdDev() {
        return stdDev;
    }

    public double getZScore() {
        return zScore;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }
}
