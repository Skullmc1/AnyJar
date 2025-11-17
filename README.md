![title](images/title.png)
#
AnyJar is a simple and flexible wrapper for running any JAR file, designed with Minecraft servers in mind. It allows you to customize the startup command for your server, which is especially useful on hosting services that don't allow you to modify the startup command directly.

## How to Use

1.  Download the latest `server.jar` from the [releases page](https://github.com/Skullmc1/AnyJar/releases).
2.  Place the `server.jar` file in the same directory as your actual server JAR file (e.g., `paper.jar`, `spigot.jar`).
3.  Run the `server.jar` file once. This will generate a `server.yml` file.
4.  Open the `server.yml` file and configure the settings to your liking.
5.  Run the `server.jar` file again to start your server with the custom startup command.

## Configuration

The `server.yml` file allows you to configure the following options:

*   `ram-max`: The maximum amount of RAM to allocate to your server (e.g., `1G`, `2048M`).
*   `ram-min`: The minimum amount of RAM to allocate to your server.
*   `server-jar`: The name of the actual server JAR file you want to run.
*   `use-options`: Set this to `true` to use the `ram-max`, `ram-min`, and `server-jar` options. If you set this to `false`, you can use your own custom startup command.
*   `manual-startup-command`: If `use-options` is set to `false`, you can specify your own custom startup command here.

## Why Use AnyJar?

Many hosting services provide a pre-configured environment that doesn't allow you to modify the startup command for your server. This can be limiting if you want to use custom flags or a different JAR file than the one provided by the host.

AnyJar solves this problem by acting as a wrapper for your server. You can upload the AnyJar `server.jar` to your host and configure it to run your actual server JAR file with any startup command you want. This gives you the flexibility to run any server or use any custom flags you need.

You can also use this to run something else entirely, such as a discord bot or a custom script.
