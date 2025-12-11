package me.qclid;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        setupLogger();
        logger.info("AnyJar started.");

        File configFile = new File("server.yml");
        if (!configFile.exists()) {
            try {
                ConfigManager.createDefaultConfig(configFile);
                logger.info("Created default server.yml.");
                System.out.println("Hello there! It seems you're new here.");
                System.out.println(
                    "I've created a `server.yml` file for you. It's like a magic scroll with instructions."
                );
                System.out.println(
                    "Go ahead and open it, and you'll find some fun options to play with."
                );
                System.out.println(
                    "Once you're done, come back here and run me again!"
                );
                System.out.println("\nPress Enter to exit...");
                new Scanner(System.in).nextLine();
                return;
            } catch (IOException e) {
                logger.log(
                    Level.SEVERE,
                    "Could not create default config: " + e.getMessage(),
                    e
                );
                return;
            }
        }

        try {
            logger.info("Loading config from server.yml.");
            ServerConfig config = ConfigManager.loadConfig(configFile);

            // Validate configuration
            if (!validateConfiguration(config)) {
                System.out.println("\nPress Enter to exit...");
                new Scanner(System.in).nextLine();
                return;
            }

            // Build the command based on configuration
            List<String> command = buildCommand(config);
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            logger.info(
                "Starting server with command: " +
                    Arrays.toString(processBuilder.command().toArray())
            );

            processBuilder.directory(new File("."));
            Process process = processBuilder.start();

            // Only add shutdown hook for Java processes (likely Minecraft servers)
            if (config.isUseOptions() && config.getServerJar().toLowerCase().endsWith(".jar")) {
                Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {
                        logger.info(
                            "AnyJar is shutting down, sending 'stop' command to server."
                        );
                        try (
                            PrintWriter writer = new PrintWriter(
                                process.getOutputStream()
                            )
                        ) {
                            writer.println("stop");
                        }
                    })
                );
            } else {
                Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {
                        logger.info("AnyJar is shutting down.");
                        // For non-Java processes, we'll just let them handle their own shutdown
                    })
                );
            }

            StreamGobbler outputGobbler = new StreamGobbler(
                process.getInputStream(),
                logger::info,
                line -> System.out.println("[AnyJar] " + line)
            );
            StreamGobbler errorGobbler = new StreamGobbler(
                process.getErrorStream(),
                logger::severe,
                line -> System.err.println("[AnyJar] " + line)
            );

            ExecutorService executorService = Executors.newFixedThreadPool(3);
            executorService.submit(outputGobbler);
            executorService.submit(errorGobbler);
            executorService.submit(() -> {
                try (
                    Scanner scanner = new Scanner(System.in);
                    PrintWriter writer = new PrintWriter(
                        process.getOutputStream()
                    )
                ) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        writer.println(line);
                        writer.flush();
                    }
                }
            });

            logger.info("Server process started successfully.");

            try {
                int exitCode = process.waitFor();
                logger.info("Server process exited with code: " + exitCode);
            } catch (InterruptedException e) {
                logger.log(
                    Level.SEVERE,
                    "Server process was interrupted: " + e.getMessage(),
                    e
                );
            } finally {
                executorService.shutdown();
                try {
                    if (
                        !executorService.awaitTermination(5, TimeUnit.SECONDS)
                    ) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                }
            }
        } catch (IOException e) {
            logger.log(
                Level.SEVERE,
                "An error occurred while starting the server: " +
                    e.getMessage(),
                e
            );
        }
        logger.info("AnyJar finished.");
    }

    /**
     * Parses a command string into a list of arguments, handling quoted strings properly.
     * @param command The command string to parse
     * @return List of command arguments
     */
    private static List<String> parseCommand(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';
        
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            
            if ((c == '"' || c == '\'') && (i == 0 || command.charAt(i-1) != '\\')) {
                if (!inQuotes) {
                    // Start of quoted string
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    // End of quoted string
                    inQuotes = false;
                } else {
                    // Different quote inside quoted string
                    currentArg.append(c);
                }
            } else if (c == ' ' && !inQuotes) {
                // End of argument
                if (currentArg.length() > 0) {
                    args.add(currentArg.toString());
                    currentArg = new StringBuilder();
                }
            } else {
                currentArg.append(c);
            }
        }
        
        // Add the last argument
        if (currentArg.length() > 0) {
            args.add(currentArg.toString());
        }
        
        return args;
    }

    /**
     * Determines the appropriate command to run based on file type and configuration.
     * @param config The server configuration
     * @return List of command arguments
     */
    private static List<String> buildCommand(ServerConfig config) {
        List<String> command = new ArrayList<>();
        
        if (config.isUseOptions()) {
            // Use the configured options
            File targetFile = new File(config.getServerJar());
            String fileName = targetFile.getName().toLowerCase();
            
            if (fileName.endsWith(".jar")) {
                // Java JAR file
                command.add("java");
                command.add("-Xmx" + config.getRamMax());
                command.add("-Xms" + config.getRamMin());
                command.add("-jar");
                command.add(config.getServerJar());
                command.add("nogui");
            } else if (fileName.endsWith(".sh")) {
                // Shell script
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    // Windows - use bash if available, otherwise try to run directly
                    command.add("bash");
                    command.add(config.getServerJar());
                } else {
                    // Unix-like system
                    command.add("bash");
                    command.add(config.getServerJar());
                }
            } else if (fileName.endsWith(".bat") || fileName.endsWith(".cmd")) {
                // Windows batch file
                command.add("cmd");
                command.add("/c");
                command.add(config.getServerJar());
            } else if (fileName.endsWith(".exe")) {
                // Windows executable
                command.add(config.getServerJar());
            } else {
                // Unknown file type - try to run directly
                command.add(config.getServerJar());
            }
        } else {
            // Use manual startup command
            command = parseCommand(config.getManualStartupCommand());
        }
        
        return command;
    }

    /**
     * Validates the configuration and target file.
     * @param config The server configuration
     * @return true if configuration is valid, false otherwise
     */
    private static boolean validateConfiguration(ServerConfig config) {
        if (config.isUseOptions()) {
            // Validate that the target file exists
            File targetFile = new File(config.getServerJar());
            if (!targetFile.exists()) {
                logger.severe("Target file not found: " + config.getServerJar());
                System.out.println("Oh no! I couldn't find the target file: " + config.getServerJar());
                System.out.println("Please make sure the file exists and the name is correct in your server.yml.");
                return false;
            }
            
            // Validate that the file is executable (if not on Windows)
            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                if (!targetFile.canExecute()) {
                    logger.warning("Target file is not executable: " + config.getServerJar());
                    System.out.println("Warning: Target file is not executable: " + config.getServerJar());
                    System.out.println("You may need to run: chmod +x " + config.getServerJar());
                }
            }
        } else {
            // Validate manual startup command
            String manualCommand = config.getManualStartupCommand();
            if (manualCommand == null || manualCommand.trim().isEmpty()) {
                logger.severe("Manual startup command is empty");
                System.out.println("Error: Manual startup command cannot be empty when use-options is false.");
                return false;
            }
        }
        
        return true;
    }

    private static void setupLogger() {
        LogManager.getLogManager().reset();
        logger.setLevel(Level.ALL);

        try {
            File logDir = new File("Anyjar/logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd_HH-mm-ss"
            );
            String logFileName =
                "Anyjar-log-" + dateFormat.format(new Date()) + ".txt";
            FileHandler fileHandler = new FileHandler(
                logDir.getPath() + "/" + logFileName,
                true
            );

            fileHandler.setFormatter(
                new Formatter() {
                    @Override
                    public String format(LogRecord record) {
                        return (
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                                new Date(record.getMillis())
                            ) +
                            " " +
                            record.getLevel() +
                            ": " +
                            record.getMessage() +
                            "\n"
                        );
                    }
                }
            );

            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class StreamGobbler implements Runnable {

        private InputStream inputStream;
        private Consumer<String> logConsumer;
        private Consumer<String> consoleConsumer;

        public StreamGobbler(
            InputStream inputStream,
            Consumer<String> logConsumer,
            Consumer<String> consoleConsumer
        ) {
            this.inputStream = inputStream;
            this.logConsumer = logConsumer;
            this.consoleConsumer = consoleConsumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .forEach(line -> {
                    logConsumer.accept(line);
                    consoleConsumer.accept(line);
                });
        }
    }
}
