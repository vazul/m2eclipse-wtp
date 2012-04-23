/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.wtp.preferences.IMavenWtpPreferences;

@SuppressWarnings("restriction")
public abstract class AbstractWTPTestCase extends AbstractMavenProjectTestCase {

  protected static final IProjectFacetVersion DEFAULT_WEB_VERSION = WebFacetUtils.WEB_FACET.getVersion("2.5");
  protected static final IProjectFacet EJB_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.EJB); 
  protected static final IProjectFacet UTILITY_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.UTILITY);
  protected static final IProjectFacetVersion UTILITY_10 = UTILITY_FACET.getVersion("1.0");
  protected static final IProjectFacet EAR_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.ENTERPRISE_APPLICATION);
  protected static final IProjectFacetVersion DEFAULT_EAR_FACET = IJ2EEFacetConstants.ENTERPRISE_APPLICATION_13;

  protected static final String MAVEN_CLASSPATH_CONTAINER = "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER";
  protected static final String JRE_CONTAINER_J2SE_1_5 = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/J2SE-1.5";


  protected static IClasspathContainer getWebLibClasspathContainer(IJavaProject project) throws JavaModelException {
    IClasspathEntry[] entries = project.getRawClasspath();
    for(int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && "org.eclipse.jst.j2ee.internal.web.container".equals(entry.getPath().segment(0))) {
        return JavaCore.getClasspathContainer(entry.getPath(), project);
      }
    }
    return null;
  }

  private static boolean hasExtraAttribute(IClasspathEntry entry, String expectedAttribute) {
    for (IClasspathAttribute cpa : entry.getExtraAttributes()) {
      if (expectedAttribute.equals(cpa.getName())){
        return true;
      }
    }
    return false;
  }

  protected String toString(IVirtualReference[] references) {
    StringBuilder sb = new StringBuilder("[");
    
    String sep = "";
    for(IVirtualReference reference : references) {
      IVirtualComponent component = reference.getReferencedComponent();
      sb.append(sep).append(reference.getRuntimePath() + " - ");
      sb.append(component.getName());
      sb.append(" " + component.getMetaProperties());
      sep = ", ";
    }
    
    return sb.append(']').toString();
  }

  protected String toString(IFile[] files) {
    StringBuilder sb = new StringBuilder("[");
    
    String sep = "";
    for(IFile file : files) {
      sb.append(sep).append(file.getFullPath());
      sep = ", ";
    }
    
    return sb.append(']').toString();
  }

  protected void assertHasMarker(String expectedMessage, List<IMarker> markers) throws CoreException {
    Pattern p = Pattern.compile(expectedMessage);
    for (IMarker marker : markers) {
      String markerMsg = marker.getAttribute(IMarker.MESSAGE).toString(); 
      if (p.matcher(markerMsg).find()) {
        return ;
      }
    }
    fail("[" + expectedMessage + "] is not a marker. Existing markers are :"+toString(markers));
  }

  protected void assertMissingMarker(String expectedMessage, List<IMarker> markers) throws CoreException {
    Pattern p = Pattern.compile(expectedMessage);
    for (IMarker marker : markers) {
      String markerMsg = marker.getAttribute(IMarker.MESSAGE).toString(); 
      if (p.matcher(markerMsg).find()) {
        fail("[" + expectedMessage + "] was found but should be missing. Existing markers are :"+toString(markers)) ;
      }
    }
  }
  
  protected void assertNotDeployable(IClasspathEntry entry) {
    assertDeployable(entry, false);
  }

  protected void assertDeployable(IClasspathEntry entry, boolean expectedDeploymentStatus) {
    //Useless : IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY doesn't seem to be used in WTP 3.2.0. Has it ever worked???
    //assertEquals(entry.toString() + " " + IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY, expectedDeploymentStatus,      hasExtraAttribute(entry, IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY));
    assertEquals(entry.toString() + " " + IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY, !expectedDeploymentStatus, hasExtraAttribute(entry, IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY));
  }

  protected static IClasspathEntry[] getClassPathEntries(IProject project) throws Exception {
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    return container.getClasspathEntries();
  }

  protected static IResource[] getUnderlyingResources(IProject project) {
    IVirtualComponent component = ComponentCore.createComponent(project);
    IVirtualFolder root = component.getRootFolder();
    IResource[] underlyingResources = root.getUnderlyingResources();
    return underlyingResources;
  }

  protected static String getAsString(IFile file) throws IOException, CoreException {
    assert file != null;
    assert file.isAccessible();
    InputStream ins = null;
    String content = null;
    try {
      ins = file.getContents();
      content = IOUtil.toString(ins, 1024);
    } finally {
      IOUtil.close(ins);   
    }
    return content;
  }

  public AbstractWTPTestCase() {
    super();
  }

  /**
   * Replace the project pom.xml with a new one, triggers new build
   * @param project
   * @param newPomName
   * @throws Exception
   */
  protected void updateProject(IProject project, String newPomName) throws Exception {
    updateProject(project, newPomName, -1);
  }

  /**
   * Replace the project pom.xml with a new one, triggers new build, wait for waitTime milliseconds.
   * @param project
   * @param newPomName
   * @param waitTime
   * @throws Exception
   */
  protected void updateProject(IProject project, String newPomName, int waitTime) throws Exception {    
    
    if (newPomName != null) {
      copyContent(project, newPomName, "pom.xml");
    }
    
    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    ResolverConfiguration configuration = new ResolverConfiguration();
    configurationManager.enableMavenNature(project, configuration, monitor);
    configurationManager.updateProjectConfiguration(project, monitor);
    
    waitForJobsToComplete();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    if (waitTime > 0 ) {
      Thread.sleep(waitTime);
    }
    waitForJobsToComplete();
  }

  protected void updateProject(IProject project) throws Exception {   
    updateProject(project, null, -1);
  }
  
  protected void assertContains(String findMe, String holder) {
    assertTrue("'" +findMe + "' is missing from : \n" + holder, holder.contains(findMe));
  }
  
  protected void assertNotContains(String findMe, String holder) {
    assertFalse("'" +findMe + "' was found in : \n" + holder, holder.contains(findMe));
  }
  

  protected void useBuildDirforGeneratingFiles(IProject project, boolean b) {
    IMavenWtpPreferences preferences = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().getPreferences(project);
    preferences.setApplicationXmGeneratedInBuildDirectory(b);
    preferences.setWebMavenArchiverUsesBuildDirectory(b);
    MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().savePreferences(preferences, null);
  }

  protected void useBuildDirforGeneratingFiles(boolean b) {
    useBuildDirforGeneratingFiles(null, b);
  }  
}
