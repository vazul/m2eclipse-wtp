/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.project.IMavenMarkerManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenProjectUtils;


/**
 * AbstractProjectConfiguratorDelegate
 * 
 * @author Igor Fedorenko
 * @author Fred Bricon
 */
abstract class AbstractProjectConfiguratorDelegate implements IProjectConfiguratorDelegate {

  static final IClasspathAttribute NONDEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY, "");


  protected final MavenProjectManager projectManager;

  protected final IMavenMarkerManager mavenMarkerManager;

  AbstractProjectConfiguratorDelegate() {
    this.projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    this.mavenMarkerManager = MavenPlugin.getDefault().getMavenMarkerManager();
  }
  
  public void configureProject(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws MarkedException {
    try {
      mavenMarkerManager.deleteMarkers(project);
      configure(project, mavenProject, monitor);
    } catch (CoreException cex) {
      //TODO Filter out constraint violations
      cex.printStackTrace();
      mavenMarkerManager.addErrorMarkers(project, cex);
      throw new MarkedException("Unable to configure "+project.getName(), cex);
    }
  }
 
  protected abstract void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException;

  protected List<IMavenProjectFacade> getWorkspaceDependencies(IProject project, MavenProject mavenProject) {
    Set<IProject> projects = new HashSet<IProject>();
    List<IMavenProjectFacade> dependencies = new ArrayList<IMavenProjectFacade>();
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact artifact : artifacts) {
      IMavenProjectFacade dependency = projectManager.getMavenProject(artifact.getGroupId(), artifact.getArtifactId(),
          artifact.getVersion());
      
      if((Artifact.SCOPE_COMPILE.equals(artifact.getScope()) 
          || Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) //MNGECLIPSE-1578 Runtime dependencies should be deployed 
          && dependency != null && !dependency.getProject().equals(project) && dependency.getFullPath(artifact.getFile()) != null
          && projects.add(dependency.getProject())) {
        dependencies.add(dependency);
      }
    }
    return dependencies;
  }

  protected void configureWtpUtil(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
    // Adding utility facet on JEE projects is not allowed
    if(WTPProjectsUtil.isJavaEEProject(project)) {
      return;
    }

    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    Set<Action> actions = new LinkedHashSet<Action>();
    installJavaFacet(actions, project, facetedProject);

    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.UTILITY_FACET)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, WTPProjectsUtil.UTILITY_10, null));
    } else if(!facetedProject.hasProjectFacet(WTPProjectsUtil.UTILITY_10)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, WTPProjectsUtil.UTILITY_10,
          null));
    }

    facetedProject.modify(actions, monitor);
    
    //MNGECLIPSE-904 remove tests folder links for utility jars
    //TODO handle modules in a parent pom (the following doesn't work)
    removeTestFolderLinks(project, mavenProject, monitor, "/");
    
    //Remove "library unavailable at runtime" warning.
    addContainerAttribute(project, NONDEPENDENCY_ATTRIBUTE, monitor);
  }

  protected void installJavaFacet(Set<Action> actions, IProject project, IFacetedProject facetedProject) {
    IProjectFacetVersion javaFv = JavaFacetUtils.compilerLevelToFacet(JavaFacetUtils.getCompilerLevel(project));
    if(!facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, javaFv, null));
    } else if(!facetedProject.hasProjectFacet(javaFv)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, javaFv, null));
    }
  }

  protected void removeTestFolderLinks(IProject project, MavenProject mavenProject, IProgressMonitor monitor,
      String folder) throws CoreException {
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null){
      IVirtualFolder jsrc = component.getRootFolder().getFolder(folder);
      for(IPath location : MavenProjectUtils.getSourceLocations(project, mavenProject.getTestCompileSourceRoots())) {
        jsrc.removeLink(location, 0, monitor);
      }
      for(IPath location : MavenProjectUtils.getResourceLocations(project, mavenProject.getTestResources())) {
        jsrc.removeLink(location, 0, monitor);
      }
    }
  }

  // XXX move to IJavaProjectConfiguration#configureRawClasspath
  protected void addContainerAttribute(IProject project, IClasspathAttribute attribute, IProgressMonitor monitor)
      throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
	if (javaProject == null) return;
    IClasspathEntry[] cp = javaProject.getRawClasspath();
    for(int i = 0; i < cp.length; i++ ) {
      if(IClasspathEntry.CPE_CONTAINER == cp[i].getEntryKind()
          && BuildPathManager.isMaven2ClasspathContainer(cp[i].getPath())) {
        LinkedHashMap<String, IClasspathAttribute> attrs = new LinkedHashMap<String, IClasspathAttribute>();
        for(IClasspathAttribute attr : cp[i].getExtraAttributes()) {
          attrs.put(attr.getName(), attr);
        }
        attrs.put(attribute.getName(), attribute);
        IClasspathAttribute[] newAttrs = attrs.values().toArray(new IClasspathAttribute[attrs.size()]);
        cp[i] = JavaCore.newContainerEntry(cp[i].getPath(), cp[i].getAccessRules(), newAttrs, cp[i].isExported());
        break;
      }
    }
    javaProject.setRawClasspath(cp, monitor);
  }

  /**
   * @param dependencyMavenProjectFacade
   * @param monitor
   * @return
   * @throws CoreException
   */
  protected IProject preConfigureDependencyProject(IMavenProjectFacade dependencyMavenProjectFacade, IProgressMonitor monitor) throws CoreException {
    IProject dependency = dependencyMavenProjectFacade.getProject();
    MavenProject mavenDependency = dependencyMavenProjectFacade.getMavenProject(monitor);
    String depPackaging = dependencyMavenProjectFacade.getPackaging();
    //jee dependency has not been configured yet - i.e. it has no JEE facet-
    if(JEEPackaging.isJEEPackaging(depPackaging) && !WTPProjectsUtil.isJavaEEProject(dependency)) {
      IProjectConfiguratorDelegate delegate = ProjectConfiguratorDelegateFactory
          .getProjectConfiguratorDelegate(dependencyMavenProjectFacade.getPackaging());
      if(delegate != null) {
        //Lets install the proper facets
        try {
          delegate.configureProject(dependency, mavenDependency, monitor);
        } catch(MarkedException ex) {
          //Markers already have been created for this exception, no more to do.
          return dependency;
        }
      }
    } else {
      // XXX Probably should create a UtilProjectConfiguratorDelegate
      configureWtpUtil(dependency, mavenDependency, monitor);
    }
    return dependency;
  }

  protected void configureDeployedName(IProject project, String deployedFileName) {
    //We need to remove the file extension from deployedFileName 
    int extSeparatorPos  = deployedFileName.lastIndexOf('.');
    String deployedName = extSeparatorPos > -1? deployedFileName.substring(0, extSeparatorPos): deployedFileName;
    //From jerr's patch in MNGECLIPSE-965
    IVirtualComponent projectComponent = ComponentCore.createComponent(project);
    if(!deployedName.equals(projectComponent.getDeployedName())){//MNGECLIPSE-2331 : Seems projectComponent.getDeployedName() can be null 
      StructureEdit moduleCore = null;
      try {
        moduleCore = StructureEdit.getStructureEditForWrite(project);
        WorkbenchComponent component = moduleCore.getComponent();
        component.setName(deployedName);
        moduleCore.saveIfNecessary(null);
      } finally {
        if (moduleCore != null) {
          moduleCore.dispose();
        }
      }
    }  
  }

  /**
   * Link a project's file to a specific deployment destination. Existing links will be deleted beforehand. 
   * @param project 
   * @param customFile the existing file to deploy
   * @param targetRuntimePath the target runtime/deployment location of the file
   * @param monitor
   * @throws CoreException
   */
  protected void linkFile(IProject project, String customFile, String targetRuntimePath, IProgressMonitor monitor) throws CoreException {
      IPath runtimePath = new Path(targetRuntimePath);
      //We first delete any existing links
      WTPProjectsUtil.deleteLinks(project, runtimePath, monitor);
      if (customFile != null) {
        //Create the new link
        IVirtualComponent component = ComponentCore.createComponent(project);
        if (component != null){
          IVirtualFile virtualCustomFile = component.getRootFolder().getFile(runtimePath);
          IPath virtualCustomFilePath = new Path(customFile);
          virtualCustomFile.createLink(virtualCustomFilePath, 0, monitor);
        }
      }
  }

  protected boolean hasChanged(IVirtualReference[] existingRefs, IVirtualReference[] refArray) {
  
    if (existingRefs==refArray) {
      return false;
    }
    if (existingRefs == null || existingRefs.length != refArray.length) {
      return true;
    }
    for (int i=0; i<existingRefs.length;i++){
      IVirtualReference existingRef = existingRefs[i];
      IVirtualReference newRef = refArray[i];
      if ((existingRef.getArchiveName() != null && !existingRef.getArchiveName().equals(newRef.getArchiveName())) ||
          !existingRef.getReferencedComponent().equals(newRef.getReferencedComponent()) ||
          !existingRef.getRuntimePath().equals(newRef.getRuntimePath())) 
      {
        return true;  
      }
    }
    return false;    
  }
}
