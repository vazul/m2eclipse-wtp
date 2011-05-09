package org.maven.ide.eclipse.wtp.common.tests;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class WorkspaceUtil {

  public static IProject[] importProjects(String baseDir, IWorkspace workspace, String ... projectNames) throws Exception {
    if (projectNames == null || projectNames.length == 0) {
      return null;
    }
    IProject[] projects = new IProject[projectNames.length];
    int i = 0;
    for(String projectName : projectNames) {
      projects[i++] = importProject(baseDir, workspace, projectName);
    }
    return projects;
  }

  public static IProject importProject(String baseDir, IWorkspace workspace, String projectName) throws Exception {
    IProgressMonitor monitor = new NullProgressMonitor();
    IProject project = workspace.getRoot().getProject(projectName);
    if (project.exists()) {
      project.delete(true, monitor);
    }
    File base = new File(baseDir).getCanonicalFile();
    File src = new File(base, projectName);
    File dst = new File(workspace.getRoot().getLocation().toFile(), projectName);
    copyFiles(src, dst);
    project = workspace.getRoot().getProject(projectName);
    project.create(monitor);
    if(!project.isOpen()) {
      project.open(monitor);
    }
    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    return project;
  }

  private static void copyFiles(File srcDir, File destDir) throws IOException {
    //Use some library here
    FileUtils.copyDirectory(srcDir, destDir);
  }

}
