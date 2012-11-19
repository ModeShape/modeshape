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

package org.modeshape.jcr.federation;

/**
 * The key used to uniquely identify a page of children. A page key
 * is formed by joining together the id of the owning document, the offset of the page and the size of the block.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class PageKey {

    private static final String SEPARATOR = "#";

    private final String parentId;
    private final String offset;
    private final long blockSize;

    /**
     * Creates a new instance from a raw string, which is expected to contain 3 parts, each representing a piece of information
     *
     * @param key a {@code non-null} string
     * @throws IllegalArgumentException if the string is not correctly formed.
     */
    public PageKey( String key ) {
        String[] parts = key.split(SEPARATOR);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid block key string " + key + " . Expected parentId|offset|blockSize");
        }
        this.parentId = parts[0];
        this.offset = parts[1];
        try {
            this.blockSize = Long.valueOf(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Block size is not a valid integer: " + parts[2]);
        }
    }

    protected PageKey( String parentId,
                       String offset,
                       long blockSize ) {
        this.blockSize = blockSize;
        this.offset = offset;
        this.parentId = parentId;
    }

    protected PageKey( String parentId,
                       int offset,
                       long blockSize ) {
        this.blockSize = blockSize;
        this.offset = String.valueOf(offset);
        this.parentId = parentId;
    }

    /**
     * Returns the size of the block
     *
     * @return a long value
     */
    public long getBlockSize() {
        return blockSize;
    }

    /**
     * Returns the offset of the block, which semantically represents a pointer to the start of the block.
     *
     * @return a {@code non-null} String
     */
    public String getOffsetString() {
        return offset;
    }

    /**
     * Returns the integer representation of the offset, if the offset is convertible to an integer. Otherwise, {@code null}
     * is returned.
     *
     * @return the int representation of the offset, or null.
     */
    public Integer getOffsetInt() {
        try {
            return Integer.valueOf(offset);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the id of the document which owns the block and effectively the list of children,
     *
     * @return a {@code non-null} String
     */
    public String getParentId() {
        return parentId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(parentId).append(SEPARATOR);
        sb.append(offset).append(SEPARATOR);
        sb.append(blockSize);
        return sb.toString();
    }
}
