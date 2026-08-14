package com.example.monitor.detection;

import com.example.monitor.persistence.AnomalyEventEntity;
import com.example.monitor.persistence.AnomalyEventRepository;
import com.example.monitor.simulator.DeviceSimulatorService;
import com.example.monitor.simulator.SensorReading;
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

    public AnomalyDetectorService(DetectionProperties properties,
                                   DeviceSimulatorService deviceSimulatorService,
                                   AnomalyEventRepository anomalyEventRepository) {
        this.properties = properties;
        this.deviceSimulatorService = deviceSimulatorService;
        this.anomalyEventRepository = anomalyEventRepository;
        this.sink = Sinks.many().multicast().onBackpressureBuffer(properties.sinkBufferSize(), false);
    }

    @PostConstruct
    void subscribeToReadings() {
        deviceSimulatorService.readingsStream()
                .map(this::evaluate)
                .subscribe(this::publish);
    }

    private void publish(AnomalyResult result) {
        Sinks.EmitResult emitResult = sink.tryEmitNext(result);
        if (emitResult.isFailure()) {
            log.warn("results sink buffer overflow, dropped event for {}", result.deviceId());
        }
        if (result.anomaly()) {
            handleAnomaly(result);
        }
    }

    private void handleAnomaly(AnomalyResult result) {
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
