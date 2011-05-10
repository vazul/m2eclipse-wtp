package org.maven.ide.eclipse.wtp.overlay.modulecore;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.wtp.overlay.OverlayPluginActivator;
import org.maven.ide.eclipse.wtp.overlay.utilities.CompressionUtil;

public class UnpackArchiveJob extends WorkspaceJob {

	private IFolder unpackFolder;
	private File archive;

	public UnpackArchiveJob(String name, File archive, IFolder unpackFolder) {
		super(name);
		assert unpackFolder != null;
		assert archive != null && archive.exists() && archive.canRead();
		this.unpackFolder = unpackFolder;
		this.archive = archive;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
		try {
			unpack(archive, unpackFolder.getLocation().toOSString(), monitor);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, OverlayPluginActivator.PLUGIN_ID, "Error unpacking "+archive.getName(), e);
		} catch (InterruptedException e) {
			return new Status(IStatus.ERROR, OverlayPluginActivator.PLUGIN_ID, "Unpacking "+archive.getName() + " was interrupted", e);
		}

		IContainer parent = unpackFolder.getParent();
		if (parent != null) {
			parent.refreshLocal(IFolder.DEPTH_INFINITE, monitor);
		}
		return Status.OK_STATUS;
	}

	protected void unpack(File archive, String unpackFolderPath, IProgressMonitor monitor) throws IOException, CoreException,
			InterruptedException {
		File unpackFolder = new File(unpackFolderPath);
		CompressionUtil.unzip(archive, unpackFolder, monitor);
		File unpackLocation = new File(unpackFolder, archive.getName());
		unpackLocation.setLastModified(archive.lastModified());
	}
}
