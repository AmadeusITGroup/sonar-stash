package org.sonar.plugins.stash;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.fs.InputFile;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class InputFileCache implements BatchComponent {

  private final Map<String, InputFile> inputFileByKey = new HashMap<>();
  
  public void putInputFile(String componentKey, InputFile inputFile) {
    inputFileByKey.put(componentKey, inputFile);
  }

  @CheckForNull
  public InputFile getInputFile(String componentKey) {
    return inputFileByKey.get(componentKey);
  }

  @Override
  public String toString() {
    return "Stash Plugin InputFile Cache";
  }

}
