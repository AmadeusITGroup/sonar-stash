package org.sonar.plugins.stash.issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

public class CoverageIssuesReportTest extends Report {
  
  @Spy
  CoverageIssuesReport myReport;
  
  @Mock
  CoverageIssue coverageIssue1;
  
  @Mock
  CoverageIssue coverageIssue2;
  
  @Before
  public void setUp() {
    coverageIssue1 = mock(CoverageIssue.class);
    when(coverageIssue1.getCoverage()).thenReturn(10.0);
    when(coverageIssue1.getKey()).thenReturn("key1");
    when(coverageIssue1.getSeverity()).thenReturn("INFO");
    
    coverageIssue2 = mock(CoverageIssue.class);
    when(coverageIssue2.getCoverage()).thenReturn(23.0);
    when(coverageIssue2.getKey()).thenReturn("key2");
    when(coverageIssue2.getSeverity()).thenReturn("INFO");
    
    CoverageIssuesReport report = new CoverageIssuesReport();
    report.add(coverageIssue1);
    report.add(coverageIssue2);
    
    myReport = spy(report);
  }
    
  @Test
  public void testGetProjectCoverage() {
    when(coverageIssue1.getLinesToCover()).thenReturn(60.0);
    when(coverageIssue1.getUncoveredLines()).thenReturn(30.0);
    
    when(coverageIssue2.getLinesToCover()).thenReturn(40.0);
    when(coverageIssue2.getUncoveredLines()).thenReturn(20.0);
    
    assertTrue(myReport.getProjectCoverage() == 50);
  }
  
  @Test
  public void testGetProjectCoverageWithNoLinesToCover() {
    when(coverageIssue1.getLinesToCover()).thenReturn(0.0);
    when(coverageIssue1.getUncoveredLines()).thenReturn(30.0);
    
    when(coverageIssue2.getLinesToCover()).thenReturn(0.0);
    when(coverageIssue2.getUncoveredLines()).thenReturn(20.0);
    
    assertTrue(myReport.getProjectCoverage() == 0);
  }
  
  @Test
  public void testGetProjectCoverageWithNoIssues() {
   myReport = new CoverageIssuesReport();
   
   assertTrue(myReport.getProjectCoverage() == 0);
  }
  
  @Test
  public void testGetProjectCoverageWithCodeCoverageNull() {
   when(coverageIssue1.getCoverage()).thenReturn(0.0);
   when(coverageIssue2.getCoverage()).thenReturn(0.0);
   
   assertTrue(myReport.getProjectCoverage() == 0);
  }
  
  @Test
  public void testIsNotEmpty() {
    assertFalse(myReport.isEmpty());
  }
  
  @Test
  public void testIsEmpty() {
    myReport = new CoverageIssuesReport();
    assertTrue(myReport.isEmpty());
  }
  
  @Test
  public void testIsEmptyWithCodeCoverageNull() {
    myReport = new CoverageIssuesReport();
    
    when(coverageIssue1.getCoverage()).thenReturn(0.0);
    myReport.add(coverageIssue1);
    
    assertFalse(myReport.isEmpty());
  }
    
  @Test
  public void testGetLoweredIssuesWithNoLoweredIssues() {
    when(coverageIssue1.isLowered()).thenReturn(false);
    when(coverageIssue2.isLowered()).thenReturn(false);
    
    Collection result = myReport.getLoweredIssues();
    assertEquals(0, result.size());
  }
  
  @Test
  public void testGetLoweredIssuesWithOneLoweredIssue() {
    when(coverageIssue1.isLowered()).thenReturn(true);
    when(coverageIssue2.isLowered()).thenReturn(false);
    
    Collection result = myReport.getLoweredIssues();
    assertEquals(result.size(), 1);
    
    CoverageIssue coverageIssue = (CoverageIssue) CollectionUtils.get(result, 0);
    assertEquals("key1", coverageIssue.getKey());
  }
  
