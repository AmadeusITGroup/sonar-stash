package org.sonar.plugins.stash.issue.collector;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sonar.plugins.stash.StashPlugin;
import org.sonar.plugins.stash.exceptions.StashReportExtractionException;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiff;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;

public final class StashCollector {

   private static final String AUTHOR  = "author";
   private static final String VERSION = "version";

  private StashCollector() {
    // NOTHING TO DO
    // Pure static class
  }

  public static StashCommentReport extractComments(JSONObject jsonComments) throws StashReportExtractionException {
    StashCommentReport result = new StashCommentReport();

    JSONArray jsonValues = (JSONArray)jsonComments.get("values");
    if (jsonValues != null) {

      for (Object obj : jsonValues.toArray()) {
        JSONObject jsonComment = (JSONObject)obj;

        StashComment comment = extractComment(jsonComment);
        result.add(comment);
      }
    }

    return result;
  }
  
  public static StashComment extractComment(JSONObject jsonComment, String path, Long line)
         throws StashReportExtractionException {

    long id = (long)jsonComment.get("id");
    String message = (String)jsonComment.get("text");

    long version = (long)jsonComment.get(VERSION);

    JSONObject jsonAuthor = (JSONObject)jsonComment.get(AUTHOR);
    StashUser stashUser = extractUser(jsonAuthor);

    return new StashComment(id, message, path, line, stashUser, version);
  }
  
  public static StashComment extractComment(JSONObject jsonComment) throws StashReportExtractionException {

    JSONObject jsonAnchor = (JSONObject)jsonComment.get("anchor");
    if (jsonAnchor == null) {
      throw new StashReportExtractionException("JSON Comment does not contain any \"anchor\" tag"
                                               + " to describe comment \"line\" and \"path\"");
    }

    String path = (String)jsonAnchor.get("path");

    // can be null if comment is attached to the global file
    Long line = (Long)jsonAnchor.get("line");

    return extractComment(jsonComment, path, line);
  }
  
  public static StashPullRequest extractPullRequest(String project,
                                                    String repository,
                                                    String pullRequestId,
                                                    JSONObject jsonPullRequest) throws StashReportExtractionException {
    StashPullRequest result = new StashPullRequest(project, repository, pullRequestId);

    long version = (long)jsonPullRequest.get(VERSION);
    result.setVersion(version);

    JSONArray jsonReviewers = (JSONArray)jsonPullRequest.get("reviewers");
    if (jsonReviewers != null) {
      for (Object objReviewer : jsonReviewers.toArray()) {
        JSONObject jsonReviewer = (JSONObject)objReviewer;

        JSONObject jsonUser = (JSONObject)jsonReviewer.get("user");
        if (jsonUser != null) {
          StashUser reviewer = extractUser(jsonUser);
          result.addReviewer(reviewer);
        }
      }
    }

    return result;
  }

  public static StashUser extractUser(JSONObject jsonUser) throws StashReportExtractionException {
    long id      = (long)jsonUser.get("id");
    String name  = (String)jsonUser.get("name");
    String slug  = (String)jsonUser.get("slug");
    String email = (String)jsonUser.get("email");

    return new StashUser(id, name, slug, email);
  }

