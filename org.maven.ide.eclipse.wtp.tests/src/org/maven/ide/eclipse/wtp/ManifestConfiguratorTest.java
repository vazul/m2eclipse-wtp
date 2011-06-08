
package org.maven.ide.eclipse.wtp;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.junit.Test;


public class ManifestConfiguratorTest extends AbstractWTPTestCase {

  @Test
  public void testMECLIPSEWTP45_JarManifest() throws Exception {

    IProject[] projects = importProjects("projects/manifests/MECLIPSEWTP-45/", new String[]{"pom.xml", "jar/pom.xml", "war/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
   
    IProject jar =  projects[1];
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

  
}
