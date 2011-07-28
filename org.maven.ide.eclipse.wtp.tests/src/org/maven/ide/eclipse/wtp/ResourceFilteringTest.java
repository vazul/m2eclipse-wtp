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

@SuppressWarnings("restriction")
public class ResourceFilteringTest extends AbstractWTPTestCase {
  
  private static String FILTERED_FOLDER_NAME = "target/" + MavenWtpConstants.M2E_WTP_FOLDER+ "/" + MavenWtpConstants.WEB_RESOURCES_FOLDER; 
  private static String EAR_FILTERED_FOLDER_NAME = "target/" + MavenWtpConstants.M2E_WTP_FOLDER+ "/" + MavenWtpConstants.EAR_RESOURCES_FOLDER; 
  
  @Test
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
    assertEquals(web.getFolder(FILTERED_FOLDER_NAME), warResources[0]);
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

  @Test
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
    assertEquals(web.getFolder(FILTERED_FOLDER_NAME), warResources[0]);
    assertEquals(web.getFolder("/src/main/webapp"), warResources[1]);

    //Check properties from settings.xml have been used
    IFile contextXml = filteredFolder.getFile("META-INF/context.xml");
    assertTrue(contextXml.getName() +" is missing",contextXml.exists());
    String xml = getAsString(contextXml);
    assertTrue("${db.username} from META-INF/context.xml was not interpolated : "+xml, xml.contains("username=\"fred\""));

    
  }

  @Test
  public void testMECLIPSEWTP5_webXmlfiltering() throws Exception {
    IProject web = importProject("projects/WebResourceFiltering/example-web/pom.xml");
    web.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    //assertNoErrors(web);
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

  @Test
  public void testMECLIPSEWTP95_filteringErrors() throws Exception {
    IProject web = importProject("projects/WebResourceFiltering/typo-filtering/pom.xml");
    web.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    List<IMarker> markers = findErrorMarkers(web);
    assertFalse("Should have some markers",markers.isEmpty());
    assertHasMarker("An error occurred while filtering resources", markers);
    //TODO see why the marker disappears on import 
    //assertHasMarker("org.maven.ide.eclipse.maven2Problem:Cannot find setter, adder nor field in org.apache.maven.plugin.resources.Resource for 'filter'", markers);
    
    IFolder filteredFolder = web.getFolder(FILTERED_FOLDER_NAME);
    //assertTrue("Filtered folder should always be created", filteredFolder.exists());
       
    //Let's change the active profile to see if the values are updated
    updateProject(web, "good.pom.xml");    
    
    assertNoErrors(web);
    assertTrue("Files should have been filtered",filteredFolder.members().length > 0);
  }

  @Test
  public void testMECLIPSE97_OldWebResourceSupport() throws Exception {
    IProject web = importProject("projects/WebResourceFiltering/old-webresource-support/pom.xml");
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
    assertTrue("${project.artifactId} from pom was not interpolated", index.contains("<body>Welcome @ old-webresource-support</body>"));

    Properties props = getFileAsProperties(filteredFolder, "index.properties");
    assertEquals("${custom.version} from webfilter.properties was not interpolated","0.0.1-SNAPSHOT",props.get("app.version"));

    IFile webXml = filteredFolder.getFile("WEB-INF/web.xml");
    assertTrue(webXml.getName() +" is missing",webXml.exists());
    String xml = getAsString(webXml);
    assertTrue("${welcome.page} from webfilter.properties was not interpolated", xml.contains("<welcome-file>index.html</welcome-file>"));

    IFile ignored = filteredFolder.getFile("ignoreme.txt");
    assertFalse("ignoreme.txt should be excluded",ignored.exists());
  }
  
  
  @Test
  public void testMECLIPSE124_earfiltering() throws Exception {
    IProject[] projects = importProjects("projects/MECLIPSEWTP-124/", 
        new String[]{"pom.xml", "ear/pom.xml", "ejb/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();
    
    IProject ear = projects[1];
    ear.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    assertNoErrors(ear);
    
    IFolder earResourcesFolder = ear.getFolder(EAR_FILTERED_FOLDER_NAME);
    assertTrue("Filtered folder doesn't exist", earResourcesFolder.exists());

    IFile jbossServiceFile = earResourcesFolder.getFile("META-INF/jboss-service.xml");
    assertTrue("jboss-service.xml doesn't exist", jbossServiceFile.exists());

    String jbossService = getAsString(jbossServiceFile);
    String expectedAttribute = "<attribute name=\"CustomAttribute\">MBean Attribute Value</attribute>";
    assertTrue("File was not filtered "+jbossService, jbossService.contains(expectedAttribute));
  }


  @Test
  public void testMECLIPSE124_earFilterFile() throws Exception {
    IProject[] projects = importProjects("projects/MECLIPSEWTP-124/", 
        new String[]{"pom.xml", "ear-filters/pom.xml", "ejb/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();
    
    IProject ear = projects[1];
    ear.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    assertNoErrors(ear);
    
    IFolder earResourcesFolder = ear.getFolder(EAR_FILTERED_FOLDER_NAME);
    assertTrue("Filtered folder doesn't exist", earResourcesFolder.exists());

    IFile jbossServiceFile = earResourcesFolder.getFile("META-INF/jboss-service.xml");
    assertTrue("jboss-service.xml doesn't exist", jbossServiceFile.exists());

    String jbossService = getAsString(jbossServiceFile);
    String expectedAttribute = "<attribute name=\"CustomAttribute\">MBean Attribute Value from config.properties</attribute>";
    assertTrue("File was not filtered "+jbossService, jbossService.contains(expectedAttribute));
    
  }

  @Test
  public void testMECLIPSE124_advancedEarFiltering() throws Exception {
    IProject[] projects = importProjects("projects/MECLIPSEWTP-124/", 
        new String[]{"pom.xml", "advanced-ear-filters/pom.xml", "ejb/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();
    
    IProject ear = projects[1];
    ear.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    assertNoErrors(ear);
    
    IFolder earResourcesFolder = ear.getFolder(EAR_FILTERED_FOLDER_NAME);
    assertTrue("Filtered folder doesn't exist", earResourcesFolder.exists());

    IFile jbossServiceFile = earResourcesFolder.getFile("META-INF/jboss-service.xml");
    assertTrue("jboss-service.xml doesn't exist", jbossServiceFile.exists());

    String jbossService = getAsString(jbossServiceFile);
    String expectedAttribute = "<attribute name=\"CustomAttribute\">MBean Attribute Value from config.properties</attribute>";
    assertTrue("File was not filtered "+jbossService, jbossService.contains(expectedAttribute));
  
    String expectedNonFilteredAttribute = "<attribute name=\"id\">${project.artifactId}</attribute>";
    assertTrue("escapeString was ignored "+jbossService, jbossService.contains(expectedNonFilteredAttribute));

    IFile propertiesFile = earResourcesFolder.getFile("META-INF/ignored.properties");
    assertTrue("ignored.properties should exist", propertiesFile.exists());
    
    String ignored = getAsString(propertiesFile);
    assertTrue("ignored.properties  was filtered "+ignored, ignored.contains("attribute=${my.custom.mbean.attribute.value}"));
  }

  
  @Test
  public void testResourcesOutsideProject() throws Exception {
    IProject[] projects = importProjects("projects/WebResourceFiltering/top/", 
                  new String[]{"pom.xml", "mid/pom.xml", "mid/web/pom.xml"}, 
                  new ResolverConfiguration());
    
    IProject web = projects[2];
    web.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    assertNoErrors(web);
    IFolder filteredFolder = web.getFolder(FILTERED_FOLDER_NAME);
    assertTrue("Filtered folder doesn't exist", filteredFolder.exists());
    
    IFile indexHtml = filteredFolder.getFile("index.html");
    assertTrue("index.html is missing",indexHtml.exists());
    String index = getAsString(indexHtml);
    assertTrue("${artifactId} is missing", index.contains("${artifactId}"));
    
    //Let's activate filtering, see if the values are updated
    updateProject(web, "pom2.xml");    
    
    index = getAsString(indexHtml);
    assertTrue("${artifactId} has not been interpolated", index.contains("web"));
    
    
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
