package service.system;

import core.HubStatus;

import java.time.Instant;

/**
 * Data transfer object. Represents the health of the entire system at a given moment.
 */
public class SystemSnapshot {
    private final double fleetHealthPercentage;
    private final int activeAlarmCount;
    private final double alarmDensity;
    private final SystemHealthStatus systemHealthStatus;
    private final HubStatus systemMode;
    private final Instant timestamp;

    public SystemSnapshot(double fleetHealthPercentage,
                          int activeAlarmCount,
                          double alarmDensity,
                          SystemHealthStatus systemHealthStatus,
                          HubStatus systemMode,
                          Instant timestamp) {

        this.fleetHealthPercentage = fleetHealthPercentage;
        this.activeAlarmCount = activeAlarmCount;
        this.alarmDensity = alarmDensity;
        this.systemHealthStatus = systemHealthStatus;
        this.systemMode = systemMode;
        this.timestamp = timestamp;
    }

    public double getFleetHealthPercentage() {
        return fleetHealthPercentage;
    }

    public int getActiveAlarmCount() {
        return activeAlarmCount;
    }

    public double getAlarmDensity() {
        return alarmDensity;
    }

    public SystemHealthStatus getSystemHealthStatus() {
        return systemHealthStatus;
    }

    public HubStatus getSystemMode() {
        return systemMode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "SystemSnapshot{" +
                "fleetHealthPercentage=" + fleetHealthPercentage +
                ", activeAlarmCount=" + activeAlarmCount +
                ", alarmDensity=" + alarmDensity +
                ", systemHealthStatus=" + systemHealthStatus +
                ", systemMode=" + systemMode +
                ", timestamp=" + timestamp +
                '}';
    }
}