package com.example.monitor.detection;

import java.time.Instant;

/**
 * 한 번의 센서 판독값에 대한 이상 탐지 결과.
 * mean/stdDev는 이 판독값을 반영하기 "직전" 상태 기준이다 (자기 자신이 자신의 기준선을 왜곡하지 않도록).
 */
public record AnomalyResult(
        String deviceId,
        double value,
        double mean,
        double stdDev,
        double zScore,
        boolean anomaly,
        Instant timestamp
) {
}
