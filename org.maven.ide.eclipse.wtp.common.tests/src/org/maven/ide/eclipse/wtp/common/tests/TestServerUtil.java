package org.maven.ide.eclipse.wtp.common.tests;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ModuleDelegate;

public class TestServerUtil {
  
  public static IModuleResource[] getServerModuleResources(IProject project) throws Exception {
    IModule module  = ServerUtil.getModule(project);
    if (module == null) {
      throw  new IllegalArgumentException(project.getName() + " is not a Server IModule");
    }
    
    ModuleDelegate d = (ModuleDelegate)module.loadAdapter(ModuleDelegate.class, new NullProgressMonitor());
    if (d == null) {
      throw new NullPointerException("can not find ModuleDelegate for [" + module.getModuleType().getId() + ", " + module.getClass()+ "]");
    }
    //1st call to members() will trigger unzipping of archive overlays
    d.members();
    Thread.sleep(1000);
    //Should get the complete members now
    IModuleResource[] resources = d.members();
    //System.err.println("top resources " + toList(resources));
    List<IModuleResource> all = new ArrayList<IModuleResource>();
    if (resources != null) {
      walk(resources, all);
    }
    resources = all.toArray(new IModuleResource[all.size()]);
   
    return resources;
  }
  
  public static List<String> toList(IModuleResource[] resources) throws Exception {
    List<String> resourcesAsString = new ArrayList<String>();
    if (resources != null) {
      for (IModuleResource r : resources) {
          resourcesAsString.add(getPath(r));
      }
    }
    return resourcesAsString;
  }
  
  public static IFile findFile(String path, IModuleResource[] resources) {
    if (resources != null) {
      for (IModuleResource r : resources) {
          if (path.equals(getPath(r))) {
            return (IFile) r.getAdapter(IFile.class);
          }
      }
    }
    return null;
  }
  
  public static String getPath(IModuleResource resource) {
    if (resource == null) return null;
    String path = "";
    if (!resource.getModuleRelativePath().isEmpty()) {
      path=resource.getModuleRelativePath()+"/";
    }
    path+=resource.getName();
    return path;
  }
  
  private static void walk(IModuleResource[] resources, List<IModuleResource> all) {
    if (resources == null || resources.length == 0) return;
    
    for(IModuleResource r : resources) {
      all.add(r);
      if (r instanceof IModuleFolder) {
        walk(((IModuleFolder)r).members(), all);
      }
    }
    
  }

  public static IServer createPreviewServer() throws CoreException {
    IRuntimeType rt = ServerCore.findRuntimeType("org.eclipse.jst.server.preview.runtime");
    IRuntimeWorkingCopy wc = rt.createRuntime("preview", null);
    IRuntime runtime = wc.save(true, null);
    IServerType st = ServerCore.findServerType("org.eclipse.jst.server.preview.server");
    ServerWorkingCopy swc = (ServerWorkingCopy) st.createServer("previewServer", null, null);
    swc.setServerConfiguration(null);
    swc.setName("previewServer");
    swc.setRuntime(runtime);
    IServer server = swc.save(true, null);
    return server;
  }
  
  public static void addProjectToServer(IProject project, IServer server) throws CoreException {
    IModule module  = ServerUtil.getModule(project);
    if (ServerUtil.containsModule(server, module, new NullProgressMonitor())) {
      return;
    }
    IServerWorkingCopy copy = server.createWorkingCopy();
    copy.modifyModules(new IModule[]{module}, new IModule[0], new NullProgressMonitor());
    server = copy.save(true, new NullProgressMonitor());
  }
  
  
}
