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
  private final List<Long> commentIds;
  
  public StashDiff(String type, String path, long source, long destination) {
    this.type = type;
    this.path = path;
    this.source = source;
    this.destination = destination;
    this.commentIds = new ArrayList<Long>();
  }

  public void addComment(long commentId){
    this.commentIds.add(commentId);
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
  
  public boolean isTypeOfContext(){
    return StringUtils.equals(StashPlugin.CONTEXT_ISSUE_TYPE, type);
  }
  
  public boolean containsComment(long commentId){
    boolean result = false;
    for (long cid: commentIds){
      if (cid == commentId){
        result = true;
        break;
      }
    }
    
    return result;
  }
}
