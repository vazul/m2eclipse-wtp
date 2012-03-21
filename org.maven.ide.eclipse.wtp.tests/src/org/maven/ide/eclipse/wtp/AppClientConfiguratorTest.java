/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.componentcore.util.EARArtifactEdit;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Test;

@SuppressWarnings("restriction")
public class AppClientConfiguratorTest extends AbstractWTPTestCase {

  @Test
  public void testMECLIPSEWTP104_AppClientSupport() throws Exception {
    
    IProject project = importProject("projects/MECLIPSEWTP-104/appclient-jee5/pom.xml");
    waitForJobsToComplete();
    assertNoErrors(project);    
   
    IFacetedProject appclient = ProjectFacetsManager.create(project);
    assertNotNull(appclient);
    assertEquals(IJ2EEFacetConstants.APPLICATION_CLIENT_50, appclient.getInstalledVersion(IJ2EEFacetConstants.APPLICATION_CLIENT_FACET));
    assertEquals(JavaFacet.VERSION_1_5, appclient.getInstalledVersion(JavaFacet.FACET));

    IVirtualComponent appClientComp = ComponentCore.createComponent(project);
    IVirtualFolder root = appClientComp.getRootFolder();
    IResource[] acResources = root.getUnderlyingResources();
    assertEquals(2, acResources.length);
    assertEquals(project.getFolder("/src/main/resources"), acResources[0]);
    assertEquals(project.getFolder("/src/main/java"), acResources[1]);
    assertFalse("application-client.xml should not have been created", project.getFile("/src/main/resources/META-INF/application-client.xml").exists());
  }
  
  @Test
  public void testMECLIPSEWTP104_AppClientFilteringSupport() throws Exception {
    
    IProject project = importProject("projects/MECLIPSEWTP-104/appclient-jee6/pom.xml");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    assertNoErrors(project);    
   
    IFacetedProject appclient = ProjectFacetsManager.create(project);
    assertNotNull(appclient);
    assertEquals(IJ2EEFacetConstants.APPLICATION_CLIENT_60, appclient.getInstalledVersion(IJ2EEFacetConstants.APPLICATION_CLIENT_FACET));
    assertEquals(JavaFacet.VERSION_1_6, appclient.getInstalledVersion(JavaFacet.FACET));

    IVirtualComponent appClientComp = ComponentCore.createComponent(project);
    IVirtualFolder root = appClientComp.getRootFolder();
    IResource[] acResources = root.getUnderlyingResources();
    assertEquals(1, acResources.length);
    assertEquals(project.getFolder("/src/main/resources"), acResources[0]);
    
    IFile filteredAppClientXml = project.getFile("/target/classes/META-INF/application-client.xml");
    assertTrue("application-client.xml is missing", filteredAppClientXml.exists());
    
    String xml = getAsString(filteredAppClientXml);
    assertTrue("DD was not filtered : "+xml, xml.contains("<display-name>appclient-jee6</display-name>"));
  }
  
  @Test
  public void testMECLIPSEWTP104_AppClientInEar() throws Exception {
    
    IProject[] projects = importProjects("projects/MECLIPSEWTP-104/", new String[]{"ear6/pom.xml", "appclient-jee6/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    
    IProject ear =  projects[0];
    assertNoErrors(ear);
    IProject ac =  projects[1];
    assertNotNull(ac);
   
    IFacetedProject appclient = ProjectFacetsManager.create(ac);
    assertNotNull(appclient);
    assertEquals(IJ2EEFacetConstants.APPLICATION_CLIENT_60, appclient.getInstalledVersion(IJ2EEFacetConstants.APPLICATION_CLIENT_FACET));
    assertEquals(JavaFacet.VERSION_1_6, appclient.getInstalledVersion(JavaFacet.FACET));


    IVirtualComponent acComp = ComponentCore.createComponent(ac);

    IVirtualComponent earComp = ComponentCore.createComponent(ear);
    IVirtualReference[] references =earComp.getReferences(); 
    assertEquals(2, references.length);
    IVirtualReference acRef = earComp.getReference("appclient-jee6");
    assertNotNull(acRef);
    assertEquals("appclient-jee6-0.0.1-SNAPSHOT.jar",acRef.getArchiveName());
    assertEquals("/",acRef.getRuntimePath().toPortableString());
    assertEquals(acRef.getReferencedComponent(), acComp);

    //Check connector presence in application.xml
    EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
    assertNotNull(edit);
    String uri = edit.getModuleURI(acRef.getReferencedComponent());
    assertEquals("appclient-jee6-0.0.1-SNAPSHOT.jar", uri);
    
    IVirtualReference junit = references[1];
    assertEquals(junit.getArchiveName(), "junit-3.8.1.jar");
  }  

}
