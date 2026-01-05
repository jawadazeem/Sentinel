package frontend;

import service.system.SystemHealthService;
import service.system.SystemSnapshot;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class SystemReportPanel extends JPanel {
    private final SystemHealthService healthService;
    private final JLabel healthVal, alarmVal, densityVal, modeVal, timeVal;

    public SystemReportPanel(SystemHealthService healthService) {
        this.healthService = healthService;
        setLayout(new BorderLayout());
        setBackground(new Color(30, 31, 34));
        setBorder(new EmptyBorder(40, 40, 40, 40));

        // Title Section
        JLabel title = new JLabel("SYSTEM OPERATIONAL REPORT");
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        title.setFont(new Font("Inter", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        add(title, BorderLayout.NORTH);

        // Stats Grid
        JPanel statsGrid = new JPanel(new GridLayout(2, 3, 20, 20));
        statsGrid.setOpaque(false);

        healthVal = createStatCard(statsGrid, "FLEET HEALTH");
        alarmVal = createStatCard(statsGrid, "ACTIVE ALARMS");
        densityVal = createStatCard(statsGrid, "ALARM DENSITY");
        modeVal = createStatCard(statsGrid, "SYSTEM MODE");
        timeVal = createStatCard(statsGrid, "LAST SNAPSHOT");

        add(statsGrid, BorderLayout.CENTER);
        updateReport();
    }

    private JLabel createStatCard(JPanel parent, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(25, 26, 29));
        card.setBorder(BorderFactory.createLineBorder(new Color(45, 48, 51)));

        JLabel t = new JLabel(title);
        t.setForeground(Color.GRAY);
        t.setFont(new Font("Inter", Font.PLAIN, 12));
        t.setBorder(new EmptyBorder(10, 15, 0, 0));

        JLabel val = new JLabel("--");
        val.setForeground(new Color(71, 131, 192));
        val.setFont(new Font("Inter", Font.BOLD, 32));
        val.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(t, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        parent.add(card);
        return val;
    }

    public void updateReport() {
        SystemSnapshot snap = healthService.getSystemSnapshot();
        healthVal.setText((int)(snap.getFleetHealthPercentage() * 100) + "%");
        alarmVal.setText(String.valueOf(snap.getActiveAlarmCount()));
        densityVal.setText(String.format("%.2f", snap.getAlarmDensity()));
        modeVal.setText(snap.getSystemMode().toString());
        timeVal.setText(snap.getTimestamp().atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }
}
