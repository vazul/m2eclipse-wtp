/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.ide.filesystem.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.project.IMavenMarkerManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.wtp.earmodules.EarModule;


/**
 * Deployment Descriptor Management based on maven-ear-plugin
 * 
 * @author Fred Bricon
 * @author Snjezana Peco
 */
public class MavenDeploymentDescriptorManagement implements DeploymentDescriptorManagement {

  private final VersionRange VALID_EAR_PLUGIN_RANGE = VersionRange.createFromVersion("2.4.3");

  org.eclipse.jst.common.internal.modulecore.AddClasspathLibReferencesParticipant b;
  private static final IOverwriteQuery OVERWRITE_ALL_QUERY = new IOverwriteQuery() {
    public String queryOverwrite(String pathString) {
      return IOverwriteQuery.ALL;
    }
  };

  /**
   * Executes ear:generate-application-xml goal to generate application.xml (and jboss-app.xml if needed). Existing
   * files will be overwritten.
   * 
   * @throws CoreException
   */

  public void updateConfiguration(IProject project, MavenProject mavenProject, EarPluginConfiguration plugin,
     boolean useBuildDirectory, IProgressMonitor monitor) throws CoreException {

    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();

    IMavenProjectFacade mavenFacade = projectManager.getProject(project);
    MavenExecutionPlan executionPlan = mavenFacade.getExecutionPlan(monitor);
    MojoExecution genConfigMojo = getExecution(executionPlan, "maven-ear-plugin", "generate-application-xml");
    if(genConfigMojo == null) {
      //TODO Better error management
      return;
    }
    
    //Let's force the generated config files location
    Xpp3Dom configuration = genConfigMojo.getConfiguration();
    if(configuration == null) {
      configuration = new Xpp3Dom("configuration");
      genConfigMojo.setConfiguration(configuration);
    }
    
    File generatedDescriptorLocation;
    try {
      generatedDescriptorLocation = getTempDirectory();
    } catch(IOException ex) {
      IStatus status = new Status(IStatus.ERROR, MavenWtpPlugin.ID, ex.getLocalizedMessage(), ex);
      throw new CoreException(status);
    }
    Xpp3Dom genDescriptorLocationDom = configuration.getChild("generatedDescriptorLocation");
    if(genDescriptorLocationDom == null) {
      genDescriptorLocationDom = new Xpp3Dom("generatedDescriptorLocation");
      configuration.addChild(genDescriptorLocationDom);
    }
    genDescriptorLocationDom.setValue(generatedDescriptorLocation.getAbsolutePath());

    // Fix for http://jira.codehaus.org/browse/MEAR-116?focusedCommentId=232316&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_232316
    // affecting maven-ear-plugin version < 2.4.3
    if(!VALID_EAR_PLUGIN_RANGE.containsVersion(new DefaultArtifactVersion(genConfigMojo.getVersion()))) {
      overrideModules(configuration, plugin.getEarModules());
    }

    //Create a maven request + session
    IMaven maven = MavenPlugin.getDefault().getMaven();
    IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);

    //TODO check offline behavior, profiles
    MavenExecutionRequest request = projectManager.createExecutionRequest(pomResource,
        mavenFacade.getResolverConfiguration(), monitor);
    MavenSession session = maven.createSession(request, mavenProject);

    //Execute our hacked mojo 
    maven.execute(session, genConfigMojo, monitor);
    
    if (session.getResult().hasExceptions()){
      IMavenMarkerManager markerManager  = MavenPlugin.getDefault().getMavenMarkerManager();
      markerManager.addMarkers(mavenFacade.getPom(), session.getResult());
    }
    
    //Copy generated files to their final location
    File[] files = generatedDescriptorLocation.listFiles();

    //MECLIPSEWTP-56 : application.xml should not be generated in the source directory
    
    IFolder targetFolder;
    IFolder earResourcesFolder = getEarResourcesDir(project, mavenProject, monitor); 
    if (useBuildDirectory) {
      targetFolder = earResourcesFolder;
    } else {
      targetFolder = project.getFolder(plugin.getEarContentDirectory(project));

      if (earResourcesFolder.exists() && earResourcesFolder.isAccessible()) {
        earResourcesFolder.delete(true, monitor);
      }
    }
    
    IFolder metaInfFolder = targetFolder.getFolder("/META-INF/");

    if(files.length > 0) {
      //We generated something
      try {
        ImportOperation op = new ImportOperation(metaInfFolder.getFullPath(), generatedDescriptorLocation,
            new FileSystemStructureProvider(), OVERWRITE_ALL_QUERY, Arrays.asList(files));
        op.setCreateContainerStructure(false);
        op.setOverwriteResources(true);
        
        op.run(monitor);
        
      } catch(InvocationTargetException ex) {
        IStatus status = new Status(IStatus.ERROR, MavenWtpPlugin.ID, IStatus.ERROR, ex.getMessage(), ex);
        throw new CoreException(status);
      } catch(InterruptedException ex) {
        throw new OperationCanceledException(ex.getMessage());
      }
    } 
    targetFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    
    deleteDirectory(generatedDescriptorLocation);    
  }

  private IFolder getEarResourcesDir(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    String appResourcesDir = ProjectUtils.getM2eclipseWtpFolder(mavenProject, project).toPortableString()+Path.SEPARATOR+MavenWtpConstants.EAR_RESOURCES_FOLDER;
    IFolder appResourcesFolder = project.getFolder(appResourcesDir);
 
    if (!appResourcesFolder.exists()) {
      ProjectUtils.createFolder(appResourcesFolder, monitor);
    }
    if (!appResourcesFolder.isDerived()) {
      appResourcesFolder.setDerived(true);//TODO Eclipse < 3.6 doesn't support setDerived(bool, monitor)
    }
    return appResourcesFolder;
  }

  private void overrideModules(Xpp3Dom configuration, Set<EarModule> earModules) {
    Xpp3Dom modules = configuration.getChild("modules");
    if(modules == null) {
      modules = new Xpp3Dom("modules");
      configuration.addChild(modules);
    }
    //TODO find a more elegant way to clear the modules  
    while(modules.getChildCount() > 0) {
      modules.removeChild(0);
    }
    //Recreate the module's children, forcing the uri.
    for(EarModule earModule : earModules) {
      modules.addChild(earModule.getAsDom());
    }
  }

  private File getTempDirectory() throws IOException {
    File tempDir = new File(System.getProperty("java.io.tmpdir"));
    File dir = new File(tempDir, ".mavenDeploymentDescriptorManagement");
    if(dir.exists()) {
      if(dir.isFile()) {
        if(!dir.delete()) {
          throw new IOException("Could not delete temp file: " + dir.getAbsolutePath());
        } else {
          if(!deleteDirectory(dir)) {
            throw new IOException("Could not delete temp file: " + dir.getAbsolutePath());
          }
        }
      }
    }
    dir.mkdir();
    return dir;
  }

  private static boolean deleteDirectory(File path) {
    if(path.exists()) {
      File[] files = path.listFiles();
      for(int i = 0; i < files.length; i++ ) {
        if(files[i].isDirectory()) {
          deleteDirectory(files[i]);
        } else {
          files[i].delete();
        }
      }
    }
    return (path.delete());
  }

  private MojoExecution getExecution(MavenExecutionPlan executionPlan, String artifactId, String goal) throws CoreException {
    for(MojoExecution execution : getMojoExecutions(executionPlan)) {
      if(artifactId.equals(execution.getArtifactId()) && goal.equals(execution.getGoal())) {
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

}
