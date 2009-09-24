/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.eclipse.jcr.rest.client.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public final class StaticLoggerBinder implements LoggerFactoryBinder {

    // ===========================================================================================================================
    // Class Fields
    // ===========================================================================================================================

    /**
     * The class name of the logger factory.
     */
    private static final String LOGGER_FACTORY_CLASS_NAME = EclipseLoggerFactory.class.getName();

    /**
     * The unique instance of this class.
     */
    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @return the static instance of the logger
     */
    public static final StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The logger factory used.
     */
    private final ILoggerFactory loggerFactory = new EclipseLoggerFactory();

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.spi.LoggerFactoryBinder#getLoggerFactory()
     */
    @Override
    public ILoggerFactory getLoggerFactory() {
        return this.loggerFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.spi.LoggerFactoryBinder#getLoggerFactoryClassStr()
     */
    @Override
    public String getLoggerFactoryClassStr() {
        return LOGGER_FACTORY_CLASS_NAME;
    }

}
