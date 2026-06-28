/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.avisos.controller.model.node;

import java.time.Instant;

/** Returned by the Python anomaly detection microservice */
public record FleetAnomalyReport(
    Long id,
    Instant timestamp,
    boolean isAnomaly,
    String diagnosticReason,
    FleetMetrics rawPayload) {}
