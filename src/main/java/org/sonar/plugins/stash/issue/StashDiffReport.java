package org.sonar.plugins.stash.issue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sonar.plugins.stash.StashPlugin;

/**
 * This class is a representation of the Stash Diff view.
 * 
 * Purpose is to check if a SonarQube issue belongs to the Stash diff view before posting.
 * Indeed, Stash Diff view displays only comments which belong to this view.
 *
 */
public class StashDiffReport {

  private List<StashDiff> diffs;

  public StashDiffReport() {
    this.diffs = new ArrayList<>();
  }

  public List<StashDiff> getDiffs() {
    return diffs;
  }
  
  public void add(StashDiff diff) {
    diffs.add(diff);
  }
  
  public void add(StashDiffReport report) {
    for (StashDiff diff: report.getDiffs()){
      diffs.add(diff);  
    }
  }
  
  public String getType(String path, long destination){
    String result = null;
    
    for (StashDiff diff : diffs) {
      // Line 0 never belongs to Stash Diff view.
      // It is a global comment with a type set to CONTEXT.
      if (StringUtils.equals(diff.getPath(), path) && (destination == 0)){
        result = StashPlugin.CONTEXT_ISSUE_TYPE;
        break;
      } else{
        
        if (StringUtils.equals(diff.getPath(), path) && (diff.getDestination() == destination)) {
          result = diff.getType();
          break;
        }
      }
    }

    return result;
  }
  
  /**
   * Depends on the type of the diff.
   * If type == "CONTEXT", return the source line of the diff.
   * If type == "ADDED", return the destination line of the diff.
   */
  public long getLine(String path, long destination){
    long result = 0;
    for (StashDiff diff : diffs) {
      if (StringUtils.equals(diff.getPath(), path) && (diff.getDestination() == destination)) {
        if (diff.isTypeOfContext()){
          result = diff.getSource();
        } else{
          result = diff.getDestination();
        }
        
        break;
      }
    }

    return result;
  }
  
  public StashDiff getDiffByComment(long commentId){
    StashDiff result = null;
    for (StashDiff diff : diffs) {
      if (diff.containsComment(commentId)) {
        result = diff;
        break;
      }
    }

    return result;
  }
  
  /**
   * Get all comments from the Stash differential report.
   */
  public List<StashComment> getComments() {
    List<StashComment> result = new ArrayList<>();
    
    for (StashDiff diff: this.diffs) {
      List<StashComment> comments = diff.getComments();
      
      for (StashComment comment: comments) {
        if (! result.contains(comment)) {
          result.add(comment);
        }
      }
    }
    
    return result;
  }
}
