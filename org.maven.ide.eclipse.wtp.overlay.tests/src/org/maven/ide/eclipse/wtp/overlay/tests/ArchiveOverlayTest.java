package org.maven.ide.eclipse.wtp.overlay.tests;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.junit.Test;
import org.maven.ide.eclipse.wtp.common.tests.TestServerUtil;
import org.maven.ide.eclipse.wtp.overlay.internal.modulecore.OverlayVirtualArchiveComponent;

public class ArchiveOverlayTest extends AbstractOverlayIntegrationTest {

  @Test
  public void testArchiveOverlay() throws Exception {
    
    IProject project = importProject("web1");
    Assert.assertTrue("web1 was not imported", project.exists());
    Assert.assertTrue("web1 not accessible", project.isAccessible());
    IVirtualComponent component = ComponentCore.createComponent(project, true);
    Assert.assertNotNull(component);
    IVirtualReference[] refs = component.getReferences();
    Assert.assertEquals(2, refs.length);
    IVirtualComponent overlay = refs[0].getReferencedComponent();
    Assert.assertTrue("overlay type = "+overlay.getClass(), overlay instanceof OverlayVirtualArchiveComponent);
    
    
    IServer previewServer = TestServerUtil.createPreviewServer();
    TestServerUtil.addProjectToServer(project, previewServer);
    Thread.sleep(3000);
    IModuleResource[] resources = TestServerUtil.getServerModulesResources(project);

    Assert.assertNotNull("null resources", resources);
    Assert.assertEquals(dump(resources), 10, resources.length);
    
    InputStream source = new ByteArrayInputStream("temp".getBytes());
    project.getFile("temp.txt").create(source, true, new NullProgressMonitor());
    Thread.sleep(5000);
    
    IModuleResource[] newResources = TestServerUtil.getServerModulesResources(project);

    Assert.assertNotNull("null newresources", newResources);
    Assert.assertEquals(dump(newResources), resources.length, newResources.length);
    
  }

  private String dump(IModuleResource[] resources) {
    if (resources == null) {
      return "null resources";
    }
    String lf = System.getProperty("line.separator");
    StringBuilder sb = new StringBuilder();
    for (IModuleResource r : resources) {
      sb.append(r.getName()).append(lf);
    }
    return sb.toString();
  }
}
