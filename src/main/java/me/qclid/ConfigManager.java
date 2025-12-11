package me.qclid;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class ConfigManager {

    private static final Logger logger = Logger.getLogger(
        ConfigManager.class.getName()
    );

    public static void createDefaultConfig(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(
                "# Welcome to the AnyJar configuration file! Here you can customize how your server starts up.\n"
            );
            writer.write(
                "# It's like a secret control panel for your server. How cool is that?\n\n"
            );

            writer.write(
                "# ram-max: The maximum amount of RAM your server can use. Don't get too greedy!\n# You can't use more than the amout given to the JVM.\n# So you can't use this app to get more resources than you have!\n"
            );
            writer.write("ram-max: 1G\n\n");

            writer.write(
                "# ram-min: The minimum amount of RAM your server will use. It's good to have a baseline.\n"
            );
            writer.write("ram-min: 1G\n\n");

            writer.write(
                "# server-jar: The name of the actual server file you want to run. Make sure it's in the same folder!\n# If it's not in the same folder, you'll need to provide the full path to the file.\n# This can be a .jar, .sh, .bat, or other executable file.\n"
            );
            writer.write("server-jar: actual-server.jar\n\n");

            writer.write(
                "# use-options: Set this to true to use the RAM options above and automatic file type detection.\n# If you just want to change the target file, keep this true and modify server-jar.\n# If you set it to false, you can use your own custom command below for full control.\n"
            );
            writer.write("use-options: true\n\n");

            writer.write(
                "# manual-startup-command: If you're feeling adventurous, you can write your own startup command here. Just make sure to set use-options to false!\n"
            );
            writer.write(
                "# This gives you complete control over how your server or application starts.\n"
            );
            writer.write(
                "# Example: java -Xmx2G -Xms1G -jar my_server.jar nogui\n"
            );
            writer.write(
                "# Example: bash startup.sh\n"
            );
            writer.write(
                "# Example: python server.py\n"
            );
            writer.write(
                "manual-startup-command: java -jar actual-server.jar nogui\n"
            );
        }
    }

    public static ServerConfig loadConfig(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            Representer representer = new Representer();
            representer.getPropertyUtils().setSkipMissingProperties(true);

            PropertyUtils propertyUtils = new PropertyUtils() {
                @Override
                public Property getProperty(
                    Class<? extends Object> type,
                    String name
                ) {
                    if (name.indexOf('-') > -1) {
                        name = toCamelCase(name);
                    }
                    return super.getProperty(type, name);
                }
            };

            Constructor constructor = new Constructor(ServerConfig.class);
            constructor.setPropertyUtils(propertyUtils);

            Yaml yaml = new Yaml(constructor);
            ServerConfig config = yaml.load(reader);

            logger.info("Loaded config: " + config);
            return config;
        }
    }

    private static String toCamelCase(String s) {
        String[] parts = s.split("-");
        StringBuilder camelCaseString = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camelCaseString.append(toProperCase(parts[i]));
        }
        return camelCaseString.toString();
    }

    private static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
