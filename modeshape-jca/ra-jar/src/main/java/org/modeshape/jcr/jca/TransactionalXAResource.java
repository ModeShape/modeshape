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
package org.modeshape.jcr.jca;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 *
 * @author kulikov
 */
public class TransactionalXAResource implements XAResource {

    private XAResource xaResource;
    private JcrManagedConnection connection;
    private boolean ending;

    public TransactionalXAResource(JcrManagedConnection connection,
            XAResource xaResource) {
        super();
        this.xaResource = xaResource;
        this.connection = connection;
    }
    
    /**
     * There is a one-to-one Relation between this TransactionBoundXAResource
     * and the JCAManagedConnection so the used XAResource must be in sync when it is changed in the
     * JCAManagedConnection.
     * @param res
     */
    protected void rebind(XAResource res) {
        this.xaResource = res;
    }

    @Override
    public void commit(Xid xid, boolean bln) throws XAException {
        xaResource.commit(xid, bln);
    }

    @Override
    public void end(Xid xid, int i) throws XAException {
        if (!ending) {
            this.ending = true;
            try {
                xaResource.end(xid, i);
            } finally {
                this.connection.closeHandles();
            }
            // reuse the XAResource
            this.ending = false;
        }
    }

    @Override
    public void forget(Xid xid) throws XAException {
        xaResource.forget(xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return xaResource.getTransactionTimeout();
    }

    @Override
    public boolean isSameRM(XAResource xar) throws XAException {
        return xaResource.isSameRM(xar);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return xaResource.prepare(xid);
    }

    @Override
    public Xid[] recover(int i) throws XAException {
        return xaResource.recover(i);
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        xaResource.rollback(xid);
    }

    @Override
    public boolean setTransactionTimeout(int i) throws XAException {
        return xaResource.setTransactionTimeout(i);
    }

    @Override
    public void start(Xid xid, int i) throws XAException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
