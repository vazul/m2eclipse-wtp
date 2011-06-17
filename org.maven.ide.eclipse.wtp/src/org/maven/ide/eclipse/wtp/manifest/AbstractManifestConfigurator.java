/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.internal.common.classpath.J2EEComponentClasspathUpdater;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.maven.ide.eclipse.wtp.namemapping.FileNameMappingFactory;


/**
 * AbstractManifestConfigurator
 * 
 * @author Fred Bricon
 */
public abstract class AbstractManifestConfigurator extends AbstractProjectConfigurator implements IManifestConfigurator {

  private static final String MANIFEST_ENTRIES_NODE = "manifestEntries";

  private static final String ARCHIVE_NODE = "archive";

  private static final String CREATED_BY_ENTRY = "Created-By";

  private static final String MAVEN_ARCHIVER_CLASS = "org.apache.maven.archiver.MavenArchiver";

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    //Nothing to configure
  }

  protected abstract MojoExecutionKey getExecutionKey();

  public AbstractBuildParticipant getBuildParticipant(final IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {

    MojoExecutionKey key = getExecutionKey();
    if(execution.getArtifactId().equals(key.getArtifactId()) && execution.getGoal().equals(key.getGoal())) {

      return new AbstractBuildParticipant() {
        public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
          IResourceDelta delta = getDelta(projectFacade.getProject());
          
          boolean force = false;
          if (delta != null) {
            ManifestDeltaVisitor visitor = new ManifestDeltaVisitor();
            delta.accept(visitor);
            force = visitor.foundManifest;
          }
          //The manifest will be (re)generated if it doesn't exist or an existing manifest is modified
          mavenProjectChanged(projectFacade, null, force, monitor);
          return null;
        }
      };
    }
    return null;
  }

  private class ManifestDeltaVisitor implements IResourceDeltaVisitor {

    private final String MANIFEST = "MANIFEST.MF";// TODO Do not assume user provided manifests are named like that, 
    // read info from maven plugin instead. 

    boolean foundManifest;

    public boolean visit(IResourceDelta delta) throws CoreException {
      if (delta.getResource() instanceof IFile 
          && MANIFEST.equals(delta.getResource().getName())) {
        foundManifest = true;
      }
      return !foundManifest;
    }
  }
  
  /**
   * Generates the project manifest if necessary, that is if the project manifest configuration has changed or if the
   * dependencies have changed.
   */
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {

    IMavenProjectFacade oldFacade = event.getOldMavenProject();
    IMavenProjectFacade newFacade = event.getMavenProject();
    if(oldFacade == null && newFacade == null) {
      return;
    }
    mavenProjectChanged(newFacade, oldFacade, false, monitor);
  }

 
  public void mavenProjectChanged(IMavenProjectFacade newFacade, IMavenProjectFacade oldFacade, boolean forceGeneration, IProgressMonitor monitor)
      throws CoreException {

    IProject project = newFacade.getProject();
    if (!ModuleCoreNature.isFlexibleProject(project)) {
      return;
    }
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFolder output = root.getFolder(getManifestdir(newFacade).append("META-INF"));
    IFile manifest = output.getFile("MANIFEST.MF");

    //System.err.println("checking for manifest "+project);
    if(forceGeneration || needsNewManifest(manifest, oldFacade, newFacade, monitor)) {
      generateManifest(newFacade, manifest, monitor);
    }

  }

  protected abstract IPath getManifestdir(IMavenProjectFacade facade);

  /**
   * @param manifest
   * @param oldFacade
   * @param newFacade
   * @param monitor
   * @return
   */
  private boolean needsNewManifest(IFile manifest, IMavenProjectFacade oldFacade, IMavenProjectFacade newFacade,
      IProgressMonitor monitor) {

    if(!manifest.exists()) {
      return true;
    }
    //Can't compare to a previous state, so assuming it's unchanged
    //This situation actuzally occurs during incremental builds, 
    //when called from the buildParticipant
    if(oldFacade == null || oldFacade.getMavenProject() == null) {
      return false;
    }

    MavenProject newProject = newFacade.getMavenProject();
    MavenProject oldProject = oldFacade.getMavenProject();

    //Assume Sets of artifacts are actually ordered
    if(dependenciesChanged(oldProject.getArtifacts() == null ? null 
                                                             : new ArrayList<Artifact>(oldProject.getArtifacts()), 
                           newProject.getArtifacts() == null ? null
                                                             : new ArrayList<Artifact>(newProject.getArtifacts()))) {
      return true;
    }

    Xpp3Dom oldArchiveConfig = getArchiveConfiguration(oldProject);
    Xpp3Dom newArchiveConfig = getArchiveConfiguration(newProject);

    if(newArchiveConfig != null && !newArchiveConfig.equals(oldArchiveConfig) || oldArchiveConfig != null
        && newArchiveConfig == null) {
      return true;
    }

    //Name always not null
    if(!newProject.getName().equals(oldProject.getName())) {
      return true;
    }

    String oldOrganizationName = oldProject.getOrganization() == null ? null : oldProject.getOrganization().getName();
    String newOrganizationName = newProject.getOrganization() == null ? null : newProject.getOrganization().getName();

    if(newOrganizationName != null && !newOrganizationName.equals(oldOrganizationName) || oldOrganizationName != null
        && newOrganizationName == null) {
      return true;
    }
    return false;
  }

  /**
   * @param artifacts
   * @param others
   * @return
   */
  private boolean dependenciesChanged(List<Artifact> artifacts, List<Artifact> others) {
    if(artifacts==others) {
      return false;
    }
    if(artifacts.size() != others.size()) {
      return true;
    }
    for(int i = 0; i < artifacts.size(); i++ ) {
      Artifact dep = artifacts.get(i);
      Artifact dep2 = others.get(i);
      if(!areEqual(dep, dep2)) {
        return true;
      }

    }
    return false;
  }

  @SuppressWarnings("null")
  private boolean areEqual(Artifact dep, Artifact other) {
    if(dep == other) {
      return true;
    }
    if(dep == null && other != null || dep != null && other == null) {
      return false;
    }
    //So both artifacts are not null here.
    //Fast (to type) and easy way to compare artifacts. 
    //Proper solution would not rely on internal implementation of toString
    if(dep.toString().equals(other.toString()) && dep.isOptional() == other.isOptional()) {
      return true;
    }
    return false;
  }

  protected Xpp3Dom getArchiveConfiguration(MavenProject mavenProject) {
    Plugin plugin = mavenProject.getPlugin(getPluginKey());
    if(plugin == null)
      return null;

    Xpp3Dom pluginConfig = (Xpp3Dom) plugin.getConfiguration();
    if(pluginConfig == null) {
      return null;
    }
    return pluginConfig.getChild(ARCHIVE_NODE);
  }

  public void generateManifest(IMavenProjectFacade mavenFacade, IFile manifest, IProgressMonitor monitor)
      throws CoreException {

    //Find the mojoExecution
    MavenSession session = getMavenSession(mavenFacade, monitor);
    MavenProject mavenProject = mavenFacade.getMavenProject();
    MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(session, mavenProject,
        Collections.singletonList("package"), true, monitor);
    MojoExecution mojoExecution = getExecution(executionPlan, getExecutionKey());
    if(mojoExecution == null) {
      return;
    }

    //Get the target manifest file
    IFolder destinationFolder = (IFolder) manifest.getParent();
    File manifestDir = new File(destinationFolder.getRawLocation().toOSString());
    if(!manifestDir.exists()) {
      manifestDir.mkdirs();
    }
    
    Set<Artifact> originalArtifacts = mavenProject.getArtifacts();

    try {
      //Fix the project artifacts before calling the manifest generation
      mavenProject.setArtifacts(fixArtifactFileNames(mavenFacade));

      //Invoke the manifest generation API via reflection
      reflectManifestGeneration(mavenProject, mojoExecution, session, new File(manifest.getLocation().toOSString()));
      J2EEComponentClasspathUpdater.getInstance().queueUpdate(mavenFacade.getProject());
      //refresh the target folder
      destinationFolder.refreshLocal(IResource.DEPTH_ONE, null);
    } catch(Throwable ex) {
      //TODO add marker
      ex.printStackTrace();
    } finally {
      mavenProject.setArtifacts(originalArtifacts);
    }

  }
  
  protected MavenSession getMavenSession(IMavenProjectFacade mavenFacade, IProgressMonitor monitor)
      throws CoreException {
    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    IMaven maven = MavenPlugin.getMaven();
    //Create a maven request + session
    IFile pomResource = mavenFacade.getPom();
    MavenExecutionRequest request = projectManager.createExecutionRequest(pomResource,
        mavenFacade.getResolverConfiguration(), monitor);
    request.setOffline(MavenPlugin.getMavenConfiguration().isOffline());
    return maven.createSession(request, mavenFacade.getMavenProject());
  }

  private void reflectManifestGeneration(MavenProject mavenProject, MojoExecution mojoExecution, MavenSession session,
      File manifestFile) throws Exception {

    ClassLoader loader = null;
    Class<? extends Mojo> mojoClass;
    Mojo mojo = null;

    Xpp3Dom originalConfig = mojoExecution.getConfiguration();
    Xpp3Dom customConfig = Xpp3DomUtils.mergeXpp3Dom(new Xpp3Dom("configuration"), originalConfig);

    customizeManifest(customConfig, mavenProject);

    mojoExecution.setConfiguration(customConfig);

    mojo = maven.getConfiguredMojo(session, mojoExecution, Mojo.class);
    mojoClass = mojo.getClass();
    loader = mojoClass.getClassLoader();
    PrintWriter printWriter = null;

    try {
      Field archiverField = findField(getArchiverFieldName(), mojoClass);
      archiverField.setAccessible(true);
      Object archiver = archiverField.get(mojo);

      Field archiveConfigurationField = findField(getArchiveConfigurationFieldName(), mojoClass);
      archiveConfigurationField.setAccessible(true);
      Object archiveConfiguration = archiveConfigurationField.get(mojo);
      Object mavenArchiver = getMavenArchiver(archiver, manifestFile, loader);

      //Workspace project artifacts don't have a valid getFile(), so won't appear in the manifest
      //So we need to workaround the issue by creating  fake files for such artifacts
      Method getManifest = mavenArchiver.getClass().getMethod("getManifest", MavenProject.class,
          archiveConfiguration.getClass());

      //Create the Manifest instance
      Object manifest = getManifest.invoke(mavenArchiver, mavenProject, archiveConfiguration);

      //Get the user provided manifest, if it exists
      Object userManifest = getProvidedManifest(manifest.getClass(), archiveConfiguration);

      //Merge both manifests, the user provided manifest data takes precedence
      mergeManifests(manifest, userManifest);
      
      //Serialize the Manifest instance to an actual file
      Method write = manifest.getClass().getMethod("write", PrintWriter.class);
      printWriter = new PrintWriter(WriterFactory.newWriter(manifestFile, WriterFactory.UTF_8));
      write.invoke(manifest, printWriter);
      //System.err.println("wrote "+manifestFile);
    } finally {
      if(printWriter != null) {
        printWriter.close();
      }
      
      mojoExecution.setConfiguration(originalConfig);

      maven.releaseMojo(mojo, mojoExecution);
    }
  }

  /**
   * @param archiveConfiguration
   * @return
   * @throws NoSuchMethodException 
   * @throws SecurityException 
   * @throws InvocationTargetException 
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   * @throws IllegalArgumentException 
   */
  private Object getProvidedManifest(Class manifestClass, Object archiveConfiguration) 
      throws SecurityException, IllegalArgumentException, 
            InstantiationException, IllegalAccessException, InvocationTargetException {

    Object newManifest = null;
    Reader reader = null;
    try {
      Method getManifestFile = archiveConfiguration.getClass().getMethod("getManifestFile");
      File manifestFile = (File) getManifestFile.invoke(archiveConfiguration);
      if (manifestFile == null || !manifestFile.exists() || !manifestFile.canRead()) {
        return null;
      }

      reader = new FileReader(manifestFile);
      Constructor constructor = manifestClass.getConstructor(Reader.class);
      newManifest = constructor.newInstance(reader);
    } catch(FileNotFoundException ex) {
      //ignore
    } catch(NoSuchMethodException ex) {
      //ignore, this is not supported by this archiver version
    } finally {
      IOUtil.close(reader);
    }
    return newManifest;
  }

  private void mergeManifests(Object manifest, Object sourceManifest) 
      throws SecurityException, NoSuchMethodException, IllegalArgumentException, 
      IllegalAccessException, InvocationTargetException {
    if (sourceManifest == null) return;
    
    Method merge = manifest.getClass().getMethod("merge", sourceManifest.getClass());
    merge.invoke(manifest, sourceManifest);
  }

  protected abstract String getArchiverFieldName();

  /**
   * @param artifacts
   * @param facade
   * @throws IOException
   */
  private Set<Artifact> fixArtifactFileNames(IMavenProjectFacade facade) throws IOException {
    Set<Artifact> artifacts = facade.getMavenProject().getArtifacts();
    if(artifacts == null)
      return null;
    Set<Artifact> newArtifacts = new LinkedHashSet<Artifact>(artifacts.size());

    for(Artifact a : artifacts) {
      Artifact artifact;
      if(a.getFile().isDirectory()) {
        //Workaround Driven Development : Create a dummy file associated with an Artifact, 
        // so this artifact won't be ignored during the resolution of the Class-Path entry in the Manifest
        artifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getScope(), a.getType(),
            a.getClassifier(), a.getArtifactHandler());
        artifact.setFile(fakeFile(a));
      } else {
        artifact = a;
      }

      newArtifacts.add(artifact);
    }
    return newArtifacts;
  }

  /**
   * @param customConfig
   * @throws DependencyResolutionRequiredException
   * @throws CoreException
   */
  private void customizeManifest(Xpp3Dom customConfig, MavenProject mavenProject) throws CoreException {
    if(customConfig == null)
      return;
    Xpp3Dom archiveNode = customConfig.getChild(ARCHIVE_NODE);
    if(archiveNode == null) {
      archiveNode = new Xpp3Dom(ARCHIVE_NODE);
      customConfig.addChild(archiveNode);
    }

    Xpp3Dom manifestEntriesNode = archiveNode.getChild(MANIFEST_ENTRIES_NODE);
    if(manifestEntriesNode == null) {
      manifestEntriesNode = new Xpp3Dom(MANIFEST_ENTRIES_NODE);
      archiveNode.addChild(manifestEntriesNode);
    }

    Xpp3Dom createdByNode = manifestEntriesNode.getChild(CREATED_BY_ENTRY);
    //Create a default "Created-By: Maven Integration for Eclipse", because it's cool
    if(createdByNode == null) {
      createdByNode = new Xpp3Dom(CREATED_BY_ENTRY);
      createdByNode.setValue("Maven Integration for Eclipse");
      manifestEntriesNode.addChild(createdByNode);
    }
  }

  private Field findField(String name, Class<?> clazz) {
    return ReflectionUtils.getFieldByNameIncludingSuperclasses(name, clazz);
  }

  private Object getMavenArchiver(Object archiver, File manifestFile, ClassLoader loader)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException,
      NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
    Class<Object> mavenArchiverClass = (Class<Object>) Class.forName(MAVEN_ARCHIVER_CLASS, false, loader);
    Object mavenArchiver = mavenArchiverClass.newInstance();

    Method setArchiver = null;
    //TODO do a proper lookup
    for(Method m : mavenArchiver.getClass().getMethods()) {
      if("setArchiver".equals(m.getName())) {
        setArchiver = m;
        break;
      }
    }

    //Method setArchiver = mavenArchiverClass.getMethod("setArchiver", archiver.getClass());
    setArchiver.invoke(mavenArchiver, archiver);
    Method setOutputFile = mavenArchiverClass.getMethod("setOutputFile", File.class);
    setOutputFile.invoke(mavenArchiver, manifestFile);
    return mavenArchiver;
  }

  protected String getArchiveConfigurationFieldName() {
    return "archive";
  }

  protected String getPluginKey() {
    MojoExecutionKey execution = getExecutionKey();
    return execution.getGroupId() + ":" + execution.getArtifactId();
  }

  private MojoExecution getExecution(MavenExecutionPlan executionPlan, MojoExecutionKey key) {
    for(MojoExecution execution : executionPlan.getMojoExecutions()) {
      if(key.getArtifactId().equals(execution.getArtifactId()) && key.getGroupId().equals(execution.getGroupId())
          && key.getGoal().equals(execution.getGoal())) {
        return execution;
      }
    }
    return null;
  }

  private File fakeFile(Artifact artifact) throws IOException {
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    File tmpRepo = new File(tmpDir, artifact.getGroupId().replace(".", "/"));
    if(!tmpRepo.exists()) {
      tmpRepo.mkdirs();
    }
    String fileName = FileNameMappingFactory.getDefaultFileNameMapping().mapFileName(artifact);
    File fakeFile = new File(tmpRepo, fileName);
    if(!fakeFile.exists()) {
      fakeFile.createNewFile();
    }
    return fakeFile;
  }
}