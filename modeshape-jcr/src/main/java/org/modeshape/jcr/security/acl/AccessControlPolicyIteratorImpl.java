/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
