/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.wtp.AbstractDependencyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Extension reader
 * 
 * @author Eugene Kuleshov
 */
public class ExtensionReader {

  public static final String EXTENSION_DEPENDENCY_CONFIGURATORS = "org.maven.ide.eclipse.wtp.dependencyConfigurators";
  
  private static final Logger log = LoggerFactory.getLogger(ExtensionReader.class);
      
  private static final String ELEMENT_CONFIGURATOR = "configurator";
  
  private static ArrayList<AbstractDependencyConfigurator> dependencyConfigurators;

  public static List<AbstractDependencyConfigurator> readDependencyConfiguratorExtensions(MavenProjectManager projectManager,
      MavenRuntimeManager runtimeManager, IMavenMarkerManager markerManager) {
    if (dependencyConfigurators == null) {
      dependencyConfigurators = new ArrayList<AbstractDependencyConfigurator>();
      
      IExtensionRegistry registry = Platform.getExtensionRegistry();
      IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_DEPENDENCY_CONFIGURATORS);
      if(configuratorsExtensionPoint != null) {
        IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
        for(IExtension extension : configuratorExtensions) {
          IConfigurationElement[] elements = extension.getConfigurationElements();
          for(IConfigurationElement element : elements) {
            if(element.getName().equals(ELEMENT_CONFIGURATOR)) {
              try {
                Object o = element.createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);
  
                AbstractDependencyConfigurator projectConfigurator = (AbstractDependencyConfigurator) o;
                projectConfigurator.setProjectManager(projectManager);
                projectConfigurator.setRuntimeManager(runtimeManager);
                projectConfigurator.setMarkerManager(markerManager);
                
                dependencyConfigurators.add(projectConfigurator);
              } catch(CoreException ex) {
                log.error("Error configuring dependency configurator", ex);
              }
            }
          }
        }
      }
      
      return dependencyConfigurators;
    }
    
    return dependencyConfigurators;
  }
}

