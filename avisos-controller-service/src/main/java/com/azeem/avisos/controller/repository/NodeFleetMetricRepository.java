/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.avisos.controller.repository;

import com.azeem.avisos.controller.model.node.FleetMetricRecord;
import com.azeem.avisos.controller.model.node.FleetMetrics;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterConstructorMapper(NodeFleetMetricRepository.NodeFleetMetricRow.class)
public interface NodeFleetMetricRepository {

  @SqlUpdate(
      """
            CREATE TABLE IF NOT EXISTS node_fleet_metrics (
                timestamp TIMESTAMP PRIMARY KEY,
                total_nodes_evaluated INTEGER NOT NULL,
                responsive_ratio REAL NOT NULL,
                battery_above50_ratio REAL NOT NULL,
                avg_seconds_since_last_seen REAL NOT NULL
            )
            """)
  void initNodeFleetMetricTable();

  @SqlUpdate(
      """
            INSERT INTO node_fleet_metrics (
                timestamp,
                total_nodes_evaluated,
                responsive_ratio,
                battery_above50_ratio,
                avg_seconds_since_last_seen
            )
            VALUES (
                :timestamp,
                :totalNodesEvaluated,
                :responsiveRatio,
                :batteryAbove50Ratio,
                :avgSecondsSinceLastSeen
            )
            """)
  void saveMetric(
      @Bind("timestamp") Instant timestamp,
      @Bind("totalNodesEvaluated") long totalNodesEvaluated,
      @Bind("responsiveRatio") double responsiveRatio,
      @Bind("batteryAbove50Ratio") double batteryAbove50Ratio,
      @Bind("avgSecondsSinceLastSeen") double avgSecondsSinceLastSeen);

  default void saveMetric(FleetMetricRecord metric) {
    FleetMetrics fleetMetrics = metric.fleetMetrics();

    saveMetric(
        metric.timestamp(),
        fleetMetrics.totalNodesEvaluated(),
        fleetMetrics.responsiveRatio(),
        fleetMetrics.batteryAbove50Ratio(),
        fleetMetrics.avgSecondsSinceLastSeen());
  }

  @SqlQuery(
      """
            SELECT
                timestamp,
                total_nodes_evaluated,
                responsive_ratio,
                battery_above50_ratio,
                avg_seconds_since_last_seen
            FROM node_fleet_metrics
            ORDER BY timestamp DESC
            """)
  List<NodeFleetMetricRow> getMetricRows();

  default List<FleetMetricRecord> getMetrics() {
    return getMetricRows().stream()
        .map(
            row ->
                new FleetMetricRecord(
                    row.timestamp(),
                    new FleetMetrics(
                        row.totalNodesEvaluated(),
                        row.responsiveRatio(),
                        row.batteryAbove50Ratio(),
                        row.avgSecondsSinceLastSeen())))
        .toList();
  }

  @SqlQuery(
      """
            SELECT
                timestamp,
                total_nodes_evaluated,
                responsive_ratio,
                battery_above50_ratio,
                avg_seconds_since_last_seen
            FROM node_fleet_metrics
            ORDER BY timestamp DESC
            LIMIT :limit
            """)
  List<NodeFleetMetricRow> getMetricRows(@Bind("limit") int limit);

  default List<FleetMetricRecord> getMetrics(int limit) {
    return getMetricRows(limit).stream()
        .map(
            row ->
                new FleetMetricRecord(
                    row.timestamp(),
                    new FleetMetrics(
                        row.totalNodesEvaluated(),
                        row.responsiveRatio(),
                        row.batteryAbove50Ratio(),
                        row.avgSecondsSinceLastSeen())))
        .toList();
  }

  @SqlQuery(
      """
        SELECT
            timestamp,
            total_nodes_evaluated,
            responsive_ratio,
            battery_above50_ratio,
            avg_seconds_since_last_seen
        FROM node_fleet_metrics
        ORDER BY timestamp DESC
        LIMIT 1
        """)
  NodeFleetMetricRow getLatestMetricRow();

  default Optional<FleetMetricRecord> getLatestMetric() {
    NodeFleetMetricRow row = getLatestMetricRow();

    if (row == null) {
      return Optional.empty();
    }

    return Optional.of(
        new FleetMetricRecord(
            row.timestamp(),
            new FleetMetrics(
                row.totalNodesEvaluated(),
                row.responsiveRatio(),
                row.batteryAbove50Ratio(),
                row.avgSecondsSinceLastSeen())));
  }

  record NodeFleetMetricRow(
      Instant timestamp,
      @ColumnName("total_nodes_evaluated") long totalNodesEvaluated,
      @ColumnName("responsive_ratio") double responsiveRatio,
      @ColumnName("battery_above50_ratio") double batteryAbove50Ratio,
      @ColumnName("avg_seconds_since_last_seen") double avgSecondsSinceLastSeen) {}
}
