/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.filtering;

import org.maven.ide.eclipse.project.IMavenProjectFacade;

/**
 * AbstractResourceFilteringConfiguration
 *
 * @author Fred Bricon
 */
public abstract class AbstractResourceFilteringConfiguration implements ResourceFilteringConfiguration {

  protected IMavenProjectFacade mavenProjectFacade;
  
  public AbstractResourceFilteringConfiguration(IMavenProjectFacade mavenProjectFacade) {
    this.mavenProjectFacade = mavenProjectFacade;
  }
  
}
