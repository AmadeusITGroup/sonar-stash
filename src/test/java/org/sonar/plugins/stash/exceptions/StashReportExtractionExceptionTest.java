package org.sonar.plugins.stash.exceptions;

import org.junit.Test;


public class StashReportExtractionExceptionTest {

  @Test(expected = StashReportExtractionException.class)
  public void testExceptionThrowable() throws StashReportExtractionException {

    Throwable me = new Throwable();

    throw new StashReportExtractionException(me);
  }

  @Test(expected = StashReportExtractionException.class)
  public void testExceptionStringThrowable() throws StashReportExtractionException {

    Throwable me = new Throwable();
    String why = "Just because of UT ;)";

    throw new StashReportExtractionException(why, me);
  }
}