/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.overlay.modulecore;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.ComponentcorePackage;
import org.eclipse.wst.common.componentcore.internal.DependencyType;
import org.eclipse.wst.common.componentcore.internal.ReferencedComponent;
import org.eclipse.wst.common.componentcore.resolvers.IReferenceResolver;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;

/**
 * Overlay Reference Resolver
 *
 * @author Fred Bricon
 */
public class OverlayReferenceResolver implements IReferenceResolver {

  public static final String PROTOCOL = "module:/overlay/";
  
  public boolean canResolve(IVirtualComponent component, ReferencedComponent referencedComponent) {
    URI uri = referencedComponent.getHandle();
    return ((uri.segmentCount() > 1) && (uri.segment(0).equals("overlay")));
  }

  public IVirtualReference resolve(IVirtualComponent component, ReferencedComponent referencedComponent) {
    String project = referencedComponent.getHandle().segment(1);
    IProject p = null;
    if( !project.equals("")) 
      p = ResourcesPlugin.getWorkspace().getRoot().getProject(project);
    else
      p = component.getProject();
    
    IVirtualComponent comp = new OverlayVirtualComponent(p);
    IVirtualReference ref = ComponentCore.createReference(component, comp);
    ref.setArchiveName(referencedComponent.getArchiveName());
    ref.setRuntimePath(referencedComponent.getRuntimePath());
    ref.setDependencyType(referencedComponent.getDependencyType().getValue());
    return ref;
  }

  public boolean canResolve(IVirtualReference reference) {
    return  reference != null && reference.getReferencedComponent() instanceof IOverlayVirtualComponent;
  }

  public ReferencedComponent resolve(IVirtualReference reference) {
    
    if(canResolve(reference)) {
      IOverlayVirtualComponent comp = (IOverlayVirtualComponent)reference.getReferencedComponent();
      IProject p = reference.getReferencedComponent().getProject();
      ReferencedComponent rc = ComponentcorePackage.eINSTANCE.getComponentcoreFactory().createReferencedComponent();
      rc.setArchiveName(reference.getArchiveName());
      rc.setRuntimePath(reference.getRuntimePath());
      rc.setHandle(URI.createURI(PROTOCOL+p.getName())); 
      rc.setDependencyType(DependencyType.CONSUMES_LITERAL);
      return rc;
    }
    return null;
  }

}
