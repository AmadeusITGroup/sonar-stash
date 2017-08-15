package org.sonar.plugins.stash.issue;

import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang.builder.HashCodeBuilder.reflectionHashCode;

import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;

public class IssueMetaInformation {
    private String severity;
    private String message;
    private RuleKey rule;

    protected IssueMetaInformation() {}

    public String severity() {
        return severity;
    }

    public String message() {
        return message;
    }

    public RuleKey rule() {
        return rule;
    }

    public static IssueMetaInformation from(Issue issue) {
        IssueMetaInformation imi = new IssueMetaInformation();
        imi.severity = issue.severity();
        imi.message = issue.message();
        imi.rule = issue.ruleKey();
        return imi;
    }

    @Override
    public boolean equals(Object o) {
      return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
      return reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return severity() + " " + rule() + " " + message();
    }
}
