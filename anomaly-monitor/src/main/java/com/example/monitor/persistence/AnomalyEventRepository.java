package com.example.monitor.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AnomalyEventRepository extends ReactiveCrudRepository<AnomalyEventEntity, Long> {

    Flux<AnomalyEventEntity> findAllByOrderByDetectedAtDesc();

    Flux<AnomalyEventEntity> findByDeviceIdOrderByDetectedAtDesc(String deviceId);
}
