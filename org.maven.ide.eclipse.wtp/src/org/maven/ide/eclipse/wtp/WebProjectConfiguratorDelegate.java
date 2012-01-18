/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import static org.maven.ide.eclipse.wtp.WTPProjectsUtil.removeFacets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
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
import org.maven.ide.eclipse.wtp.filtering.WebResourceFilteringConfiguration;
import org.maven.ide.eclipse.wtp.internal.ExtensionReader;
import org.maven.ide.eclipse.wtp.namemapping.FileNameMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * WebProjectConfiguratorDelegate
 * 
 * @author Igor Fedorenko
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
class WebProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  public static final String WARNING_MAVEN_ARCHIVER_OUTPUT_SETTINGS_IGNORED = "Current Maven Archiver output settings are ignored " +
  		                                                                        "as web resource filtering is currently used";

  private static final Logger log = LoggerFactory.getLogger(WebProjectConfiguratorDelegate.class);
  
  /**
   * See http://wiki.eclipse.org/ClasspathEntriesPublishExportSupport
   */
  static final IClasspathAttribute DEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY, "/WEB-INF/lib");

  /**
  * Name of maven property that overrides WTP context root.
  */
  private static final String M2ECLIPSE_WTP_CONTEXT_ROOT = "m2eclipse.wtp.contextRoot";

  protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

    // make sure to update the main deployment folder
    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject, project);
    String warSourceDirectory = config.getWarSourceDirectory();
    IFile defaultWebXml = project.getFolder(warSourceDirectory).getFile("WEB-INF/web.xml");
    IFolder libDir = project.getFolder(warSourceDirectory).getFolder("WEB-INF/lib");
    
    IFolder firstInexistentfolder = null;
    IFolder contentFolder = project.getFolder(warSourceDirectory);
    IFile manifest = contentFolder.getFile("META-INF/MANIFEST.MF");
    boolean manifestAlreadyExists =manifest.exists(); 
    if (!manifestAlreadyExists) {
      firstInexistentfolder = findFirstInexistentFolder(project, contentFolder, manifest);
    }   

    boolean alreadyHasWebXml = defaultWebXml.exists();
    boolean alreadyHasLibDir = libDir.exists();
        
    Set<Action> actions = new LinkedHashSet<Action>();

    installJavaFacet(actions, project, facetedProject);
    
    IVirtualComponent component = ComponentCore.createComponent(project, true);
    
    //MNGECLIPSE-2279 get the context root from the final name of the project, or artifactId by default.
    String contextRoot = getContextRoot(mavenProject);
    
    IProjectFacetVersion webFv = config.getWebFacetVersion(project);
    IDataModel webModelCfg = getWebModelConfig(warSourceDirectory, contextRoot);
    if(!facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
      removeFacets(actions, WTPProjectsUtil.UTILITY_10);
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, webFv, webModelCfg));
    } else {
      IProjectFacetVersion projectFacetVersion = facetedProject.getProjectFacetVersion(WebFacetUtils.WEB_FACET);     
      if(webFv.getVersionString() != null && !webFv.getVersionString().equals(projectFacetVersion.getVersionString())){
          actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, webFv, webModelCfg));
      }
    }

    if(!actions.isEmpty()) {
      facetedProject.modify(actions, monitor);
    }
    
    //MECLIPSEWTP-41 Fix the missing moduleCoreNature
    fixMissingModuleCoreNature(project, monitor);
    
    // MNGECLIPSE-632 remove test sources/resources from WEB-INF/classes
    removeTestFolderLinks(project, mavenProject, monitor, "/WEB-INF/classes");

    addContainerAttribute(project, DEPENDENCY_ATTRIBUTE, monitor);

    //MNGECLIPSE-2279 change the context root if needed
    if (!contextRoot.equals(J2EEProjectUtilities.getServerContextRoot(project))) {
      J2EEProjectUtilities.setServerContextRoot(project, contextRoot);
    }

    //MNGECLIPSE-2357 support custom location of web.xml
    String customWebXml = config.getCustomWebXml(project);
    //If we have a custom web.xml but WTP created one against our will, we delete it 
    if (customWebXml != null && !alreadyHasWebXml && defaultWebXml.exists()) {
      defaultWebXml.delete(true, monitor);
    }
    //Maven /m2eclipse doesn't need a new lib dir. 
    if (!alreadyHasLibDir && libDir.exists()) {
      libDir.delete(true, monitor);
    }

    linkFileFirst(project, customWebXml, "/WEB-INF/web.xml", monitor);
    
    component = ComponentCore.createComponent(project, true);
    if(component != null) {      

      IPath warPath = new Path("/").append(contentFolder.getProjectRelativePath());
      List<IPath> sourcePaths = new ArrayList<IPath>();
      sourcePaths.add(warPath);
      if (!WTPProjectsUtil.hasLink(project, ROOT_PATH, warPath, monitor)) {
        component.getRootFolder().createLink(warPath, IVirtualResource.NONE, monitor); 
      }
      //MECLIPSEWTP-22 support web filtered resources. Filtered resources directory must be declared BEFORE
      //the regular web source directory. First resources discovered take precedence on deployment
      IPath filteredFolder = new Path("/").append(WebResourceFilteringConfiguration.getTargetFolder(mavenProject, project));
      
      boolean useBuildDir = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().getPreferences(project).isWebMavenArchiverUsesBuildDirectory();
      boolean useWebresourcefiltering = config.getWebResources() != null 
                                        && config.getWebResources().length > 0 
                                        || config.isFilteringDeploymentDescriptorsEnabled();

      if (useBuildDir || useWebresourcefiltering) {
        
        if (!useBuildDir && useWebresourcefiltering) {
          mavenMarkerManager.addMarker(project, MavenWtpConstants.WTP_MARKER_CONFIGURATION_ERROR_ID, 
                                      WARNING_MAVEN_ARCHIVER_OUTPUT_SETTINGS_IGNORED, -1, IMarker.SEVERITY_WARNING);
        }
        sourcePaths.add(filteredFolder);
        WTPProjectsUtil.insertLinkBefore(project, filteredFolder, warPath, new Path("/"), monitor);
      } else {
        component.getRootFolder().removeLink(filteredFolder,IVirtualResource.NONE, monitor);
      }

      WTPProjectsUtil.setDefaultDeploymentDescriptorFolder(component.getRootFolder(), warPath, monitor);
      
      WTPProjectsUtil.deleteLinks(project, ROOT_PATH, sourcePaths, monitor);
    }
    

    
    if (!manifestAlreadyExists && manifest.exists()) {
      manifest.delete(true, monitor);
    }
    if (firstInexistentfolder != null && firstInexistentfolder.exists() && firstInexistentfolder.members().length == 0 )
    {
      firstInexistentfolder.delete(true, monitor);
    }
    
    WTPProjectsUtil.removeWTPClasspathContainer(project);
  }

  private IDataModel getWebModelConfig(String warSourceDirectory, String contextRoot) {
    IDataModel webModelCfg = DataModelFactory.createDataModel(new WebFacetInstallDataModelProvider());
    webModelCfg.setProperty(IJ2EEModuleFacetInstallDataModelProperties.CONFIG_FOLDER, warSourceDirectory);
    webModelCfg.setProperty(IWebFacetInstallDataModelProperties.CONTEXT_ROOT, contextRoot);
    webModelCfg.setProperty(IJ2EEModuleFacetInstallDataModelProperties.GENERATE_DD, false);
    return webModelCfg;
  }

  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IVirtualComponent component = ComponentCore.createComponent(project);
    //if the attempt to create dependencies happens before the project is actually created, abort. 
    //this will be created again when the project exists.
    if(component == null){
      return;
    }
    //MECLIPSEWTP-41 Fix the missing moduleCoreNature
    fixMissingModuleCoreNature(project, monitor);
    
    DebugUtilities.debug("==============Processing "+project.getName()+" dependencies ===============");
    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject, project);
    WarPackagingOptions opts = new WarPackagingOptions(config);
    FileNameMapping fileNameMapping = config.getFileNameMapping();
    
    List<AbstractDependencyConfigurator> depConfigurators = ExtensionReader.readDependencyConfiguratorExtensions(projectManager, 
        MavenPlugin.getDefault().getMavenRuntimeManager(), mavenMarkerManager);
    
    Set<IVirtualReference> references = new LinkedHashSet<IVirtualReference>();
    List<IMavenProjectFacade> exportedDependencies = getWorkspaceDependencies(project, mavenProject);
    for(IMavenProjectFacade dependency : exportedDependencies) {
      String depPackaging = dependency.getPackaging();
      if ("pom".equals(depPackaging) //MNGECLIPSE-744 pom dependencies shouldn't be deployed
          || "war".equals(depPackaging) //Overlays are dealt with the overlay configurator
          || "zip".equals(depPackaging)) {
        continue;
      }
      
      try {
        preConfigureDependencyProject(dependency, monitor);
        
        if (!ModuleCoreNature.isFlexibleProject(dependency.getProject())) {
          //Projects unsupported by WTP (ex. adobe flex projects) should not be added as references
          continue;
        }
        MavenProject depMavenProject =  dependency.getMavenProject(monitor);
  
        IVirtualComponent depComponent = ComponentCore.createComponent(dependency.getProject());
  		      
        ArtifactKey artifactKey = ArtifactHelper.toArtifactKey(depMavenProject.getArtifact());
        Artifact artifact = ArtifactHelper.getArtifact(mavenProject.getArtifacts(), artifactKey);
        ArtifactHelper.fixArtifactHandler(artifact.getArtifactHandler());
        String deployedName = fileNameMapping.mapFileName(artifact);
        
        boolean isDeployed = !artifact.isOptional() && opts.isPackaged(deployedName);
          
    		//an artifact in mavenProject.getArtifacts() doesn't have the "optional" value as depMavenProject.getArtifact();  
    		if (isDeployed) {
    		  IVirtualReference reference = ComponentCore.createReference(component, depComponent);
    		  IPath path = new Path("/WEB-INF/lib");
    		  reference.setArchiveName(deployedName);
    		  reference.setRuntimePath(path);
    		  references.add(reference);
    		}
      } catch(RuntimeException ex) {
        //Should probably be NPEs at this point
        String dump = DebugUtilities.dumpProjectState("An error occured while configuring a dependency of  "+project.getName()+DebugUtilities.SEP, dependency.getProject());
        log.error(dump); 
        throw ex;
      }
    }

    
    IVirtualReference[] oldRefs = WTPProjectsUtil.extractHardReferences(component, false);
    
    IVirtualReference[] newRefs = references.toArray(new IVirtualReference[references.size()]);
    
    if (WTPProjectsUtil.hasChanged(oldRefs, newRefs)){
      //Only write in the .component file if necessary 
      IVirtualReference[] overlayRefs = WTPProjectsUtil.extractHardReferences(component, true);
      IVirtualReference[] allRefs = new IVirtualReference[overlayRefs.length + newRefs.length];
      System.arraycopy(newRefs, 0, allRefs, 0, newRefs.length);
      System.arraycopy(overlayRefs, 0, allRefs, newRefs.length, overlayRefs.length);
      component.setReferences(allRefs);
    }
    
    //TODO why a 2nd loop???
    for(IMavenProjectFacade dependency : exportedDependencies) {
      MavenProject depMavenProject =  dependency.getMavenProject(monitor);
      Iterator<AbstractDependencyConfigurator> configurators = depConfigurators.iterator();
      while (configurators.hasNext()) {
        try {
          configurators.next().configureDependency(mavenProject, project, depMavenProject, dependency.getProject(), monitor);
        } catch(MarkedException ex) {
          //XXX handle this
        }
      }
    }
  }
  
  /**
   * Get the context root from a maven web project
   * @param mavenProject
   * @return the final name of the project if it exists, or the project's artifactId.
   */
  protected String getContextRoot(MavenProject mavenProject) {
    String contextRoot;
	//MECLIPSEWTP-43 : Override with maven property
   String property = mavenProject.getProperties().getProperty(M2ECLIPSE_WTP_CONTEXT_ROOT);
   if (StringUtils.isEmpty(property)) {
  		String finalName = mavenProject.getBuild().getFinalName();
  		if (StringUtils.isBlank(finalName) 
  		   || finalName.equals(mavenProject.getArtifactId() + "-" + mavenProject.getVersion())) {
  		  contextRoot = mavenProject.getArtifactId();
  		}  else {
  		  contextRoot = finalName;
  		}
  	} else {
  		contextRoot = property;
  	}

    return contextRoot.trim().replace(" ", "_");
  }

  public void configureClasspath(IProject project, MavenProject mavenProject, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    
    //Improve skinny war support by generating the manifest classpath
    //similar to mvn eclipse:eclipse 
    //http://maven.apache.org/plugins/maven-war-plugin/examples/skinny-wars.html
    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject, project);
    WarPackagingOptions opts = new WarPackagingOptions(config);

    /*
     * Need to take care of three separate cases
     * 
     * 1. remove any project dependencies (they are represented as J2EE module dependencies)
     * 2. add non-dependency attribute for entries originated by artifacts with
     *    runtime, system, test scopes or optional dependencies (not sure about the last one)
     * 3. make sure all dependency JAR files have unique file names, i.e. artifactId/version collisions
     */

    Set<String> dups = new LinkedHashSet<String>();
    Set<String> names = new HashSet<String>();
    FileNameMapping fileNameMapping = config.getFileNameMapping();
    String targetDir = mavenProject.getBuild().getDirectory();

    // first pass removes projects, adds non-dependency attribute and collects colliding filenames
    Iterator<IClasspathEntryDescriptor> iter = classpath.getEntryDescriptors().iterator();
    while (iter.hasNext()) {
      IClasspathEntryDescriptor descriptor = iter.next();
      IClasspathEntry entry = descriptor.toClasspathEntry();
      String scope = descriptor.getScope();
      Artifact artifact = ArtifactHelper.getArtifact(mavenProject.getArtifacts(), descriptor.getArtifactKey());

      ArtifactHelper.fixArtifactHandler(artifact.getArtifactHandler());

      String deployedName = fileNameMapping.mapFileName(artifact);
    
      boolean isDeployed = (Artifact.SCOPE_COMPILE.equals(scope) || Artifact.SCOPE_RUNTIME.equals(scope)) 
    		  				&& !descriptor.isOptionalDependency() 
    		  				&& opts.isPackaged(deployedName)
    		  				&& !isWorkspaceProject(artifact);
      
      // add non-dependency attribute if this classpathentry is not meant to be deployed
      // or if it's a workspace project (projects already have a reference created in configure())
      if(!isDeployed) {
        descriptor.setClasspathAttribute(NONDEPENDENCY_ATTRIBUTE.getName(), NONDEPENDENCY_ATTRIBUTE.getValue());
      }
    
      //If custom fileName is used, then copy the artifact and rename the artifact under the build dir
      String fileName = entry.getPath().lastSegment(); 
      if (!deployedName.equals(fileName)) {
        IPath newPath = renameArtifact(targetDir, entry.getPath(), deployedName );
        if (newPath != null) {
          descriptor.setPath(newPath);
        }
      }
      
      if (!names.add(deployedName)) {
        dups.add(deployedName);
      }
    }

    // second pass disambiguates colliding entry file names
    iter = classpath.getEntryDescriptors().iterator();
    while (iter.hasNext()) {
      IClasspathEntryDescriptor descriptor = iter.next();
      IClasspathEntry entry = descriptor.toClasspathEntry();
      
      if (dups.contains(entry.getPath().lastSegment())) {
        String newName = descriptor.getGroupId() + "-" + entry.getPath().lastSegment();
        IPath newPath = renameArtifact(targetDir, entry.getPath(), newName );
        if (newPath != null) {
          descriptor.setPath(newPath);
        }
      }
    }
  }


  private IPath renameArtifact(String targetDir, IPath source, String newName) {
    File src = new File(source.toOSString());
    File dst = new File(targetDir, newName);
    try {
      if (src.isFile() && src.canRead()) {
        if (isDifferent(src, dst)) { // uses lastModified
          FileUtils.copyFile(src, dst);
          dst.setLastModified(src.lastModified());
        }
        return Path.fromOSString(dst.getCanonicalPath());
      }
    } catch(IOException ex) {
      log.error("File copy failed", ex);
    }
    return null;
  }

  private boolean isWorkspaceProject(Artifact artifact) {
	IMavenProjectFacade facade = projectManager.getMavenProject(artifact.getGroupId(), 
																	artifact.getArtifactId(),
																	artifact.getVersion());
		      
	return facade != null 
			&& facade.getFullPath(artifact.getFile()) != null;
	
  }

  private static boolean isDifferent(File src, File dst) {
    if (!dst.exists()) {
      return true;
    }

    return src.length() != dst.length() 
        || src.lastModified() != dst.lastModified();
  }
}
