package org.maven.ide.eclipse.wtp.overlay.internal.modulecore;

import org.eclipse.core.resources.IResource;
import org.eclipse.wst.common.componentcore.internal.flat.IFlatResource;

public interface IResourceFilter {

	boolean accepts(IResource resource);
	
	boolean accepts(IFlatResource resource);
}
