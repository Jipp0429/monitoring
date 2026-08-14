package com.example.monitor.simulator;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceSimulatorServiceTest {

    @Test
    void tickEmitsOneReadingPerActiveDevice() {
        DeviceSimulatorService service = new DeviceSimulatorService();
        service.setDeviceCount(5);

        StepVerifier.create(service.readingsStream().take(5))
                .then(service::tick)
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    void setDeviceCountClampsNegativeValuesToZero() {
        DeviceSimulatorService service = new DeviceSimulatorService();

        assertThat(service.setDeviceCount(-10)).isZero();
        assertThat(service.getDeviceCount()).isZero();
    }

    @Test
    void setDeviceCountReturnsClampedValue() {
        DeviceSimulatorService service = new DeviceSimulatorService();

        assertThat(service.setDeviceCount(250)).isEqualTo(250);
        assertThat(service.getDeviceCount()).isEqualTo(250);
    }

    @Test
    void tickWithZeroDevicesEmitsNothing() {
        DeviceSimulatorService service = new DeviceSimulatorService();
        service.setDeviceCount(0);

        StepVerifier.create(service.readingsStream())
                .then(service::tick)
                .expectNoEvent(Duration.ofMillis(200))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}
