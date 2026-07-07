// Refactored AuthSystem.java to remove tabs and redesign UI to match SchedulingProgram
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.swing.*;
import org.w3c.dom.events.MouseEvent;




public class AuthSystem {
    private static final Color PRIMARY_COLOR = new Color(76, 175, 80);   // Green
private static final Color ACCENT_COLOR = new Color(27, 94, 32);     // Dark Green
private static final Color BACKGROUND_COLOR = Color.WHITE;          // Clean white
private static final Color TEXT_COLOR = new Color(33, 33, 33);       // Dark grey text
private static final Color SIDEBAR_COLOR = new Color(56, 142, 60); // darker green


private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 26);
private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 16);
private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 16);
private static final Font INPUT_FONT = new Font("Segoe UI", Font.PLAIN, 15);




   public static void main(String[] args) {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
        JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found!", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    SwingUtilities.invokeLater(() -> {
        AuthSystem auth = new AuthSystem();
        auth.configureDialogTheme();  // 🟢 Apply custom styling
        auth.showLoginWindow();       // 🟢 Launch Login
    });
}



    private void showLoginWindow() {
    JFrame loginFrame = new JFrame("Login - Schedule Manager");
    loginFrame.setSize(720, 480);
    loginFrame.setLocationRelativeTo(null);
    loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    loginFrame.setLayout(new BorderLayout());

    // LEFT PANEL: Darker sidebar with image
    JPanel leftPanel = new JPanel();
    Color SIDEBAR_COLOR = new Color(56, 142, 60); // darker than primary
    leftPanel.setBackground(SIDEBAR_COLOR);
    leftPanel.setPreferredSize(new Dimension(280, 480));
    leftPanel.setLayout(new BorderLayout());

    try {
        ImageIcon rawIcon = new ImageIcon("src/images/calendar.png");
        Image scaledImage = rawIcon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        leftPanel.add(imageLabel, BorderLayout.CENTER);
    } catch (Exception e) {
        JLabel fallback = new JLabel("Logo", SwingConstants.CENTER);
        fallback.setForeground(Color.WHITE);
        fallback.setFont(new Font("Segoe UI", Font.BOLD, 18));
        leftPanel.add(fallback, BorderLayout.CENTER);
    }

    JLabel footer = new JLabel("", SwingConstants.CENTER);
    footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    footer.setForeground(Color.WHITE);


    // RIGHT PANEL: Login form
    JPanel rightPanel = new JPanel(null);
    rightPanel.setBackground(BACKGROUND_COLOR);

    JLabel title = new JLabel("LOGIN");
    title.setFont(TITLE_FONT);
    title.setBounds(60, 40, 200, 30);
    title.setForeground(ACCENT_COLOR);
    rightPanel.add(title);

    JLabel userLabel = new JLabel("Username / Email:");
    userLabel.setBounds(60, 90, 150, 20);
    userLabel.setFont(LABEL_FONT);
    rightPanel.add(userLabel);

    JTextField userField = new JTextField();
    userField.setBounds(60, 115, 300, 30);
    userField.setFont(INPUT_FONT);
    rightPanel.add(userField);

    JLabel passLabel = new JLabel("Password:");
    passLabel.setBounds(60, 160, 150, 20);
    passLabel.setFont(LABEL_FONT);
    rightPanel.add(passLabel);

    JPasswordField passField = new JPasswordField();
    passField.setBounds(60, 185, 300, 30);
    passField.setFont(INPUT_FONT);
    rightPanel.add(passField);

    JCheckBox showPass = new JCheckBox("Show Password");
    showPass.setBounds(60, 220, 150, 20);
    showPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    showPass.setBackground(BACKGROUND_COLOR);
    showPass.addActionListener(e -> {
        passField.setEchoChar(showPass.isSelected() ? '\0' : '•');
    });
    rightPanel.add(showPass);

    // MAIN LOGIN BUTTON
    JButton loginBtn = new JButton("Login");
    loginBtn.setBounds(60, 260, 130, 35);
    styleButton(loginBtn);
    rightPanel.add(loginBtn);

    // SECONDARY SIGN UP BUTTON (aligned right, styled separately)
    JButton signupBtn = new JButton("Sign Up");
    signupBtn.setBounds(230, 260, 130, 35);
    signupBtn.setFont(BUTTON_FONT);
    signupBtn.setBackground(Color.WHITE);
    signupBtn.setForeground(ACCENT_COLOR);
    signupBtn.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR));
    signupBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    signupBtn.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent evt) {
            signupBtn.setBackground(new Color(230, 255, 230));
        }

        public void mouseExited(MouseEvent evt) {
            signupBtn.setBackground(Color.WHITE);
        }
    });
    signupBtn.addActionListener(e -> {
        loginFrame.dispose();
        showSignupWindow();
    });
    rightPanel.add(signupBtn);

    // Add panels to frame
    loginFrame.add(leftPanel, BorderLayout.WEST);
    loginFrame.add(rightPanel, BorderLayout.CENTER);
    loginFrame.setVisible(true);

    // Login logic
    loginBtn.addActionListener(e -> {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(loginFrame, "Please fill in all fields.");
            return;
        }

        boolean loginSuccessful = validateUser(username, password);
        if (loginSuccessful) {
            updateLastLogin(username);
            JOptionPane.showMessageDialog(loginFrame, "Login successful!");
            loginFrame.dispose();
            new SchedulingProgram(); // Launch dashboard
        } else {
            JOptionPane.showMessageDialog(loginFrame, "Invalid username/email or password.");
        }
    });
}



