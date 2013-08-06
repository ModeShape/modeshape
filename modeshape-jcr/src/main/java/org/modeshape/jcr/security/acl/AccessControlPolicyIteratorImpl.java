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
package org.modeshape.jcr.security.acl;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;

/**
 * Provides iteration over series of the ACLs.
 * 
 * @author kulikov
 */
public class AccessControlPolicyIteratorImpl implements AccessControlPolicyIterator {

    private ArrayList<AccessControlPolicy> list = new ArrayList<AccessControlPolicy>();
    private int index;
    
    public final static AccessControlPolicyIteratorImpl EMPTY = new AccessControlPolicyIteratorImpl();
    
    /**
     * Creates new instance of this iterator.
     * 
     * @param policy series of the ACLs
     */
    public AccessControlPolicyIteratorImpl(AccessControlPolicy... policy) {
        for (int i = 0; i < policy.length; i++) {
            list.add(policy[i]);
        }
    }
    
    @Override
    public AccessControlPolicy nextAccessControlPolicy() {
        if (index < list.size()) {
            return list.get(index++);
        }
        throw new NoSuchElementException();
    }

    @Override
    public void skip(long amount) {
        index += amount;
    }

    @Override
    public long getSize() {
        return list.size();
    }

    @Override
    public long getPosition() {
        return index;
    }

    @Override
    public boolean hasNext() {
        return index < list.size();
    }

    @Override
    public Object next() {
        return list.get(index++);
    }

    @Override
    public void remove() {
        list.remove(index);
    }
    
}
