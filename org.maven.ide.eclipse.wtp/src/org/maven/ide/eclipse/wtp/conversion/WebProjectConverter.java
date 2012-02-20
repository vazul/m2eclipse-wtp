/*******************************************************************************
 * Copyright (c) 2012 JBoss by Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.conversion;

import static org.maven.ide.eclipse.wtp.conversion.MavenPluginUtils.configure;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;

/**
 * Converts Eclipse WTP Dynamic Web project settings into maven-war-plugin configuration 
 *
 * @author Fred Bricon
 */
public class WebProjectConverter extends AbstractWtpProjectConversionParticipant {

  private static final String DEFAULT_WAR_SOURCE_FOLDER = "src/main/webapp";
  
  private static final String WAR_SOURCE_DIRECTORY_KEY = "warSourceDirectory";

  private static final String FAIL_IF_MISSING_WEBXML_KEY = "failOnMissingWebXml";

  public void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException {
    if (!accept(project) && !"war".equals(model.getPackaging())) {
      return;
    }
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component == null) {
      return;
    }

    setWarPlugin(component, model);
  }

  private void setWarPlugin(IVirtualComponent component, Model model) {
    Build build = getOrCreateBuild(model);
    Plugin warPlugin = setPlugin(build, "org.apache.maven.plugins", "maven-war-plugin", "2.2");
  
    IFolder webContentFolder = findWebRootFolder(component);
    String webContent = webContentFolder.getProjectRelativePath().toPortableString();
    
    if (!DEFAULT_WAR_SOURCE_FOLDER.equals(webContent)) {
      configure(warPlugin, WAR_SOURCE_DIRECTORY_KEY, webContent);
    }
    
    configure(warPlugin, FAIL_IF_MISSING_WEBXML_KEY, "false");

    model.setBuild(build);
  }

  private IFolder findWebRootFolder(IVirtualComponent component) {
    return component.getProject().getFolder("WebContent");
  }

  protected IProjectFacet getRequiredFaced() {
    return WebFacetUtils.WEB_FACET;
  }

}
