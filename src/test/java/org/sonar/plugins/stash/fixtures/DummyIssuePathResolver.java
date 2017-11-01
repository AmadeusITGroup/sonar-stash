package org.sonar.plugins.stash.fixtures;

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.plugins.stash.IssuePathResolver;

public class DummyIssuePathResolver implements IssuePathResolver {
  @Override
  public String getIssuePath(PostJobIssue issue) {
    InputComponent ic = issue.inputComponent();
    if (ic == null) {
      return null;
    }
    return ((InputFile) ic).relativePath();
  }
}
