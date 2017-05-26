package org.sonar.plugins.stash.fixtures;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class SonarQube {
    protected Path installDir;
    protected Process process;
    protected Properties config = new Properties();
    protected final static String PORT_PROPERTY = "sonar.web.port";
    protected final static String HOST_PROPERTY = "sonar.web.host";

    public SonarQube(Path installDir, int port) {
        this.installDir = installDir;
        config.setProperty(PORT_PROPERTY, String.valueOf(port));
        config.setProperty(HOST_PROPERTY, "127.0.0.1");
    }

    public int getPort() {
        return Integer.parseInt(config.getProperty(PORT_PROPERTY));
    }

    public String getHost() {
        return config.getProperty(HOST_PROPERTY);
    }

    protected File getExecutable() {
        String os = System.getProperty("os.name");
        os = os.toLowerCase();

        String arch = System.getProperty("os.arch");
        if (arch.equals("amd64")) {
            arch = "x86-64";
        }
        String binary;
        if (os.equals("windows")) {
            binary =  "sonar.bat";
        } else {
            binary =  "sonar.sh";
        }

        String os_arch = os + "-" + arch;
        if (os.contains("mac")) {
            os_arch = "macosx-universal-64";
        }
        File exec = installDir.resolve("bin").resolve(os_arch).resolve(binary).toFile();
        if (!exec.exists()) {
            throw new IllegalArgumentException();
        }
        if (!exec.canExecute()) {
            throw new IllegalArgumentException();
        }
        return exec;
    }

    public void setUp() {
        // noop
    }

    public Properties getConfig() {
        return config;
    }

    protected void writeConfig() throws Exception {
        File configFile = installDir.resolve("conf").resolve("sonar.properties").toFile();
        configFile.delete();
        try (OutputStream configStream = new FileOutputStream(configFile)) {
            config.store(configStream, null);
        }
    }

    public void startAsync() throws Exception {
        writeConfig();
        process = new ProcessBuilder(this.getExecutable().toString(), "start")
                .directory(installDir.toFile())
                .inheritIO()
                .start();
        if (process.waitFor() != 0) {
            throw new Exception();
        }
    }

    public void stop() throws Exception {
        new ProcessBuilder(this.getExecutable().toString(), "stop")
                .directory(installDir.toFile())
                .start().waitFor();
    }

    public void installPlugin(File sourceArchive) throws IOException {
        if (!sourceArchive.exists())
            throw new IllegalArgumentException();

        Files.copy(sourceArchive.toPath(),
                installDir.resolve("extensions").resolve("plugins").resolve(sourceArchive.toPath().getFileName()),
                StandardCopyOption.REPLACE_EXISTING);
    }

    public void waitForReady() {
        while (true) {
            System.out.println("Waiting for SonarQube to be available at " + getUrl());
            try {
                HttpURLConnection conn = (HttpURLConnection) getUrl().openConnection();
                conn.connect();
                int code = conn.getResponseCode();
                if (code == 200) {
                    break;
                }
            } catch (IOException e) {
                /* noop */
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                /* noop */
            }
        }
        System.out.println("SonarQube is ready");
    }

    public URL getUrl() {
        try {
            return new URL("http", getHost(), getPort(), "/");
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public boolean createProject(String key, String name) throws IOException {
        URL url = new URL(getUrl(),
                "/api/projects/create?"
                + "key=" + URLEncoder.encode(key, "UTF-8")
                + "&"
                + "name=" + URLEncoder.encode(key, "UTF-8")
        );
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.connect();
        return (conn.getResponseCode() == 200);
    }
}