  public static StashDiffReport extractDiffs(JSONObject jsonObject) throws StashReportExtractionException {

    StashDiffReport result = new StashDiffReport();
    JSONArray jsonDiffs = (JSONArray)jsonObject.get("diffs");

    if (jsonDiffs == null) {
      return null;
    }

    // Let's call this for loop "objdiff_loop"
    for (Object objDiff : jsonDiffs.toArray()) {

      JSONObject jsonDiff = (JSONObject)objDiff;
      // destination path in diff view
      // if status of the file is deleted, destination == null
      JSONObject destinationPath = (JSONObject)jsonDiff.get("destination");

      if (destinationPath == null) {
        continue;  // Let's process the next item in "objdiff_loop"
      }

      String path = (String)destinationPath.get("toString");
      JSONArray jsonHunks = (JSONArray)jsonDiff.get("hunks");

      if (jsonHunks == null) {
        continue;  // Let's process the next item in "objdiff_loop"
      }

      // calling the extracted section to scan the jsonHunks & jsonDiff into usable diffs
      result.add(parseHunksIntoDiffs(path, jsonHunks, jsonDiff));

      // Extract File Comments: this kind of comment will be attached to line 0
      JSONArray jsonLineComments = (JSONArray)jsonDiff.get("fileComments");

      if (jsonLineComments == null) {
        continue;  // Let's process the next item in "objdiff_loop"
      }

      StashDiff initialDiff = new StashDiff(StashPlugin.CONTEXT_ISSUE_TYPE, path, 0, 0);

      // Let's call this for loop "objlinc_loop"
      for (Object objLineComment : jsonLineComments.toArray()) {

        JSONObject jsonLineComment = (JSONObject)objLineComment;

        long lineCommentId = (long)jsonLineComment.get("id");
        String lineCommentMessage = (String)jsonLineComment.get("text");
        long lineCommentVersion = (long)jsonLineComment.get(VERSION);

        JSONObject objAuthor = (JSONObject)jsonLineComment.get(AUTHOR);

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

  private static StashDiffReport parseHunksIntoDiffs(String path, JSONArray jsonHunks, JSONObject jsonDiff)
          throws StashReportExtractionException {

    StashDiffReport result = new StashDiffReport();

    // Let's call this for loop "objhunk_loop"
    for (Object objHunk : jsonHunks.toArray()) {

      JSONObject jsonHunk    = (JSONObject)objHunk;
      JSONArray jsonSegments = (JSONArray)jsonHunk.get("segments");

      if (jsonSegments == null) {
        continue;  // Let's process the next item in "objhunk_loop"
      }

      // Let's call this for loop "objsegm_loop"
      for (Object objSegment : jsonSegments.toArray()) {

        JSONObject jsonSegment = (JSONObject)objSegment;
        // type of the diff in diff view
        // We filter REMOVED type, like useless for SQ analysis
        String type = (String)jsonSegment.get("type");
        //
        JSONArray jsonLines = (JSONArray)jsonSegment.get("lines");

        if (StringUtils.equals(type, StashPlugin.REMOVED_ISSUE_TYPE) || jsonLines == null) {

          continue;  // Let's process the next item in "objsegm_loop"
        }

        // Let's call this for loop "objline_loop"
        for (Object objLine : jsonLines.toArray()) {

          JSONObject jsonLine = (JSONObject)objLine;
          // destination line in diff view
          long source = (long)jsonLine.get("source");
          long destination = (long)jsonLine.get("destination");

          StashDiff diff = new StashDiff(type, path, source, destination);
          // Add comment attached to the current line
          JSONArray jsonCommentIds = (JSONArray)jsonLine.get("commentIds");

          if (jsonCommentIds == null) {
            result.add(diff);
          } else {
            // To keep this method depth under control (squid:S134), we outsourced the comments extraction
            result.add(extractCommentsForDiff(diff, jsonDiff, jsonCommentIds));
          }
        }
      }
    }
    return result;
  }

  private static StashDiff extractCommentsForDiff(StashDiff diff, JSONObject jsonDiff, JSONArray jsonCommentIds)
          throws StashReportExtractionException {

    // Let's call this for loop "objcomm_loop"
    for (Object objCommentId: jsonCommentIds.toArray()) {

      long commentId = (long)objCommentId;
      JSONArray jsonLineComments = (JSONArray)jsonDiff.get("lineComments");

      if (jsonLineComments == null) {
        continue;  // Let's process the next item in "objcomm_loop"
      }

      // Let's call this for loop "objlico_loop"
      for (Object objLineComment : jsonLineComments.toArray()) {

        JSONObject jsonLineComment = (JSONObject)objLineComment;
        long lineCommentId = (long)jsonLineComment.get("id");

        if (lineCommentId != commentId) {
          continue;  // Let's process the next item in "objlico_loop"
        }

        String lineCommentMessage = (String)jsonLineComment.get("text");
        long lineCommentVersion = (long)jsonLineComment.get(VERSION);

        JSONObject objAuthor = (JSONObject)jsonLineComment.get(AUTHOR);
 
        if (objAuthor == null) {
          continue;  // Let's process the next item in "objlico_loop"
        }

        StashUser author = extractUser(objAuthor);

        StashComment comment = new StashComment(lineCommentId, lineCommentMessage, diff.getPath(),
                                                diff.getDestination(), author, lineCommentVersion);
        diff.addComment(comment);

        // get the tasks linked to the current comment
        JSONArray jsonTasks = (JSONArray)jsonLineComment.get("tasks");

        if (jsonTasks == null) {
          continue;  // Let's process the next item in "objlico_loop"
        }

        for (Object objTask : jsonTasks.toArray()) {
          JSONObject jsonTask = (JSONObject)objTask;

          comment.addTask(extractTask(jsonTask.toString()));
        }
      }
    }
    return diff;
  }
  
  public static StashTask extractTask(String jsonBody) throws StashReportExtractionException {
    try {
      JSONObject jsonTask = (JSONObject)new JSONParser().parse(jsonBody);

      long taskId = (long)jsonTask.get("id");
      String taskText = (String)jsonTask.get("text");
      String taskState = (String)jsonTask.get("state");
      
      boolean deletable = true;
      
      JSONObject objPermission = (JSONObject)jsonTask.get("permittedOperations");
      if (objPermission != null) {
        deletable = (boolean)objPermission.get("deletable"); 
      }
      
      return new StashTask(taskId, taskText, taskState, deletable);
        
    } catch (ParseException e) {
      throw new StashReportExtractionException(e);
    }
  }
    
  public static boolean isLastPage(JSONObject jsonObject) throws StashReportExtractionException {
    if (jsonObject.get("isLastPage") != null) {
      return (Boolean)jsonObject.get("isLastPage");
    }
    return true;
  }

  public static long getNextPageStart(JSONObject jsonObject) throws StashReportExtractionException {

    if (jsonObject.get("nextPageStart") != null) {
      return (Long)jsonObject.get("nextPageStart");
    }

    return 0;
  }

}
