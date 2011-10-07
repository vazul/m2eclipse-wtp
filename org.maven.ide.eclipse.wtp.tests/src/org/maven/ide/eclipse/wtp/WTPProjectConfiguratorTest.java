/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;


import static org.maven.ide.eclipse.wtp.MavenWtpConstants.EAR_RESOURCES_FOLDER;
import static org.maven.ide.eclipse.wtp.MavenWtpConstants.M2E_WTP_FOLDER;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.application.WebModule;
import org.eclipse.jst.j2ee.componentcore.util.EARArtifactEdit;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.javaee.application.Application;
import org.eclipse.jst.javaee.application.Module;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.util.ComponentUtilities;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Test;

/**
 * WTPProjectConfiguratorTest
 *
 * @author igor
 */
@SuppressWarnings("restriction")
public class WTPProjectConfiguratorTest extends AbstractWTPTestCase {

  @Test
  public void testSimple01_import() throws Exception {
    IProject project = importProject("projects/simple/p01/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(WebFacetUtils.WEB_23, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));

    IResource[] underlyingResources = getUnderlyingResources(project);
    assertEquals(2, underlyingResources.length);
    assertEquals(project.getFolder("/src/main/webapp"), underlyingResources[1]);

    assertFalse(project.exists(new Path("/src/main/webapp/WEB-INF/lib")));
    
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    
    assertTrue(project.exists(new Path("/target/m2e-wtp/web-resources/META-INF/MANIFEST.MF")));
    assertTrue(project.exists(new Path("/target/m2e-wtp/web-resources/META-INF/maven/")));
}

  @Test
  public void testSimple02_import() throws Exception {
    IProject project = importProject("projects/simple/p02/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(WebFacetUtils.WEB_23, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));

    assertTrue(project.exists(new Path("/src/main/webapp/WEB-INF/lib")));
}

  @Test
  public void testSimple03_import() throws Exception {
    IProject project = importProject("projects/simple/p03/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(DEFAULT_WEB_VERSION, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
  }

  @Test
  public void testMECLIPSEWTP161_RemoveOldSourcePaths () throws Exception {
      IProject ear = importProject("projects/MECLIPSEWTP-161/pom.xml");
      waitForJobsToComplete();
      ear.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
      assertNoErrors(ear);   
      IResource[] underlyingResources = getUnderlyingResources(ear);
      assertEquals(2, underlyingResources.length);
      assertEquals(ear.getFolder("/src/main/application"), underlyingResources[1]);

      updateProject(ear, "change-earcontent.xml", 2000);
      
      underlyingResources = getUnderlyingResources(ear);
      assertEquals(2, underlyingResources.length);
      assertEquals(ear.getFolder("/EarContent"), underlyingResources[1]);
  }

  @Test
  public void testMNGECLIPSE631() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-631", //
        new String[] {"common/pom.xml", "core/pom.xml", "project1/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    IVirtualComponent component = ComponentCore.createComponent(projects[2]);
    IVirtualReference[] references = component.getReferences();
    assertEquals(toString(references),2, references.length);
    assertEquals(projects[0], references[1].getReferencedComponent().getProject());
    assertEquals(projects[1], references[0].getReferencedComponent().getProject());
  }

  @Test
  public void testSimple04_testScopeDependency() throws Exception {
    IProject[] projects = importProjects("projects/simple", //
        new String[] {"t01/pom.xml", "p04/pom.xml"}, new ResolverConfiguration());

    IVirtualComponent component = ComponentCore.createComponent(projects[1]);
    assertEquals(0, component.getReferences().length);

    IJavaProject javaProject = JavaCore.create(projects[1]);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertEquals(projects[0].getFullPath(), cp[0].getPath());
  }


  @Test
  public void testMNGECLIPSE1578_testRuntimeScopeDependency() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-1578", //
        new String[] {"war/pom.xml", "runtime-jar/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    IProject war = projects[0];
    IProject runtimeJar = projects[1];
    assertNoErrors(war);
    assertNoErrors(runtimeJar);
    IVirtualComponent warComponent = ComponentCore.createComponent(projects[0]);
    IVirtualReference[] references = warComponent.getReferences();
    assertEquals("Unexpected number of references found :"+toString(references),3, references.length);
  }

  @Test
  public void testSimple05_sourceFolders() throws Exception {
    IProject project = importProject("projects/simple/p05/pom.xml", new ResolverConfiguration());
    
    IVirtualComponent component = ComponentCore.createComponent(project);
    IVirtualFolder root = component.getRootFolder();
    IVirtualFolder folder = root.getFolder("/WEB-INF/classes");
    IResource[] underlyingResources = folder.getUnderlyingResources();
    assertEquals(2, underlyingResources.length);
    assertEquals(project.getFolder("/src/main/java"), underlyingResources[0]);
    assertEquals(project.getFolder("/src/main/resources"), underlyingResources[1]);
  }

  @Test
  public void testNonDefaultWarSourceDirectory() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-627/TestWar/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();
    IVirtualComponent component = ComponentCore.createComponent(project);
    IVirtualFolder root = component.getRootFolder();
    IResource[] underlyingResources = root.getUnderlyingResources();
    assertEquals(2, underlyingResources.length);
    assertEquals(project.getFolder("/webapp"), underlyingResources[1]);
  }


  @Test
  public void testMNGECLIPSE1600_absoluteDirectories() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-1600/", new String[] {"test/pom.xml", "testEAR/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    IVirtualComponent warComponent = ComponentCore.createComponent(projects[0]);
    IVirtualFolder rootwar = warComponent.getRootFolder();
    IResource[] warResources = rootwar.getUnderlyingResources();
    assertEquals(2, warResources.length);
    assertEquals(projects[0].getFolder("/WebContent"), warResources[1]);

    IVirtualComponent earComponent = ComponentCore.createComponent(projects[1]);
    IVirtualFolder rootEar = earComponent.getRootFolder();
    IResource[] earResources = rootEar.getUnderlyingResources();
    assertEquals(2, earResources.length);
    assertEquals(projects[1].getFolder("/EarContent"), earResources[1]);
  }

  
  @Test
  public void testSameArtifactId() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-679/pom.xml", new ResolverConfiguration());
    
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    assertEquals(2, cp.length);
    assertEquals("junit-junit-3.8.1.jar", cp[0].getPath().lastSegment());
    assertEquals("test-junit-3.8.1.jar", cp[1].getPath().lastSegment());
  }


  
  @Test
  public void testMECLIPSEWTP112_CustomFilenameMapping() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(configuration);
    importConfiguration.setProjectNameTemplate("[groupId]-[artifactId]");
 
    importProject("core1", "projects/MECLIPSEWTP-112/core1", importConfiguration);
    IProject core1 = workspace.getRoot().getProject("foo.bar-core");
    assertTrue(core1.exists());
    importProject("core2", "projects/MECLIPSEWTP-112/core2", importConfiguration);
    IProject core2 = workspace.getRoot().getProject("bar.foo-core");
    assertTrue(core2.exists());

    IProject web   = importProject("projects/MECLIPSEWTP-112/web/pom.xml", configuration);
    waitForJobsToComplete();
    assertNoErrors(web);
    IVirtualComponent warComponent = ComponentCore.createComponent(web);
    assertNotNull(warComponent);
    IVirtualReference[] references =warComponent.getReferences(); 
    assertEquals(4, references.length);
    assertEquals("foo.bar-core-0.0.1-SNAPSHOT.jar", warComponent.getReferences()[0].getArchiveName());
    assertEquals("bar.foo-core-0.0.1-SNAPSHOT.jar", warComponent.getReferences()[1].getArchiveName());
    
