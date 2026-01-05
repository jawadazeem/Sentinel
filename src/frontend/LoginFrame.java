package frontend;

import core.SecurityHub;
import infrastructure.logger.Logger;
import infrastructure.repository.UserRepository;
import service.auth.AuthService;
import user.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField userField;
    private final JPasswordField passField;
    private final JLabel statusLabel;

    // Dependencies needed for the eventual handoff
    private final SecurityHub hub;
    private final Logger logger;
    private final AuthService authService;

    public LoginFrame(SecurityHub hub, Logger logger, UserRepository userRepository) {
        this.hub = hub;
        this.logger = logger;
        this.authService = new AuthService(userRepository);

        setTitle("SENTINEL | Secure Access");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 500);
        setLocationRelativeTo(null); // Center on screen
        getContentPane().setBackground(new Color(25, 25, 26));
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(40, 20, 20, 20));

        JLabel brand = new JLabel("SENTINEL SYSTEMS");
        brand.setFont(new Font("Inter", Font.BOLD, 22));
        brand.setForeground(new Color(71, 131, 192));
        header.add(brand);

        // Form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(20, 40, 20, 40));

        userField = createStyledTextField("Username");
        passField = createStyledPasswordField();

        statusLabel = new JLabel("Enter credentials to unlock node.");
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("Inter", Font.PLAIN, 12));

        JButton loginBtn = new JButton("AUTHORIZE ACCESS");
        styleLoginButton(loginBtn);
        loginBtn.addActionListener(e -> attemptLogin());

        form.add(new JLabel("USER ID")).setForeground(Color.GRAY);
        form.add(userField);
        form.add(Box.createRigidArea(new Dimension(0, 20)));
        form.add(new JLabel("ACCESS KEY")).setForeground(Color.GRAY);
        form.add(passField);
        form.add(Box.createRigidArea(new Dimension(0, 30)));
        form.add(loginBtn);
        form.add(Box.createRigidArea(new Dimension(0, 20)));
        form.add(statusLabel);

        // Add credit line below the status label
        JLabel creditLabel = new JLabel("<html><div style='text-align:center'>Application by Jawad Azeem. To learn more, visit www.jawadazeem.com/sentinel</div></html>");
        creditLabel.setForeground(Color.GRAY);
        creditLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        creditLabel.setFont(new Font("Inter", Font.PLAIN, 11));
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(creditLabel);

        add(form, BorderLayout.CENTER);

        // Allow "Enter" key to trigger login
        getRootPane().setDefaultButton(loginBtn);
    }

    private void attemptLogin() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("ENTER USERNAME AND PASSWORD");
            statusLabel.setForeground(new Color(239, 68, 68));
            return;
        }

        // First boot: no users exist yet → create admin
        if (!authService.hasAnyUsers()) {
            authService.saveUser(user, pass);
            statusLabel.setText("ADMIN ACCOUNT CREATED");
            statusLabel.setForeground(new Color(34, 197, 94));

            SwingUtilities.invokeLater(() -> {
                new SentinelDashboard(hub, logger).setVisible(true);
                this.dispose();
            });
            return;
        }

        // Normal login
        if (authService.authenticate(user, pass)) {
            statusLabel.setText("ACCESS GRANTED. INITIALIZING...");
            statusLabel.setForeground(new Color(34, 197, 94));

            SwingUtilities.invokeLater(() -> {
                new SentinelDashboard(hub, logger).setVisible(true);
                this.dispose();
            });
        } else {
            statusLabel.setText("INVALID CREDENTIALS");
            statusLabel.setForeground(new Color(239, 68, 68));
            passField.setText("");
        }
    }

    // Styling Helpers
    private JTextField createStyledTextField(String placeholder) {
        JTextField f = new JTextField();
        setupField(f);
        return f;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField f = new JPasswordField();
        setupField(f);
        return f;
    }

    private void setupField(JTextField f) {
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setBackground(new Color(35, 36, 40));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 56, 60)),
                new EmptyBorder(5, 10, 5, 10)
        ));
    }

    private void styleLoginButton(JButton btn) {
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btn.setBackground(new Color(71, 131, 192));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Inter", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
    }
}