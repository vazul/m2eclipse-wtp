package org.maven.ide.eclipse.wtp;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.junit.Test;
import org.maven.ide.eclipse.wtp.common.tests.TestServerUtil;
import org.maven.ide.eclipse.wtp.overlay.internal.modulecore.OverlaySelfComponent;
import org.maven.ide.eclipse.wtp.overlay.internal.modulecore.OverlayVirtualArchiveComponent;
import org.maven.ide.eclipse.wtp.overlay.internal.modulecore.OverlayVirtualComponent;

public class OverlayTest extends AbstractWTPTestCase {

  @Test
  public void testArchiveOverlay() throws Exception {
      IProject war = importProject("projects/overlays/war-overlay1/pom.xml");
      waitForJobsToComplete();
      war.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
      waitForJobsToComplete();
      assertNoErrors(war);
      System.out.println("Build complete"+war);
      IVirtualComponent comp = ComponentCore.createComponent(war);
      assertNotNull(comp);
      
      IVirtualReference[] references = comp.getReferences();
      assertEquals(2, references.length);
      assertEquals(OverlayVirtualArchiveComponent.class, references[0].getReferencedComponent().getClass());
      assertEquals(OverlaySelfComponent.class, references[1].getReferencedComponent().getClass());
      System.out.println("2 references read for "+war);
      
      System.out.println("Creating preview server");
      IServer server = TestServerUtil.createPreviewServer();
      System.out.println("adding project to preview server");
      TestServerUtil.addProjectToServer(war, server);
      System.out.println("project added to preview server");

      List<String> resources = TestServerUtil.toList(TestServerUtil.getServerModuleResources(war));
      System.out.println("server module resources :"+resources.size());
      
      assertTrue("META-INF/MANIFEST.MF is missing from "+ resources, resources.contains("META-INF/MANIFEST.MF"));
      assertTrue("WEB-INF/lib/junit-3.8.2.jar is missing from "+ resources,resources.contains("WEB-INF/lib/junit-3.8.2.jar"));
      assertTrue("index.html is missing from "+ resources,resources.contains("index.html"));
      assertTrue("excluded/included.properties is missing from "+ resources, resources.contains("excluded/included.properties"));
      assertTrue("excluded/excluded.properties is missing from "+ resources, resources.contains("excluded/excluded.properties"));
  }
  
  @Test
  public void testProjectOverlay() throws Exception {
      IProject[] projects = importProjects("projects/overlays/",
                              new String[]{"war-overlay2/pom.xml", "overlaid-war/pom.xml"},
                              new ResolverConfiguration()
                            );
      waitForJobsToComplete();
      IProject war = projects[0];
      assertNoErrors(war);
      IProject overlaid = projects[1];
      assertNoErrors(overlaid);
      
      IVirtualComponent comp = ComponentCore.createComponent(war);
      assertNotNull(comp);
      
      IVirtualReference[] references = comp.getReferences();
      
      assertEquals(2, references.length);
      
      assertEquals(OverlayVirtualComponent.class, references[0].getReferencedComponent().getClass());
      assertEquals(OverlaySelfComponent.class, references[1].getReferencedComponent().getClass());
      
      IServer server = TestServerUtil.createPreviewServer();
      TestServerUtil.addProjectToServer(war, server);
      
      List<String> resources = TestServerUtil.toList(TestServerUtil.getServerModuleResources(war));
      
      assertTrue("META-INF/MANIFEST.MF is missing from "+ resources, resources.contains("META-INF/MANIFEST.MF"));
      assertTrue("WEB-INF/lib/junit-3.8.1.jar is missing from "+ resources,resources.contains("WEB-INF/lib/junit-3.8.1.jar"));
      assertTrue("index.html is missing from "+ resources,resources.contains("index.html"));
      assertTrue("excluded/included.properties is missing from "+ resources, resources.contains("excluded/included.properties"));
      assertTrue("excluded/excluded.properties is missing from "+ resources, resources.contains("excluded/excluded.properties"));
      
      updateProject(overlaid, "pom2.xml");
      assertNoErrors(overlaid);

      resources = TestServerUtil.toList(TestServerUtil.getServerModuleResources(war));
      //Check the deployed resources are updated
      assertTrue("META-INF/MANIFEST.MF is missing", resources.contains("META-INF/MANIFEST.MF"));
      assertTrue("WEB-INF/lib/commons-lang-2.4.jar is missing", resources.contains("WEB-INF/lib/commons-lang-2.4.jar"));
      assertTrue("index.html is missing", resources.contains("index.html"));
      assertTrue("excluded/included.properties is missing", resources.contains("excluded/included.properties"));
      assertTrue("excluded/excluded.properties is missing", resources.contains("excluded/excluded.properties"));
      
  }
  
