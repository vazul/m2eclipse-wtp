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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.war.Overlay;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.ComponentResource;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.wtp.filtering.WebResourceFilteringConfiguration;
import org.maven.ide.eclipse.wtp.internal.StringUtils;
import org.maven.ide.eclipse.wtp.overlay.ExplodedWarCleaner;
import org.maven.ide.eclipse.wtp.overlay.LinkedOverlaysConstants;
import org.maven.ide.eclipse.wtp.overlay.UnpackArchiveToStateLocationJob;
import org.maven.ide.eclipse.wtp.overlay.modulecore.IOverlayVirtualComponent;
import org.maven.ide.eclipse.wtp.overlay.modulecore.OverlayComponentCore;

/**
 * OverlayConfigurator
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class OverlayConfigurator extends WTPProjectConfigurator {

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = event.getMavenProject();
    if(facade == null) { return; }
    IProject project = facade.getProject();
    if (project.getResourceAttributes().isReadOnly()){
      return;
    }

    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    if(!facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
      return;
    }

    MavenProject mavenProject = facade.getMavenProject(monitor);
    try {
      markerManager.deleteMarkers(facade.getPom(), MavenWtpConstants.WTP_MARKER_OVERLAY_ERROR);
      setModuleDependencies(project, mavenProject, monitor);
    } catch(Exception ex) {
      markerManager.addErrorMarkers(facade.getPom(), MavenWtpConstants.WTP_MARKER_OVERLAY_ERROR,ex);
    }
    
  }

  /**
   * Setup overlays using WTP component reference with dependency type: consume
   * 
   * @param project
   * @param mavenProject
   * @param monitor
   * @throws CoreException
   */
  private void setModuleDependenciesWithOverlayModules(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {

    IVirtualComponent warComponent = ComponentCore.createComponent(project);
    if (warComponent == null) {
      return;
    }
    
    Set<IVirtualReference> newOverlayRefs = new LinkedHashSet<IVirtualReference>();
    MavenSessionHelper helper = new MavenSessionHelper(mavenProject);
    try {
      helper.ensureDependenciesAreResolved("maven-war-plugin", "war:war");
      
      MavenPlugin.getMaven();
      
    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject, project);
    
    List<Overlay> overlays = config.getOverlays();
    //1 overlay = current project => no overlay component needed
    if (overlays.size() > 1) {

      //Component order must be inverted to follow maven's overlay order behaviour 
      //as in WTP, last components supersede the previous ones
      Collections.reverse(overlays);
      for(Overlay overlay : overlays) {

        if (overlay.shouldSkip()) {
          continue;
        }
        
        Artifact artifact = overlay.getArtifact();
        IOverlayVirtualComponent overlayComponent = null;
        IMavenProjectFacade workspaceDependency = projectManager.getMavenProject(
            artifact.getGroupId(), 
            artifact.getArtifactId(),
            artifact.getVersion());

        if(workspaceDependency != null) {
          //artifact dependency is a workspace project
          IProject overlayProject = workspaceDependency.getProject();

          if (overlayProject.equals(project)) {
            overlayComponent = OverlayComponentCore.createSelfOverlayComponent(project);
          } else {
            overlayComponent = OverlayComponentCore.createOverlayComponent(overlayProject);
          }
        } else {
          overlayComponent = createOverlayArchiveComponent(project, mavenProject, overlay);
        }

        if (overlayComponent != null) {
          
          overlayComponent.setInclusions(new LinkedHashSet<String>(Arrays.asList(overlay.getIncludes())));
          overlayComponent.setExclusions(new LinkedHashSet<String>(Arrays.asList(overlay.getExcludes())));
          
          IVirtualReference depRef = ComponentCore.createReference(warComponent, overlayComponent);
          String targetPath = StringUtils.nullOrEmpty(overlay.getTargetPath())?"/":overlay.getTargetPath();
          depRef.setRuntimePath(new Path(targetPath));
          newOverlayRefs.add(depRef);
        }
      }
      
    }
    
    IVirtualReference[] oldOverlayRefs = WTPProjectsUtil.extractHardReferences(warComponent, true);
    
    IVirtualReference[] updatedOverlayRefs = newOverlayRefs.toArray(new IVirtualReference[newOverlayRefs.size()]);
    
    if (WTPProjectsUtil.hasChanged2(oldOverlayRefs, updatedOverlayRefs)){
      //Only write in the .component file if necessary 
      IVirtualReference[] nonOverlayRefs = WTPProjectsUtil.extractHardReferences(warComponent, false);
      IVirtualReference[] allRefs = new IVirtualReference[nonOverlayRefs.length + updatedOverlayRefs.length];
      System.arraycopy(nonOverlayRefs, 0, allRefs, 0, nonOverlayRefs.length);
      System.arraycopy(updatedOverlayRefs, 0, allRefs, nonOverlayRefs.length, updatedOverlayRefs.length);
      warComponent.setReferences(allRefs);
    }

      //remove overlays links if previously set up by "use linked folders" configuration
      List<String> defaultLinks = new ArrayList<String>();
      defaultLinks.add(WebResourceFilteringConfiguration.getTargetFolder(mavenProject, project).toString());
      defaultLinks.add(project.getFolder(config.getWarSourceDirectory()).getProjectRelativePath().toString());
      setUpLinkedFolders(defaultLinks, warComponent, false);

      //remove overlays virtual folder:
      IFolder overlaysFolder = project.getFolder(LinkedOverlaysConstants.OVERLAYS_FOLDER);
      if(overlaysFolder.exists()) {
        //schedule an exploded war cleaner
        ExplodedWarCleaner.scheduleClean();

        overlaysFolder.delete(true, null);
      }

      //remove default root source folder:
      IFolder defaultRootSource = project.getFolder(ProjectUtils.getM2eclipseWtpFolder(mavenProject, project).append(
          LinkedOverlaysConstants.DEFAULT_ROOT_SOURCE));
      if(defaultRootSource.exists()) {
        defaultRootSource.delete(true, null);
      }

    } finally {
      helper.dispose();
    }

  }

  private IOverlayVirtualComponent createOverlayArchiveComponent(IProject project, MavenProject mavenProject, Overlay overlay) throws CoreException {
    IPath m2eWtpFolder = ProjectUtils.getM2eclipseWtpFolder(mavenProject, project);
    IPath unpackDirPath = new Path(m2eWtpFolder.toOSString()+"/overlays");
    String archiveLocation = ArtifactHelper.getM2REPOVarPath(overlay.getArtifact());
    String targetPath = StringUtils.nullOrEmpty(overlay.getTargetPath())?"/":overlay.getTargetPath();
    IOverlayVirtualComponent component = OverlayComponentCore.createOverlayArchiveComponent(
                                                                project, 
                                                                archiveLocation, 
                                                                unpackDirPath, 
                                                                new Path(targetPath));
    return component;
  }
  
  
  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
      throws CoreException {
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.WTPProjectConfigurator#getBuildParticipant(org.eclipse.m2e.core.project.IMavenProjectFacade, org.apache.maven.plugin.MojoExecution, org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata)
   */
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    return null;
  }

  /**
   * @param project
   * @param mavenProject
   * @param monitor
   * @throws CoreException 
   */
  private void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {

    if(MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().getPreferences(project).isWarOverlaysUsesLinkedFolders()) {
      setModuleDependenciesWithLinkedFolders(project, mavenProject, monitor);
    } else {
      setModuleDependenciesWithOverlayModules(project, mavenProject, monitor);
    }
  }

  /**
   * Set module dependencies with linked folders
   * 
   * @param project
   * @param mavenProject
   * @param monitor
   * @throws CoreException
   */
  private void setModuleDependenciesWithLinkedFolders(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
    final IVirtualComponent warComponent = ComponentCore.createComponent(project);
    if(warComponent == null) {
      return;
    }

    final MavenSessionHelper helper = new MavenSessionHelper(mavenProject);
    try {
      helper.ensureDependenciesAreResolved("maven-war-plugin", "war:war");

      MavenPlugin.getMaven();

      //schedule a clean after 10 seconds... cancel a clean job if it is in progress or in sleeping
      ExplodedWarCleaner.scheduleClean();

      final WarPluginConfiguration config = new WarPluginConfiguration(mavenProject, project);

      final List<String> linkedOverlays = new ArrayList<String>();
      final Map<IFolder, Artifact> explodedArtifacts = new HashMap<IFolder, Artifact>();
      final Set<String> overlayFolders = new HashSet<String>();

      final IFolder overlaysFolder = project.getFolder(LinkedOverlaysConstants.OVERLAYS_FOLDER);

      final List<Overlay> overlays = config.getOverlays();
      if(overlays.size() > 0) {

        //create .overlays virtual folder
        if(!overlaysFolder.exists()) {
          overlaysFolder.create(IResource.DERIVED | IResource.VIRTUAL, true, null);
        } else if(!overlaysFolder.isVirtual()) {
          throw new CoreException(new Status(Status.ERROR, MavenWtpPlugin.ID,
              "The '" + LinkedOverlaysConstants.OVERLAYS_FOLDER + "' folder exists and is not virtual in project " + project.getName()));
        }

        //Component order must be inverted to follow maven's overlay order behaviour
        //as in WTP, last components supersede the previous ones
        Collections.reverse(overlays);

        for(final Overlay overlay : overlays) {

          if(overlay.shouldSkip()) {
            continue;
          }

          final Artifact artifact = overlay.getArtifact();
          final IMavenProjectFacade workspaceDependency = projectManager.getMavenProject(artifact.getGroupId(),
              artifact.getArtifactId(), artifact.getVersion());

          final String foldername = artifact.getArtifactId() + "-" + artifact.getBaseVersion();

          if(workspaceDependency != null) {
            //artifact dependency is a workspace project
            final IProject overlayProject = workspaceDependency.getProject();

            if(overlayProject.equals(project)) {
              //add self folders:
              linkedOverlays.add(WebResourceFilteringConfiguration.getTargetFolder(mavenProject, project).toString());
              linkedOverlays.add(project.getFolder(config.getWarSourceDirectory()).getProjectRelativePath().toString());
              //create folder for web.xml, and set as default source root
              IFolder defaultRootSource = project.getFolder(ProjectUtils.getM2eclipseWtpFolder(mavenProject, project)
                  .append(LinkedOverlaysConstants.DEFAULT_ROOT_SOURCE));
              if(!defaultRootSource.exists()) {
                defaultRootSource.create(true, true, null);
              }
              linkedOverlays.add(0, defaultRootSource.getProjectRelativePath().toString());

              IFolder defaultWebInf = defaultRootSource.getFolder("WEB-INF");
              if(!defaultWebInf.exists()) {
                defaultWebInf.create(true, true, null);
              }
              String webXmlLocation = config.getCustomWebXml(project);
              IFile webXml;
              if(webXmlLocation != null) {
                webXml = project.getFile(webXmlLocation);
              } else {
                webXml = project.getFolder(config.getWarSourceDirectory()).getFile("WEB-INF/web.xml");
              }

              try {
                FileUtils.copyFileIfModified(webXml.getLocation().toFile(), new File(defaultWebInf.getLocation()
                    .toFile(), "web.xml"));
              } catch(IOException ex) {
                throw new CoreException(new Status(Status.ERROR, MavenWtpPlugin.ID,
                    "Cannot copy web.xml to default source root: " + defaultWebInf.getLocation(), ex));
              }
              defaultWebInf.refreshLocal(IResource.DEPTH_INFINITE, null);

            } else {
              overlayFolders.add(foldername);
              final WarPluginConfiguration depConfig = new WarPluginConfiguration(
                  workspaceDependency.getMavenProject(), workspaceDependency.getProject());

              final IFolder overlayContainer = overlaysFolder.getFolder(foldername);
              if(overlayContainer.exists()) {
                if(overlayContainer.isLinked() && !overlayContainer.isVirtual()) {
                  //linked in an exploded war, remove it
                  overlayContainer.delete(true, null);
                }
              }
              if(!overlayContainer.exists()) {
                overlayContainer.create(IResource.DERIVED | IResource.VIRTUAL, true, null);
              }

              final IFolder webResources = overlayContainer.getFolder(artifact.getArtifactId() + LinkedOverlaysConstants.WEB_RESOURCES_LINK_POSTFIX);

              webResources.createLink(new Path("WORKSPACE_LOC/" + workspaceDependency.getProject().getName())
                  .append(WebResourceFilteringConfiguration.getTargetFolder(workspaceDependency.getMavenProject(),
                      workspaceDependency.getProject())),
                  IResource.ALLOW_MISSING_LOCAL | IResource.REPLACE, null);
              
              //add web-resources to the linked folders
              linkedOverlays.add(webResources.getProjectRelativePath().toString());

              final IFolder warSource = overlayContainer.getFolder(artifact.getArtifactId() + LinkedOverlaysConstants.WEBAPP_LINK_POSTFIX);
              final IPath warSourcePath = workspaceDependency.getProject().getFolder(depConfig.getWarSourceDirectory()).getFullPath();
              warSource.createLink(new Path("WORKSPACE_LOC" + warSourcePath.toString()), IResource.ALLOW_MISSING_LOCAL
                  | IResource.REPLACE, null);
              
              //add webapp to the linked folders
              linkedOverlays.add(warSource.getProjectRelativePath().toString());
            }
          } else {
            overlayFolders.add(foldername);

            //add it to the linked folders, extract it to plugin's state location
            final IPath stateLocation = MavenWtpPlugin.getDefault().getStateLocation();
            final IPath relativePath = stateLocation.makeRelativeTo(project.getWorkspace().getRoot().getLocation());
            final IFolder explodedLink = overlaysFolder.getFolder(foldername);
            if(explodedLink.isVirtual()) {
              //virtual folder for workspace overlay, remove it
              explodedLink.delete(true, null);
            }
            explodedLink.createLink(new Path("WORKSPACE_LOC/" + relativePath + "/exploded-wars/" + foldername),
                IResource.ALLOW_MISSING_LOCAL | IResource.REPLACE, null);

            //add it to the linked folders
            linkedOverlays.add(explodedLink.getProjectRelativePath().toString());

            //add it to the exploded war list
            explodedArtifacts.put(explodedLink, overlay.getArtifact());
          }
        }
      }

      for(final Entry<IFolder, Artifact> entry : explodedArtifacts.entrySet()) {
        explodeArtifact(entry.getKey(), entry.getValue(), monitor);
      }

      if(linksHasChanged(linkedOverlays, warComponent.getRootFolder().getUnderlyingFolders())) {
        setUpLinkedFolders(linkedOverlays, warComponent, true);
      }

      //delete unnecessary folders
      if(linkedOverlays.size() == 0) {
        overlaysFolder.delete(true, null);
      } else {
        for(final IResource folder : overlaysFolder.members()) {
          if(!overlayFolders.contains(folder.getName())) {
            folder.delete(true, null);
          }
        }
      }

      if(WTPProjectsUtil.extractHardReferences(warComponent, true).length > 0) {
        //remove overlay module references
        final IVirtualReference[] nonOverlayRefs = WTPProjectsUtil.extractHardReferences(warComponent, false);
        warComponent.setReferences(nonOverlayRefs);
      }

      //remove old extract folde rlocation
      final IFolder oldOverlaysFolder = project.getFolder(ProjectUtils.getM2eclipseWtpFolder(mavenProject, project)
          .append("overlays"));
      if(oldOverlaysFolder.exists()) {
        oldOverlaysFolder.delete(true, null);
      }

    } finally {
      helper.dispose();
    }

  }

  /**
   * Explode a war artifact to the plugins state location
   * 
   * @param explodedLink
   * @param overlayArtifact
   * @param monitor
   */
  private void explodeArtifact(final IFolder explodedLink, final Artifact overlayArtifact,
      final IProgressMonitor monitor) {
    final File sourceFile = overlayArtifact.getFile();

    final IPath explodedLocation = explodedLink.getLocation();
    final File targetDir = explodedLocation.toFile();

    if(!targetDir.exists() || targetDir.lastModified() != sourceFile.lastModified()) {
      new UnpackArchiveToStateLocationJob("Unpacking " + explodedLink.getName(), sourceFile, targetDir, explodedLink).schedule();
    }
  }

  /**
   * Setup linked folder in compoent
   * 
   * @param linkedOverlays
   * @param warComponent
   */
  @SuppressWarnings("unchecked")
  private void setUpLinkedFolders(final List<String> linkedOverlays, final IVirtualComponent warComponent,
      final boolean removeTag) {
    StructureEdit moduleCore = null;
    try {
      moduleCore = StructureEdit.getStructureEditForWrite(warComponent.getProject());
      final WorkbenchComponent component = moduleCore.getComponent();
      if(null != component) {
        final EList<ComponentResource> resourcesList = component.getResources();
        final Map<String, ComponentResource> resourceMap = new HashMap<String, ComponentResource>();
        final List<ComponentResource> otherResources = new ArrayList<ComponentResource>();
        for(final ComponentResource componentResource : resourcesList) {
          if(componentResource.getRuntimePath().toString().equals("/")) {
            //remove possible defaultRootSource tag:
            if(removeTag) {
              componentResource.setTag(null);
            }
            resourceMap.put(componentResource.getSourcePath().toString(), componentResource);
          } else if(componentResource.getRuntimePath().toString().equals("/WEB-INF/web.xml")) {
            //skip linked web.xml: it does not support "serve modules without publishing"
          } else {
            otherResources.add(componentResource);
          }
        }

        resourcesList.clear();
        for(final String overlayPath : linkedOverlays) {
          if(resourceMap.containsKey("/" + overlayPath)) {
            resourcesList.add(resourceMap.get("/" + overlayPath));
          } else {
            final ComponentResource componentResource = moduleCore.createWorkbenchModuleResource(warComponent
                .getProject().getFolder(overlayPath));
            componentResource.setRuntimePath(IVirtualComponent.ROOT);
            componentResource.getExclusions();
            resourcesList.add(componentResource);
          }
        }

        resourcesList.addAll(otherResources);
      }

    } finally {
      if(moduleCore != null) {
        moduleCore.saveIfNecessary(new NullProgressMonitor());
        moduleCore.dispose();
      }
    }
  }

  /**
   * Checks if links has changed
   * 
   * @param linkedOverlays
   * @param underlayingFolders
   * @return
   */
  private boolean linksHasChanged(final List<String> linkedOverlays, final IContainer[] underlayingFolders) {
    if(linkedOverlays.size() != underlayingFolders.length) {
      return true;
    }
    for(int i = 0; i < underlayingFolders.length; i++ ) {
      if(!linkedOverlays.get(i).equals(underlayingFolders[i].getProjectRelativePath().toString())) {
        return true;
      }
    }
    return false;
  }

}
