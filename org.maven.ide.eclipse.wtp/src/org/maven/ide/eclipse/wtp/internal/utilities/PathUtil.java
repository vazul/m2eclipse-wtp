/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.maven.ide.eclipse.wtp.internal.utilities;

import java.io.File;

public class PathUtil {

	public static String toOsPath(String path) {
		if (path == null) return null;
		return path.replace('/', File.separatorChar)
	            .replace('\\', File.separatorChar);
	}
	
	public static String toPortablePath(String path) {
	  if (path == null) return null;
	  return path.replace('\\', '/');
	}
}
