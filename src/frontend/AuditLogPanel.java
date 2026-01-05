package frontend;

import infrastructure.logger.LogListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

public class AuditLogPanel extends JPanel implements LogListener {
    private final JTextArea logArea;
    private final String logFilePath = "logs.txt";

    // Buffer for incoming log lines; flushed to the JTextArea on the EDT periodically
    private final StringBuilder pendingBuffer = new StringBuilder();
    private final Object bufferLock = new Object();
    private final Timer flushTimer;

    // Limit reading from disk to the tail to avoid loading very large files into memory
    private static final int MAX_TAIL_BYTES = 200 * 1024; // 200 KB

    // Maximum characters appended to the JTextArea per flush to avoid long EDT work
    private static final int MAX_APPEND_CHARS_PER_FLUSH = 8 * 1024; // 8 KB

    public AuditLogPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 31, 34));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header Section
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("SYSTEM AUDIT LOGS");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JButton refreshBtn = new JButton("RELOAD FROM DISK");
        styleButton(refreshBtn);
        refreshBtn.addActionListener(e -> loadLogFile());

        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        add(header, BorderLayout.NORTH);

        // Log Display Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(20, 20, 21));
        logArea.setForeground(new Color(71, 131, 192)); // Terminal Cyan
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(45, 48, 51)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Timer to flush pending log updates to the text area at a controlled rate (runs on EDT)
        flushTimer = new Timer(250, e -> flushBufferToTextArea());
        flushTimer.setCoalesce(true);
        flushTimer.start();

        // Load existing history on startup
        loadLogFile();
    }

    private void styleButton(JButton btn) {
        btn.setFont(new Font("Inter", Font.BOLD, 11));
        btn.setBackground(new Color(45, 48, 51));
        btn.setForeground(Color.LIGHT_GRAY);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 63, 65)),
                new EmptyBorder(5, 15, 5, 15)
        ));
    }

    /**
     * Reads the tail of logs.txt and populates the text area.
     * Reading is done on a background thread; only a limited tail (MAX_TAIL_BYTES) is loaded.
     */
    public void loadLogFile() {
        logArea.setText("--- LOADING AUDIT HISTORY (tail) ---\n");

        // Use a background thread for File I/O to keep Swing responsive
        new Thread(() -> {
            try {
                java.io.File f = new java.io.File(logFilePath);
                if (!f.exists() || f.length() == 0) {
                    SwingUtilities.invokeLater(() -> logArea.append("(no logs present)\n"));
                    return;
                }

                try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
                    long length = raf.length();
                    long start = Math.max(0, length - MAX_TAIL_BYTES);
                    raf.seek(start);

                    // If we started in the middle of a line, skip the partial line
                    if (start > 0) {
                        raf.readLine();
                    }

                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = raf.readLine()) != null) {
                        content.append(line).append('\n');
                    }

                    final String toSet = content.toString();
                    SwingUtilities.invokeLater(() -> {
                        logArea.setText(toSet);
                        scrollToBottom();
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> logArea.append("ERROR: Could not read logs.txt\n"));
            }
        }, "audit-log-loader").start();
    }

    private void scrollToBottom() {
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * Implementation of LogListener for live updates.
     * Instead of updating the UI directly for every incoming log line (which can overwhelm the EDT),
     * we buffer incoming lines and flush them at a controlled interval on the EDT.
     */
    @Override
    public void receiveLog(String formattedLog) {
        synchronized (bufferLock) {
            pendingBuffer.append(formattedLog).append('\n');
            // If buffer grows too large, drop the oldest content to keep memory bounded
            if (pendingBuffer.length() > 64 * 1024) { // 64 KB
                pendingBuffer.delete(0, pendingBuffer.length() - 32 * 1024); // keep last 32KB
                pendingBuffer.insert(0, "... (older logs dropped) ...\n");
            }
        }
    }

    /**
     * Flushes the buffered logs to the JTextArea. Runs on the EDT via Swing Timer.
     */
    private void flushBufferToTextArea() {
        String toAppend = null;
        synchronized (bufferLock) {
            if (pendingBuffer.length() > 0) {
                toAppend = pendingBuffer.toString();
                pendingBuffer.setLength(0);
            }
        }
        if (toAppend != null) {
            // Truncate the append to avoid long work on the EDT
            if (toAppend.length() > MAX_APPEND_CHARS_PER_FLUSH) {
                String truncated = "... (truncated) ...\n" + toAppend.substring(toAppend.length() - MAX_APPEND_CHARS_PER_FLUSH);
                logArea.append(truncated);
            } else {
                logArea.append(toAppend);
            }
            scrollToBottom();
        }
    }
}