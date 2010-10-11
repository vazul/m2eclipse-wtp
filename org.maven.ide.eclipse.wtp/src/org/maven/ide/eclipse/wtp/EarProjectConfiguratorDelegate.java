/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;



import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.earcreation.IEarFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.internal.earcreation.EarFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.j2ee.model.IEARModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.application.Application;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelProvider;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.wtp.earmodules.EarModule;


/**
 * Configures Ear projects from maven-ear-plugin.
 * 
 * @see org.eclipse.jst.j2ee.ui.AddModulestoEARPropertiesPage
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
class EarProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  private static final IStatus OK_STATUS = IDataModelProvider.OK_STATUS;

  protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

    if(facetedProject.hasProjectFacet(WTPProjectsUtil.EAR_FACET)) {
      try {
        facetedProject.modify(Collections.singleton(new IFacetedProject.Action(IFacetedProject.Action.Type.UNINSTALL,
            facetedProject.getInstalledVersion(WTPProjectsUtil.EAR_FACET), null)), monitor);
      } catch(Exception ex) {
        MavenLogger.log("Error removing EAR facet", ex);
      }
    }

    EarPluginConfiguration config = new EarPluginConfiguration(mavenProject);
    Set<Action> actions = new LinkedHashSet<Action>();
    // WTP doesn't allow facet versions changes for JEE facets
    String contentDir = config.getEarContentDirectory(project);
    
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.EAR_FACET)) {
      IDataModel earModelCfg = DataModelFactory.createDataModel(new EarFacetInstallDataModelProvider());

      // Configuring content directory
      earModelCfg.setProperty(IEarFacetInstallDataModelProperties.CONTENT_DIR, contentDir);

      IProjectFacetVersion earFv = config.getEarFacetVersion();
      
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, earFv, earModelCfg));
    }

    if(!actions.isEmpty()) {
      facetedProject.modify(actions, monitor);
    }

    // FIXME Sometimes, test folders are still added to org.eclipse.wst.common.component
    removeTestFolderLinks(project, mavenProject, monitor, "/");

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
        
        depComponent = createDependencyComponent(earComponent, depProject);
        configureDeployedName(depProject, earModule.getBundleFileName());
      } else {
        //artifact dependency should be added as a JEE module, referenced with M2_REPO variable 
        depComponent = createDependencyComponent(earComponent, earModule.getArtifact());
      }

      IVirtualReference depRef = ComponentCore.createReference(earComponent, depComponent);
      String bundleDir = (StringUtils.isBlank(earModule.getBundleDir()))?"/":earModule.getBundleDir();
      depRef.setRuntimePath(new Path(bundleDir));
      depRef.setArchiveName(earModule.getBundleFileName());
      newRefs.add(depRef);
    }
    
    IVirtualReference[] newRefsArray = new IVirtualReference[newRefs.size()];
    newRefs.toArray(newRefsArray);
    
    //Only change the project references if they've changed
    if (hasChanged(earComponent.getReferences(), newRefsArray)) {
      earComponent.setReferences(newRefsArray);
    }

    DeploymentDescriptorManagement.INSTANCE.updateConfiguration(project, mavenProject, config, monitor);
  }



  private void updateLibDir(IProject project, String newLibDir, IProgressMonitor monitor) {
    //Update lib dir only applies to Java EE 5 ear projects
    if(!J2EEProjectUtilities.isJEEProject(project)){ 
      return;
    }
    
    //if the ear project Java EE level was < 5.0, the following would throw a ClassCastException  
    final Application app = (Application)ModelProviderManager.getModelProvider(project).getModelObject();
    if (app != null) {
      if (newLibDir == null || "/".equals(newLibDir)) {
        newLibDir = J2EEConstants.EAR_DEFAULT_LIB_DIR;
      }
      String oldLibDir = app.getLibraryDirectory();
      if (newLibDir.equals(oldLibDir)) return;
      final String libDir = newLibDir;
      final IEARModelProvider earModel = (IEARModelProvider)ModelProviderManager.getModelProvider(project);
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
  
  public void configureClasspath(IProject project, MavenProject mavenProject, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    // do nothing
  }
}
