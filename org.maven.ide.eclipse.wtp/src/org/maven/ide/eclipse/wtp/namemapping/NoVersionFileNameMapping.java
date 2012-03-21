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
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.output.NoVersionFileNameMapping 
 * 
 * A simplified version of the standard file name mapping which does not
 * retain the version in the generated file name.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class NoVersionFileNameMapping
    extends AbstractFileNameMapping
{

    public String mapFileName( Artifact a )
    {
        return generateFileName( a, false );
    }

}
