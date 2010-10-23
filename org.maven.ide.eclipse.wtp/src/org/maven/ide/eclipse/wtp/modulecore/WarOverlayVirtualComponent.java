/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.modulecore;

import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;

/**
 * WarOverlayVirtualComponent
 *
 * @author Fred Bricon
 */
public class WarOverlayVirtualComponent implements IVirtualComponent, IWarOverlayVirtualComponent {

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  public Object getAdapter(Class adapter) {
    // TODO Auto-generated method getAdapter
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getName()
   */
  public String getName() {
    // TODO Auto-generated method getName
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getDeployedName()
   */
  public String getDeployedName() {
    // TODO Auto-generated method getDeployedName
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getComponent()
   */
  public IVirtualComponent getComponent() {
    // TODO Auto-generated method getComponent
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getMetaProperties()
   */
  public Properties getMetaProperties() {
    // TODO Auto-generated method getMetaProperties
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#setMetaProperty(java.lang.String, java.lang.String)
   */
  public void setMetaProperty(String paramString1, String paramString2) {
    // TODO Auto-generated method setMetaProperty
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#setMetaProperties(java.util.Properties)
   */
  public void setMetaProperties(Properties paramProperties) {
    // TODO Auto-generated method setMetaProperties
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getMetaResources()
   */
  public IPath[] getMetaResources() {
    // TODO Auto-generated method getMetaResources
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#setMetaResources(org.eclipse.core.runtime.IPath[])
   */
  public void setMetaResources(IPath[] paramArrayOfIPath) {
    // TODO Auto-generated method setMetaResources
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getReferences()
   */
  public IVirtualReference[] getReferences() {
    // TODO Auto-generated method getReferences
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getReferences(java.util.Map)
   */
  public IVirtualReference[] getReferences(Map<String, Object> paramMap) {
    // TODO Auto-generated method getReferences
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getReference(java.lang.String)
   */
  public IVirtualReference getReference(String paramString) {
    // TODO Auto-generated method getReference
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#setReferences(org.eclipse.wst.common.componentcore.resources.IVirtualReference[])
   */
  public void setReferences(IVirtualReference[] paramArrayOfIVirtualReference) {
    // TODO Auto-generated method setReferences
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#addReferences(org.eclipse.wst.common.componentcore.resources.IVirtualReference[])
   */
  public void addReferences(IVirtualReference[] paramArrayOfIVirtualReference) {
    // TODO Auto-generated method addReferences
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#isBinary()
   */
  public boolean isBinary() {
    // TODO Auto-generated method isBinary
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#create(int, org.eclipse.core.runtime.IProgressMonitor)
   */
  public void create(int paramInt, IProgressMonitor paramIProgressMonitor) throws CoreException {
    // TODO Auto-generated method create
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getRootFolder()
   */
  public IVirtualFolder getRootFolder() {
    // TODO Auto-generated method getRootFolder
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getProject()
   */
  public IProject getProject() {
    // TODO Auto-generated method getProject
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#exists()
   */
  public boolean exists() {
    // TODO Auto-generated method exists
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.componentcore.resources.IVirtualComponent#getReferencingComponents()
   */
  public IVirtualComponent[] getReferencingComponents() {
    // TODO Auto-generated method getReferencingComponents
    return null;
  }

}
