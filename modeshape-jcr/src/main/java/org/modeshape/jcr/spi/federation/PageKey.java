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

package org.modeshape.jcr.spi.federation;

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

    public PageKey( String parentId,
                    String offset,
                    long blockSize ) {
        this.blockSize = blockSize;
        this.offset = offset;
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
