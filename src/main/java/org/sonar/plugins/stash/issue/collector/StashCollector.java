package org.sonar.plugins.stash.issue.collector;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.StashPlugin;
import org.sonar.plugins.stash.StashPlugin.IssueType;
import org.sonar.plugins.stash.exceptions.StashReportExtractionException;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiff;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;

public final class StashCollector {

  private static final String AUTHOR = "author";
  private static final String VERSION = "version";

  private StashCollector() {}

  public static StashCommentReport extractComments(JsonObject jsonComments) throws StashReportExtractionException {
    StashCommentReport result = new StashCommentReport();

    JsonArray jsonValues = (JsonArray)jsonComments.get("values");
    if (jsonValues != null) {

      for (Object obj : jsonValues.toArray()) {
        JsonObject jsonComment = (JsonObject)obj;

        StashComment comment = extractComment(jsonComment);
        result.add(comment);
      }
    }

    return result;
  }

  public static Optional<StashComment> extractCommentFromActivity(JsonObject json) {
    return Optional.ofNullable((JsonObject) json.get("comment")).filter(Objects::nonNull).map(j -> StashCollector.extractComment(j, null, null));
  }

  public static StashComment extractComment(JsonObject jsonComment, String path, Long line) {
    long id = getLong(jsonComment, "id");
    String message = (String)jsonComment.get("text");

    long version = getLong(jsonComment, VERSION);

    JsonObject jsonAuthor = (JsonObject)jsonComment.get(AUTHOR);
    StashUser stashUser = extractUser(jsonAuthor);

    StashComment result = new StashComment(id, message, path, line, stashUser, version);
    // FIXME do this at some central place
    updateCommentTasks(result, (JsonArray) jsonComment.get("tasks"));
    return result;
  }

  public static StashComment extractComment(JsonObject jsonComment) throws StashReportExtractionException {

    JsonObject jsonAnchor = (JsonObject)jsonComment.get("anchor");
    if (jsonAnchor == null) {
      throw new StashReportExtractionException("JSON Comment does not contain any \"anchor\" tag"
                                               + " to describe comment \"line\" and \"path\"");
    }

    String path = (String)jsonAnchor.get("path");

    // can be null if comment is attached to the global file
    Long line = getLong(jsonAnchor, "line");

    return extractComment(jsonComment, path, line);
  }

  public static StashPullRequest extractPullRequest(PullRequestRef pr,
                                                    JsonObject jsonPullRequest) {
    StashPullRequest result = new StashPullRequest(pr);

    long version = getLong(jsonPullRequest, VERSION);
    result.setVersion(version);

    JsonArray jsonReviewers = (JsonArray)jsonPullRequest.get("reviewers");
    if (jsonReviewers != null) {
      for (Object objReviewer : jsonReviewers.toArray()) {
        JsonObject jsonReviewer = (JsonObject)objReviewer;

        JsonObject jsonUser = (JsonObject)jsonReviewer.get("user");
        if (jsonUser != null) {
          StashUser reviewer = extractUser(jsonUser);
          result.addReviewer(reviewer);
        }
      }
    }

    return result;
  }

  public static StashUser extractUser(JsonObject jsonUser) {
    long id = getLong(jsonUser, "id");
    String name = (String)jsonUser.get("name");
    String slug = (String)jsonUser.get("slug");
    String email = (String)jsonUser.get("email");

    return new StashUser(id, name, slug, email);
  }

  public static StashDiffReport extractDiffs(JsonObject jsonObject) {

    StashDiffReport result = new StashDiffReport();
    JsonArray jsonDiffs = (JsonArray)jsonObject.get("diffs");

    if (jsonDiffs == null) {
      return null;
    }

    // Let's call this for loop "objdiff_loop"
    for (Object objDiff : jsonDiffs.toArray()) {

      JsonObject jsonDiff = (JsonObject)objDiff;
      // destination path in diff view
      // if status of the file is deleted, destination == null
      JsonObject destinationPath = (JsonObject)jsonDiff.get("destination");

      if (destinationPath == null) {
        continue;  // Let's process the next item in "objdiff_loop"
      }

      String path = (String)destinationPath.get("toString");
      JsonArray jsonHunks = (JsonArray)jsonDiff.get("hunks");

      if (jsonHunks == null) {
        continue;  // Let's process the next item in "objdiff_loop"
      }

      // calling the extracted section to scan the jsonHunks & jsonDiff into usable diffs
      result.add(parseHunksIntoDiffs(path, jsonHunks, jsonDiff));

      // Extract File Comments: this kind of comment will be attached to line 0
      JsonArray jsonLineComments = (JsonArray)jsonDiff.get("fileComments");

      if (jsonLineComments == null) {
        continue;  // Let's process the next item in "objdiff_loop"
      }

      StashDiff initialDiff = new StashDiff(IssueType.CONTEXT, path, 0, 0);

      // Let's call this for loop "objlinc_loop"
      for (Object objLineComment : jsonLineComments.toArray()) {

        JsonObject jsonLineComment = (JsonObject)objLineComment;

        long lineCommentId = getLong(jsonLineComment, "id");
        String lineCommentMessage = (String)jsonLineComment.get("text");
        long lineCommentVersion = getLong(jsonLineComment, VERSION);

        JsonObject objAuthor = (JsonObject)jsonLineComment.get(AUTHOR);

        if (objAuthor == null) {
          continue;  // Let's process the next item in "objlinc_loop"
        }

        StashUser author = extractUser(objAuthor);

        StashComment comment = new StashComment(lineCommentId, lineCommentMessage, path,
                                                (long)0, author, lineCommentVersion);
        initialDiff.addComment(comment);
      }

      result.add(initialDiff);
    }
    return result;
  }

