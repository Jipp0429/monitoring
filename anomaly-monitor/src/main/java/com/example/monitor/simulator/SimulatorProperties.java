package com.example.monitor.simulator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "monitor.simulator")
public record SimulatorProperties(
        @DefaultValue("50.0") double baselineMean,
        @DefaultValue("5.0") double baselineStdDev,
        @DefaultValue("0.03") double anomalyProbability,
        @DefaultValue("40.0") double anomalyMagnitude,
        @DefaultValue("100") int initialDeviceCount,
        // 디바이스 수가 수천 대로 늘어나면 한 tick에서 수천 개 이벤트가 한꺼번에 쏟아진다.
        // 기본 버퍼(256, Queues.SMALL_BUFFER_SIZE)로는 그 버스트를 못 받아내고
        // tryEmitNext()가 조용히 실패해 이벤트가 드롭되므로 넉넉하게 잡는다.
        @DefaultValue("65536") int sinkBufferSize
) {
}
