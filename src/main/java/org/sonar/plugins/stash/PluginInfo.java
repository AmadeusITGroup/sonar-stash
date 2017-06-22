package org.sonar.plugins.stash;

public class PluginInfo {
  private String name;
  private String version;

  public PluginInfo(String name, String version) {
    this.name = name;
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public String getName() {
    return name;
  }
}
