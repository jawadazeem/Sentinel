/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.avisos.controller.service.node;

import com.azeem.avisos.controller.model.node.FleetAnomalyReport;
import com.azeem.avisos.controller.model.node.FleetMetricRecord;
import com.azeem.avisos.controller.model.node.FleetMetrics;
import com.azeem.avisos.controller.model.node.NodeRecord;
import com.azeem.avisos.controller.model.node.NodeStatus;
import com.azeem.avisos.controller.repository.NodeFleetMetricRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FleetMetricService {

  private final NodeFleetMetricRepository repository;
  private final NodeService nodeService;
  private AtomicReference<FleetAnomalyReport> latestFleetAnomalyReport = new AtomicReference<>();

  public FleetMetricService(NodeFleetMetricRepository repository, NodeService nodeService) {
    this.repository = repository;
    this.nodeService = nodeService;
  }

  public void updateAnomalyReport(FleetAnomalyReport report) {
    latestFleetAnomalyReport.set(report);
  }

  /** latestFleetAnomalyReport only holds the latest, most up to date, report */
  public Optional<FleetAnomalyReport> getLatestAnomalyReport() {
    return Optional.ofNullable(latestFleetAnomalyReport.get());
  }

  // Compute node fleet health metrics every minute
  @Scheduled(fixedRate = 60000)
  public void generateLatestMetrics() {
    Instant now = Instant.now();

    List<NodeRecord> nodes =
        nodeService.getRegisteredNodes().stream()
            .map(nodeService::getNode)
            .flatMap(Optional::stream)
            .toList();

    FleetMetrics metrics =
        new FleetMetrics(
            nodes.size(),
            calculateResponsiveRatio(nodes),
            calculateBatteryAbove50Ratio(nodes),
            calculateAverageSecondsSinceLastSeen(nodes, now));

    FleetMetricRecord record = new FleetMetricRecord(now, metrics);

    saveMetric(record);
  }

  public List<FleetMetricRecord> getMetrics() {
    return repository.getMetrics();
  }

  public List<FleetMetricRecord> getMetrics(int limit) {
    return repository.getMetrics(limit);
  }

  public Optional<FleetMetricRecord> getLatestMetrics() {
    return repository.getLatestMetric();
  }

  private void saveMetric(FleetMetricRecord metric) {
    repository.saveMetric(metric);
  }

  private double calculateResponsiveRatio(List<NodeRecord> nodes) {

    if (nodes.isEmpty()) {
      return 0.0;
    }

    long responsiveCount =
        nodes.stream().filter(node -> node.status() == NodeStatus.RESPONSIVE).count();

    return (double) responsiveCount / nodes.size();
  }

  private double calculateBatteryAbove50Ratio(List<NodeRecord> nodes) {

    if (nodes.isEmpty()) {
      return 0.0;
    }

    long count = nodes.stream().filter(node -> node.batteryLevel() > 50.0).count();

    return (double) count / nodes.size();
  }

  private double calculateAverageSecondsSinceLastSeen(List<NodeRecord> nodes, Instant now) {

    if (nodes.isEmpty()) {
      return 0.0;
    }

    return nodes.stream()
        .mapToLong(
            node ->
                Duration.between(node.lastSeen().atZone(ZoneId.systemDefault()).toInstant(), now)
                    .getSeconds())
        .average()
        .orElse(0.0);
  }
}
