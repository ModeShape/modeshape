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
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 */
public class SvnActionExecutor implements ScmActionExecutor {

    private final SVNRepository repository;

    /**
     * @param repository
     */
    public SvnActionExecutor( SVNRepository repository ) {
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
     * @throws SVNException
     */
    public void execute( ScmAction action,
                         String message ) throws SVNException {
        ISVNEditor editor = this.repository.getCommitEditor(message, null);
        editor.openRoot(-1);
        try {
            action.applyAction(editor);
        } catch (Exception e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "This error is appeared: '{0}'", e.getMessage());
            throw new SVNException(err, e);
        }
        editor.closeDir();
        editor.closeEdit();

    }
}
