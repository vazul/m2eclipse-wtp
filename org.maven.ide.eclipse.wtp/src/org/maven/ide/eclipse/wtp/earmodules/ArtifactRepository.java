/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * The Apache License v2.0 is available at
 *   http://www.apache.org/licenses/LICENSE-2.0.html
 * You may elect to redistribute this code under either of these licenses.
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.earmodules;

import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;


/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.util.ArtifactRepository
 * 
 * An artifact repository used to resolve {@link org.maven.ide.eclipse.wtp.earmodules.EarModule}. This is a Java 5 port
 * of org.apache.maven.plugin.ear.ArtifactRepository from maven-ear-plugin
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class ArtifactRepository {
  private final Set<Artifact> artifacts;

  private final String mainArtifactId;

  private final ArtifactTypeMappingService artifactTypeMappingService;

  /**
   * Creates a new repository wih the specified artifacts.
   * 
   * @param artifacts the artifacts
   * @param mainArtifactId the id to use for the main artifact (no classifier)
   */
  public ArtifactRepository(Set<Artifact> artifacts, String mainArtifactId,
      ArtifactTypeMappingService artifactTypeMappingService) {
    this.artifacts = artifacts;
    this.mainArtifactId = mainArtifactId;
    this.artifactTypeMappingService = artifactTypeMappingService;
  }

  /**
   * Returns the artifact with the specified parameters. <p/> If the artifact is classified and is the only one with the
   * specified groupI, artifactId and type, it will be returned. <p/> If the artifact is classified and is not the only
   * one with the specified groupI, artifactId and type, it returns null. <p/> If the artifact is not found, it returns
   * null.
   * 
   * @param groupId the group id
   * @param artifactId the artifact id
   * @param type the type
   * @param classifier the classifier
   * @return the artifact or null if no artifact were found
   */
  public Artifact getUniqueArtifact(String groupId, String artifactId, String type, String classifier) {
    final Set<Artifact> candidates = getArtifacts(groupId, artifactId, type);
    if(candidates.isEmpty()) {
      return null;
    } else if(candidates.size() == 1 && classifier == null) {
      return candidates.iterator().next();
    } else if(classifier != null) {
      for(Artifact a : artifacts) {
        if(a.getClassifier() == null && classifier.equals(mainArtifactId)) {
          return a;
        } else if(classifier.equals(a.getClassifier())) {
          return a;
        }
      }
    }
    // All other cases, classifier is null and more than one candidate ; artifact not found
    return null;
  }

  /**
   * Returns the artifact with the specified parameters. <p/> If the artifact is classified and is the only one with the
   * specified groupI, artifactId and type, it will be returned. <p/> If the artifact is classified and is not the only
   * one with the specified groupI, artifactId and type, it returns null. <p/> If the artifact is not found, it returns
   * null.
   * 
   * @param groupId the group id
   * @param artifactId the artifact id
   * @param type the type
   * @return the artifact or null if no artifact were found
   */
  public Artifact getUniqueArtifact(String groupId, String artifactId, String type) {
    return getUniqueArtifact(groupId, artifactId, type, null);
  }

  /**
   * Returns the artifacts with the specified parameters.
   * 
   * @param groupId the group id
   * @param artifactId the artifact id
   * @param type the type
   * @return the artifacts or an empty set if no artifact were found
   */
  public Set<Artifact> getArtifacts(String groupId, String artifactId, String type) {
    final Set<Artifact> result = new TreeSet<Artifact>();
    for(Artifact a : artifacts) {
      // If the groupId, the artifactId and if the
      // artifact's type is known, then we have found a candidate.
      if(a.getGroupId().equals(groupId) && a.getArtifactId().equals(artifactId)
          && artifactTypeMappingService.isMappedToType(type, a.getType())) {
        result.add(a);
      }
    }
    return result;
  }

  public Artifact resolveArtifact(String groupId, String artifactId, String type, String classifier)
      throws EarPluginException {
    // If the artifact is already set no need to resolve it
    // Make sure that at least the groupId and the artifactId are specified
    if(groupId == null || artifactId == null) {
      throw new IllegalArgumentException("Could not resolve artifact[" + type + ":" + groupId + ":" + artifactId +"]");
    }
    Artifact artifact = getUniqueArtifact(groupId, artifactId, type, classifier);
    // Artifact has not been found
    if(artifact == null) {
      Set<Artifact> candidates = getArtifacts(groupId, artifactId, type);
      if(candidates.size() > 1) {
        throw new EarPluginException("Artifact[" + type + ":" + groupId + ":" + artifactId +"] has " + candidates.size()
            + " candidates, please provide a classifier.");
      } else {
        throw new EarPluginException("Artifact[" + type + ":" + groupId + ":" + artifactId +"] is not a dependency of the project.");
      }
    }
    return artifact;
  }

}
