/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.maven.ide.eclipse.wtp.internal.AntPathMatcher;


/**
 * PackagingConfiguration
 * 
 * @author Lars K�dderitzsch
 * @author Fred Bricon
 */
public class PackagingConfiguration implements IPackagingConfiguration {

  String[] packagingIncludes;

  String[] packagingExcludes;

  private AntPathMatcher matcher;

  public PackagingConfiguration(String[] packagingIncludes, String[] packagingExcludes) {
    this.packagingIncludes = toPortablePathArray(packagingIncludes);
    this.packagingExcludes = toPortablePathArray(packagingExcludes);
    matcher = new AntPathMatcher();
  }

  public boolean isPackaged(String virtualPath) {
    if (virtualPath == null) {
      return false;
    }
    virtualPath = toPortablePath(virtualPath);
    if (packagingIncludes != null) {
      for(String excl : packagingExcludes) {
        if(matcher.match(excl, virtualPath)) {
          //stop here already, since exclusions have precedence over inclusions
          return false;
        }
      }
    }

    //so the path is not excluded, check if it is included into packaging
    if (packagingIncludes == null || packagingIncludes.length == 0) {
      return true;
    }
    for(String incl : packagingIncludes) {
      if(matcher.match(incl, virtualPath)) {
        return true;
      }
    }

    //Definitely not included
    return false;
  }

  private String[] toPortablePathArray(String[] patterns) {
    if (patterns == null) {
      return null;
    }
    String[] newPatterns = new String[patterns.length];
    for (int i = 0; i < patterns.length; i++) {
      newPatterns[i] = toPortablePath(patterns[i]);
    }
    return newPatterns;
  }
  
  private String toPortablePath(String path) {
    return (path==null)?null:path.replace("\\", "/");
  }
  
}
