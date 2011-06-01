/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.manifest;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.maven.ide.eclipse.wtp.MavenWtpConstants;
import org.maven.ide.eclipse.wtp.ProjectUtils;

/**
 * WarManifestConfigurator
 *
 * @author Fred Bricon
 */
public class WarManifestConfigurator extends JarManifestConfigurator {

  protected IPath getManifestdir(IMavenProjectFacade facade) {
    IProject project = facade.getProject();
    IPath localEarResourceFolder =  ProjectUtils.getM2eclipseWtpFolder(facade.getMavenProject(), project);
    return project.getFullPath().append(localEarResourceFolder)
                                .append(MavenWtpConstants.WEB_RESOURCES_FOLDER)
                                .append("META-INF");
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.manifest.AbstractManifestConfigurator#getArchiverFieldName()
   */
  protected String getArchiverFieldName() {
    return "warArchiver";
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.manifest.AbstractManifestConfigurator#getExecutionKey()
   */
  protected MojoExecutionKey getExecutionKey() {
    MojoExecutionKey key = new MojoExecutionKey("org.apache.maven.plugins", "maven-war-plugin", "", "war", null, null);
    return key;
  }
}
