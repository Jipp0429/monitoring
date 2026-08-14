package com.example.monitor.web;

import com.example.monitor.persistence.AnomalyEventEntity;
import com.example.monitor.persistence.AnomalyEventRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * DB에 영속화된 이상치 이력을 조회하는 API.
 */
@RestController
@RequestMapping("/api/anomalies")
public class AnomalyHistoryController {

    private final AnomalyEventRepository anomalyEventRepository;

    public AnomalyHistoryController(AnomalyEventRepository anomalyEventRepository) {
        this.anomalyEventRepository = anomalyEventRepository;
    }

    @GetMapping("/recent")
    public Flux<AnomalyEventEntity> recent(@RequestParam(defaultValue = "50") int limit) {
        return anomalyEventRepository.findAllByOrderByDetectedAtDesc().take(limit);
    }

    @GetMapping("/device/{deviceId}")
    public Flux<AnomalyEventEntity> byDevice(@PathVariable String deviceId) {
        return anomalyEventRepository.findByDeviceIdOrderByDetectedAtDesc(deviceId);
    }
}
