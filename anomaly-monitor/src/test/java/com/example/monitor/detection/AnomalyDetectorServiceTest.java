package com.example.monitor.detection;

import com.example.monitor.persistence.AnomalyEventEntity;
import com.example.monitor.persistence.UnsupportedAnomalyEventRepository;
import com.example.monitor.simulator.DeviceSimulatorService;
import com.example.monitor.simulator.SensorReading;
import com.example.monitor.simulator.SimulatorProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AnomalyDetectorService의 upstream(DeviceSimulatorService)과 저장소를
 * 손으로 만든 fake로 대체해서, 판독값을 원하는 순서/타이밍으로 주입하며 검증한다.
 * (이 환경의 Java 25 JVM에서 Mockito의 바이트코드 생성이 계속 멎어버려 Mockito 없이 작성했다.)
 */
class AnomalyDetectorServiceTest {

    private static final DetectionProperties DETECTION_PROPERTIES = new DetectionProperties(3.0, 10, 65536);
    private static final int MIN_SAMPLES_FOR_DETECTION = (int) DETECTION_PROPERTIES.minSamplesForDetection();

    private Sinks.Many<SensorReading> upstream;
    private RecordingAnomalyEventRepository repository;
    private AnomalyDetectorService service;

    @BeforeEach
    void setUp() {
        upstream = Sinks.many().multicast().onBackpressureBuffer();
        repository = new RecordingAnomalyEventRepository();

        SimulatorProperties simulatorProperties = new SimulatorProperties(50.0, 5.0, 0.03, 40.0, 100, 20000, 65536);
        DeviceSimulatorService fakeSimulator = new DeviceSimulatorService(simulatorProperties, new SimpleMeterRegistry()) {
            @Override
            public Flux<SensorReading> readingsStream() {
                return upstream.asFlux();
            }
        };

        service = new AnomalyDetectorService(DETECTION_PROPERTIES, fakeSimulator, repository, new SimpleMeterRegistry());
        service.subscribeToReadings();
    }

    @Test
    void firstReadingUsesPreUpdateBaselineAndIsNeverFlaggedAsAnomaly() {
        StepVerifier.create(service.resultsStream())
                .then(() -> upstream.tryEmitNext(new SensorReading("device-a", 9999.0, Instant.now())))
                .assertNext(r -> {
                    // 최초 판독값은 반영 전(mean=0, stdDev=0) 기준으로 z-score가 계산되고,
                    // 표본 수가 최소 기준 미만이라 값과 무관하게 anomaly=false여야 한다.
                    assertThat(r.mean()).isZero();
                    assertThat(r.stdDev()).isZero();
                    assertThat(r.anomaly()).isFalse();
                })
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void suppressesAnomaliesDuringWarmUpThenDetectsAfterMinSamples() {
        int warmUpCount = MIN_SAMPLES_FOR_DETECTION - 1;

        StepVerifier.Step<AnomalyResult> step = StepVerifier.create(service.resultsStream())
                .then(() -> emitAlternatingBaseline("device-b", warmUpCount));
        for (int i = 0; i < warmUpCount; i++) {
            step = step.expectNextMatches(r -> !r.anomaly());
        }
        step.then(() -> upstream.tryEmitNext(new SensorReading("device-b", 500.0, Instant.now())))
                .assertNext(r -> assertThat(r.anomaly()).isTrue())
                .thenCancel()
                .verify(Duration.ofSeconds(2));

        assertThat(repository.saved).hasSize(1);
        assertThat(repository.saved.get(0).getDeviceId()).isEqualTo("device-b");
    }

    @Test
    void normalValuesWithinRangeAreNotFlagged() {
        // resultsStream()은 AnomalyDetectorService 자신의 내부 sink라서
        // upstream이 끝나도 완료 신호가 전파되지 않는다(원래도 계속 살아있어야 하는 스트림).
        // 그래서 완료를 기다리지 않고 받은 개수만큼만 소비한 뒤 취소한다.
        int count = MIN_SAMPLES_FOR_DETECTION + 5;

        StepVerifier.Step<AnomalyResult> step = StepVerifier.create(service.resultsStream())
                .then(() -> emitAlternatingBaseline("device-c", count));
        for (int i = 0; i < count; i++) {
            step = step.expectNextMatches(r -> !r.anomaly());
        }
        step.thenCancel().verify(Duration.ofSeconds(2));

        assertThat(repository.saved).isEmpty();
    }

    private void emitAlternatingBaseline(String deviceId, int count) {
        for (int i = 0; i < count; i++) {
            double value = (i % 2 == 0) ? 48.0 : 52.0;
            Sinks.EmitResult result = upstream.tryEmitNext(new SensorReading(deviceId, value, Instant.now()));
            assertThat(result).as("emit #%d for %s", i, deviceId).isEqualTo(Sinks.EmitResult.OK);
        }
    }

    /** save()만 실제로 동작하고 나머지는 이 테스트에서 쓰이지 않는 최소 fake. */
    private static class RecordingAnomalyEventRepository extends UnsupportedAnomalyEventRepository {

        final List<AnomalyEventEntity> saved = new CopyOnWriteArrayList<>();

        @Override
        public <S extends AnomalyEventEntity> Mono<S> save(S entity) {
            saved.add(entity);
            return Mono.just(entity);
        }
    }
}
