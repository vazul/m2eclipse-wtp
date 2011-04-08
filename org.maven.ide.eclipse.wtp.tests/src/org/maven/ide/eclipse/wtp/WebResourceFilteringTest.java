package org.maven.ide.eclipse.wtp;

import java.io.InputStream;
import java.util.Properties;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

public class WebResourceFilteringTest extends AbstractWTPTestCase {
  
  private static String FILTERED_FOLDER_NAME = "target/m2eclipse-wtp/webresources"; 
  
  public void testMECLIPSE22_webfiltering() throws Exception {
    IProject web = importProject("projects/WebResourceFiltering/webfiltering/pom.xml");
    web.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    assertNoErrors(web);
    IFolder filteredFolder = web.getFolder(FILTERED_FOLDER_NAME);
    assertTrue("Filtered folder doesn't exist", filteredFolder.exists());
    
    //Check all the files are correctly filtered
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
    assertTrue(webXml.getName() +" is missing",webXml.exists());
    String xml = getAsString(webXml);
    assertTrue("${welcome.page} from webfilter.properties was not interpolated", xml.contains("<welcome-file>index.html</welcome-file>"));

    IFile ignored = filteredFolder.getFile("ignoreme.txt");
    assertFalse("ignoreme.txt should be excluded",ignored.exists());
    
    //Check the filtered folder will be deployed first
    IVirtualComponent webComponent = ComponentCore.createComponent(web);
    IVirtualFolder rootwar = webComponent.getRootFolder();
    IResource[] warResources = rootwar.getUnderlyingResources();
    assertEquals(2, warResources.length);
    assertEquals(web.getFolder("/target/m2eclipse-wtp/webresources"), warResources[0]);
    assertEquals(web.getFolder("/src/main/webapp"), warResources[1]);
    
    String newFilter = "welcome.page=default.jsp\ncustom.version=1.0";
    IFile filterFile = web.getFile("src/main/filters/webfilter.properties");
    FileUtils.fileWrite(filterFile.getLocation().toOSString(), newFilter);
    filterFile.refreshLocal(1, new NullProgressMonitor());
    web.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    
    indexProp = filteredFolder.getFile("index.properties");
    assertTrue("index.properties is missing",indexProp.exists());
    props = new Properties();
    ins = indexProp.getContents();
    try {
      props.load(ins);  
    } finally {
      IOUtil.close(ins);
    }
    assertEquals("${custom.version} from webfilter.properties was not updated "+ getAsString(filterFile),"1.0",props.get("app.version"));

  }
}
