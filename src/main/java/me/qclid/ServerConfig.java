package me.qclid;

public class ServerConfig {

    private String ramMax;
    private String ramMin;
    private String serverJar;
    private boolean useOptions;
    private String manualStartupCommand;

    public String getRamMax() {
        return ramMax;
    }

    public void setRamMax(String ramMax) {
        this.ramMax = ramMax;
    }

    public String getRamMin() {
        return ramMin;
    }

    public void setRamMin(String ramMin) {
        this.ramMin = ramMin;
    }

    public String getServerJar() {
        return serverJar;
    }

    public void setServerJar(String serverJar) {
        this.serverJar = serverJar;
    }

    public boolean isUseOptions() {
        return useOptions;
    }

    public void setUseOptions(boolean useOptions) {
        this.useOptions = useOptions;
    }

    public String getManualStartupCommand() {
        return manualStartupCommand;
    }

    public void setManualStartupCommand(String manualStartupCommand) {
        this.manualStartupCommand = manualStartupCommand;
    }

    @Override
    public String toString() {
        return (
            "ServerConfig{" +
            "ramMax='" +
            ramMax +
            '\'' +
            ", ramMin='" +
            ramMin +
            '\'' +
            ", serverJar='" +
            serverJar +
            '\'' +
            ", useOptions=" +
            useOptions +
            ", manualStartupCommand='" +
            manualStartupCommand +
            '\'' +
            '}'
        );
    }
}
