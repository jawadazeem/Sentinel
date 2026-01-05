package frontend;

import core.SecurityHub;
import infrastructure.logger.Logger;
import service.system.SystemHealthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SentinelDashboard extends JFrame {
    private final SecurityHub hub;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContentArea = new JPanel(cardLayout);
    private final Logger logger;

    // Sub-Panels
    private final SystemReportPanel reportPage;
    private final DeviceFleetPanel fleetPage;
    private final ReceiverPanel receiverPage;

    public SentinelDashboard(SecurityHub hub, Logger logger) {
        this.hub = hub;
        this.logger = logger;
        setupFrame();

        // 1. Initialize Content Panels
        reportPage = new SystemReportPanel(new SystemHealthService(hub));
        fleetPage = new DeviceFleetPanel(hub);
        receiverPage = new ReceiverPanel();

        // 2. Build Card Stack
        mainContentArea.add(reportPage, "REPORT");
        mainContentArea.add(fleetPage, "FLEET");
        mainContentArea.add(receiverPage, "RECEIVER");
        mainContentArea.add(new JPanel(), "ALARM_HISTORY"); // Placeholder
        mainContentArea.add(new JPanel(), "SETTINGS"); // Placeholder

        AuditLogPanel auditPage = new AuditLogPanel();
        mainContentArea.add(auditPage, "AUDIT_LOGS");
        logger.registerListener(auditPage);

        // 3. Assemble Layout
        add(createHeader(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);
        add(mainContentArea, BorderLayout.CENTER);

        // 4. Global Refresh Timer: reduced frequency to lower EDT load
        Timer uiTimer = new Timer(1000, e -> updateDisplay());
        uiTimer.start();
    }

    private void setupFrame() {
        setTitle("SENTINEL SYSTEMS | Enterprise Security Node");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLayout(new BorderLayout());
    }

    private void updateDisplay() {

        SwingUtilities.invokeLater(() -> {
            reportPage.updateReport();
            fleetPage.refresh();
        });
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(25, 25, 26));
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(new EmptyBorder(30, 20, 30, 20));

        sidebar.add(Box.createRigidArea(new Dimension(0, 40)));
        sidebar.add(createNavButton("System Report", "REPORT"));
        sidebar.add(createNavButton("Device Fleet", "FLEET"));
        sidebar.add(createNavButton("Receivers", "RECEIVERS"));
        sidebar.add(createNavButton("Alarm History", "ALARM_HISTORY"));
        sidebar.add(createNavButton("Audit Logs", "AUDIT_LOGS"));
        sidebar.add(createNavButton("System Settings", "SETTINGS"));

        return sidebar;
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Inter", Font.BOLD, 18));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setPreferredSize(new Dimension(200, 45));
        btn.setMaximumSize(new Dimension(200, 45));
        btn.putClientProperty("JButton.buttonType", "toolBarButton");
        btn.addActionListener(e -> cardLayout.show(mainContentArea, cardName));
        return btn;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(25, 26, 29));
        header.setBorder(new EmptyBorder(15, 25, 15, 25));

        JLabel brand = new JLabel("Sentinel Systems");
        brand.setFont(new Font("Inter", Font.BOLD, 26));
        brand.setForeground(new Color(71, 131, 192));
        header.add(brand, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton armBtn = createActionBtn("ARM SYSTEM", new Color(34, 197, 94));
        armBtn.addActionListener(e -> hub.armHub());

        JButton disarmBtn = createActionBtn("DISARM", new Color(239, 68, 68));
        disarmBtn.addActionListener(e -> hub.disarmHub());

        actions.add(armBtn);
        actions.add(disarmBtn);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JButton createActionBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Inter", Font.BOLD, 15));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(150, 40));
        return btn;
    }
}