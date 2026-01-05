package core;

import alarm.Alarm;
import alarm.AlarmSeverity;
import commands.*;
import commands.securityhubcommands.*;
import devices.api.Device;
import devices.model.DeviceStatus;
import infrastructure.logger.*;
import infrastructure.repository.AlarmLogRepository;
import infrastructure.repository.DeviceRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// TODO: Stop injecting the repositories, use stateless service layers!

public class SecurityHub {
    private static SecurityHub hub;
    private final Logger logger;
    private final List<Device> devices = new CopyOnWriteArrayList<>();
    private final List<Device> decommissionedDevices = new ArrayList<>();
    private final Deque<SecurityHubCommand> taskQueue = new ArrayDeque<>();
    private final Map<UUID, Alarm> activeAlarms = new ConcurrentHashMap<>();
    private HubStatus status = HubStatus.DISARMED;
    private final boolean isMaintenanceMode = false;
    private AlarmLogRepository alarmRepo;
    private DeviceRepository deviceRepo;

    private SecurityHub() {
        this.logger = new TimestampLogger(new ConsoleLogger());
    }

    public void addDevice(Device device) {
        devices.add(device);
        logger.log( "A new " + device.getDeviceType() + " device (ID: " + device.getId() + ") was added to the devices list.", LogLevel.INFO);

        if (deviceRepo != null) {
            deviceRepo.save(device);
        } else {
            logger.log("Warning: Device added but Repository not initialized.", LogLevel.WARNING);
        }
    }

    public void removeDevice(Device device) {
        if (devices.contains(device)) {
            devices.remove(device);
        } else {
            logger.log("An attempt was made to remove a nonexistent device.", LogLevel.WARNING);
        }
    }

    public void setAlarmRepository(AlarmLogRepository repo) {
        this.alarmRepo = repo;
    }

    public void setDeviceRepo(DeviceRepository repo) {
        this.deviceRepo = repo;
    }

    public Logger getLogger() {
        return logger;
    }

    public HubStatus currentMode() {
        return status;
    }

    public void processNextCommand() {
        if (!taskQueue.isEmpty()) {
            taskQueue.poll().execute();
        }
        if (taskQueue.isEmpty()) {
            logger.log("The commands queue is now empty. All commands processed.", LogLevel.INFO);
        }
    }

    public void processAllCommands() {
        if (status != HubStatus.ARMED) {
            logger.log("Hub must be armed to process commands", LogLevel.WARNING);
            return;
        }

        while (!taskQueue.isEmpty()) {
            processNextCommand();
        }
    }

    public void processCommand(Command cmd) {
        if (status != HubStatus.ARMED) {
            logger.log("Hub must be armed to process commands", LogLevel.WARNING);
            return;
        }
        cmd.execute();
    }

    public void registerAlarm(Alarm alarm) {
        activeAlarms.put(alarm.getDeviceId(), alarm);
        if (alarmRepo != null) {
            alarmRepo.save(alarm);
        } else {
            logger.log("Warning: Alarm triggered but Repository not initialized.", LogLevel.WARNING);
        }

        logger.log("Alarm registered: " + alarm.getDeviceId(), LogLevel.INFO);
    }

    public void resolveAlarm(Alarm alarm) {
        if (status != HubStatus.ARMED) {
            logger.log("Hub must be armed to resolve active alarms", LogLevel.WARNING);
            return;
        }
        alarm.resolveAlarm();
        activeAlarms.remove(alarm.getDeviceId());
    }

    public void resolveAlarmsByDevice(Device device) {
        if (status != HubStatus.ARMED) {
            logger.log("Hub must be armed to resolve active alarms", LogLevel.WARNING);
            return;
        }
        activeAlarms.remove(device.getId());
    }

    public void resolveAllAlarms() {
        if (status != HubStatus.ARMED) {
            logger.log("Hub must be armed to resolve active alarms", LogLevel.WARNING);
            return;
        }
        activeAlarms.clear();
    }

    public void armHub() {
        if (status == HubStatus.ARMED) {
            logger.log("Hub is already armed. Current Status: ARMED", LogLevel.INFO);
        } else {
            status = HubStatus.ARMED;
            logger.log("Successfully armed hub. Current Status: ARMED", LogLevel.INFO);
        }
    }

