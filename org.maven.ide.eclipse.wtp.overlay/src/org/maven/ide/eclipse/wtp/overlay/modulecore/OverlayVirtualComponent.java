/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.overlay.modulecore;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualComponent;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

/**
 * Overlay Virtual Component
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class OverlayVirtualComponent extends VirtualComponent implements IOverlayVirtualComponent {
  
  protected IProject project;
  
  public OverlayVirtualComponent(IProject project) {
    super(project, new Path("/"));
    this.project = project;
  }
  
  public IVirtualFolder getRootFolder() {
    // Creates a new instance each time to ensure it's not cached
//    IContainer[] containers = getUnderlyingContainers();
//    IResource[] looseResources = getLooseResources();
//    ResourceListVirtualFolder root = new ResourceListVirtualFolder(project, new Path("/"), containers, looseResources);
//    IVirtualFolder folder = new VirtualFolder(aComponentProject, aRuntimePath)
    IVirtualFolder root = new VirtualFolder(project, new Path("/"));
    
    IVirtualComponent component = ComponentCore.createComponent(project);
    IVirtualFolder classes = component.getRootFolder().getFolder("/WEB-INF/classes");
    return classes;
  }  
}
