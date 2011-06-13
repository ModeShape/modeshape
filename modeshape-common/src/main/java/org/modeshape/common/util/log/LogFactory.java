/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
* See the AUTHORS.txt file in the distribution for a full listing of 
* individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 * The abstract class for the LogFactory, which is called to create a specific implementation of the @link Logger.
 */

package org.modeshape.common.util.log;


import org.modeshape.common.util.ClassUtil;
import org.modeshape.common.util.Logger;

public abstract class LogFactory {
    
    private static LogFactory LOGFACTORY;
        
    static {
	try {
	    ClassUtil.loadClassStrict("org.apache.log4j.Logger");
	    LOGFACTORY = new SLF4JLogFactory();

	} catch (ClassNotFoundException cnfe) {
	    LOGFACTORY = new JdkLogFactory();
	}

    }
    
    public static LogFactory getLogFactory() {
	return LOGFACTORY;
    }

        
    /**
     * Return a logger named corresponding to the class passed as parameter, using the statically bound {@link ILoggerFactory}
     * instance.
     * 
     * @param clazz the returned logger will be named after clazz
     * @return logger
     */
    public abstract Logger getLogger( Class<?> clazz );

    /**
     * Return a logger named according to the name parameter using the statically bound {@link ILoggerFactory} instance.
     * 
     * @param name The name of the logger.
     * @return logger
     */
    public abstract Logger getLogger( String name );

}

final class SLF4JLogFactory extends LogFactory {
    
    public Logger getLogger( Class<?> clazz ) {
	return getLogger(clazz.getName());
    }

    public Logger getLogger( String name ) {
        return new SLF4JLoggerImpl(name);
    } 
    
   
}

final class JdkLogFactory extends LogFactory {
    public Logger getLogger( Class<?> clazz ) {
	return getLogger(clazz.getName());
    }

    public Logger getLogger( String name ) {
        return new JdkLoggerImpl(name);
    } 
}

