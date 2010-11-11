package org.maven.ide.eclipse.wtp.overlay.modulecore;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.wst.common.componentcore.internal.flat.FlatVirtualComponent;
import org.eclipse.wst.common.componentcore.internal.flat.IFlatFile;
import org.eclipse.wst.common.componentcore.internal.flat.IFlatFolder;
import org.eclipse.wst.common.componentcore.internal.flat.IFlatResource;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualContainer;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;

/**
 * Virtual folder mapping a FlatVirtualComponent
 * 
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class CompositeVirtualFolder implements IVirtualFolder {

	private FlatVirtualComponent flatVirtualComponent;
	
	public CompositeVirtualFolder(FlatVirtualComponent flatVirtualComponent, IPath aRuntimePath) {
		this.flatVirtualComponent = flatVirtualComponent;
	}

	public IProject getProject() {
		if (flatVirtualComponent != null && flatVirtualComponent.getComponent() != null) {
			return flatVirtualComponent.getComponent().getProject();
		}
		return null;
	}

	public IVirtualResource[] members() throws CoreException {	 
		IFlatResource[] flatResources = flatVirtualComponent.fetchResources();
		IVirtualResource[] members = new IVirtualResource[flatResources.length];
		int i = 0;
		for (IFlatResource flatResource : flatResources) {
			members[i] = convert(flatResource);
			i++;
		}
		return members;
	}

	private IVirtualResource convert(IFlatResource flatResource) {
		// TODO How do I convert an IFlatResource into a IVirtualResource ??
		IVirtualResource virtualResource = null;
		if (flatResource instanceof IFlatFolder) {
			//TODO extract convert method to a utility class?
			virtualResource = convertFolder((IFlatFolder) flatResource);
		} else if (flatResource instanceof IFlatFile){
			virtualResource = convertFile((IFlatFile) flatResource);
		}
		return virtualResource;
	}

	private IVirtualFolder convertFolder(IFlatFolder flatFolder) {
		//Do we need to go recursive? Please no!!! for (IFlatResource flatResource  : flatFolder.members()) { convert(...)}; 
		return null;
	}

	private IVirtualFile convertFile(IFlatFile flatFile) {
		//Do we need to create a new implementation of IVirtualFile?
		IPath path = flatFile.getModuleRelativePath();
		return null;
	}
	
	public void create(int arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	public boolean exists(IPath arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public IVirtualResource findMember(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtualResource findMember(IPath arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtualResource findMember(String arg0, int arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtualResource findMember(IPath arg0, int arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtualFile getFile(IPath arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtualFile getFile(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtualFolder getFolder(IPath arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtualFolder getFolder(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtualResource[] getResources(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtualResource[] members(int arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public void createLink(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

	public void delete(int arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	public IVirtualComponent getComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getFileExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtualContainer getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	public IPath getProjectRelativePath() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getResourceType() {
		// TODO Auto-generated method stub
		return null;
	}

	public IPath getRuntimePath() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	public IResource getUnderlyingResource() {
		// TODO Auto-generated method stub
		return null;
	}

	public IResource[] getUnderlyingResources() {
		// TODO Auto-generated method stub
		return null;
	}

	public IPath getWorkspaceRelativePath() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAccessible() {
		// TODO Auto-generated method stub
		return false;
	}

	public void removeLink(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

	public void setResourceType(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public boolean contains(ISchedulingRule rule) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isConflicting(ISchedulingRule rule) {
		// TODO Auto-generated method stub
		return false;
	}

	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	public IContainer getUnderlyingFolder() {
		// TODO Auto-generated method stub
		return null;
	}

	public IContainer[] getUnderlyingFolders() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
