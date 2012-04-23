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

import org.apache.maven.artifact.Artifact;


/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.ParModule
 *  
 * The {@link EarModule} implementation for a Par module.
 * 
 * @author Stephane Nicoll <snicoll@apache.org>
 */
public class ParModule extends EjbModule {

  public ParModule() {
    super();
  }

  public ParModule(Artifact a) {
    super(a);
  }

  public String getType() {
    return "par";
  }
  
  protected String getModuleType() {
    return "parModule";
  }
  
}
