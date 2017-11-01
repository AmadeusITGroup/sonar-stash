package org.sonar.plugins.stash;

import org.sonar.api.batch.postjob.issue.PostJobIssue;

public final class CoverageCompat {
  private static final String REPO = "coverageEvolution";

  private CoverageCompat() {}

  public static boolean isCoverageEvolution(PostJobIssue issue) {
    String r = issue.ruleKey().repository();
    if (r == null) {
      return false;
    }
    return r.startsWith(REPO + "-");
  }

  public static String coverageEvolutionRepository(String language) {
    return REPO + "-" + language;
  }
}
