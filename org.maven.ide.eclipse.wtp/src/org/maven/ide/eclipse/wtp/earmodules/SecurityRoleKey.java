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


/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.SecurityRole
 * 
 * SecurityRoleKey
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @author Fred Bricon
 */
public class SecurityRoleKey {
  private String id;
  
  private String roleName;
  
  private String description;
  
  /**
   * @return Returns the id.
   */
  public String getId() {
    return id;
  }

  /**
   * @param id The id to set.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return Returns the roleName.
   */
  public String getRoleName() {
    return roleName;
  }

  /**
   * @param roleName The roleName to set.
   */
  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  /**
   * @return Returns the description.
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description The description to set.
   */
  public void setDescription(String description) {
    this.description = description;
  }
  
}
