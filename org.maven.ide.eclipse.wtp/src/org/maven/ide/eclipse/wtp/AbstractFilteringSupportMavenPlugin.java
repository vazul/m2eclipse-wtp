/*******************************************************************************
 * Copyright (c) 2011 JBoss by Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.maven.ide.eclipse.wtp.internal.StringUtils;

/**
 * AbstractFilteringSupportMavenPlugin
 *
 * @author Fred Bricon
 */
public abstract class AbstractFilteringSupportMavenPlugin {
  
  private Xpp3Dom configuration;

  public final void setConfiguration(Xpp3Dom configuration) {
    this.configuration = configuration;
  }

  public Xpp3Dom getConfiguration() {
    return configuration;
  }
  
  public String getEscapeString() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      return DomUtils.getChildValue(config, "escapeString");
    }
    return null;
  }

  public Xpp3Dom[] getNonfilteredExtensions() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom extensionsNode = config.getChild("nonFilteredFileExtensions");
      if (extensionsNode != null && extensionsNode.getChildCount() > 0) {
        return extensionsNode.getChildren();
      }
    }
    return new Xpp3Dom[0];
  }
  
  public Collection<String> getFilters() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom filtersNode = config.getChild("filters");
      if (filtersNode != null && filtersNode.getChildCount() > 0) {
        List<String> filters = new ArrayList<String>(filtersNode.getChildCount());
        for (Xpp3Dom filterNode : filtersNode.getChildren("filter")) {
          String  filter = filterNode.getValue();
          if (!StringUtils.nullOrEmpty(filter)) {
            filters.add(filter);
          }
        }
        return filters;
      }
    }
    return Collections.emptyList();
  }  
  
  public boolean isFilteringDeploymentDescriptorsEnabled()  {
    Xpp3Dom configuration = getConfiguration();
    if(configuration == null) {
      return false;
    }
    return DomUtils.getBooleanChildValue(configuration, getFilteringAttribute());
  }

  protected abstract String getFilteringAttribute();
}
