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

package org.modeshape.test.integration;

import javax.inject.Inject;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.xml.sax.ContentHandler;

/**
 * A test class which uses CDI and the {@link RepositoryProvider} class to obtain a node at a given path.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class CDIRepositoryConsumer {

    @Inject
    private CDIRepositoryProvider repositoryProvider;

    protected Node nodeAt(String absPath) throws RepositoryException {
        return repositoryProvider.getSampleRepositorySession().getNode(absPath);
    }

    protected ContentHandler importContentHandler(String absPath) throws RepositoryException {
        return repositoryProvider.getSampleRepositorySession().getImportContentHandler(absPath, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
    }
}
