package org.maven.ide.eclipse.wtp.jpt;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jpt.common.core.internal.resource.SimpleJavaResourceLocator;
import org.eclipse.jpt.common.core.resource.ResourceLocator;

public class MavenResourceLocator extends SimpleJavaResourceLocator implements ResourceLocator {

	public MavenResourceLocator() {
		System.err.println("Creating MavenResourceLocator" );
	}
	
	@Override
	public boolean acceptResourceLocation(IProject project, IContainer container) {
		//boolean accepts = super.acceptResourceLocation(project, container);
		//System.err.println("acceptResourceLocation " + project  + "::" + container + " = " + accepts);
		return true;
	}

	public IContainer getDefaultResourceLocation(IProject project) {
		System.err.println("getDefaultResourceLocation " + project );
		return project.getFolder("src/main/resources/META-INF/");
	}

	public IPath getResourcePath(IProject project, IPath runtimePath) {
		System.err.println("getResourcePath " + project + " : " + runtimePath);
		return project.getFolder("target/classes/").getFullPath().append(runtimePath);
	}

	public IPath getRuntimePath(IProject project, IPath resourcePath) {
		System.err.println("getRuntimePath " + project + " : " + resourcePath);
		return null;
	}

}
