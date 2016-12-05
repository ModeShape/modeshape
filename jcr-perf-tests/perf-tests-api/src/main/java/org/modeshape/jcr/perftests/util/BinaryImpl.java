/*
 * JBoss, Home of Professional Open Source
 * Copyright [2011], Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.perftests.util;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * @author Horia Chiorean
 */
public final class BinaryImpl implements Binary {

    private byte[] randomBytes;

    public BinaryImpl(int size) {
        this.randomBytes = new byte[size];
        new Random().nextBytes(randomBytes);
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        return new ByteArrayInputStream(randomBytes);
    }

    @Override
    public int read( byte[] b, long position ) throws IOException, RepositoryException {
        System.arraycopy(randomBytes, (int) position, b, 0, b.length); //should never use data large enough for the cast to be a problem
        return b.length;
    }

    @Override
    public long getSize() throws RepositoryException {
        return randomBytes.length;
    }

    @Override
    public void dispose() {
        randomBytes = null;
    }
}
