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
package org.jboss.dna.connector.svn;

import org.jboss.dna.connector.scm.ScmAction;
import org.jboss.dna.connector.scm.ScmActionExecutor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * @author Serge Pagop
 */
public class SVNActionExecutor implements ScmActionExecutor {

    private SVNRepository repository;

    /**
     * @param repository
     */
    public SVNActionExecutor( SVNRepository repository ) {
        this.repository = repository;
    }

    /**
     * @return repository
     */
    public SVNRepository getRepository() {
        return repository;
    }

    /**
     * @param action
     * @param message
     * @throws Exception 
     */
    public void execute( ScmAction action,
                         String message ) throws Exception {
        try {
            ISVNEditor editor = this.repository.getCommitEditor( message,
                                                                 null );
            editor.openRoot( -1 );
            action.applyAction( editor );
            editor.closeDir();
            editor.closeEdit();
        } catch ( SVNException e ) {
            e.printStackTrace();
            //logger.error( "svn error: " );
            throw e;
        }
    }
}
