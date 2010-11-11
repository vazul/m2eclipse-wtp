/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.overlay.modulecore;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;


/**
 * Overlay Component Core
 *
 * @author Fred Bricon
 */
public class OverlayComponentCore {

  public static IVirtualComponent createOverlayComponent(IProject aProject) {
    return new OverlayVirtualComponent(aProject);
  }
}
