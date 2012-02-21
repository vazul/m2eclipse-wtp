
package org.maven.ide.eclipse.wtp;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.junit.Test;

@SuppressWarnings("restriction")
public class ManifestConfiguratorTest extends AbstractWTPTestCase {
  
  @Test
  public void testMECLIPSEWTP66_unWantedManifests() throws Exception {

    IProject[] projects = importProjects("projects/manifests/MECLIPSEWTP-66/", 
        new String[]{"pom.xml", 
                     "jar/pom.xml", 
                     "jar2/pom.xml", 
                     "jar3/pom.xml", 
                     "jar4/pom.xml",
                     "war/pom.xml"}, 
        new ResolverConfiguration());

    //10 to 30% of my test runs, jar2 is not updated 'cause
    //The worker thread is gone like : 
    //Worker thread ended job: Updating Maven Dependencies(76), but still holds rule: ThreadJob(Updating Maven Dependencies(76),[R/,])
    //Let's add an ugly delay, see if it improves the situation
    long delay = 5000;
    System.err.println("Waiting an extra "+delay + " ms");
    Thread.sleep(delay);
    waitForJobsToComplete();
   
    String expectedManifest = "target/classes/META-INF/MANIFEST.MF";
    IProject jar =  projects[1];
    assertNoErrors(jar);    
    assertMissingMetaInf(jar);
    assertTrue(jar.getFile(expectedManifest).exists());;
    
    IProject jar2 =  projects[2];
    assertNoErrors(jar2);
    assertMissingMetaInf(jar2);
    assertTrue(jar2.getFile(expectedManifest).exists());;
    
    IProject jar3 =  projects[3];
    assertNoErrors(jar3);    
    assertMissingMetaInf(jar3);
    assertTrue(jar3.getFile(expectedManifest).exists());;

    //Check the existing folder hasn't been deleted
    IProject jar4 =  projects[4];
    assertNoErrors(jar4);
    IFolder metaInf = jar4.getFolder("src/main/resources/META-INF/");
    assertTrue(metaInf.exists());
    //But no Manifest should be there
    assertFalse(metaInf.getFile("MANIFEST.MF").exists());
    assertTrue(jar4.getFile(expectedManifest).exists());;

    IProject war =  projects[5];
    assertNoErrors(war);
    assertMissingMetaInf(war);
    
  }
  
  @Test
  public void testMECLIPSEWTP45_JarManifest() throws Exception {

    IProject[] projects = importProjects("projects/manifests/MECLIPSEWTP-45/", 
        new String[]{"pom.xml", 
                    "jar/pom.xml", 
                    "war/pom.xml"}, 
        new ResolverConfiguration());
    waitForJobsToComplete();
    
    IProject jar =  projects[1];
    jar.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    assertNoErrors(jar);    
    IProject war =  projects[2];
    assertNoErrors(war);
    
    IFile manifestFile = jar.getFile("target/classes/META-INF/MANIFEST.MF");
    assertTrue("The manifest is missing", manifestFile.exists());
    String manifest =getAsString(manifestFile);
    String url = "url: http://www.eclipse.org";
    String mode = "mode: development";
    String createdBy = "Created-By: Maven Integration for Eclipse";
    assertContains(url, manifest);
    assertContains(mode, manifest);
    assertContains(createdBy, manifest);
    assertNotContains("Class-Path", manifest);
    
    
    //Check manifest is updated on config change
    updateProject(jar, "pom2.xml");
    assertNoErrors(jar);
    manifest =getAsString(manifestFile);
    createdBy = "Created-By: Some dude"; 
    assertContains(createdBy, manifest);//Created-By can be overridden manually
    assertNotContains("Class-Path", manifest);//Nothing to add to the classpath, so it's not added
    assertContains(url, manifest);
    assertContains(mode, manifest);
    assertContains("Implementation-Title: jar", manifest);
    assertContains("Specification-Version: 0.0.1-SNAPSHOT", manifest);
    assertContains(mode, manifest);
    
    //Change the classpath by changing the junit scope
    updateProject(jar, "pom3.xml");
    assertNoErrors(jar);
    manifest =getAsString(manifestFile);
    assertContains("Class-Path: junit-3.8.1.jar", manifest);
  }

