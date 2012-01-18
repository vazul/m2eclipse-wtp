package org.maven.ide.eclipse.wtp;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Test;

public class WebFragmentConfiguratorTest extends AbstractWTPTestCase {

  @Test
  public void testMECLIPSEWTP143_webFragmentImport() throws Exception {
    IProject project = importProject("projects/MECLIPSEWTP-143/webfrag/pom.xml");
    waitForJobsToComplete();
    assertNoErrors(project);
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(2, facetedProject.getProjectFacets().size());
    assertTrue(facetedProject.hasProjectFacet(WTPProjectsUtil.WEB_FRAGMENT_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));

    assertFalse(project.exists(new Path("/src/main/java/META-INF/")));
    assertFalse(project.exists(new Path("/src/main/resources/META-INF/MANIFEST.MF")));
  }

  @Test
  public void testMECLIPSEWTP143_webFragmentError() throws Exception {
    IProject project = importProject("projects/MECLIPSEWTP-143/webfrag-bad/pom.xml");
    waitForJobsToComplete();
    //Web fragment facet requires Java 1.6
    List<IMarker> markers = findErrorMarkers(project);
    assertEquals(2, markers.size());
    assertHasMarker("One or more constraints have not been satisfied.", markers);
    assertHasMarker("Web Fragment Module requires Java 1.6 or newer.", markers);

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(0, facetedProject.getProjectFacets().size());

    assertFalse(project.exists(new Path("/src/main/java/META-INF/")));
    assertFalse(project.exists(new Path("/src/main/resources/META-INF/MANIFEST.MF")));
  }
  
  
  @Test
  public void testMECLIPSEWTP143_webFragmentAsWARDependency() throws Exception {
    testMECLIPSEWTP143_webFragmentAsDependency("webapp");
  }

  @Test
  public void testMECLIPSEWTP143_webFragmentAsEARDependency() throws Exception {
    testMECLIPSEWTP143_webFragmentAsDependency("ear");
  }

  private void testMECLIPSEWTP143_webFragmentAsDependency(String consumerProjectName) throws Exception {
    IProject[] projects = importProjects("projects/MECLIPSEWTP-143/", 
        new String[]{consumerProjectName+"/pom.xml", "webfrag/pom.xml"}, 
        new ResolverConfiguration());
    waitForJobsToComplete();
    IProject project = projects[0];
    IProject fragment = projects[1];
    
    assertNoErrors(project);
    assertNoErrors(fragment);
    
    IFacetedProject facetedProject = ProjectFacetsManager.create(fragment);
    assertNotNull(facetedProject);
    assertEquals(2, facetedProject.getProjectFacets().size());
    assertTrue(facetedProject.hasProjectFacet(WTPProjectsUtil.WEB_FRAGMENT_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
  }

  @Test
  public void testMECLIPSEWTP143_utilityTurnedWebFragment() throws Exception {
    IProject[] projects = importProjects("projects/MECLIPSEWTP-143/", 
        new String[]{"webapp2/pom.xml", "utility-webfrag/pom.xml"}, 
        new ResolverConfiguration());
    waitForJobsToComplete();
    IProject webapp = projects[0];
    IProject utility = projects[1];
    
    assertNoErrors(webapp);
    assertNoErrors(utility);
    
    IFacetedProject facetedProject = ProjectFacetsManager.create(utility);
    assertNotNull(facetedProject);
    assertEquals(2, facetedProject.getProjectFacets().size());
    assertTrue(facetedProject.hasProjectFacet(WTPProjectsUtil.UTILITY_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
    
    IFile fragment = utility.getFile("web-fragment.xml");
    IProgressMonitor monitor = new NullProgressMonitor();
    fragment.move(new Path("src/main/resources/META-INF/web-fragment.xml"), true, monitor);
    
    updateProject(utility);
    waitForJobsToComplete();
    
    assertNoErrors(utility);
    
    facetedProject = ProjectFacetsManager.create(utility);
    assertNotNull(facetedProject);
    assertEquals(2, facetedProject.getProjectFacets().size());
    assertTrue(facetedProject.hasProjectFacet(WTPProjectsUtil.WEB_FRAGMENT_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
  }

  @Test
  public void testMECLIPSEWTP193_NPEWithExternalResource() throws Exception {
    IProject[] projects = importProjects("projects/MECLIPSEWTP-193/", 
        new String[]{"war/pom.xml", "util/pom.xml", "jar/pom.xml"}, 
        new ResolverConfiguration());
    waitForJobsToComplete();
    IProject jar = projects[1];
    assertNoErrors(jar);
  }
}