  @Test
  public void testProjectOverlayExclusions() throws Exception {
      IProject[] projects = importProjects("projects/overlays/",
                              new String[]{"war-overlay3/pom.xml", "overlaid-war/pom.xml"},
                              new ResolverConfiguration()
                            );
      waitForJobsToComplete();
      IProject war = projects[0];
      assertNoErrors(war); 
      
      IServer server = TestServerUtil.createPreviewServer();
      TestServerUtil.addProjectToServer(war, server);
      
      List<String> resources = TestServerUtil.toList(TestServerUtil.getServerModuleResources(war));
      assertTrue("META-INF/MANIFEST.MF is missing", resources.contains("META-INF/MANIFEST.MF"));
      assertFalse("WEB-INF/lib/junit-3.8.1.jar should be missing", resources.contains("WEB-INF/lib/junit-3.8.1.jar"));
      assertFalse("index.html should be missing", resources.contains("index.html"));
      assertFalse("excluded/excluded.properties should be missing", resources.contains("excluded/excluded.properties"));
      assertTrue("excluded/included.properties is missing",resources.contains("excluded/included.properties"));
      
      //Change the exclusion policy per overlay
      updateProject(war, "pom2.xml");
      assertNoErrors(war);
      
      resources = TestServerUtil.toList(TestServerUtil.getServerModuleResources(war));
      assertTrue("META-INF/MANIFEST.MF is missing", resources.contains("META-INF/MANIFEST.MF"));
      assertTrue("WEB-INF/lib/junit-3.8.1.jar is missing",resources.contains("WEB-INF/lib/junit-3.8.1.jar"));
      assertTrue("index.html is missing", resources.contains("index.html"));
      assertFalse("excluded/excluded.properties should be missing", resources.contains("excluded/excluded.properties"));
      assertTrue("excluded/included.properties is missing",resources.contains("excluded/included.properties"));
     
      //Change the exclusion policy, use the default overlay in/exclusions
      updateProject(war, "pom3.xml");
      assertNoErrors(war);
      
      resources = TestServerUtil.toList(TestServerUtil.getServerModuleResources(war));
      assertTrue("META-INF/MANIFEST.MF is missing", resources.contains("META-INF/MANIFEST.MF"));
      assertTrue("WEB-INF/lib/junit-3.8.1.jar is missing",resources.contains("WEB-INF/lib/junit-3.8.1.jar"));
      assertFalse("index.html should be missing", resources.contains("index.html"));
      assertFalse("excluded/excluded.properties should be missing", resources.contains("excluded/excluded.properties"));
      assertFalse("excluded/included.properties should be missing", resources.contains("excluded/included.properties"));
      
  }
  
