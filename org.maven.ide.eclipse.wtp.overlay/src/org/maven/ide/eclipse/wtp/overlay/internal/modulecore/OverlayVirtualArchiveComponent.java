/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.overlay.internal.modulecore;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualArchiveComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.maven.ide.eclipse.wtp.overlay.modulecore.IOverlayVirtualComponent;
import org.maven.ide.eclipse.wtp.overlay.utilities.CompressionUtil;

/**
 * Overlay Virtual Archive Component
 *
 * @author Fred Bricon
 */
public class OverlayVirtualArchiveComponent extends VirtualArchiveComponent implements IOverlayVirtualComponent {

	protected IPath unpackDirPath;
	
	public OverlayVirtualArchiveComponent(IProject aComponentProject,
			String archiveLocation, IPath unpackDirPath, IPath aRuntimePath) {
		super(aComponentProject, archiveLocation, aRuntimePath);
		this.unpackDirPath = unpackDirPath;
	}

	public void setInclusions(Set<String> inclusionPatterns) {
		// TODO Auto-generated method stub
		
	}

	public void setExclusions(Set<String> inclusionPatterns) {
		// TODO Auto-generated method stub
		
	}
	
	public IVirtualFolder getRootFolder() {
		IVirtualComponent component = ComponentCore.createComponent(getProject());
		IVirtualFolder root =null;
		if (component != null) {
			IFolder overlaysFolder =  getProject().getFolder(unpackDirPath);
			try {
				IContainer[] containers = new IContainer[] {overlaysFolder.getFolder(getArchive().getName())};
				root = new ResourceListVirtualFolder(getProject(), getRuntimePath(), containers);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return root;
	}

	public void unpackIfNeeded(IProgressMonitor monitor) throws CoreException {
		File archive;
		try {
			archive = getArchive();
			IFolder unpackFolder =  getProject().getFolder(unpackDirPath);
			if (needsUnpacking(archive, unpackFolder)) {
				unpack(archive, unpackFolder, monitor);
				refreshProject();
			}
		} catch (IOException e) {
			CoreException cex = new CoreException(new Status(IStatus.ERROR, "Overlay", -1, "Unable to unpack the component", e));
			throw cex;
		} catch (InterruptedException e) {
			CoreException cex = new CoreException(new Status(IStatus.ERROR, "Overlay", -1, "Unpacking was interrupted", e));
			throw cex;
		}

	}

	private File getArchive() throws IOException {
		File archive = (File)getAdapter(File.class);
		if (archive == null || !archive.exists() || !archive.canRead()) {
			throw new IOException("Unable to read "+ getArchivePath());
		}
		return archive;
	}
	
	private void refreshProject() throws CoreException {
		IFolder unpackFolder = getProject().getFolder(unpackDirPath);
		if (unpackFolder.exists()) {
			unpackFolder.refreshLocal(IFolder.DEPTH_INFINITE, null);
		}
	}

	private boolean needsUnpacking(File archive, IFolder unpackFolder) {
		return !unpackFolder.getFolder(archive.getName()).exists() 
			|| isObsolete(archive, unpackFolder);
	}

	private boolean isObsolete(File archive, IFolder unpackFolder) {
		IFile flag = getFlagFile(archive, unpackFolder);
		if (!flag.exists()) {
			return true;
		}
		long lastUnpacked = new File(flag.getLocation().toOSString()).lastModified();
		long lastModified = archive.lastModified();
		return lastModified > lastUnpacked;
	}

	private IFile getFlagFile(File archive, IFolder unpackFolder) {
		return unpackFolder.getFile(archive.getName()+".lastUpdated");
	}

	private void unpack(File archive, IFolder unpackFolder,
			IProgressMonitor monitor) throws IOException, CoreException, InterruptedException {
		IFolder unpackLocation = unpackFolder.getFolder(archive.getName()); 
		createFolder(unpackLocation, monitor);
		CompressionUtil.unzip(archive, new File(unpackLocation.getLocation().toOSString()), monitor);
		IFile flag = getFlagFile(archive, unpackFolder);
		if (!flag.exists()) {
			File underlyingFlagFile = new File(flag.getLocation().toOSString());
			underlyingFlagFile.createNewFile();
			flag.refreshLocal(0, monitor);
			flag.setHidden(true);
		}
		flag.touch(monitor);
	}
	
	private static void createFolder(IFolder folder, IProgressMonitor monitor) throws CoreException {
	  if (folder == null || folder.exists()) {
	      return;
	  }
	  IContainer parent = folder.getParent();
	  if (parent instanceof IFolder) {
	    createFolder((IFolder)parent, monitor);
	  }
	  folder.create(true, true, monitor);
	}
}
