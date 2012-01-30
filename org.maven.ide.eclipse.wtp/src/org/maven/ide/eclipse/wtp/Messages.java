/*************************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.maven.ide.eclipse.wtp;

import org.eclipse.osgi.util.NLS;

/**
 * 
 * @author Fred Bricon
 *
 */
public class Messages extends NLS {
	
  public static String markers_unsupported_dependencies_warning;
	
  static {
		// initialize resource bundle
		NLS.initializeMessages(MavenWtpPlugin.ID, Messages.class);
	}

	private Messages() {
	}
}
