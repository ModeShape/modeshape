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

package org.modeshape.jcr.text;

import org.modeshape.jcr.api.text.TextExtractor;


/**
 * A {@link org.modeshape.jcr.api.text.TextExtractor.Output} implementation which appends each incoming text into a buffer,
 * separating the content via the configured separator.
 *
 * @author Horia Chiorean
 */
public final class TextExtractorOutput implements TextExtractor.Output {

    private static final String DEFAULT_SEPARATOR = " ";

    private final StringBuilder buffer = new StringBuilder("");
    private final String separator;

    public TextExtractorOutput() {
        this(DEFAULT_SEPARATOR);
    }

    public TextExtractorOutput( String separator ) {
        this.separator = separator;
    }

    @Override
    public void recordText( String text ) {
        if (buffer.length() > 0) {
            buffer.append(separator);
        }
        buffer.append(text);
    }

    public String getText() {
        return buffer.toString();
    }
}