private void showSignupWindow() {
    JFrame signupFrame = new JFrame("Sign Up - Schedule Manager");
    signupFrame.setSize(720, 480);
    signupFrame.setLocationRelativeTo(null);
    signupFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    signupFrame.setLayout(new BorderLayout());

    // LEFT PANEL: Sidebar with logo
    JPanel leftPanel = new JPanel();
    Color SIDEBAR_COLOR = new Color(56, 142, 60); // darker green
    leftPanel.setBackground(SIDEBAR_COLOR);
    leftPanel.setPreferredSize(new Dimension(280, 480));
    leftPanel.setLayout(new BorderLayout());

    try {
        ImageIcon rawIcon = new ImageIcon("src/images/calendar.png");
        Image scaledImage = rawIcon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        leftPanel.add(imageLabel, BorderLayout.CENTER);
    } catch (Exception e) {
        JLabel fallback = new JLabel("Logo", SwingConstants.CENTER);
        fallback.setForeground(Color.WHITE);
        fallback.setFont(new Font("Segoe UI", Font.BOLD, 18));
        leftPanel.add(fallback, BorderLayout.CENTER);
    }

    JLabel footer = new JLabel("", SwingConstants.CENTER);
    footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    footer.setForeground(Color.WHITE);

    // RIGHT PANEL: Form area
    JPanel rightPanel = new JPanel(null);
    rightPanel.setBackground(BACKGROUND_COLOR);

    JLabel title = new JLabel("SIGN UP");
    title.setFont(TITLE_FONT);
    title.setBounds(60, 30, 200, 30);
    title.setForeground(ACCENT_COLOR);
    rightPanel.add(title);

    JLabel userLabel = new JLabel("Username:");
    userLabel.setBounds(60, 80, 100, 20);
    userLabel.setFont(LABEL_FONT);
    rightPanel.add(userLabel);

    JTextField userField = new JTextField();
    userField.setBounds(60, 105, 300, 30);
    userField.setFont(INPUT_FONT);
    rightPanel.add(userField);

    JLabel emailLabel = new JLabel("Email:");
    emailLabel.setBounds(60, 145, 100, 20);
    emailLabel.setFont(LABEL_FONT);
    rightPanel.add(emailLabel);

    JTextField emailField = new JTextField();
    emailField.setBounds(60, 170, 300, 30);
    emailField.setFont(INPUT_FONT);
    rightPanel.add(emailField);

    JLabel passLabel = new JLabel("Password:");
    passLabel.setBounds(60, 210, 100, 20);
    passLabel.setFont(LABEL_FONT);
    rightPanel.add(passLabel);

    JPasswordField passField = new JPasswordField();
    passField.setBounds(60, 235, 300, 30);
    passField.setFont(INPUT_FONT);
    rightPanel.add(passField);

    JLabel confirmLabel = new JLabel("Confirm Password:");
    confirmLabel.setBounds(60, 275, 150, 20);
    confirmLabel.setFont(LABEL_FONT);
    rightPanel.add(confirmLabel);

    JPasswordField confirmField = new JPasswordField();
    confirmField.setBounds(60, 300, 300, 30);
    confirmField.setFont(INPUT_FONT);
    rightPanel.add(confirmField);

    JCheckBox showPass = new JCheckBox("Show Password");
    showPass.setBounds(60, 335, 150, 20);
    showPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    showPass.setBackground(BACKGROUND_COLOR);
    showPass.addActionListener(e -> {
        char echo = showPass.isSelected() ? '\0' : '•';
        passField.setEchoChar(echo);
        confirmField.setEchoChar(echo);
    });
    rightPanel.add(showPass);

    // ✅ Primary Sign Up Button
JButton signupBtn = new JButton("Sign Up");
signupBtn.setBounds(60, 370, 130, 35);
styleButton(signupBtn); // green button
rightPanel.add(signupBtn);

// ✅ Secondary Login Button
JButton loginBtn = new JButton("Login");
loginBtn.setBounds(230, 370, 130, 35);
loginBtn.setFont(BUTTON_FONT);
loginBtn.setBackground(Color.WHITE);
loginBtn.setForeground(ACCENT_COLOR);
loginBtn.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR));
loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
loginBtn.addMouseListener(new java.awt.event.MouseAdapter() {
    public void mouseEntered(java.awt.event.MouseEvent e) {
        loginBtn.setBackground(new Color(230, 255, 230));
    }
    public void mouseExited(java.awt.event.MouseEvent e) {
        loginBtn.setBackground(Color.WHITE);
    }
});



    loginBtn.addActionListener(e -> {
        signupFrame.dispose();
        showLoginWindow();
    });
    rightPanel.add(loginBtn);

    // Sign Up logic
    signupBtn.addActionListener(e -> {
        String username = userField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(signupFrame, "All fields are required.");
            return;
        }

        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(signupFrame, "Passwords do not match.");
            return;
        }

        if (password.length() < 6) {
            JOptionPane.showMessageDialog(signupFrame, "Password must be at least 6 characters.");
            return;
        }

        if (createUser(username, email, password)) {
            JOptionPane.showMessageDialog(signupFrame, "Account created! Please login.");
            signupFrame.dispose();
            showLoginWindow();
        }
    });

    // Final UI Assembly
    signupFrame.add(leftPanel, BorderLayout.WEST);
    signupFrame.add(rightPanel, BorderLayout.CENTER);
    signupFrame.setVisible(true);
}

