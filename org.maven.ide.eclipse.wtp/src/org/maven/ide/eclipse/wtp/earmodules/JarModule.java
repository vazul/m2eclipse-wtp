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
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.JarModule
 * 
 * The {@link EarModule} implementation for a non J2EE module such as third party libraries. <p/> Such module is not
 * incorporated in the generated <tt>application.xml<tt>
 * but some application servers support it. To include it in the generated
 * deployment descriptor anyway, set the <tt>includeInApplicationXml</tt> boolean flag. <p/> This class deprecates
 * {@link org.apache.maven.plugin.ear.JavaModule}.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class JarModule extends AbstractEarModule {
  
  private boolean includeInApplicationXml = false;


  public JarModule(Artifact a) {
    super(a);
  }

  public JarModule() {
    super();
  }

  public String getType() {
    return "jar";
  }

  void setLibBundleDir(String defaultLibBundleDir) {
    if(defaultLibBundleDir != null && bundleDir == null) {
      this.bundleDir = defaultLibBundleDir;
    }
  }

  public boolean isIncludeInApplicationXml() {
    return includeInApplicationXml;
  }

  public void setIncludeInApplicationXml(boolean includeInApplicationXml) {
    this.includeInApplicationXml = includeInApplicationXml;
  }

  protected void setCustomValues(Xpp3Dom module) {
    Xpp3Dom contextRootDom = new Xpp3Dom("includeInApplicationXml");
    contextRootDom.setValue(Boolean.toString(includeInApplicationXml));
    module.addChild(contextRootDom); 
  }
  
}
