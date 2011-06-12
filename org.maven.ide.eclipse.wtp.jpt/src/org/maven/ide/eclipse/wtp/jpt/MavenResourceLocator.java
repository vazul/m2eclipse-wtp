package org.maven.ide.eclipse.wtp.jpt;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jpt.common.core.resource.ResourceLocator;

public class MavenResourceLocator implements ResourceLocator {

	public MavenResourceLocator() {
		System.err.println("Creating MavenResourceLocator" );
	}
	
	public boolean acceptResourceLocation(IProject project, IContainer container) {
		System.err.println("acceptResourceLocation " + project );
		return false;
	}

	public IContainer getDefaultResourceLocation(IProject project) {
		System.err.println("getDefaultResourceLocation " + project );
		return null;
	}

	public IPath getResourcePath(IProject project, IPath runtimePath) {
		System.err.println("getResourcePath " + project + " : " + runtimePath);
		return null;
	}

	public IPath getRuntimePath(IProject project, IPath resourcePath) {
		System.err.println("getRuntimePath " + project + " : " + resourcePath);
		return null;
	}

}
