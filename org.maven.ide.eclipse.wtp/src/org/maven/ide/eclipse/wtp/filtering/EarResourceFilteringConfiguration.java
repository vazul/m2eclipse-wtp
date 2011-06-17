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
import org.maven.ide.eclipse.wtp.EarPluginConfiguration;
import org.maven.ide.eclipse.wtp.MavenWtpConstants;
import org.maven.ide.eclipse.wtp.ProjectUtils;

/**
 * EarResourceFilteringConfiguration
 *
 * @author Fred Bricon
 */
public class EarResourceFilteringConfiguration extends AbstractResourceFilteringConfiguration {

  private EarPluginConfiguration pluginConfiguration;
  
  public EarResourceFilteringConfiguration(IMavenProjectFacade mavenProjectFacade) {
    super(mavenProjectFacade);
    pluginConfiguration = new EarPluginConfiguration(mavenProjectFacade.getMavenProject());
  }

  public IPath getTargetFolder() {
    return getTargetFolder(mavenProjectFacade.getMavenProject(), mavenProjectFacade.getProject());
  }

  public static IPath getTargetFolder(MavenProject mavenProject, IProject project) {
    return ProjectUtils.getM2eclipseWtpFolder(mavenProject, project).append(MavenWtpConstants.EAR_RESOURCES_FOLDER);
  }

  public List<Xpp3Dom> getResources() {
    if (!pluginConfiguration.isFiltering()) {
      return null;
    }
    String earContentDir = pluginConfiguration.getEarContentDirectory(mavenProjectFacade.getProject());
    Xpp3Dom resource = new Xpp3Dom("resource");
    Xpp3Dom directory = new Xpp3Dom("directory");
    directory.setValue(earContentDir);
    resource.addChild(directory);
    Xpp3Dom filter = new Xpp3Dom("filtering");
    filter.setValue(Boolean.TRUE.toString());
    resource.addChild(filter);
    
    return Arrays.asList(resource);
  }

  public List<String> getFilters() {
    List<String> filters = new ArrayList<String>(mavenProjectFacade.getMavenProject().getFilters());
    filters.addAll(pluginConfiguration.getEarFilters());
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
