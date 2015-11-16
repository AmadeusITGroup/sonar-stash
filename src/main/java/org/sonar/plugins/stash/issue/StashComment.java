package org.sonar.plugins.stash.issue;


public class StashComment {

  private final long id;
  private final String message;
  private final String path;
  private final StashUser author;
  private final long version;
  private long line;
  
  public StashComment(long id, String message, String path, Long line, StashUser author, long version) {
    this.id = id;
    this.message = message;
    this.path = path;
    this.author = author;
    this.version = version;
    
    // Stash comment can be null if comment is global to all the file
    if (line == null) {
      this.line = 0;
    } else{
      this.line = line.longValue();  
    }
  }

  public long getId(){
    return id;
  }
  
  public void setLine(long line){
    this.line = line;
  }
  
  public String getMessage() {
    return message;
  }

  public String getPath() {
    return path;
  }

  public long getLine() {
    return line;
  }
  
  public StashUser getAuthor() {
    return author;
  }
  
  public long getVersion() {
    return version;
  }
}
