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

package org.modeshape.jcr.mimetype;

import javax.jcr.Binary;
import org.modeshape.common.annotation.Immutable;

/**
 * Implementation of {@link MimeTypeDetector} which doesn't detect mime-types.
 * 
 * @author Horia Chiorean
 */
@Immutable
public final class NullMimeTypeDetector implements MimeTypeDetector {

    public static final MimeTypeDetector INSTANCE = new NullMimeTypeDetector();

    private NullMimeTypeDetector() {
    }

    @Override
    public String mimeTypeOf( String name,
                              Binary binaryValue ) {
        return null;
    }
}
