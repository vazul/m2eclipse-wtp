package org.maven.ide.eclipse.wtp.overlay.utilities;

import java.io.File;

public class PathUtil {

	public static String useSystemSeparator(String name) {
		if (name == null) return null;
		return name.replace('/', File.separatorChar)
	            .replace('\\', File.separatorChar);
	}
}
