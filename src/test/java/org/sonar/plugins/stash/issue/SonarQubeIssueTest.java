package org.sonar.plugins.stash.issue;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SonarQubeIssueTest {

  SonarQubeIssue myIssue;
  
  @Before
  public void setUp() {
    myIssue = new SonarQubeIssue("key1", "INFO", "message1", "rule1", "path/to/file", 1);
  }
     
  @Test
  public void testPrintIssueMarkdown() {
    assertEquals("*INFO* - message1 [[rule1](http://sonar/url/" + MarkdownPrinter.CODING_RULES_RULE_KEY + "rule1)]", myIssue.printIssueMarkdown("http://sonar/url"));
  }

}
