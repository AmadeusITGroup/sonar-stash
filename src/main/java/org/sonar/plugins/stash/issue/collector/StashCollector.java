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

  private StashCollector() {
    // NOTHING TO DO
    // Pure static class
  }

  public static StashCommentReport extractComments(String jsonBody) throws StashReportExtractionException {
    StashCommentReport result = new StashCommentReport();

    try {
      JSONObject jsonComments = (JSONObject) new JSONParser().parse(jsonBody);

      JSONArray jsonValues = (JSONArray) jsonComments.get("values");
      if (jsonValues != null) {

        for (Object obj : jsonValues.toArray()) {
          JSONObject jsonComment = (JSONObject) obj;

          StashComment comment = extractComment(jsonComment.toJSONString());
          result.add(comment);
        }
      }
    } catch (ParseException e) {
      throw new StashReportExtractionException(e);
    }
    
    return result;
  }
  
  public static StashComment extractComment(String jsonBody, String path, Long line) throws StashReportExtractionException {
    StashComment result = null;
    
    try {
      JSONObject jsonComment = (JSONObject) new JSONParser().parse(jsonBody);
      
      long id = (long) jsonComment.get("id");
      String message = (String) jsonComment.get("text");
      
      long version = (long) jsonComment.get("version");
      
      JSONObject jsonAuthor = (JSONObject) jsonComment.get("author");
      StashUser stashUser = extractUser(jsonAuthor.toJSONString());
      
      result = new StashComment(id, message, path, line, stashUser, version);

    } catch (ParseException e) {
      throw new StashReportExtractionException(e);
    }
    
    return result;
  }
  
  public static StashComment extractComment(String jsonBody) throws StashReportExtractionException {
    StashComment result = null;
    
    try {
      JSONObject jsonComment = (JSONObject) new JSONParser().parse(jsonBody);
      
      JSONObject jsonAnchor = (JSONObject) jsonComment.get("anchor");
      if (jsonAnchor == null) {
        throw new StashReportExtractionException("JSON Comment does not contain any \"anchor\" tag to describe comment \"line\" and \"path\"");
      }
      
      String path = (String) jsonAnchor.get("path");
        
      // can be null if comment is attached to the global file
      Long line = (Long) jsonAnchor.get("line");

      result = extractComment(jsonBody, path, line);
      
    } catch (ParseException e) {
      throw new StashReportExtractionException(e);
    }
    
    return result;
  }
  
  public static StashPullRequest extractPullRequest(String project, String repository, String pullRequestId, String jsonBody) throws StashReportExtractionException {
    StashPullRequest result = new StashPullRequest(project, repository, pullRequestId);
    
    try {
      JSONObject jsonPullRequest = (JSONObject) new JSONParser().parse(jsonBody);

      long version = (long) jsonPullRequest.get("version");
      result.setVersion(version);
    
      JSONArray jsonReviewers = (JSONArray) jsonPullRequest.get("reviewers");
      if (jsonReviewers != null) {
        for (Object objReviewer : jsonReviewers.toArray()) {
          JSONObject jsonReviewer = (JSONObject) objReviewer;
          
          JSONObject jsonUser = (JSONObject) jsonReviewer.get("user");
          if (jsonUser != null){
            StashUser reviewer = extractUser(jsonUser.toJSONString());
            result.addReviewer(reviewer);
          }
        }
      }
    } catch (ParseException e) {
      throw new StashReportExtractionException(e);
    }
    
    return result;
  }
  
  public static StashUser extractUser(String jsonBody) throws StashReportExtractionException {
    try {
      JSONObject jsonUser = (JSONObject) new JSONParser().parse(jsonBody);

      long id = (long) jsonUser.get("id");
      String name = (String) jsonUser.get("name");
      String slug = (String) jsonUser.get("slug");
      String email = (String) jsonUser.get("email");
              
      return new StashUser(id, name, slug, email);
    
    } catch (ParseException e) {
      throw new StashReportExtractionException(e);
    }
  }
  
  public static StashDiffReport extractDiffs(String jsonBody) throws StashReportExtractionException {
    StashDiffReport result = new StashDiffReport();

    try {
      JSONObject jsonObject = (JSONObject) new JSONParser().parse(jsonBody);

      JSONArray jsonDiffs = (JSONArray) jsonObject.get("diffs");
      if (jsonDiffs != null) {
        for (Object objDiff : jsonDiffs.toArray()) {
          JSONObject jsonDiff = (JSONObject) objDiff;
          
          // destination path in diff view
          // if status of the file is deleted, destination == null
          JSONObject destinationPath = (JSONObject) jsonDiff.get("destination");
          if (destinationPath != null){
            String path = (String) destinationPath.get("toString");
            
            JSONArray jsonHunks = (JSONArray) jsonDiff.get("hunks");
            if (jsonHunks != null) {
              for (Object objHunk : jsonHunks.toArray()) {
                JSONObject jsonHunk = (JSONObject) objHunk;
                
                JSONArray jsonSegments = (JSONArray) jsonHunk.get("segments");
                if (jsonSegments != null) {
                  for (Object objSegment : jsonSegments.toArray()) {
                    JSONObject jsonSegment = (JSONObject) objSegment;
                    
                    // type of the diff in diff view
                    // We filter REMOVED type, like useless for SQ analysis
                    String type = (String) jsonSegment.get("type");
                    if (!StringUtils.equals(type, StashPlugin.REMOVED_ISSUE_TYPE)){
                    
                      JSONArray jsonLines = (JSONArray) jsonSegment.get("lines");
                      if (jsonLines != null) {
                        for (Object objLine : jsonLines.toArray()) {
                          JSONObject jsonLine = (JSONObject) objLine;
                          
                          // destination line in diff view
                          long source = (long) jsonLine.get("source");
                          long destination = (long) jsonLine.get("destination");
                          
                          StashDiff diff = new StashDiff(type, path, source, destination);
                          
                          // Add comment attached to the current line
                          JSONArray jsonCommentIds = (JSONArray) jsonLine.get("commentIds");
                          if (jsonCommentIds != null) {
                            
                            for (Object objCommentId: jsonCommentIds.toArray()) {
                              long commentId = (long) objCommentId; 
                              
                              JSONArray jsonLineComments = (JSONArray) jsonDiff.get("lineComments");
                              if (jsonLineComments != null) {
                                for (Object objLineComment : jsonLineComments.toArray()) {
                                  JSONObject jsonLineComment = (JSONObject) objLineComment;
                                  
                                  long lineCommentId = (long) jsonLineComment.get("id");
                                  if (lineCommentId == commentId) { 
                                  
                                    String lineCommentMessage = (String) jsonLineComment.get("text");
                                    long lineCommentVersion = (long) jsonLineComment.get("version");
                                  
                                    JSONObject objAuthor = (JSONObject) jsonLineComment.get("author");
                                    if (objAuthor != null) {
                                      
                                      StashUser author = extractUser(objAuthor.toJSONString());
                                      
                                      StashComment comment = new StashComment(lineCommentId, lineCommentMessage, path, destination, author, lineCommentVersion);
                                      diff.addComment(comment);
                                  
                                      // get the tasks linked to the current comment
                                      JSONArray jsonTasks = (JSONArray) jsonLineComment.get("tasks");
                                      if (jsonTasks != null) {
                                        for (Object objTask : jsonTasks.toArray()) {
                                          JSONObject jsonTask = (JSONObject) objTask;
                                      
                                          comment.addTask(extractTask(jsonTask.toString()));
                                        }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                          
                          result.add(diff);
                        }
                      }
                    }
                  }
                }
              }
              
              // Extract File Comments: this kind of comment will be attached to line 0
              JSONArray jsonLineComments = (JSONArray) jsonDiff.get("fileComments");
              if (jsonLineComments != null) {
                
                StashDiff initialDiff = new StashDiff(StashPlugin.CONTEXT_ISSUE_TYPE, path, 0, 0);
                          
                for (Object objLineComment : jsonLineComments.toArray()) {
                  JSONObject jsonLineComment = (JSONObject) objLineComment;
                              
                  long lineCommentId = (long) jsonLineComment.get("id");
                  String lineCommentMessage = (String) jsonLineComment.get("text");
                  long lineCommentVersion = (long) jsonLineComment.get("version");
                              
                  JSONObject objAuthor = (JSONObject) jsonLineComment.get("author");
                  if (objAuthor != null) {
                    
                    StashUser author = extractUser(objAuthor.toJSONString());
                                
                    StashComment comment = new StashComment(lineCommentId, lineCommentMessage, path, (long) 0, author, lineCommentVersion);
                    initialDiff.addComment(comment);
                  }
                }
                          
                result.add(initialDiff);
              }
            }
          }
        }
      }
    } catch (ParseException e) {
      throw new StashReportExtractionException(e);
    }
    
    return result;
  }
  
  public static StashTask extractTask(String jsonBody) throws StashReportExtractionException {
    try {
      JSONObject jsonTask = (JSONObject) new JSONParser().parse(jsonBody);

      long taskId = (long) jsonTask.get("id");
      String taskText = (String) jsonTask.get("text");
      String taskState = (String) jsonTask.get("state");
      
      boolean deletable = true;
      
      JSONObject objPermission = (JSONObject) jsonTask.get("permittedOperations");
      if (objPermission != null) {
        deletable = (boolean) objPermission.get("deletable"); 
      }
      
      return new StashTask(taskId, taskText, taskState, deletable);
        
    } catch (ParseException e) {
      throw new StashReportExtractionException(e);
    }
  }
    
  public static boolean isLastPage(String jsonBody) throws StashReportExtractionException {
    boolean result = true;

    try {
      JSONObject jsonObject = (JSONObject) new JSONParser().parse(jsonBody);
      if (jsonObject.get("isLastPage") != null) {
        result = (Boolean) jsonObject.get("isLastPage");
      }

    } catch (ParseException e) {
      throw new StashReportExtractionException(e);
    }

    return result;
  }

  public static long getNextPageStart(String jsonBody) throws StashReportExtractionException {
    long result = 0;

    try {
      JSONObject jsonObject = (JSONObject) new JSONParser().parse(jsonBody);
      if (jsonObject.get("nextPageStart") != null) {
        result = (Long) jsonObject.get("nextPageStart");
      }

    } catch (ParseException e) {
      throw new StashReportExtractionException(e);
    }

    return result;
  }
}
