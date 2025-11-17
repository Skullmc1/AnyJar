package me.qclid;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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

            File serverJar = new File(config.getServerJar());
            if (!serverJar.exists()) {
                logger.severe(
                    "Server JAR file not found: " + config.getServerJar()
                );
                System.out.println(
                    "Oh no! I couldn't find the server JAR file: " +
                        config.getServerJar()
                );
                System.out.println(
                    "Please make sure the file exists and the name is correct in your server.yml."
                );
                System.out.println("\nPress Enter to exit...");
                new Scanner(System.in).nextLine();
                return;
            }

            ProcessBuilder processBuilder = new ProcessBuilder();

            if (config.isUseOptions()) {
                processBuilder.command(
                    "java",
                    "-Xmx" + config.getRamMax(),
                    "-Xms" + config.getRamMin(),
                    "-jar",
                    config.getServerJar(),
                    "nogui"
                );
            } else {
                processBuilder.command(
                    config.getManualStartupCommand().split(" ")
                );
            }

            logger.info(
                "Starting server with command: " +
                    Arrays.toString(processBuilder.command().toArray())
            );

            processBuilder.directory(new File("."));
            Process process = processBuilder.start();

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
