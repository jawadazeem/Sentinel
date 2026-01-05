package commands.securityhubcommands;

import alarm.AlarmSeverity;
import commands.CommandType;
import devices.api.Device;

public class SystemDiagnosticCommand implements SecurityHubCommand {
    private Device device;
    private CommandType commandType = CommandType.DIAGNOSTIC;

    public SystemDiagnosticCommand(Device device) {
        this.device = device;
    }

    @Override
    public void execute() {
        device.performSelfCheck();
    }

    @Override
    public AlarmSeverity getSeverity() {
        return AlarmSeverity.MEDIUM;
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
        return true;
    }

    @Override
    public String validationMessage() {
        return "System Diagnostic Command was validated successfully";
    }
}
