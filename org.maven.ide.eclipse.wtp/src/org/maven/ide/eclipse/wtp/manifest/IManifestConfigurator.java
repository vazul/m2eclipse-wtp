/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * IManifestConfigurator
 *
 * @author Fred Bricon
 */
public interface IManifestConfigurator {

  public abstract void generateManifest(IMavenProjectFacade mavenFacade, IFile manifest, IProgressMonitor monitor) throws CoreException;

}
