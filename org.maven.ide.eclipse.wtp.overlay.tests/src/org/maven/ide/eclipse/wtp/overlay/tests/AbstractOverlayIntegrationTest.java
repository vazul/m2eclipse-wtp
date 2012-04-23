/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.maven.ide.eclipse.wtp.overlay.tests;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.junit.After;
import org.junit.Before;
import org.maven.ide.eclipse.wtp.common.tests.AbstractIntegrationTest;


public class AbstractOverlayIntegrationTest extends AbstractIntegrationTest {
  
  private static final String REPO = "REPO";
  
  protected File repo;
  
  @Before
  public void addRepoVariable() throws Exception {
    
    IPath repoPath = JavaCore.getClasspathVariable("repo");
    if (repoPath == null) {
      repo = new File(REPO.toLowerCase()).getCanonicalFile();
      
      JavaCore.setClasspathVariable(REPO, //
          new Path(repo.getAbsolutePath()), //
          new NullProgressMonitor());
    }
  }
  
  @After 
  public void clean() {
    JavaCore.removeClasspathVariable(REPO, new NullProgressMonitor());
    repo = null;
  }

}
