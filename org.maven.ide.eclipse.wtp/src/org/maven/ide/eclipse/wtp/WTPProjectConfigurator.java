/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.maven.ide.eclipse.wtp.filtering.ResourceFilteringBuildParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Project configurator for WTP projects. Specific project configuration is delegated to the
 * IProjectConfiguratorDelegate bound to a maven packaging type.
 * 
 * @author Igor Fedorenko
 */
public class WTPProjectConfigurator extends AbstractProjectConfigurator implements IJavaProjectConfigurator {

  private static final Logger LOG = LoggerFactory.getLogger(WTPProjectConfigurator.class); 
      
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = request.getMavenProject();
    //Lookup the project configurator 
    IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
        .getProjectConfiguratorDelegate(mavenProject.getPackaging());
    if(configuratorDelegate != null) {
      IProject project = request.getProject();
      if (project.getResourceAttributes().isReadOnly()){
        return;
      }

      try {
        configuratorDelegate.configureProject(project, mavenProject, monitor);
      } catch(MarkedException ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = event.getMavenProject();

    if(facade != null) {
      IProject project = facade.getProject();
      if (project.getResourceAttributes().isReadOnly()){
        return;
      }

      if(isWTPProject(project)) {
        MavenProject mavenProject = facade.getMavenProject(monitor);
        IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
            .getProjectConfiguratorDelegate(mavenProject.getPackaging());
        if(configuratorDelegate != null) {
          configuratorDelegate.setModuleDependencies(project, mavenProject, monitor);
        }
      }
    }
  }

  protected static boolean isWTPProject(IProject project) {
    return ModuleCoreNature.getModuleCoreNature(project) != null;
  }

  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);
    //Lookup the project configurator 
    IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
        .getProjectConfiguratorDelegate(mavenProject.getPackaging());
    if(configuratorDelegate != null) {
      IProject project = facade.getProject();
      try {
        configuratorDelegate.configureClasspath(project, mavenProject, classpath, monitor);
      } catch(CoreException ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
  }

  public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    // we do not change raw project classpath, do we? 
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator#getBuildParticipant(org.apache.maven.plugin.MojoExecution)
   */  
  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    
    //FIXME that's ugly. should refactor that by removing the project configurator delegates
      if ("maven-war-plugin".equals(execution.getArtifactId()) && "war".equals(execution.getGoal()) 
        || "maven-ear-plugin".equals(execution.getArtifactId()) && "generate-application-xml".equals(execution.getGoal())
        || "maven-acr-plugin".equals(execution.getArtifactId()) && "acr".equals(execution.getGoal()))
      {
        return new ResourceFilteringBuildParticipant(); 
      }
      return null;
  }
  
}
