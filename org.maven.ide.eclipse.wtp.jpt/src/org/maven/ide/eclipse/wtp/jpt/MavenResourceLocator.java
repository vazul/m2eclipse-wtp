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
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jpt.common.core.internal.resource.ModuleResourceLocator;
import org.eclipse.jpt.common.core.internal.resource.SimpleJavaResourceLocator;
import org.eclipse.jpt.common.core.resource.ResourceLocator;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;

/**
 * Maven resource Locator
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class MavenResourceLocator implements ResourceLocator {
	
  private static IPath META_INF_PATH = new Path("META-INF");

  /**
   * Accepts all resources not under the build output and test build output folders
   */
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
      accept = getDefaultDelegate().acceptResourceLocation(project, container);
    }
    //System.err.println("acceptResourceLocation(" + project +", "+ container + ") ="+ accept );
    return accept;
	}

	
	private ResourceLocator getDelegate(IProject project) {
	  if (ModuleCoreNature.isFlexibleProject(project)) {
	    return new ModuleResourceLocator();
	  }
    return getDefaultDelegate();
  }
	
	 private ResourceLocator getDefaultDelegate() {
	    return new SimpleJavaResourceLocator();
	 }

	/**
	 * Returns the resource path from Maven's resource folders mapped to the runtimePath.  
	 */
  public IPath getResourcePath(IProject project, IPath runtimePath) {
    IPath resourcePath = null; 
    IMavenProjectFacade mavenProjectFacade = getMavenProjectFacade(project);
    if (mavenProjectFacade != null && mavenProjectFacade.getMavenProject() != null) {
      for (Resource resourceFolder : mavenProjectFacade.getMavenProject().getBuild().getResources()) {
        resourcePath = getFilePath(getWorkspaceRelativePath(resourceFolder), runtimePath);
        if (resourcePath != null) {
          break;
        }
      } 
    } else {
      //Maven project not loaded yet, we fallback on the JavaProject source folders lookup
      IJavaProject javaProject = JavaCore.create(project);
      try {
        for(IClasspathEntry entry : javaProject.getRawClasspath()) {
          if (IClasspathEntry.CPE_SOURCE == entry.getEntryKind()) {
            resourcePath = getFilePath(entry.getPath(), runtimePath);
            if (resourcePath != null) {
              break;
            }
          }
        }
      } catch (JavaModelException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    System.err.println("getResourcePath (" + project + ", " + runtimePath + ") = " + resourcePath);
    if (resourcePath == null) {
      resourcePath = getDefaultDelegate().getResourcePath(project, runtimePath);
    }
    return resourcePath;
  }

  private IPath getFilePath(IPath containerPath, IPath runtimePath) {
    if (containerPath != null){
      final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IFile resource = root.getFile(containerPath.append(runtimePath));
      if (resource.exists()) {
        return resource.getFullPath();
      }
    }
    return null;
  }

  /**
   * Returns the META-INF folder found in one of Maven's resource.  
   */
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
      defaultLocation = getDefaultDelegate().getDefaultResourceLocation(project);
    }
    return defaultLocation;
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

  public IPath getRuntimePath(IProject project, IPath resourcePath) {
    IPath runtimePath = getDelegate(project).getRuntimePath(project, resourcePath);
    System.err.println("getRuntimePath " + project + " : " + resourcePath + " = " + runtimePath);
    return runtimePath;
  }
}
