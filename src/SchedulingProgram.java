import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class SchedulingProgram extends JFrame {
    
    private SchedulingBackend backend;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JComboBox<String> filterCategoryCombo;
    private JComboBox<String> filterValueCombo;
    private List<JButton> menuButtons = new ArrayList<>();
    private String currentActiveCard = "welcome";
    private JPanel welcomePanel; // <-- add this field
    private JSplitPane manualScheduleSplitPane; // Field to store the JSplitPane
    private static final Color SECONDARY_COLOR = new Color(165, 214, 167); // Light green

    private static final Color PRIMARY_COLOR = new Color(76, 175, 80); // Medium green
    // Make the background a little greener white (was 245, 248, 245)
    private static final Color BACKGROUND_COLOR = new Color(215, 240, 215); // Greener soft white
    private static final Color MENU_BG = new Color(46, 125, 50); // Dark green
    private static final Color MENU_FG = Color.WHITE; // White text
    private static final Color ACCENT_COLOR = new Color(27, 94, 32); // Darker green for active menu items
    private static final Color ACTIVE_INDICATOR_COLOR = new Color(165, 214, 167); // Light green indicator
    private static final Color BUTTON_HOVER = new Color(56, 142, 60); // Slightly lighter dark green for hover
    private static final Color BORDER_COLOR = new Color(224, 224, 224); // Light gray border

    // New colors for negative action buttons
    private static final Color NEGATIVE_ACTION_COLOR = new Color(229, 115, 115); // Soft Red
    private static final Color NEGATIVE_ACTION_HOVER_COLOR = new Color(211, 47, 47); // Darker Red for hover

    private static final Font MENU_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font CONTENT_TITLE_FONT = new Font("Tahoma", Font.BOLD, 20);
    private static final Font CONTENT_FONT = new Font("Verdana", Font.PLAIN, 14);
    private static final Font MANUAL_SCHEDULE_COMBO_FONT = new Font("Verdana", Font.PLAIN, 12); // Font for Manual Schedule dropdowns

    // Helper method to wrap text with HTML if it exceeds available width
    private static String formatTextForMultiLineDisplay(String text, Font font, int availableWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // Ensure availableWidth has a reasonable minimum value
        if (availableWidth < 50) availableWidth = 50; 

        FontMetrics fm = new JLabel().getFontMetrics(font); 
        int textWidth = fm.stringWidth(text);

        // Only use HTML for wrapping if the plain text actually exceeds the available width
        if (textWidth > availableWidth) {
            String escapedText = text.replace("&", "&amp;")
                                     .replace("<", "&lt;")
                                     .replace(">", "&gt;")
                                     .replace("\n", "<br>"); // Convert newlines

            // Adjust CSS width buffer, and use HTML table for better vertical centering
            int cssWidth = availableWidth - 5; // Small buffer for HTML engine
            if (cssWidth < 40) cssWidth = 40; 

            return "<html><body style='margin:0; padding:0; width: " + cssWidth + "px; text-align: left; word-wrap: break-word;'>" +
                   "<table border=0 width='100%' height='100%'><tr><td align='left' valign='middle'>" +
                   escapedText +
                   "</td></tr></table></body></html>";
        } else {
            return text; // Fits, return plain text
        }
    }

    private static class RoundCheckBoxIcon implements Icon {
        private final int size;
        private final Color color;

        public RoundCheckBoxIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        // …inside RoundCheckBoxIcon class, replace paintIcon(…) with:
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            AbstractButton b = (AbstractButton) c;
            // draw outline always so unchecked box remains visible
            g2.setColor(b.isSelected() ? color : Color.LIGHT_GRAY);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(x, y, size, size);
            if (b.isSelected()) {
                // fill interior when selected
                g2.setColor(color);
                g2.fillOval(x + 3, y + 3, size - 6, size - 6);
                // draw check mark
                g2.setColor(Color.WHITE);
                int pad = size / 4;
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(x + pad, y + size / 2, x + size / 2, y + size - pad);
                g2.drawLine(x + size / 2, y + size - pad, x + size - pad, y + pad);
            }
            g2.dispose();
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }
    }

    public SchedulingProgram() {
        // initialize DB connection
        try {
            DBConnection.getConnection();
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Database connection failed: " + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        backend = new SchedulingBackendImpl();
        // remove file-based loading override
        // backend.setUseDatabase(false);
        backend.loadData();

        setTitle("Scheduling Program");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main layout
        setLayout(new BorderLayout());

        JPanel topBar = createTopMenuBar();              // ⬅ create top‐menu
        JPanel menuPanel = createMenuPanel();
        // Create content panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BACKGROUND_COLOR);

        // Create cards for each section
        contentPanel.add(createWelcomePanel(), "welcome");
        contentPanel.add(createCoursesPanel(), "courses");
        contentPanel.add(createSectionsPanel(), "sections");
        contentPanel.add(createProfessorsPanel(), "professors");
        contentPanel.add(createRoomsPanel(), "rooms");
        contentPanel.add(createManualSchedulePanel(), "manualschedule");
        contentPanel.add(createScheduleManagementPanel(), "schedulemanagement");
        contentPanel.add(createSmartSchedulePanel(), "smartschedule");
        
        // Show welcome panel first
        cardLayout.show(contentPanel, "welcome");

        // Add panels to main frame
        //add(topBar, BorderLayout.NORTH);                 // ⬅ add top‐menu
        add(menuPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        configureDialogTheme();

        setVisible(true);

        // Add WindowStateListener to handle resizing from fullscreen
        addWindowStateListener(new java.awt.event.WindowStateListener() {
            public void windowStateChanged(java.awt.event.WindowEvent e) {
                // Check if returning to normal state from maximized state
                if ((e.getOldState() & Frame.MAXIMIZED_BOTH) != 0 && 
                    (e.getNewState() & Frame.MAXIMIZED_BOTH) == 0) {
                    
                    // Check if the manual schedule panel is the current active card
                    if ("manualschedule".equals(currentActiveCard) && manualScheduleSplitPane != null) {
                        SwingUtilities.invokeLater(() -> {
                            // Force the JSplitPane to re-evaluate its layout
                            manualScheduleSplitPane.resetToPreferredSizes();
                            manualScheduleSplitPane.setDividerLocation(0.6); // Re-apply desired proportion
                            
                            // Revalidate and repaint the split pane and its parent container
                            manualScheduleSplitPane.revalidate();
                            manualScheduleSplitPane.repaint();
                            
                            // It might also be necessary to revalidate the parent of the split pane
                            if (manualScheduleSplitPane.getParent() != null) {
                                manualScheduleSplitPane.getParent().revalidate();
                                manualScheduleSplitPane.getParent().repaint();
                            }
                            // And possibly the main content panel too
                            contentPanel.revalidate();
                            contentPanel.repaint();
                        });
                    }
                }
            }
        });
    }

    private void styleScrollBar(JScrollBar sb) {
        sb.setPreferredSize(new Dimension(8, 0));
        sb.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = SECONDARY_COLOR;
                thumbHighlightColor = PRIMARY_COLOR;
                trackColor = BACKGROUND_COLOR;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(trackColor);
                g2.fillRect(r.x, r.y, r.width, r.height);
                g2.dispose();
            }
        });
    }

    // new helper to give sort‑combo a matching styled UI and renderer
    private void styleSortCombo(JComboBox<String> combo, boolean useFixedWidth, int newFixedWidth) {
        final Color normalBg = PRIMARY_COLOR;
        final Color hoverBg = BUTTON_HOVER;

        combo.setOpaque(false);
        combo.setBackground(normalBg);
        combo.setForeground(Color.WHITE);

        FontMetrics fmForHeight;
        int preferredHeight;
        if (useFixedWidth) { // Multi-line mode (e.g., Manual Schedule)
            combo.setFont(MANUAL_SCHEDULE_COMBO_FONT); // Use smaller font for these
            fmForHeight = combo.getFontMetrics(MANUAL_SCHEDULE_COMBO_FONT);
            preferredHeight = (fmForHeight.getHeight() * 3) + 25; // Height for up to 3 lines of smaller font
        } else { // Single-line mode (e.g., standard Sort dropdowns)
            combo.setFont(CONTENT_FONT); // Use standard font
            fmForHeight = combo.getFontMetrics(CONTENT_FONT);
            preferredHeight = fmForHeight.getHeight() + 15; // Enough for single line + padding
        }
        
        int initialPreferredWidth = Math.max(120, combo.getPreferredSize().width);
        // Apply fixed width if specified, otherwise use initial preferred width
        if (useFixedWidth && newFixedWidth > 0) {
            Dimension finalFixedSize = new Dimension(newFixedWidth, preferredHeight);
            combo.setPreferredSize(finalFixedSize);
            combo.setMinimumSize(finalFixedSize);
            combo.setMaximumSize(finalFixedSize);
        } else {
            Dimension styledSize = new Dimension(initialPreferredWidth, preferredHeight);
            combo.setPreferredSize(styledSize);
            // For non-fixed width, we might not want to set min/max size strictly to allow for some flexibility
            // combo.setMinimumSize(styledSize); 
            // combo.setMaximumSize(new Dimension(Short.MAX_VALUE, preferredHeight));
        }

        combo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

        combo.setUI(new BasicComboBoxUI() {
            private JLabel rendererLabel = new JLabel(); 

            @Override
            protected JButton createArrowButton() {
                // Return a minimal, effectively invisible button
                JButton arrowButton = new JButton(); // No text or icon
                arrowButton.setOpaque(false);
                arrowButton.setContentAreaFilled(false);
                arrowButton.setBorderPainted(false);
                arrowButton.setFocusPainted(false);
                arrowButton.setMargin(new Insets(0, 0, 0, 0));
                // Set a zero size so it doesn't take up visual space or appear as a square
                arrowButton.setPreferredSize(new Dimension(0, 0)); 
                arrowButton.setMinimumSize(new Dimension(0,0));
                arrowButton.setMaximumSize(new Dimension(0,0));
                return arrowButton;
            }

            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(c.getBackground());
                int radius = 10;
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), radius, radius);
                
                Rectangle r = rectangleForCurrentValue();
                String originalText = comboBox.getSelectedItem() != null ? comboBox.getSelectedItem().toString() : "";
                
                // Declare availableWidthForSelected here for correct scope
                int availableWidthForSelected = r.width - comboBox.getInsets().left;
                String displayText;

                if (useFixedWidth) { // For Manual Schedule, allow multi-line for selected item
                    // Use the pre-calculated availableWidthForSelected
                    displayText = formatTextForMultiLineDisplay(originalText, comboBox.getFont(), availableWidthForSelected);
                } else { // For Sort dropdowns, selected item should be single line
                    displayText = originalText;
                }

                rendererLabel.setText(displayText);
                rendererLabel.setFont(comboBox.getFont());
                rendererLabel.setForeground(comboBox.getForeground());
                rendererLabel.setOpaque(false);
                rendererLabel.setBorder(null);

                int textX = r.x;
                int textY = r.y;
                
                g2.translate(textX, textY);
                // The rendererLabel should use the full available width of the drawing rectangle 'r'
                // if not using fixedWidth, or the specifically calculated 'availableWidthForSelected' if it was.
                // However, for simplicity and to ensure it fits within 'r', r.width is a safe bet here.
                rendererLabel.setBounds(0, 0, r.width, r.height); 
                rendererLabel.paint(g2);
                g2.translate(-textX, -textY);

                g2.dispose();
            }
            
            // paintCurrentValueBackground is not needed as paint() handles the entire background.
            // paintCurrentValue is also effectively replaced by direct drawing in paint().

            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = (BasicComboPopup) super.createPopup();
                //popup.getList().setFont(CONTENT_FONT); // Old unconditional set
                popup.getList().setBackground(BACKGROUND_COLOR); 
                popup.getList().setSelectionBackground(ACCENT_COLOR); 
                popup.getList().setSelectionForeground(Color.WHITE);  
                
                FontMetrics listFm;
                //int listCellHeight; // No longer used here
                if (useFixedWidth) { // Multi-line mode for Manual Schedule
                    popup.getList().setFont(MANUAL_SCHEDULE_COMBO_FONT); // Use smaller font for list
                    // No longer setting fixed cell height here; renderer will determine height
                } else { // Single-line mode for standard Sort dropdowns
                    popup.getList().setFont(CONTENT_FONT); // Use standard font for list
                    // listFm = popup.getList().getFontMetrics(CONTENT_FONT);
                    // listCellHeight = listFm.getHeight() + 10; // Enough for single line item + padding
                    // popup.getList().setFixedCellHeight(listCellHeight); // REMOVED: Set for non-adaptive single line
                }
                // popup.getList().setFixedCellHeight(listCellHeight); // Original common line removed

                popup.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1));
                return popup;
            }
        });

        // Renderer for items IN THE DROPDOWN LIST.
        combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            DefaultListCellRenderer renderer = new DefaultListCellRenderer();
            Component c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                String originalText = value != null ? value.toString() : "";

                int cellHorizontalPadding = 20; // 10 left + 10 right from EmptyBorder
                int scrollbarAndInternalAllowance = 20;
                int listRenderWidth = list.getWidth() > 0 ? list.getWidth() : combo.getWidth();
                // If list width is still 0 (e.g. popup not shown yet), use combo's width as a fallback
                if (listRenderWidth == 0 && combo != null) listRenderWidth = combo.getWidth();
                
                int availableWidthForListItem = listRenderWidth - cellHorizontalPadding - scrollbarAndInternalAllowance;
                if (availableWidthForListItem < 50) availableWidthForListItem = 50; 

                String displayText = formatTextForMultiLineDisplay(originalText, list.getFont(), availableWidthForListItem);
                label.setText(displayText);
                label.setFont(list.getFont()); // Use the font from the JList
                // Padding is now part of the cell's fixed height and width calculation for wrapping
                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); 
            }

            if (isSelected) {
                c.setBackground(ACCENT_COLOR); 
                c.setForeground(Color.WHITE);
            } else {
                c.setBackground(BACKGROUND_COLOR); 
                c.setForeground(Color.DARK_GRAY);  
            }
            return c;
        });

        combo.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (combo.isEnabled()) {
                    combo.setBackground(hoverBg);
                    // setBackground should trigger repaint
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                combo.setBackground(normalBg);
                // setBackground should trigger repaint
            }
        });
    }
    // Overload for existing calls that don't specify fixed width behavior
    private void styleSortCombo(JComboBox<String> combo) {
        styleSortCombo(combo, false, 0); // Defaults to not using fixed width
    }

    // Helper method to create styled menu buttons
    private JButton createMenuButton(String text, String cardName) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Get current model state for hover effect
                boolean isHovered = getModel().isRollover() && !cardName.equals(currentActiveCard);

                // Draw rounded background with appropriate colors based on state
                if (cardName.equals(currentActiveCard)) {
                    g2d.setColor(ACCENT_COLOR); // Active button - solid color
                } else if (isHovered) {
                    // Hover color with higher opacity
                    g2d.setColor(
                            new Color(BUTTON_HOVER.getRed(), BUTTON_HOVER.getGreen(), BUTTON_HOVER.getBlue(), 120));
                } else {
                    // Semi-transparent background for non-active buttons
                    g2d.setColor(new Color(MENU_BG.getRed(), MENU_BG.getGreen(), MENU_BG.getBlue(), 40));
                }

                // Fill the button
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // Show left indicator based on state
                if (cardName.equals(currentActiveCard)) {
                    // Active indicator
                    g2d.setColor(ACTIVE_INDICATOR_COLOR);
                    g2d.fillRect(0, 0, 6, getHeight());
                } else if (isHovered) {
                    // Hover indicator
                    g2d.setColor(new Color(ACTIVE_INDICATOR_COLOR.getRed(),
                            ACTIVE_INDICATOR_COLOR.getGreen(), ACTIVE_INDICATOR_COLOR.getBlue(), 180));
                    g2d.fillRect(0, 0, 4, getHeight());
                }

                // Draw text with appropriate font weight
                if (isHovered) {
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    g2d.setColor(Color.WHITE);
                } else {
                    g2d.setFont(getFont());
                    g2d.setColor(getForeground());
                }

                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(text, 12, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };

        // Remove the MouseListener and just use Swing's built-in rollover state
        button.setRolloverEnabled(true);
        button.setModel(new DefaultButtonModel());

        button.setFont(MENU_FONT);
        button.setForeground(MENU_FG);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(200, 50));
        button.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 20));

        button.setActionCommand(cardName);
        button.addActionListener(e -> {
            setActiveMenu(cardName); // Sets currentActiveCard and styles buttons immediately
            // Defer the potentially complex panel refresh operation slightly on the EDT
            SwingUtilities.invokeLater(() -> {
                refreshPanel();      // Refreshes the panel associated with currentActiveCard
            });
        });

        menuButtons.add(button);
        return button;
    }

    private void setActiveMenu(String cardName) {
        currentActiveCard = cardName;
        for (JButton btn : menuButtons) {
            String btnText = btn.getText();
            // Map button text to cardName
            String btnCard = switch (btnText) {
                case "Home" -> "welcome";
                case "Course Management" -> "courses";
                case "Section Management" -> "sections";
                case "Professor Management" -> "professors";
                case "Room Management" -> "rooms";
                case "Manual Schedule" -> "manualschedule";
                case "Schedule Management" -> "schedulemanagement";
                case "Smart Schedule" -> "smartschedule";
                default -> "";
            };

            if (btnCard.equals(cardName)) {
                // Enhanced active state with darker background
                btn.setBackground(ACCENT_COLOR); // Darker green for active
                btn.setFont(new Font("Segoe UI", Font.BOLD, 14)); // Make text bold
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 6, 0, 0, ACTIVE_INDICATOR_COLOR),
                        BorderFactory.createEmptyBorder(0, 12, 0, 20) // Added 20px right padding
                ));
            } else {
                btn.setBackground(MENU_BG);
                btn.setFont(MENU_FONT); // Reset font
                btn.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 20)); // Consistent padding with 20px right
            }
            btn.repaint(); // Force repaint to show the indicator bar
        }
        // Removed specific refresh logic for "welcome" panel from here.
        // refreshPanel() will handle refreshing the content of the active panel.
    }

    private JPanel createMenuPanel() {
        JPanel menuPanel = new JPanel() {
            private Image backgroundImage;
            {
                // Try to load the menu background image (logo)
                try {
                    // Try with proper path from classpath
                    backgroundImage = ImageIO.read(getClass().getResource("/images/OLFULOGOS.png"));
                    if (backgroundImage == null) {
                        // Try with direct file path from src directory
                        backgroundImage = ImageIO.read(new File("src/images/OLFULOGOS.png"));
                        if (backgroundImage == null) {
                            // Try alternate path
                            backgroundImage = ImageIO.read(new File("resources/images/menu_background.jpg"));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not load menu background image: " + e.getMessage());
                    e.printStackTrace(); // Print full stack trace for debugging
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Draw the solid green background
                g2d.setColor(new Color(46, 125, 50, 255));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Draw the logo image at the top center (if available) with opacity
                if (backgroundImage != null) {
                    // Make the image bigger: use up to 80% of the panel width, but not more than
                    // image's natural size
                    int maxImgWidth = (int) (getWidth() * 3);
                    int imgWidth = Math.min(backgroundImage.getWidth(this), maxImgWidth);
                    // Optionally, allow upscaling if you want it even bigger:
                    // int imgWidth = maxImgWidth;
                    int imgHeight = backgroundImage.getHeight(this) * imgWidth / backgroundImage.getWidth(this);
                    int x = (getWidth() - imgWidth) / 2;
                    int y = 20; // 20px from the top

                    // Set opacity (e.g., 0.2f for 20% opacity)
                    float opacity = 0.2f;
                    Composite originalComposite = g2d.getComposite();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                    g2d.drawImage(backgroundImage, x, y, imgWidth, imgHeight, this);
                    g2d.setComposite(originalComposite); // Restore original composite
                }
            }
        };

        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(20, -120, 20, 10));

        // Rest of your existing menu panel code...

        // Application title with no extra left padding
        JLabel titleLabel = new JLabel("<html>Schedule<br>Manager</html>");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        titleLabel.setForeground(BACKGROUND_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Remove left padding from the title label
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 150, 30, 0));

        // Create menu buttons with uniform style
        JButton homeButton = createMenuButton("Home", "welcome");
        JButton coursesButton = createMenuButton("Course Management", "courses");
        JButton sectionsButton = createMenuButton("Section Management", "sections");
        JButton professorsButton = createMenuButton("Professor Management", "professors");
        JButton roomsButton = createMenuButton("Room Management", "rooms");
        JButton scheduleButton = createMenuButton("Manual Schedule", "manualschedule");
        JButton manageScheduleButton = createMenuButton("Schedule Management", "schedulemanagement");
        JButton smartScheduleButton = createMenuButton("Smart Schedule", "smartschedule");

        // Add components with spacing to menu panel
        menuPanel.add(titleLabel);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        menuPanel.add(homeButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        menuPanel.add(coursesButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        menuPanel.add(sectionsButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        menuPanel.add(professorsButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        menuPanel.add(roomsButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        menuPanel.add(scheduleButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        menuPanel.add(smartScheduleButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        menuPanel.add(manageScheduleButton);
        // Add vertical glue to push content toward the top

        menuPanel.add(Box.createVerticalGlue());

        // Set initial highlight and refresh the welcome panel
        SwingUtilities.invokeLater(() -> {
            setActiveMenu("welcome");
            refreshPanel(); // Ensure the welcome panel is also refreshed using the new mechanism
        });

        return menuPanel;
    }

    // Create content panels for each section
    private JPanel createWelcomePanel() {
        // Define the new color palette
        Color softWhite = new Color(245, 248, 245);
        Color lightGreen = new Color(165, 214, 167);
        Color mediumGreen = new Color(76, 175, 80);
        Color darkGreen = new Color(46, 125, 50);

        // Main panel with gradient background
        JPanel panel = new JPanel(new BorderLayout(0, 30)) {
            private Image backgroundImage;

            {
                // Load the image (place your image in the resources folder)
                try {
                    backgroundImage = ImageIO.read(getClass().getResource("/images/OLFUBG.jpg"));
                    // If image not found, try to load from file
                    if (backgroundImage == null) {
                        backgroundImage = ImageIO.read(new File("resources/images/campus_background.jpg"));
                    }
                } catch (Exception e) {
                    System.out.println("Could not load background image: " + e.getMessage());
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Draw the background image with low opacity
                if (backgroundImage != null) {
                    // Save original composite
                    Composite originalComposite = g2d.getComposite();

                    // Set transparency (0.2f = 20% opacity)
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

                    // Draw image to fit panel
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);

                    // Restore original composite for subsequent drawing
                    g2d.setComposite(originalComposite);
                }

                // Create a subtle gradient from top to bottom (as already implemented)
                // You can control the blend point by changing the second y value (e.g.,
                // getHeight() * 0.5f for halfway)
                float blendY = getHeight() * 0.45f; // Blend at 50% of the panel height
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(245, 248, 245, 0), // fully transparent at top
                        0, blendY, new Color(225, 255, 225, 255)); // solid white at blendY
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // ... after drawing the background image ...
                // Draw overlay behind the number
                int overlayWidth = 70, overlayHeight = 50;
                int overlayX = (getWidth() - overlayWidth) / 2;
                int overlayY = getHeight() / 2 - overlayHeight / 2;
                g2d.setColor(new Color(255, 255, 255, 200)); // semi-transparent white
                g2d.fillRoundRect(overlayX, overlayY, overlayWidth, overlayHeight, 20, 20);
            }
        };
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Create header panel with logo and title
        JPanel headerPanel = new JPanel(new BorderLayout(20, 0));
        headerPanel.setOpaque(false);

        // Application logo (you can replace with an actual logo)
        JLabel logoLabel;
        try {
            ImageIcon logoIcon = new ImageIcon(getClass().getResource("/images/calendar.png"));
            // Increase the size of the calendar icon
            Image scaledLogo = logoIcon.getImage().getScaledInstance(72, 72, Image.SCALE_SMOOTH); 
            logoLabel = new JLabel(new ImageIcon(scaledLogo));
        } catch (Exception e) {
            // Fallback to text if image not found
            logoLabel = new JLabel("📅"); 
            logoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 72)); // Match larger size
            logoLabel.setForeground(darkGreen);
        }

        // Welcome message with styled title
        // Reduce vertical gap in GridLayout for closer line spacing
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 0)); 
        titlePanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel("Welcome to");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 28)); // Increased font size
        welcomeLabel.setForeground(mediumGreen);

        JLabel titleLabel = new JLabel("Schedule Manager");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 40)); // Increased font size
        titleLabel.setForeground(darkGreen);

        titlePanel.add(welcomeLabel);
        titlePanel.add(titleLabel);

        headerPanel.add(logoLabel, BorderLayout.WEST);
        headerPanel.add(titlePanel, BorderLayout.CENTER);

        // Create a panel for stats with a softer background
        JPanel statsContainer = new JPanel(new BorderLayout());
        statsContainer.setOpaque(false);
        statsContainer.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Add a subtle description
        JLabel descriptionLabel = new JLabel("Your comprehensive solution for academic scheduling");
        descriptionLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        descriptionLabel.setForeground(new Color(100, 100, 100));
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Create stats panel with more elegant layout
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 25, 25));
        statsPanel.setOpaque(false);

    // Add stat cards
    Runnable refreshStats = () -> {
        statsPanel.removeAll();
        statsPanel.add(createEnhancedStatCard("Courses", String.valueOf(backend.getCourses().size()),
                "/images/course.png", new Color(245, 248, 245), new Color(165, 214, 167),
                new Color(76, 175, 80), new Color(46, 125, 50), "courses")); // Added "courses"
        statsPanel.add(createEnhancedStatCard("Sections", String.valueOf(backend.getSections().size()),
                "/images/sections.png", new Color(245, 248, 245), new Color(165, 214, 167),
                new Color(76, 175, 80), new Color(46, 125, 50), "sections")); // Added "sections"
        statsPanel.add(createEnhancedStatCard("Professors", String.valueOf(backend.getProfessors().size()),
                "/images/prof.png", new Color(245, 248, 245), new Color(165, 214, 167),
                new Color(76, 175, 80), new Color(46, 125, 50), "professors")); // Added "professors"
        statsPanel.add(createEnhancedStatCard("Rooms", String.valueOf(backend.getRooms().size()),
                "/images/rooms.png", new Color(245, 248, 245), new Color(165, 214, 167),
                new Color(76, 175, 80), new Color(46, 125, 50), "rooms")); // Added "rooms"
        statsPanel.revalidate();
        statsPanel.repaint();
    };
    refreshStats.run();

        statsContainer.add(descriptionLabel, BorderLayout.NORTH);
        statsContainer.add(statsPanel, BorderLayout.CENTER);

        // Add components to main panel
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(statsContainer, BorderLayout.CENTER);

        // Add a footer with version information
        JLabel versionLabel = new JLabel("Version 1.0");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(150, 150, 150));
        versionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(versionLabel, BorderLayout.SOUTH);
        panel.putClientProperty("refreshStats", refreshStats);

        return panel;
    }

    // Create an enhanced stat card with the green palette
    private JPanel createEnhancedStatCard(String title, String value, String iconPath,
            Color bgColor, Color lightGreen, Color mediumGreen, Color darkGreen, String targetCardName) { // Added targetCardName
        final boolean[] isHovered = {false}; // Keep existing hover logic
        JPanel card = new JPanel() {
            private Image backgroundImage;
            {
                try {
                    // First try with resource
                    backgroundImage = ImageIO.read(getClass().getResource(iconPath));
                    // If that fails, try direct file path
                    if (backgroundImage == null) {
                        backgroundImage = ImageIO.read(new File(iconPath));
                    }
                    // If still null, try with other common directories
                    if (backgroundImage == null) {
                        backgroundImage = ImageIO.read(new File("resources/" + iconPath));
                    }
                } catch (Exception e) {
                    System.out.println("Could not load background image: " + iconPath);
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fill rounded rectangle with background color
                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                // Draw background image with opacity and maintain aspect ratio
                if (backgroundImage != null) {
                    Composite originalComposite = g2d.getComposite();
                    float baseOpacity = 0.3f;
                    float hoverOpacity = 0.45f;
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, isHovered[0] ? hoverOpacity : baseOpacity));
                    // Calculate dimensions to maintain aspect ratio
                    double imageRatio = (double) backgroundImage.getWidth(this) / backgroundImage.getHeight(this);
                    double panelRatio = (double) getWidth() / getHeight();
                    int drawWidth, drawHeight;
                    if (imageRatio > panelRatio) {
                        drawWidth = getWidth();
                        drawHeight = (int) (getWidth() / imageRatio);
                    } else {
                        drawHeight = getHeight();
                        drawWidth = (int) (getHeight() * imageRatio);
                    }
                    int x = (getWidth() - drawWidth) / 2;
                    int y = (getHeight() - drawHeight) / 2;
                    g2d.drawImage(backgroundImage, x, y, drawWidth, drawHeight, this);
                    g2d.setComposite(originalComposite);
                }

                // Add a subtle border
                g2d.setColor(lightGreen);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);

                // Add a subtle bottom accent
                g2d.setColor(mediumGreen);
                g2d.fillRoundRect(0, getHeight() - 8, getWidth(), 8, 8, 8); // Fixed: Added missing arcHeight argument

                // Draw overlay behind the number
                int overlayWidth = 70, overlayHeight = 50;
                int overlayX = (getWidth() - overlayWidth) / 2;
                int overlayY = getHeight() / 2 - overlayHeight / 2;
                g2d.setColor(new Color(255, 255, 255, isHovered[0] ? 230 : 200)); // more opaque on hover
                g2d.fillRoundRect(overlayX, overlayY, overlayWidth, overlayHeight, 20, 20);

                // Draw the value (number) centered in the overlay with shadow
                String valueStr = value;
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 48));
                FontMetrics fm = g2d.getFontMetrics();
                int strWidth = fm.stringWidth(valueStr);
                int strHeight = fm.getAscent();
                int strX = overlayX + (overlayWidth - strWidth) / 2;
                int strY = overlayY + (overlayHeight + strHeight) / 2 - 8;
                g2d.setColor(new Color(0,0,0,80)); // shadow color
                g2d.drawString(valueStr, strX+2, strY+2);
                g2d.setColor(mediumGreen); // main color
                g2d.drawString(valueStr, strX, strY);
            }
        };

        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setOpaque(false);

        // Title in header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(darkGreen);

        headerPanel.add(titleLabel);

        card.add(headerPanel, BorderLayout.NORTH);
        // Remove valueLabel from card.add

        // Add hover effect (syncs with image/overlay)
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                card.setBorder(BorderFactory.createEmptyBorder(18, 18, 22, 22));
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));
                ((JPanel)evt.getSource()).putClientProperty("hovered", true);
                isHovered[0] = true;
                card.repaint();
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                isHovered[0] = false;
                card.repaint();
            }
            // NOTE: Not adding mouseClicked here to keep changes distinct
        });

        // Add a NEW MouseListener specifically for click navigation
        if (targetCardName != null && !targetCardName.isEmpty()) {
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    setActiveMenu(targetCardName);
                    refreshPanel();
                    cardLayout.show(contentPanel, targetCardName);
                }
            });
        }

        return card;
    }

    private JButton createRefreshButton(String cardName) {
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(MENU_FONT);
        refreshButton.setForeground(MENU_FG);
        refreshButton.setBackground(MENU_BG);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        refreshButton.addActionListener(e -> {
            cardLayout.show(contentPanel, cardName);
            refreshPanel();
        });

        return refreshButton;
    }

    private JPanel createSectionsPanel() {
        JPanel panel = createStandardContentPanel("Section Management");

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(BACKGROUND_COLOR);

        JButton addButton = createActionButton("Add Section");
        JButton editButton = createActionButton("Edit Section");
        JButton removeButton = createActionButton("Remove", true); // Negative action

        String[] sortOptions = { "Sort: None", "Name", "Year Level", "Semester", "Students" };
        JComboBox<String> sortCombo = new JComboBox<>(sortOptions);
        styleSortCombo(sortCombo);            // ⬅ apply styling

        // Sub-panel for the buttons (left-aligned)
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftButtons.setBackground(BACKGROUND_COLOR);
        leftButtons.add(addButton);
        leftButtons.add(editButton);
        leftButtons.add(sortCombo); // Moved sortCombo before removeButton
        leftButtons.add(removeButton);

        buttonPanel.add(leftButtons, BorderLayout.WEST);
        // Remove: buttonPanel.add(sortCombo, BorderLayout.EAST);

        panel.add(buttonPanel, BorderLayout.NORTH);
    
        // ...inside createSectionsPanel()...
    // Add table directly to the panel
    String[] columns = { "Section Name", "Year Level", "Semester", "Program" };
    DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
    JTable table = createStyledModelTable(tableModel);
    JScrollPane scrollPane = new JScrollPane(table);
    styleScrollBar(scrollPane.getVerticalScrollBar());
    styleScrollBar(scrollPane.getHorizontalScrollBar());
    scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
    panel.add(scrollPane, BorderLayout.CENTER);

    // Populate table with section data
    Runnable refreshTable = () -> {
        tableModel.setRowCount(0);
        for (SchedulingBackend.Section section : backend.getSections()) {
            tableModel.addRow(new Object[] {
            section.name, section.yearLevel, section.semester, section.courseName
        });
        }
    };
    sortCombo.addActionListener(e -> {
        String sortBy = (String) sortCombo.getSelectedItem();
        java.util.List<Vector<Object>> rows = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            rows.add((Vector<Object>) tableModel.getDataVector().elementAt(i));
        }
        int colIndex = switch (sortBy) {
            case "Name" -> 0;
            case "Year Level" -> 1;
            case "Semester" -> 2;
            case "Students" -> 3;
            default -> -1;
        };
        if (colIndex >= 0) {
            rows.sort((r1, r2) -> r1.get(colIndex).toString().compareToIgnoreCase(r2.get(colIndex).toString()));
            tableModel.setRowCount(0);
            for (Vector<Object> row : rows) tableModel.addRow(row);
        }
    });

    addButton.addActionListener(e -> {
        JTextField courseField = new JTextField(15);
        JTextField yearLevelField = new JTextField(5);
        JTextField semesterField = new JTextField(5);
        JTextField sectionNumberField = new JTextField(10);  // renamed field

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        formPanel.add(new JLabel("Course Name:"));
        formPanel.add(courseField);
        formPanel.add(new JLabel("Year Level:"));
        formPanel.add(yearLevelField);
        formPanel.add(new JLabel("Semester:"));
        formPanel.add(semesterField);
        formPanel.add(new JLabel("Section Number:")); // renamed label
        formPanel.add(sectionNumberField);

        int result = JOptionPane.showConfirmDialog(this, formPanel, "Add New Section", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String courseName = courseField.getText().trim();
                int yearLevel = Integer.parseInt(yearLevelField.getText().trim());
                int semester = Integer.parseInt(semesterField.getText().trim());
                String sectionNumber = sectionNumberField.getText().trim();

                backend.getSections().add(
                    new SchedulingBackend.Section(courseName, yearLevel, semester, sectionNumber)
                );
                backend.saveData();
                showSuccess("Section added successfully!");
            } catch (NumberFormatException ex) {
                showError("Invalid input: Year Level and Semester must be numeric.");
            }
        }
        refreshTable.run();
    });



    editButton.addActionListener(e -> {
        if (backend.getSections().isEmpty()) {
            showError("No sections available to edit!");
            return;
        }

        JComboBox<String> sectionCombo = new JComboBox<>(
            backend.getSections().stream().map(s -> s.name).toArray(String[]::new));
        JTextField nameField = new JTextField();
        JTextField yearField = new JTextField();
        JTextField semesterField = new JTextField();
        JTextField programField = new JTextField();

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        formPanel.add(new JLabel("Select Section:"));
        formPanel.add(sectionCombo);
        formPanel.add(new JLabel("New Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Year Level:"));
        formPanel.add(yearField);
        formPanel.add(new JLabel("Semester:"));
        formPanel.add(semesterField);
        formPanel.add(new JLabel("Program:"));
        formPanel.add(programField);

        // Pre-fill fields for selected section
        sectionCombo.addActionListener(ev -> {
            int index = sectionCombo.getSelectedIndex();
            if (index >= 0) {
                SchedulingBackend.Section selected = backend.getSections().get(index);
                nameField.setText(selected.name);
                yearField.setText(String.valueOf(selected.yearLevel));
                semesterField.setText(String.valueOf(selected.semester));
                programField.setText(selected.courseName);
            }
        });
        // Trigger once for initial selection
        sectionCombo.setSelectedIndex(0);

        int result = JOptionPane.showConfirmDialog(this, formPanel, "Edit Section", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int index = sectionCombo.getSelectedIndex();
                SchedulingBackend.Section selected = backend.getSections().get(index);

                selected.name = nameField.getText().trim();
                selected.yearLevel = Integer.parseInt(yearField.getText().trim());
                selected.semester = Integer.parseInt(semesterField.getText().trim());
                selected.courseName = programField.getText().trim();

                backend.saveData();
                showSuccess("Section updated successfully!");
                refreshPanel();
            } catch (NumberFormatException ex) {
                showError("Invalid input: Year Level and Semester must be numeric.");
            }
        }
        refreshTable.run();
    });


    removeButton.addActionListener(e -> {
        if (backend.getSections().isEmpty()) {
            showError("No sections available to remove!");
            return;
        }

        JComboBox<String> sectionCombo = new JComboBox<>(
            backend.getSections().stream().map(s -> s.name).toArray(String[]::new));

        JButton removeAllButton = new JButton("Remove All");
        JPanel removePanel = new JPanel(new GridLayout(2, 2, 10, 10));
        removePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        removePanel.add(new JLabel("Select Section to Remove:"));
        removePanel.add(sectionCombo);
        removePanel.add(new JLabel());
        removePanel.add(removeAllButton);

        final boolean[] removeAllClicked = {false};
        removeAllButton.addActionListener(ev -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove ALL sections?", "Confirm Remove All", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                backend.getSections().clear();
                backend.saveData();
                showSuccess("All sections removed successfully!");
                removeAllClicked[0] = true;
                SwingUtilities.getWindowAncestor(removePanel).dispose();
                refreshPanel();
            }
        });

        int result = JOptionPane.showConfirmDialog(
            this, removePanel, "Remove Section", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION && !removeAllClicked[0]) {
            int selectedIndex = sectionCombo.getSelectedIndex();
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this section?", "Confirm Remove", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                backend.getSections().remove(selectedIndex);
                backend.saveData();
                showSuccess("Section removed successfully!");
                refreshPanel();
            }
        }
        
        refreshTable.run();
    });

    refreshTable.run();
        return panel;
    }    private JPanel createCoursesPanel() {
        JPanel panel = createStandardContentPanel("Course Management");

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(BACKGROUND_COLOR);

        JButton addButton = createActionButton("Add Course");
        JButton editButton = createActionButton("Edit Course");
        JButton removeButton = createActionButton("Remove", true); // Negative action

        String[] sortOptions = { "Sort: None", "Code", "Name", "Year Level", "Semester", "Program", "Duration", "Room Type" };
        JComboBox<String> sortCombo = new JComboBox<>(sortOptions);
        styleSortCombo(sortCombo);            // ⬅ apply styling

        // Sub-panel for the buttons (left-aligned)
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftButtons.setBackground(BACKGROUND_COLOR);
        leftButtons.add(addButton);
        leftButtons.add(editButton);
        leftButtons.add(sortCombo); // Corrected order: sortCombo before removeButton
        leftButtons.add(removeButton);

        buttonPanel.add(leftButtons, BorderLayout.WEST);
        // Remove: buttonPanel.add(sortCombo, BorderLayout.EAST);

        panel.add(buttonPanel, BorderLayout.NORTH);

        // Add table directly to the panel
        String[] columns = { "Code", "Name", "Year Level", "Semester", "Program", "Duration", "Room Type" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = createStyledModelTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Populate table with course data
        Runnable refreshTable = () -> {
            tableModel.setRowCount(0);
            for (SchedulingBackend.Course course : backend.getCourses()) {
                tableModel.addRow(new Object[] {
                    course.code, course.name, course.yearLevel, course.semester, course.programIntended,
                    course.durationHours, course.requiredRoomType
                });
            }
        };
        sortCombo.addActionListener(e -> {
            String sortBy = (String) sortCombo.getSelectedItem();
            java.util.List<Vector<Object>> rows = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                rows.add((Vector<Object>) tableModel.getDataVector().elementAt(i));
            }
            int colIndex = switch (sortBy) {
                case "Code" -> 0;
                case "Name" -> 1;
                case "Year Level" -> 2;
                case "Semester" -> 3;
                case "Program" -> 4;
                case "Duration" -> 5;
                case "Room Type" -> 6;
                default -> -1;
            };
            if (colIndex >= 0) {
                rows.sort((r1, r2) -> r1.get(colIndex).toString().compareToIgnoreCase(r2.get(colIndex).toString()));
                tableModel.setRowCount(0);
                for (Vector<Object> row : rows) tableModel.addRow(row);
            }
        });

        // Add button implementations (similar to the original dialog logic)
        addButton.addActionListener(e -> {
            JTextField codeField = new JTextField(15);
            JTextField nameField = new JTextField(15);
            JTextField yearLevelField = new JTextField(5);
            JTextField semesterField = new JTextField(5);
            JTextField programField = new JTextField(10);
            JTextField durationField = new JTextField(5);
            JTextField requiredroomField = new JTextField(10);

            JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10)); // now 7 rows
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            formPanel.add(new JLabel("Course Code:"));
            formPanel.add(codeField);
            formPanel.add(new JLabel("Course Name:"));
            formPanel.add(nameField);
            formPanel.add(new JLabel("Year Level:"));
            formPanel.add(yearLevelField);
            formPanel.add(new JLabel("Semester:"));
            formPanel.add(semesterField);
            formPanel.add(new JLabel("Program Intended:"));
            formPanel.add(programField);
            formPanel.add(new JLabel("Duration Hours:"));
            formPanel.add(durationField);
            formPanel.add(new JLabel("Required Room Type:"));
            formPanel.add(requiredroomField);

            int result = JOptionPane.showConfirmDialog(this, formPanel, "Add New Course", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String code = codeField.getText().trim();
                String name = nameField.getText().trim();
                int yearLevel = Integer.parseInt(yearLevelField.getText().trim());
                int semester = Integer.parseInt(semesterField.getText().trim());
                String program = programField.getText().trim();
                int duration = Integer.parseInt(durationField.getText().trim());
                String requiredRoom = requiredroomField.getText().trim();

                backend.getCourses().add(
                        new SchedulingBackend.Course(code, name, yearLevel, semester, program, duration, requiredRoom));
                backend.saveData();
                showSuccess("Course added successfully!");
            }
            refreshTable.run(); // Refresh the table after adding a course
        });

        editButton.addActionListener(e -> {
            if (backend.getCourses().isEmpty()) {
                showError("No courses available to edit!");
                return;
            }

            JComboBox<String> courseCombo = new JComboBox<>(
                    backend.getCourses().stream().map(c -> c.code + " - " + c.name).toArray(String[]::new));
            JTextField nameField = new JTextField();
            JTextField yearField = new JTextField();
            JTextField semField = new JTextField();
            JTextField programField = new JTextField();
            JTextField durationField = new JTextField();
            JTextField roomTypeField = new JTextField();

            JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10));
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            formPanel.add(new JLabel("Select Course:"));
            formPanel.add(courseCombo);
            formPanel.add(new JLabel("New Name:"));
            formPanel.add(nameField);
            formPanel.add(new JLabel("Year Level:"));
            formPanel.add(yearField);
            formPanel.add(new JLabel("Semester:"));
            formPanel.add(semField);
            formPanel.add(new JLabel("Program Intended:"));
            formPanel.add(programField);
            formPanel.add(new JLabel("Duration Hours:"));
            formPanel.add(durationField);
            formPanel.add(new JLabel("Required Room Type:"));
            formPanel.add(roomTypeField);

            int result = JOptionPane.showConfirmDialog(this, formPanel, "Edit Course", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                int index = courseCombo.getSelectedIndex();
                SchedulingBackend.Course selected = backend.getCourses().get(index);

                selected.name = nameField.getText().trim();
                selected.yearLevel = Integer.parseInt(yearField.getText().trim());
                selected.semester = Integer.parseInt(semField.getText().trim());
                selected.programIntended = programField.getText().trim();
                selected.durationHours = Integer.parseInt(durationField.getText().trim());
                selected.requiredRoomType = roomTypeField.getText().trim();

                backend.saveData();
                showSuccess("Course updated successfully!");
                refreshPanel();
            }
            refreshTable.run();
        });

        removeButton.addActionListener(e -> {
            if (backend.getCourses().isEmpty()) {
                showError("No courses available to remove!");
                return;
            }

        
            JComboBox<String> courseCombo = new JComboBox<>(
                    backend.getCourses().stream().map(c -> c.code + " - " + c.name).toArray(String[]::new));
        
            JButton removeAllButton = new JButton("Remove All");
            JPanel removePanel = new JPanel(new GridLayout(2, 2, 10, 10));
            removePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            removePanel.add(new JLabel("Select Course to Remove:"));
            removePanel.add(courseCombo);
            removePanel.add(new JLabel()); // empty cell
            removePanel.add(removeAllButton);
        
            final boolean[] removeAllClicked = {false};
            removeAllButton.addActionListener(ev -> {
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove ALL courses?", "Confirm Remove All", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    backend.getCourses().clear();
                    backend.saveData();
                    showSuccess("All courses removed successfully!");
                    removeAllClicked[0] = true;
                    SwingUtilities.getWindowAncestor(removePanel).dispose();
                    refreshPanel();
                }
            });
        
            int result = JOptionPane.showConfirmDialog(
                    this, removePanel, "Remove Course", JOptionPane.OK_CANCEL_OPTION);
        
            if (result == JOptionPane.OK_OPTION && !removeAllClicked[0]) {
                int selectedIndex = courseCombo.getSelectedIndex();
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this course?", "Confirm Remove", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    backend.getCourses().remove(selectedIndex);
                    backend.saveData();
                    showSuccess("Course removed successfully!");
                    refreshPanel();
                }
            }
            
            refreshTable.run();
        });        // Initialize the table with data
        refreshTable.run();
        return panel;
    }

    private JPanel createProfessorsPanel() {
        JPanel panel = createStandardContentPanel("Professor Management");
    
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        JButton addButton = createActionButton("Add Professor");
        JButton editButton = createActionButton("Edit Professor");
        JButton removeButton = createActionButton("Remove", true); // Negative action

        String[] sortOptions = { "Sort: None", "ID", "Name", "Specialization" };
        JComboBox<String> sortCombo = new JComboBox<>(sortOptions);
        styleSortCombo(sortCombo);

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(sortCombo); // SortCombo added before removeButton
        buttonPanel.add(removeButton); // removeButton is now last in this FlowLayout

        panel.add(buttonPanel, BorderLayout.NORTH);
    
        // Add table directly to the panel
        String[] columns = { "ID", "Name", "Specialization" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = createStyledModelTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        panel.add(scrollPane, BorderLayout.CENTER);
    
        // Populate table with professor data
        Runnable refreshTable = () -> {
            tableModel.setRowCount(0);
            for (SchedulingBackend.Professor professor : backend.getProfessors()) {
                tableModel.addRow(new Object[] {
                    professor.id, professor.name, professor.specialization
                });
            }
        };

        sortCombo.addActionListener(e -> {
            String sortBy = (String) sortCombo.getSelectedItem();
            java.util.List<Vector<Object>> rows = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                rows.add((Vector<Object>) tableModel.getDataVector().elementAt(i));
            }
            int colIndex = switch (sortBy) {
                case "ID" -> 0;
                case "Name" -> 1;
                case "Specialization" -> 2;
                default -> -1;
            };
            if (colIndex >= 0) {
                rows.sort((r1, r2) -> r1.get(colIndex).toString().compareToIgnoreCase(r2.get(colIndex).toString()));
                tableModel.setRowCount(0);
                for (Vector<Object> row : rows) tableModel.addRow(row);
            }
        }); 

        addButton.addActionListener(e -> {
            JTextField idField = new JTextField(15);
            JTextField nameField = new JTextField(15);
            JTextField specializationField = new JTextField(10); // NEW

            JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            formPanel.add(new JLabel("Professor ID:"));
            formPanel.add(idField);
            formPanel.add(new JLabel("Professor Name:"));
            formPanel.add(nameField);
            formPanel.add(new JLabel("Specialization (e.g., BSCS or ANY):"));
            formPanel.add(specializationField);

            int result = JOptionPane.showConfirmDialog(
                    this, formPanel, "Add New Professor", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                String id = idField.getText().trim();
                String name = nameField.getText().trim();
                String specialization = specializationField.getText().trim();

                if (id.isEmpty() || name.isEmpty()) {
                    showError("Both fields are required!");
                    return;
                }

                if (backend.getProfessors().stream().anyMatch(p -> p.id.equalsIgnoreCase(id))) {
                    showError("Professor ID already exists!");
                    return;
                }

                backend.getProfessors().add(new SchedulingBackend.Professor(id, name, specialization));
                backend.saveData();
                showSuccess("Professor added successfully!");
                refreshPanel();
            }
            refreshTable.run();
        });

        editButton.addActionListener(e -> {
            if (backend.getProfessors().isEmpty()) {
                showError("No professors available to edit!");
                return;
            }

            JComboBox<String> profCombo = new JComboBox<>(
                    backend.getProfessors().stream().map(p -> p.id + " - " + p.name).toArray(String[]::new));
            JTextField nameField = new JTextField();
            JTextField specializationField = new JTextField();

            JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            formPanel.add(new JLabel("Select Professor:"));
            formPanel.add(profCombo);
            formPanel.add(new JLabel("New Name:"));
            formPanel.add(nameField);
            formPanel.add(new JLabel("Specialization:"));
            formPanel.add(specializationField);

            int result = JOptionPane.showConfirmDialog(this, formPanel, "Edit Professor", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                int index = profCombo.getSelectedIndex();
                SchedulingBackend.Professor selected = backend.getProfessors().get(index);
                selected.name = nameField.getText().trim();
                selected.specialization = specializationField.getText().trim();

                backend.saveData();
                showSuccess("Professor updated successfully!");
                refreshPanel();
            }
            refreshTable.run();
        });

        removeButton.addActionListener(e -> {
            if (backend.getProfessors().isEmpty()) {
                showError("No professors available to remove!");
                return;
            }
        
            JComboBox<String> professorCombo = new JComboBox<>(
                    backend.getProfessors().stream().map(p -> p.id + " - " + p.name).toArray(String[]::new));
        
            JButton removeAllButton = new JButton("Remove All");
            JPanel removePanel = new JPanel(new GridLayout(2, 2, 10, 10));
            removePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            removePanel.add(new JLabel("Select Professor to Remove:"));
            removePanel.add(professorCombo);
            removePanel.add(new JLabel());
            removePanel.add(removeAllButton);
        
            final boolean[] removeAllClicked = {false};
            removeAllButton.addActionListener(ev -> {
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove ALL professors?", "Confirm Remove All", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    backend.getProfessors().clear();
                    backend.saveData();
                    showSuccess("All professors removed successfully!");
                    removeAllClicked[0] = true;
                    SwingUtilities.getWindowAncestor(removePanel).dispose();
                    refreshPanel();
                }
            });
        
            int result = JOptionPane.showConfirmDialog(
                    this, removePanel, "Remove Professor", JOptionPane.OK_CANCEL_OPTION);
        
            if (result == JOptionPane.OK_OPTION && !removeAllClicked[0]) {
                int selectedIndex = professorCombo.getSelectedIndex();
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this professor?", "Confirm Remove", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    backend.getProfessors().remove(selectedIndex);
                    backend.saveData();
                    showSuccess("Professor removed successfully!");
                    refreshPanel();
                }
            }
            refreshTable.run();
        });
        
        // Border for the scrollPane was already added earlier
        // We already added the scrollPane to the panel in the code above
        refreshTable.run();
        return panel;
    }

