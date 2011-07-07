/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.mavenarchiver;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.maven.ide.eclipse.wtp.MavenWtpConstants;
import org.maven.ide.eclipse.wtp.ProjectUtils;
import org.sonatype.m2e.mavenarchiver.internal.JarArchiverConfigurator;

/**
 * WarMavenArchiverConfigurator
 *
 * @author Fred Bricon
 */
public class WarMavenArchiverConfigurator extends JarArchiverConfigurator {

  //private static final Logger log = LoggerFactory.getLogger(WarMavenArchiverConfigurator.class);

  static final IClasspathAttribute NONDEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY, "");
  
  @Override
  protected IPath getOutputDir(IMavenProjectFacade facade) {
    IProject project = facade.getProject();
    IPath localEarResourceFolder =  ProjectUtils.getM2eclipseWtpFolder(facade.getMavenProject(), project);
    return project.getFullPath().append(localEarResourceFolder)
                                .append(MavenWtpConstants.WEB_RESOURCES_FOLDER);
  }
  
  @Override
  protected String getArchiverFieldName() {
    return "warArchiver";
  }
  
  @Override
  protected MojoExecutionKey getExecutionKey() {
    MojoExecutionKey key = new MojoExecutionKey("org.apache.maven.plugins", "maven-war-plugin", "", "war", null, null);
    return key;
  }

  @Override
  protected boolean needsNewManifest(IFile manifest, IMavenProjectFacade oldFacade, IMavenProjectFacade newFacade,
      IProgressMonitor monitor) {

    if (!ModuleCoreNature.isFlexibleProject(newFacade.getProject())) {
      return false;
    }
    return super.needsNewManifest(manifest, oldFacade, newFacade, monitor);
  }
}
