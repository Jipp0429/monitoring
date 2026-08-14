package com.example.monitor.web;

import com.example.monitor.simulator.DeviceSimulatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시뮬레이션 중인 디바이스 수를 런타임에 조절하는 API (스케일 테스트용).
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceScaleController {

    private final DeviceSimulatorService deviceSimulatorService;

    public DeviceScaleController(DeviceSimulatorService deviceSimulatorService) {
        this.deviceSimulatorService = deviceSimulatorService;
    }

    @GetMapping("/scale")
    public DeviceCountResponse getCount() {
        return new DeviceCountResponse(deviceSimulatorService.getDeviceCount());
    }

    @PutMapping("/scale")
    public DeviceCountResponse setCount(@RequestParam int count) {
        return new DeviceCountResponse(deviceSimulatorService.setDeviceCount(count));
    }

    public record DeviceCountResponse(int count) {
    }
}
