package org.maven.ide.eclipse.wtp;

import java.io.InputStream;
import java.util.Properties;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

public class WebResourceFilteringTest extends AbstractWTPTestCase {
  
  private static String FILTERED_FOLDER_NAME = "target/m2eclipse-wtp/webresources"; 
  
  @Test
  public void testMECLIPSE22_webfiltering() throws Exception {
    IProject web = importProject("projects/WebResourceFiltering/webfiltering/pom.xml");
    web.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    assertNoErrors(web);
    IFolder filteredFolder = web.getFolder(FILTERED_FOLDER_NAME);
    assertTrue("Filtered folder doesn't exist", filteredFolder.exists());
    IFile indexHtml = filteredFolder.getFile("index.html");
    assertTrue("index.html is missing",indexHtml.exists());
    String index = getAsString(indexHtml);
    assertTrue("${phrase} property from pom was not interpolated", index.contains("<title>m2e rocks!</title>"));
    assertTrue("${project.artifactId} from pom was not interpolated", index.contains("<body>Welcome @ webfiltering</body>"));

    IFile indexProp = filteredFolder.getFile("index.properties");
    assertTrue("index.properties is missing",indexProp.exists());
    Properties props = new Properties();
    InputStream ins = indexProp.getContents();
    try {
      props.load(ins);  
    } finally {
      IOUtil.close(ins);
    }
    assertEquals("${custom.version} from webfilter.properties was not interpolated","0.0.1-SNAPSHOT",props.get("app.version"));

    IFile webXml = filteredFolder.getFile("WEB-INF/web.xml");
    assertTrue("WEB-INF/web.xml is missing",indexProp.exists());
    String xml = getAsString(webXml);
    assertTrue("${welcome.page} from webfilter.properties was not interpolated", xml.contains("<welcome-file>index.html</welcome-file>"));

    IFile ignored = filteredFolder.getFile("ignoreme.txt");
    assertFalse("ignoreme.txt should be excluded",ignored.exists());

  }
}
