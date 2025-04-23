package dev.timduerr.ipu;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.Font;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dev.timduerr.ipu.Theme.*;

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

        initializeTheme();

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

        JMenu viewMenu = new JMenu("Theme");

        JMenuItem macLight = new JMenuItem("Mac Light");
        macLight.addActionListener(e -> switchToTheme(MAC_LIGHT, true));
        viewMenu.add(macLight);

        JMenuItem macDark = new JMenuItem("Mac Dark");
        macDark.addActionListener(e -> switchToTheme(MAC_DARK, true));
        viewMenu.add(macDark);

        JMenuItem intelliJ = new JMenuItem("IntelliJ");
        intelliJ.addActionListener(e -> switchToTheme(INTELLIJ, true));
        viewMenu.add(intelliJ);

        JMenuItem darcula = new JMenuItem("Darcula");
        darcula.addActionListener(e -> switchToTheme(DRACULA, true));
        viewMenu.add(darcula);

        JMenuItem classicLight = new JMenuItem("Classic Light");
        classicLight.addActionListener(e -> switchToTheme(CLASSIC_LIGHT, true));
        viewMenu.add(classicLight);

        JMenuItem classicDark = new JMenuItem("Classic Dark");
        classicDark.addActionListener(e -> switchToTheme(CLASSIC_DARK, true));
        viewMenu.add(classicDark);

        JMenuItem helpMenu = new JMenu("Help");

        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this, "IntelliJ Plugin Version Modifier\nVersion 1.0\n\nCreated by Tim Dürr", "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(about);

        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

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
        jarList.addListSelectionListener(e -> onJarSelected());

        // Use our custom renderer to show a jar icon next to each entry
        jarList.setCellRenderer(new JarListCellRenderer(32));

        JScrollPane listScroll = new JScrollPane(jarList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Select JAR to Modify"));
        mainPanel.add(listScroll, BorderLayout.CENTER);
    }


    private void configureBottomBar() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomPanel.add(new JLabel("New until-build:"));
        versionSpinner = new JSpinner(new SpinnerNumberModel(getNewestBuildMajor(), 0, 999, 1));
        versionSpinner.setEnabled(false);
        ((JSpinner.DefaultEditor) versionSpinner.getEditor()).getTextField().setColumns(5);
        bottomPanel.add(versionSpinner);

        modifyButton = new JButton("Modify & Save");
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

    private void initializeTheme() {
        String themeString = SettingsManager.getSetting("theme", "macLight");
        Theme theme = Theme.fromName(themeString);

        if (theme == null) {
            theme = MAC_LIGHT;
        }

        switchToTheme(theme, false);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Themes
    private void switchToTheme(Theme theme, boolean update) {
        switch (theme) {
            case MAC_LIGHT -> FlatMacLightLaf.setup();
            case MAC_DARK -> FlatMacDarkLaf.setup();
            case INTELLIJ -> FlatIntelliJLaf.setup();
            case DRACULA -> FlatDarculaLaf.setup();
            case CLASSIC_LIGHT -> FlatLightLaf.setup();
            case CLASSIC_DARK -> FlatDarkLaf.setup();
        }

        SwingUtilities.updateComponentTreeUI(this);

        if (update) {
            SettingsManager.setSetting("theme", theme.getName());
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Utility methods
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

    private int getNewestBuildMajor() {
        String build = getNewestBuild();
        int dotIndex = build.indexOf('.');
        return dotIndex > 0 ? Integer.parseInt(build.substring(0, dotIndex)) : Integer.parseInt(build);
    }

    private String getNewestBuild() {
        final String apiUrl = "https://data.services.jetbrains.com/products?code=IIU";

        try {
            String json = HttpClient
                    .newHttpClient()
                    .send(java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(apiUrl))
                            .build(), java.net.http.HttpResponse.BodyHandlers.ofString())
                    .body();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            return root.get(0)
                    .get("releases")
                    .get(0)
                    .get("build")
                    .asText();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not fetch latest build number: {}", e.getMessage());
            return "";
        }
    }
}
