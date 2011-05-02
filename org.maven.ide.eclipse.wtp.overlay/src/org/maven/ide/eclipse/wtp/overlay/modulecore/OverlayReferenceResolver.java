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
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.ComponentcorePackage;
import org.eclipse.wst.common.componentcore.internal.DependencyType;
import org.eclipse.wst.common.componentcore.internal.ReferencedComponent;
import org.eclipse.wst.common.componentcore.resolvers.IReferenceResolver;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.maven.ide.eclipse.wtp.overlay.internal.modulecore.OverlaySelfComponent;
import org.maven.ide.eclipse.wtp.overlay.internal.modulecore.OverlayVirtualArchiveComponent;
import org.maven.ide.eclipse.wtp.overlay.internal.modulecore.OverlayVirtualComponent;

/**
 * Overlay Reference Resolver
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class OverlayReferenceResolver implements IReferenceResolver {

  public static final String PROTOCOL = "module:/overlay/";
  
  public static final String PROJECT_PROTOCOL = PROTOCOL+"prj/";

  public static final String VAR_ARCHIVE_PROTOCOL = PROTOCOL+"var/";

  public static final String SELF_PROTOCOL = PROTOCOL+"slf/";

  public boolean canResolve(IVirtualComponent component, ReferencedComponent referencedComponent) {
    URI uri = referencedComponent.getHandle();
    return (uri.segmentCount() > 2) && (uri.segment(0).equals("overlay"));
  }

  public IVirtualReference resolve(IVirtualComponent component, ReferencedComponent referencedComponent) {
	String type = referencedComponent.getHandle().segment(1); 
    IVirtualComponent comp = null;
	String url = referencedComponent.getHandle().toString();
	if ("prj".equals(type)) {
		comp = createProjectComponent(component, url.substring(PROJECT_PROTOCOL.length()));
	} else if ("var".equals(type)) {
		comp = createArchivecomponent(component, url.substring(PROTOCOL.length()));
	} else if ("slf".equals(type)){
		comp = createSelfComponent(component);
	}
	if (comp == null) {
		throw new IllegalArgumentException(referencedComponent.getHandle() + " could not be resolved");
	}
    IVirtualReference ref = ComponentCore.createReference(component, comp);
    ref.setArchiveName(referencedComponent.getArchiveName());
    ref.setRuntimePath(referencedComponent.getRuntimePath());
    ref.setDependencyType(referencedComponent.getDependencyType().getValue());
    return ref;
  }

  private IVirtualComponent createSelfComponent(IVirtualComponent component) {
	  return new OverlaySelfComponent(component.getProject());
  }

  private IVirtualComponent createArchivecomponent(IVirtualComponent component, String url) {
  	return new OverlayVirtualArchiveComponent(component.getProject(), 
  			url, 
  			component.getProject().getFolder("target/m2e-wtp/overlays/").getProjectRelativePath(), 
  			new Path("/"));
  }

  private IVirtualComponent createProjectComponent(IVirtualComponent component, String name) {
    IProject p = null;   
	if("".equals(name)) {
      p = component.getProject();
    } else {
      p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);    	
    }
	if (p == null) {
		throw new IllegalArgumentException(name + " is not a workspace project");
	}
	return new OverlayVirtualComponent(p);
  }



  public boolean canResolve(IVirtualReference reference) {
    return  reference != null && reference.getReferencedComponent() instanceof IOverlayVirtualComponent;
  }

  public ReferencedComponent resolve(IVirtualReference reference) {
    if(canResolve(reference)) {
      IOverlayVirtualComponent comp = (IOverlayVirtualComponent)reference.getReferencedComponent();
      ReferencedComponent rc = ComponentcorePackage.eINSTANCE.getComponentcoreFactory().createReferencedComponent();
      rc.setArchiveName(reference.getArchiveName());
      rc.setRuntimePath(reference.getRuntimePath());
      URI handle;
      if (comp instanceof OverlayVirtualComponent) {
    	  IProject p = comp.getProject();
    	  if (p.equals(reference.getEnclosingComponent().getProject())) {
        	  handle = URI.createURI(SELF_PROTOCOL);
    	  } else {
        	  handle = URI.createURI(PROJECT_PROTOCOL+p.getName());
    	  }
      } else {
    	  handle = URI.createURI(VAR_ARCHIVE_PROTOCOL+((OverlayVirtualArchiveComponent)comp).getArchivePath().toString());//FIXME
      }
	  rc.setHandle(handle); 
      rc.setDependencyType(DependencyType.CONSUMES_LITERAL);
      return rc;
    }
    return null;
  }

}
