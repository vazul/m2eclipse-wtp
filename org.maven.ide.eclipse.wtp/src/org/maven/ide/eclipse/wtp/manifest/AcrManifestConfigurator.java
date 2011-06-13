/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.manifest;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;

/**
 * AcrManifestConfigurator
 *
 * @author Fred Bricon
 */
public class AcrManifestConfigurator extends JarManifestConfigurator {

  protected MojoExecutionKey getExecutionKey() {
    MojoExecutionKey key = new MojoExecutionKey("org.apache.maven.plugins", "maven-acr-plugin", "", "acr", null, null);
    return key;
  }
}
