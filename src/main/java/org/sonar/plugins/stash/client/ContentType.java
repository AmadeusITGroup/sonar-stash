package org.sonar.plugins.stash.client;

import org.apache.commons.lang3.StringUtils;

/*
 * basic implementation of RFC 7231, section 3.1.1.1
 * subset of javax.mail.internet.ContentType
 */
public class ContentType {

  private String primaryType;
  private String subType;

  public ContentType(String primaryType, String subType, Object list) {
    if (list != null) {
      throw new IllegalArgumentException();
    }
    this.primaryType = primaryType;
    this.subType = subType;
  }

  public boolean match(String s) {
    String[] parts = s.split(";", 2);
    // we ignore the parameters, match() does not care and we can't have our own
    String[] types = StringUtils.strip(parts[0]).split("/", 2);
    if (types.length < 2) {
      return false;
    }
    return StringUtils.equalsIgnoreCase(primaryType, types[0])
           && StringUtils.equalsIgnoreCase(subType, types[1]);
  }
}
