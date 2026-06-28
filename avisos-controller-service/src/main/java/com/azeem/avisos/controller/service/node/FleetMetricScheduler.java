/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.avisos.controller.service.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FleetMetricScheduler {

  private static final Logger log = LoggerFactory.getLogger(FleetMetricScheduler.class);
  private final RestClient restClient;
  private final FleetMetricService fleetMetricService;

  public FleetMetricScheduler(FleetMetricService fleetMetricService) {
    this.fleetMetricService = fleetMetricService;
    this.restClient =
        RestClient.builder()
            .requestFactory(new SimpleClientHttpRequestFactory())
            .baseUrl("http://fleet-anomaly-detection-api:8000")
            .build();
  }

  @Scheduled(fixedRate = 60000)
  public void syncFleetHealth() {
    var metrics = fleetMetricService.getLatestMetrics();

    metrics.ifPresent(
        m -> {
          try {
            restClient.post().uri("/fleet-health").body(m).retrieve().toBodilessEntity();
          } catch (Exception e) {
            log.error("Failed to sync with Python: " + e.getMessage());
          }
        });
  }
}
