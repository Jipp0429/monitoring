package com.example.monitor.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * device count 만큼의 가상 디바이스를 시뮬레이션한다.
 * 매 tick(기본 1초)마다 활성 디바이스 각각이 센서 값을 하나씩 발행한다.
 * 평소에는 정상 범위(SimulatorProperties의 baselineMean/baselineStdDev)에서 값이 나오지만,
 * 확률적으로(anomalyProbability) 급격히 튀는 이상치를 섞어 넣어
 * 실제 서비스처럼 이상 탐지 로직이 동작하는 걸 확인할 수 있게 한다.
 */
@Service
public class DeviceSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(DeviceSimulatorService.class);

    private final SimulatorProperties properties;

    // 현재 시뮬레이션 중인 디바이스 수. API로 런타임에 조절 가능 (스케일 테스트용)
    private final AtomicInteger deviceCount;

    private final Sinks.Many<SensorReading> sink;

    public DeviceSimulatorService(SimulatorProperties properties) {
        this.properties = properties;
        this.deviceCount = new AtomicInteger(properties.initialDeviceCount());
        this.sink = Sinks.many().multicast().onBackpressureBuffer(properties.sinkBufferSize(), false);
    }

    public Flux<SensorReading> readingsStream() {
        return sink.asFlux();
    }

    public int getDeviceCount() {
        return deviceCount.get();
    }

    public int setDeviceCount(int newCount) {
        int clamped = Math.max(0, newCount);
        deviceCount.set(clamped);
        return clamped;
    }

    @Scheduled(fixedRate = 1000)
    public void tick() {
        int count = deviceCount.get();
        Instant now = Instant.now();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int dropped = 0;

        for (int i = 0; i < count; i++) {
            String deviceId = "device-" + i;
            double value = properties.baselineMean() + random.nextGaussian() * properties.baselineStdDev();

            if (random.nextDouble() < properties.anomalyProbability()) {
                double direction = random.nextBoolean() ? 1 : -1;
                value += direction * properties.anomalyMagnitude() * random.nextDouble();
            }

            Sinks.EmitResult result = sink.tryEmitNext(new SensorReading(deviceId, value, now));
            if (result.isFailure()) {
                dropped++;
            }
        }

        if (dropped > 0) {
            log.warn("readings sink buffer overflow: {}/{} events dropped this tick", dropped, count);
        }
    }
}
