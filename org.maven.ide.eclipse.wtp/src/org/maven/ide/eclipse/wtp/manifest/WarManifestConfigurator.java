/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.manifest;

import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.maven.ide.eclipse.wtp.IPackagingConfiguration;
import org.maven.ide.eclipse.wtp.MavenWtpConstants;
import org.maven.ide.eclipse.wtp.ProjectUtils;
import org.maven.ide.eclipse.wtp.WarPackagingOptions;
import org.maven.ide.eclipse.wtp.WarPluginConfiguration;
import org.maven.ide.eclipse.wtp.namemapping.FileNameMappingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WarManifestConfigurator
 *
 * @author Fred Bricon
 */
public class WarManifestConfigurator extends JarManifestConfigurator implements IJavaProjectConfigurator {

  private static final Logger log = LoggerFactory.getLogger(WarManifestConfigurator.class);

  static final IClasspathAttribute NONDEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY, "");
  
  protected IPath getManifestdir(IMavenProjectFacade facade) {
    IProject project = facade.getProject();
    IPath localEarResourceFolder =  ProjectUtils.getM2eclipseWtpFolder(facade.getMavenProject(), project);
    return project.getFullPath().append(localEarResourceFolder)
                                .append(MavenWtpConstants.WEB_RESOURCES_FOLDER);
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.manifest.AbstractManifestConfigurator#getArchiverFieldName()
   */
  protected String getArchiverFieldName() {
    return "warArchiver";
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.manifest.AbstractManifestConfigurator#getExecutionKey()
   */
  protected MojoExecutionKey getExecutionKey() {
    MojoExecutionKey key = new MojoExecutionKey("org.apache.maven.plugins", "maven-war-plugin", "", "war", null, null);
    return key;
  }
  
  protected IPackagingConfiguration getPackagingConfiguration (IMavenProjectFacade facade) {
    WarPluginConfiguration warconfig = new WarPluginConfiguration(facade.getMavenProject(), facade.getProject());
    IPackagingConfiguration packagingconfig = new WarPackagingOptions(warconfig);
    return packagingconfig;
  }
  
  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
      throws CoreException {
    return;
    /*
    IProject project = facade.getProject();
    MavenProject mavenProject = facade.getMavenProject();
  
    IJavaProject javaProject = JavaCore.create(project);
    if (javaProject == null) return;
  
    //Improve skinny war support by generating the manifest classpath
    //similar to mvn eclipse:eclipse 
    //http://maven.apache.org/plugins/maven-war-plugin/examples/skinny-wars.html
    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject, project);
    WarPackagingOptions opts = new WarPackagingOptions(config);

    IClasspathEntry[] earContainerEntries = ProjectUtils.getEarContainerEntries(javaProject);
    /*
     * Need to take care of three separate cases
     * 
     * 1. remove any project dependencies (they are represented as J2EE module dependencies)
     * 2. add non-dependency attribute for entries originated by artifacts with
     *    runtime, system, test scopes or optional dependencies (not sure about the last one)
     * 3. make sure all dependency JAR files have unique file names, i.e. artifactId/version collisions
     * /

    // first pass removes projects, adds non-dependency attribute and collects colliding filenames
    Iterator<IClasspathEntryDescriptor> iter = classpath.getEntryDescriptors().iterator();
    while (iter.hasNext()) {
      IClasspathEntryDescriptor descriptor = iter.next();
      IClasspathEntry entry = descriptor.toClasspathEntry();
      String scope = descriptor.getScope();
      String key = ArtifactUtils.versionlessKey(descriptor.getGroupId(),descriptor.getArtifactId());
      Artifact artifact = mavenProject.getArtifactMap().get(key);

      //Remove dependent project from the Maven Library, as it's supposed to be brought by the Web Library
      if(IClasspathEntry.CPE_PROJECT == entry.getEntryKind()
      && (Artifact.SCOPE_COMPILE.equals(scope) || Artifact.SCOPE_RUNTIME.equals(scope))) {
        //get deployed name for project dependencies
        //TODO can this be done somehow more elegantly?
        IProject p = (IProject) ResourcesPlugin.getWorkspace().getRoot().findMember(entry.getPath());
        
        IVirtualComponent component = ComponentCore.createComponent(p);
        //component will be null if the underlying project hasn't been configured properly
        if(component == null){
          continue;
        }
        if (!descriptor.isOptionalDependency()) {
          // remove mandatory project dependency from classpath
          iter.remove();
          continue;
        }
      }
      
      String deployedName = FileNameMappingFactory.getDefaultFileNameMapping().mapFileName(artifact);
      boolean packaged  = opts.isPackaged(deployedName);
      boolean usedInEar = !packaged && isUsedInEar(earContainerEntries, deployedName);

      if (usedInEar) {
        // remove mandatory project dependency from classpath
        iter.remove();
        continue;
      }//else : optional dependency not used in ear -> need to trick ClasspathAttribute with NONDEPENDENCY_ATTRIBUTE 
    
      // add non-dependency attribute
      // Check the scope & set WTP non-dependency as appropriate
      // Optional artifact shouldn't be deployed
      if((Artifact.SCOPE_PROVIDED.equals(scope) || Artifact.SCOPE_TEST.equals(scope)
          || Artifact.SCOPE_SYSTEM.equals(scope) || descriptor.isOptionalDependency()) || !packaged) {
        descriptor.setClasspathAttribute(NONDEPENDENCY_ATTRIBUTE.getName(), NONDEPENDENCY_ATTRIBUTE.getValue());
      }
    }
    */
  }

  /**
   * @param earContainerEntries
   * @param deployedName
   * @return
   */
  private boolean isUsedInEar(IClasspathEntry[] earContainerEntries, String deployedName) {
    if (earContainerEntries == null || earContainerEntries.length == 0)
    return false;
    
    for(IClasspathEntry entry : earContainerEntries) {
      if (deployedName.equals(entry.getPath().lastSegment())){
        return true;
      }
    }
    return false;
  }


  public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    // TODO Auto-generated method configureRawClasspath
    
  }
}
