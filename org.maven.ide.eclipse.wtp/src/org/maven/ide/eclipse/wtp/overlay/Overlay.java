/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.overlay;

import org.apache.maven.artifact.Artifact;

/**
 * Maven Overlay
 *
 * @author Fred Bricon
 */
public class Overlay implements IOverlay {

  private String targetPath;
  private Artifact artifact;

  public Overlay (Artifact artifact, String targetPath) {
    this.artifact = artifact;
    this.targetPath = targetPath;
  }
  
  /**
   * @see org.maven.ide.eclipse.wtp.overlay.IOverlay#getArtifact()
   */
  public Artifact getArtifact() {
    return artifact;
  }

  /**
   * @see org.maven.ide.eclipse.wtp.overlay.IOverlay#getTargetPath()
   */
  public String getTargetPath() {
    return targetPath;
  }

}
