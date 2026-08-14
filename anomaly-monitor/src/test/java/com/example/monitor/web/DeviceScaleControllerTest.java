package com.example.monitor.web;

import com.example.monitor.simulator.DeviceSimulatorService;
import com.example.monitor.simulator.SimulatorProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceScaleControllerTest {

    private static DeviceScaleController newController() {
        SimulatorProperties properties = new SimulatorProperties(50.0, 5.0, 0.03, 40.0, 100, 20000, 65536);
        DeviceSimulatorService simulator = new DeviceSimulatorService(properties, new SimpleMeterRegistry());
        return new DeviceScaleController(simulator);
    }

    @Test
    void getCountReturnsCurrentDeviceCount() {
        assertThat(newController().getCount().count()).isEqualTo(100);
    }

    @Test
    void setCountUpdatesAndReturnsNewValue() {
        DeviceScaleController controller = newController();

        assertThat(controller.setCount(500).count()).isEqualTo(500);
        assertThat(controller.getCount().count()).isEqualTo(500);
    }

    @Test
    void setCountClampsNegativeToZero() {
        assertThat(newController().setCount(-10).count()).isZero();
    }

    @Test
    void setCountClampsAboveConfiguredMaximum() {
        assertThat(newController().setCount(999_999).count()).isEqualTo(20000);
    }
}
