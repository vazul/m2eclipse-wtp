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
import org.codehaus.plexus.util.xml.Xpp3Dom;


/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.WebModule
 * 
 * The {@link EarModule} implementation for a Web application module.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class WebModule extends AbstractEarModule {
  
  private String contextRoot;

  public WebModule() {
    super();
  }

  public WebModule(Artifact a) {
    super(a);
  }

  /**
   * Returns the context root to use for the web module. <p/> Note that this might return <tt>null</tt> till the
   * artifact has been resolved.
   * 
   * @return the context root
   */
  public String getContextRoot() {
    // Context root has not been customized - using default
    if(contextRoot == null) {
      contextRoot = getDefaultContextRoot(getArtifact());
    }
    return contextRoot;
  }

  public String getType() {
    return "war";
  }

  /**
   * Generates a default context root for the given artifact, based on the <tt>artifactId</tt>.
   * 
   * @param a the artifact
   * @return a context root for the artifact
   */
  private static String getDefaultContextRoot(Artifact a) {
    if(a == null) {
      throw new NullPointerException("Artifact could not be null.");
    }
    return "/" + a.getArtifactId();
  }

  public void setContextRoot(String contextRoot) {
    this.contextRoot = contextRoot;
  }
  
  protected void setCustomValues(Xpp3Dom module) {
    Xpp3Dom contextRootDom = new Xpp3Dom("contextRoot");
    contextRootDom.setValue(getContextRoot());
    module.addChild(contextRootDom);
  }

  protected String getModuleType() {
    return "webModule";
  }
}
