
package org.maven.ide.eclipse.wtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
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
    
    InputStream is = null;
    try {
      is = manifestFile.getContents();
      String manifest = IOUtil.toString(is);
      String url = "url: http://www.eclipse.org";
      String mode = "mode: development";
      String createdBy = "Created-By: Maven Integration for Eclipse";
      assertContains(url, manifest);
      assertContains(mode, manifest);
      assertContains(createdBy, manifest);
    } finally {
      IOUtil.close(is);
    }
  }

  
}
