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
package org.modeshape.common.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.common.util.SecureHash.HashingInputStream;

public class SecureHashTest {

    @Test
    public void shouldCorrectlyComputeSecureHashUsingMD2() throws Exception {
        assertCorrectlyComputeSecureHashUsing(Algorithm.MD2);
    }

    @Test
    public void shouldCorrectlyComputeSecureHashUsingMD5() throws Exception {
        assertCorrectlyComputeSecureHashUsing(Algorithm.MD5);
    }

    @Test
    public void shouldCorrectlyComputeSecureHashUsingSHA1() throws Exception {
        assertCorrectlyComputeSecureHashUsing(Algorithm.SHA_1);
    }

    @Test
    public void shouldCorrectlyComputeSecureHashUsingSHA256() throws Exception {
        assertCorrectlyComputeSecureHashUsing(Algorithm.SHA_256);
    }

    @Test
    public void shouldCorrectlyComputeSecureHashUsingSHA384() throws Exception {
        assertCorrectlyComputeSecureHashUsing(Algorithm.SHA_384);

    }

    protected void assertCorrectlyComputeSecureHashUsing( Algorithm algorithm ) throws Exception {
        assertSecureHashStreamWorks(algorithm, "/org/modeshape/common/i18n/I18nTest$TestI18n_en.properties");
        assertSecureHashStreamWorks(algorithm, "/org/modeshape/common/i18n/I18nTest$TestI18n_fr.properties");
        assertSecureHashStreamWorks(algorithm, "/org/modeshape/common/i18n/MockI18n.properties");
        assertSecureHashStreamWorks(algorithm, "/org/modeshape/common/util/additionalmime.types");
        assertSecureHashStreamWorks(algorithm, "/org/modeshape/common/util/LoggerTest.properties");
        assertSecureHashStreamWorks(algorithm, "/log4j.properties");
        assertSecureHashStreamWorks(algorithm, "/maven-metadata-repository.jboss.org.xml");
    }

    protected void assertSecureHashStreamWorks( Algorithm algorithm,
                                                String resourceName ) throws IOException, NoSuchAlgorithmException {
        // Find the content of the file ...
        InputStream stream = getClass().getResourceAsStream(resourceName);
        assertThat(stream, is(notNullValue()));
        byte[] bytesThruStream = IoUtil.readBytes(stream);

        // Find the secure hash of the file ...
        stream = getClass().getResourceAsStream(resourceName);
        assertThat(stream, is(notNullValue()));
        byte[] hashThruStream = null;
        try {
            hashThruStream = SecureHash.getHash(algorithm, stream);
        } finally {
            stream.close();
        }

        // Now try reading the stream using a hash stream ...
        stream = getClass().getResourceAsStream(resourceName);
        assertThat(stream, is(notNullValue()));
        HashingInputStream hashingStream = SecureHash.createHashingStream(algorithm, stream);
        byte[] bytesThruHashingStream = IoUtil.readBytes(hashingStream); // closes stream
        byte[] hashThruHashingStream = hashingStream.getHash();

        // The content should be the same ..
        assertThat(bytesThruHashingStream, is(bytesThruStream));

        // The hash should also be the same ...
        assertThat(hashThruHashingStream, is(hashThruStream));

        // System.out.println(algorithm.digestName() + "---> " + hashingStream.getHashAsHexString() + " of " + resourceName);
    }

}
