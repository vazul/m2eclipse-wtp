/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.filtering;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.internal.project.GenericBuildParticipant;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.wtp.internal.MavenWtpPlugin;

/**
 * ResourceFilteringBuildParticipant
 *
 * @author Fred Bricon
 */
public class ResourceFilteringBuildParticipant extends GenericBuildParticipant {


  private EclipseBuildContext forceCopyBuildContext; 
  
  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
    
      IMavenProjectFacade facade = getMavenProjectFacade();
      ResourceFilteringConfiguration configuration = ResourceFilteringConfigurationFactory.getConfiguration(facade);
      List<Xpp3Dom> resources = null; 
      if (configuration == null || (resources = configuration.getResources()) == null) {
        //Nothing to filter
        return null;
      }

      IProject project = facade.getProject();
      //FIXME assuming path relative to current project
      IPath targetFolder = configuration.getTargetFolder();      
      IResourceDelta delta =  getDelta(project);

      BuildContext oldBuildContext  = ThreadBuildContext.getContext();
      
      try {
        forceCopyBuildContext = null;
        List<String> filters = configuration.getFilters();
        if (changeRequiresForcedCopy(facade, filters, delta)) {
          log.info("Changed resources require a complete clean of filtered resources of {}",project.getName());
          Map<String, Object> contextState = new HashMap<String, Object>();
          project.setSessionProperty(MavenBuilder.BUILD_CONTEXT_KEY, contextState);
          //String id = ((AbstractEclipseBuildContext)super.getBuildContext()).getCurrentBuildParticipantId();
          forceCopyBuildContext = new EclipseBuildContext(project, contextState);      
          //forceCopyBuildContext.setCurrentBuildParticipantId(id);
          ThreadBuildContext.setThreadBuildContext(forceCopyBuildContext);
        }   
        if (forceCopyBuildContext != null || hasResourcesChanged(facade, delta, resources)) {
          log.info("Executing resource filtering for {}",project.getName());
          executeCopyResources(facade, filters, targetFolder, resources, monitor);  
          //FIXME deal with absolute paths
          IFolder destFolder = project.getFolder(targetFolder);
          if (destFolder.exists()){
            destFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
          }
        }
      } finally {
        ThreadBuildContext.setThreadBuildContext(oldBuildContext);
      }    
    return null;
  }

  protected BuildContext getBuildContext() {
     return (forceCopyBuildContext == null)?super.getBuildContext() : forceCopyBuildContext;
  }

  /**
   * If the pom.xml or any of the project's filters were changed, a forced copy is required 
   * @param facade 
   * @param delta
   * @return
   */
  private boolean changeRequiresForcedCopy(IMavenProjectFacade facade, List<String> filters, IResourceDelta delta) {
    if (delta == null) {
      return false;
    }

    if (delta.findMember(facade.getPom().getProjectRelativePath()) != null ) {
      return true;
    }
    for (String filter : filters) {
      
      IPath filterPath = facade.getProjectRelativePath(filter);
      if (filterPath == null) {
        filterPath =Path.fromOSString(filter); 
      }
      if (delta.findMember(filterPath) != null){
        return true;
      }
    }
    return false;
    
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant#clean(org.eclipse.core.runtime.IProgressMonitor)
   */
  public void clean(IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = getMavenProjectFacade();
    ResourceFilteringConfiguration configuration = ResourceFilteringConfigurationFactory.getConfiguration(facade);
    if (configuration == null) {
      //Nothing to do
      return;
    }

    IProject project = facade.getProject();
    IPath targetFolderPath = configuration.getTargetFolder();
    deleteFilteredResources(project, targetFolderPath);
    super.clean(monitor);
  }
  
  private void deleteFilteredResources(IProject project, IPath targetFolderPath) throws CoreException {
    IFolder targetFolder = project.getFolder(targetFolderPath);
    if (targetFolder.exists()) {
       MavenLogger.log("Cleaning filtered folder for "+project.getName());
      //Can't delete the folder directly as it would also delete the related entry in .component 
      // and user would then need to manually update the maven configuration
      IProgressMonitor nullMonitor = new NullProgressMonitor();
      for (IResource resource : targetFolder.members()) {
        resource.delete(true, nullMonitor);         
      }
      targetFolder.refreshLocal(IResource.DEPTH_INFINITE, nullMonitor); 
    }    
    //super.clean(monitor);
  }

  
  /**
   * @param mavenProject
   * @param iResourceDelta 
   * @param resources
   * @return
   */
  private boolean hasResourcesChanged(IMavenProjectFacade facade, IResourceDelta delta, List<Xpp3Dom> resources) {
    if (resources == null || resources.isEmpty()){
      return false;
    }
      
    Set<IPath> resourcePaths = getResourcePaths(facade, resources);
  
    if(delta == null) {
      return !resourcePaths.isEmpty();
    }
  
    for(IPath resourcePath : resourcePaths) {
      IResourceDelta member = delta.findMember(resourcePath);
      //XXX deal with member kind/flags
      if(member != null) {
          return true; 
          //we need to deal with superceded resources on the maven level
      }
    }
  
    return false;
  }

  
  private Set<IPath> getResourcePaths(IMavenProjectFacade facade, List<Xpp3Dom> resources) {
    Set<IPath> resourcePaths = new LinkedHashSet<IPath>();
    for(Xpp3Dom resource : resources) {
      IPath folder= null;
      Xpp3Dom xpp3Directory = resource.getChild("directory");
      if (xpp3Directory != null)
      {
        String dir = xpp3Directory.getValue();
        if (StringUtils.isNotEmpty(dir)){
          IPath dirPath = new Path(dir);
          folder = dirPath.makeRelativeTo(facade.getProject().getLocation());          
        }
      }
      if(folder != null && !folder.isEmpty()) {
        resourcePaths.add(folder);
      }
    }

    return resourcePaths;
  }
  
  /**
   * @param facade
   * @param filters 
   * @param project 
   * @param targetFolder
   * @param resources
   * @param monitor 
   * @throws CoreException 
   */
  private void executeCopyResources(IMavenProjectFacade facade, IPath targetFolder, List<Xpp3Dom> resources, IProgressMonitor monitor) throws CoreException {

    MavenExecutionPlan executionPlan = facade.getExecutionPlan(monitor);
    MojoExecution copyFilteredResourcesMojo = getExecution(executionPlan, "maven-resources-plugin");

    if (copyFilteredResourcesMojo == null) return;

    Xpp3Dom  configuration = copyFilteredResourcesMojo.getConfiguration();

    //Set resources directories to read

    Xpp3Dom  resourcesNode = configuration.getChild("resources");
    if (resourcesNode==null){
      resourcesNode = new Xpp3Dom("resources");
      configuration.addChild(resourcesNode);
    } else {
      DomUtils.removeChildren(resourcesNode);
    }
    for (Xpp3Dom resource : resources)
    {
      resourcesNode.addChild(resource);
    }
    configuration.addChild(resourcesNode);

    //Force overwrite
    Xpp3Dom  overwriteNode = configuration.getChild("overwrite");
    if (overwriteNode==null){
      overwriteNode = new Xpp3Dom("overwrite");
      configuration.addChild(overwriteNode);
    }
    overwriteNode.setValue(Boolean.TRUE.toString());
    
    //Limit placeholder delimiters
    Xpp3Dom  useDefaultDelimitersNode = configuration.getChild("useDefaultDelimiters");
    if (useDefaultDelimitersNode==null){
      useDefaultDelimitersNode = new Xpp3Dom("useDefaultDelimiters");
      configuration.addChild(useDefaultDelimitersNode);
    }
    useDefaultDelimitersNode.setValue(Boolean.FALSE.toString());

    Xpp3Dom  delimitersNode = configuration.getChild("delimiters");
    if (delimitersNode==null){
      delimitersNode = new Xpp3Dom("delimiters");
      configuration.addChild(delimitersNode);
    } else {
      DomUtils.removeChildren(delimitersNode);
    }
    Xpp3Dom delimiter = new Xpp3Dom("delimiter");
    delimiter.setValue("${*}");
    delimitersNode.addChild(delimiter);
    
    //Set output directory
    Xpp3Dom  outPutDirNode = new Xpp3Dom("outputDirectory");
    outPutDirNode.setValue(targetFolder.toPortableString());
    configuration.addChild(outPutDirNode);
    
    //Setup filters
    if (!filters.isEmpty()) {
      Xpp3Dom  filtersNode = configuration.getChild("filters");
      if (filtersNode==null){
        filtersNode = new Xpp3Dom("filters");
        configuration.addChild(filtersNode);
      } else {
        DomUtils.removeChildren(filtersNode);
      }
      for (String filter : filters) {
        Xpp3Dom filterNode = new Xpp3Dom("filter");
        filterNode.setValue(filter);
        filtersNode.addChild(filterNode );
      }
    }

    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();

    //Create a maven request + session
    IMaven maven = MavenPlugin.getDefault().getMaven();
    ResolverConfiguration resolverConfig = facade.getResolverConfigurat
    
    MavenExecutionRequest request = projectManager.createExecutionRequest(facade.getPom(), resolverConfig, monitor);
    request.setRecursive(false);
    request.setOffline(true);
    MavenSession session = maven.createSession(request, facade.getMavenProject());

    //Execute our hacked mojo 
    copyFilteredResourcesMojo.getMojoDescriptor().setGoal("copy-resources");
    maven.execute(session, copyFilteredResourcesMojo, monitor);
    
    if (session.getResult().hasExceptions()){
      //move exceptions up to the original session, so they can be handled by the maven builder
      //XXX current exceptions refer to maven-resource-plugin (since that's what we used), we should probably 
      // throw a new exception instead to indicate the problem(s) come(s) from web resource filtering
      for(Throwable t : session.getResult().getExceptions())
      {
        getSession().getResult().addException(t);    
      }
    }
    
  }

  
  private MojoExecution getExecution(MavenExecutionPlan executionPlan, String artifactId) throws CoreException {
    for(MojoExecution execution : getMojoExecutions(executionPlan)) {
      if(artifactId.equals(execution.getArtifactId()) ) {
        return execution;
      }
    }
    return null;
  }

  private Collection<MojoExecution> getMojoExecutions(MavenExecutionPlan executionPlan) throws CoreException {
    Collection<MojoExecution> mojoExecutions;
    try {
      mojoExecutions = executionPlan.getMojoExecutions();
    } catch (NoSuchMethodError nsme) {
      //Support older versions of m2eclipse-core (pre Maven 3 era)
      try {
        Method getExecutionsMethod = MavenExecutionPlan.class.getMethod("getExecutions");
        mojoExecutions = (Collection<MojoExecution>) getExecutionsMethod.invoke(executionPlan);
      } catch(Exception e) {
        IStatus status = new Status(IStatus.ERROR, MavenWtpPlugin.ID, IStatus.ERROR, e.getMessage(), e);
        throw new CoreException(status);
      }
    }
    return mojoExecutions;
  }

  public static ResourceFilteringBuildParticipant getParticipant(MojoExecution execution) {
    if ("maven-war-plugin".equals(execution.getArtifactId()) && "war".equals(execution.getGoal()))
    {
      return new ResourceFilteringBuildParticipant(); 
    }
    return null;
  }
}
