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
package org.modeshape.jcr;

import java.util.Map;
import javax.transaction.Synchronization;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.infinispan.AdvancedCache;

/**
 * An extension of {@link JcrSession} that is also an {@link XAResource}, enabling clients to explicitly enlist the session in the
 * transaction.
 * <p>
 * This implementation simply delegates to the {@link AdvancedCache#getXAResource() Infinispan cache's XAResource} instance, since
 * ModeShape does not need to itself be a resource that participates in the transaction. (Instead, ModeShape registers as a
 * {@link Synchronization} on the transaction, ensuring that it is notified when the transaction completes.)
 * </p>
 */
public class JcrXaSession extends JcrSession implements XAResource {

    protected JcrXaSession( JcrRepository repository,
                            String workspaceName,
                            ExecutionContext context,
                            Map<String, Object> sessionAttributes,
                            boolean readOnly ) {
        super(repository, workspaceName, context, sessionAttributes, readOnly);
    }

    protected final XAResource delegate() {
        return repository.documentStore().xaResource();
    }

    @Override
    public void start( Xid xid,
                       int flags ) throws XAException {
        delegate().start(xid, flags);
    }

    @Override
    public void end( Xid xid,
                     int flags ) throws XAException {
        delegate().end(xid, flags);
    }

    @Override
    public int prepare( Xid xid ) throws XAException {
        return delegate().prepare(xid);
    }

    @Override
    public void commit( Xid xid,
                        boolean onePhase ) throws XAException {
        delegate().commit(xid, onePhase);
    }

    @Override
    public void rollback( Xid xid ) throws XAException {
        delegate().rollback(xid);
    }

    @Override
    public void forget( Xid xid ) throws XAException {
        delegate().forget(xid);
    }

    @Override
    public Xid[] recover( int flag ) throws XAException {
        return delegate().recover(flag);
    }

    @Override
    public boolean isSameRM( XAResource xaRes ) throws XAException {
        return delegate().isSameRM(xaRes);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return delegate().getTransactionTimeout();
    }

    @Override
    public boolean setTransactionTimeout( int seconds ) throws XAException {
        return delegate().setTransactionTimeout(seconds);
    }

}