    public void disarmHub() {
        if (status == HubStatus.DISARMED) {
            logger.log("Hub is already disarmed. Current Status: DISARMED", LogLevel.INFO);
        } else {
            status = HubStatus.DISARMED;
            logger.log("Successfully disarmed hub. Current Status: DISARMED", LogLevel.INFO);
        }
    }

    public void addCommand(SecurityHubCommand cmd) {
        if (!cmd.validate()) {
            logger.log(cmd.validationMessage(), LogLevel.ERROR);
            return;
        }

        if (cmd.getCommandType() == CommandType.RESET || cmd.getCommandType() == CommandType.DIAGNOSTIC) {
            taskQueue.add(cmd);
            return;
        }

        if (cmd.getDevice().getDeviceStatus() != DeviceStatus.OPERATIONAL) {
            logger.log("Command unsuccessful, must target an operational device.", LogLevel.ERROR);
            return;
        }

        if (status == HubStatus.DISARMED) {
            logger.log("The hub must be armed to trigger it. Current Status: DISARMED", LogLevel.WARNING);

        } else {
            if (cmd.getSeverity() != AlarmSeverity.LOW && !isMaintenanceMode) {
                taskQueue.add(cmd);
                logger.log("Successfully added security hub commands.", LogLevel.INFO);
            }
        }
    }

    /** Use this instead of individually calling addCommand and processCommand,
     * both of which are for primarily for debugging.
     */
    public void executeCommand(SecurityHubCommand cmd) {
        addCommand(cmd);
        processCommand(cmd);
    }

    public void initiateFleetCheck() {
        logger.log("Initiated fleet check. Any errors will show below.", LogLevel.HEALTH);
        for (Device d : devices) {
            addCommand(new SystemDiagnosticCommand(d));
        }
    }

    public int numActiveAlarms() {
        return activeAlarms.size();
    }

    public List<Alarm> getActiveAlarms() {
        return activeAlarms.values().stream().toList();
    }

    public void pingDevices() {
        for (Device d : devices) {
            d.ping();
        }
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void checkDeviceBatteryLevels() {
        int numLowBatteriedDevices = 0;

        for (Device d : devices) {
            if (d.getBatteryLife() < 10) {
                logger.log(d.getId() + " is low on charge", LogLevel.HEALTH);
                numLowBatteriedDevices++;
            }
        }
        logger.log("There are " + numLowBatteriedDevices + " low batteried devices.", LogLevel.HEALTH);
    }

    public void monitorAndHandleDeviceHealth() {
        updateDeviceFailureState();
        updateDeviceStatus();
    }

    private void updateDeviceFailureState() {
        for (Device d : devices) {
            if (!d.ping()) {
                d.incrementFailureCount();
                logger.log("Device ping failed", LogLevel.WARNING);
            } else {
                d.resetFailureCount();
            }
        }
    }

    public void removeAllDevices() {
        devices.clear();

        if (deviceRepo != null) {
            deviceRepo.removeAll();
        }
    }

    private void updateDeviceStatus() {
        List<Device> devicesToDecommission = new ArrayList<>();

        for (Device d : devices) {
            int numFails = d.getFailureCount();

            if (numFails == 1 || numFails == 2) {
                logger.log("Device failure count is " + numFails, LogLevel.WARNING);
            } else if (numFails == 3) {
                d.setDeviceStatus(DeviceStatus.RECOVERY_MODE);
                logger.log("Device failure count is " + numFails + ". The device is in " + d.getDeviceStatus(), LogLevel.CRITICAL);
                taskQueue.addFirst(new SystemDiagnosticCommand(d));
            } else if (numFails == 4) {
                logger.log("Device failure count is " + numFails + ". The device is in " + d.getDeviceStatus(), LogLevel.CRITICAL);
                taskQueue.addFirst(new SystemResetCommand(d));
            } else if (numFails == 5) {
                d.setDeviceStatus(DeviceStatus.DECOMMISSIONED);
                devicesToDecommission.add(d);
                decommissionedDevices.add(d);
                logger.log("Device failure count is " + numFails + ". The device is in " + d.getDeviceStatus(), LogLevel.CRITICAL);
            }
        }
        devices.removeIf(devicesToDecommission::contains);
    }

    public static synchronized SecurityHub getInstance() {
        if (hub == null) {
            hub = new SecurityHub();
        }
        return hub;
    }
}
