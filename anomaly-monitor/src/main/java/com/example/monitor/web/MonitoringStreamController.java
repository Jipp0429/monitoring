package com.example.monitor.web;

import com.example.monitor.detection.AnomalyDetectorService;
import com.example.monitor.detection.AnomalyResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class MonitoringStreamController {

    private final AnomalyDetectorService anomalyDetectorService;

    public MonitoringStreamController(AnomalyDetectorService anomalyDetectorService) {
        this.anomalyDetectorService = anomalyDetectorService;
    }

    @GetMapping(value = "/stream/readings", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AnomalyResult> streamReadings() {
        return anomalyDetectorService.resultsStream();
    }
}
