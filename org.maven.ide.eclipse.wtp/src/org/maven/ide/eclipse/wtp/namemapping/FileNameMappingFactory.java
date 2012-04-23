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

/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.output.FileNameMapping 
 * 
 * Provides access to {@link FileNameMapping} implementations.
 * <p/>
 * Two basic implementations are provided by default:
 * <ul>
 * <li>standard: the default implementation</li>
 * <li>full: an implementation that maps to a 'full' file name, i.e. containing the groupId</li>
 * </ul>
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: FileNameMappingFactory.java 992847 2010-09-05 18:16:55Z snicoll $
 */
public final class FileNameMappingFactory
{
    static final String STANDARD_FILE_NAME_MAPPING = "standard";

    static final String FULL_FILE_NAME_MAPPING = "full";

    static final String NO_VERSION_FILE_NAME_MAPPING = "no-version";


    private FileNameMappingFactory()
    {
    }

    public static FileNameMapping getDefaultFileNameMapping()
    {
        return new StandardFileNameMapping();
    }

    /**
     * Returns the file name mapping implementation based on a logical name
     * of a fully qualified name of the class.
     *
     * @param nameOrClass a name of the fqn of the implementation
     * @return the file name mapping implementation
     * @throws IllegalStateException if the implementation is not found
     */
    public static FileNameMapping getFileNameMapping( final String nameOrClass )
        throws IllegalStateException
    {
        if ( STANDARD_FILE_NAME_MAPPING.equals( nameOrClass ) )
        {
            return getDefaultFileNameMapping();
        }
        if ( FULL_FILE_NAME_MAPPING.equals( nameOrClass ) )
        {
            return new FullFileNameMapping();
        }
        if ( NO_VERSION_FILE_NAME_MAPPING.equals( nameOrClass ) )
        {
            return new NoVersionFileNameMapping();
        }
        try
        {
            final Class<? extends FileNameMapping> c = Class.forName( nameOrClass ).asSubclass(FileNameMapping.class);
            return c.newInstance();
        }
        catch ( ClassNotFoundException e )
        {
            throw new IllegalStateException(
                "File name mapping implementation[" + nameOrClass + "] was not found " + e.getMessage() );
        }
        catch ( InstantiationException e )
        {
            throw new IllegalStateException( "Could not instantiate file name mapping implementation[" + nameOrClass +
                                                 "] make sure it has a default public constructor" );
        }
        catch ( IllegalAccessException e )
        {
            throw new IllegalStateException( "Could not access file name mapping implementation[" + nameOrClass +
                                                 "] make sure it has a default public constructor" );
        }
        catch ( ClassCastException e )
        {
            throw new IllegalStateException(
                "Specified class[" + nameOrClass + "] does not implement[" + FileNameMapping.class.getName() + "]" );
        }
    }
}
