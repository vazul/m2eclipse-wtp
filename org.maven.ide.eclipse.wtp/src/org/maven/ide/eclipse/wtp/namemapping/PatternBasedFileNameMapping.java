/*******************************************************************************
 * Copyright (c) 2011 JBoss by Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.namemapping;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.war.util.MappingUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.wtp.MavenWtpPlugin;

/**
 * Pattern Based FileName Mapping
 *
 * @author Fred Bricon
 */
public class PatternBasedFileNameMapping implements FileNameMapping {

  private String pattern;
  
  public PatternBasedFileNameMapping(String pattern) {
    if (pattern == null || pattern.trim().length() == 0) {
      pattern = "@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@";
    }
    this.pattern = pattern;
  }

  public String mapFileName(Artifact artifact) {
    try {
      return MappingUtils.evaluateFileNameMapping(pattern, artifact);
    } catch(InterpolationException ex) {
      throw new RuntimeException(ex);
      //throw new CoreException(new Status(IStatus.ERROR, MavenWtpPlugin.ID, "File name can not be resolved", ex));
    }
  }

}
