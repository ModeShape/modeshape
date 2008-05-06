/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.jcr.AbstractJcrRepositoryTest;
import org.jboss.dna.repository.util.ExecutionContext;
import org.jboss.dna.repository.util.JcrTools;
import org.jboss.dna.repository.util.SessionFactory;

/**
 * @author Randall Hauch
 */
public class MockExecutionContext implements ExecutionContext {

    private JcrTools tools = new JcrTools();
    private SessionFactory sessionFactory;

    public MockExecutionContext( final AbstractJcrRepositoryTest test, final String repositoryName ) {
        this.sessionFactory = new SessionFactory() {

            public Session createSession( String name ) throws RepositoryException {
                assertThat(name, is(repositoryName));
                try {
                    return test.getRepository().login(test.getTestCredentials());
                } catch (IOException e) {
                    throw new SystemFailureException(e);
                }
            }
        };
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * {@inheritDoc}
     */
    public JcrTools getTools() {
        return tools;
    }
}
