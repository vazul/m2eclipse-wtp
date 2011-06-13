/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See http://maven.apache.org/plugins/maven-acr-plugin/acr-mojo.html
 * 
 * @author Fred Bricon
 */
class AcrPluginConfiguration {
  
  private static final Logger log = LoggerFactory.getLogger(AcrPluginConfiguration.class); 

  private static final IProjectFacetVersion DEFAULT_APPCLIENT_FACET_VERSION = IJ2EEFacetConstants.APPLICATION_CLIENT_50;
  
  final Plugin plugin;
 
  final MavenProject mavenProject;
  
  public AcrPluginConfiguration(MavenProject mavenProject) {

    if (JEEPackaging.APP_CLIENT != JEEPackaging.getValue(mavenProject.getPackaging()))
      throw new IllegalArgumentException("Maven project must have app-client packaging");
    
    this.mavenProject = mavenProject;
    this.plugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-acr-plugin");
  }

  public IProjectFacetVersion getFacetVersion() {
    return DEFAULT_APPCLIENT_FACET_VERSION; 
  }
  
  /**
   * @return the first resource location directory declared in pom.xml
   */
  public String getContentDirectory(IProject project) {
    IPath[] resources = MavenProjectUtils.getResourceLocations(project, mavenProject.getResources());
    return resources[0].toPortableString();
  }
  
}