private void styleButton(JButton button) {
    button.setFont(BUTTON_FONT);
    button.setBackground(PRIMARY_COLOR);
    button.setForeground(Color.WHITE);
    button.setFocusPainted(false);
    button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    // ✅ Use MouseAdapter to avoid abstract method errors
    button.addMouseListener(new java.awt.event.MouseAdapter() {

        public void mouseEntered(MouseEvent evt) {
            button.setBackground(ACCENT_COLOR);
        }


        public void mouseExited(MouseEvent evt) {
            button.setBackground(PRIMARY_COLOR);
        }
    });
}
private void configureDialogTheme() {
    // Set global dialog appearance
    UIManager.put("OptionPane.background", BACKGROUND_COLOR);
    UIManager.put("Panel.background", BACKGROUND_COLOR);
    UIManager.put("OptionPane.messageForeground", ACCENT_COLOR);
    UIManager.put("OptionPane.messageFont", INPUT_FONT);  // fallback if CONTENT_FONT not defined
    UIManager.put("Button.background", PRIMARY_COLOR);
    UIManager.put("Button.foreground", Color.WHITE);
    UIManager.put("Button.font", BUTTON_FONT);
    UIManager.put("Button.select", ACCENT_COLOR);  // hover color
    UIManager.put("Button.focus", new Color(0, 0, 0, 0));
}




    private static boolean createUser(String username, String email, String password) {
        String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, hashPassword(password));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Database", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static boolean validateUser(String username, String password) {
        String sql = "SELECT password_hash FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password_hash").equals(hashPassword(password));
            }
            return false;
        } catch (SQLException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Database", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static void updateLastLogin(String username) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE username = ? OR email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
