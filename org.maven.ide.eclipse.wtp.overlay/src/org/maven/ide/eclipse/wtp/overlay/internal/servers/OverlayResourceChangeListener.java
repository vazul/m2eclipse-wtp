package org.maven.ide.eclipse.wtp.overlay.internal.servers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.Server;
import org.maven.ide.eclipse.wtp.overlay.internal.modulecore.OverlaySelfComponent;
import org.maven.ide.eclipse.wtp.overlay.modulecore.IOverlayVirtualComponent;

public class OverlayResourceChangeListener implements IResourceChangeListener {

	public void resourceChanged(IResourceChangeEvent event) {
		
		IResourceDelta delta =  event.getDelta();
		if (delta == null) {
			return;
		}

		IServer[] servers = ServerCore.getServers();
		if (servers.length == 0) {
			//No servers defined, so nothing to do
			return;
		}
		
		IResourceDelta[] projectDeltas = delta.getAffectedChildren();
		if (projectDeltas == null || projectDeltas.length == 0) {
			return;
		}
		
		Set<IProject> changedProjects  = getChangedProjects(projectDeltas);
		if (changedProjects.isEmpty()) {
			return;
		}
		
		Set<IServer> republishableServers = new HashSet<IServer>(servers.length);
		
		for (IServer server : servers) {
			modules : for (IModule module : server.getModules()) {
				IProject moduleProject = module.getProject();
				for (IProject changedProject : changedProjects) {
					if (hasOverlayChanged(changedProject, moduleProject, delta)) {
						//System.err.println(moduleProject.getName() + " overlays " +changedProject.getName());
						republishableServers.add(server);
						break modules;
					}
				}
			}
		}
		
		for(IServer server : republishableServers) {
      /* Looks like clearing the module cache is no longer necessary
			if (server instanceof Server) {
			  System.err.println("Clearing "+server.getName() + "'s module cache");
				synchronized (server) {
					((Server)server).clearModuleCache();
				}
			}
      */
      //TODO Publish more elegantly (check server status ...)
			server.publish(IServer.PUBLISH_INCREMENTAL, new NullProgressMonitor());
		}
	}

	private Set<IProject> getChangedProjects(IResourceDelta[] projectDeltas) {
		Set<IProject> projects = new HashSet<IProject>();
		if (projectDeltas != null) {
			for (IResourceDelta delta : projectDeltas) {
				IResource resource = delta.getResource();
				if (resource != null && resource instanceof IProject) {
					projects.add((IProject) resource);
				}
			}
		}
		return projects;
	}
	
	/**
	 * Return true if moduleProject references changedProject as an IOverlayComponent
	 * @param changedProject
	 * @param projectDeployedOnServer
	 * @param delta 
	 * @return true if moduleProject references changedProject as an IOverlayComponent
	 */
	private boolean hasOverlayChanged(IProject changedProject, IProject projectDeployedOnServer, IResourceDelta delta) {
		if (!ModuleCoreNature.isFlexibleProject(projectDeployedOnServer)) {
			return false; 
		}
		IVirtualComponent component = ComponentCore.createComponent(projectDeployedOnServer);
		if (component == null) {
			return false;
		}
		IVirtualReference[] references = component.getReferences();
		if (references == null || references.length == 0) {
			return false;
		}
		for (IVirtualReference reference : references) {
			IVirtualComponent vc = reference.getReferencedComponent();
			if (vc instanceof IOverlayVirtualComponent){
			  IProject overlaidProject = vc.getProject(); 
			  if (vc instanceof OverlaySelfComponent) {
			    IPath componentFilePath = overlaidProject.getFile(".settings/org.eclipse.wst.common.component").getFullPath();
			    if (delta.findMember(componentFilePath) != null) {
			      return true;
			    }
			  } else if (overlaidProject.equals(changedProject)){
			    return true;
			  }
			}
		}
		return false;
	}


}
