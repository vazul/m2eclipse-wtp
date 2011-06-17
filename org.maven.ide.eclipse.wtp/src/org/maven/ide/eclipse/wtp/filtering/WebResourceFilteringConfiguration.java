/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.filtering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.maven.ide.eclipse.wtp.MavenWtpConstants;
import org.maven.ide.eclipse.wtp.ProjectUtils;
import org.maven.ide.eclipse.wtp.WarPluginConfiguration;

/**
 * WebResourceFilteringConfiguration
 *
 * @author Fred Bricon
 */
public class WebResourceFilteringConfiguration extends AbstractResourceFilteringConfiguration {

  private WarPluginConfiguration pluginConfiguration;
  
  public WebResourceFilteringConfiguration(IMavenProjectFacade mavenProjectFacade) {
    super(mavenProjectFacade);
    pluginConfiguration = new WarPluginConfiguration(mavenProjectFacade.getMavenProject(), mavenProjectFacade.getProject());
  }

  public IPath getTargetFolder() {
    return getTargetFolder(mavenProjectFacade.getMavenProject(), mavenProjectFacade.getProject());
  }

  public static IPath getTargetFolder(MavenProject mavenProject, IProject project) {
    return ProjectUtils.getM2eclipseWtpFolder(mavenProject, project).append(MavenWtpConstants.WEB_RESOURCES_FOLDER);
  }

  public List<Xpp3Dom> getResources() {
    Xpp3Dom[] domResources = pluginConfiguration.getWebResources();
    if(domResources == null || domResources.length == 0){
      return Collections.emptyList();
    }
    return Arrays.asList(domResources);
  }

  public List<String> getFilters() {
    List<String> filters = new ArrayList<String>(mavenProjectFacade.getMavenProject().getFilters());
    filters.addAll(pluginConfiguration.getWebResourcesFilters());
    return filters;
  }

  public String getEscapeString() {
    return pluginConfiguration.getEscapeString();
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.filtering.ResourceFilteringConfiguration#getNonfilteredExtensions()
   */
  public List<Xpp3Dom> getNonfilteredExtensions() {
    Xpp3Dom[] domext = pluginConfiguration.getNonfilteredExtensions();
    if(domext == null || domext.length == 0){
      return Collections.emptyList();
    }
    return Arrays.asList(domext);
  }
  
}
