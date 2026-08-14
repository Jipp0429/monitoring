package com.example.monitor.persistence;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AnomalyEventRepository의 테스트용 기본 구현체.
 * 모든 메서드가 기본적으로 UnsupportedOperationException을 던지므로,
 * 각 테스트는 실제로 필요한 메서드만 오버라이드해서 쓴다.
 */
public class UnsupportedAnomalyEventRepository implements AnomalyEventRepository {

    @Override
    public <S extends AnomalyEventEntity> Mono<S> save(S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Flux<AnomalyEventEntity> findAllByOrderByDetectedAtDesc() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Flux<AnomalyEventEntity> findByDeviceIdOrderByDetectedAtDesc(String deviceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends AnomalyEventEntity> Flux<S> saveAll(Iterable<S> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends AnomalyEventEntity> Flux<S> saveAll(Publisher<S> entityStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<AnomalyEventEntity> findById(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<AnomalyEventEntity> findById(Publisher<Long> id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Boolean> existsById(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Boolean> existsById(Publisher<Long> id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Flux<AnomalyEventEntity> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Flux<AnomalyEventEntity> findAllById(Iterable<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Flux<AnomalyEventEntity> findAllById(Publisher<Long> idStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Long> count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Void> deleteById(Publisher<Long> id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Void> delete(AnomalyEventEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Void> deleteAllById(Iterable<? extends Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends AnomalyEventEntity> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Void> deleteAll(Publisher<? extends AnomalyEventEntity> entityStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Void> deleteAll() {
        throw new UnsupportedOperationException();
    }
}
