package sim;

import alarm.AlarmSeverity;
import commands.alarmcommands.PanicAlarmCommand;
import commands.alarmcommands.TriggerAlarmCommand;
import core.HubStatus;
import core.SecurityHub;
import devices.api.Device;
import devices.model.DeviceStatus;
import devices.api.HardwareLink;
import infrastructure.logger.*;

import java.util.List;

public class SimulationEngine implements Runnable, HardwareLink {
    private final SecurityHub hub;
    private final List<Device> devices;
    private final Logger logger;

    public SimulationEngine(SecurityHub hub, Logger logger) {
        this.hub = hub;
        devices = hub.getDevices();
        this.logger = logger;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(10000); // The "Tick"
                lowerBatteries();
                changeSignalStrength();
                chargeBatteries();
                triggerAlarmsRandomly();
                triggerPanicAlarmsRandomly();
                // Tell the UI to refresh if needed
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void lowerBatteries() {
        devices.forEach((d) -> {
            if (d.getBatteryLife() >= 5) {
                d.setBatteryLife(d.getBatteryLife() - (int)(Math.random() * 5));
            } else {
                d.setBatteryLife(0);
            }
        });
    }

    public void chargeBatteries() {
        devices.forEach((d) -> {
            if (d.getBatteryLife() <= 95)
                d.setBatteryLife(d.getBatteryLife()+5);
        });
    }

    public void changeSignalStrength() {
        devices.forEach((d) -> {
            if (Math.random() > 0.5 && d.getSignalStrength() > -90) {
                d.setSignalStrength(d.getSignalStrength() - (int)(Math.random() * 10));
            } else if (d.getSignalStrength() <= -10) {
                d.setSignalStrength(d.getSignalStrength() + (int) (Math.random() * 10));
            }
        });
    }

    @Override
    public boolean isDeviceResponsive(Device device) {
        if (devices.contains(device)
                && device.getBatteryLife() > 0
                && device.getSignalStrength() >= -100
                && device.getDeviceStatus() == DeviceStatus.OPERATIONAL) {
            return Math.random() > 0.05;
        }
        return false;
    }

    /**
     * Simulates alarms being triggered by creating and executing TriggerAlarmCommand objects randomly
     */
    public void triggerAlarmsRandomly() {
        if (SecurityHub.getInstance().currentMode() == HubStatus.ARMED) {
            devices.stream().filter(d -> d.getDeviceStatus() == DeviceStatus.OPERATIONAL).forEach((d) -> {
                if (Math.random() > 0.7) {
                    AlarmSeverity severity;
                    double random = Math.random();
                    if (random < 0.3) {
                        severity = AlarmSeverity.LOW;
                    } else if (random > 0.6) {
                        severity = AlarmSeverity.MEDIUM;
                    } else {
                        severity = AlarmSeverity.HIGH;
                    }
                    TriggerAlarmCommand cmd = new TriggerAlarmCommand(d, "Alarm triggered", severity);
                    cmd.execute();
                }
            });
        }
    }

    /**
     * Simulates panic alarms being triggered by creating and executing PanicAlarmCommand objects randomly
     */
    public void triggerPanicAlarmsRandomly() {
        if (SecurityHub.getInstance().currentMode() == HubStatus.ARMED) {
            devices.stream()
                    .filter(d -> d.getDeviceStatus() == DeviceStatus.OPERATIONAL)
                    .forEach((d) -> {
                        // Throttle panic generation: only a small chance per tick per device
                        if (Math.random() > 0.995) { // ~0.5% chance
                            PanicAlarmCommand cmd = new PanicAlarmCommand(d, "Panic Alarm triggered");
                            cmd.execute();
                        }
                    });
        }
    }
}
