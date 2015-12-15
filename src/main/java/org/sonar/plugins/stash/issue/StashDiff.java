package org.sonar.plugins.stash.issue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sonar.plugins.stash.StashPlugin;

public class StashDiff {

  private final String type;
  private final String path;
  private final long source;
  private final long destination;
  private final List<StashComment> comments;
  
  public StashDiff(String type, String path, long source, long destination) {
    this.type = type;
    this.path = path;
    this.source = source;
    this.destination = destination;
    this.comments = new ArrayList<>();
  }

  public void addComment(StashComment comment){
    this.comments.add(comment);
  }
  
  public String getPath() {
    return path;
  }

  public long getSource() {
    return source;
  }
  
  public long getDestination() {
    return destination;
  }
  
  public String getType(){
    return type;
  }
  
  public List<StashComment> getComments(){
    return comments;
  }
  
  public boolean isTypeOfContext(){
    return StringUtils.equals(StashPlugin.CONTEXT_ISSUE_TYPE, type);
  }
  
  public boolean containsComment(long commentId){
    boolean result = false;
    for (StashComment comment: comments){
      long cid = comment.getId();
      if (cid == commentId){
        result = true;
        break;
      }
    }
    
    return result;
  }
}
