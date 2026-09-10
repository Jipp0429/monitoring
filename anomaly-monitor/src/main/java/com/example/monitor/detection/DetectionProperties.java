package com.example.monitor.detection;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "monitor.detection")
public record DetectionProperties(
        @DefaultValue("3.0") double zScoreThreshold,
        // 표본이 너무 적으면 mean/stdDev 자체가 불안정해서 정상 범위 값도 이상치로 오판되기 쉽다.
        // 디바이스가 새로 추가된 직후(warm-up 구간)에 이런 오탐이 몰리므로 최소 표본 수를 두고 그 전엔 판정을 보류한다.
        @DefaultValue("10") long minSamplesForDetection,
        // directBestEffort(무버퍼)는 구독자가 없을 때 쌓인 과거분이 재연결 시 한꺼번에 쏟아지는 문제는 없앴지만,
        // 한 tick 안에서 동기적으로 몰리는 burst를 흡수하지 못해 활성 구독자에게도 이벤트가 유실되는 부작용이 있었다.
        // maxDeviceCount 정도로 작게 버퍼를 두면, tick당 burst는 흡수하면서도(활성 구독자 유실 방지),
        // 오래 유휴 상태였다가 재연결해도 밀린 이벤트가 최대 이 크기만큼으로 제한된다(과거의 65536/무제한 누적과 다름).
        @DefaultValue("20000") int resultsBufferSize
) {
}
