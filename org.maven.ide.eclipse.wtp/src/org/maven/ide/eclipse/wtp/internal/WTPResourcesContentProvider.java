/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.wtp.WTPProjectsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * WTP resources content provider
 *
 * @author Eugene Kuleshov
 */
public class WTPResourcesContentProvider extends BaseWorkbenchContentProvider implements IPipelinedTreeContentProvider {

  private static final Logger LOG = LoggerFactory.getLogger(WTPResourcesContentProvider.class); 
  
  public void init(ICommonContentExtensionSite config) {
  }

  public void restoreState(IMemento memento) {
  }

  public void saveState(IMemento memento) {
  }

  public Object[] getChildren(Object element) {
    if(element instanceof WTPResourcesNode) {
      return ((WTPResourcesNode) element).getResources();
    }
    return super.getChildren(element);
  }

  // IPipelinedTreeContentProvider

  @SuppressWarnings("rawtypes")
  public void getPipelinedElements(Object element, Set currentElements) {
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void getPipelinedChildren(Object parent, Set currentChildren) {
    if (parent instanceof IProject) {
      IProject project = (IProject) parent;
      if(project.isAccessible()) {
        try {
          IFacetedProject facetedProject = ProjectFacetsManager.create(project);//MNGECLIPSE-1992 there's no reason to actually create a ProjectFacet at this point
          if(facetedProject != null && 
              (facetedProject.hasProjectFacet(WTPProjectsUtil.DYNAMIC_WEB_FACET) || 
               facetedProject.hasProjectFacet(WTPProjectsUtil.EAR_FACET))) {
            List newChildren = new ArrayList<Object>();
            newChildren.add(new WTPResourcesNode(project));
            newChildren.addAll(currentChildren);
            currentChildren.clear();
            currentChildren.addAll(newChildren);
          }
        } catch(CoreException ex) {
          LOG.error("Error getting pipelined children", ex);
        }
      }
    }
  }

  public Object getPipelinedParent(Object element, Object suggestedParent) {
    return suggestedParent;
  }

  public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
    return false;
  }
  
  public boolean interceptUpdate(PipelinedViewerUpdate updateSynchronization) {
    return false;
  }
  
  public PipelinedShapeModification interceptAdd(PipelinedShapeModification addModification) {
    return addModification;
  }

  public PipelinedShapeModification interceptRemove(PipelinedShapeModification removeModification) {
    return removeModification;
  }
  
}

