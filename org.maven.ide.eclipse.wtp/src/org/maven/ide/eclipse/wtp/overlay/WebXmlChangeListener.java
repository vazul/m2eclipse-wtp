/*******************************************************************************
 * Copyright (c) 2011 JBoss by Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.overlay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.wtp.JEEPackaging;
import org.maven.ide.eclipse.wtp.MavenWtpPlugin;
import org.maven.ide.eclipse.wtp.OverlayConfigurator;


/**
 * WebXmlChangeListener
 *
 * @author varadi
 */
public class WebXmlChangeListener implements IResourceChangeListener {

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
   */
  public void resourceChanged(IResourceChangeEvent event) {
    IResourceDelta delta = event.getDelta();
    try {

      final HashSet<IMavenProjectFacade> projects = new HashSet<IMavenProjectFacade>();

      delta.accept(new IResourceDeltaVisitor() {
        public boolean visit(IResourceDelta delta) throws CoreException {
          if(delta.getResource() instanceof IProject) {
            IProject project = (IProject) delta.getResource();
            IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project,
                new NullProgressMonitor());
            if(facade != null && JEEPackaging.getValue(facade.getPackaging()) == JEEPackaging.WAR)
            {
              projects.add(facade);
            }
            return false;
          }
          return true;
        }
      });
      
      final List<IMavenProjectFacade> needsToBeConfigured = new ArrayList<IMavenProjectFacade>(3);
      List<IPath> webXmlLocations = new ArrayList<IPath>(3);

      for(IMavenProjectFacade facade : projects) {
        String webXmlPath = facade.getProject().getPersistentProperty(OverlayConfigurator.WEBXML_PATH);
        if(webXmlPath != null) {
          webXmlLocations.add(new Path(webXmlPath));
        } else {
          needsToBeConfigured.add(facade);
        }
      }

      for(IPath webXmlPath : webXmlLocations) {
        IResourceDelta webXmlChanged = delta.findMember(webXmlPath);
        if(webXmlChanged != null) {
          IProject project = webXmlChanged.getResource().getProject();
          String targetPath = project.getPersistentProperty(OverlayConfigurator.WEBXML_TARGET_PATH);
          final IFolder targetFolder = project.getFolder(targetPath);
          try {
            FileUtils.copyFileIfModified(webXmlChanged.getResource().getLocation().toFile(), new File(targetFolder
                .getLocation().toFile(), "web.xml"));
          } catch(IOException ex) {
            throw new CoreException(new Status(Status.ERROR, MavenWtpPlugin.ID,
                "Cannot copy web.xml to default source root from: " + webXmlPath, ex));
          }
          WorkspaceJob job = new WorkspaceJob("Refresh " + targetFolder.getFullPath().toPortableString()) {

            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                targetFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
                return Status.OK_STATUS;
            }
          };
          job.schedule();
        }
      }

      System.out.println("UPDATE REQUIRED: " + needsToBeConfigured);
      if(needsToBeConfigured.size() > 0) {
        final IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

        WorkspaceJob job = new WorkspaceJob("Updating maven projects ") {

          public IStatus runInWorkspace(IProgressMonitor monitor) {
            try {
              SubMonitor progress = SubMonitor.convert(monitor, "Updating Maven projects", 100);
              SubMonitor subProgress = SubMonitor.convert(progress.newChild(5), needsToBeConfigured.size() * 100);
              for(IMavenProjectFacade facade : needsToBeConfigured) {
                if(progress.isCanceled()) {
                  throw new OperationCanceledException();
                }
                IProject project = facade.getProject();
                subProgress.subTask("Updating configuration for " + project.getName());

                configurationManager.updateProjectConfiguration(project, subProgress);
              }

            } catch(CoreException ex) {
              return ex.getStatus();
            }
            return Status.OK_STATUS;
          }
        };
        job.setRule(configurationManager.getRule());
        job.schedule();
      }
    } catch(Exception ex) {
      // TODO: handle exception
    }
  }

}
