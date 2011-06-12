package org.maven.ide.eclipse.wtp.jpt;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class DaliPluginActivator extends Plugin {
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		System.err.println("start "+context);
	}
}
