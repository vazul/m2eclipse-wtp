package org.maven.ide.eclipse.wtp.overlay.internal.modulecore;

import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

public interface IFilteredVirtualFolder extends IVirtualFolder {

	void setFilter(IResourceFilter filter);
	
	IResourceFilter getFilter();
}
