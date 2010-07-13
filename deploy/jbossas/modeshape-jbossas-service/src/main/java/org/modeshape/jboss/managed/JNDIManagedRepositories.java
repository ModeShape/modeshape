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
package org.modeshape.jboss.managed;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.util.naming.Util;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.common.util.Logger.Level;
import org.modeshape.jcr.api.Repositories;

public final class JNDIManagedRepositories implements Repositories,
	Serializable {

    /**
     */
    private static final long serialVersionUID = 1L;

    /**
     */

    private static final Logger LOGGER = Logger
	    .getLogger(JNDIManagedRepositories.class.getName());

    private String jndiName = null;
    private transient ManagedEngine managedEngine = null;

    public JNDIManagedRepositories() {
    }

    public void setJndiName(String jndiName) throws Exception {
	CheckArg.isNotNull(jndiName, "jndiName");
	this.jndiName = jndiName;
    }

    public void start() throws NamingException {
	try {
	    rebind();
	} catch (NamingException e) {
	    NamingException ne = new NamingException(StringUtil
		    .createString(JBossManagedI18n.errorBindingToJNDI
			    .text(new Object[] { this.jndiName })));
	    ne.setRootCause(e);
	    throw ne;
	}
    }

    public void stop() {
	unbind(this.jndiName);
    }

    public void setManagedEngine(ManagedEngine engine) {
	this.managedEngine = engine;
    }

    @Override
    public javax.jcr.Repository getRepository(String repositoryName)
	    throws RepositoryException {
	return managedEngine.getEngine().getRepository(repositoryName);

    }

    @Override
    public Set<String> getRepositoryNames() {
	try {
	    return managedEngine.getEngine().getRepositoryNames();
	} catch (Exception e) {
	    e.printStackTrace();
	    return Collections.emptySet();
	}
    }

    private void rebind() throws NamingException {
	Context ctx = new InitialContext();

	Util.rebind(ctx, jndiName, this);

	LOGGER.log(Level.INFO, JBossManagedI18n.logModeShapeBoundToJNDI,
		new Object[] { jndiName });
    }

    private void unbind(String jndiName) {
	try {
	    InitialContext ctx = new InitialContext();
	    Util.unbind(ctx, jndiName);

	    LOGGER.log(Level.INFO, JBossManagedI18n.logModeShapeUnBoundToJNDI,
		    new Object[] { jndiName });
	} catch (NamingException e) {
	    // do nothing
	}
    }

}
