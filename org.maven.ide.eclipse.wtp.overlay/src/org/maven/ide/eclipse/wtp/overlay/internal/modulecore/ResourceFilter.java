package org.maven.ide.eclipse.wtp.overlay.internal.modulecore;

import org.eclipse.core.resources.IResource;

public interface ResourceFilter {

	boolean accepts(IResource resource);

}
