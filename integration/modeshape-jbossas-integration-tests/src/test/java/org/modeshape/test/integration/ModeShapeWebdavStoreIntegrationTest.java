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

import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.util.SardineException;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import org.modeshape.web.jcr.webdav.ModeShapeWebdavStoreClientTest;

/**
 * Integration test which runs the same tests as {@link ModeShapeWebdavStoreClientTest}, only against the AS7 distribution.
 *
 * @author Horia Chiorean
 */
@RunWith( Arquillian.class)
public class ModeShapeWebdavStoreIntegrationTest extends ModeShapeWebdavStoreClientTest {

    @Override
    protected Sardine initializeWebDavClient() throws SardineException {
        return SardineFactory.begin("admin", "admin");
    }

    @Override
    protected String getServerContext() {
        //this should be the context of the web application deployed inside AS7
        return "http://localhost:8080/modeshape-webdav";
    }

    @Override
    protected String getRepositoryName() {
        return "sample";
    }
}
