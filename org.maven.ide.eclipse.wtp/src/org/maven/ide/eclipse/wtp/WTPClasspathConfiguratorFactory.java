/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.jdt.AbstractClasspathConfigurator;
import org.maven.ide.eclipse.jdt.AbstractClasspathConfiguratorFactory;
import org.maven.ide.eclipse.project.IMavenProjectFacade;


/**
 * @author Igor Fedorenko
 */
public class WTPClasspathConfiguratorFactory extends AbstractClasspathConfiguratorFactory {

  @Override
  public AbstractClasspathConfigurator createConfigurator(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);
    if(WarPluginConfiguration.isWarProject(mavenProject)) {
      return new WTPClasspathConfigurator(mavenProject.getBuild().getDirectory());
    }
    return null;
  }

}
