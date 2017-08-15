package org.sonar.plugins.stash.client;

public class StashCredentials {

  private final String login;
  private final String password;
  private final String userSlug;

  public StashCredentials(String login, String password, String userSlug) {
    this.login = login;
    this.password = password;
    this.userSlug = userSlug;
  }

  public String getLogin() {
    return login;
  }

  public String getPassword() {
    return password;
  }

  public String getUserSlug() {
    return userSlug;
  }

}
