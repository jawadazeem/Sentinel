package commands.securityhubcommands;

import alarm.AlarmSeverity;
import commands.CommandType;
import core.SecurityHub;
import devices.api.Device;
import devices.model.DeviceStatus;

public class SystemResetCommand implements SecurityHubCommand {

    private Device device;
    private CommandType commandType = CommandType.RESET;

    public SystemResetCommand(Device device) {
        this.device = device;
    }

    @Override
    public void execute() {
        device.setDeviceStatus(DeviceStatus.OPERATIONAL);
        device.resetFailureCount();
        SecurityHub.getInstance().resolveAlarmsByDevice(device);
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
        return "System Reset Command was validated successfully";
    }
}
