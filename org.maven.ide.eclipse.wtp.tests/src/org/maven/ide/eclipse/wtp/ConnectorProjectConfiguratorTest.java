/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.componentcore.util.EARArtifactEdit;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;

public class ConnectorProjectConfiguratorTest extends AbstractWTPTestCase {


  public void testMNGECLIPSE1949_RarSupport() throws Exception {
    
    IProject[] projects = importProjects("projects/MNGECLIPSE-1949/", new String[]{"rar1/pom.xml", "core/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    
    IProject rar1 =  projects[0];
    assertMarkers(rar1, 0);    
    assertNotNull(rar1);
    IProject core =  projects[1];
    assertNotNull(core);
    assertMarkers(core, 0);
   
    IFacetedProject connector = ProjectFacetsManager.create(rar1);
    assertNotNull(connector);
    assertEquals(IJ2EEFacetConstants.JCA_15, connector.getInstalledVersion(IJ2EEFacetConstants.JCA_FACET));
    assertTrue(connector.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertMarkers(connector.getProject(), 0);


    IVirtualComponent rarComp = ComponentCore.createComponent(rar1);
    IVirtualFolder rootRar = rarComp.getRootFolder();
    IResource[] rarResources = rootRar.getUnderlyingResources();
    assertEquals(2, rarResources.length);
    assertEquals(rar1.getFolder("/src/main/rar"), rarResources[0]);
    assertEquals(rar1.getFolder("/src/main/java"), rarResources[1]);

    
    if (!WTPProjectsUtil.isJavaEE6Available()) {
      //Component export is broken for WTP < 3.2 (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=298735)
      //So we skip this last part.
      return;
    }
    
    IVirtualReference[] references = rarComp.getReferences();


    assertEquals(4, references.length);
    IVirtualReference commonsCollections = references[0];
    assertTrue("commons-collections-2.0.jar expected but was "+commonsCollections.getArchiveName(), commonsCollections.getArchiveName().endsWith("commons-collections-2.0.jar"));
    IVirtualReference javaee = references[1];
    assertTrue("javaee-api-6.0.jar expected but was "+javaee.getArchiveName(), javaee.getArchiveName().endsWith("javaee-api-6.0.jar"));
    IVirtualReference coreRef = references[2];
    assertTrue("core-0.0.1-SNAPSHOT.jar expected but was "+coreRef.getArchiveName(), coreRef.getArchiveName().endsWith("core-0.0.1-SNAPSHOT.jar"));
    IVirtualComponent coreComp = ComponentCore.createComponent(core);
    assertEquals(coreRef.getReferencedComponent(),coreComp);   
    IVirtualReference junit = references[3];
    assertTrue("junit-3.8.1.jar expected but was "+junit.getArchiveName(), junit.getArchiveName().endsWith("junit-3.8.1.jar"));
    
    //Check core project won't deploy it's test resources : 
    IResource[] coreResources = coreComp.getRootFolder().getUnderlyingResources();
    assertEquals(2, rarResources.length);
    assertEquals(core.getFolder("/src/main/java"), coreResources[0]);
    assertEquals(core.getFolder("/src/main/resources"), coreResources[1]);

    
    
    updateProject(rar1, "changeDependencies.xml");    
    assertMarkers(rar1, 0); //FIXME maven compiler plugin barks and a marker is present :
    //org.eclipse.jdt.core.problem:The project cannot be built until its prerequisite core is built. Cleaning and building all projects is recommended 
    //ignored for now, as manual tests show no problem
  
    references = rarComp.getReferences();
    assertEquals(1, references.length);//Provided dependency not deployed
    IVirtualReference commonsLang = references[0];
    assertTrue("commons-lang-2.4.jar expected but was "+commonsLang.getArchiveName(), commonsLang.getArchiveName().endsWith("commons-lang-2.4.jar"));

  }


  public void testMNGECLIPSE1949_RarSourceDirectory() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-1949/rar2/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();
    IFacetedProject connector = ProjectFacetsManager.create(project);
    assertNotNull(connector);
    assertMarkers(project, 0);

    IVirtualComponent rarComp = ComponentCore.createComponent(project);
    IVirtualFolder rootRar = rarComp.getRootFolder();
    IResource[] rarResources = rootRar.getUnderlyingResources();
    assertEquals(2, rarResources.length);
    //Check non default rarSourceDirectory 
    assertEquals(project.getFolder("/connector"), rarResources[0]);
    assertEquals(project.getFolder("/src/main/java"), rarResources[1]);

    updateProject(project, "changeRaDir.xml");    
    assertMarkers(project, 0);    

    rarComp = ComponentCore.createComponent(project);
    rootRar = rarComp.getRootFolder();
    rarResources = rootRar.getUnderlyingResources();
    assertEquals(3, rarResources.length);//FIXME I haven't found a way to delete the previous content directory (WTP has no such thing as .getContentDirectory() for connector projects)
    assertEquals(project.getFolder("/src/main/rar"), rarResources[2]);
  }

  public void testMNGECLIPSE1949_IncludeJar() throws Exception {
    //Check non default rarSourceDirectory 
    IProject project = importProject("projects/MNGECLIPSE-1949/rar3/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();
    IFacetedProject connector = ProjectFacetsManager.create(project);
    assertNotNull(connector);
    assertMarkers(project, 0);

    IVirtualComponent rarComp = ComponentCore.createComponent(project);
    IVirtualFolder rootRar = rarComp.getRootFolder();
    IResource[] rarResources = rootRar.getUnderlyingResources();
    //includeJar set to false, classes won't be included
    assertEquals(1, rarResources.length);
    assertEquals(project.getFolder("/src/main/rar"), rarResources[0]);

    updateProject(project, "includeJar.xml");    
    assertMarkers(project, 0);    

    rarResources = rootRar.getUnderlyingResources();
    assertEquals(3, rarResources.length);
    //includeJar set to true, classes and resources are included 
    assertEquals(project.getFolder("/src/main/java"), rarResources[1]);
    assertEquals(project.getFolder("/src/main/resources"), rarResources[2]);
  }

  public void testMNGECLIPSE1949_CustomRarXml() throws Exception {
    if (!WTPProjectsUtil.isJavaEE6Available()) {
      //WTP < 3.2 is just stupid and finds 2 underlying files for virtualRaXml instead of 1.
      //Skip it.
      return;
    }
    
    IProject project = importProject("projects/MNGECLIPSE-1949/rar4/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();
    IFacetedProject connector = ProjectFacetsManager.create(project);
    assertNotNull(connector);
    assertMarkers(project, 0);

    IVirtualComponent rarComp = ComponentCore.createComponent(project);
    IVirtualFolder rootRar = rarComp.getRootFolder();
    IVirtualFile virtualRaXml = rootRar.getFile("META-INF/ra.xml");
    assertTrue(virtualRaXml.exists());
    IFile[] raXmlFiles = virtualRaXml.getUnderlyingFiles();
    assertEquals("found "+toString(raXmlFiles),  1, raXmlFiles.length);
    //Check non default ra.xml 
    assertEquals(project.getFile("/etc/ra.xml"), raXmlFiles[0]);
    
    updateProject(project, "ChangeCustomRaXml.xml");    
    assertMarkers(project, 0);    
    assertTrue(virtualRaXml.exists());
    raXmlFiles = virtualRaXml.getUnderlyingFiles();
    assertEquals(project.getFile("/etc2/custom-ra.xml"), raXmlFiles[0]);
    
    updateProject(project, "DeleteCustomRaXml.xml");    
    assertMarkers(project, 0);    
    assertFalse(virtualRaXml.exists());
  }


  public void testMNGECLIPSE1949_RarInEar() throws Exception {
    
    IProject[] projects = importProjects("projects/MNGECLIPSE-1949/", new String[]{"rar5/pom.xml", "core/pom.xml", "connector-ear/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    
    IProject rar5 =  projects[0];
    assertNotNull(rar5);
    IProject core =  projects[1];
    assertMarkers(core, 0);
    IProject ear =  projects[2];
    assertMarkers(ear, 0);
   
    IFacetedProject connector = ProjectFacetsManager.create(rar5);
    assertNotNull(connector);
    assertEquals(IJ2EEFacetConstants.JCA_15, connector.getInstalledVersion(IJ2EEFacetConstants.JCA_FACET));
    assertTrue(connector.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertMarkers(connector.getProject(), 0);


    IVirtualComponent rarComp = ComponentCore.createComponent(rar5);
    IVirtualFolder rootRar = rarComp.getRootFolder();
    IResource[] rarResources = rootRar.getUnderlyingResources();
    assertEquals(2, rarResources.length);
    assertEquals(rar5.getFolder("/src/main/rar"), rarResources[0]);
    assertEquals(rar5.getFolder("/src/main/java"), rarResources[1]);

    IVirtualComponent earComp = ComponentCore.createComponent(ear);
    assertEquals(1, earComp.getReferences().length);
    IVirtualReference rarRef = earComp.getReference("rar5");
    assertNotNull(rarRef);
    assertEquals("rar5-0.0.1-SNAPSHOT.rar",rarRef.getArchiveName());

    //Check connector presence in application.xml
    EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
    assertNotNull(edit);
    String uri = edit.getModuleURI(rarRef.getReferencedComponent());
    assertEquals("rar5-0.0.1-SNAPSHOT.rar", uri);
    
    if (!WTPProjectsUtil.isJavaEE6Available()) {
      //Component export is broken for WTP < 3.2 (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=298735)
      //So we skip this last part.
      return;
    }
    
    IVirtualReference[] references = rarComp.getReferences();


    assertEquals(3, references.length);
    IVirtualReference commonsCollections = references[0];
    assertTrue("commons-collections-2.0.jar expected but was "+commonsCollections.getArchiveName(), commonsCollections.getArchiveName().endsWith("commons-collections-2.0.jar"));
    IVirtualReference coreRef = references[1];
    assertTrue("core-0.0.1-SNAPSHOT.jar expected but was "+coreRef.getArchiveName(), coreRef.getArchiveName().endsWith("core-0.0.1-SNAPSHOT.jar"));
    IVirtualComponent coreComp = ComponentCore.createComponent(core);
    assertEquals(coreRef.getReferencedComponent(),coreComp);   
    IVirtualReference junit = references[2];
    assertTrue("junit-3.8.1.jar expected but was "+junit.getArchiveName(), junit.getArchiveName().endsWith("junit-3.8.1.jar"));
  }

}