  @Test
  public void testMECLIPSEWTP63_EjbManifest() throws Exception {
    IProject[] projects = importProjects(
        "projects/manifests/MECLIPSEWTP-63/", //
        new String[] {"javaEE/pom.xml", 
                      "javaEE/ear/pom.xml", 
                      "javaEE/core/pom.xml", 
                      "javaEE/ejb/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    IProject ear =  projects[1];
    assertNoErrors(ear);    
    IProject jar =  projects[2];
    assertNoErrors(jar);    
    IProject ejb =  projects[3];
    assertNoErrors(ejb);      
    
    IFile manifestFile = ejb.getFile("target/classes/META-INF/MANIFEST.MF");
    String manifest =getAsString(manifestFile);
    assertContains("Class-Path: lib/log4j-1.2.13.jar lib/core-0.0.1-SNAPSHOT.jar lib/junit", manifest);
    
    manifestFile = ejb.getFile("src/main/resources/META-INF/MANIFEST.MF");
    assertFalse(manifestFile.exists());
  }
  
  @Test
  public void testManifestInEarAndConnector() throws Exception {
    IProject[] projects = importProjects(
        "projects/manifests/ear-connector/", //
        new String[] { 
                      "ear/pom.xml", 
                      "core/pom.xml", 
                      "rar/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    IProject ear =  projects[0];
    assertNoErrors(ear);    
    IProject jar =  projects[1];
    assertNoErrors(jar);    
    IProject rar =  projects[2];
    assertNoErrors(rar);      
    
    IFile rarManifestFile = rar.getFile("target/classes/META-INF/MANIFEST.MF");
    String manifest =getAsString(rarManifestFile);
    assertContains("Class-Path: lib/commons-collections-2.0.jar lib/core-0.0.1-SNAPSHOT.ja", manifest);
    //For some reason, CR/LF not detected in String.contains, so we split the assert
    assertContains(" r lib/junit-3.8.1.jar", manifest);
    
    rarManifestFile = rar.getFile("src/main/rar/META-INF/MANIFEST.MF");
    assertFalse(rarManifestFile.exists());

    IFile earManifestFile = ear.getFile("target/m2e-wtp/ear-resources/META-INF/MANIFEST.MF");
    String earManifest =getAsString(earManifestFile);
    assertNotContains("Class-Path:", earManifest);
    String createdBy = "Created-By: Maven Integration for Eclipse";
    assertContains(createdBy, manifest);
    
    earManifestFile = rar.getFile("src/main/application/META-INF/MANIFEST.MF");
    assertFalse(earManifestFile.exists());
  }
  

  @Test
  public void testMECLIPSEWTP136_WarManifestInSource() throws Exception {
    useBuildDirforGeneratingFiles(false);
    try {
      IProject project = importProject("projects/MECLIPSEWTP-136/war1/pom.xml");
      project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
      waitForJobsToComplete();
      List<IMarker> markers = findErrorMarkers(project);
      assertTrue("Should not have any markers",markers.isEmpty());
      
      assertFalse(project.exists(new Path("/target/m2e-wtp/web-resources/META-INF/MANIFEST.MF")));
      assertFalse(project.exists(new Path("/target/m2e-wtp/web-resources/META-INF/maven/")));
      
      IVirtualComponent warComponent = ComponentCore.createComponent(project);
      IVirtualFolder rootwar = warComponent.getRootFolder();
      IResource[] warResources = rootwar.getUnderlyingResources();
      assertEquals(1, warResources.length);
      assertEquals(project.getFolder("/src/main/webapp"), warResources[0]);
      
      assertTrue(project.exists(new Path("/src/main/webapp/META-INF/MANIFEST.MF")));
      assertTrue(project.exists(new Path("/src/main/webapp/META-INF/maven/")));
      
    } finally {
      useBuildDirforGeneratingFiles(true);
    }
    
  }

  @Test
  public void testMECLIPSEWTP136_OverrideArchiverSettingWhenFiltering() throws Exception {
    useBuildDirforGeneratingFiles(false);
    try {
      IProject project = importProject("projects/MECLIPSEWTP-136/war2/pom.xml");
      project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
      waitForJobsToComplete();
      List<IMarker> markers = findMarkers(project, IMarker.SEVERITY_WARNING);
      assertFalse("Should have some markers",markers.isEmpty());
      assertHasMarker(Messages.markers_mavenarchiver_output_settings_ignored_warning, markers);
      
      assertTrue(project.exists(new Path("/target/m2e-wtp/web-resources/META-INF/MANIFEST.MF")));
      assertTrue(project.exists(new Path("/target/m2e-wtp/web-resources/META-INF/maven/")));
      
      IVirtualComponent warComponent = ComponentCore.createComponent(project);
      IVirtualFolder rootwar = warComponent.getRootFolder();
      IResource[] warResources = rootwar.getUnderlyingResources();
      assertEquals(2, warResources.length);
      assertEquals(project.getFolder("/src/main/webapp"), warResources[1]);
      
      assertFalse(project.exists(new Path("/src/main/webapp/META-INF/MANIFEST.MF")));
      assertFalse(project.exists(new Path("/src/main/webapp/META-INF/maven/")));
      
    } finally {
      useBuildDirforGeneratingFiles(true);
    }
    
  }

  @Test
  public void testProvidedManifest() throws Exception {
    IProject ejb = importProject("projects/manifests/ejb-provided-manifest/pom.xml");
    waitForJobsToComplete();
    
    IFile manifestFile = ejb.getFile("src/main/resources/META-INF/MANIFEST.MF");
    assertTrue("The manifest was deleted", manifestFile.exists());
    
    IFile generatedManifestFile = ejb.getFile("target/classes/META-INF/MANIFEST.MF");
    assertTrue("The generated manifest is missing", generatedManifestFile.exists());
    
    String manifest =getAsString(generatedManifestFile);
    String createdBy = "Created-By: Maven Integration for Eclipse";
    assertContains(createdBy, manifest);
    assertContains("Built-By: You know who", manifest);
    assertContains("Implementation-Title: ejb-provided-manifest", manifest);
    assertContains("Class-Path: custom.jar", manifest);    
  }

  
  protected void assertMissingMetaInf(IProject project) {
    String metaInf= "META-INF";
    assertMissingFolder(project.getFolder(metaInf));
    assertMissingFolder(project.getFolder("src/main/java/"+metaInf));
    assertMissingFolder(project.getFolder("src/main/resources/" +metaInf));
    assertMissingFolder(project.getFolder("src/main/webapp/" +metaInf));
  }
  
  protected void assertMissingFolder(IFolder folder) {
    assertFalse(folder + " should not exist ", folder.exists());
  }
  
  
  
}
