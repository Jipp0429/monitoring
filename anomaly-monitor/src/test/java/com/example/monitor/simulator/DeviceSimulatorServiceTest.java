package com.example.monitor.simulator;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceSimulatorServiceTest {

    private static SimulatorProperties defaultProperties() {
        return new SimulatorProperties(50.0, 5.0, 0.03, 40.0, 100, 65536);
    }

    @Test
    void tickEmitsOneReadingPerActiveDevice() {
        DeviceSimulatorService service = new DeviceSimulatorService(defaultProperties());
        service.setDeviceCount(5);

        StepVerifier.create(service.readingsStream().take(5))
                .then(service::tick)
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    void setDeviceCountClampsNegativeValuesToZero() {
        DeviceSimulatorService service = new DeviceSimulatorService(defaultProperties());

        assertThat(service.setDeviceCount(-10)).isZero();
        assertThat(service.getDeviceCount()).isZero();
    }

    @Test
    void setDeviceCountReturnsClampedValue() {
        DeviceSimulatorService service = new DeviceSimulatorService(defaultProperties());

        assertThat(service.setDeviceCount(250)).isEqualTo(250);
        assertThat(service.getDeviceCount()).isEqualTo(250);
    }

    @Test
    void tickWithZeroDevicesEmitsNothing() {
        DeviceSimulatorService service = new DeviceSimulatorService(defaultProperties());
        service.setDeviceCount(0);

        StepVerifier.create(service.readingsStream())
                .then(service::tick)
                .expectNoEvent(Duration.ofMillis(200))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}