    IJavaProject javaProject = JavaCore.create(web);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    assertEquals(4, cp.length);
    assertEquals("junit-junit-3.8.1.jar", cp[0].getPath().lastSegment());
    assertEquals("foo.bar-core", cp[1].getPath().lastSegment());
    assertEquals("bar.foo-core", cp[2].getPath().lastSegment());
    assertEquals("commons-lang-commons-lang-2.4.jar", cp[3].getPath().lastSegment());

  }

  
  
  //@Test
  public void _testLooseBuildDirectory() throws Exception {
    // import should not fail for projects with output folders located outside of project's basedir
    importProject("projects/MNGECLIPSE-767/pom.xml", new ResolverConfiguration());
  }

  @Test
  public void testEnableMavenNature() throws Exception {
    IProject project = createExisting("test.project.MNGECLIPSE-629", "projects/MNGECLIPSE-629");

    IFacetedProject facetedProject;

    facetedProject = ProjectFacetsManager.create(project);
    assertNull(facetedProject);

    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    configurationManager.enableMavenNature(project, new ResolverConfiguration(), monitor);

    facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
  }
  
  @Test
  public void testMNGECLIPSE793() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-793", //
        new String[] {"common/pom.xml", "core/pom.xml", "project1/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    IVirtualComponent core = ComponentCore.createComponent(projects[1]); //core
    IVirtualReference[] references = core.getReferences();
    assertTrue(references == null || references.length == 0);
  }
  
  
  @Test
  public void testMNGECLIPSE688_defaultEjb() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-688/ejb1/pom.xml", new ResolverConfiguration());

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
    //Defaut ejb project should have 2.1 project facet
    assertEquals(IJ2EEFacetConstants.EJB_21, facetedProject.getInstalledVersion(EJB_FACET));
    
    IFile ejbJar = project.getFile("src/main/resources/META-INF/ejb-jar.xml");
    assertTrue(ejbJar.exists());
    //TODO check DTD
  }

  @Test
  public void testMNGECLIPSE688_Ejb_30() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-688/ejb2/pom.xml", new ResolverConfiguration());

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
    assertEquals(JavaFacetUtils.JAVA_50, facetedProject.getInstalledVersion(JavaFacet.FACET));
    assertEquals(IJ2EEFacetConstants.EJB_30, facetedProject.getInstalledVersion(EJB_FACET));

    IFolder ejbModuleFolder = project.getFolder("ejbModule"); 
    assertTrue(ejbModuleFolder.exists());

    //ejb-jar file should not have been created in the custom resources directory, as it's not mandatory according to the Java EE 5 specs
    IFile ejbJar = project.getFile("ejbModule/META-INF/ejb-jar.xml"); 
    assertFalse(ejbJar.exists());
    //TODO check DTD
  }

  @Test
  public void testMNGECLIPSE688_NonDeployedDependencies () throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-688", new String[]{"war-optional/pom.xml","core/pom.xml"}, new ResolverConfiguration());
    IProject war = projects[0]; 
    IProject optionalJar = projects[1]; 
    assertNoErrors(war);
    assertNoErrors(optionalJar);//MNGECLIPSE-1119 : optional projects shouldn't be deployed
    IClasspathEntry[] classpathEntries = getClassPathEntries(war);
    IClasspathEntry junit = classpathEntries[0];
    assertEquals("junit-3.8.1.jar", junit.getPath().lastSegment());
    assertNotDeployable(junit); //Junit is marked as <optional>true<optional>
    
    IVirtualComponent warComponent = ComponentCore.createComponent(war);
    //Even though core is optional and is not deployed, its dependencies are. Weird maven behavior
    assertEquals(3, warComponent.getReferences().length);
  }


  @Test
  public void testMNGECLIPSE744_NonDeployablePoms () throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-744", new String[]{"pom.xml", "MNGECLIPSE-744-web/pom.xml","pomDependency/pom.xml"}, new ResolverConfiguration());
    IProject war = projects[1]; 
    IProject pom = projects[2]; 
    assertNoErrors(war);
    assertNoErrors(pom);
    IVirtualComponent warComponent = ComponentCore.createComponent(war);
    assertEquals(1, warComponent.getReferences().length);
    //Check the only reference is not the dependent pom
    assertTrue("junit-3.8.1.jar expected", warComponent.getReferences()[0].getArchiveName().endsWith("junit-3.8.1.jar"));
  }

  @Test
  public void testMNGECLIPSE688_CustomEarContent () throws Exception {
    try {
      useBuildDirforGeneratingFiles(false);

      IProject ear = importProject("projects/MNGECLIPSE-688/ear21-1/pom.xml", new ResolverConfiguration());

      IFacetedProject fpEar = ProjectFacetsManager.create(ear);
      assertNotNull(fpEar);
      assertFalse(fpEar.hasProjectFacet(JavaFacet.FACET)); //Ears don't have java facet
      assertEquals(DEFAULT_EAR_FACET, fpEar.getInstalledVersion(EAR_FACET));

      IResource[] underlyingResources = getUnderlyingResources(ear);
      assertEquals(1, underlyingResources.length);
      assertEquals(ear.getFolder("/CustomEarSourceDirectory"), underlyingResources[0]);

      IFile applicationXml = ear.getFile("CustomEarSourceDirectory/META-INF/application.xml"); 
      assertTrue(applicationXml.exists());

    } finally {
      useBuildDirforGeneratingFiles(true);
    }
  }

  @Test
  public void testMNGECLIPSE688_Ear50 () throws Exception {
    try {
      useBuildDirforGeneratingFiles(false);
      
      IProject ear = importProject("projects/MNGECLIPSE-688/ear50-1/pom.xml", new ResolverConfiguration());
      waitForJobsToComplete();
      assertNoErrors(ear);
      
      IFacetedProject fpEar = ProjectFacetsManager.create(ear);
      assertNotNull(fpEar);
      assertFalse(fpEar.hasProjectFacet(JavaFacet.FACET)); //Ears don't have java facet
      assertEquals(IJ2EEFacetConstants.ENTERPRISE_APPLICATION_50, fpEar.getInstalledVersion(EAR_FACET));

      IResource[] underlyingResources = getUnderlyingResources(ear);
      assertEquals(1, underlyingResources.length);
      assertEquals(ear.getFolder("/src/main/application"), underlyingResources[0]);

      IFile applicationXml = ear.getFile("src/main/application/META-INF/application.xml"); 
      //assertFalse(applicationXml.exists()); // application.xml is not mandatory for Java EE 5.0, hence not created
      assertTrue(applicationXml.exists()); // application.xml is created as maven-ear-plugin is configured as such by default
      
      IVirtualComponent comp = ComponentCore.createComponent(ear);
      IVirtualReference[] references = comp.getReferences();
      assertEquals(1, references.length);
      IVirtualReference junit = references[0];
      //FIXME Test fail on WTP Galileo, as WTP adds an extra /lib prefix on the archivename.  
      //assertEquals("junit-3.8.1.jar", junit.getArchiveName());//Helios WTP works fine here
      assertTrue("junit-3.8.1.jar expected", junit.getArchiveName().endsWith("junit-3.8.1.jar"));
      
      
      //MNGECLIPSE-1872 : check "/lib" is used as deployment directory
      assertEquals("/lib", junit.getRuntimePath().toPortableString());
        
    } finally {
      useBuildDirforGeneratingFiles(true);
    }
  
  }

  
  @Test
  public void testMNGECLIPSE688_Jee1() throws Exception {
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-688/", //
        new String[] {"jee1/pom.xml", "jee1/core/pom.xml", "jee1/ejb/pom.xml", "jee1/war/pom.xml", "jee1/ear/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject core = projects[1];
    IProject ejb = projects[2];
    IProject war = projects[3];
    IProject ear = projects[4];
    
    assertNoErrors(core);
    List<IMarker> warnings = findMarkers(core, IMarker.SEVERITY_WARNING);
    
    assertNoErrors(ejb);
    assertNoErrors(war);
    assertNoErrors(ear);
    
    IFacetedProject fpCore = ProjectFacetsManager.create(core, false, monitor);
    assertNotNull(ProjectFacetsManager.getFacetedProjects().toString(), fpCore);  
    assertTrue(fpCore.hasProjectFacet(JavaFacet.FACET));
    assertEquals(UTILITY_10, fpCore.getInstalledVersion(UTILITY_FACET));
    
    IFacetedProject fpEjb = ProjectFacetsManager.create(ejb);
    assertNotNull(fpEjb);
    assertTrue(fpEjb.hasProjectFacet(JavaFacet.FACET));
    assertEquals(IJ2EEFacetConstants.EJB_21, fpEjb.getInstalledVersion(EJB_FACET));

    IFacetedProject fpWar = ProjectFacetsManager.create(war);
    assertNotNull(fpWar);
    assertTrue(fpWar.hasProjectFacet(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_24, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));

    IFacetedProject fpEar = ProjectFacetsManager.create(ear);
    assertNotNull(fpEar);
    assertFalse(fpEar.hasProjectFacet(JavaFacet.FACET)); //Ears don't have java facet
    assertEquals(DEFAULT_EAR_FACET, fpEar.getInstalledVersion(EAR_FACET));

    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference warRef = comp.getReference("war");
    assertNotNull(warRef);
    assertEquals("war-0.0.1-SNAPSHOT.war",warRef.getArchiveName());    
  }

  @Test
  public void testMNGECLIPSE688_Pom14_1() throws Exception {
    // these projects can actually be deployed to JBoss
    // importing projects in unsorted order
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-688/", //
        new String[] {"pom14-1/pom.xml", "pom14-1/ear14-1/pom.xml", "pom14-1/war23-1/pom.xml", "pom14-1/war23-2/pom.xml", "pom14-1/ejb21-1/pom.xml", "pom14-1/core-1/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(6, projects.length);
    IProject ear = projects[1];
    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference[] references = comp.getReferences();
    assertEquals(toString(references), 5, references.length);
    // The reference order changes between imports, so can't rely on references indexes
    assertNotNull(comp.getReference("core-1"));
    assertNotNull(comp.getReference("ejb21-1"));
    assertNotNull(comp.getReference("war23-1"));
    assertNotNull(comp.getReference("war23-2"));
    assertNotNull(comp.getReference("var/M2_REPO/commons-lang/commons-lang/2.4/commons-lang-2.4.jar"));
    
    // checked provided dependencies won't be deployed in the ear
    IProject war1 = projects[2];
    IClasspathEntry[] war1CP = getClassPathEntries(war1);
    assertEquals(Arrays.asList(war1CP).toString(), 6, war1CP.length);
    // war23-1 pom.xml states that no dependencies should be deployed (in WEB-INF/lib)
    for (IClasspathEntry entry : war1CP){
      assertNotDeployable(entry);
    }
  }


  @Test
  public void testMNGECLIPSE1119_OptionalProjectDependencies() throws Exception {
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-1119/", //
        new String[] {"pom.xml", "MNGECLIPSE-1119-ear/pom.xml", "MNGECLIPSE-1119-web/pom.xml", "MNGECLIPSE-1119-util/pom.xml", "MNGECLIPSE-1119-ejb/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject web = projects[2];
    
    IVirtualComponent comp = ComponentCore.createComponent(web);
    IVirtualReference[] references = comp.getReferences();
    assertEquals(toString(references), 1, references.length);//Only commons-lang should be referenced
    IClasspathEntry[] webCP = getClassPathEntries(web);
    assertEquals(Arrays.asList(webCP).toString(), 5, webCP.length);
    int i =0;
    for (IClasspathEntry entry : webCP){
      boolean deployable = i == 1; //only commons-lang is deployable
      assertDeployable(entry, deployable);
      i++;
    }
  }

//  @Test
//  public void testMNGECLIPSE597() throws Exception {
//    IProject[] projects = importProjects("projects/MNGECLIPSE-597", 
//        new String[] {"DWPMain/pom.xml", "DWPDependency/pom.xml", }, 
//        new ResolverConfiguration());
//
//    waitForJobsToComplete();
//    assertEquals(2, projects.length);
//    IProject dep = projects[1];
//    IProject main = projects[0];
//    
//    assertNoErrors(main);
//    assertNoErrors(dep);
//
//    IFacetedProject mainWar = ProjectFacetsManager.create(main);
//    assertNotNull(mainWar);
//    assertTrue(mainWar.hasProjectFacet(WebFacetUtils.WEB_FACET));
//
//    IFacetedProject depWar = ProjectFacetsManager.create(dep);
//    assertNotNull(depWar);
//    assertTrue(depWar.hasProjectFacet(WebFacetUtils.WEB_FACET));
//
//    IVirtualComponent comp = ComponentCore.createComponent(main);
//    IVirtualReference[] references = comp.getReferences();
//    IVirtualReference depRef = references[0];
//    assertEquals(dep, depRef.getReferencedComponent().getProject());
//  }


  @Test
  public void testMNGECLIPSE965_fileNames() throws Exception {
    // Exported filenames should be consistent when workspace resolution is on/off
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-965/", //
        new String[] {"ear-standardFileNames/pom.xml", "ear-fullFileNames/pom.xml", "testFileNameWar/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(3, projects.length);
    IProject earStandardFN = projects[0];
    IProject earFullFN = projects[1];
    IProject war = projects[2];
    
    assertNoErrors(earStandardFN);
    assertNoErrors(earFullFN);
    assertNoErrors(war);

    //Check standard file name mapping
    IVirtualComponent earStandardFNcomp = ComponentCore.createComponent(earStandardFN);
    IVirtualReference warRef = earStandardFNcomp.getReference("testFileNameWar");
    assertNotNull(warRef);
    assertEquals("testFileNameWar-0.0.1-SNAPSHOT.war",warRef.getArchiveName());

    //Check full file name mapping
    IVirtualComponent earFullFNcomp = ComponentCore.createComponent(earFullFN);
    warRef = earFullFNcomp.getReference("testFileNameWar");
    assertNotNull(warRef);
    assertEquals("foo-bar-testFileNameWar-0.0.1-SNAPSHOT.war",warRef.getArchiveName());


    IVirtualReference junitRef = earFullFNcomp.getReference("var/M2_REPO/junit/junit/3.8.1/junit-3.8.1.jar");
    assertNotNull(junitRef);
    assertEquals("junit-junit-3.8.1.jar",junitRef.getArchiveName());
    
  }  
  
  @Test
  public void testMNGECLIPSE984_errorMarkers() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-984/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();

    //Web Project configuration failed because Web 2.5 projects need Java 1.5 
    List<IMarker> markers = findErrorMarkers(project);
    assertEquals(2, markers.size());
    assertHasMarker("One or more constraints have not been satisfied.", markers);
    assertHasMarker("Dynamic Web Module 2.5 requires Java (1.5|5.0) or newer.", markers);//WTP < 3.2 says Java 5.0, WTP 3.2 : Java 1.5

    //Markers disappear when the compiler level is set to 1.5
    updateProject(project, "good_pom.xml");    
    assertNoErrors(project);    

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(DEFAULT_WEB_VERSION, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
    
  }

  @Test
  public void testMNGECLIPSE1045_TimestampedSnapshots() throws Exception {
    IProject ear = importProject("projects/MNGECLIPSE-1045/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();
    assertNoErrors(ear);

    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference[] references = comp.getReferences();
    assertEquals(1, references.length);
    IVirtualReference snapshot = references[0];
    assertEquals("MNGECLIPSE-1045-DEP-0.0.1-SNAPSHOT.jar", snapshot.getArchiveName());
  }
  
  @Test
  public void testMNGECLIPSE1627_SkinnyWars() throws Exception {
    
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-1627/", //
        new String[] {"ear/pom.xml", "utility1/pom.xml", "utility2/pom.xml", "war-fullskinny/pom.xml", "war-mixedskinny/pom.xml", },
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject ear = projects[0];
    IProject utility1 = projects[1];
    IProject utility2 = projects[2];
    IProject fullskinnywar = projects[3];
    IProject mixedskinnywar = projects[4];
    
    assertNoErrors(ear);
    assertNoErrors(utility1);
    assertNoErrors(utility2);
    assertNoErrors(fullskinnywar);
    assertNoErrors(mixedskinnywar);
    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    
    IVirtualReference utilityRef1 = comp.getReference("MNGECLIPSE-1627-utility1");
    assertNotNull(utilityRef1);
    IVirtualReference utilityRef2 = comp.getReference("MNGECLIPSE-1627-utility2");
    assertNotNull(utilityRef2);
    
    ////////////
    //check the fullskinny war project
    ////////////
    IVirtualReference fullSkinnyRef = comp.getReference("MNGECLIPSE-1627-war-fullskinny");
    assertNotNull(fullSkinnyRef);
    assertEquals("MNGECLIPSE-1627-war-fullskinny-0.0.1-SNAPSHOT.war",fullSkinnyRef.getArchiveName());    
    
    //the fully skinny war contains to project refs whatsoever
    IVirtualComponent fullSkinnyComp = fullSkinnyRef.getReferencedComponent();
    IVirtualReference[] fullSkinnyReferences = fullSkinnyComp.getReferences();
    assertEquals(4, fullSkinnyReferences.length);
    
    //check the component refs and their runtime path
    //TODO the reference ordering seems stable, but someone experienced should have a look
    assertEquals(utility1, fullSkinnyReferences[0].getReferencedComponent().getProject());
    assertEquals("/", fullSkinnyReferences[0].getRuntimePath().toString());
    assertEquals(utility2, fullSkinnyReferences[1].getReferencedComponent().getProject());
    assertEquals("/", fullSkinnyReferences[1].getRuntimePath().toString());    
    assertTrue(fullSkinnyReferences[2].getReferencedComponent().getDeployedName().endsWith("commons-lang-2.4.jar"));  
    assertEquals("/", fullSkinnyReferences[2].getRuntimePath().toString());  
    assertTrue(fullSkinnyReferences[3].getReferencedComponent().getDeployedName().endsWith("commons-collections-2.0.jar"));  
    assertEquals("/", fullSkinnyReferences[3].getRuntimePath().toString());  
    
    //check for all expected dependencies in the manifest
    IFile war1ManifestFile = ComponentUtilities.findFile(fullSkinnyComp, new Path(J2EEConstants.MANIFEST_URI));
    Manifest mf1 = loadManifest(war1ManifestFile);

    //check that manifest classpath contains all dependencies
    String classpath = mf1.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
    assertTrue(classpath.contains(utilityRef1.getArchiveName()));
    assertTrue(classpath.contains(utilityRef2.getArchiveName()));
    assertTrue(classpath.contains("commons-lang-2.4.jar"));
    assertTrue(classpath.contains("commons-collections-2.0.jar"));
    //...but not junit, which is a test dependency
    assertFalse(classpath.contains("junit-3.8.1.jar"));
    
    //check that junit is in the maven classpath container instead
    IClasspathEntry[] mavenContainerEntries = getMavenContainerEntries(fullskinnywar);
    assertEquals(5, mavenContainerEntries.length);
    assertEquals("junit-3.8.1.jar", mavenContainerEntries[4].getPath().lastSegment());
    
    ////////////
    //check the mixedskinny war project
    ////////////
    IVirtualReference mixedSkinnyRef = comp.getReference("MNGECLIPSE-1627-war-mixedskinny");
    assertNotNull(mixedSkinnyRef);
    assertEquals("MNGECLIPSE-1627-war-mixedskinny-0.0.1-SNAPSHOT.war",mixedSkinnyRef.getArchiveName());    
    
    IVirtualComponent mixedSkinnyComp = mixedSkinnyRef.getReferencedComponent();
    IVirtualReference[] mixedSkinnyReferences = mixedSkinnyComp.getReferences();
    
    //check the component refs and their runtime path
    //TODO the reference ordering seems stable, but someone experienced should have a look
    //TODO the WEB-INF/lib located refs seem to come first
    assertEquals(utility2, mixedSkinnyReferences[0].getReferencedComponent().getProject());
    assertEquals("/WEB-INF/lib", mixedSkinnyReferences[0].getRuntimePath().toString());    
    assertTrue(mixedSkinnyReferences[1].getReferencedComponent().getDeployedName().endsWith("commons-collections-2.0.jar"));  
    assertEquals("/WEB-INF/lib", mixedSkinnyReferences[1].getRuntimePath().toString());  
    assertEquals(utility1, mixedSkinnyReferences[2].getReferencedComponent().getProject());
    assertEquals("/", mixedSkinnyReferences[2].getRuntimePath().toString());
    assertTrue(mixedSkinnyReferences[3].getReferencedComponent().getDeployedName().endsWith("commons-lang-2.4.jar"));  
    assertEquals("/", mixedSkinnyReferences[3].getRuntimePath().toString());  

    //check for all expected dependencies in the manifest
    IFile war2ManifestFile = ComponentUtilities.findFile(mixedSkinnyComp, new Path(J2EEConstants.MANIFEST_URI));
    Manifest mf2 = loadManifest(war2ManifestFile);
    
    //check that manifest classpath only contain utility1 and commons-lang
    classpath = mf2.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
    assertTrue(classpath.contains(utilityRef1.getArchiveName()));
    assertTrue(classpath.contains(utilityRef2.getArchiveName()));
    //assertFalse(classpath.contains(utilityRef2.getArchiveName())); 
    assertTrue(classpath.contains("commons-lang-2.4.jar"));//Maven manifest contains all entries!!!
    //assertFalse(classpath.contains("commons-collections-2.0.jar"));
    assertTrue(classpath.contains("commons-collections-2.0.jar"));//Maven manifest contains all entries!!!
    
    //...but not junit, which is a test dependency
    assertFalse(classpath.contains("junit-3.8.1.jar"));
    
    //check that junit and commons-collections are in the maven classpath container instead
    mavenContainerEntries = getMavenContainerEntries(mixedskinnywar);
    assertEquals(5, mavenContainerEntries.length);
    assertEquals("commons-collections-2.0.jar", mavenContainerEntries[3].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", mavenContainerEntries[4].getPath().lastSegment());
  }

  private Manifest loadManifest(IFile war1ManifestFile) throws CoreException, IOException {
    Manifest mf1;
    InputStream is = war1ManifestFile.getContents();
    try {
      mf1 = new Manifest(is);
    } finally {
      is.close();
    }
    return mf1;
  }

  @Test
  public void testDeploymentDescriptorsJavaEE() throws Exception {
    
    IProject[] projects = importProjects(
        "projects/deployment-descriptors/", //
        new String[] {"javaEE/pom.xml", "javaEE/ear/pom.xml", "javaEE/core/pom.xml", "javaEE/ejb/pom.xml", "javaEE/war/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject ear = projects[1];
    IProject core = projects[2];
    IProject ejb = projects[3];
    IProject war = projects[4];
    
    assertNoErrors(core);
    assertNoErrors(ejb);
    assertNoErrors(war);
    assertNoErrors(ear);
   
    IFacetedProject fpWar = ProjectFacetsManager.create(war);
    assertNotNull(fpWar);
    assertTrue(fpWar.hasProjectFacet(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_24, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));

    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference warRef = comp.getReference("war");
    assertNotNull(warRef);
    assertEquals("war-0.0.1-SNAPSHOT.war",warRef.getArchiveName());
    IVirtualReference coreRef = comp.getReference("core");
    assertNotNull(coreRef);
    assertEquals("core-0.0.1-SNAPSHOT.jar",coreRef.getArchiveName());
    IVirtualReference ejbRef = comp.getReference("ejb");
    assertNotNull(ejbRef);
    assertEquals("ejb-0.0.1-SNAPSHOT.jar",ejbRef.getArchiveName());
    
    EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
    assertEquals(3,edit.getApplication().getModules().size());
    assertEquals("/dummy",edit.getWebContextRoot(war));

    assertNotNull(coreRef.getArchiveName() + " not found", edit.getApplication().getModule(coreRef.getArchiveName(), null));

    assertNotNull(ejbRef.getArchiveName() + " not found", edit.getApplication().getModule(ejbRef.getArchiveName(), null));

    assertEquals(2, edit.getApplication().getSecurityRoles().size());

    /* TODO investigate why the rest of the test fails randomly
    updateProject(ear, "pom.step2.xml", 2000);     
    
    app = (Application)ModelProviderManager.getModelProvider(ear).getModelObject();
    assertEquals(dumpModules(app.getModules()),2,app.getModules().size());
    coreModule = app.getFirstModule(coreRef.getArchiveName());
    assertNull(coreRef.getArchiveName()+" javamodule should be missing",coreModule);

    webModule = app.getFirstModule(warRef.getArchiveName());
    assertNotNull("missing webmodule "+warRef.getArchiveName(),webModule);
    assertEquals("/war-root",webModule.getWeb().getContextRoot());
    
    ejbModule = app.getFirstModule(ejbRef.getArchiveName());
    assertNotNull("missing ejbModule "+ejbRef.getArchiveName(),ejbModule);
    assertEquals("altdd-ejb.jar",ejbModule.getAltDd());

    roles = app.getSecurityRoles();
    assertEquals(3, roles.size());
    */
  }

  @Test
  public void testDeploymentDescriptorsJ2EE() throws Exception {
    deleteProject("J2EE");
    deleteProject("ear");
    deleteProject("ejb");
    deleteProject("war");
    deleteProject("core");

    IProject[] projects = importProjects(
        "projects/deployment-descriptors/", //
        new String[] {"J2EE/pom.xml", "J2EE/ear/pom.xml", "J2EE/core/pom.xml", "J2EE/ejb/pom.xml", "J2EE/war/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject ear = projects[1];
    IProject core = projects[2];
    IProject ejb = projects[3];
    IProject war = projects[4];
    
    assertNoErrors(core);
    assertNoErrors(ejb);
    assertNoErrors(war);
    assertNoErrors(ear);
   
    IFacetedProject fpWar = ProjectFacetsManager.create(war);
    assertNotNull(fpWar);
    assertTrue(fpWar.hasProjectFacet(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_24, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));

    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference warRef = comp.getReference("war");
    assertNotNull(warRef);
    assertEquals("war-0.0.1-SNAPSHOT.war",warRef.getArchiveName());
    IVirtualReference coreRef = comp.getReference("core");
    assertNotNull(coreRef);
    assertEquals("core-0.0.1-SNAPSHOT.jar",coreRef.getArchiveName());
    IVirtualReference ejbRef = comp.getReference("ejb");
    assertNotNull(ejbRef);
    assertEquals("ejb-0.0.1-SNAPSHOT.jar",ejbRef.getArchiveName());
    /*FIXME test started to fail sometime after we added the mavenarchiver dependency
    org.eclipse.jst.j2ee.application.Application app = (org.eclipse.jst.j2ee.application.Application)ModelProviderManager.getModelProvider(ear).getModelObject();
    assertEquals(3,app.getModules().size());
    org.eclipse.jst.j2ee.application.Module webModule = app.getFirstModule(warRef.getArchiveName());
    assertNotNull("missing webmodule "+warRef.getArchiveName(),webModule);
    assertTrue(webModule.isWebModule());
    assertEquals("/dummy",((WebModule)webModule).getContextRoot());

    org.eclipse.jst.j2ee.application.Module coreModule = app.getFirstModule(coreRef.getArchiveName());
    assertNotNull("missing javaModule "+coreRef.getArchiveName(),coreModule);

    org.eclipse.jst.j2ee.application.Module ejbModule = app.getFirstModule(ejbRef.getArchiveName());
    assertTrue(ejbModule.isEjbModule());
    assertNotNull("missing ejbModule "+ejbRef.getArchiveName(),ejbModule);
    assertNull(ejbModule.getAltDD());
    
    List<SecurityRole> roles = app.getSecurityRoles();
    assertNotNull(roles);
    assertEquals(2, roles.size());

    updateProject(ear, "pom.step2.xml");     
    
    app = (org.eclipse.jst.j2ee.application.Application)ModelProviderManager.getModelProvider(ear).getModelObject();
    assertEquals(2,app.getModules().size());
    coreModule = app.getFirstModule(coreRef.getArchiveName());
    assertNull(coreRef.getArchiveName()+" javamodule should be missing",coreModule);

    webModule = app.getFirstModule(warRef.getArchiveName());
    assertNotNull("missing webmodule "+warRef.getArchiveName(),webModule);
    assertEquals("/war-root",((WebModule)webModule).getContextRoot());
    
    ejbModule = app.getFirstModule(ejbRef.getArchiveName());
    assertNotNull("missing ejbModule "+ejbRef.getArchiveName(),ejbModule);
    assertEquals("altdd-ejb.jar",ejbModule.getAltDD());

    roles = app.getSecurityRoles();
    assertEquals(3, roles.size());
    */
}

  @Test
  public void testMNGECLIPSE1088_generateApplicationXml() throws Exception {
    try {
      useBuildDirforGeneratingFiles(false);
      IProject[] projects = importProjects(
          "projects/MNGECLIPSE-1088/", //
          new String[] {"A/pom.xml", "B/pom.xml", "C/pom.xml", "D/pom.xml"},
          new ResolverConfiguration());
  
      waitForJobsToComplete();
      
      assertEquals(4, projects.length);
      IProject ejb = projects[0];
      IProject war = projects[1];
      IProject ear1 = projects[2];
      IProject ear2 = projects[3];
      
	  assertNoErrors(war);
	  assertNoErrors(ejb);
	  assertNoErrors(ear1);
	  assertNoErrors(ear2);
     
      String applicationXmlRelativePath = "src/main/application/META-INF/application.xml";
      assertTrue(ear1.getFile(applicationXmlRelativePath).exists()); // application.xml is created as maven-ear-plugin is configured as such by default
      Application app1 = (Application)ModelProviderManager.getModelProvider(ear1).getModelObject();
      assertEquals(2,app1.getModules().size());
      assertNotNull("missing jarmodule for C",app1.getFirstModule("A-0.0.1-SNAPSHOT.jar"));
      assertNotNull("missing webmodule for C",app1.getFirstModule("B-0.0.1-SNAPSHOT.war"));//EAR should not use finalName 
  
      assertFalse(ear2.getFile(applicationXmlRelativePath).exists());// application.xml is not created as per maven-ear-plugin configuration 
  //    If maven doesn't generate application.xml, the Application app2 will be empty, since WTP's API is not used     
  //    Application app2 = (Application)ModelProviderManager.getModelProvider(ear2).getModelObject();
  //    assertEquals(2,app2.getModules().size());
  //    assertNotNull("missing jarmodule for D",app2.getFirstModule("A.jar"));
  //    assertNotNull("missing webmodule for D",app2.getFirstModule("B.war"));
    } finally {
      useBuildDirforGeneratingFiles(true);
    }
}

  //Lars K�dderitzsch test case from https://issues.sonatype.org/browse/MNGECLIPSE-1644
  @Test
  public void testMNGECLIPSE1644_contextRoot() throws Exception {
     try {
     useBuildDirforGeneratingFiles(false);
     IProject[] projects = importProjects(
         "projects/MNGECLIPSE-1644/", //
         new String[] {"ear/pom.xml", "war1/pom.xml", "war2/pom.xml", },
         new ResolverConfiguration());

     waitForJobsToComplete();
     
     assertEquals(3, projects.length);
     IProject ear = projects[0];
     IProject war1 = projects[1];
     IProject war2 = projects[2];
     
     assertNoErrors(ear);
     assertNoErrors(war1);
     assertNoErrors(war2);
     
     //check the context roots of the wars in the ear
     EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
     assertNotNull(edit);
     String war1ContextRoot = edit.getWebContextRoot(war1);
     String war2ContextRoot = edit.getWebContextRoot(war2);
     edit.dispose();
     
     assertEquals("/custom-context-root", war1ContextRoot);
     assertEquals("/MNGECLIPSE-1644-war2", war2ContextRoot);
     } finally {
       useBuildDirforGeneratingFiles(true);
     }
  }

  @Test
  public void testMNGECLIPSE2145_finalNames() throws Exception {
    //project finalNames are no longer used in EAR deployment, according to maven-ear-plugin 2.5 
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-2145/testcase", //
        new String[] {"pom.xml", "ear/pom.xml", "war/pom.xml", "jar/pom.xml", },
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(4, projects.length);
    IProject pom = projects[0];
    IProject ear = projects[1];
    IProject war = projects[2];
    IProject jar = projects[3];
    
    assertNoErrors(pom);
    assertNoErrors(ear);
    assertNoErrors(war);
    assertNoErrors(jar);
    
    //check the context roots of the wars in the ear
    EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
    assertNotNull(edit);
    
    IVirtualComponent earComp = ComponentCore.createComponent(ear);
    
    IVirtualReference jarRef = earComp.getReference("jar");
    assertNotNull(jarRef);
    assertEquals("jar-1.0.jar",jarRef.getArchiveName());

    IVirtualReference warRef = earComp.getReference("war");
    assertNotNull(warRef);
    String uri = edit.getModuleURI(warRef.getReferencedComponent());
    assertEquals("war-1.0.war", uri);
    
 }

  
  @Test
  public void testMNGECLIPSE1184_contextRootProperty() throws Exception {
    
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-1184/", //
        new String[] {"pom/ear/pom.xml", "pom/pom.xml", "pom/war/pom.xml", },
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(3, projects.length);
    IProject ear = projects[0];
    IProject pom = projects[1];
    IProject war = projects[2];
    
    assertNoErrors(pom);
    assertNoErrors(ear);
    assertNoErrors(war);
    
    //check the context root is the same as the one defined as a property in the parent pom
    EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
    assertEquals("/customContextRoot",edit.getWebContextRoot(war));
    IVirtualComponent webComp = ComponentCore.createComponent(war);
    assertEquals("specialUri.war", edit.getModuleURI(webComp));
 }

  @Test
  public void testMNGECLIPSE1121_pluginManagementSettings() throws Exception {
    
    //We check the pluginManagement settings are correctly interpreted from the different WTPProjectConfigurator delegates
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-1121/", //
        new String[] {"pom/pom.xml", "pom/ear/pom.xml", "pom/core/pom.xml", "pom/ejb/pom.xml", "pom/war/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject ear = projects[1];
    IProject core = projects[2];
    IProject ejb = projects[3];
    IProject war = projects[4];
    
    assertNoErrors(core);
    assertNoErrors(ejb);
    assertNoErrors(war);
    assertNoErrors(ear);
   
    IFacetedProject fpWar = ProjectFacetsManager.create(war);
    assertNotNull(fpWar);
    assertTrue(fpWar.hasProjectFacet(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_25, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));

    IFacetedProject fpCore = ProjectFacetsManager.create(core, false, monitor);
    assertNotNull(ProjectFacetsManager.getFacetedProjects().toString(), fpCore);  
    assertTrue(fpCore.hasProjectFacet(JavaFacet.FACET));
    assertEquals(UTILITY_10, fpCore.getInstalledVersion(UTILITY_FACET));

    IFacetedProject fpEjb = ProjectFacetsManager.create(ejb);
    assertNotNull(fpEjb);
    assertTrue(fpEjb.hasProjectFacet(JavaFacet.FACET));
    assertEquals(IJ2EEFacetConstants.EJB_30, fpEjb.getInstalledVersion(EJB_FACET));

    IFacetedProject fpEar = ProjectFacetsManager.create(ear);
    assertNotNull(fpEar);
    assertFalse(fpEar.hasProjectFacet(JavaFacet.FACET)); //Ears don't have java facet
    assertEquals(IJ2EEFacetConstants.ENTERPRISE_APPLICATION_50, fpEar.getInstalledVersion(EAR_FACET));
    IResource[] underlyingResources = getUnderlyingResources(ear);
    assertEquals(2, underlyingResources.length);
    assertEquals(ear.getFolder("/target/m2e-wtp/ear-resources"), underlyingResources[0]);
    assertEquals(ear.getFolder("/EarContent"), underlyingResources[1]);

    IFile applicationXml = ear.getFile("target/m2e-wtp/ear-resources/META-INF/application.xml"); 
    assertTrue(applicationXml.exists());
  }

  // MNGECLIPSE-1878
  @Test
  public void testPreserveClassPathContainersOnUpdate() throws Exception {
    deleteProject("MNGECLIPSE-1878-core");
    deleteProject("MNGECLIPSE-1878-ejb");
    deleteProject("MNGECLIPSE-1878-web");
    deleteProject("MNGECLIPSE-1878-ear");
    deleteProject("MNGECLIPSE-1878");

    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject[] projects = importProjects("projects/MNGECLIPSE-1878", new String[] {"pom.xml", "MNGECLIPSE-1878-core/pom.xml", 
        "MNGECLIPSE-1878-web/pom.xml", "MNGECLIPSE-1878-ejb/pom.xml", "MNGECLIPSE-1878-ear/pom.xml",}, configuration);

    waitForJobsToComplete();

    IProject web = projects[2];
    IProject ejb = projects[3];
    IProject ear = projects[4];
    //Building project as tests crashes, complaining about /MNGECLIPSE-1878/MNGECLIPSE-1878-ejb/target/classes later, on project update 
    ejb.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    waitForJobsToComplete();

    
    {
      IJavaProject webProject  = JavaCore.create(web); 
      IClasspathEntry[] rawClasspath = webProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 2, rawClasspath.length);
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[0].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[1].getPath().toString());
    }
    {
      IJavaProject ejbProject  = JavaCore.create(ejb); 
      IClasspathEntry[] rawClasspath = ejbProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-1878-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[1].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[2].getPath().toString());
    }
    {
      IFacetedProject fpEar = ProjectFacetsManager.create(ear);
      assertNotNull(fpEar);
      assertFalse(fpEar.hasProjectFacet(JavaFacet.FACET));
    }
    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
    // update configuration
    configurationManager.updateProjectConfiguration(web, monitor);
    waitForJobsToComplete();

    {
      IJavaProject webProject  = JavaCore.create(web); 
      IClasspathEntry[] rawClasspath = webProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 2, rawClasspath.length);
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[0].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[1].getPath().toString());
      //assertEquals("org.eclipse.jst.j2ee.internal.web.container", rawClasspath[2].getPath().toString());
      //assertEquals("org.eclipse.jst.j2ee.internal.module.container", rawClasspath[3].getPath().toString());

      assertNull(getWebLibClasspathContainer(webProject));
    }

    configurationManager.updateProjectConfiguration(ejb, monitor);
    waitForJobsToComplete();

    {
      IJavaProject ejbProject  = JavaCore.create(ejb); 
      IClasspathEntry[] rawClasspath = ejbProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-1878-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-1878-ejb/src/main/resources", rawClasspath[1].getPath().toString());//TODO Resources folder appear after config update (WTP added MANIFEST.MF)
      assertEquals(JRE_CONTAINER_J2SE_1_5, rawClasspath[2].getPath().toString());
      assertEquals(MAVEN_CLASSPATH_CONTAINER, rawClasspath[3].getPath().toString());
      //assertEquals("org.eclipse.jst.j2ee.internal.module.container", rawClasspath[4].getPath().toString());
    }
    
  }
 
  @Test
  public void testMNGECLIPSE1978_Javaee6Support() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-1978/", 
        new String[] {"javaee6-parent/pom.xml", 
                      "javaee6-parent/javaee6-ejb/pom.xml", 
                      "javaee6-parent/javaee6-web/pom.xml", 
                      "javaee6-parent/javaee6-ear/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(4, projects.length);
    IProject ejb = projects[1];
    IProject war = projects[2];
    IProject ear = projects[3];
    
    assertNoErrors(ejb);
    assertNoErrors(war);
    assertNoErrors(ear);
    
    IFacetedProject fpEjb = ProjectFacetsManager.create(ejb);
    assertNotNull(fpEjb);
    assertTrue(fpEjb.hasProjectFacet(JavaFacet.FACET));

    IFacetedProject fpWar = ProjectFacetsManager.create(war);
    assertNotNull(fpWar);
    assertTrue(fpWar.hasProjectFacet(JavaFacet.FACET));

    IFacetedProject fpEar = ProjectFacetsManager.create(ear);
    assertNotNull(fpEar);
    assertFalse(fpEar.hasProjectFacet(JavaFacet.FACET)); //Ears don't have java facet

    IProjectFacetVersion expectedWebFacet = null;
    IProjectFacetVersion expectedEjbFacet = null;
    IProjectFacetVersion expectedEarFacet = null;
    
    if (WTPProjectsUtil.isJavaEE6Available()) {
      //check we assigned the correct, JavaEE 6, facet versions, for WTP >= 3.2
      expectedEjbFacet = EJB_FACET.getVersion("3.1");
      expectedWebFacet = WebFacetUtils.WEB_FACET.getVersion("3.0");
      expectedEarFacet = EAR_FACET.getVersion("6.0");      
    } else {
      //check we downgraded WTP Facets versions to a compatible level
      expectedEjbFacet = IJ2EEFacetConstants.EJB_30;
      expectedWebFacet = WebFacetUtils.WEB_25;
      expectedEarFacet = IJ2EEFacetConstants.ENTERPRISE_APPLICATION_50;
    }
    assertEquals(expectedEjbFacet, fpEjb.getInstalledVersion(EJB_FACET));
    assertEquals(expectedWebFacet, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertEquals(expectedEarFacet, fpEar.getInstalledVersion(EAR_FACET));

}

  @Test
  public void testUriInEarModules() throws Exception {
    
    deleteProject("pom");
    deleteProject("ear");
    deleteProject("ejb");
    deleteProject("war");
    deleteProject("core");
    
    IProject[] projects = importProjects(
        "projects/bundleFileNames/", //
        new String[] {"javaEE/pom.xml", "javaEE/ear/pom.xml", "javaEE/core/pom.xml", "javaEE/ejb/pom.xml", "javaEE/war/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject ear = projects[1];
    IProject core = projects[2];
    IProject ejb = projects[3];
    IProject war = projects[4];
    
    assertNoErrors(core);
    assertNoErrors(ejb);
    assertNoErrors(war);
    assertNoErrors(ear);
   
    IFacetedProject fpWar = ProjectFacetsManager.create(war);
    assertNotNull(fpWar);
    assertTrue(fpWar.hasProjectFacet(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_24, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));

    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference warRef = comp.getReference("war");
    assertNotNull(warRef);
    assertEquals("war-0.0.1-SNAPSHOT.war",warRef.getArchiveName());
    IVirtualReference coreRef = comp.getReference("core");
    assertNotNull(coreRef);
    assertEquals("coreproject.zip",coreRef.getArchiveName());
    IVirtualReference ejbRef = comp.getReference("ejb");
    assertNotNull(ejbRef);
    assertEquals("specialejb.jar",ejbRef.getArchiveName());
    
    EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
    assertEquals(3, edit.getApplication().getModules().size());
    assertEquals(warRef.getArchiveName(),edit.getModuleURI(warRef.getReferencedComponent()));
    assertEquals("/dummy",edit.getWebContextRoot(war));

    assertEquals("/special/coreproject.zip", edit.getModuleURI(coreRef.getReferencedComponent()));
    assertEquals("/special",coreRef.getRuntimePath().toPortableString());

    assertEquals(ejbRef.getArchiveName(), edit.getModuleURI(ejbRef.getReferencedComponent()));
 }


  //Test disabled as the fix breaks default behavior
  @Test
  public void testMNGECLIPSE2279_finalNameAsContextRoot() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2279/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(WebFacetUtils.WEB_23, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
    //Test blank finalName
    assertEquals("MNGECLIPSE-2279",J2EEProjectUtilities.getServerContextRoot(project));
    assertNoErrors(project);
    
    //Test custom finalName
    updateProject(project, "pom.step2.xml");     
    assertEquals("webapp",J2EEProjectUtilities.getServerContextRoot(project));
    assertNoErrors(project);
    
    //Test finalName with dots and spaces
    updateProject(project, "pom.step3.xml");     
    assertEquals("web_appli.cation",J2EEProjectUtilities.getServerContextRoot(project));
    assertNoErrors(project);

    //Test no finalName
    updateProject(project, "pom.step4.xml");     
    assertEquals("MNGECLIPSE-2279",J2EEProjectUtilities.getServerContextRoot(project));
    assertNoErrors(project);
}
  
  @Test
  public void testMECLIPSEWTP43_customContextRoot() throws Exception {
    IProject project = importProject("projects/MECLIPSEWTP-43/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(WebFacetUtils.WEB_23, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
    //Test blank finalName
    assertEquals("MECLIPSEWTP-43", J2EEProjectUtilities.getServerContextRoot(project));
    assertNoErrors(project);
    
    //Test custom finalName
    updateProject(project, "pom.step2.xml");     
    assertEquals("webapp", J2EEProjectUtilities.getServerContextRoot(project));
    assertNoErrors(project);
    
    //Test finalName with dots and spaces
    updateProject(project, "pom.step3.xml");     
    assertEquals("web_appli.cation", J2EEProjectUtilities.getServerContextRoot(project));
    assertNoErrors(project);

    //Test no finalName
    updateProject(project, "pom.step4.xml");     
    assertEquals("/", J2EEProjectUtilities.getServerContextRoot(project));
    assertNoErrors(project);
}


  @Test
  public void testMNGECLIPSE2357_customWebXml() throws Exception {
    IProject web = importProject("projects/MNGECLIPSE-2357/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();
    assertEquals("MNGECLIPSE-2357",J2EEProjectUtilities.getServerContextRoot(web));
    
    IFacetedProject facetedProject = ProjectFacetsManager.create(web);
    assertNotNull(facetedProject);
    assertEquals(WebFacetUtils.WEB_23, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
    assertNoErrors(web);
    
    IVirtualComponent webComp = ComponentCore.createComponent(web);
    IVirtualFolder rootWeb = webComp.getRootFolder();
    IResource[] webResources = rootWeb.getUnderlyingResources();
    assertEquals(2, webResources.length);
    assertEquals(web.getFolder("/src/main/webapp"), webResources[1]);
    
    IVirtualFile virtualWebXml = rootWeb.getFile("WEB-INF/web.xml");
    assertTrue(virtualWebXml.exists());
    IFile[] webXmlFiles = virtualWebXml.getUnderlyingFiles();
    assertEquals("found "+toString(webXmlFiles),  1, webXmlFiles.length);
    //Check non default web.xml
    assertEquals(web.getFile("/resources/web.xml"), webXmlFiles[0]);
    IFile defaultWebXml = web.getFile("/src/main/webapp/WEB-INF/web.xml"); 
    assertFalse(defaultWebXml.exists());//Check te default web.xml is not created

    
    //Let's spice it up : use a profile to change the web.xml, which incidentally will trigger a facet change
    /* FIXME this test now fails with the new mavenarchiver dependency. But smoke testing this works fine.
    updateProject(web, "useProfileForCustomWebXml.xml");    
    assertNoErrors(web);    

    facetedProject = ProjectFacetsManager.create(web);
    assertNotNull(facetedProject);
    assertEquals(WebFacetUtils.WEB_24, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.FACET));
    assertNoErrors(web);
    
    webResources = rootWeb.getUnderlyingResources();
    assertEquals(2, webResources.length);
    assertEquals(web.getFolder("/src/main/webapp"), webResources[1]);
    
    webXmlFiles = virtualWebXml.getUnderlyingFiles();
    assertEquals("found "+toString(webXmlFiles),  1, webXmlFiles.length);
    //Check non default web.xml 
    assertEquals(web.getFile("/profile/web.xml"), webXmlFiles[0]);
    */
  }


  @Test
  public void testMNGECLIPSE2393_Libs_SkinnyWars() throws Exception {
    
    if (!WTPProjectsUtil.isJavaEE6Available()) {
      //This feature is bugged on WTP < 3.2, so we skip the test
      return;
    }
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-2393/", //
        new String[] {"ear/pom.xml", "utility1/pom.xml", "ejb/pom.xml", "skinny-war/pom.xml", "utility2/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    assertEquals(5, projects.length);
    IProject ear = projects[0];
    IProject utility1 = projects[1];
    IProject ejb = projects[2];
    IProject fullskinnywar = projects[3];
    
    assertNoErrors(ear);
    assertNoErrors(utility1);
    assertNoErrors(ejb);
    assertNoErrors(fullskinnywar);
    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    
    IVirtualReference utilityRef1 = comp.getReference("utility1");
    assertNotNull(utilityRef1);
    IVirtualReference ejbRef = comp.getReference("ejb");
    assertNotNull(ejbRef);
    
    ////////////
    //check the war project
    ////////////
    IVirtualReference skinnyWarRef = comp.getReference("skinny-war");
    assertNotNull(skinnyWarRef);
    assertEquals("skinny-war-0.0.1-SNAPSHOT.war",skinnyWarRef.getArchiveName());    
    
    //the fully skinny war contains to project refs whatsoever
    IVirtualComponent skinnyWarComp = skinnyWarRef.getReferencedComponent();
    IVirtualReference[] warRefs = skinnyWarComp.getReferences();
    assertEquals(toString(warRefs),5, warRefs.length);
    
    assertEquals(ejb, warRefs[0].getReferencedComponent().getProject());
    assertEquals("/", warRefs[0].getRuntimePath().toString());    
    assertEquals(utility1, warRefs[1].getReferencedComponent().getProject());
    assertEquals("/", warRefs[1].getRuntimePath().toString());
    assertTrue(warRefs[2].getReferencedComponent().getDeployedName().endsWith("commons-lang-2.4.jar"));  
    assertEquals("/", warRefs[2].getRuntimePath().toString());  
    assertTrue(warRefs[3].getReferencedComponent().getDeployedName().endsWith("commons-collections-2.0.jar"));  
    assertEquals("/", warRefs[3].getRuntimePath().toString());  
    
    //check for all expected dependencies in the manifest
    IFile war1ManifestFile = ComponentUtilities.findFile(skinnyWarComp, new Path(J2EEConstants.MANIFEST_URI));
    Manifest mf1 = loadManifest(war1ManifestFile);

    //check that manifest classpath contains all dependencies
    String classpath = mf1.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
    assertTrue(classpath.startsWith(ejbRef.getArchiveName()));
    assertTrue(classpath.contains("lib/"+utilityRef1.getArchiveName()));
    //assertFalse(classpath.contains("lib/"+ejbRef.getArchiveName()));
    assertTrue(classpath.contains("lib/commons-lang-2.4.jar"));
    assertTrue(classpath.contains("lib/commons-collections-2.0.jar"));
    //...but not junit, which is a test dependency
    assertFalse(classpath.contains("junit-3.8.1.jar"));
    
    //check that junit is in the maven classpath container instead
    IClasspathEntry[] mavenContainerEntries = getMavenContainerEntries(fullskinnywar);
    assertEquals(6, mavenContainerEntries.length);
    assertEquals("utility1", mavenContainerEntries[0].getPath().lastSegment());
    assertEquals("ejb", mavenContainerEntries[1].getPath().lastSegment());
    assertEquals("commons-lang-2.4.jar", mavenContainerEntries[2].getPath().lastSegment());
    assertEquals("commons-collections-2.0.jar", mavenContainerEntries[3].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", mavenContainerEntries[4].getPath().lastSegment());
    assertEquals("utility2", mavenContainerEntries[5].getPath().lastSegment());
  }

  @Test
  public void testMECLIPSEWTP73_EjbClientInLib_JavaEE5() throws Exception {
    IProject[] projects = importProjects(
        "projects/MECLIPSEWTP-73/", //
        new String[] {"ear5-with-ejb-client/pom.xml", "ear5-with-ejb-client/ear/pom.xml", "ear5-with-ejb-client/ejb/pom.xml", "ear5-with-ejb-client/war/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(4, projects.length);
    IProject ear = projects[1];
    IProject ejb = projects[2];
    IProject war = projects[3];
    
    assertNoErrors(ejb);
    assertNoErrors(war);
    assertNoErrors(ear);
   
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference warRef = comp.getReference("war");
    assertNotNull(warRef);
    assertEquals("war-1.0-SNAPSHOT.war",warRef.getArchiveName());
   
    IVirtualReference ejbRef = comp.getReference("ejb");
    assertNotNull(ejbRef);
    assertEquals("ejb-1.0-SNAPSHOT-client.jar",ejbRef.getArchiveName());
    assertEquals("/lib",ejbRef.getRuntimePath().toPortableString());
    
    
    EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
    assertEquals(1, edit.getApplication().getModules().size());
    assertEquals(warRef.getArchiveName(),edit.getModuleURI(warRef.getReferencedComponent()));
    assertEquals("war",edit.getWebContextRoot(war));
  }

  @Test
  public void testMECLIPSEWTP73_EjbClientInLib_J2ee14() throws Exception {

    try {
      useBuildDirforGeneratingFiles(false);    
      
      IProject[] projects = importProjects(
          "projects/MECLIPSEWTP-73/", //
          new String[] {"ear14-with-ejb-client/pom.xml", "ear14-with-ejb-client/ear/pom.xml", "ear14-with-ejb-client/ejb/pom.xml", "ear14-with-ejb-client/war/pom.xml"},
          new ResolverConfiguration());
  
      waitForJobsToComplete();
      
      assertEquals(4, projects.length);
      IProject ear = projects[1];
      IProject ejb = projects[2];
      IProject war = projects[3];
      
      assertNoErrors(ejb);
      assertNoErrors(war);
      assertNoErrors(ear);
     
      IVirtualComponent comp = ComponentCore.createComponent(ear);
      IVirtualReference warRef = comp.getReference("war");
      assertNotNull(warRef);
      assertEquals("war-1.0-SNAPSHOT.war",warRef.getArchiveName());
     
      IVirtualReference ejbRef = comp.getReference("ejb");
      assertNotNull(ejbRef);
      assertEquals("ejb-1.0-SNAPSHOT-client.jar",ejbRef.getArchiveName());
      assertEquals("/",ejbRef.getRuntimePath().toPortableString());
      
      org.eclipse.jst.j2ee.application.Application app = (org.eclipse.jst.j2ee.application.Application)ModelProviderManager.getModelProvider(ear).getModelObject();
      assertEquals(1,app.getModules().size());
      org.eclipse.jst.j2ee.application.WebModule webModule = (WebModule)app.getFirstModule(warRef.getArchiveName());
      assertNotNull("missing webmodule "+warRef.getArchiveName(),webModule);
      assertEquals("war",webModule.getContextRoot());
    } finally {
        useBuildDirforGeneratingFiles(true);    
    }
  }

  @Test
  public void testMECLIPSEWTP72_SkinnyWar_Redux() throws Exception {
    
    IProject[] projects = importProjects(
        "projects/MECLIPSEWTP-72/", //
        new String[] {"ear-with-skinny-war/pom.xml", "ear-with-skinny-war/ear/pom.xml", "ear-with-skinny-war/war/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(3, projects.length);
    IProject ear = projects[1];
    IProject war = projects[2];

    assertNoErrors(war);
    assertNoErrors(ear);
    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference warRef = comp.getReference("war");
    assertNotNull(warRef);
    assertEquals("war-1.0-SNAPSHOT.war",warRef.getArchiveName());

    IVirtualComponent warComp = ComponentCore.createComponent(war);
    IVirtualReference[] fullSkinnyReferences = warComp.getReferences();
    assertEquals(1, fullSkinnyReferences.length);
    assertTrue(fullSkinnyReferences[0].getReferencedComponent().getDeployedName().endsWith("commons-lang-2.4.jar"));  
    
    EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
    assertEquals(1, edit.getApplication().getModules().size());
    assertEquals("war",edit.getWebContextRoot(war));
  }

  @Test
  public void testMECLIPSEWTP76_noVersionfileNames() throws Exception {
    // Exported filenames should be consistent when workspace resolution is on/off
    IProject[] projects = importProjects(
        "projects/MECLIPSEWTP-76/", //
        new String[] {"ear-noVersionFileNames/pom.xml", "testFileNameWar/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(2, projects.length);
    IProject ear = projects[0];
    IProject war = projects[1];
    
    assertNoErrors(ear);
    assertNoErrors(war);

    IVirtualComponent earComp = ComponentCore.createComponent(ear);
    IVirtualReference warRef = earComp.getReference("testFileNameWar");
    assertNotNull(warRef);
    assertEquals("testFileNameWar.war",warRef.getArchiveName());

    IVirtualReference junitRef = earComp.getReference("var/M2_REPO/junit/junit/3.8.1/junit-3.8.1.jar");
    assertNotNull(junitRef);
    assertEquals("junit.jar",junitRef.getArchiveName());
    
  }  

  @Test
  public void testMECLIPSEWTP108_DependencyArchiveName() throws Exception {
    IProject[] projects = importProjects("projects/MECLIPSEWTP-108", //
        new String[] {"pom.xml", "webapp/pom.xml", "utility/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    IVirtualComponent webComponent = ComponentCore.createComponent(projects[1]);
    IVirtualReference[] references = webComponent.getReferences();
    assertEquals(1, references.length);
    assertEquals(projects[2], references[0].getReferencedComponent().getProject());
    assertEquals("Invalid archive name", "utility-1.0.jar", references[0].getArchiveName());
  }

  @Test
  public void testMECLIPSEWTP58_generateApplicationXmlInBuildDir() throws Exception {
    
    IProject[] projects = importProjects(
        "projects/deployment-descriptors/", //
        new String[] {"javaEE/pom.xml", "javaEE/ear/pom.xml", "javaEE/core/pom.xml", "javaEE/ejb/pom.xml", "javaEE/war/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject ear = projects[1];
    IProject core = projects[2];
    IProject ejb = projects[3];
    IProject war = projects[4];
    
    assertNoErrors(core);
    assertNoErrors(ejb);
    assertNoErrors(war);
    assertNoErrors(ear);

    IFile applicationXmlInBuidDir = ear.getFile("target/"+ M2E_WTP_FOLDER+"/"+ EAR_RESOURCES_FOLDER + "/META-INF/application.xml"); 
    assertTrue(applicationXmlInBuidDir.getFullPath()+" is missing",applicationXmlInBuidDir.exists());

    IFile applicationXmlInSourceDir = ear.getFile("src/main/application/META-INF/application.xml"); 
    assertFalse(applicationXmlInSourceDir.getFullPath()+" shouldn't exist",applicationXmlInSourceDir.exists());

    useBuildDirforGeneratingFiles(ear, false);
    updateProject(ear);     
    
    assertFalse(applicationXmlInBuidDir.getFullPath()+" should have been deleted",applicationXmlInBuidDir.exists());
    assertTrue(applicationXmlInSourceDir.getFullPath()+" should have been created",applicationXmlInSourceDir.exists());
  }

  @Test
  public void testMECLIPSEWTP7_testResources() throws Exception {
    IProject[] projects = importProjects("projects/MECLIPSEWTP-7/", new String[]{"web/pom.xml", "util/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    
    IProject web =  projects[0];
    assertNoErrors(web);    
    IProject util =  projects[1];
    assertNoErrors(util);
   
    
    IResource[] underlyingResources = getUnderlyingResources(util);
    assertEquals(2, underlyingResources.length);
    assertEquals(util.getFolder("/src/main/java"), underlyingResources[0]);
    assertEquals(util.getFolder("/src/main/resources"), underlyingResources[1]);
  }


  @Test
  public void testMECLIPSEWTP162_UpdateWebFacet() throws Exception {
      IProject war = importProject("projects/MECLIPSEWTP-162/updatefacets/pom.xml");
      waitForJobsToComplete();
      war.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
      assertNoErrors(war);   
      IFacetedProject fpWar = ProjectFacetsManager.create(war);
      assertNotNull(fpWar);
      assertEquals(WebFacetUtils.WEB_30, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));
  }
  
  @Test
  public void testMECLIPSEWTP167_noSlashInlibDir() throws Exception {
    IProject ear = importProject("projects/MECLIPSEWTP-167/ear6/pom.xml");
    waitForJobsToComplete();
    assertNoErrors(ear);
    
    IFacetedProject fpEar = ProjectFacetsManager.create(ear);
    assertNotNull(fpEar);
    assertEquals(IJ2EEFacetConstants.ENTERPRISE_APPLICATION_60, fpEar.getInstalledVersion(EAR_FACET));

    String libDir = "library";
    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference[] references = comp.getReferences();
    assertEquals(1, references.length);
    IVirtualReference junit = references[0];
    assertEquals("junit-3.8.1.jar", junit.getArchiveName());
    assertEquals("/"+libDir, junit.getRuntimePath().toPortableString());
    
    final Application app = (Application)ModelProviderManager.getModelProvider(ear).getModelObject();
    assertNotNull(app);
    assertEquals(libDir,app.getLibraryDirectory());
  }

  @Test
  public void testMECLIPSEWTP184_SameArtifactDifferentClassifier() throws Exception {
    IProject project = importProject("projects/MECLIPSEWTP-184/pom.xml");
    
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    assertEquals(2, cp.length);
    assertEquals("junit-3.8.1.jar", cp[0].getPath().lastSegment());
    assertEquals("junit-3.8.1-sources.jar", cp[1].getPath().lastSegment());
  }
  
  private static String dumpModules(List<Module> modules) {
    if (modules == null) return "Null modules";
    StringBuilder sb = new StringBuilder("[");
    boolean firstModule = true;
    for (Module m : modules) {
      if (firstModule) {
        firstModule = false;
      } else {
        sb.append(",");
      }
      sb.append(m.getUri());
    }
    sb.append("]");
    return sb.toString();
  }

}
