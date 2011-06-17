/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.apache.maven.artifact.Artifact;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.maven.ide.eclipse.wtp.internal.AntPathMatcher;


/**
 * WarPackagingOptions
 * 
 * @author Lars Ködderitzsch
 * @author Fred Bricon
 */
public class WarPackagingOptions implements IPackagingConfiguration {

  private boolean isAddManifestClasspath;

  //these are used in the skinny use case to decide wheter a dependencies gets 
  //referenced from the ear, or if it is (exceptionally) placed in the WEB-INF/lib
  String[] packagingIncludes;

  String[] packagingExcludes;

  public WarPackagingOptions(WarPluginConfiguration config) {

    isAddManifestClasspath = config.isAddManifestClasspath();

    packagingExcludes = config.getPackagingExcludes();
    packagingIncludes = config.getPackagingIncludes();
  }

  public boolean isSkinnyWar() {
    return isAddManifestClasspath;
  }

  public boolean isReferenceFromEar(IClasspathEntryDescriptor descriptor) {

    String scope = descriptor.getScope();
    //these dependencies aren't added to the manifest cp
    //retain optional dependencies here, they might be used just to express the 
    //dependency to be used in the manifest
    if(Artifact.SCOPE_PROVIDED.equals(scope) || Artifact.SCOPE_TEST.equals(scope)
        || Artifact.SCOPE_SYSTEM.equals(scope)) {
      return false;
    }

    //calculate in regard to includes/excludes whether this jar is
    //to be packaged into  WEB-INF/lib
    String jarFileName = "WEB-INF/lib/" + descriptor.getPath().lastSegment();
    return isExcludedFromWebInfLib(jarFileName);
  }

  /**
   * @param depComponent
   * @return
   */
  public boolean isReferenceFromEar(String jarFileName) {

    //calculate in regard to includes/excludes wether this jar is
    //to be packaged into  WEB-INF/lib
    return isExcludedFromWebInfLib("WEB-INF/lib/" + jarFileName);
  }

  private boolean isExcludedFromWebInfLib(String virtualLibPath) {

    AntPathMatcher matcher = new AntPathMatcher();

    for(String excl : packagingExcludes) {
      if(matcher.match(excl, virtualLibPath)) {

        //stop here already, since exclusions seem to have precedence over inclusions
        //it is not documented as such for the maven war-plugin, I concluded this from experimentation
        //should be verfied, though
        return true;
      }
    }

    //so the path is not excluded, check if it is included into the war packaging
    for(String incl : packagingIncludes) {
      if(matcher.match(incl, virtualLibPath)) {
        return false;
      }
    }

    //if we're here it means the path has not been specifically included either
    //that means either no inclusions are defined at all (<packagingIncludes> missing or empty)
    //or the jar is really not included
    if(packagingIncludes.length == 0) {
      //undefined inclusions mean maven war plugin default -> will be included in war
      return false;
    } else {
      //specifically not included
      return true;
    }
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.IPackagingConfiguration#isIncluded(org.apache.maven.artifact.Artifact)
   */
  public boolean isPackaged(String fileName) {
    return !isExcludedFromWebInfLib("WEB-INF/lib/" + fileName);
  }

}
