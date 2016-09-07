package org.sonar.plugins.stash.issue;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class StashUser {

  private final long id;
  private final String name;
  private final String slug;
  private final String email;
  
  public StashUser(long id, String name, String slug, String email){
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.email = email;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public String getEmail() {
    return email;
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
