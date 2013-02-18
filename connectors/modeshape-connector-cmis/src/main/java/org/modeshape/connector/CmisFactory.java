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
package org.modeshape.connector;

import java.util.Map;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.bindings.spi.StandardAuthenticationProvider;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.w3c.dom.Element;

/**
 * Establishes session with CMIS repository.
 *
 * The general purpose of this class is to provide two types of session
 * implementations: real apache chemistry and dummy implementation which is used
 * for test purposes only.
 * 
 * @author kulikov
 */
public class CmisFactory {
    public enum Mode {
        TEST, SERVICE
    }
    
    public Session session(Mode mode, Map<String, String> parameters) {
        switch (mode) {
            case TEST:
                return TestModeFactory.getSession();
            case SERVICE:
                SessionFactoryImpl factory = SessionFactoryImpl.newInstance();
                return factory.createSession(parameters, null,
                        new StandardAuthenticationProvider() {
                            @Override
                            public Element getSOAPHeaders(Object portObject) {
                                //Place headers here
                                return super.getSOAPHeaders(portObject);
                            };
                        }, null);

        }
        return null;
    }
}
