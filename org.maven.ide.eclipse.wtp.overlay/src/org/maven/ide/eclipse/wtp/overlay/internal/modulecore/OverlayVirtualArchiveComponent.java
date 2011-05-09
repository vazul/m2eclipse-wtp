/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
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
import org.eclipse.core.runtime.NullProgressMonitor;
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
@SuppressWarnings("restriction")
public class OverlayVirtualArchiveComponent extends VirtualArchiveComponent implements IOverlayVirtualComponent {

	protected IPath unpackDirPath;
	
	private Set<String> inclusions;
	
	private Set<String> exclusions;
	
	public OverlayVirtualArchiveComponent(IProject aComponentProject,
			String archiveLocation, IPath unpackDirPath, IPath aRuntimePath) {
		super(aComponentProject, archiveLocation, aRuntimePath);
		this.unpackDirPath = unpackDirPath;
	}

	public void setInclusions(Set<String> inclusionPatterns) {
		this.inclusions = inclusionPatterns;
	}

	public void setExclusions(Set<String> exclusionPatterns) {
		this.exclusions = exclusionPatterns;
	}
	
	public IVirtualFolder getRootFolder() {
		IVirtualComponent component = ComponentCore.createComponent(getProject());
		ResourceListVirtualFolder root =null;
		if (component != null) {
			IFolder overlaysFolder =  getProject().getFolder(unpackDirPath);
			try {
				unpackIfNeeded(new NullProgressMonitor());
				
				IFolder unpackedFolder = overlaysFolder.getFolder(getArchive().getName());
				IContainer[] containers = new IContainer[] {unpackedFolder};
				root = new ResourceListVirtualFolder(getProject(), getRuntimePath(), containers);
				root.setFilter(new DefaultResourceFilter(inclusions, exclusions, unpackedFolder.getLocation()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CoreException e) {
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
				IFolder unpackArchiveFolder  =  unpackFolder.getFolder(archive.getName());
				if (unpackArchiveFolder.exists()) {
					unpackArchiveFolder.delete(true, monitor);
				}
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
		IFolder unpackArchiveFolder = unpackFolder.getFolder(archive.getName());
		if (!unpackArchiveFolder.exists()) {
			return true;
		}
		long lastUnpacked = new File(unpackArchiveFolder.getLocation().toOSString()).lastModified();
		long lastModified = archive.lastModified();
		return lastModified > lastUnpacked;
	}

	private void unpack(File archive, IFolder unpackFolder,
			IProgressMonitor monitor) throws IOException, CoreException, InterruptedException {
		IFolder unpackLocation = unpackFolder.getFolder(archive.getName()); 
		createFolder(unpackLocation, monitor);
		CompressionUtil.unzip(archive, new File(unpackLocation.getLocation().toOSString()), monitor);
		File unpackLocationFolder = new File(unpackLocation.getLocation().toOSString());
		unpackLocationFolder.setLastModified(archive.lastModified());
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

	public IPath getUnpackFolderPath() {
		return unpackDirPath;
	}

	public Set<String> getExclusions() {
		return exclusions;
	}

	public Set<String> getInclusions() {
		return inclusions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((exclusions == null) ? 0 : exclusions.hashCode());
		result = prime * result
				+ ((inclusions == null) ? 0 : inclusions.hashCode());
		result = prime * result
				+ ((unpackDirPath == null) ? 0 : unpackDirPath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		OverlayVirtualArchiveComponent other = (OverlayVirtualArchiveComponent) obj;
		if (!super.equals(obj)) {
			return false;
		}
		if (exclusions == null) {
			if (other.exclusions != null)
				return false;
		} else if (!exclusions.equals(other.exclusions))
			return false;
		if (inclusions == null) {
			if (other.inclusions != null)
				return false;
		} else if (!inclusions.equals(other.inclusions))
			return false;
		if (unpackDirPath == null) {
			if (other.unpackDirPath != null)
				return false;
		} else if (!unpackDirPath.equals(other.unpackDirPath))
			return false;
		return true;
	}
	
}
