package org.sonar.plugins.stash.issue;

import java.util.Objects;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rule.RuleKey;

public class IssueMetaInformation {
    private Severity severity;
    private String message;
    private RuleKey rule;

    protected IssueMetaInformation() {}

    public Severity severity() {
        return severity;
    }

    public String message() {
        return message;
    }

    public RuleKey rule() {
        return rule;
    }

    public static IssueMetaInformation from(PostJobIssue issue) {
        IssueMetaInformation imi = new IssueMetaInformation();
        imi.severity = issue.severity();
        imi.message = issue.message();
        imi.rule = issue.ruleKey();
        return imi;
    }

    @Override
    public String toString() {
        return severity() + " " + rule() + " " + message();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
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
