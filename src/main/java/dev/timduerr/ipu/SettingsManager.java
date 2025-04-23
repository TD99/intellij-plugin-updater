package dev.timduerr.ipu;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SettingsManager {

    private static final Logger logger = Logger.getLogger(SettingsManager.class.getName());

    private static final String SETTINGS_FILE = "settings.json";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Map<String, String> settings = new HashMap<>();

    static {
        load();
    }

    private static void load() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try {
                settings = mapper.readValue(file, new TypeReference<>() {});
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to load settings: " + e.getMessage());
            }
        }
    }

    public static void save() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(SETTINGS_FILE), settings);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save settings: " + e.getMessage());
        }
    }

    public static String getSetting(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }

    public static void setSetting(String key, String value) {
        settings.put(key, value);
        save();
    }
}
