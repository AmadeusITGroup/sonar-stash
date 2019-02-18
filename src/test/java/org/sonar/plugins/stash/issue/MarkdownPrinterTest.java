package org.sonar.plugins.stash.issue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.stash.DefaultIssue;
import org.sonar.plugins.stash.fixtures.DummyIssuePathResolver;

public class MarkdownPrinterTest {

  PostJobIssue issue;

  List<PostJobIssue> report = new ArrayList<>();
  private File baseDir= new File("./src/test/java");
  private static final String SONAR_URL = "sonarqube/URL";
  private MarkdownPrinter printer;
  private int issueThreshold;

  @BeforeEach
  public void setUp() {
    PostJobIssue issueBlocker = new DefaultIssue().setKey("key1")
        .setSeverity(Severity.BLOCKER)
        .setMessage("messageBlocker")
        .setRuleKey(RuleKey.of("RepoBlocker", "RuleBlocker"))
        .setInputComponent(new DefaultInputFile(new DefaultIndexedFile("foo1", baseDir.toPath(), "scripts/file1.example", "de_DE"), x -> x.hash()))
        .setLine(1);
    PostJobIssue issueCritical = new DefaultIssue().setKey("key2")
        .setSeverity(Severity.CRITICAL)
        .setMessage("messageCritical")
        .setRuleKey(RuleKey.of("RepoCritical", "RuleCritical"))
        .setInputComponent(new DefaultInputFile(new DefaultIndexedFile("foo2", baseDir.toPath(), "scripts/file2.example", "de_DE"), x -> x.hash()))
        .setLine(1);
    PostJobIssue issueMajor = new DefaultIssue().setKey("key3")
        .setSeverity(Severity.MAJOR)
        .setMessage("messageMajor")
        .setRuleKey(RuleKey.of("RepoMajor", "RuleMajor"))
        .setInputComponent(new DefaultInputFile(new DefaultIndexedFile("foo3", baseDir.toPath(), "scripts/file3.example", "de_DE"), x -> x.hash()))
        .setLine(1);
    PostJobIssue issueSameFile = new DefaultIssue().setKey("key3")
        .setSeverity(Severity.MAJOR)
        .setMessage("messageMajor")
        .setRuleKey(RuleKey.of("RepoMajor", "RuleMajor"))
            .setInputComponent(new DefaultInputFile(new DefaultIndexedFile("foo3", baseDir.toPath(), "scripts/tests/file3.example", "de_DE"), x -> x.hash()))
        .setLine(5);
    PostJobIssue issueSameFileHidden = new DefaultIssue().setKey("key3")
        .setSeverity(Severity.MAJOR)
        .setMessage("messageMajor")
        .setRuleKey(RuleKey.of("RepoMajor", "RuleMajor"))
            .setInputComponent(new DefaultInputFile(new DefaultIndexedFile("foo3", baseDir.toPath(), "scripts/file3.example", "de_DE"), x -> x.hash()))
        .setLine(15);

    report.add(issueBlocker);
    report.add(issueCritical);
    report.add(issueMajor);
    report.add(issueSameFile);
    report.add(issueSameFileHidden);

    issue = issueBlocker;

    issueThreshold = 100;

    printer = new MarkdownPrinter(issueThreshold, SONAR_URL, 2, new DummyIssuePathResolver());
  }

  @Test
  public void testPrintIssueMarkdown() {
    assertEquals(
        "*BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)]",
        printer.printIssueMarkdown(report.get(0))
    );
  }

