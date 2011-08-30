/*******************************************************************************
 * Copyright (c) 2011 JBoss by Red Hat
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.facets;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.WtpUtils;
import org.eclipse.jst.common.project.facet.core.ClasspathHelper;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.internal.common.J2EEVersionUtil;
import org.eclipse.jst.j2ee.internal.common.classpath.J2EEComponentClasspathContainer;
import org.eclipse.jst.j2ee.internal.web.classpath.WebAppLibrariesContainer;
import org.eclipse.jst.j2ee.internal.web.plugin.WebPlugin;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.FacetDataModelProvider;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;


/**
 * War update delegate. Based off the org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDelegate
 * 
 * @author Fred Bricon
 */
public class WarUpdateDelegate implements IDelegate {

  public void execute(IProject project, IProjectFacetVersion fv, Object cfg, IProgressMonitor monitor)
      throws CoreException {

    if (monitor != null) {
      monitor.beginTask("Updating Dynamic Web facet to "+fv, 1); //$NON-NLS-1$
    }

    try {
      final IDataModel model = (IDataModel) cfg;

      final IVirtualComponent c = ComponentCore.createComponent(project, true);
      if (c == null) {
        return;
      }
      
      // Setup the flexible project structure.
      try {
        if (model != null) {

          final IWorkspace ws = ResourcesPlugin.getWorkspace();
          final IPath pjpath = project.getFullPath();

          final IPath contentdir = setContentPropertyIfNeeded(model, pjpath, project);
          mkdirs(ws.getRoot().getFolder(contentdir), monitor);

          String contextRoot = model.getStringProperty(IWebFacetInstallDataModelProperties.CONTEXT_ROOT);
          setContextRootPropertyIfNeeded(c, contextRoot);

          
          IDataModelOperation notificationOperation = ((IDataModelOperation) model.getProperty(FacetDataModelProvider.NOTIFICATION_OPERATION));
          if (notificationOperation != null) {
            notificationOperation.execute(monitor, null);
          }
        }
      } catch (ExecutionException e) {
        WebPlugin.logError(e);
      }
      
      System.err.println("Changing Dyn Web Facet version to "+fv);
      if (monitor != null) {
        monitor.worked(1);
      }
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

  
  private static void mkdirs(final IFolder folder, IProgressMonitor monitor) throws CoreException {
    if (!folder.exists()) {
      if (folder.getParent() instanceof IFolder) {
        mkdirs((IFolder) folder.getParent(), monitor);
      }
      folder.create(true, true, null);
    }
    else
    {
        IContainer x = folder;
        while( x instanceof IFolder && x.isDerived() )
        {
            x.setDerived( false, monitor);
            x = x.getParent();
        }
    }
  }

  private void setContextRootPropertyIfNeeded(final IVirtualComponent c, String contextRoot) {
    String existing = c.getMetaProperties().getProperty("context-root"); //$NON-NLS-1$
    if (existing == null)
      c.setMetaProperty("context-root", contextRoot); //$NON-NLS-1$
  }

  private IPath setContentPropertyIfNeeded(final IDataModel model, final IPath pjpath, IProject project) {
    IVirtualComponent c = ComponentCore.createComponent(project, false);
    if (c.exists()) {
      if( !c.getRootFolder().getProjectRelativePath().isRoot() ){
        return c.getRootFolder().getUnderlyingResource().getFullPath();
      }
    }
    return pjpath.append(model.getStringProperty(IJ2EEModuleFacetInstallDataModelProperties.CONFIG_FOLDER));
  }
}
