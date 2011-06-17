/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * ProjectUtils
 *
 * @author Fred Bricon
 */
public class ProjectUtils {

  /**
   * Transform an absolute path into a relative path to a project, if possible
   * @param project
   * @param absolutePath : relative path to the project
   * @return
   */
  public static String getRelativePath(IProject project, String absolutePath){
	//Code copied from org.maven.ide.eclipse.jdt.internal.AbstractJavaProjectConfigurator 
	//since Path.makeRelativeTo() doesn't work on Linux
    File basedir = project.getLocation().toFile();
    String relative;
    if(absolutePath.equals(basedir.getAbsolutePath())) {
      relative = ".";
    } else if(absolutePath.startsWith(basedir.getAbsolutePath())) {
      relative = absolutePath.substring(basedir.getAbsolutePath().length() + 1);
    } else {
      relative = absolutePath;
    }
    return relative.replace('\\', '/'); //$NON-NLS-1$ //$NON-NLS-2$
  }
  
  /*
  public static IPath getM2eclipseWtpFolder(IMavenProjectFacade facade) {
    return getM2eclipseWtpFolder(facade.getMavenProject(), facade.getProject());
  }
  */
  
  /**
   * @param mavenProject
   * @param project
   * @return
   */
  public static IPath getM2eclipseWtpFolder(MavenProject mavenProject, IProject project) {
    String buildOutputDir = mavenProject.getBuild().getDirectory();
    String relativeBuildOutputDir = getRelativePath(project, buildOutputDir);
    return new Path(relativeBuildOutputDir).append(MavenWtpConstants.M2E_WTP_FOLDER);
  }

  /**
   * @param project
   * @throws CoreException 
   */
  public static void hideM2eclipseWtpFolder(MavenProject mavenProject, IProject project) throws CoreException {
    IPath m2eclipseWtpPath = getM2eclipseWtpFolder(mavenProject, project);
    IFolder folder = project.getFolder(m2eclipseWtpPath);
    if (folder.exists()) {
      IProgressMonitor monitor = new NullProgressMonitor();
      if (!folder.isDerived()) {
        folder.setDerived(true);//TODO Eclipse < 3.6 doesn't support setDerived(bool, monitor)
      }
      if (!folder.isHidden()) {
        folder.setHidden(true);
      }
      folder.getParent().refreshLocal(IResource.DEPTH_ZERO,monitor);
    }
  }
  
  public static void createFolder(IFolder folder, IProgressMonitor monitor) throws CoreException {
    if (folder == null || folder.exists()) {
      return;
    }
    IContainer parent = folder.getParent();
    if (parent instanceof IFolder) {
      createFolder((IFolder)parent, monitor);
    }
    folder.create(true, true, monitor);
  }
  
  public static void removeNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException {
    if (project.hasNature(natureId)) {
      IProjectDescription description = project.getDescription();
      String[] prevNatures = description.getNatureIds();
      String[] newNatures = new String[prevNatures.length - 1];
      for (int i=0, j = 0 ; i < prevNatures.length; i++) {
        if (!prevNatures[i].equals(natureId)) {
          newNatures[j++] = prevNatures[i];
        }
      }
      description.setNatureIds(newNatures);
      project.setDescription(description, monitor);
    }
  }
  
  public static IClasspathEntry[] getEarContainerEntries(IJavaProject javaProject) {
    IClasspathEntry[] entries = null;
    if(javaProject != null) {
      try {
        IClasspathContainer earContainer = 
            JavaCore.getClasspathContainer(new Path("org.eclipse.jst.j2ee.internal.module.container"), javaProject);
        if (earContainer != null) {
          entries = earContainer.getClasspathEntries();
        }
      } catch(JavaModelException ex) {
        //Ignore
      }
    }
    return entries;
  }
  
}
