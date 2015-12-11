package org.sonar.plugins.stash;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

@Properties({
  @Property(key = StashPlugin.STASH_NOTIFICATION, name = "Stash Notification", defaultValue = "false", description = "Analysis result will be issued in Stash pull request",
    global = false),
  @Property(key = StashPlugin.STASH_PROJECT, name = "Stash Project", description = "Stash project of current pull-request", global = false),
  @Property(key = StashPlugin.STASH_REPOSITORY, name = "Stash Repository", description = "Stash project of current pull-request", global = false),
  @Property(key = StashPlugin.STASH_PULL_REQUEST_ID, name = "Stash Pull-request Id", description = "Stash pull-request Id", global = false)})
public class StashPlugin extends SonarPlugin {

  private static final String DEFAULT_STASH_TIMEOUT_VALUE = "10000";
  private static final String DEFAULT_STASH_THRESHOLD_VALUE = "100";
  private static final boolean DEFAULT_STASH_DISPLAY_ANALYSIS_OVERVIEW = true;
  private static final boolean DEFAULT_STASH_DISPLAY_ANALYSIS_SUMMARY = false;

  private static final String CONFIG_PAGE_SUB_CATEGORY_STASH = "Stash";

  public static final String CONTEXT_ISSUE_TYPE = "CONTEXT";
  public static final String REMOVED_ISSUE_TYPE = "REMOVED";
  public static final String ADDED_ISSUE_TYPE = "ADDED";

  public static final String STASH_NOTIFICATION = "sonar.stash.notification";
  public static final String STASH_PROJECT = "sonar.stash.project";
  public static final String STASH_REPOSITORY = "sonar.stash.repository";
  public static final String STASH_PULL_REQUEST_ID = "sonar.stash.pullrequest.id";
  public static final String STASH_URL = "sonar.stash.url";
  public static final String STASH_LOGIN = "sonar.stash.login";
  public static final String STASH_PASSWORD = "sonar.stash.password";
  public static final String STASH_ISSUE_THRESHOLD = "sonar.stash.issue.threshold";
  public static final String STASH_TIMEOUT = "sonar.stash.timeout";
  public static final String STASH_DISPLAY_ANALYSIS_OVERVIEW = "sonar.stash.analysis.overview.display";
  public static final String STASH_DISPLAY_ANALYSIS_SUMMARY = "sonar.stash.analysis.summary.display";
  public static final String SONARQUBE_URL = "sonar.host.url";

  @Override
  public List getExtensions() {
    return Arrays.asList(
      StashIssueReportingPostJob.class,
      StashPluginConfiguration.class,
      InputFileCache.class,
      InputFileCacheSensor.class,
      StashProjectBuilder.class,
      StashRequestFacade.class,
      PropertyDefinition.builder(STASH_URL)
        .name("Stash Base URL")
        .description("HTTP URL of Stash instance, such as http://yourhost.yourdomain/stash")
        .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
        .onQualifiers(Qualifiers.PROJECT)
        .index(0)
        .build(),
      PropertyDefinition.builder(STASH_LOGIN)
        .name("Stash Base User")
        .description("User who pushes data to Stash instance")
        .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
        .onQualifiers(Qualifiers.PROJECT)
        .index(1)
        .build(),
      PropertyDefinition.builder(STASH_TIMEOUT)
        .name("Stash Issue Timeout")
        .description("Timeout when pushing a new issue to Stash (in ms)")
        .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue(DEFAULT_STASH_TIMEOUT_VALUE)
        .index(2)
        .build(),
      PropertyDefinition.builder(STASH_ISSUE_THRESHOLD)
        .name("Stash Issues Threshold")
        .description("Maximum number of issues to display on pull request")
        .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue(DEFAULT_STASH_THRESHOLD_VALUE)
        .index(3)
        .build(),
      PropertyDefinition.builder(STASH_DISPLAY_ANALYSIS_OVERVIEW)
        .name("Display Analysis Overview")
        .description("Set to true to display the analysis overview on pull requests, set to false otherwise.")
        .type(PropertyType.BOOLEAN)
        .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue(Boolean.toString(DEFAULT_STASH_DISPLAY_ANALYSIS_OVERVIEW))
        .index(4)
        .build(),
      PropertyDefinition.builder(STASH_DISPLAY_ANALYSIS_SUMMARY)
        .name("Display Analysis Summary")
        .description("Set to true to display the analysis summary on pull requests, set to false otherwise.")
        .type(PropertyType.BOOLEAN)
        .subCategory(CONFIG_PAGE_SUB_CATEGORY_STASH)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue(Boolean.toString(DEFAULT_STASH_DISPLAY_ANALYSIS_SUMMARY))
        .index(5)
        .build());
  }

}
