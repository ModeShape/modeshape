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
package org.modeshape.sequencer.javafile;

import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.sequencer.javafile</code> packages.
 */
public final class JavaFileI18n {

    public static I18n unhandledAnnotationType;
    public static I18n unhandledAnnotationTypeBodyDeclarationType;
    public static I18n unhandledBodyDeclarationType;
    public static I18n unhandledCommentType;
    public static I18n unhandledTopLevelType;
    public static I18n unhandledType;

    static {
        try {
            I18n.initialize(JavaFileI18n.class);
        } catch (final Exception e) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(e);
        }
    }

    /**
     * Don't allow construction outside of this class.
     */
    private JavaFileI18n() {
        // nothing to do
    }

}
