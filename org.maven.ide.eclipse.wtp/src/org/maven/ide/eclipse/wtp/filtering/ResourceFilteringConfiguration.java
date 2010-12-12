/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.filtering;

import java.util.List;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.IPath;

/**
 * ResourceFilteringConfiguration
 *
 * @author Fred Bricon
 */
public interface ResourceFilteringConfiguration {

  IPath getTargetFolder();
  
  //FIXME return List<org.apache.maven.model.Resource> instead
  List<Xpp3Dom> getResources();
}
