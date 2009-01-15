/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.property.basic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.Base64;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.SecureHash;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.ValueComparators;

/**
 * An abstract implementation of {@link Binary} that provides some common capabilities for other implementations.
 * 
 * @author Randall Hauch
 */
@Immutable
public abstract class AbstractBinary implements Binary {

    protected static final Set<String> ALGORITHMS_NOT_FOUND_AND_LOGGED = new CopyOnWriteArraySet<String>();
    private static final SecureHash.Algorithm ALGORITHM = SecureHash.Algorithm.SHA_1;
    private static final byte[] NO_HASH = new byte[] {};

    protected static final byte[] EMPTY_CONTENT = new byte[0];

    /**
     * Version {@value} .
     */
    private static final long serialVersionUID = 1L;

    protected AbstractBinary() {
    }

    protected byte[] computeHash( byte[] content ) {
        try {
            return SecureHash.getHash(ALGORITHM, content);
        } catch (NoSuchAlgorithmException e) {
            if (ALGORITHMS_NOT_FOUND_AND_LOGGED.add(ALGORITHM.digestName())) {
                Logger.getLogger(getClass()).error(e, GraphI18n.messageDigestNotFound, ALGORITHM.digestName());
            }
            return NO_HASH;
        }
    }

    protected byte[] computeHash( File file ) {
        try {
            return SecureHash.getHash(ALGORITHM, file);
        } catch (NoSuchAlgorithmException e) {
            if (ALGORITHMS_NOT_FOUND_AND_LOGGED.add(ALGORITHM.digestName())) {
                Logger.getLogger(getClass()).error(e, GraphI18n.messageDigestNotFound, ALGORITHM.digestName());
            }
            return NO_HASH;
        } catch (IOException e) {
            if (ALGORITHMS_NOT_FOUND_AND_LOGGED.add(ALGORITHM.digestName())) {
                Logger.getLogger(getClass()).error(e, GraphI18n.messageDigestNotFound, ALGORITHM.digestName());
            }
            return NO_HASH;
        }
    }

    protected byte[] computeHash( InputStream stream ) {
        try {
            return SecureHash.getHash(ALGORITHM, stream);
        } catch (NoSuchAlgorithmException e) {
            if (ALGORITHMS_NOT_FOUND_AND_LOGGED.add(ALGORITHM.digestName())) {
                Logger.getLogger(getClass()).error(e, GraphI18n.messageDigestNotFound, ALGORITHM.digestName());
            }
            return NO_HASH;
        } catch (IOException e) {
            if (ALGORITHMS_NOT_FOUND_AND_LOGGED.add(ALGORITHM.digestName())) {
                Logger.getLogger(getClass()).error(e, GraphI18n.messageDigestNotFound, ALGORITHM.digestName());
            }
            return NO_HASH;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Binary o ) {
        return ValueComparators.BINARY_COMPARATOR.compare(this, o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Binary) {
            Binary that = (Binary)obj;
            if (this.getSize() != that.getSize()) return false;
            return ValueComparators.BINARY_COMPARATOR.compare(this, that) == 0;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            acquire();
            StringBuilder sb = new StringBuilder(super.toString());
            sb.append(" len=").append(getSize()).append("; [");
            sb.append(Base64.encodeBytes(getBytes()));
            return sb.toString();
        } finally {
            release();
        }
    }

}
