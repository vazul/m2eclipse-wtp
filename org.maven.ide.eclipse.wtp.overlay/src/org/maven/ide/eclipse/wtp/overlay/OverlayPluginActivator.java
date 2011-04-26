package org.maven.ide.eclipse.wtp.overlay;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Plugin;
import org.maven.ide.eclipse.wtp.overlay.internal.servers.OverlayResourceChangeListener;
import org.osgi.framework.BundleContext;

public class OverlayPluginActivator extends Plugin {
	
	IResourceChangeListener overlayresourceChangeListener;
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		overlayresourceChangeListener = new OverlayResourceChangeListener();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
	    workspace.addResourceChangeListener(overlayresourceChangeListener);
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (overlayresourceChangeListener != null) {
		    workspace.removeResourceChangeListener(overlayresourceChangeListener);
		}
		super.stop(context);
	}

}
