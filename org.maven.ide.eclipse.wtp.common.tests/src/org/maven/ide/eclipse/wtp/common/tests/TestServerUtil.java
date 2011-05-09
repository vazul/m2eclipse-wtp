package org.maven.ide.eclipse.wtp.common.tests;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.common.componentcore.internal.flat.FlatFolder;
import org.eclipse.wst.common.componentcore.internal.flat.IFlatFolder;
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
  
  public static IModuleResource[] getServerModulesResources(IProject project) throws Exception {
    IModule module  = ServerUtil.getModule(project);
    if (module == null) {
      throw  new IllegalArgumentException(project.getName() + " is not a Server IModule");
    }

    ModuleDelegate d = (ModuleDelegate) module.getAdapter(ModuleDelegate.class);
    if (d == null) {
      throw new NullPointerException("can not find ModuleDelegate for [" + module.getModuleType().getId() + ", " + module.getClass()+ "]");
    }
   
    IModuleResource[] resources = d.members();
    List<IModuleResource> all = new ArrayList<IModuleResource>();
    if (resources != null) {
      walk(resources, all);
    }
    resources = all.toArray(new IModuleResource[all.size()]);
   
    return resources;
  }
  
  private static void walk(IModuleResource[] resources, List<IModuleResource> all) {
    if (resources == null || resources.length == 0) return;
    
    for(IModuleResource r : resources) {
      all.add(r);
      if (r instanceof IModuleFolder) {
        walk((IModuleResource[])((IModuleFolder)r).members(), all);
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
    server = copy.save(false, new NullProgressMonitor());
  }
  
  
}
