import alarm.Alarm;
import com.formdev.flatlaf.FlatDarkLaf;
import core.SecurityHub;
import devices.api.Device;
import devices.api.HardwareLink;
import frontend.LoginFrame;
import infrastructure.factories.MotionDeviceFactory;
import infrastructure.logger.*;
import infrastructure.repository.*;
import infrastructure.subscribers.*;
import service.system.SystemHealthService;
import service.subscriber.notification.SubscriberNotificationService;
import sim.SimulationEngine;

import javax.swing.*;
import java.util.List;

public class SentinelApplication {
    public static void main(String[] args) {
        // 1. Set up the "SaaS" Look and Feel before any UI is created
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf. Falling back to default.");
        }

        // 1. Initialize Infrastructure
        SecurityHub hub = SecurityHub.getInstance();
        setupLogging(hub.getLogger());
        hub.armHub();
        SimulationEngine hardwareLink = new SimulationEngine(hub, hub.getLogger());

        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Thread thread = new Thread(hardwareLink);
        thread.start();

        SystemHealthService systemHealthSvc = new SystemHealthService(hub);

        // Initialize Repos
        DeviceRepository dRepo = new DeviceRepository(hub.getLogger(), hardwareLink);
        AlarmLogRepository aRepo = new AlarmLogRepository(hub.getLogger());
        hub.setAlarmRepository(aRepo);

        UserRepository uRepo = new UserRepository(hub.getLogger());

        List<Device> savedDevices = dRepo.loadAll();
        for(Device d : savedDevices) {
            hub.addDevice(d);
        }

        List<Alarm> activeAlarms = aRepo.loadAllActiveAlarms();
        for(Alarm a : activeAlarms) {
            hub.registerAlarm(a);
        }

        addShutdownHook(dRepo, aRepo);
//
//        hub.executeCommand(new SystemResetCommand(motionSensor));
//        hub.executeCommand(new SystemResetCommand(smokeSensor));
//
//        // 3. Configure Alerts (Observers)

        // 4. Operational Sequence
        hub.armHub();

        // Use the Hub to reset everything centrally
        hub.initiateFleetCheck();

        hub.monitorAndHandleDeviceHealth();

        // 6. Execution
        hub.processAllCommands(); // Processes Hub commands ONLY, not device commands.
        // registerSubscribers(hub);

        SwingUtilities.invokeLater(() -> {
            new LoginFrame(hub, hub.getLogger(), uRepo).setVisible(true);
        });
    }

    private static void setupLogging(Logger logger) {
        logger.registerListener(new LogFileArchiver("logs.txt"));
    }

    private static void registerSubscribers(SecurityHub hub) {
        SubscriberRepository subscriberRepository = new SubscriberRepository(hub.getLogger());
        SubscriberNotificationService SNS = new SubscriberNotificationService(subscriberRepository, hub.getLogger());
        SNS.addSubscriber(new SecurityTeamPhoneAppAlarm());
        SNS.addSubscriber(new PoliceStationLink());
    }

    /**
     * Calling repeatedly in main will cause an abundance of devices.
     */
    private static void createHardwareFleet(SecurityHub hub, HardwareLink hardwareLink) {
        for (int i=0; i<2; i++) {
            hub.addDevice(new MotionDeviceFactory().create(hub.getLogger(), hardwareLink));
        }
    }

    private static void addShutdownHook(DeviceRepository dRepo, AlarmLogRepository aRepo) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Sentinel... Saving Fleet State.");
            List<Device> currentFleet = SecurityHub.getInstance().getDevices();
            for (Device d : currentFleet) {
                dRepo.save(d);
            }

            List<Alarm> currentActiveAlarms = SecurityHub.getInstance().getActiveAlarms();
            for (Alarm a : currentActiveAlarms) {
                aRepo.save(a);
            }
        }));
    }
}