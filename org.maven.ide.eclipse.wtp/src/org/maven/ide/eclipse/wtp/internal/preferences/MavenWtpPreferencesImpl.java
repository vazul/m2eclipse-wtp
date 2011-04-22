/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.internal.preferences;

import org.maven.ide.eclipse.wtp.preferences.IMavenWtpPreferences;


/**
 * MavenWtpProjectPreferences
 *
 * @author Fred Bricon
 */
public class MavenWtpPreferencesImpl implements IMavenWtpPreferences {
  
  private boolean isApplicationXmGeneratedInBuildDirectory;
  
  private boolean isEnabledProjectSpecificSettings; 
  
  
  public boolean isApplicationXmGeneratedInBuildDirectory() {
    return isApplicationXmGeneratedInBuildDirectory;
  }

  /**
   * @see org.maven.ide.eclipse.wtp.preferences.IMavenWtpPreferences#setApplicationXmGeneratedInBuildDirectory(boolean)
   */
  public void setApplicationXmGeneratedInBuildDirectory(boolean isEnabled) {
    isApplicationXmGeneratedInBuildDirectory = isEnabled;
  }

  /**
   * @see org.maven.ide.eclipse.wtp.preferences.IMavenWtpPreferences#isEnabledProjectSpecificSettings()
   */
  public boolean isEnabledProjectSpecificSettings() {
    return isEnabledProjectSpecificSettings;
  }

  /**
   * @see org.maven.ide.eclipse.wtp.preferences.IMavenWtpPreferences#setEnabledProjectSpecificSettings(boolean)
   */
  public void setEnabledProjectSpecificSettings(boolean isEnabled) {
    isEnabledProjectSpecificSettings = isEnabled;    
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isApplicationXmGeneratedInBuildDirectory ? 1231 : 1237);
    result = prime * result + (isEnabledProjectSpecificSettings ? 1231 : 1237);
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if(this == obj)
      return true;
    if(obj == null)
      return false;
    if(getClass() != obj.getClass())
      return false;
    MavenWtpPreferencesImpl other = (MavenWtpPreferencesImpl) obj;
    if(isApplicationXmGeneratedInBuildDirectory != other.isApplicationXmGeneratedInBuildDirectory)
      return false;
    if(isEnabledProjectSpecificSettings != other.isEnabledProjectSpecificSettings)
      return false;
    return true;
  }
}