  @Test
  public void testGetLoweredIssues() {
    when(coverageIssue1.isLowered()).thenReturn(true);
    when(coverageIssue2.isLowered()).thenReturn(true);
    
    Collection result = myReport.getLoweredIssues();
    assertEquals(result.size(), 2);
  
    CoverageIssue coverageIssue1 = (CoverageIssue) CollectionUtils.get(result, 0);
    assertEquals("key1", coverageIssue1.getKey());
    
    CoverageIssue coverageIssue2 = (CoverageIssue) CollectionUtils.get(result, 1);
    assertEquals("key2", coverageIssue2.getKey());
  }
  
  @Test
  public void testCountLoweredIssuesWithSeverityOption() {
    when(coverageIssue1.getSeverity()).thenReturn("INFO");
    when(coverageIssue1.isLowered()).thenReturn(true);
    
    when(coverageIssue2.getSeverity()).thenReturn("INFO");
    when(coverageIssue2.isLowered()).thenReturn(true);
    
    assertEquals(2, myReport.countLoweredIssues("INFO"));
  }
  
  @Test
  public void testCountLoweredIssuesWithSeverityOptionWithNoIssue() {
    myReport = new CoverageIssuesReport();
    assertEquals(0, myReport.countLoweredIssues("INFO"));
  }
  
  @Test
  public void testCountLoweredIssuesWithSeverityOptionWithDifferentSeverity() {
    when(coverageIssue1.getSeverity()).thenReturn("INFO");
    when(coverageIssue1.isLowered()).thenReturn(true);
    
    when(coverageIssue2.getSeverity()).thenReturn("MAJOR");
    when(coverageIssue2.isLowered()).thenReturn(true);
    
    assertEquals(1, myReport.countLoweredIssues("INFO"));
  }
 
  @Test
  public void testCountLoweredIssuesWithSeverityOptionWithNoMatchingSeverity() {
    when(coverageIssue1.getSeverity()).thenReturn("MAJOR");
    when(coverageIssue1.isLowered()).thenReturn(true);
    
    when(coverageIssue2.getSeverity()).thenReturn("MAJOR");
    when(coverageIssue2.isLowered()).thenReturn(true);
    
    assertEquals(0, myReport.countLoweredIssues("INFO"));
  }
  
  @Test
  public void testCountLoweredIssuesWithSeverityOptionWithNotLoweredIssue() {
    when(coverageIssue1.getSeverity()).thenReturn("INFO");
    when(coverageIssue1.isLowered()).thenReturn(true);
    
    when(coverageIssue2.getSeverity()).thenReturn("INFO");
    when(coverageIssue2.isLowered()).thenReturn(false);
    
    assertEquals(1, myReport.countLoweredIssues("INFO"));
  }
  
  @Test
  public void testCountLoweredIssues() {
    when(coverageIssue1.isLowered()).thenReturn(true);
    when(coverageIssue2.isLowered()).thenReturn(true);
    
    assertEquals(2, myReport.countLoweredIssues());
  }
  
  @Test
  public void testCountLoweredIssuesWithNoIssue() {
    myReport = new CoverageIssuesReport();
    assertEquals(0, myReport.countLoweredIssues());
  }
  
  @Test
  public void testCountLoweredIssuesWithWithNotLoweredIssue() {
    when(coverageIssue1.isLowered()).thenReturn(true);
    when(coverageIssue2.isLowered()).thenReturn(false);
    
    assertEquals(1, myReport.countLoweredIssues());
  }
  
  @Test
  public void testGetEvolution() {
    CoverageIssuesReport report = new CoverageIssuesReport();
    myReport = spy(report);
    
    when(myReport.getProjectCoverage()).thenReturn(60.0);
    when(myReport.getPreviousProjectCoverage()).thenReturn(80.33333);
    
    assertTrue("Expected coverage: -20.3, Found coverage: " + myReport.getEvolution(), myReport.getEvolution() == -20.3);
  }
  
  @Test
  public void testGetEvolutionWithEmptyReport() {
    myReport = new CoverageIssuesReport();
    assertTrue(myReport.getEvolution() == 0);
  }
}
