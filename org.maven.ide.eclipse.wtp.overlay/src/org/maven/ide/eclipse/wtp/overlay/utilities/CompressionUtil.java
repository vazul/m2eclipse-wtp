/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.maven.ide.eclipse.wtp.overlay.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.util.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Compression utility class.
 * 
 * Most of the code is copied from <i>org.eclipse.gef.examples.ui.pde.internal.wizards.ProjectUnzipperNewWizard</i>
 *  
 * @author Fred Bricon
 *
 */
public class CompressionUtil {
	
	private final static int BUFFER = 1024*4;
	
	private CompressionUtil() {}

	/**
	 * Unzips the platform formatted zip file to specified folder
	 * 
	 * @param zipFile
	 *            The platform formatted zip file
	 * @param projectFolderFile
	 *            The folder where to unzip the archive
	 * @param monitor
	 *            Monitor to display progress and/or cancel operation
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws InterruptedException
	 */
	public static void unzip(File archive, File projectFolderFile,
			IProgressMonitor monitor) throws IOException,
			FileNotFoundException, InterruptedException {

		initialize(projectFolderFile);
		
		ZipFile zipFile = new ZipFile(archive);
		Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) zipFile.entries();

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		while (e.hasMoreElements()) {
			ZipEntry zipEntry = (ZipEntry) e.nextElement();
			File file = new File(projectFolderFile, zipEntry.getName());

			if (!zipEntry.isDirectory()) {

				File parentFile = file.getParentFile();
				if (null != parentFile && !parentFile.exists()) {
					parentFile.mkdirs();
				}

				InputStream is = null;
				OutputStream os = null;

				try {
					is = zipFile.getInputStream(zipEntry);
					os = new FileOutputStream(file);

					byte[] buffer = new byte[BUFFER];
					while (true) {
						int len = is.read(buffer);
						if (len < 0)
							break;
						os.write(buffer, 0, len);
					}
				} finally {
					FileUtils.close(is);
					FileUtils.close(os);
				}
			}

			monitor.worked(1);

			if (monitor.isCanceled()) {
				throw new InterruptedException(" unzipping " +archive.getAbsolutePath() + " to "+ projectFolderFile.getAbsolutePath() +" was interrupted");
			}
		}
	}

	private static void initialize(File outputDirectory) throws IOException {
      // Create output directory if needed
      if (!outputDirectory.mkdirs() && !outputDirectory.exists())
      {
         throw new IOException("Unable to create archive output directory - " + outputDirectory);
      }
      if (outputDirectory.isFile())
      {
         throw new IllegalArgumentException("Unable to unpack to "
               + outputDirectory.getAbsolutePath() + ", it points to an existing file");
      }
	}
	
}
