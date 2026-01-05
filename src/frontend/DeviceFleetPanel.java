package frontend;

import core.SecurityHub;
import devices.api.Device;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeviceFleetPanel extends JPanel {
    private final SecurityHub hub;
    private final JPanel cardGrid;

    // Reuse cards rather than recreating them every refresh
    private final Map<UUID, DeviceCard> cardMap = new HashMap<>();

    public DeviceFleetPanel(SecurityHub hub) {
        this.hub = hub;
        setLayout(new BorderLayout());
        setBackground(new Color(30, 31, 34));

        // Title Section
        JLabel title = new JLabel("DEVICE FLEET");
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        title.setFont(new Font("Inter", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        add(title, BorderLayout.NORTH);

        // Setup the Grid
        cardGrid = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        cardGrid.setBackground(new Color(30, 31, 34));

        JScrollPane scroll = new JScrollPane(cardGrid);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16); // Smoother scrolling

        add(scroll, BorderLayout.CENTER);
        refresh();
    }

    public void refresh() {
        // Snapshot list to avoid concurrent modification
        List<Device> devices = hub.getDevices();

        // Track which IDs are still present
        Map<UUID, DeviceCard> stillPresent = new HashMap<>();

        // Add or update existing cards
        for (Device d : devices) {
            UUID id = d.getId();
            DeviceCard card = cardMap.get(id);
            if (card == null) {
                card = new DeviceCard(d, hub);
                cardMap.put(id, card);
                cardGrid.add(card);
            } else {
                // Update the backing device reference and refresh the visual elements
                card.setDevice(d);
                card.updateFromDevice();
            }
            stillPresent.put(id, card);
        }

        // Remove cards that are no longer present
        for (UUID existingId : cardMap.keySet().toArray(new UUID[0])) {
            if (!stillPresent.containsKey(existingId)) {
                DeviceCard toRemove = cardMap.remove(existingId);
                if (toRemove != null) cardGrid.remove(toRemove);
            }
        }

        // Revalidate & repaint once after batch update
        cardGrid.revalidate();
        cardGrid.repaint();
    }
}