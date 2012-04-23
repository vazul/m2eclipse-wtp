/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.maven.ide.eclipse.wtp.overlay.internal.modulecore;

import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

public interface IFilteredVirtualFolder extends IVirtualFolder {

	void setFilter(IResourceFilter filter);
	
	IResourceFilter getFilter();
}
