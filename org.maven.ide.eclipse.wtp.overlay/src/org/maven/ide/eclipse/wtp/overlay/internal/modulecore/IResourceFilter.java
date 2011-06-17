package org.maven.ide.eclipse.wtp.overlay.internal.modulecore;


public interface IResourceFilter {

	boolean accepts(String path, boolean isFile);
}
