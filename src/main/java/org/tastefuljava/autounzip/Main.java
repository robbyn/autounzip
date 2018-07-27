package org.tastefuljava.autounzip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static final String HOME = System.getProperty("user.home");
    private static final File CONF_FILE
            = new File(HOME, "autounzip.properties");

    public static void main(String[] args) {
        try {
            initLogging();
            Configurations configs = new Configurations();
            if (!CONF_FILE.isFile() && !CONF_FILE.createNewFile()) {
                throw new IOException("Could not create file: " + CONF_FILE);
            }
            FileBasedConfigurationBuilder<PropertiesConfiguration> builder
                    = configs.propertiesBuilder(CONF_FILE);
            Configuration conf = builder.getConfiguration();
            int st = 0;
            String prop = null;
            boolean propsChanged = false;
            for (String arg: args) {
                switch (st) {
                    case 0:
                        if (arg.startsWith("--")) {
                            prop = arg.substring(2);
                            st = 1;
                        }
                        break;
                    case 1:
                        assert prop != null;
                        conf.setProperty(prop, arg);
                        prop = null;
                        propsChanged = true;
                        st = 0;
                        break;
                }
            }
            // This is especially useful on Mac OS to avoid the default app
            // to be launched and appear in the dock and in the menu bar
            System.setProperty("java.awt.headless", "true");
            Unzipper unzipper = new Unzipper(conf);
            builder.save();
            unzipper.run();
        } catch (ConfigurationException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void initLogging() {
        if (System.getProperty("java.util.logging.config.file") == null) {
            // Use default logging configuration
            try (InputStream inputStream = Main.class.getResourceAsStream(
                    "default-logging.properties")) {
                LogManager.getLogManager().readConfiguration(inputStream);
            } catch (final IOException e) {
                LOG.severe(e.getMessage());
            }
        }
    }
}
