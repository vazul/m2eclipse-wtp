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

/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.output.FileNameMapping 
 * 
 * Maps file name {@link Artifact}.
 * <p/>
 * TODO: it might be easier to use a token-based approach instead.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: FileNameMapping.java 992370 2010-09-03 16:48:59Z snicoll $
 */
public interface FileNameMapping
{

    /**
     * Returns the file name of the specified artifact.
     *
     * @param a the artifact
     * @return the name of the file for the specified artifact
     */
    String mapFileName( final Artifact a );
}
