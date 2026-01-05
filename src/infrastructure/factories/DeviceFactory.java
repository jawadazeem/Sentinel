package infrastructure.factories;


import devices.api.Device;
import devices.api.HardwareLink;
import infrastructure.logger.Logger;

import java.util.UUID;

public interface DeviceFactory {

    Device create(UUID Id, Logger logger, HardwareLink hardwareLink);
    Device create(Logger logger, HardwareLink hardwareLink);
}
