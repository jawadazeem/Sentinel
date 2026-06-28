/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.avisos.controller.web.api;

import com.azeem.avisos.controller.model.node.FleetAnomalyReport;
import com.azeem.avisos.controller.model.node.FleetMetricRecord;
import com.azeem.avisos.controller.service.node.FleetMetricService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

/** REST API for querying historical fleet-wide telemetry metrics. */
@RestController
@RequestMapping("/api/fleet-metrics")
public class FleetMetricController {

  private final FleetMetricService fleetMetricService;
  private final RestClient restClient;

  public FleetMetricController(FleetMetricService fleetMetricService) {
    this.fleetMetricService = fleetMetricService;
    this.restClient =
        RestClient.builder()
            .requestFactory(new SimpleClientHttpRequestFactory())
            .baseUrl("http://fleet-anomaly-detection-api:8000")
            .build();
  }

  @GetMapping
  public List<FleetMetricRecord> getFleetMetrics() {
    return fleetMetricService.getMetrics();
  }

  @GetMapping("/metrics/latest")
  public ResponseEntity<FleetMetricRecord> getLatestMetric() {
    return fleetMetricService
        .getLatestMetrics()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/anomaly-report/latest")
  public ResponseEntity<FleetAnomalyReport> getLatestAnomalyReport() {
    return fleetMetricService
        .getLatestAnomalyReport()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/anomaly-report/latest")
  public ResponseEntity<Void> updateAnomalyReport(@RequestBody FleetAnomalyReport report) {
    fleetMetricService.updateAnomalyReport(report);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/analyze")
  public ResponseEntity<String> analyzeData(@Valid @RequestBody FleetMetricRecord metrics) {
    restClient.post().uri("/fleet-health").body(metrics).retrieve().toBodilessEntity();
    return ResponseEntity.accepted().build();
  }
}