private void refreshPanel() {
    switch (currentActiveCard) {
        case "welcome" -> {
            backend.loadData(); // Ensure the backend reloads data from the database
            JPanel welcomePanel = createWelcomePanel();
            contentPanel.add(welcomePanel, "welcome");
            cardLayout.show(contentPanel, "welcome");
        }
        case "courses" -> {
            // Reload courses from the database
            backend.loadData(); // Ensure the backend reloads data from the database
            backend.deleteCourse("PL129");
backend.deleteCourse("PL316");
backend.deleteCourse("PL967");
            backend.saveData();

            JPanel coursesPanel = createCoursesPanel();
            contentPanel.add(coursesPanel, "courses");
        }
        case "sections" -> {
            // Reload sections from the database
            backend.loadData();
            backend.deleteCourse("PL129");
backend.deleteCourse("PL316");
backend.deleteCourse("PL967");
            backend.saveData();

            JPanel sectionsPanel = createSectionsPanel();
            contentPanel.add(sectionsPanel, "sections");
        }
        case "professors" -> {
            // Reload professors from the database
            backend.loadData();
            backend.deleteCourse("PL129");
backend.deleteCourse("PL316");
backend.deleteCourse("PL967");
            backend.saveData();

            JPanel professorsPanel = createProfessorsPanel();
            contentPanel.add(professorsPanel, "professors");
        }
        case "rooms" -> {
            // Reload rooms from the database
            backend.loadData();
            backend.deleteCourse("PL129");
backend.deleteCourse("PL316");
backend.deleteCourse("PL967");
            backend.saveData();


            JPanel roomsPanel = createRoomsPanel();
            contentPanel.add(roomsPanel, "rooms");
        }
        case "manualschedule" -> {
            backend.loadData();
            backend.deleteCourse("PL129");
backend.deleteCourse("PL316");
backend.deleteCourse("PL967");
            backend.saveData();

            JPanel manualSchedulePanel = createManualSchedulePanel(); // This assigns to manualScheduleSplitPane
            contentPanel.add(manualSchedulePanel, "manualschedule");
            // Show the card first, then adjust its split pane in invokeLater
            cardLayout.show(contentPanel, currentActiveCard); 

            if (manualScheduleSplitPane != null) {
                SwingUtilities.invokeLater(() -> {
                    // Ensure components have a chance to report their preferred sizes
                    manualScheduleSplitPane.resetToPreferredSizes(); 
                    manualScheduleSplitPane.setDividerLocation(0.6); // Apply proportional location
                    
                    manualScheduleSplitPane.revalidate();
                    manualScheduleSplitPane.repaint();
                    if (manualScheduleSplitPane.getParent() != null) {
                        manualScheduleSplitPane.getParent().revalidate();
                        manualScheduleSplitPane.getParent().repaint();
                    }
                });
            }
        }
        case "schedulemanagement" -> {
            // Reload schedule management from the database
            backend.loadData();
            backend.deleteCourse("PL129");
backend.deleteCourse("PL316");
backend.deleteCourse("PL967");
            backend.saveData();

            JPanel scheduleManagementPanel = createScheduleManagementPanel();
            contentPanel.add(scheduleManagementPanel, "schedulemanagement");
        }
        case "smartschedule" -> {
            // Reload smart schedule from the database
            backend.loadData();
            backend.deleteCourse("PL129");
backend.deleteCourse("PL316");
backend.deleteCourse("PL967");
            backend.saveData();

            JPanel smartSchedulePanel = createSmartSchedulePanel();
            contentPanel.add(smartSchedulePanel, "smartschedule");
        }
        default -> {
            // Default case for other panels
            cardLayout.show(contentPanel, currentActiveCard);
        }
    }
    cardLayout.show(contentPanel, currentActiveCard); // Ensure the current card is displayed
}

    private JPanel createRoomsPanel() {
        JPanel panel = createStandardContentPanel("Room Management");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Changed from BorderLayout
        buttonPanel.setBackground(BACKGROUND_COLOR);

        JButton addButton = createActionButton("Add Room");
        JButton editButton = createActionButton("Edit Room");
        JButton removeButton = createActionButton("Remove", true); // Changed label, kept negative action

        String[] sortOptions = { "Sort: None", "Building", "Floor", "Room Number", "Capacity", "Room Type" };
        JComboBox<String> sortCombo = new JComboBox<>(sortOptions);
        styleSortCombo(sortCombo);

        // Sub-panel for the buttons (left-aligned)
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); // hgap to 0 for strut control
        leftButtons.setBackground(BACKGROUND_COLOR);

        leftButtons.add(addButton);
        leftButtons.add(Box.createHorizontalStrut(10));
        leftButtons.add(editButton);
        leftButtons.add(Box.createHorizontalStrut(15)); // Spacing before sort combo
        leftButtons.add(sortCombo); 
        leftButtons.add(Box.createHorizontalStrut(15)); // Spacing after sort combo
        leftButtons.add(removeButton);

        // The buttonPanel now directly adds leftButtons (or you could add leftButtons directly to the main panel's NORTH)
        buttonPanel.add(leftButtons); 

        panel.add(buttonPanel, BorderLayout.NORTH);

        String[] columns = { "Building", "Floor", "Room Number", "Capacity", "Room Type" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = createStyledModelTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        panel.add(scrollPane, BorderLayout.CENTER);
    
        // Populate table with room data
        Runnable refreshTable = () -> {
            tableModel.setRowCount(0);
            for (SchedulingBackend.Room room : backend.getRooms()) {
                tableModel.addRow(new Object[] {
                    room.building, room.floor, room.roomNumber, room.capacity, room.roomType
                });
            }
        };
        sortCombo.addActionListener(e -> {
            String sortBy = (String) sortCombo.getSelectedItem();
            java.util.List<Vector<Object>> rows = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                rows.add((Vector<Object>) tableModel.getDataVector().elementAt(i));
            }
            int colIndex = switch (sortBy) {
                case "Building" -> 0;
                case "Floor" -> 1;
                case "Room Number" -> 2;
                case "Capacity" -> 3;
                case "Room Type" -> 4;
                default -> -1;
            };
            if (colIndex >= 0) {
                rows.sort((r1, r2) -> r1.get(colIndex).toString().compareToIgnoreCase(r2.get(colIndex).toString()));
                tableModel.setRowCount(0);
                for (Vector<Object> row : rows) tableModel.addRow(row);
            }
        });

        addButton.addActionListener(e -> {
            JTextField buildingField = new JTextField(10);
            JTextField floorField = new JTextField(5);
            JTextField roomNumberField = new JTextField(5);
            JTextField capacityField = new JTextField(5);
            JTextField roomTypeField = new JTextField(10); // NEW

            JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            formPanel.add(new JLabel("Building:"));
            formPanel.add(buildingField);
            formPanel.add(new JLabel("Floor:"));
            formPanel.add(floorField);
            formPanel.add(new JLabel("Room Number:"));
            formPanel.add(roomNumberField);
            formPanel.add(new JLabel("Capacity:"));
            formPanel.add(capacityField);
            formPanel.add(new JLabel("Room Type:"));
            formPanel.add(roomTypeField);

            int result = JOptionPane.showConfirmDialog(this, formPanel, "Add New Room", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    
                    String building = buildingField.getText().trim();
                    String floor = floorField.getText().trim();
                    String roomNumber = roomNumberField.getText().trim();
                    int capacity = Integer.parseInt(capacityField.getText().trim());
                    String roomType = roomTypeField.getText().trim();
                    String roomId = building + "-" + floor + "-" + roomNumber;  // Auto-generate


                    SchedulingBackend.Room room = new SchedulingBackend.Room(roomId, building, floor, roomNumber, capacity, roomType);


                    if (backend.getRooms().stream().anyMatch(r -> r.id.equalsIgnoreCase(room.id))) {
                        showError("Room already exists!");
                        return;
                    }

                    backend.getRooms().add(room);
                    backend.saveData();
                    showSuccess("Room added successfully!");
                    refreshPanel();
                } catch (Exception ex) {
                    showError("Please enter valid data!");
                }
            }
        });


        editButton.addActionListener(e -> {
            if (backend.getRooms().isEmpty()) {
                showError("No rooms available to edit!");
                return;
            }

            JComboBox<String> roomCombo = new JComboBox<>(
                    backend.getRooms().stream().map(r -> r.id).toArray(String[]::new));

            JTextField buildingField = new JTextField();
            JTextField floorField = new JTextField();
            JTextField roomNumberField = new JTextField();
            JTextField capacityField = new JTextField();
            JTextField roomTypeField = new JTextField();

            JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            formPanel.add(new JLabel("Select Room:"));
            formPanel.add(roomCombo);
            formPanel.add(new JLabel("Building:"));
            formPanel.add(buildingField);
            formPanel.add(new JLabel("Floor:"));
            formPanel.add(floorField);
            formPanel.add(new JLabel("Room Number:"));
            formPanel.add(roomNumberField);
            formPanel.add(new JLabel("Capacity:"));
            formPanel.add(capacityField);
            formPanel.add(new JLabel("Room Type:"));
            formPanel.add(roomTypeField);

            int result = JOptionPane.showConfirmDialog(this, formPanel, "Edit Room", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    int index = roomCombo.getSelectedIndex();
                    String building = buildingField.getText().trim();
                    String floor = floorField.getText().trim();
                    String roomNumber = roomNumberField.getText().trim();
                    int capacity = Integer.parseInt(capacityField.getText().trim());
                    String roomType = roomTypeField.getText().trim();
                    String roomId = building + "-" + floor + "-" + roomNumber;  // Auto-generate

                    

                    SchedulingBackend.Room room = new SchedulingBackend.Room(roomId, building, floor, roomNumber, capacity, roomType);

                    backend.getRooms().set(index, room);
                    backend.saveData();
                    showSuccess("Room updated successfully!");
                    refreshPanel();
                } catch (Exception ex) {
                    showError("Invalid input!");
                }
            }
        });

        removeButton.addActionListener(e -> {
            var rooms = backend.getRooms();
        
            if (rooms.isEmpty()) {
                showError("No rooms available to remove!");
                return;
            }
        
            JComboBox<String> roomCombo = new JComboBox<>();
            for (SchedulingBackend.Room room : rooms) {
                roomCombo.addItem(room.id + " (Capacity: " + room.capacity + ")");
            }
        
            JButton removeAllButton = new JButton("Remove All");
            JPanel removePanel = new JPanel(new GridLayout(2, 2, 10, 10));
            removePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            removePanel.add(new JLabel("Select Room to Remove:"));
            removePanel.add(roomCombo);
            removePanel.add(new JLabel());
            removePanel.add(removeAllButton);
        
            final boolean[] removeAllClicked = {false};
            removeAllButton.addActionListener(ev -> {
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove ALL rooms?", "Confirm Remove All", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    rooms.clear();
                    backend.saveData();
                    showSuccess("All rooms removed successfully!");
                    removeAllClicked[0] = true;
                    SwingUtilities.getWindowAncestor(removePanel).dispose();
                    refreshPanel();
                }
            });
        
            int result = JOptionPane.showConfirmDialog(this, removePanel, "Remove Room", JOptionPane.OK_CANCEL_OPTION);
        
            if (result == JOptionPane.OK_OPTION && !removeAllClicked[0]) {
                int selectedIndex = roomCombo.getSelectedIndex();
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this room?", "Confirm Remove", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (selectedIndex >= 0 && selectedIndex < rooms.size()) {
                        rooms.remove(selectedIndex);
                        backend.saveData();
                        showSuccess("Room removed successfully!");
                        refreshPanel();
                    } else {
                        showError("Invalid room selection!");
                    }
                }
            }
            refreshTable.run();
        });
        refreshTable.run();
        return panel;
    }

    @SuppressWarnings("unchecked")
    private JPanel createManualSchedulePanel() {
        JPanel mainPanel = createStandardContentPanel("Manual Schedule");

        // Table model and columns
        String[] columns = { "Section", "Course", "Professor", "Room", "Day", "Time" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable scheduleTable = createStyledModelTable(tableModel); // Custom method for styled JTable
        scheduleTable.setPreferredScrollableViewportSize(new Dimension(500, 400));
        JScrollPane tableScroll = new JScrollPane(scheduleTable);
        styleScrollBar(tableScroll.getVerticalScrollBar());
        styleScrollBar(tableScroll.getHorizontalScrollBar());
        tableScroll.setBorder(BorderFactory.createTitledBorder("Manual Schedule List"));

        // Method to refresh table
        Runnable refreshTable = () -> {
            tableModel.setRowCount(0);
            for (ScheduleEntry entry : backend.getScheduleEntries()) {
                tableModel.addRow(new Object[] {
                        entry.section.name,
                        entry.course.name,
                        entry.professor.name,
                        entry.room.id,
                        entry.day,
                        String.format("%02d:00 - %02d:00", entry.startHour, (entry.startHour + entry.course.durationHours) % 24)
                });
            }
        };
        refreshTable.run();

        // Form panel
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 15, 15));
        formPanel.setBackground(BACKGROUND_COLOR);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Add Manual Schedule"),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        JComboBox<String> sectionCombo = new JComboBox<>();
        JComboBox<String> courseCombo = new JComboBox<>();
        JComboBox<String> professorCombo = new JComboBox<>();
        JComboBox<String> roomCombo = new JComboBox<>();
        JComboBox<String> timeCombo = new JComboBox<>();

        JPanel dayPanel = new JPanel();
        // Change layout to GridLayout for two columns
        dayPanel.setLayout(new GridLayout(0, 2, 5, 5)); // 0 rows, 2 columns, 5px hgap, 5px vgap
        dayPanel.setBackground(BACKGROUND_COLOR);
        String[] days = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
        JCheckBox[] dayCheckboxes = new JCheckBox[days.length];
        for (int i = 0; i < days.length; i++) {
            dayCheckboxes[i] = new JCheckBox(days[i]);
            dayCheckboxes[i].setBackground(BACKGROUND_COLOR);
            dayPanel.add(dayCheckboxes[i]);
        }

        // Adjust preferred, minimum, and maximum sizes if necessary, or remove if GridLayout handles it well
        // dayPanel.setPreferredSize(new Dimension(140, 180)); 
        // dayPanel.setMinimumSize(new Dimension(140, 180));
        // dayPanel.setMaximumSize(new Dimension(140, 180));

        for (JCheckBox cb : dayCheckboxes) {
            cb.setIcon(new RoundCheckBoxIcon(14, BACKGROUND_COLOR));
            cb.setSelectedIcon(new RoundCheckBoxIcon(14, PRIMARY_COLOR));
            cb.setFont(CONTENT_FONT);
            cb.setForeground(Color.DARK_GRAY);
            cb.setOpaque(false);
        }

        // Populate dropdowns
        backend.getSections().forEach(s -> sectionCombo.addItem(s.name));
        backend.getCourses().forEach(c -> courseCombo.addItem(c.code + " - " + c.name));
        backend.getProfessors().forEach(p -> professorCombo.addItem(p.id + " - " + p.name));
        backend.getRooms().forEach(r -> roomCombo.addItem(r.id + " (" + r.roomType + ")"));

        courseCombo.addActionListener(e -> {
            timeCombo.removeAllItems();
            int selectedIndex = courseCombo.getSelectedIndex();
            if (selectedIndex >= 0) {
                int duration = backend.getCourses().get(selectedIndex).durationHours;
                for (int hour = 7; hour <= 21 - duration; hour++) {
                    timeCombo.addItem(String.format("%02d:00 - %02d:00", hour, hour + duration));
                }
            }
        });
        
        // Initialize time dropdown if a course is already selected
        if (courseCombo.getItemCount() > 0) {
            courseCombo.setSelectedIndex(0);  // This will trigger the actionListener above
        }

        formPanel.add(new JLabel("Section:"));
        formPanel.add(sectionCombo);
        formPanel.add(new JLabel("Course:"));
        formPanel.add(courseCombo);
        formPanel.add(new JLabel("Professor:"));
        formPanel.add(professorCombo);
        formPanel.add(new JLabel("Room:"));
        formPanel.add(roomCombo);
        formPanel.add(new JLabel("Course Hours:"));
        formPanel.add(timeCombo);
        formPanel.add(new JLabel("Day(s):"));
        formPanel.add(dayPanel);

        // …existing code in createManualSchedulePanel() after you add sectionCombo,
        // courseCombo, professorCombo, roomCombo, timeCombo…

        // Apply the standard styling using styleSortCombo
        int manualScheduleDropdownWidth = 180; // Defined width for these dropdowns
        styleSortCombo(sectionCombo, true, manualScheduleDropdownWidth);
        styleSortCombo(courseCombo, true, manualScheduleDropdownWidth);
        styleSortCombo(professorCombo, true, manualScheduleDropdownWidth);
        styleSortCombo(roomCombo, true, manualScheduleDropdownWidth);
        styleSortCombo(timeCombo, true, manualScheduleDropdownWidth);
        // Remove the old custom styling loop for these JComboBoxes
        /*
        for (JComboBox<String> combo : new JComboBox[] { sectionCombo, courseCombo, professorCombo, roomCombo,
                timeCombo }) {
            combo.setUI(new BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton b = new JButton("▼");
                    b.setBorder(new RoundedBorder(8));
                    b.setContentAreaFilled(false);
                    b.setOpaque(true);
                    b.setBackground(SECONDARY_COLOR);
                    return b;
                }
            });
            combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
                JLabel l = new JLabel(value);
                l.setOpaque(true);
                l.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                if (isSelected) {
                    l.setBackground(PRIMARY_COLOR);
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(BACKGROUND_COLOR);
                    l.setForeground(Color.DARK_GRAY);
                }
                l.setFont(CONTENT_FONT);
                return l;
            });
            combo.setBackground(BACKGROUND_COLOR);
            combo.setBorder(new RoundedBorder(8));
        }
        */

        // Add button
        JButton addButton = createActionButton("Add Schedule");
        addButton.addActionListener(e -> {
            int courseIdx = courseCombo.getSelectedIndex();
            int sectionIdx = sectionCombo.getSelectedIndex();
            int profIdx = professorCombo.getSelectedIndex();
            int roomIdx = roomCombo.getSelectedIndex();
            String timeText = (String) timeCombo.getSelectedItem();

            if (courseIdx < 0 || sectionIdx < 0 || profIdx < 0 || roomIdx < 0 || timeText == null) {
                showError("Please fill out all fields.");
                return;
            }

            int startHour = Integer.parseInt(timeText.split(":")[0]);
            int endHour = Integer.parseInt(timeText.split("-")[1].trim().split(":")[0]);

            StringBuilder daysSelected = new StringBuilder();
            for (JCheckBox box : dayCheckboxes) {
                if (box.isSelected()) {
                    if (daysSelected.length() > 0)
                        daysSelected.append(", ");
                    daysSelected.append(box.getText());
                }
            }

            if (daysSelected.length() == 0) {
                showError("Please select at least one day.");
                return;
            }

            // Add only one row to the list, with grouped days
            backend.getScheduleEntries().add(new ScheduleEntry(
                    backend.getSections().get(sectionIdx),
                    backend.getCourses().get(courseIdx),
                    backend.getProfessors().get(profIdx),
                    backend.getRooms().get(roomIdx),
                    daysSelected.toString(), startHour, endHour));

            backend.saveData();
            refreshTable.run();
            showSuccess("Schedule added!");
        });

        // Right panel (form + button)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(BACKGROUND_COLOR);
        rightPanel.add(formPanel, BorderLayout.CENTER);
        rightPanel.add(addButton, BorderLayout.SOUTH);
        rightPanel.setMinimumSize(new Dimension(350, 200)); 
        // Give the form panel a preferred width to guide the JSplitPane
        rightPanel.setPreferredSize(new Dimension(400, rightPanel.getPreferredSize().height)); 

        // Left panel (table)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(BACKGROUND_COLOR);
        leftPanel.add(tableScroll, BorderLayout.CENTER);
        leftPanel.setMinimumSize(new Dimension(300, 200)); // Add minimum size

        // Split layout
        manualScheduleSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        // Give more resize weight to the left panel (table), making the form panel more fixed
        manualScheduleSplitPane.setResizeWeight(0.7); 
        manualScheduleSplitPane.setContinuousLayout(true);
        manualScheduleSplitPane.setDividerSize(8); 

        mainPanel.add(manualScheduleSplitPane, BorderLayout.CENTER);

        return mainPanel;
    }

    @SuppressWarnings("unchecked")
    private JPanel createScheduleManagementPanel() {
        JPanel panel = createStandardContentPanel("Schedule Management");
        DefaultTableModel tableModel = new DefaultTableModel(
                new String[] { "Section", "Course", "Professor", "Room", "Day", "Time" }, 0);
        JTable table = createStyledModelTable(tableModel); // Use your styled table method

        panel.setLayout(new BorderLayout()); // Enable top/middle/bottom sections

        Runnable loadAllEntriesToTable = () -> {
            tableModel.setRowCount(0);
            // Ensure backend data is fresh if this is the primary load point after an action
            // backend.loadData(); // Consider if needed here or handled by refreshPanel
            for (ScheduleEntry entry : backend.getScheduleEntries()) {
                tableModel.addRow(new Object[] {
                        entry.section.name,
                        entry.course.name,
                        entry.professor.name,
                        entry.room.id,
                        entry.day,
                        String.format("%02d:00 - %02d:00", entry.startHour, entry.endHour)
                });
            }
        };

        // === Top Controls ===
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10)); // hgap = 0, vgap = 10
        topPanel.setBackground(BACKGROUND_COLOR);

        // Filter Category Dropdown
        String[] filterCategories = { "Course", "Room", "Section", "Professor", "Day", "Time" };
        filterCategoryCombo = new JComboBox<>(filterCategories);
        filterValueCombo = new JComboBox<>();

        // Define the filtering logic as a Runnable
        Runnable applyFilterAction = () -> {
            String selectedCategory = (String) filterCategoryCombo.getSelectedItem();
            String selectedValue = (String) filterValueCombo.getSelectedItem();

            if (selectedValue == null || selectedCategory == null) return; // Nothing selected, or combo is being repopulated

            List<ScheduleEntry> allEntries = backend.getScheduleEntries(); 
            List<ScheduleEntry> filteredEntries;

            if ("All Values".equals(selectedValue)) {
                filteredEntries = new ArrayList<>(allEntries); // Show all
            } else {
                filteredEntries = allEntries.stream()
                    .filter(entry -> {
                        String entryValue = "";
                        switch (selectedCategory) {
                            case "Section": entryValue = entry.section.name; break;
                            case "Course": entryValue = entry.course.name; break;
                            case "Professor": entryValue = entry.professor.name; break;
                            case "Room": entryValue = entry.room.id; break;
                            case "Day": entryValue = entry.day; break;
                            case "Time": 
                                entryValue = String.format("%02d:00 - %02d:00", entry.startHour, entry.endHour);
                                break;
                        }
                        return selectedValue.equals(entryValue);
                    })
                    .collect(Collectors.toList());
            }
            refreshScheduleTable(tableModel, filteredEntries);
        };

        filterCategoryCombo.addActionListener(e -> {
            String selectedCategory = (String) filterCategoryCombo.getSelectedItem();
            filterValueCombo.removeAllItems();

            filterValueCombo.addItem("All Values"); 
            filterValueCombo.setSelectedItem("All Values");

            List<String> values = backend.getScheduleEntries().stream().map(entry -> {
                return switch (selectedCategory) {
                    case "Section" -> entry.section.name;
                    case "Course" -> entry.course.name;
                    case "Professor" -> entry.professor.name;
                    case "Room" -> entry.room.id;
                    case "Day" -> entry.day;
                    case "Time" -> String.format("%02d:00 - %02d:00", entry.startHour, entry.endHour);
                    default -> "";
                };
            }).distinct().sorted().collect(Collectors.toList());

            for (String val : values) {
                filterValueCombo.addItem(val);
            }
            // When category changes, apply filter with default "All Values"
            applyFilterAction.run(); 
        });

        // Add ActionListener to filterValueCombo to apply the filter
        filterValueCombo.addActionListener(e_filter -> applyFilterAction.run());

        // Add sort combo box
        String[] sortOptions = { "None", "Section", "Course", "Professor", "Room", "Day", "Time" };
        JComboBox<String> sortCombo = new JComboBox<>(sortOptions);

        sortCombo.addActionListener(e2 -> {
            String sortBy = (String) sortCombo.getSelectedItem();
            // Using the captured tableModel instance from the panel's setup

            if ("None".equals(sortBy)) {
                loadAllEntriesToTable.run(); // Reloads from backend in default order
                return;
            }

            // Create a list of COPIES of row data to sort
            java.util.List<Vector<Object>> rowsToRender = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Vector<Object> rowDataCopy = new Vector<>();
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    rowDataCopy.add(tableModel.getValueAt(i, j)); // Copy data cell by cell
                }
                rowsToRender.add(rowDataCopy);
            }

            int colIndexToSort = -1;
            switch (sortBy) {
                case "Section": colIndexToSort = 0; break;
                case "Course": colIndexToSort = 1; break;
                case "Professor": colIndexToSort = 2; break;
                case "Room": colIndexToSort = 3; break;
                case "Day": colIndexToSort = 4; break;
                case "Time": colIndexToSort = 5; break;
                // No default needed, colIndexToSort = -1 will be handled
            }

            if (colIndexToSort >= 0) {
                final int finalColIndex = colIndexToSort; // for use in lambda
                rowsToRender.sort((r1, r2) -> {
                    Object o1 = r1.get(finalColIndex);
                    Object o2 = r2.get(finalColIndex);

                    String v1 = (o1 == null) ? "" : o1.toString();
                    String v2 = (o2 == null) ? "" : o2.toString();
                    
                    // Standard lexicographical sort. Time will be sorted alphabetically.
                    return v1.compareToIgnoreCase(v2);
                });

                tableModel.setRowCount(0); // Clear table
                for (Vector<Object> row : rowsToRender) {
                    tableModel.addRow(row); // Add sorted rows
                }
            } else if (!"None".equals(sortBy)) {
                // This case handles if sortBy is an unexpected value (not "None" and not a valid column)
                // As a fallback, reload original data to ensure a predictable state.
                loadAllEntriesToTable.run();
            }
            // If sortBy was "None", it's handled by the first if.
            // If colIndexToSort was < 0 due to an invalid sortBy (that wasn't "None"), the else-if handles it.
        });

        // Style the JLabels
        JLabel filterLabel = new JLabel("Filter by:");
        filterLabel.setFont(CONTENT_FONT);
        filterLabel.setForeground(Color.DARK_GRAY);

        JLabel sortLabel = new JLabel("Sort:");
        sortLabel.setFont(CONTENT_FONT);
        sortLabel.setForeground(Color.DARK_GRAY);

        // Add components to topPanel with controlled spacing
        topPanel.add(filterLabel);
        topPanel.add(Box.createHorizontalStrut(8));
        topPanel.add(filterCategoryCombo);
        topPanel.add(Box.createHorizontalStrut(8));
        topPanel.add(filterValueCombo);
        topPanel.add(Box.createHorizontalStrut(15)); 
        topPanel.add(sortLabel);
        topPanel.add(Box.createHorizontalStrut(8));
        topPanel.add(sortCombo);

        // Apply the unified styling to the JComboBoxes
        styleSortCombo(filterCategoryCombo);
        styleSortCombo(filterValueCombo);
        styleSortCombo(sortCombo);

        // Remove the old, verbose styling for filterCategoryCombo, filterValueCombo, and sortCombo
        // The code from approximately line 1648 to 1725 that sets custom UI and renderers
        // for these specific combos in this panel is no longer needed.
        // The styleSortCombo method now handles this.

        // ... (The old styling code that was here is now removed) ...

        // Add to panel
        panel.add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(table);
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        scrollPane.setBorder(BorderFactory.createTitledBorder("Schedule Entries"));
        panel.add(scrollPane, BorderLayout.CENTER);

        loadAllEntriesToTable.run();

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(BACKGROUND_COLOR);

        JButton saveChangesButton = createActionButton("Save Changes");
        JButton deleteRowButton = createActionButton("Delete Row", true); 
        JButton refreshButton = createActionButton("Refresh");
        JButton deleteAllButton = createActionButton("Delete Schedule", true); 

        // Add buttons in the desired visual order (left-to-right):
        // Save Changes, Refresh, Delete Row, Delete Schedule
        bottomPanel.add(saveChangesButton);
        bottomPanel.add(refreshButton);
        bottomPanel.add(deleteRowButton);
        bottomPanel.add(deleteAllButton);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        saveChangesButton.addActionListener(e -> {
            List<ScheduleEntry> updatedEntries = new ArrayList<>();

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                try {
                    String sectionName = (String) tableModel.getValueAt(i, 0);
                    String courseName = (String) tableModel.getValueAt(i, 1);
                    String professorName = (String) tableModel.getValueAt(i, 2);
                    String roomId = (String) tableModel.getValueAt(i, 3);
                    String day = (String) tableModel.getValueAt(i, 4);
                    String timeStr = (String) tableModel.getValueAt(i, 5);

                    int startHour = Integer.parseInt(timeStr.split(":")[0].trim());
                    int endHour = Integer.parseInt(timeStr.split("-")[1].trim().split(":")[0]);

                    // Lookup corresponding objects
                    var section = backend.getSections().stream().filter(s -> s.name.equals(sectionName)).findFirst()
                            .orElse(null);
                    var course = backend.getCourses().stream().filter(c -> c.name.equals(courseName)).findFirst()
                            .orElse(null);
                    var professor = backend.getProfessors().stream().filter(p -> p.name.equals(professorName))
                            .findFirst().orElse(null);
                    var room = backend.getRooms().stream().filter(r -> r.id.equals(roomId)).findFirst().orElse(null);

                    if (section != null && course != null && professor != null && room != null) {
                        updatedEntries
                                .add(new ScheduleEntry(section, course, professor, room, day, startHour, endHour));
                    }
                } catch (Exception ex) {
                    showError("Failed to parse row " + (i + 1));
                }
            }

            backend.getScheduleEntries().clear();
            backend.getScheduleEntries().addAll(updatedEntries);
            backend.saveData();
            showSuccess("Changes saved successfully!");
        });

        deleteRowButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                tableModel.removeRow(selectedRow);
            } else {
                showError("Please select a row to delete.");
            }
        });

        refreshButton.addActionListener(e -> {
            backend.loadData();
            backend.deleteCourse("PL129");
backend.deleteCourse("PL316");
backend.deleteCourse("PL967");
            backend.saveData();

            loadAllEntriesToTable.run();
            showSuccess("Schedule refreshed from database.");
        });

        deleteAllButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the entire schedule?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                backend.getScheduleEntries().clear();
                tableModel.setRowCount(0);
                backend.saveData();
                showSuccess("Schedule cleared.");
            }
        });

        return panel;
    }

    private void refreshScheduleTable(DefaultTableModel tableModel, List<ScheduleEntry> entries) {
        tableModel.setRowCount(0);
        for (ScheduleEntry entry : entries) {
            tableModel.addRow(new Object[] {
                    entry.section.name,
                    entry.course.name,
                    entry.professor.name,
                    entry.room.id,
                    entry.day,
                    String.format("%02d:00 - %02d:00", entry.startHour, entry.endHour)
            });
        }
    }
    private String[] generateTimeOptions(int startHour, int endHour) {
        java.util.List<String> times = new java.util.ArrayList<>();
        for (int hour = startHour; hour <= endHour; hour++) {
            String label = (hour < 12)
                ? String.format("%d:00 AM", hour)
                : (hour == 12 ? "12:00 PM" : String.format("%d:00 PM", hour - 12));
            times.add(label);
        }
        return times.toArray(new String[0]);
    }

    private JPanel createSmartSchedulePanel() {
        JPanel panel = createStandardContentPanel("Smart Schedule Generation");

        // Create options panel
        JPanel optionsPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        optionsPanel.setBackground(BACKGROUND_COLOR);
        optionsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER_COLOR), "Scheduling Options"),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        // Add scheduling preferences
        JCheckBox avoidConflictsBox = new JCheckBox("Avoid Time Conflicts");
        JCheckBox balanceLoadBox = new JCheckBox("Balance Professor Load");
        JCheckBox optimizeRoomsBox = new JCheckBox("Optimize Room Usage");
        JCheckBox considerBreaksBox = new JCheckBox("Consider Lunch Breaks");
        for (JCheckBox cb : new JCheckBox[] { avoidConflictsBox, balanceLoadBox, optimizeRoomsBox,
                considerBreaksBox }) {
            cb.setIcon(new RoundCheckBoxIcon(16, BACKGROUND_COLOR));
            cb.setSelectedIcon(new RoundCheckBoxIcon(16, PRIMARY_COLOR));
            cb.setFont(CONTENT_FONT);
            cb.setForeground(Color.DARK_GRAY);
            cb.setOpaque(false);
        }

        avoidConflictsBox.setSelected(true);
        avoidConflictsBox.setBackground(BACKGROUND_COLOR);
        balanceLoadBox.setSelected(true);
        balanceLoadBox.setBackground(BACKGROUND_COLOR);
        optimizeRoomsBox.setSelected(true);
        optimizeRoomsBox.setBackground(BACKGROUND_COLOR);
        considerBreaksBox.setSelected(true);
        considerBreaksBox.setBackground(BACKGROUND_COLOR);

        // Time preferences
        String[] startTimes = generateTimeOptions(7, 20); // 7 AM to 8 PM
        String[] endTimes = generateTimeOptions(8, 21);   // 8 AM to 9 PM

        JComboBox<String> startTimeCombo = new JComboBox<>(startTimes);
        JComboBox<String> endTimeCombo = new JComboBox<>(endTimes);

        startTimeCombo.setSelectedIndex(0);
        endTimeCombo.setSelectedIndex(endTimeCombo.getItemCount() - 1);

        optionsPanel.add(new JLabel("Earliest Start Time:"));
        optionsPanel.add(startTimeCombo);
        optionsPanel.add(new JLabel("Latest End Time:"));
        optionsPanel.add(endTimeCombo);
        optionsPanel.add(avoidConflictsBox);
        optionsPanel.add(balanceLoadBox);
        optionsPanel.add(optimizeRoomsBox);
        optionsPanel.add(considerBreaksBox);
        styleSortCombo(startTimeCombo);
        styleSortCombo(endTimeCombo);

        // Create progress panel
        JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
        progressPanel.setBackground(BACKGROUND_COLOR);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready to generate");
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 30)); // Slightly taller
        progressBar.setForeground(PRIMARY_COLOR);
        progressBar.setBackground(SECONDARY_COLOR);
        progressBar.setFont(CONTENT_FONT.deriveFont(Font.BOLD));
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE); // String color
        UIManager.put("ProgressBar.selectionBackground", PRIMARY_COLOR); // String background when progressing
        progressBar.setBorder(BorderFactory.createLineBorder(MENU_BG, 1));

        JTextArea logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13)); // Monospaced, slightly larger
        logArea.setEditable(false);
        logArea.setMargin(new Insets(10, 10, 10, 10));
        logArea.setBackground(Color.WHITE); // White background for log
        logArea.setForeground(Color.DARK_GRAY); // Dark gray text for log

        JScrollPane logScroll = new JScrollPane(logArea);
        styleScrollBar(logScroll.getVerticalScrollBar());
        styleScrollBar(logScroll.getHorizontalScrollBar());
        logScroll.setPreferredSize(new Dimension(600, 200));
        // Styled border for the log scroll pane
        logScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        progressPanel.add(progressBar, BorderLayout.NORTH);
        progressPanel.add(logScroll, BorderLayout.CENTER);

        // Create buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        JButton generateButton = createActionButton("Generate Schedule");
        JButton cancelButton = createActionButton("Cancel");

        final javax.swing.Timer[] animationTimer = {null}; // Holder for the timer

        cancelButton.setEnabled(false);

        // Creating result panel to preview the generated schedule
        DefaultTableModel tableModel = new DefaultTableModel(
                new String[]{"Section", "Course", "Professor", "Room", "Day", "Time"}, 0);
        JTable scheduleTable = createStyledModelTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(scheduleTable);
        styleScrollBar(tableScrollPane.getVerticalScrollBar());
        styleScrollBar(tableScrollPane.getHorizontalScrollBar());
        tableScrollPane.setPreferredSize(new Dimension(600, 200));
        tableScrollPane.setVisible(false); // Initially hidden

        Runnable refreshScheduleTable = () -> {
            tableModel.setRowCount(0);
            // Load fresh data from the database
            backend.loadData();
            backend.deleteCourse("PL129");
backend.deleteCourse("PL316");
backend.deleteCourse("PL967");
            backend.saveData();

            for (ScheduleEntry entry : backend.getScheduleEntries()) {
                tableModel.addRow(new Object[]{
                        entry.section.name,
                        entry.course.name,
                        entry.professor.name,
                        entry.room.id,
                        entry.day,
                        String.format("%02d:00 - %02d:00", entry.startHour, entry.endHour)
                });
            }
        };

        generateButton.addActionListener(e -> {
            String[] options = {"Clear and Generate New", "Add to Existing", "Cancel"};
            int choice = JOptionPane.showOptionDialog(this,
                "Do you want to clear the existing schedule and generate a new one,\n" +
                "or attempt to add new entries to the current schedule?",
                "Smart Schedule Option",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return; // User cancelled or closed the dialog
            }
            boolean clearExisting = (choice == JOptionPane.YES_OPTION); // Corresponds to "Clear and Generate New"

            // Disable generate button and enable cancel button
            generateButton.setEnabled(false);
            cancelButton.setEnabled(true);
            tableScrollPane.setVisible(false);
            progressBar.setValue(0);
            // progressBar.setString("Generating schedule..."); // Initial animation set by timer
            logArea.setText("");

            // Stop any previous timer
            if (animationTimer[0] != null && animationTimer[0].isRunning()) {
                animationTimer[0].stop();
            }

            // Start animation timer
            final String baseText = "Generating schedule";
            final String[] suffixes = { ".", "..", "..." };
            final int[] suffixIndex = {0};
            animationTimer[0] = new javax.swing.Timer(400, ae -> {
                progressBar.setString(baseText + suffixes[suffixIndex[0]]);
                suffixIndex[0] = (suffixIndex[0] + 1) % suffixes.length;
            });
            animationTimer[0].start();

            final int[] countBeforeAddAttemptHolder = {0}; // Use a final array to hold the value
            if (!clearExisting) {
                countBeforeAddAttemptHolder[0] = backend.getScheduleEntries().size();
                logArea.append("Attempting to add to existing schedule. Current entries: " + countBeforeAddAttemptHolder[0] + "\n");
            }

            // Create a background thread for schedule generation
            new Thread(() -> {
                try {
                    logArea.append("Starting smart schedule process...\n");
                    if (clearExisting) {
                        logArea.append("User chose: Clear existing schedule and generate new.\n");
                        backend.getScheduleEntries().clear();
                        backend.saveData(); // Save the cleared state to DB
                        logArea.append("Existing schedule has been cleared.\n");
                    } else {
                        logArea.append("User chose: Add to existing schedule.\n");
                        // Ensure backend data is current if we are adding to it.
                        // backend.loadData(); // Consider if needed, can be intensive.
                    }

                    // Generate schedule entries
                    // Store the count *before* attempting to generate/add more.
                    int countBeforeAddAttempt = 0;
                    if (!clearExisting) { // Only relevant if we are in "add to existing" mode
                        // Ensure we have the most up-to-date list from backend before getting its size
                        // backend.loadData(); // This might be too slow or cause UI flicker if called here.
                                          // Relying on the list already in backend.getScheduleEntries() being reasonably current.
                        countBeforeAddAttempt = backend.getScheduleEntries().size();
                        logArea.append("Attempting to add to existing schedule. Current entries: " + countBeforeAddAttempt + "\n");
                    }

                    String selectedStart = (String) startTimeCombo.getSelectedItem();
String selectedEnd = (String) endTimeCombo.getSelectedItem();
int earliestHour = Integer.parseInt(selectedStart.split(":")[0]);
int latestHour = Integer.parseInt(selectedEnd.split(":")[0]);

if (selectedEnd.contains("PM") && latestHour < 12) latestHour += 12;
if (selectedStart.contains("PM") && earliestHour < 12) earliestHour += 12;

List<ScheduleEntry> generatedThisRunEntries = backend.generateSchedule(
    logArea,
    avoidConflictsBox.isSelected(),
    balanceLoadBox.isSelected(),
    optimizeRoomsBox.isSelected(),
    considerBreaksBox.isSelected(),
    clearExisting,
    earliestHour,
    latestHour
);


                    // Update UI on the EDT
                    SwingUtilities.invokeLater(() -> {
                        try {
                            if (animationTimer[0] != null) animationTimer[0].stop(); // Stop animation

                            // If clearing, ensure the in-memory list is pristine before adding.
                            // This is slightly redundant if the earlier DB clear worked, but defensive.
                            if (clearExisting) {
                                backend.getScheduleEntries().clear(); 
                            }
                            backend.getScheduleEntries().addAll(generatedThisRunEntries);
                            
                            backend.saveData(); // Save the new state (either fresh or appended)
                            backend.loadData(); // Reload to reflect actual DB state
                            
                            int numActuallyProcessedInThisRun = generatedThisRunEntries.size();
                            int totalEntriesReportedByDB = backend.getScheduleEntries().size(); // Count after reload

                            progressBar.setValue(100);
                            logArea.append("Schedule processing complete!\n");

                            if (clearExisting) {
                                progressBar.setString("Complete - New Schedule (" + numActuallyProcessedInThisRun + " entries)");
                                if (numActuallyProcessedInThisRun > 0) {
                                    logArea.append("Generated a new schedule with " + numActuallyProcessedInThisRun + " entries.\n");
                                    showSuccess("Successfully generated a new schedule with " + numActuallyProcessedInThisRun + " entries.\nTotal reported by database after reload: " + totalEntriesReportedByDB);
                                } else {
                                    logArea.append("Cleared existing schedule. 0 new entries were generated.\n");
                                    showInfo("Existing schedule cleared. No new entries were generated.\nTotal reported by database after reload: " + totalEntriesReportedByDB);
                                }
                                logArea.append("The new schedule was intended to have " + numActuallyProcessedInThisRun + " entries. Database now reports: " + totalEntriesReportedByDB + " total entries.\n");
                            } else { // Additive mode
                                if (numActuallyProcessedInThisRun > 0) {
                                    logArea.append("Backend processed " + numActuallyProcessedInThisRun + " potential entries in this run.\n");
                                    if (totalEntriesReportedByDB > countBeforeAddAttemptHolder[0]) {
                                        int trulyAddedCount = totalEntriesReportedByDB - countBeforeAddAttemptHolder[0];
                                        progressBar.setString("Complete - " + trulyAddedCount + " New Entries Added");
                                        logArea.append(trulyAddedCount + " new unique entries were successfully added and saved.\n");
                                        showSuccess(trulyAddedCount + " new unique entries added.\nTotal entries in schedule: " + totalEntriesReportedByDB + ".");
                                    } else {
                                        progressBar.setString("Complete - No Net Change");
                                        logArea.append("Although " + numActuallyProcessedInThisRun + " entries were processed, no new unique entries were added (total remains " + totalEntriesReportedByDB + ").\n");
                                        logArea.append("This may indicate all valid slots are filled or the processed entries were effectively duplicates.\n");
                                        showInfo(numActuallyProcessedInThisRun + " entries were processed, but no new unique entries were added.\nThe schedule total remains " + totalEntriesReportedByDB + ". This might mean the schedule is saturated or at its limit for unique entries.");
                                    }
                                } else { // numActuallyProcessedInThisRun is 0 for additive mode
                                    progressBar.setString("Complete - No New Entries Found");
                                    logArea.append("No new schedule entries were found to process. Total in schedule: " + totalEntriesReportedByDB + ".\n");
                                    showInfo("No new entries found to process. The schedule remains with " + totalEntriesReportedByDB + " entries.");
                                }
                            }
                            logArea.append("Current schedule state (total " + totalEntriesReportedByDB + " entries) saved to database and reloaded.\n");

                            // Enable buttons for further actions
                            generateButton.setEnabled(true);
                            cancelButton.setEnabled(false);
                            
                            // Refresh the schedule management panel
                            currentActiveCard = "schedulemanagement";
                            refreshPanel();
                            
                            // Switch to schedule management panel
                            cardLayout.show(contentPanel, "schedulemanagement");
                            
                        } catch (Exception ex) {
                            logArea.append("Error updating UI: " + ex.getMessage() + "\n");
                            showError("Error updating UI: " + ex.getMessage());
                        }
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Error");
                        logArea.append("Error during schedule generation: " + ex.getMessage() + "\n");
                        generateButton.setEnabled(true);
                        cancelButton.setEnabled(false);
                        if (animationTimer[0] != null) animationTimer[0].stop(); // Stop animation
                        progressBar.setString("Error"); // Error status
                        showError("Error generating schedule: " + ex.getMessage());
                    });
                }
            }).start();
        });

        cancelButton.addActionListener(e -> {
            // Implementation for cancelling schedule generation
            if (animationTimer[0] != null && animationTimer[0].isRunning()) {
                animationTimer[0].stop();
            }
            progressBar.setString("Cancelled");
            generateButton.setEnabled(true);
            cancelButton.setEnabled(false);
            logArea.append("Schedule generation cancelled by user.\n");
        });

        buttonPanel.add(generateButton);
        buttonPanel.add(cancelButton);

        // Add all components to main panel
        panel.add(optionsPanel, BorderLayout.NORTH);
        
        // Create a split pane for log area and table
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(progressPanel);
        splitPane.setBottomComponent(tableScrollPane);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);
        
        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStandardContentPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(CONTENT_TITLE_FONT);
        titleLabel.setForeground(ACCENT_COLOR);
        panel.add(titleLabel, BorderLayout.NORTH);

        return panel;
    }

    private JButton createActionButton(String text) {
        return createActionButton(text, false); // Overload for existing calls, defaults to not negative
    }

    private JButton createActionButton(String text, boolean isNegative) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fill rounded background
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        button.setFont(CONTENT_FONT);
        button.setForeground(Color.WHITE);
        // Set background based on whether it's a negative action button
        button.setBackground(isNegative ? NEGATIVE_ACTION_COLOR : PRIMARY_COLOR);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(130, 35)); // Consider making width more dynamic or a bit larger
        button.setBorder(new RoundedBorder(10));
        button.setContentAreaFilled(false); 
        button.setOpaque(false); 

        // Add hover effect (change background color)
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(isNegative ? NEGATIVE_ACTION_HOVER_COLOR : BUTTON_HOVER);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(isNegative ? NEGATIVE_ACTION_COLOR : PRIMARY_COLOR);
            }
        });

        return button;
    }

    private static class RoundedBorder implements Border {
        private int radius;

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius + 1, this.radius + 1, this.radius + 2, this.radius);
        }

        public boolean isBorderOpaque() {
            return true;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }

    // Apply to your table creation method
    private JTable createStyledTable(Object[][] data, String[] columns) {
        JTable table = new JTable(data, columns);
        table.setFont(CONTENT_FONT);
        table.setRowHeight(34); // Match dropdown height from screenshot
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Header styling to match form title
        JTableHeader header = table.getTableHeader();
        header.setFont(MENU_FONT);
        header.setBackground(PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));

        // Selection styling
        table.setSelectionBackground(new Color(240, 247, 255));
        table.setSelectionForeground(Color.BLACK);

        // Focus styling
        table.setFocusable(false);

        // Add left padding to table cells
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setCellRenderer(createPaddedCellRenderer());
        }

        return table;
    }

    private JTable createStyledModelTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(CONTENT_FONT);
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setFont(MENU_FONT);
        table.getTableHeader().setBackground(PRIMARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(232, 240, 254));
        table.setSelectionForeground(Color.BLACK);

        // Add left padding to table cells
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setCellRenderer(createPaddedCellRenderer());
        }

        return table;
    }

    private TableCellRenderer createPaddedCellRenderer() {
        return new DefaultTableCellRenderer() {
            {
                setOpaque(true);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Use consistent padding that matches the form inputs
                setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 5));

                // Text alignment - left-align text
                setHorizontalAlignment(SwingConstants.LEFT);

                // Apply subtle border styling for distinction between rows
                if (!isSelected) {
                    // Lighter border color matching the form fields
                    Color borderColor = new Color(230, 230, 230);
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
                            BorderFactory.createEmptyBorder(0, 15, 0, 5)));

                    // Alternate row colors for better readability
                    if (row % 2 == 0) {
                        setBackground(Color.WHITE);
                    } else {
                        setBackground(new Color(248, 249, 250));
                    }
                }

                return c;
            }
        };
    }

    private JDialog createFramedDialog(String title, JComponent content) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(700, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // X button works

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(BACKGROUND_COLOR);
        panel.add(content, BorderLayout.CENTER);

        dialog.setContentPane(panel);
        return dialog;
    }

    private void showError(String message) {
        configureDialogTheme();

        JOptionPane optionPane = new JOptionPane(
                message,
                JOptionPane.ERROR_MESSAGE);

        JDialog dialog = optionPane.createDialog(this, "Error");
        dialog.setBackground(BACKGROUND_COLOR);
        dialog.setVisible(true);
    }

    private void showSuccess(String message) {
        configureDialogTheme();

        JOptionPane optionPane = new JOptionPane(
                message,
                JOptionPane.INFORMATION_MESSAGE);

        JDialog dialog = optionPane.createDialog(this, "Success");
        dialog.setBackground(BACKGROUND_COLOR);
        dialog.setVisible(true);
    }

    private void showInfo(String message) {
        configureDialogTheme();

        JOptionPane optionPane = new JOptionPane(
                message,
                JOptionPane.INFORMATION_MESSAGE);

        JDialog dialog = optionPane.createDialog(this, "Information");
        dialog.setBackground(BACKGROUND_COLOR);
        dialog.setVisible(true);
    }

    private void configureDialogTheme() {
        // Set global dialog appearance
        UIManager.put("OptionPane.background", BACKGROUND_COLOR);
        UIManager.put("Panel.background", BACKGROUND_COLOR);
        UIManager.put("OptionPane.messageForeground", ACCENT_COLOR);
        UIManager.put("OptionPane.messageFont", CONTENT_FONT);
        UIManager.put("Button.background", PRIMARY_COLOR);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", CONTENT_FONT);
        UIManager.put("Button.select", BUTTON_HOVER);
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));

    }

    private JPanel createTopMenuBar() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        top.setBackground(MENU_BG);
        for (JButton sideBtn : menuButtons) {
            JButton btn = new JButton(sideBtn.getText());
            btn.setActionCommand(sideBtn.getActionCommand());
            btn.setFont(MENU_FONT);
            btn.setForeground(MENU_FG);
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorder(BorderFactory.createEmptyBorder(4,12,4,12));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                String card = e.getActionCommand();
                setActiveMenu(card);
                cardLayout.show(contentPanel, card);
            });
            top.add(btn);
        }
        return top;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SchedulingProgram());
    }
}

class RoundedPanel extends JPanel {
    private Color backgroundColor;
    private int cornerRadius = 20;
    private boolean drawBorder = false;
    private Color borderColor = Color.GRAY;

    public RoundedPanel(LayoutManager layout, int radius) {
        super(layout);
        cornerRadius = radius;
        setOpaque(false);
    }

    public RoundedPanel(LayoutManager layout, int radius, Color bgColor) {
        super(layout);
        cornerRadius = radius;
        backgroundColor = bgColor;
        setOpaque(false);
    }

    public RoundedPanel(LayoutManager layout, int radius, Color bgColor, boolean border, Color borderColor) {
        super(layout);
        cornerRadius = radius;
        backgroundColor = bgColor;
        this.drawBorder = border;
        this.borderColor = borderColor;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension arcs = new Dimension(cornerRadius, cornerRadius);
        int width = getWidth();
        int height = getHeight();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draws the rounded panel with borders.
        if (backgroundColor != null) {
            graphics.setColor(backgroundColor);
        } else {
            graphics.setColor(getBackground());
        }
        graphics.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height); // paint background
        if (drawBorder) {
            graphics.setColor(borderColor);
            graphics.setStroke(new BasicStroke(1));
            graphics.drawRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height); // paint border
        }
    }
}