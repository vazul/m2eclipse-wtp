package org.maven.ide.eclipse.wtp.overlay;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Plugin;
import org.maven.ide.eclipse.wtp.overlay.internal.servers.OverlayResourceChangeListener;
import org.osgi.framework.BundleContext;

public class OverlayPluginActivator extends Plugin {
	
	public static final String PLUGIN_ID = OverlayConstants.PLUGIN_ID;
	
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
		if (overlayresourceChangeListener != null) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
		    workspace.removeResourceChangeListener(overlayresourceChangeListener);
		}
		super.stop(context);
	}
}
