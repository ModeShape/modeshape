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
package org.modeshape.web;

import static org.junit.Assert.assertEquals;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.Test;

/**
 * Testing class for interaction with Modeshape Web Explorer
 *
 * @author Oleg Kulikov (okulikov@redhat.com)
 */
public class ExplorerTest {

    @Test
    public void shouldAccessInitialPage() throws Exception {
        URL url = new URL("http://localhost:8090/modeshape-explorer");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();
        assertEquals(200, code);
    }

}
