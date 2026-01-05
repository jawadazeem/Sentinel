package infrastructure.factories;

import devices.api.Device;
import devices.api.HardwareLink;
import devices.impl.ThermalDevice;
import infrastructure.logger.Logger;

import java.util.UUID;

public class ThermalDeviceFactory implements DeviceFactory {

    @Override
    public Device create(UUID Id, Logger logger, HardwareLink hardwareLink) {
        return new ThermalDevice(Id, logger, hardwareLink);
    }

    @Override
    public Device create (Logger logger, HardwareLink hardwareLink) {
        return new ThermalDevice(UUID.randomUUID(), logger, hardwareLink);
    }
}
