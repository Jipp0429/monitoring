package com.example.monitor.detection;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "monitor.detection")
public record DetectionProperties(
        @DefaultValue("3.0") double zScoreThreshold,
        // 표본이 너무 적으면 mean/stdDev 자체가 불안정해서 정상 범위 값도 이상치로 오판되기 쉽다.
        // 디바이스가 새로 추가된 직후(warm-up 구간)에 이런 오탐이 몰리므로 최소 표본 수를 두고 그 전엔 판정을 보류한다.
        @DefaultValue("10") long minSamplesForDetection,
        // DeviceSimulatorService와 동일한 이유로 기본 버퍼(256)보다 넉넉하게 잡는다.
        @DefaultValue("65536") int sinkBufferSize
) {
}
