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

import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class CompressionUtil {
	
	private final static int BUFFER = 1024*4;
	
	private CompressionUtil() {}

	/**
	 * Unzips the platform formatted zip file to specified folder
	 * 
	 * @param zipFile
	 *            The platform formatted zip file
	 * @param projectFolderFile
	 *            The folder where to unzip the project archive
	 * @param monitor
	 *            Monitor to display progress and/or cancel operation
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws InterruptedException
	 */
	public static void unzip(File archive, File projectFolderFile,
			IProgressMonitor monitor) throws IOException,
			FileNotFoundException, InterruptedException {

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
					FileUtil.safeClose(is);
					FileUtil.safeClose(os);
				}
			}

			monitor.worked(1);

			if (monitor.isCanceled()) {
				throw new InterruptedException();
			}
		}
	}

	private static File initialize(String destDir) throws IOException {
	      // Create output directory
	      final File outputDirectory = new File(destDir);
	      if (!outputDirectory.mkdir() && !outputDirectory.exists())
	      {
	         throw new IOException("Unable to create archive output directory - " + outputDirectory);
	      }
	      if (outputDirectory.isFile())
	      {
	         throw new IllegalArgumentException("Unable to unpack to "
	               + outputDirectory.getAbsolutePath() + ", it points to an existing file");
	      }
	      return outputDirectory;		
	}
	
}
