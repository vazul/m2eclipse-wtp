/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.common.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.After;
import org.junit.Before;

public class AbstractIntegrationTest {

  protected String projectsDir;
  
  protected IWorkspace workspace;
  
  public AbstractIntegrationTest(String baseDir) {
    projectsDir = baseDir;
  }
  
  public AbstractIntegrationTest() {
    this("projects");
  }
  
  @Before
  public void setUp() {
    workspace = ResourcesPlugin.getWorkspace();
  }
  
  protected void deleteProjects() throws CoreException {
    for (IProject p : workspace.getRoot().getProjects()) {
      p.delete(IResource.FORCE, new NullProgressMonitor());
    }
  }
  
  @After
  public void tearDown() throws CoreException {
    deleteProjects();
    workspace = null;
  }
  
  protected IProject importProject(String projectName) throws Exception {
    return WorkspaceUtil.importProject(projectsDir, workspace, projectName);
  }
  
  protected IProject[] importProjects(String ... projectNames) throws Exception {
    return WorkspaceUtil.importProjects(projectsDir, workspace, projectNames);
  }
}
