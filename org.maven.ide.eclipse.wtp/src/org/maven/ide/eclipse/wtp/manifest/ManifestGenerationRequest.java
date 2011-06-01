/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.manifest;

import org.maven.ide.eclipse.wtp.manifest.wrapper.IArchiver;
import org.maven.ide.eclipse.wtp.manifest.wrapper.IMavenArchiver;

/**
 * ManifestGenerationRequest
 *
 * @author fbricon
 */
public class ManifestGenerationRequest {

  /**
   * @return
   */
  public IMavenArchiver getMavenArchiverWrapper() {
    return null;
  }

  /**
   * @return
   */
  public IArchiver getPackageArchiverWrapper() {
    // TODO Auto-generated method getPackageArchiverWrapper
    return null;
  }

  /**
   * @return
   */
  public String getTargetDirectory() {
    // TODO Auto-generated method getTargetDirectory
    return null;
  }

}
