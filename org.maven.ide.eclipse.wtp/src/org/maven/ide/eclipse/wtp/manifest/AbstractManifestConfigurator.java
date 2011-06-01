/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.manifest;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

/**
 * AbstractManifestConfigurator
 *
 * @author Fred Bricon
 */
public abstract class AbstractManifestConfigurator extends AbstractProjectConfigurator implements IManifestConfigurator {

  private static final String MAVEN_ARCHIVER = "org.apache.maven.archiver.MavenArchiver";

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    //Nothing to configure
  }

  public AbstractBuildParticipant getBuildParticipant(final IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    
    MojoExecutionKey key = getExecutionKey();
    if (execution.getArtifactId().equals(key.getArtifactId()) 
      && execution.getGoal().equals(key.getGoal())) {

      return new AbstractBuildParticipant() {
        public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
          //Will basically only generate the MANIFEST.MF if it doesn't exist
          mavenProjectChanged(projectFacade, null, monitor);
          return null;
        }
      };
    }
    return null;
  }
    
  /**
   * Generates the project manifest if necessary, that is if the project manifest configuration has changed or if the dependencies have changed.
   */
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    
    IMavenProjectFacade oldFacade = event.getOldMavenProject(); 
    IMavenProjectFacade newFacade = event.getMavenProject();
    if (oldFacade == null && newFacade == null) {
      return;
    }
    mavenProjectChanged(newFacade, oldFacade, monitor);
  }

  public void mavenProjectChanged(IMavenProjectFacade newFacade, 
                                  IMavenProjectFacade oldFacade, 
                                  IProgressMonitor monitor) throws CoreException {
    
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFolder output = root.getFolder(getManifestdir(newFacade));
    IFile manifest = output.getFile("MANIFEST.MF");
    
    if (needsNewManifest(manifest, oldFacade, newFacade, monitor)) {
        generateManifest(newFacade, manifest, monitor);
    }
  }
  
  /**
   * @param manifest 
   * @param oldFacade
   * @param newFacade
   * @param monitor
   * @return
   */
  private boolean needsNewManifest(IFile manifest, IMavenProjectFacade oldFacade, IMavenProjectFacade newFacade,
      IProgressMonitor monitor) {
    
    if (!manifest.exists()) {
      return true;
    }
    //Can't compare to a previous state, so assuming it's unchanged
    if (oldFacade == null) {
      return false;
    }
    
    MavenProject newProject = newFacade.getMavenProject();
    MavenProject oldProject = oldFacade.getMavenProject();
    
    //Assume Sets of artifacts are actually ordered
    if (dependenciesChanged(new ArrayList<Artifact>(oldProject.getArtifacts()), 
                            new ArrayList<Artifact>(newProject.getArtifacts()))) {
      return true;
    }
    
    Xpp3Dom oldArchiveConfig = getArchiveConfiguration(oldProject);
    Xpp3Dom newArchiveConfig = getArchiveConfiguration(newProject);
    
    if (newArchiveConfig != null && !newArchiveConfig.equals(oldArchiveConfig)
        || oldArchiveConfig != null && newArchiveConfig == null) {
      return true;
    }
    
    //Name always not null
    if (!newProject.getName().equals(oldProject.getName())) {
        return true;
    }
    
    String oldOrganizationName = oldProject.getOrganization() == null? null :
                                                                       oldProject.getOrganization().getName();
    String newOrganizationName = newProject.getOrganization() == null? null :
                                                                       newProject.getOrganization().getName();
    
    if (newOrganizationName != null && !newOrganizationName.equals(oldOrganizationName) 
      || oldOrganizationName != null && newOrganizationName == null) {
      return true;
    }
    return false;
  }

  /**
   * @param facade 
   * @return
   */
  protected abstract IPath getManifestdir(IMavenProjectFacade facade);

  /**
   * @param artifacts
   * @param others
   * @return
   */
  private boolean dependenciesChanged(List<Artifact> artifacts, List<Artifact> others) {
    if (artifacts.equals(others)) {
      return false;
    }
    if (artifacts.size() != others.size()) {
      return true;
    }
    for (int i= 0; i < artifacts.size(); i++) {
      Artifact dep = artifacts.get(i);
      Artifact dep2 = others.get(i);
      if (!areEqual(dep, dep2)) {
        return true;
      }

    }
    return false;
  }
  
  @SuppressWarnings("null")
  private boolean areEqual(Artifact dep, Artifact other) {
    if (dep == other) {
      return true;
    }
    if (dep == null && other != null 
     || dep != null && other == null) {
        return false;
    }
    //So both artifacts are not null here.
    //Fast (to type) and easy way to compare artifacts. 
    //Proper solution would not rely on internal implementation of toString
    if (dep.toString().equals(other.toString()) && dep.isOptional() == other.isOptional()){
      return true;
    }
    return false;
  }
  /**
   * @param mavenProject
   * @return
   */
  protected Xpp3Dom getArchiveConfiguration(MavenProject mavenProject) {
    Plugin plugin = mavenProject.getPlugin(getPluginKey());
    if (plugin == null) return null;
    
    Xpp3Dom pluginConfig = (Xpp3Dom)plugin.getConfiguration();
    if (pluginConfig == null) {
      return null;
    }
    return pluginConfig.getChild("archive");
  }

  protected MavenSession getMavenSession(IMavenProjectFacade mavenFacade, IProgressMonitor monitor) throws CoreException {
    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    IMaven maven = MavenPlugin.getMaven();
    //Create a maven request + session
    IFile pomResource = mavenFacade.getPom();
    MavenExecutionRequest request = projectManager.createExecutionRequest(pomResource, 
                                                                          mavenFacade.getResolverConfiguration(),
                                                                          monitor);
    return maven.createSession(request, mavenFacade.getMavenProject());
  }
  
  private void reflectManifestGeneration(MavenProject mavenProject, 
                                         MojoExecution mojoExecution, 
                                         MavenSession session, 
                                         File manifestFile) throws CoreException {
    
    ClassLoader loader = null;
    Class<? extends Mojo> mojoClass;
    Mojo mojo = null; 
    
    
    //TODO? hackWorkspaceArtifacts(mavenProject);
    
    try {
      mojo = maven.getConfiguredMojo(session, mojoExecution, Mojo.class );
      mojoClass = mojo.getClass();
      loader = mojoClass.getClassLoader();
      Field archiverField = findField(getArchiverFieldName(), mojoClass);
      archiverField.setAccessible(true);
      Object archiver = archiverField.get(mojo);
      
      Field archiveConfigurationField = findField(getArchiveConfigurationFieldName(), mojoClass);
      archiveConfigurationField.setAccessible(true);
      Object archiveConfiguration = archiveConfigurationField.get(mojo);
      Object mavenArchiver = getMavenArchiver(archiver, manifestFile, loader);

      PrintWriter printWriter = null;
      try
      {
        Method getManifest = mavenArchiver.getClass().getMethod("getManifest", 
                                                                MavenProject.class, 
                                                                archiveConfiguration.getClass());
        Object manifest = getManifest.invoke(mavenArchiver, mavenProject, archiveConfiguration);
        Method write = manifest.getClass().getMethod("write", PrintWriter.class);
        printWriter = new PrintWriter( WriterFactory.newWriter( manifestFile, WriterFactory.UTF_8 ) );
        write.invoke(manifest, printWriter );
      }
      finally
      {
        if (printWriter != null) {
          printWriter.close();
        }
      }
    } catch(Throwable ex) {
      ex.printStackTrace();
    } finally {
      if (mojo != null) {
        maven.releaseMojo(mojo, mojoExecution);
      }
    }
  }


  private Field findField(String name, Class<?> clazz) {
    return ReflectionUtils.getFieldByNameIncludingSuperclasses(name, clazz);
  }
  
  private Object getMavenArchiver(Object archiver, File manifestFile, ClassLoader loader) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
    Class<Object> mavenArchiverClass = (Class<Object>) Class.forName(MAVEN_ARCHIVER, false, loader);
    Object mavenArchiver  = mavenArchiverClass.newInstance();
    
    Method setArchiver = null;
    for (Method m : mavenArchiver.getClass().getMethods()) {
      if ("setArchiver".equals(m.getName())) {
        setArchiver = m;
        break;
      }
    }
    
    //Method setArchiver = mavenArchiverClass.getMethod("setArchiver", archiver.getClass());
    setArchiver.invoke(mavenArchiver, archiver);
    Method setOutputFile = mavenArchiverClass.getMethod("setOutputFile", File.class);
    setOutputFile.invoke(mavenArchiver, manifestFile);
    return mavenArchiver;
  }

  public void generateManifest(IMavenProjectFacade mavenFacade, IFile manifest, IProgressMonitor monitor) throws CoreException {
    
    MavenSession session = getMavenSession(mavenFacade, monitor);
    MavenProject mavenProject = mavenFacade.getMavenProject();
    MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(session, 
                                                                    mavenProject, 
                                                                    Collections.singletonList("package"), 
                                                                    true, 
                                                                    monitor);
    MojoExecution mojoExecution = getExecution(executionPlan, getExecutionKey());
    if (mojoExecution == null) {
      return;
    }

    IFolder destinationFolder = (IFolder)manifest.getParent();
    
    File manifestDir = new File(destinationFolder.getRawLocation().toOSString());
    if ( !manifestDir.exists() )
    {
        manifestDir.mkdirs();
    }
    
    reflectManifestGeneration(mavenProject, mojoExecution, session, new File(manifest.getLocation().toOSString()));
    
    destinationFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
  }

  protected abstract MojoExecutionKey getExecutionKey();
  protected abstract String getArchiveConfigurationFieldName();
  protected abstract String getArchiverFieldName();
  
  protected String getPluginKey() {
    MojoExecutionKey execution = getExecutionKey();
    return execution.getGroupId()+":"+execution.getArtifactId();
  }
  
  private MojoExecution getExecution(MavenExecutionPlan executionPlan, MojoExecutionKey key) {
    for(MojoExecution execution : executionPlan.getMojoExecutions()) {
      if(key.getArtifactId().equals(execution.getArtifactId()) 
      && key.getGroupId().equals(execution.getGroupId())
      && key.getGoal().equals(execution.getGoal())) {
        return execution;
      }
    }
    return null;
  }
}