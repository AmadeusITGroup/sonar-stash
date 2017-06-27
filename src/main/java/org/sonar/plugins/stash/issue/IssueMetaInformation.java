package org.sonar.plugins.stash.issue;

import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;

import java.util.Objects;

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IssueMetaInformation that = (IssueMetaInformation) o;
        return Objects.equals(severity, that.severity) &&
                Objects.equals(message, that.message) &&
                Objects.equals(rule, that.rule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(severity, message, rule);
    }
}
