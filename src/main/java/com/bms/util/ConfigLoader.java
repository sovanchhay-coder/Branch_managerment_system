package com.bms.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigLoader {
    private static final String CONFIG_FILE = "config.properties";
    private final Properties props;

    public ConfigLoader() {
        props = new Properties();
        Path external = Paths.get(CONFIG_FILE);
        try (InputStream in = Files.exists(external)
                ? new FileInputStream(external.toFile())
                : getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Could not load config.properties: " + e.getMessage());
        }
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public String getDbUrl() {
        return props.getProperty("db.url", "jdbc:mysql://localhost:3306/branch_management");
    }

    public String getDbUser() {
        return props.getProperty("db.user", "root");
    }

    public String getDbPassword() {
        return props.getProperty("db.password", "Chhay120607");
    }
}