  @Test
  public void testPrintIssueMarkdownWithEmptyMessage() {
    assertEquals(
        "No message",
        printer.printIssueMarkdown(new DefaultIssue()
            .setMessage(null)
            .setSeverity(Severity.MINOR)
            .setRuleKey(RuleKey.of("foo", "bar")))
    );
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdown() {
    assertEquals(
        "| BLOCKER | 1 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.BLOCKER)
    );

    assertEquals(
        "| MAJOR | 3 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.MAJOR)
    );

    assertEquals(
        "| INFO | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.INFO)
    );
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoIssues() {
    Collection<PostJobIssue> report = new ArrayList<>();

    assertEquals("| BLOCKER | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.BLOCKER));
    assertEquals("| CRITICAL | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.CRITICAL));
    assertEquals("| MAJOR | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.MAJOR));
    assertEquals("| MINOR | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.MINOR));
    assertEquals("| INFO | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.INFO));
  }

  @Test
  public void testPrintReportMarkdown() {
    String issueReportMarkdown = printer.printReportMarkdown(report);
    String reportString = "## SonarQube analysis Overview\n"
        + "| Total New Issues | 5 |\n"
        + "|-----------------|------|\n"
        + "| BLOCKER | 1 |\n"
        + "| CRITICAL | 1 |\n"
        + "| MAJOR | 3 |\n"
        + "| MINOR | 0 |\n"
        + "| INFO | 0 |\n\n\n"
        + "| Issues list |\n"
        + "|-------------|\n"
        + "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n"
        + "| &nbsp;&nbsp; *Files: scripts/file1.example:1* |\n"
        + "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n"
        + "| &nbsp;&nbsp; *Files: scripts/file2.example:1* |\n"
        + "| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |\n"
        + "| &nbsp;&nbsp; *Files: scripts/file3.example:1, scripts/file3.example:15, ...* |\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintReportMarkdownWithIssueLimitation() {
    printer = new MarkdownPrinter(3, SONAR_URL, 0, new DummyIssuePathResolver());
    String issueReportMarkdown = printer.printReportMarkdown(report);
    String reportString = "## SonarQube analysis Overview\n"
        + "### Too many issues detected (5/3): Issues cannot be displayed in Diff view.\n\n"
        + "| Total New Issues | 5 |\n"
        + "|-----------------|------|\n"
        + "| BLOCKER | 1 |\n"
        + "| CRITICAL | 1 |\n"
        + "| MAJOR | 3 |\n"
        + "| MINOR | 0 |\n"
        + "| INFO | 0 |\n\n\n"
        + "| Issues list |\n"
        + "|-------------|\n"
        + "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n"
        + "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n"
        + "| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintReportMarkdownWithFileWideIssues() {
    PostJobIssue issueWithoutLine = new DefaultIssue().setKey("key36")
        .setSeverity(Severity.CRITICAL)
        .setMessage("messageCritical")
        .setRuleKey(RuleKey.of("RepoCritical", "RuleCritical"))
        .setInputComponent(new DefaultInputFile(new DefaultIndexedFile("foo2", baseDir.toPath(), "scripts/file2.example", "de_DE"), x -> x.hash()))
        .setLine(null);
    report.add(issueWithoutLine);
    printer = new MarkdownPrinter(100, SONAR_URL, 100, new DummyIssuePathResolver());
    String issueReportMarkdown = printer.printReportMarkdown(report);
    String reportString = "## SonarQube analysis Overview\n"
        + "| Total New Issues | 6 |\n"
        + "|-----------------|------|\n"
        + "| BLOCKER | 1 |\n"
        + "| CRITICAL | 2 |\n"
        + "| MAJOR | 3 |\n"
        + "| MINOR | 0 |\n"
        + "| INFO | 0 |\n\n\n"
        + "| Issues list |\n"
        + "|-------------|\n"
        + "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n"
        + "| &nbsp;&nbsp; *Files: scripts/file1.example:1* |\n"
        + "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n"
        + "| &nbsp;&nbsp; *Files: scripts/file2.example, scripts/file2.example:1* |\n"
        + "| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |\n"
        + "| &nbsp;&nbsp; *Files: scripts/file3.example:1, scripts/file3.example:15, scripts/tests/file3.example:5* |\n";

    assertEquals(reportString, issueReportMarkdown);

  }

  @Test
  public void testPrintEmptyReportMarkdown() {
    report = new ArrayList<>();

    String issueReportMarkdown = printer.printReportMarkdown(report);
    String reportString = "## SonarQube analysis Overview\n"
        + "### No new issues detected!\n\n";

    assertEquals(reportString, issueReportMarkdown);
  }
}
