package org.sonar.plugins.stash;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import org.sonar.api.rule.Severity;

public class SeverityComparator implements Comparator<String> {
    private List<String> severities = Severity.ALL;

    @Override
    public int compare(String o1, String o2) {
      return Comparator.comparingInt(this::indexOf).compare(o1, o2);
    }

    private int indexOf(String o) {
      int r = severities.indexOf(o);
      if (r == -1) {
        throw new IllegalArgumentException(
            MessageFormat.format("Invalid Severity \"{0}\", expecting one of {1}", o, severities)
        );
      }
      return r;
    }
}