  @Test
  public void testOverlayOrder() throws Exception {
      IProject[] projects = importProjects("projects/overlays/",
                              new String[]{"war-overlay4/pom.xml", "overlaid-war/pom.xml"},
                              new ResolverConfiguration()
                            );
      waitForJobsToComplete();
      IProject war = projects[0];
      assertNoErrors(war); 
      
      IServer server = TestServerUtil.createPreviewServer();
      TestServerUtil.addProjectToServer(war, server);
      
      IModuleResource[] moduleResources = TestServerUtil.getServerModuleResources(war);
      
      List<String> resources = TestServerUtil.toList(moduleResources);
     
      resources = TestServerUtil.toList(moduleResources);
      assertTrue("META-INF/MANIFEST.MF is missing", resources.contains("META-INF/MANIFEST.MF"));
      assertTrue("WEB-INF/lib/junit-3.8.1.jar is missing",resources.contains("WEB-INF/lib/junit-3.8.1.jar"));
      assertTrue("WEB-INF/lib/junit-3.8.2.jar is missing",resources.contains("WEB-INF/lib/junit-3.8.2.jar"));
      assertTrue("index.html is missing", resources.contains("index.html"));
      assertFalse("excluded/excluded.properties should be missing", resources.contains("excluded/excluded.properties"));
      assertTrue("excluded/included.properties is missing",resources.contains("excluded/included.properties"));
     
      IFile indexFile = TestServerUtil.findFile("index.html", moduleResources);
      assertNotNull(indexFile);
      String index = getAsString(indexFile);
      assertContains("overlaid-war", index);
      
      //Change the overlay order
      updateProject(war, "pom2.xml");
      assertNoErrors(war);

      moduleResources = TestServerUtil.getServerModuleResources(war);
      indexFile = TestServerUtil.findFile("index.html", moduleResources);
      assertNotNull(indexFile);
      index = getAsString(indexFile);
      assertContains("war-archive-overlay", index);
      
      //Change the overlay order
      updateProject(war, "pom3.xml");
      assertNoErrors(war);

      moduleResources = TestServerUtil.getServerModuleResources(war);
      indexFile = TestServerUtil.findFile("index.html", moduleResources);
      assertNotNull(indexFile);
      index = getAsString(indexFile);
      assertContains("war-overlay4", index);
            
  }
  
  @Test
  public void testZippedArchiveOverlay() throws Exception {
      IProject war = importProject("projects/overlays/war-zipped-archive-overlay/pom.xml");
      waitForJobsToComplete();
      assertNoErrors(war);
      
      IVirtualComponent comp = ComponentCore.createComponent(war);
      assertNotNull(comp);
      
      IVirtualReference[] references = comp.getReferences();
      assertEquals(2, references.length);
      
      assertEquals(OverlayVirtualArchiveComponent.class, references[0].getReferencedComponent().getClass());
      assertEquals(OverlaySelfComponent.class, references[1].getReferencedComponent().getClass());
      
      IServer server = TestServerUtil.createPreviewServer();
      TestServerUtil.addProjectToServer(war, server);
      
      List<String> resources = TestServerUtil.toList(TestServerUtil.getServerModuleResources(war));
      
      assertTrue(resources.contains("junit/framework/Assert.class"));
  }
  

  @Test
  public void testArchiveOverlayInclusion() throws Exception {
      IProject war = importProject("projects/overlays/war-overlay5/pom.xml");
      waitForJobsToComplete();
      war.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
      waitForJobsToComplete();
      assertNoErrors(war);
      
      IServer server = TestServerUtil.createPreviewServer();
      TestServerUtil.addProjectToServer(war, server);

      List<String> resources = TestServerUtil.toList(TestServerUtil.getServerModuleResources(war));
      System.out.println("server module resources :"+resources);
      
      assertTrue("META-INF/MANIFEST.MF is missing from "+ resources, resources.contains("META-INF/MANIFEST.MF"));
      assertFalse("WEB-INF/lib/junit-3.8.2.jar should be missing from "+ resources,resources.contains("WEB-INF/lib/junit-3.8.2.jar"));
      assertFalse("index.html should be missing from "+ resources,resources.contains("index.html"));
      assertTrue("excluded/included.properties is missing from "+ resources, resources.contains("excluded/included.properties"));
      assertFalse("excluded/excluded.properties should be missing from "+ resources, resources.contains("excluded/excluded.properties"));
      assertTrue("META-INF/maven/test.overlays/war-archive-overlay/pom.properties is missing from "+ resources, resources.contains("META-INF/maven/test.overlays/war-archive-overlay/pom.properties"));
      assertTrue("META-INF/maven/test.overlays/war-archive-overlay/pom.xml is missing from "+ resources, resources.contains("META-INF/maven/test.overlays/war-archive-overlay/pom.xml"));
  }
  
  
}
