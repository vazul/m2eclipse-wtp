
/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.earcreation.IEarFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.internal.earcreation.EarFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.j2ee.model.IEARModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.application.Application;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.wtp.earmodules.EarModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Configures Ear projects from maven-ear-plugin.
 * 
 * @see org.eclipse.jst.j2ee.ui.AddModulestoEARPropertiesPage
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
class EarProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  private static final Logger log = LoggerFactory.getLogger(EarProjectConfiguratorDelegate.class);
  
  protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    
    monitor.setTaskName("Configuring EAR project " + project.getName());
    
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

    EarPluginConfiguration config = new EarPluginConfiguration(mavenProject);
    Set<Action> actions = new LinkedHashSet<Action>();
    // WTP doesn't allow facet versions changes for JEE facets
    String contentDir = config.getEarContentDirectory(project);
  
    IFolder firstInexistentfolder = null;
    IFolder contentFolder = project.getFolder(contentDir);
    IFile manifest = contentFolder.getFile("META-INF/MANIFEST.MF");
    boolean manifestAlreadyExists =manifest.exists(); 
    if (!manifestAlreadyExists) {
      firstInexistentfolder = findFirstInexistentFolder(project, contentFolder, manifest);
    }   

    IProjectFacetVersion earFv = config.getEarFacetVersion();
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.EAR_FACET)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, earFv, getEarModel(contentDir)));
    } else {
      //MECLIPSEWTP-37 : don't uninstall the EAR Facet, as it causes constraint failures when used with RAD
      IProjectFacetVersion projectFacetVersion = facetedProject.getProjectFacetVersion(WTPProjectsUtil.EAR_FACET);     
      if(earFv.getVersionString() != null && !earFv.getVersionString().equals(projectFacetVersion.getVersionString())){
          actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, earFv, getEarModel(contentDir)));
      } 
    }
    
    if(!actions.isEmpty()) {
      facetedProject.modify(actions, monitor);
    }

    //MECLIPSEWTP-41 Fix the missing moduleCoreNature
    fixMissingModuleCoreNature(project, monitor);
    
    IVirtualComponent earComponent = ComponentCore.createComponent(project);
    IPath contentDirPath = new Path((contentDir.startsWith("/"))?contentDir:"/"+contentDir);
    //Ensure the EarContent link has been created
    if (!WTPProjectsUtil.hasLink(project, ROOT_PATH, contentDirPath, monitor)) {
      earComponent.getRootFolder().createLink(contentDirPath, IVirtualResource.NONE, monitor);
    }
    WTPProjectsUtil.setDefaultDeploymentDescriptorFolder(earComponent.getRootFolder(), contentDirPath, monitor);

    //MECLIPSEWTP-56 : application.xml should not be generated in the source directory
    boolean useBuildDirectory = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().getPreferences(project).isApplicationXmGeneratedInBuildDirectory();

    if (!manifestAlreadyExists && manifest.exists()) {
      manifest.delete(true, monitor);
    }
    
    List<IPath> sourcePaths = new ArrayList<IPath>();
    sourcePaths.add(contentDirPath);
    
    if (useBuildDirectory && earComponent != null) {
      IPath m2eclipseWtpFolderPath = new Path("/").append(ProjectUtils.getM2eclipseWtpFolder(mavenProject, project));
      ProjectUtils.hideM2eclipseWtpFolder(mavenProject, project);
      IPath generatedResourcesPath = m2eclipseWtpFolderPath.append(Path.SEPARATOR+MavenWtpConstants.EAR_RESOURCES_FOLDER);
      sourcePaths.add(generatedResourcesPath);
      if (!WTPProjectsUtil.hasLink(project, ROOT_PATH, generatedResourcesPath, monitor)) {
        WTPProjectsUtil.insertLinkBefore(project, generatedResourcesPath, contentDirPath, ROOT_PATH, monitor);      
      }

      if (firstInexistentfolder != null && firstInexistentfolder.exists())
      {
        firstInexistentfolder.delete(true, monitor);
      }
     }

    //MECLIPSEWTP-161 remove stale source paths
    WTPProjectsUtil.deleteLinks(project, ROOT_PATH, sourcePaths, monitor);
    
    removeTestFolderLinks(project, mavenProject, monitor, "/");
    
    ProjectUtils.removeNature(project, "org.eclipse.jdt.core.javanature", monitor);

    //configureDeployedName(project, mavenProject.getBuild().getFinalName());
    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

  }

  private IDataModel getEarModel(String contentDir) {
    IDataModel earModelCfg = DataModelFactory.createDataModel(new EarFacetInstallDataModelProvider());
    earModelCfg.setProperty(IEarFacetInstallDataModelProperties.CONTENT_DIR, contentDir);
    earModelCfg.setProperty(IEarFacetInstallDataModelProperties.GENERATE_DD, false);
    return earModelCfg;
  }

  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.EAR_FACET)) {
      return;
    }

    IVirtualComponent earComponent = ComponentCore.createComponent(project);
    
    Set<IVirtualReference> newRefs = new LinkedHashSet<IVirtualReference>();
    
    EarPluginConfiguration config = new EarPluginConfiguration(mavenProject);
    // Retrieving all ear module configuration from maven-ear-plugin : User defined modules + artifacts dependencies.
    Set<EarModule> earModules = config.getEarModules();

    String libBundleDir = config.getDefaultBundleDirectory();

    updateLibDir(project, libBundleDir, monitor);
    
    for(EarModule earModule : earModules) {

      Artifact artifact = earModule.getArtifact();
      IVirtualComponent depComponent = null;
      IMavenProjectFacade workspaceDependency = projectManager.getMavenProject(artifact.getGroupId(), artifact
          .getArtifactId(), artifact.getVersion());

      if(workspaceDependency != null && !workspaceDependency.getProject().equals(project)
          && workspaceDependency.getFullPath(artifact.getFile()) != null) {
        //artifact dependency is a workspace project
        IProject depProject = preConfigureDependencyProject(workspaceDependency, monitor);
        if (ModuleCoreNature.isFlexibleProject(depProject)) {
          depComponent = createDependencyComponent(earComponent, depProject);
          configureDeployedName(depProject, earModule.getBundleFileName());
        }
      } else {
        //artifact dependency should be added as a JEE module, referenced with M2_REPO variable 
        depComponent = createDependencyComponent(earComponent, earModule.getArtifact());
      }
      
      if (depComponent != null) {
        IVirtualReference depRef = ComponentCore.createReference(earComponent, depComponent);
        String bundleDir = (StringUtils.isBlank(earModule.getBundleDir()))?"/":earModule.getBundleDir();
        depRef.setRuntimePath(new Path(bundleDir));
        depRef.setArchiveName(earModule.getBundleFileName());
        newRefs.add(depRef);
      }
    }
    
    IVirtualReference[] newRefsArray = new IVirtualReference[newRefs.size()];
    newRefs.toArray(newRefsArray);
    
    //Only change the project references if they've changed
    if (hasChanged(earComponent.getReferences(), newRefsArray)) {
      earComponent.setReferences(newRefsArray);
    }

    boolean useBuildDirectory = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().getPreferences(project).isApplicationXmGeneratedInBuildDirectory();
    DeploymentDescriptorManagement.INSTANCE.updateConfiguration(project, mavenProject, config, useBuildDirectory, monitor);
  }



  private void updateLibDir(IProject project, String newLibDir, IProgressMonitor monitor) {
    //Update lib dir only applies to Java EE 5 ear projects
    if(!J2EEProjectUtilities.isJEEProject(project)){ 
      return;
    }
    
    //if the ear project Java EE level was < 5.0, the following would throw a ClassCastException  
    final IEARModelProvider earModel = (IEARModelProvider)ModelProviderManager.getModelProvider(project);
    if (earModel == null) {
      return;
    }
    final Application app = (Application)earModel.getModelObject();
    if (app != null) {
      if (newLibDir == null || "/".equals(newLibDir)) {
        newLibDir = "lib";
      } 
      //MECLIPSEWTP-167 : lib directory mustn't start with a slash
      else if (newLibDir.startsWith("/")) {
        newLibDir = newLibDir.substring(1);
      }
      String oldLibDir = app.getLibraryDirectory();
      if (newLibDir.equals(oldLibDir)) return;
      final String libDir = newLibDir;
      earModel.modify(new Runnable() {
        public void run() {     
        app.setLibraryDirectory(libDir);
      }}, null);
    }
  }


  private IVirtualComponent createDependencyComponent(IVirtualComponent earComponent, IProject project) {
    IVirtualComponent depComponent = ComponentCore.createComponent(project);
    return depComponent;
  }

  private IVirtualComponent createDependencyComponent(IVirtualComponent earComponent, Artifact artifact) {
      //Create dependency component, referenced from the local Repo.
      String artifactPath = ArtifactHelper.getM2REPOVarPath(artifact);
      IVirtualComponent depComponent = ComponentCore.createArchiveComponent(earComponent.getProject(), artifactPath);
      return depComponent;
  }
}
