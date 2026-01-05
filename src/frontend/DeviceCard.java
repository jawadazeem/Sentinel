package frontend;

import core.SecurityHub;
import devices.api.Device;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static java.lang.String.valueOf;

class DeviceCard extends JPanel {
    private Device device;
    private final JLabel typeLabel;
    private final JLabel idLabel;
    private final JLabel signalLabel;
    private final JProgressBar batteryBar;
    private final JLabel batteryLabel;
    private final JPanel statusDot;

    public DeviceCard(Device d, SecurityHub hub) { // Pass hub for actions
        this.device = d;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(240, 180));
        setBackground(new Color(25, 26, 29));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(35, 38, 41), 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Header: Type + Status Indicator Dot
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        typeLabel = new JLabel(valueOf(device.getDeviceType()).toUpperCase());
        typeLabel.setFont(new Font("Inter", Font.BOLD, 15)); // Small caps style
        typeLabel.setForeground(new Color(71, 131, 192));
        header.add(typeLabel, BorderLayout.WEST);

        // Body: ID and Signal
        JPanel body = new JPanel(new GridLayout(3, 1));
        body.setOpaque(false);
        idLabel = new JLabel("ID: ..." + device.getId().toString().substring(0, 12)); // Shorten UUID
        idLabel.setForeground(Color.LIGHT_GRAY);
        signalLabel = new JLabel("Signal: " + device.getSignalStrength() + " dBm");
        signalLabel.setForeground(Color.GRAY);
        body.add(idLabel);
        body.add(signalLabel);

        // Action: Manual Ping Button
        JButton pingBtn = new JButton("PING");
        pingBtn.setFont(new Font("Inter", Font.BOLD, 10));
        pingBtn.setMargin(new Insets(10, 20, 10, 20));
        // Run ping on a background thread to avoid blocking the EDT
        pingBtn.addActionListener(e -> {
            Thread t = new Thread(() -> device.ping(), "device-ping-thread");
            t.setDaemon(true);
            t.start();
        });

        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        top.setOpaque(false);

        top.add(header, BorderLayout.NORTH);
        top.add(pingBtn, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);

        add(body, BorderLayout.CENTER);

        // Footer: Battery
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        batteryBar = new JProgressBar(0, 100);
        batteryBar.setValue((int) device.getBatteryLife());
        batteryBar.setPreferredSize(new Dimension(100, 12));
        batteryLabel = new JLabel("⚡ " + device.getBatteryLife() + "%");
        batteryLabel.setForeground(Color.LIGHT_GRAY);
        footer.add(batteryLabel, BorderLayout.WEST);
        footer.add(batteryBar, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);

        // Status Indicator Dot (Top Right) — reads device state when painting
        statusDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Color based on Device Status
                if (device != null) {
                    switch (device.getDeviceStatus()) {
                        case OPERATIONAL -> g2.setColor(new Color(23, 109, 45)); // Green
                        case RECOVERY_MODE -> g2.setColor(Color.ORANGE);
                        case DECOMMISSIONED -> g2.setColor(Color.RED);
                        default -> g2.setColor(Color.GRAY);
                    }
                } else {
                    g2.setColor(Color.GRAY);
                }
                g2.fillOval(5, 5, 10, 10);
            }
        };
        statusDot.setPreferredSize(new Dimension(20, 20));
        statusDot.setOpaque(false);
        header.add(statusDot, BorderLayout.EAST);

        // Battery Bar Coloring
        refreshBatteryColor();
    }

    /**
     * Update UI elements from the current device state. Call this instead of recreating the card.
     */
    public void updateFromDevice() {
        if (device == null) return;
        // Update labels and bars quickly on EDT
        SwingUtilities.invokeLater(() -> {
            typeLabel.setText(valueOf(device.getDeviceType()).toUpperCase());
            idLabel.setText("ID: ..." + device.getId().toString().substring(0, 12));
            signalLabel.setText("Signal: " + device.getSignalStrength() + " dBm");
            batteryBar.setValue((int) device.getBatteryLife());
            batteryLabel.setText("⚡ " + device.getBatteryLife() + "%");
            refreshBatteryColor();
            statusDot.repaint();
        });
    }

    private void refreshBatteryColor() {
        int life = (int) device.getBatteryLife();
        if (life < 20) {
            batteryBar.setForeground(Color.RED);
        } else if (life < 50) {
            batteryBar.setForeground(Color.YELLOW);
        } else {
            batteryBar.setForeground(new Color(0, 255, 136));
        }
    }

    /**
     * Replace the device backing this card (used if a new instance is loaded for the same id)
     */
    public void setDevice(Device d) {
        this.device = d;
        updateFromDevice();
    }
}
