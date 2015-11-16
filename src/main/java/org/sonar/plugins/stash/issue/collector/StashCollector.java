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
          long id = (long) jsonComment.get("id");
          String message = (String) jsonComment.get("text");

          JSONObject jsonAnchor = (JSONObject) jsonComment.get("anchor");
          String path = (String) jsonAnchor.get("path");
          
          // can be null if comment is attached to the global file
          Long line = (Long) jsonAnchor.get("line");
          
          long version = (long) jsonComment.get("version");
          
          JSONObject jsonAuthor = (JSONObject) jsonComment.get("author");
          long authorId = (long) jsonAuthor.get("id");
          String name = (String) jsonAuthor.get("name");
          String slug = (String) jsonAuthor.get("slug");
          String email = (String) jsonAuthor.get("email");
          
          StashUser stashUser = new StashUser(authorId, name, slug, email);
          
          StashComment comment = new StashComment(id, message, path, line, stashUser, version);
          result.add(comment);
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
      String email = (String) jsonUser.get("emailAddress");
              
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
                          
                          JSONArray jsonCommentIds = (JSONArray) jsonLine.get("commentIds");
                          if (jsonCommentIds != null) {
                            for (Object objCommentId: jsonCommentIds.toArray()) {
                              diff.addComment((long) objCommentId);
                            }
                          }
                          
                          result.add(diff);
                        }
                      }
                    }
                  }
                }
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
