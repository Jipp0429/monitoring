package com.example.monitor.detection;

import com.example.monitor.persistence.AnomalyEventEntity;
import com.example.monitor.persistence.AnomalyEventRepository;
import com.example.monitor.simulator.DeviceSimulatorService;
import com.example.monitor.simulator.SensorReading;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 디바이스별 WelfordStats를 유지하며 Z-score 기반으로 이상치를 판단한다.
 * Z-score는 해당 판독값을 통계에 반영하기 전의 mean/stdDev를 기준으로 계산한다.
 * 그렇지 않으면 극단값 하나가 자신의 기준선(mean/stdDev)을 즉시 넓혀버려서
 * 정작 그 값 자체는 이상치로 잡히지 않는 문제가 생긴다.
 */
@Service
public class AnomalyDetectorService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectorService.class);

    private final DetectionProperties properties;
    private final DeviceSimulatorService deviceSimulatorService;
    private final AnomalyEventRepository anomalyEventRepository;
    private final Map<String, WelfordStats> statsByDevice = new ConcurrentHashMap<>();
    private final Sinks.Many<AnomalyResult> sink;
    private final Counter evaluatedCounter;
    private final Counter anomalyCounter;
    private final Counter resultsDroppedCounter;

    public AnomalyDetectorService(DetectionProperties properties,
                                   DeviceSimulatorService deviceSimulatorService,
                                   AnomalyEventRepository anomalyEventRepository,
                                   MeterRegistry meterRegistry) {
        this.properties = properties;
        this.deviceSimulatorService = deviceSimulatorService;
        this.anomalyEventRepository = anomalyEventRepository;
        // SSE로 나가는 "지금 상태" 스트림이라 구독자가 없던 동안의 과거분을 쌓아둘 이유가 없다.
        // onBackpressureBuffer는 autoCancel=false와 맞물려 구독자가 없을 때도 계속 버퍼링하다가,
        // 새 클라이언트가 붙는 순간 쌓인 옛날 이벤트를 한꺼번에 쏟아내는 버그가 있었다(대시보드 "초당 이벤트" 폭주).
        // directBestEffort는 버퍼링 없이 그 순간 받을 준비가 된 구독자에게만 전달하고, 없으면 그냥 흘려보낸다.
        this.sink = Sinks.many().multicast().directBestEffort();
        this.evaluatedCounter = meterRegistry.counter("monitor.detection.readings.evaluated");
        this.anomalyCounter = meterRegistry.counter("monitor.detection.anomalies.total");
        this.resultsDroppedCounter = meterRegistry.counter("monitor.detection.results.dropped");
    }

    @PostConstruct
    void subscribeToReadings() {
        deviceSimulatorService.readingsStream()
                .map(this::evaluate)
                .subscribe(this::publish);
    }

    private void publish(AnomalyResult result) {
        Sinks.EmitResult emitResult = sink.tryEmitNext(result);
        // 구독자가 아예 없는 건(대시보드를 아무도 안 보고 있음) 정상 상태이지 드롭이 아니다.
        // 그 외 실패(느린 구독자가 못 받아간 경우 등)만 진짜 드롭으로 집계한다.
        if (emitResult.isFailure() && emitResult != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            resultsDroppedCounter.increment();
            log.warn("results sink emit failed ({}), dropped event for {}", emitResult, result.deviceId());
        }
        if (result.anomaly()) {
            handleAnomaly(result);
        }
    }

    private void handleAnomaly(AnomalyResult result) {
        anomalyCounter.increment();
        log.warn("ANOMALY detected deviceId={} value={} mean={} stdDev={} zScore={} timestamp={}",
                result.deviceId(), result.value(), result.mean(), result.stdDev(),
                result.zScore(), result.timestamp());

        AnomalyEventEntity entity = new AnomalyEventEntity(
                result.deviceId(), result.value(), result.mean(), result.stdDev(),
                result.zScore(), result.timestamp());

        // 저장은 fire-and-forget으로: 이상치 처리 흐름이 DB 쓰기 지연 때문에 막히면 안 된다.
        anomalyEventRepository.save(entity).subscribe(
                saved -> { },
                error -> log.error("failed to persist anomaly event for {}", result.deviceId(), error)
        );
    }

    private AnomalyResult evaluate(SensorReading reading) {
        evaluatedCounter.increment();
        WelfordStats stats = statsByDevice.computeIfAbsent(reading.deviceId(), id -> new WelfordStats());

        double meanBefore;
        double stdDevBefore;
        double zScore;
        long countAfter;
        synchronized (stats) {
            meanBefore = stats.getMean();
            stdDevBefore = stats.getStdDev();
            zScore = stats.zScore(reading.value());
            stats.update(reading.value());
            countAfter = stats.getCount();
        }

        boolean anomaly = countAfter >= properties.minSamplesForDetection()
                && Math.abs(zScore) > properties.zScoreThreshold();

        return new AnomalyResult(
                reading.deviceId(),
                reading.value(),
                meanBefore,
                stdDevBefore,
                zScore,
                anomaly,
                reading.timestamp()
        );
    }

    public Flux<AnomalyResult> resultsStream() {
        return sink.asFlux();
    }
}
