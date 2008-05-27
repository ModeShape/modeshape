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
package org.jboss.dna.spi.graph;

import java.io.InputStream;
import java.io.Serializable;
import net.jcip.annotations.Immutable;

/**
 * Value holder for binary data. Binary instances are not mutable.
 * @author Randall Hauch
 */
@Immutable
public interface Binary extends Comparable<Binary>, Serializable {

    /**
     * Get the length of this binary data.
     * @return the number of bytes in this binary data
     */
    public long getSize();

    /**
     * Get the contents of this data as a stream.
     * @return the stream to this data's contents
     */
    public InputStream getStream();

    /**
     * Get the contents of this data as a byte array.
     * @return the data as an array
     */
    public byte[] getBytes();

    /**
     * Acquire any resources for this data. This method must be called before any other method on this object.
     */
    public void acquire();

    /**
     * Release any acquired resources. This method must be called after a client is finished with this value.
     */
    public void release();

}
