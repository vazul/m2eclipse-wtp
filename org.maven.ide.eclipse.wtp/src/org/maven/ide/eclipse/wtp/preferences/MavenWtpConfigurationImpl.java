/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * MavenWtpIntegrationConfiguration
 *
 * @author fbricon
 */
public class MavenWtpConfigurationImpl implements IMavenWtpConfiguration {

  private IPreferenceStore preferenceStore;

  public MavenWtpConfigurationImpl (IPreferenceStore preferenceStore) {
    this.preferenceStore = preferenceStore;
  }
  
  /** 
   * @see org.maven.ide.eclipse.wtp.preferences.IMavenWtpConfiguration#isApplicationXmGeneratedInBuildDirectoryl()
   */
  public boolean isApplicationXmGeneratedInBuildDirectoryl() {
    return preferenceStore.getBoolean(MavenWtpPreferencesConstants.P_APPLICATION_XML_IN_BUILD_DIR);
  }

}
