package org.sonar.plugins.stash.client;

import static java.util.Objects.requireNonNull;

/*
 * basic implementation of RFC 7231, section 3.1.1.1
 * subset of javax.mail.internet.ContentType
 */
public class ContentType {

  private String primaryType;
  private String subType;

  public static final int CONTENTTYPE_ELEM_NUM = 2;

  public ContentType(String primaryType, String subType, Object list) {
    if (list != null) {
      throw new IllegalArgumentException();
    }
    this.primaryType = requireNonNull(primaryType);
    this.subType = requireNonNull(subType);
  }

  public boolean match(String s) {
    String[] parts = s.split(";", CONTENTTYPE_ELEM_NUM);
    // we ignore the parameters, match() does not care and we can't have our own
    String[] types = parts[0].trim().split("/", CONTENTTYPE_ELEM_NUM);
    if (types.length < CONTENTTYPE_ELEM_NUM) {
      return false;
    }
    
    return primaryType.equalsIgnoreCase(types[0])
           && subType.equalsIgnoreCase(types[1]);
  }

  @Override
  public String toString() {
    return primaryType + "/" + subType;
  }
}
