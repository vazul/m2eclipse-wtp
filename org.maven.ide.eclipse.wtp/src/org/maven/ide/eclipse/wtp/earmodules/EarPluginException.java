/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * The Apache License v2.0 is available at
 *   http://www.apache.org/licenses/LICENSE-2.0.html
 * You may elect to redistribute this code under either of these licenses.
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.earmodules;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.internal.IMavenConstants;


/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.EarPluginException
 * 
 * The base exception of the EAR plugin.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @author Fred Bricon
 */
public class EarPluginException extends CoreException {
  private static final long serialVersionUID = -819727447130647982L;

  private static final String DEFAULT_MESSAGE = "Error in ear plugin configuration";

  public EarPluginException() {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, DEFAULT_MESSAGE));
  }

  public EarPluginException(String message) {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, message));
  }

  public EarPluginException(Throwable cause) {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, DEFAULT_MESSAGE, cause));
  }

  public EarPluginException(String message, Throwable cause) {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, message, cause));
  }
}
