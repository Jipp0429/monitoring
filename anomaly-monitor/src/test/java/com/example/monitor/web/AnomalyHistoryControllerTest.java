package com.example.monitor.web;

import com.example.monitor.persistence.AnomalyEventEntity;
import com.example.monitor.persistence.UnsupportedAnomalyEventRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyHistoryControllerTest {

    private static AnomalyEventEntity entity(String deviceId, Instant detectedAt) {
        return new AnomalyEventEntity(deviceId, 1.0, 0.0, 0.0, 0.0, detectedAt);
    }

    @Test
    void recentAppliesLimitOnTopOfRepositoryOrder() {
        List<AnomalyEventEntity> stored = List.of(
                entity("device-a", Instant.parse("2026-01-01T00:00:03Z")),
                entity("device-b", Instant.parse("2026-01-01T00:00:02Z")),
                entity("device-c", Instant.parse("2026-01-01T00:00:01Z"))
        );
        AnomalyHistoryController controller = new AnomalyHistoryController(new FakeRepository(stored));

        StepVerifier.create(controller.recent(2))
                .assertNext(e -> assertThat(e.getDeviceId()).isEqualTo("device-a"))
                .assertNext(e -> assertThat(e.getDeviceId()).isEqualTo("device-b"))
                .verifyComplete();
    }

    @Test
    void byDeviceDelegatesToDeviceScopedRepositoryQuery() {
        List<AnomalyEventEntity> stored = List.of(
                entity("device-x", Instant.now()),
                entity("device-y", Instant.now())
        );
        AnomalyHistoryController controller = new AnomalyHistoryController(new FakeRepository(stored));

        StepVerifier.create(controller.byDevice("device-x"))
                .assertNext(e -> assertThat(e.getDeviceId()).isEqualTo("device-x"))
                .verifyComplete();
    }

    /** 두 조회 메서드만 실제로 동작하고 나머지는 이 테스트에서 쓰이지 않는 최소 fake. */
    private static class FakeRepository extends UnsupportedAnomalyEventRepository {

        private final List<AnomalyEventEntity> data;

        FakeRepository(List<AnomalyEventEntity> data) {
            this.data = data;
        }

        @Override
        public Flux<AnomalyEventEntity> findAllByOrderByDetectedAtDesc() {
            return Flux.fromIterable(data);
        }

        @Override
        public Flux<AnomalyEventEntity> findByDeviceIdOrderByDetectedAtDesc(String deviceId) {
            return Flux.fromIterable(data).filter(e -> e.getDeviceId().equals(deviceId));
        }
    }
}
