package org.maven.ide.eclipse.wtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.javaee.web.internal.util.WebXMLHelperImpl;
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

    Properties props = getFileAsProperties(filteredFolder, "index.properties");
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
    
    props = getFileAsProperties(filteredFolder, "index.properties");
    assertEquals("${custom.version} from webfilter.properties was not updated "+ getAsString(filterFile),"1.0",props.get("app.version"));
  }

  
  public void testMECLIPSE22_webfilteringFolderOrder() throws Exception {
    IProject web = importProject("projects/WebResourceFiltering/war-with-filtered-resources/pom.xml");
    web.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    assertNoErrors(web);
    IFolder filteredFolder = web.getFolder(FILTERED_FOLDER_NAME);
    assertTrue("Filtered folder doesn't exist", filteredFolder.exists());
        
    //Check the filtered folder will be deployed first
    IVirtualComponent webComponent = ComponentCore.createComponent(web);
    IVirtualFolder rootwar = webComponent.getRootFolder();
    IResource[] warResources = rootwar.getUnderlyingResources();
    assertEquals(2, warResources.length);
    assertEquals(web.getFolder("/target/m2eclipse-wtp/webresources"), warResources[0]);
    assertEquals(web.getFolder("/src/main/webapp"), warResources[1]);

    //Check properties from settings.xml have been used
    IFile contextXml = filteredFolder.getFile("META-INF/context.xml");
    assertTrue(contextXml.getName() +" is missing",contextXml.exists());
    String xml = getAsString(contextXml);
    assertTrue("${db.username} from META-INF/context.xml was not interpolated : "+xml, xml.contains("username=\"fred\""));

    
  }

  public void testMECLIPSEWTP5_webXmlfiltering() throws Exception {
    IProject web = importProject("projects/WebResourceFiltering/example-web/pom.xml");
    web.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    assertNoErrors(web);
    IFolder filteredFolder = web.getFolder(FILTERED_FOLDER_NAME);
    assertTrue("Filtered folder doesn't exist", filteredFolder.exists());
    
    //Check all the files are correctly filtered
    IFile webXml = filteredFolder.getFile("WEB-INF/web.xml");
    assertTrue(webXml.getName() +" is missing",webXml.exists());
    String xml = getAsString(webXml);
    assertTrue("${web.xml.facelets.development} from localhost.properties was not interpolated", xml.contains("<param-name>facelets.DEVELOPMENT</param-name><param-value>true</param-value>"));
    assertTrue("${web.xml.myfaces.pretty_html} from localhost.properties was not interpolated", xml.contains("<param-name>org.apache.myfaces.PRETTY_HTML</param-name><param-value>true</param-value>"));
    assertTrue("${web.xml.myfaces.validate} from localhost.properties was not interpolated", xml.contains("<param-name>org.apache.myfaces.VALIDATE</param-name><param-value>false</param-value>"));
    assertTrue("${props.target.env} from localhost profile was not interpolated", xml.contains("<param-name>com.swisscom.asterix.intertax.build.targetEnv</param-name><param-value>localhost</param-value>"));

    IFile ignored = filteredFolder.getFile("ignoreme.txt");
    assertFalse("ignoreme.txt should be excluded",ignored.exists());
    
    //Let's change the active profile to see if the values are updated
    updateProject(web, "pom.dev.xml");    
    
    xml = getAsString(webXml);
    assertTrue("${web.xml.facelets.development} from dev.properties was not interpolated", xml.contains("<param-name>facelets.DEVELOPMENT</param-name><param-value>false</param-value>"));
    assertTrue("${web.xml.myfaces.pretty_html} from dev.properties was not interpolated", xml.contains("<param-name>org.apache.myfaces.PRETTY_HTML</param-name><param-value>false</param-value>"));
    assertTrue("${web.xml.myfaces.validate} from dev.properties was not interpolated", xml.contains("<param-name>org.apache.myfaces.VALIDATE</param-name><param-value>true</param-value>"));
    assertTrue("${props.target.env} from dev profile was not interpolated", xml.contains("<param-name>com.swisscom.asterix.intertax.build.targetEnv</param-name><param-value>DEV</param-value>"));
  }
  
  /**
   * @param folder
   * @return
   * @throws CoreException
   * @throws IOException
   */
  private Properties getFileAsProperties(IFolder folder, String fileName) throws CoreException, IOException {
    IFile file;
    Properties props;
    InputStream ins;
    file = folder.getFile(fileName);
    assertTrue(fileName +" is missing",file.exists());
    props = new Properties();
    ins = file.getContents();
    try {
      props.load(ins);  
    } finally {
      IOUtil.close(ins);
    }
    return props;
  }
}
