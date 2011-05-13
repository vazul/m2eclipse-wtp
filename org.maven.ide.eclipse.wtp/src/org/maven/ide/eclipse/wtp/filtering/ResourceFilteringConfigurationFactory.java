/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.filtering;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.maven.ide.eclipse.wtp.JEEPackaging;

/**
 * ResourceFilteringConfigurationFactory
 *
 * @author Fred Bricon
 */
public class ResourceFilteringConfigurationFactory {

  //TODO Use an extension point to let 3rd party plugin register their own ResourceFilteringConfiguration 
  public static ResourceFilteringConfiguration getConfiguration(IMavenProjectFacade mavenProjectFacade) {

    JEEPackaging packaging = JEEPackaging.getValue(mavenProjectFacade.getPackaging());
    if (JEEPackaging.WAR == packaging)
    {
      return new WebResourceFilteringConfiguration(mavenProjectFacade);
    } else if (JEEPackaging.EAR == packaging)
    {
      return new EarResourceFilteringConfiguration(mavenProjectFacade);
    }
    return null;
  }

}
