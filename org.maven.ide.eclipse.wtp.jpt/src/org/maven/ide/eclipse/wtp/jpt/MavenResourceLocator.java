package org.maven.ide.eclipse.wtp.jpt;

import java.io.File;

import org.apache.maven.model.Resource;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jpt.common.core.JptCommonCorePlugin;
import org.eclipse.jpt.common.core.internal.resource.SimpleJavaResourceLocator;
import org.eclipse.jpt.common.core.resource.ResourceLocator;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

@SuppressWarnings("restriction")
public class MavenResourceLocator extends SimpleJavaResourceLocator implements ResourceLocator {
	
	@Override
	public boolean acceptResourceLocation(IProject project, IContainer container) {
    IMavenProjectFacade mavenProjectFacade = getMavenProjectFacade(project);
    boolean accept = true;
    if (mavenProjectFacade != null && mavenProjectFacade.getMavenProject() != null) {
      IPath classesPath = mavenProjectFacade.getOutputLocation();
      IPath testClassesPath = mavenProjectFacade.getTestOutputLocation();
      if (classesPath.isPrefixOf(container.getFullPath())
        || testClassesPath.isPrefixOf(container.getFullPath())) {
        //Reject everything coming from target/classes and target/testClasses
        accept = false;
      }
    } else {
      //Maven project not loaded yet, fallback to default behaviour.
      accept = super.acceptResourceLocation(project, container);
    }
    System.err.println("acceptResourceLocation(" + project +", "+ container + ") ="+ accept );
    return accept;
    
	}

	/*
  @Override 
  public IPath _getResourcePath(IProject project, IPath runtimePath) {
    IPath resourcePath = super.getResourcePath(project, runtimePath); 
    if (isFiltered(getMavenProjectFacade(project), resourcePath)) {
      IPath filteredResourcePath = getFilteredResourcePath(project, runtimePath);
      if (filteredResourcePath != null) {
        resourcePath = filteredResourcePath;
      }
    }
		System.err.println("getResourcePath (" + project + ", " + runtimePath + ") = " + resourcePath);
		return resourcePath;
	}
  */

	@Override 
  public IPath getResourcePath(IProject project, IPath runtimePath) {
    IPath resourcePath = null; 
    IMavenProjectFacade mavenProjectFacade = getMavenProjectFacade(project);
    if (mavenProjectFacade != null && mavenProjectFacade.getMavenProject() != null) {
      final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      for (Resource resourceFolder : mavenProjectFacade.getMavenProject().getBuild().getResources()) {
        IPath p = getWorkspaceRelativePath(resourceFolder);
        if (p != null){
          IFile resource = root.getFile(p.append(runtimePath));
          if (resource.exists()) {
            resourcePath = resource.getFullPath();
            break;
          }
        }
      }
    }
    System.err.println("getResourcePath (" + project + ", " + runtimePath + ") = " + resourcePath);
    if (resourcePath == null) {
      resourcePath = super.getResourcePath(project, runtimePath);
    }
    return resourcePath;
  }
  
  @Override
  public IContainer getDefaultResourceLocation(IProject project) {
    IMavenProjectFacade mavenProjectFacade = getMavenProjectFacade(project);
    IContainer defaultLocation = null;
    if (mavenProjectFacade != null && mavenProjectFacade.getMavenProject() != null) {
      final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      for (Resource resourceFolder : mavenProjectFacade.getMavenProject().getBuild().getResources()) {
        IPath p = getWorkspaceRelativePath(resourceFolder);
        if (p != null){
          IFolder candidate = root.getFolder(p.append(META_INF_PATH));
          if (candidate.exists()) {
            System.err.println("getDefaultResourceLocation = "+candidate);
            return candidate;
          }
          if (defaultLocation == null) {
            defaultLocation = candidate;
          }
        }
      }
    }
  
    if (defaultLocation == null) {
      defaultLocation = super.getDefaultResourceLocation(project);
    }
    return defaultLocation;
  }
  
  private boolean isFiltered(IMavenProjectFacade mavenProjectFacade, IPath resourcePath) {
    if (mavenProjectFacade == null || mavenProjectFacade.getMavenProject() == null) {
      return false;
    }
    for (Resource resourceFolder : mavenProjectFacade.getMavenProject().getBuild().getResources()) {
      if (contains(mavenProjectFacade.getProject(), resourceFolder, resourcePath )) {
        return resourceFolder.isFiltering();
      }
    }
    
    return false;
  }
  
  private boolean contains(IProject project, Resource mavenResourceFolder, IPath jpaResourcePath) {
    IPath mavenResourceFolderRelativePath = getWorkspaceRelativePath(mavenResourceFolder);
    return (mavenResourceFolderRelativePath != null) && mavenResourceFolderRelativePath.isPrefixOf(jpaResourcePath);
  }

  private IPath getWorkspaceRelativePath(Resource mavenResourceFolder) {
    File resourceDirectory = new File(mavenResourceFolder.getDirectory());
    IPath relativePath = null;
    if(resourceDirectory.exists() && resourceDirectory.isDirectory()) {
      relativePath = getWorkspaceRelativePath(mavenResourceFolder.getDirectory());
    }
    return relativePath;
  }
  
  private IPath getWorkspaceRelativePath(String absolutePath) {
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IPath relativePath = new Path(absolutePath).makeRelativeTo(root.getLocation());
    return relativePath;
  }
  
  /**
   * Returns the cached IMavenProjectFacade in m2e's project registry, 
   * or null if the project was not cached yet.
   */
  private IMavenProjectFacade getMavenProjectFacade(IProject project) {
    return MavenPlugin.getMavenProjectRegistry().getProject(project);
  }

  private IPath getFilteredResourcePath(IProject project, IPath runtimePath) {
    IJavaProject javaProject = JavaCore.create(project);
    IPath resourcePath = null;
    try {
      IPath buildOutput = javaProject.getOutputLocation();
      resourcePath = buildOutput.append(runtimePath);
    } catch (JavaModelException jme) {
      JptCommonCorePlugin.log(jme);
    }
    return resourcePath;
  }

  @Override
	public IPath getRuntimePath(IProject project, IPath resourcePath) {
    //Never called, can't find any reference to this method 
    IPath runtimePath = super.getRuntimePath(project, resourcePath);
		System.err.println("getRuntimePath " + project + " : " + resourcePath + " = " + runtimePath);
    return runtimePath;
	}

}
