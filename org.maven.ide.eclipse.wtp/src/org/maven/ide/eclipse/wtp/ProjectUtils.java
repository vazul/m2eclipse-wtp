/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.io.File;

import org.eclipse.core.resources.IProject;

/**
 * ProjectUtils
 *
 * @author Fred Bricon
 */
public class ProjectUtils {

  /**
   * Transform an absolute path into a relative path to a project, if possible
   * @param project
   * @param absolutePath : relative path to the project
   * @return
   */
  public static String getRelativePath(IProject project, String absolutePath){
	//Code copied from org.maven.ide.eclipse.jdt.internal.AbstractJavaProjectConfigurator 
	//since Path.makeRelativeTo() doesn't work on Linux
    File basedir = project.getLocation().toFile();
    String relative;
    if(absolutePath.equals(basedir.getAbsolutePath())) {
      relative = ".";
    } else if(absolutePath.startsWith(basedir.getAbsolutePath())) {
      relative = absolutePath.substring(basedir.getAbsolutePath().length() + 1);
    } else {
      relative = absolutePath;
    }
    return relative.replace('\\', '/'); //$NON-NLS-1$ //$NON-NLS-2$
  }
}
