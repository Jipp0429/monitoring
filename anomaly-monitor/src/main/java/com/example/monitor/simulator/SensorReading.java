package com.example.monitor.simulator;

import java.time.Instant;

/**
 * 디바이스 한 대가 한 시점에 보낸 센서 값 하나를 표현하는 불변 객체.
 */
public record SensorReading(
        String deviceId,
        double value,
        Instant timestamp
) {
}