  private static StashDiffReport parseHunksIntoDiffs(String path, JsonArray jsonHunks, JsonObject jsonDiff) {

    StashDiffReport result = new StashDiffReport();

    // Let's call this for loop "objhunk_loop"
    for (Object objHunk : jsonHunks.toArray()) {

      JsonObject jsonHunk = (JsonObject)objHunk;
      JsonArray jsonSegments = (JsonArray)jsonHunk.get("segments");

      if (jsonSegments == null) {
        continue;  // Let's process the next item in "objhunk_loop"
      }

      // Let's call this for loop "objsegm_loop"
      for (Object objSegment : jsonSegments.toArray()) {

        JsonObject jsonSegment = (JsonObject)objSegment;
        // type of the diff in diff view
        // We filter REMOVED type, like useless for SQ analysis
        IssueType type = IssueType.valueOf((String)jsonSegment.get("type"));
        //
        JsonArray jsonLines = (JsonArray)jsonSegment.get("lines");

        if (type == IssueType.REMOVED || jsonLines == null) {

          continue;  // Let's process the next item in "objsegm_loop"
        }

        // Let's call this for loop "objline_loop"
        for (Object objLine : jsonLines.toArray()) {

          JsonObject jsonLine = (JsonObject)objLine;
          // destination line in diff view
          long source = getLong(jsonLine, "source");
          long destination = getLong(jsonLine,"destination");

          StashDiff diff = new StashDiff(type, path, source, destination);
          // Add comment attached to the current line
          JsonArray jsonCommentIds = (JsonArray)jsonLine.get("commentIds");

          // To keep this method depth under control (squid:S134), we outsourced the comments extraction
          result.add(extractCommentsForDiff(diff, jsonDiff, jsonCommentIds));
        }
      }
    }
    return result;
  }

  private static StashDiff extractCommentsForDiff(StashDiff diff, JsonObject jsonDiff, JsonArray jsonCommentIds) {

    // If there is no comments, we just return the diff as-is
    if (jsonCommentIds == null) {
      return diff;
    }

    // Let's call this for loop "objcomm_loop"
    for (Object objCommentId : jsonCommentIds.toArray()) {

      long commentId = getLong(objCommentId);
      JsonArray jsonLineComments = (JsonArray)jsonDiff.get("lineComments");

      if (jsonLineComments == null) {
        continue;  // Let's process the next item in "objcomm_loop"
      }

      // Let's call this for loop "objlico_loop"
      for (Object objLineComment : jsonLineComments.toArray()) {

        JsonObject jsonLineComment = (JsonObject)objLineComment;

        long lineCommentId = getLong(jsonLineComment, "id");

        if (lineCommentId != commentId) {
          continue;  // Let's process the next item in "objcomm_loop"
        }

        // Sending the JSON for processing into a nice comment
        StashComment comment = buildCommentFromJSON(jsonLineComment, diff);

        // If there is no valid comment in the JSON, we just consider the next element in "objlico_loop"
        if (comment == null) {
          continue;
        }

        // At this point, we can save the comment and add any relevant task to it
        diff.addComment(comment);

        // get the tasks linked to the current comment
        updateCommentTasks(comment, (JsonArray)jsonLineComment.get("tasks"));
      }
    }
    return diff;
  }

  private static StashComment buildCommentFromJSON(JsonObject jsonLineComment, StashDiff diff) {

    long lineCommentId = getLong(jsonLineComment, "id");

    String lineCommentMessage = (String)jsonLineComment.get("text");
    long lineCommentVersion = getLong(jsonLineComment, VERSION);

    JsonObject objAuthor = (JsonObject)jsonLineComment.get(AUTHOR);

    if (objAuthor == null) {
      return null;
    }

    StashUser author = extractUser(objAuthor);

    return new StashComment(lineCommentId, lineCommentMessage, diff.getPath(),
                            diff.getDestination(), author, lineCommentVersion);
  }

  private static void updateCommentTasks(StashComment comment, JsonArray jsonTasks) {

    // No need to fail on NullPointerException but we want to keep caller's complexity down
    if (jsonTasks == null) {
      return;
    }

    for (Object objTask : jsonTasks.toArray()) {
      JsonObject jsonTask = (JsonObject)objTask;

      comment.addTask(extractTask(jsonTask));
    }
  }

  public static StashTask extractTask(JsonObject jsonTask) {
    long taskId = getLong(jsonTask, "id");
    String taskText = (String)jsonTask.get("text");
    String taskState = (String)jsonTask.get("state");

    boolean deletable = true;

    JsonObject objPermission = (JsonObject)jsonTask.get("permittedOperations");
    if (objPermission != null) {
      deletable = (boolean)objPermission.get("deletable");
    }

    return new StashTask(taskId, taskText, taskState, deletable);
  }

  public static boolean isLastPage(JsonObject jsonObject) {
    if (jsonObject.get("isLastPage") != null) {
      return (Boolean)jsonObject.get("isLastPage");
    }
    return true;
  }

  public static long getNextPageStart(JsonObject jsonObject) {

    if (jsonObject.get("nextPageStart") != null) {
      return getLong(jsonObject,"nextPageStart");
    }

    return 0;
  }

  private static final Long getLong(JsonObject o, String name) {
    return getLong(o.get(name));
  }

  private static final Long getLong(Object o) {
    BigDecimal bd = (BigDecimal) o;
    if (bd == null) {
      return null;
    }

    return bd.longValue();
  }

}
