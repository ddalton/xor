package tools.xor.util;

import org.apache.commons.configuration.PropertiesConfiguration;
//import org.apache.commons.configuration2.PropertiesConfiguration;
//import org.apache.commons.configuration2.builder.fluent.Configurations;

import java.io.File;

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

            getInstance().setConfiguration(new PropertiesConfiguration(propertiesFile));
            /* New PropertiesConfiguration. Old one above is for backwards compatibility.
             * ========================================================================== 
             * Configurations configs = new Configurations();
             * PropertiesConfiguration config = configs.properties(propertiesFile);
             * 
             * getInstance().setConfiguration(config);
             */
        } catch (Exception e) {
            // throw ClassUtil.wrapRun(e);
            // Empty
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
