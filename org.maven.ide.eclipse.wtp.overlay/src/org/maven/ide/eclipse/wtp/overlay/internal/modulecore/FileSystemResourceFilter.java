package org.maven.ide.eclipse.wtp.overlay.internal.modulecore;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.DirectoryScanner;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.maven.ide.eclipse.wtp.overlay.utilities.PathUtil;

public class FileSystemResourceFilter implements IResourceFilter {

	private SimpleScanner scanner;

	public FileSystemResourceFilter(Collection<String> inclusions, Collection<String> exclusions, IPath baseDirPath) {
		scanner = new SimpleScanner(baseDirPath);
		if (inclusions != null && !inclusions.isEmpty()) {
			scanner.setIncludes(inclusions.toArray(new String[inclusions.size()]));
		} else {
			scanner.setIncludes(new String[]{"**/**"});
		}
		if (exclusions != null && !exclusions.isEmpty()) {
			scanner.addExcludes(exclusions.toArray(new String[exclusions.size()]));
		}
		scanner.addDefaultExcludes();
		scanner.scan();
	}

	public boolean accepts(String resourcePath, boolean isFile) {
		return scanner.accepts(resourcePath, isFile);
	}
	
	class SimpleScanner extends DirectoryScanner {
		
		private IPath baseDirPath;
		private Set<String> includedFiles;
		private Set<String> excludedFiles;
		private Set<String> includedFolders;
		private Set<String> excludedFolders;
		
		public SimpleScanner(IPath baseDirPath) {
			this.baseDirPath = baseDirPath;
			setBasedir(baseDirPath.toPortableString());
		}

		@Override
		public void scan() throws IllegalStateException {
			super.scan();
			//cache the included and excluded files (to avoid several array copies)
			includedFiles = new HashSet<String>(Arrays.asList(getIncludedFiles()));
			excludedFiles = new HashSet<String>(Arrays.asList(getExcludedFiles()));
			includedFolders =  new HashSet<String>(Arrays.asList(getIncludedDirectories()));
			excludedFolders =  new HashSet<String>(Arrays.asList(getExcludedDirectories()));
			
			completeIncludedFolders();
			System.out.println(baseDirPath +" includes "+includedFiles.size() +" files");
		}
		
		private void completeIncludedFolders() {
			
	    	for(String file : includedFiles) {
	    		//For /some/foo/bar/file.ext, we need to add 
	    		// /some/foo/bar/
	    		// /some/foo/
	    		// /some/
	    		// as included folders
	    		
	    		IPath filePath = new Path(file);
	    		IPath parentPath = filePath.removeLastSegments(1);
	    		while (parentPath.segmentCount()>0) {
	    			if (includedFolders.add(parentPath.toPortableString())) {
	    				parentPath = parentPath.removeLastSegments(1);
	    			} else {
	    				//Parent hierarchy already added
	    				break;
	    			}
	    		}
	    	}
		}

		protected boolean accepts(String name, boolean isFile) {
			
			if (name.startsWith(baseDirPath.toPortableString())) {
				name = name.substring(baseDirPath.toPortableString().length()+1);
			}
			
			boolean res;
			name = PathUtil.useSystemSeparator(name);
			if (isFile) {
				res = includedFiles.contains(name) && !excludedFiles.contains(name);
			} else {
				res = includedFolders.contains(name) && !excludedFolders.contains(name);
			}
			//if (!res) System.err.println(name + (res?" included": " excluded"));
			return res;
		}
	}
}
