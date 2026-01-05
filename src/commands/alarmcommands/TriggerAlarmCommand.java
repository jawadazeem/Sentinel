package commands.alarmcommands;

import alarm.Alarm;
import alarm.AlarmSeverity;
import alarm.AlarmStatus;
import commands.CommandType;
import core.SecurityHub;
import devices.api.Device;

import java.time.LocalDateTime;

public class TriggerAlarmCommand implements AlarmCommand {
    private Device device;
    private String message;
    private AlarmSeverity alarmSeverity;
    private CommandType commandType = CommandType.ALARM;

    public TriggerAlarmCommand(Device device, String message, AlarmSeverity alarmSeverity) {
        this.device = device;
        this.message = message;
        this.alarmSeverity = alarmSeverity;
    }

    @Override
    public void execute() {
        SecurityHub.getInstance().registerAlarm(new Alarm(device.getDeviceType(), device.getId(), AlarmStatus.ACTIVE, alarmSeverity, LocalDateTime.now()));
        device.updateAllSubscribers(message);
    }

    @Override
    public AlarmSeverity getSeverity() {
        return alarmSeverity;
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
        return !message.isBlank();
    }

    @Override
    public String validationMessage() {
        if (message.isBlank()) {
            return "Command validation error for Trigger Alarm Command, the message you have provided is blank.";
        }
        return "Trigger Alarm Command was validated successfully";
    }
}
