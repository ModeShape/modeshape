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
package org.modeshape.connector.cmis;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.BasicName;

/**
 * Lexicon of names used by CMIS connector.
 * 
 * @author kulikov
 */
@Immutable
public class CmisLexicon {
    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/cmis/1.0";
        public static final String PREFIX = "cmis";
    }

    public static final Name REPOSITORY = new BasicName(Namespace.URI, "repository");
    public static final Name VENDOR_NAME = new BasicName(Namespace.URI, "vendorName");
    public static final Name PRODUCT_NAME = new BasicName(Namespace.URI, "productName");
    public static final Name PRODUCT_VERSION = new BasicName(Namespace.URI, "productVersion");
}
