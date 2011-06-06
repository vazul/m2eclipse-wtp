/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.manifest;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.maven.ide.eclipse.wtp.WTPProjectsUtil;
import org.maven.ide.eclipse.wtp.namemapping.FileNameMappingFactory;

/**
 * JarManifestConfigurator
 *
 * @author fbricon
 */
public class JarManifestConfigurator extends AbstractManifestConfigurator {

  protected IPath getManifestdir(IMavenProjectFacade facade) {
    IPath outputLocation = facade.getOutputLocation();
    return outputLocation;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.manifest.AbstractManifestConfigurator#getArchiverFieldName()
   */
  protected String getArchiverFieldName() {
    return "jarArchiver";
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.manifest.AbstractManifestConfigurator#getExecutionKey()
   */
  protected MojoExecutionKey getExecutionKey() {
    MojoExecutionKey key = new MojoExecutionKey("org.apache.maven.plugins", "maven-jar-plugin", "", "jar", null, null);
    return key;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.manifest.AbstractManifestConfigurator#getClasspathFileNames()
   */
  protected List<String> getClasspathFileNames(List<Artifact> artifacts) {
    if (artifacts == null) {
      return null;
    }
    List<String> fileNames = new ArrayList<String>(artifacts.size());
    for (Artifact artifact : artifacts) {
      fileNames.add(getFileName(artifact));
    }
    return fileNames;
  }

  /**
   * @param artifact
   * @return
   */
  private String getFileName(Artifact artifact) {
    return FileNameMappingFactory.getDefaultFileNameMapping().mapFileName(artifact);
  }

}
