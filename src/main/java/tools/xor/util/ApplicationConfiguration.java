package tools.xor.util;

import java.io.File;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

public class ApplicationConfiguration {
    private final static ApplicationConfiguration INSTANCE = new ApplicationConfiguration();

    private final static String XOR_CONFIG_FILENAME = "xor.properties";

    public static ApplicationConfiguration getInstance() {
        return INSTANCE;
    }

    private PropertiesConfiguration configuration;

    static {
        try {
            File propertiesFile = new File(
                    getInstance().getClass().getClassLoader().getResource(XOR_CONFIG_FILENAME).getFile());
 
             Configurations configs = new Configurations();
             PropertiesConfiguration config = configs.properties(propertiesFile);
              
             getInstance().setConfiguration(config);
        } catch (Exception e) {
            // Create an empty configuration for installations not having the file
            getInstance().setConfiguration(new PropertiesConfiguration());
        }
    }

    public void setConfiguration(PropertiesConfiguration config) {
        this.configuration = config;
    }

    public static PropertiesConfiguration config() {
        return getInstance().configuration;
    }
}
