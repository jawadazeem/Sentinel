package commands.alarmcommands;

import alarm.Alarm;
import alarm.AlarmSeverity;
import alarm.AlarmStatus;
import commands.CommandType;
import core.SecurityHub;
import devices.api.Device;

import java.time.LocalDateTime;

public class PanicAlarmCommand implements AlarmCommand {
    private Device device;
    private String message;
    private CommandType commandType = CommandType.HIGH_PRIORITY_ALARM;

    public PanicAlarmCommand(Device device, String message) {
        this.device = device;
        this.message = message;
    }

    @Override
    public void execute() {
        SecurityHub.getInstance().registerAlarm(new Alarm(device.getDeviceType(), device.getId(), AlarmStatus.ACTIVE, AlarmSeverity.CRITICAL, LocalDateTime.now()));
        device.updateAllSubscribers(message);
    }

    @Override
    public AlarmSeverity getSeverity() {
        return AlarmSeverity.CRITICAL;
    }

    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public CommandType getCommandType() {
        return commandType;
    }

    @Override
    public boolean validate() {
        return !validationMessage().contains("error");
    }

    @Override
    public String validationMessage() {
        if (device.getBatteryLife() < 5 && (message == null || message.isBlank())) {
            return "Command validation error, the message you have provided is blank and the device's battery life is less than 5%.";
        }
        if (device.getBatteryLife() < 5) {
            return "Command validation error, the device's battery life is less than 5%.";
        }
        if (message.isBlank()) {
            return "Command validation error for Panic Command, the message you have provided is blank.";
        }
        return "Panic Command was validated successfully";
    }
}
