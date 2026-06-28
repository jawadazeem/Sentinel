/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.avisos.controller.model.node;

public record FleetMetrics(
    long totalNodesEvaluated,
    double responsiveRatio,
    double batteryAbove50Ratio,
    double avgSecondsSinceLastSeen) {}
