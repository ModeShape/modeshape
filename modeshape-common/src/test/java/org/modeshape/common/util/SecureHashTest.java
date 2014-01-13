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
        assertSecureHashStreamWorks(algorithm, "/org/modeshape/common/logging/LoggerTest.properties");
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
