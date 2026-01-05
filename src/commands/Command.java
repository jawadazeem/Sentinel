package commands;

import alarm.AlarmSeverity;
import devices.api.Device;

public interface Command {
    void execute();
    boolean validate();
    String validationMessage();
    AlarmSeverity getSeverity();
    CommandType getCommandType();
    Device getDevice();
}
