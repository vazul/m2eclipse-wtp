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
package org.maven.ide.eclipse.wtp.namemapping;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.maven.ide.eclipse.wtp.ArtifactHelper;



/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.output.AbstractFileNameMapping 
 * 
 * A base class used to generate the standard name of an
 * artifact instead of relying on the (potentially) wrong
 * file name provided by {@link org.apache.maven.artifact.Artifact#getFile()}.
 *
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public abstract class AbstractFileNameMapping
    implements FileNameMapping
{


    /**
     * Generates a standard file name for the specified {@link Artifact}.
     * <p/>
     * Returns something like <tt>artifactId-version[-classifier].extension</tt>
     * if <tt>addVersion</tt> is true. Otherwise it generates something
     * like <tt>artifactId[-classifier].extension</tt>
     *
     * @param a          the artifact to generate a filename from
     * @param addVersion whether the version should be added
     * @return the filename, with a standard format
     */
    protected String generateFileName( final Artifact a, boolean addVersion )
    {
    	ArtifactHandler artifactHandler = a.getArtifactHandler();
    	ArtifactHelper.fixArtifactHandler(artifactHandler);
        String extension = artifactHandler.getExtension();
        final StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( a.getArtifactId() );
        if ( addVersion )
        {
            buffer.append( '-' ).append( a.getBaseVersion() );
        }
        if ( a.hasClassifier() )
        {
            buffer.append( '-' ).append( a.getClassifier() );
        }
        if ( extension != null && extension.length() > 0 )
        {
            buffer.append( '.' ).append( extension );
        }

        return buffer.toString();
    }
}
