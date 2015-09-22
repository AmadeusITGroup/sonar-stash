package org.sonar.plugins.stash.client;

public class StashCredentials {

  private final String login;
  private final String password;

  public StashCredentials(String login, String password) {
    this.login = login;
    this.password = password;
  }

  public String getLogin() {
    return login;
  }

  public String getPassword() {
    return password;
  }

}
