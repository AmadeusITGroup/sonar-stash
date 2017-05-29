package org.sonar.plugins.stash;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.coverage.CoverageProjectStore;
import org.sonar.plugins.stash.coverage.CoverageRule;
import org.sonar.plugins.stash.coverage.CoverageSensor;

import com.google.common.collect.Lists;

@Properties({
    @Property(key = StashPlugin.STASH_NOTIFICATION, name = "Stash Notification", defaultValue = "false", description = "Analysis result will be issued in Stash pull request", global = false),
    @Property(key = StashPlugin.STASH_PROJECT, name = "Stash Project", description = "Stash project of current pull-request", global = false),
    @Property(key = StashPlugin.STASH_REPOSITORY, name = "Stash Repository", description = "Stash project of current pull-request", global = false),
    @Property(key = StashPlugin.STASH_PULL_REQUEST_ID, name = "Stash Pull-request Id", description = "Stash pull-request Id", global = false),
    @Property(key = StashPlugin.STASH_BRANCH_NAME, name = "Stash branch name", description = "Name of the branch to look for pull request", global = false)
})
public class StashPlugin extends SonarPlugin {

  private static final String DEFAULT_STASH_TIMEOUT_VALUE = "10000";
  private static final String DEFAULT_STASH_THRESHOLD_VALUE = "100";
  private static final boolean DEFAULT_STASH_ANALYSIS_OVERVIEW = true;

  private static final String CONFIG_PAGE_SUB_CATEGORY_STASH = "Stash";
  
  public static final String SEVERITY_NONE = "NONE";
  
  // INFO, MINOR, MAJOR, CRITICAL, BLOCKER
  protected static final List<String> SEVERITY_LIST = Severity.ALL;
  
  public static final String CONTEXT_ISSUE_TYPE = "CONTEXT";
  public static final String REMOVED_ISSUE_TYPE = "REMOVED";
  public static final String ADDED_ISSUE_TYPE = "ADDED";
  
  public static final String STASH_NOTIFICATION = "sonar.stash.notification";
  public static final String STASH_PROJECT = "sonar.stash.project";
  public static final String STASH_REPOSITORY = "sonar.stash.repository";
  public static final String STASH_PULL_REQUEST_ID = "sonar.stash.pullrequest.id";
  public static final String STASH_BRANCH_NAME = "sonar.stash.branch";
  public static final String STASH_RESET_COMMENTS = "sonar.stash.comments.reset";
  public static final String STASH_URL = "sonar.stash.url";
  public static final String STASH_LOGIN = "sonar.stash.login";
  public static final String STASH_PASSWORD = "sonar.stash.password";
  public static final String STASH_PASSWORD_ENVIRONMENT_VARIABLE = "sonar.stash.password.variable";
  public static final String STASH_REVIEWER_APPROVAL = "sonar.stash.reviewer.approval";
  public static final String STASH_ISSUE_THRESHOLD = "sonar.stash.issue.threshold";
  public static final String STASH_TIMEOUT = "sonar.stash.timeout";
  public static final String SONARQUBE_URL = "sonar.host.url";
  public static final String STASH_TASK_SEVERITY_THRESHOLD = "sonar.stash.task.issue.severity.threshold";
  public static final String STASH_INCLUDE_ANALYSIS_OVERVIEW = "sonar.stash.include.overview";
  public static final String STASH_REPOSITORY_ROOT = "sonar.stash.repository.root";

  @Override
  public List getExtensions() {
    return Arrays.asList(
        StashIssueReportingPostJob.class,
        StashPluginConfiguration.class,
        InputFileCache.class,
        StashProjectBuilder.class,
        StashRequestFacade.class,
        CoverageRule.class,
        CoverageSensor.class,
        CoverageProjectStore.class,
        InputFileCacheSensor.class,
        PropertyDefinition.builder(STASH_URL)
            .name("Stash base URL")
            .description("HTTP URL of Stash instance, such as http://yourhost.yourdomain/stash")
            .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
            .onQualifiers(Qualifiers.PROJECT).build(),
        PropertyDefinition.builder(STASH_LOGIN)
            .name("Stash base User")
            .description("User to push data on Stash instance")
            .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
            .onQualifiers(Qualifiers.PROJECT).build(),
        PropertyDefinition.builder(STASH_PASSWORD)
                .name("Stash base Password")
                .description("Password for Stash base User " +
                        "(Do NOT use in production, passwords are public for everyone with UNAUTHENTICATED HTTP access to SonarQube")
                .type(PropertyType.PASSWORD)
                .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
                .onQualifiers(Qualifiers.PROJECT).build(),
        PropertyDefinition.builder(STASH_TIMEOUT)
            .name("Stash issue Timeout")
            .description("Timeout when pushing a new issue to Stash (in ms)")
            .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
            .onQualifiers(Qualifiers.PROJECT)
            .defaultValue(DEFAULT_STASH_TIMEOUT_VALUE).build(),
        PropertyDefinition.builder(STASH_REVIEWER_APPROVAL)
            .name("Stash reviewer approval")
            .description("Does SonarQube approve the pull-request if there is no new issues?")
            .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
            .onQualifiers(Qualifiers.PROJECT)
            .type(PropertyType.BOOLEAN)
            .defaultValue("false").build(),
        PropertyDefinition.builder(STASH_ISSUE_THRESHOLD)
            .name("Stash issue Threshold")
            .description("Threshold to limit the number of issues pushed to Stash server")
            .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
            .onQualifiers(Qualifiers.PROJECT)
            .defaultValue(DEFAULT_STASH_THRESHOLD_VALUE).build(),
        PropertyDefinition.builder(STASH_TASK_SEVERITY_THRESHOLD)
            .name("Stash tasks severity threshold")
            .description("Only create tasks for issues with the same or higher severity")
            .type(PropertyType.SINGLE_SELECT_LIST)
            .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
            .onQualifiers(Qualifiers.PROJECT)
            .defaultValue(SEVERITY_NONE)
            .options(Lists.asList(SEVERITY_NONE, SEVERITY_LIST.toArray(new String[] {}))).build(),
        PropertyDefinition.builder(STASH_INCLUDE_ANALYSIS_OVERVIEW)
            .name("Include Analysis Overview Comment")
            .description("Create a comment to  the Pull Request providing a overview of the results")
            .type(PropertyType.BOOLEAN)
            .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
            .onQualifiers(Qualifiers.PROJECT)
            .defaultValue(Boolean.toString(DEFAULT_STASH_ANALYSIS_OVERVIEW)).build()
    );
  }
}

