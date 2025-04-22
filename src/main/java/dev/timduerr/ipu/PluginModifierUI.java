package dev.timduerr.ipu;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.Font;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginModifierUI extends JFrame {

    private static final Logger logger = Logger.getLogger(PluginModifierUI.class.getName());

    private JSpinner versionSpinner;
    private JButton modifyButton;
    private JPanel mainPanel;
    private JList<String> jarList;
    private DefaultListModel<String> listModel;

    private File selectedZip;
    private List<File> discoveredJars;

    public PluginModifierUI() {
        super("IntelliJ Plugin Version Modifier");

        initializeFont();
        initializeIcon();

        switchToLight();

        configureWindow();
        configureMainPanel();
        configureMenuBar();

        configureZipButton();
        configureJarList();
        configureBottomBar();

        setVisible(true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(PluginModifierUI::new);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Configuration methods
    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(500, 300));
        setMinimumSize(new Dimension(500, 300));
        setLocationRelativeTo(null);
    }

    private void configureMainPanel() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        setContentPane(mainPanel);
    }

    private void configureMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu viewMenu = new JMenu("View");

        JMenuItem lightMode = new JMenuItem("Light Mode");
        lightMode.addActionListener(e -> switchToLight());

        JMenuItem darkMode = new JMenuItem("Dark Mode");
        darkMode.addActionListener(e -> switchToDark());

        viewMenu.add(lightMode);
        viewMenu.add(darkMode);
        menuBar.add(viewMenu);

        setJMenuBar(menuBar);
    }

    private void configureZipButton() {
        JButton openZipButton = new JButton("Open Plugin ZIP…");
        openZipButton.addActionListener(e -> openZip());
        mainPanel.add(openZipButton, BorderLayout.NORTH);
    }

    private void configureJarList() {
        listModel = new DefaultListModel<>();
        jarList = new JList<>(listModel);
        jarList.setEnabled(false);
        jarList.addListSelectionListener((ListSelectionEvent e) -> onJarSelected());
        JScrollPane listScroll = new JScrollPane(jarList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Select JAR to Modify"));
        mainPanel.add(listScroll, BorderLayout.CENTER);
    }

    private void configureBottomBar() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomPanel.add(new JLabel("New until-build:"));
        versionSpinner = new JSpinner(new SpinnerNumberModel(243, 0, 999, 1));
        versionSpinner.setEnabled(false);
        ((JSpinner.DefaultEditor) versionSpinner.getEditor()).getTextField().setColumns(5);
        bottomPanel.add(versionSpinner);

        modifyButton = new JButton("Modify & Download");
        modifyButton.setEnabled(false);
        modifyButton.addActionListener(e -> modifyPlugin());
        bottomPanel.add(modifyButton);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Initialization methods
    private void initializeFont() {
        try {
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getResourceAsStream("/Inter.ttf")));
            customFont = customFont.deriveFont(12f);
            UIManager.put("defaultFont", customFont);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not load application font: {}", e.getMessage());
        }
    }

    private void initializeIcon() {
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icon.png")));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Utility methods
    private void switchToLight() {
        FlatLightLaf.setup();
        FlatMacLightLaf.setup();
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void switchToDark() {
        FlatDarkLaf.setup();
        FlatMacDarkLaf.setup();
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void openZip() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("ZIP Archives", "zip"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                discoveredJars = PluginProcessor.discoverJars(file);
                listModel.clear();
                for (File jar : discoveredJars) {
                    listModel.addElement(jar.getName());
                }
                jarList.setEnabled(!discoveredJars.isEmpty());
                selectedZip = file;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid ZIP: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onJarSelected() {
        boolean hasSelection = !jarList.isSelectionEmpty();
        versionSpinner.setEnabled(hasSelection);
        modifyButton.setEnabled(hasSelection);
    }

    private void modifyPlugin() {
        int newVersion = (Integer) versionSpinner.getValue();
        int index = jarList.getSelectedIndex();
        File jarFile = discoveredJars.get(index);
        try {
            File modified = PluginProcessor.modifyPlugin(selectedZip, jarFile, newVersion);
            JOptionPane.showMessageDialog(this, "Modified ZIP saved at:\n" + modified.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
