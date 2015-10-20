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
package org.modeshape.jcr.index.lucene;

import org.modeshape.common.i18n.I18n;

/**
 * I18n constants for the Lucene index provider.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
public final class LuceneIndexProviderI18n {
    
    public static I18n warnErrorWhileClosingSearcher;      
    public static I18n multiColumnTextIndexesNotSupported;      
    public static I18n invalidColumnType; 
    public static I18n invalidOperatorForPropertyType;
    public static I18n invalidOperatorForOperand;

    private LuceneIndexProviderI18n() {
    }

    static {
        try {
            I18n.initialize(LuceneIndexProviderI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(err);
        }
    }
}
