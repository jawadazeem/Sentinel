package service.system;

import core.HubStatus;
import core.SecurityHub;
import devices.api.Device;
import devices.model.DeviceStatus;

import java.time.Instant;

/**
 * The `SystemHealthService` class provides a stateless service layer for monitoring
 * the overall health and status of the system. It interacts with the `SecurityHub`
 * to gather data about devices, alarms, and system modes, and calculates metrics
 * such as fleet health percentage, alarm density, and system health status.
 *
 * <p>This class is architecturally a singleton and is designed to be lightweight
 * and stateless.</p>
 */
public class SystemHealthService {
    private final SecurityHub hub; // The core security hub instance for accessing system data

    /**
     * Constructs a `SystemHealthService` instance with the specified `SecurityHub`.
     *
     * @param hub The `SecurityHub` instance used to retrieve system data.
     */
    public SystemHealthService(SecurityHub hub) {
        this.hub = hub;
    }

    /**
     * Retrieves a snapshot of the current system state, including fleet health,
     * active alarms, alarm density, system health status, system mode, and the
     * timestamp of the snapshot.
     *
     * @return A `SystemSnapshot` object containing the current system state.
     */
    public SystemSnapshot getSystemSnapshot() {
        return new SystemSnapshot(
                getFleetHealthPercentage(),
                getActiveAlarmCount(),
                getAlarmDensity(),
                getSystemHealthStatus(),
                getSystemMode(),
                Instant.now()
        );
    }

    /**
     * Retrieves the current mode of the system from the `SecurityHub`.
     *
     * @return The current `HubStatus` of the system.
     */
    private HubStatus getSystemMode() {
        return hub.currentMode();
    }

    /**
     * Calculates the total number of active alarms in the system.
     *
     * @return The count of active alarms.
     */
    private int getActiveAlarmCount() {
        return hub.getActiveAlarms().size();
    }

    /**
     * Calculates the alarm density, defined as the ratio of active alarms to a
     * fixed denominator (1000).
     *
     * @return The alarm density as a double value.
     */
    private double getAlarmDensity() {
        return (double)hub.getActiveAlarms().size() / 1000;
    }

    /**
     * Calculates the fleet health percentage based on the status of devices in
     * the system. Devices in `OPERATIONAL` or `AWAY` status are considered healthy.
     *
     * @return The fleet health percentage as a double value between 0.0 and 1.0.
     */
    private double getFleetHealthPercentage() {
        double healthyDevices = 0.0;
        double total = 0.0;
        DeviceStatus ds;

        for (Device d : hub.getDevices()) {
            ds = d.getDeviceStatus();
            if (ds == DeviceStatus.OPERATIONAL || ds == DeviceStatus.AWAY) {
                total++;
                healthyDevices++;
            } else if (ds == DeviceStatus.DECOMMISSIONED || ds == DeviceStatus.RECOVERY_MODE) {
                total++;
            }
        }
        if (total == 0) return 1.0;
        return healthyDevices / total;
    }

    /**
     * Determines the overall system health status based on the fleet health
     * percentage. The status is categorized as `HEALTHY`, `SUB_HEALTHY`, or
     * `FAILURE` based on predefined thresholds.
     *
     * @return The `SystemHealthStatus` representing the overall system health.
     */
    private SystemHealthStatus getSystemHealthStatus() {
        double health = getFleetHealthPercentage();
        if (health > 0.90) {
            return SystemHealthStatus.HEALTHY;
        } else if (health < 0.70) {
            return SystemHealthStatus.FAILURE;
        } else {
            return SystemHealthStatus.SUB_HEALTHY;
        }
    }
}