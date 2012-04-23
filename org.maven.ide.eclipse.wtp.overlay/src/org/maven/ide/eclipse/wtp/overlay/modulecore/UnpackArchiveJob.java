/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.maven.ide.eclipse.wtp.overlay.modulecore;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
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
		setRule(unpackFolder);
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
		try {
			if (unpackFolder.exists()) {
	      System.out.println(getName() +" deleting "+unpackFolder);
	      		//delete members as deleting unpackFolder will use scheduling rule of its parent, so an IllegalArgumentException would be thrown otherwise
				final IResource[] members = unpackFolder.members(IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
				for (final IResource member : members)
				{
					member.delete(true, monitor);
				}
			}
			unpack(archive, unpackFolder.getLocation().toOSString(), monitor);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, OverlayPluginActivator.PLUGIN_ID, "Error unpacking "+archive.getName(), e);
		} catch (InterruptedException e) {
			return new Status(IStatus.ERROR, OverlayPluginActivator.PLUGIN_ID, "Unpacking "+archive.getName() + " was interrupted", e);
		}
		
		//will run in scheduling rule of parent of unpackfolder, so should be run in a different job
		new WorkspaceJob(unpackFolder.getLocation().toString() + " refresher") {

			@Override
			public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException
			{
				unpackFolder.refreshLocal(IFolder.DEPTH_INFINITE, null);
				return Status.OK_STATUS;
			}
		}.schedule();

		return Status.OK_STATUS;
	}

	protected void unpack(File archive, String unpackFolderPath, IProgressMonitor monitor) throws IOException, CoreException,
			InterruptedException {
    System.out.println(getName() +" unpacking "+archive + " to " +unpackFolderPath);
		File unpackFolder = new File(unpackFolderPath);
		CompressionUtil.unzip(archive, unpackFolder, monitor);
		unpackFolder.setLastModified(archive.lastModified());
	}
}
