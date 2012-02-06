/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class DomUtils {

  /**
   * Return the value of a child node.
   * @param parent - the parent node.
   * @param childName - the child node name.
   * @return the child node value or null if it doesn't exist.
   */
  public static final String getChildValue(Xpp3Dom parent, String childName) {
    String result = null;
    if (parent != null && childName != null) {
      Xpp3Dom dom = parent.getChild(childName);
      if (dom != null && dom.getValue() != null) {//MNGECLIPSE-2328 add null-safety in case of <somenode/> 
        result = dom.getValue().trim();
      }
    }
    return result;
  }
  
  public static final String getChildValue(Xpp3Dom parent, String childName, String defaultValue) {
    String result = getChildValue(parent, childName);
    return StringUtils.defaultString(result, defaultValue);
  }

  public static final boolean getBooleanChildValue(Xpp3Dom parent, String childName) {
    return Boolean.valueOf(getChildValue(parent, childName));
  }
  /**
   * @param node
   */
  public static final void removeChildren(Xpp3Dom node) {
    if (node == null) return;
    for (int i= node.getChildCount()-1 ; i>-1 ; i--){
      node.removeChild(i);
    }
  }
  
  public static String[] getChildrenAsStringArray(Xpp3Dom root, String childName) {
    String[] values = null;
    if (root != null) {
      Xpp3Dom[] children = root.getChildren(childName); 
      if (children != null) {
        values = new String[children.length];
        int i = 0;
        for (Xpp3Dom child : children) {
          values[i++] = child.getValue();
        }
      }
    }
    return values;
  }  
  
  public static String[] getPatternsAsArray(Xpp3Dom config, String patternParameterName) {
    if(config != null) {
        Xpp3Dom excl = config.getChild(patternParameterName);
        if(excl != null) {
          return org.maven.ide.eclipse.wtp.internal.StringUtils.tokenizeToStringArray(excl.getValue(), ",");
        }
    }
    return new String[0];
  }  
}
