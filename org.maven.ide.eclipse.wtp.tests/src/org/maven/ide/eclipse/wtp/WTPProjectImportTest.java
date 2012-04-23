/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Test;

@SuppressWarnings("restriction")
public class WTPProjectImportTest extends AbstractWTPTestCase {

  @Test
  public void testProjectImportDefault() throws Exception {

    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject[] projects = importProjects("projects/MNGECLIPSE-20", new String[] {"pom.xml", "type/pom.xml",
        "app/pom.xml", "web/pom.xml", "ejb/pom.xml", "ear/pom.xml",}, configuration);

    waitForJobsToComplete();

    {
      IJavaProject javaProject = JavaCore.create(projects[1]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-type/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-type/src/test/java", rawClasspath[1].getPath().toString());
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[2].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[3].getPath().toString());

      IMarker[] markers = projects[1].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(projects[2]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-type", classpathEntries[0].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("junit-3.8.1.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-app/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-app/src/test/java", rawClasspath[1].getPath().toString());
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[2].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[3].getPath().toString());

      IMarker[] markers = projects[2].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(projects[3]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app", classpathEntries[0].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.asList(rawClasspath).toString(), 4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-web/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-web/src/test/java", rawClasspath[1].getPath().toString());
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[2].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[3].getPath().toString());

      IMarker[] markers = projects[3].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(projects[4]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app", classpathEntries[0].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.asList(rawClasspath).toString(), 5, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/src/main/resources", rawClasspath[1].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/src/test/java", rawClasspath[2].getPath().toString());
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[3].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[4].getPath().toString());
      
      IMarker[] markers = projects[4].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IMarker[] markers = projects[5].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }
  }

  @Test
  public void testProjectImportNoWorkspaceResolution() throws Exception {

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(false);
    configuration.setActiveProfiles("");

    IProject[] projects = importProjects("projects/MNGECLIPSE-20", 
        new String[] {
            "pom.xml", 
            "type/pom.xml",
            "app/pom.xml", 
            "web/pom.xml", 
            "ejb/pom.xml", 
            "ear/pom.xml"}, 
        configuration);

    waitForJobsToComplete();

    projects[0].refreshLocal(IResource.DEPTH_INFINITE, monitor);

    IResource res1 = projects[0].getFolder("ejb/target");
    IResource res2 = projects[4].getFolder("target");

    assertTrue(res1.exists());
    assertTrue(res2.exists());

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    {
      // type
      IJavaProject javaProject = JavaCore.create(projects[1]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-type/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-type/src/test/java", rawClasspath[1].getPath().toString());
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[2].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[3].getPath().toString());

      // IMarker[] markers = projects[1].findMarkers(null, true, IResource.DEPTH_INFINITE);
      List<IMarker> markers = findErrorMarkers(projects[1]);
      assertEquals(toString(markers), 0, markers.size());
    }

    {
      // app
      IJavaProject javaProject = JavaCore.create(projects[2]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("junit-3.8.1.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-app/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-app/src/test/java", rawClasspath[1].getPath().toString());
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[2].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[3].getPath().toString());

      // IMarker[] markers = projects[2].findMarkers(null, true, IResource.DEPTH_INFINITE);
      List<IMarker> markers = findErrorMarkers(projects[2]);
      assertEquals(toString(markers), 3, markers.size());
    }

    {
      // web
      projects[3].build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
      IJavaProject javaProject = JavaCore.create(projects[3]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(2, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[1].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-web/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-web/src/test/java", rawClasspath[1].getPath().toString());
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[2].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[3].getPath().toString());

      // IMarker[] markers = projects[3].findMarkers(null, true, IResource.DEPTH_INFINITE);
      List<IMarker> markers = findErrorMarkers(projects[3]);
      assertEquals(toString(markers), 4, markers.size());
    }

    {
      // ejb
      IJavaProject javaProject = JavaCore.create(projects[4]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(2, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[1].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.asList(rawClasspath).toString(), 5, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/src/main/resources", rawClasspath[1].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/target/classes", rawClasspath[1].getOutputLocation().toString());
      assertEquals("/MNGECLIPSE-20-ejb/src/test/java", rawClasspath[2].getPath().toString());
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[3].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[4].getPath().toString());

      // IMarker[] markers = projects[4].findMarkers(null, true, IResource.DEPTH_INFINITE);
      List<IMarker> markers = findErrorMarkers(projects[4]);
      assertEquals(toString(markers), 4, markers.size());
    }

    {
      // ear
      List<IMarker> markers = findErrorMarkers(projects[5]);
      //Seems like using m2e 1.0.200-SNAPSHOT yields less errors (3 instead of 4)
      //assertEquals(toString(markers), 4, markers.size());
      assertTrue(toString(markers), markers.size() > 2);
    }
  }

  @Test
  public void testMNGECLIPSE1028() throws Exception {

    IProject[] projects = importProjects("projects/import-order-matters", new String[] {"pom.xml", "project1/pom.xml",
        "project2/pom.xml", "project3/pom.xml", "project4/pom.xml", "project5/pom.xml",}, new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(projects.length, 6);
    for (IProject project : projects)
    {
      assertNoErrors(project);    
    }
  }

  @Test
  public void testMNGECLIPSE1028_JavaVersion() throws Exception {

    IProject[] projects = importProjects("projects/import-order-matters2", new String[] {"pom.xml", "project1-ear/pom.xml",
        "project2-war/pom.xml", "project3-jar/pom.xml"}, new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(projects.length, 4);
    for (IProject project : projects)
    {
      assertNoErrors(project);    
    }
    
    IFacetedProject jarUtilityProject = ProjectFacetsManager.create(projects[3]);
    assertNotNull(jarUtilityProject);
    assertTrue(jarUtilityProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertTrue(jarUtilityProject.hasProjectFacet(ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.UTILITY)));
    assertEquals(JavaFacetUtils.JAVA_13, jarUtilityProject.getInstalledVersion(JavaFacetUtils.JAVA_FACET));
  }
}

