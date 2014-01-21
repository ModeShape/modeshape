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
package org.modeshape.web.jcr.webdav;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.web.jcr.webdav</code> package.
 */
@Immutable
public final class WebdavI18n {

    public static I18n noStoredRequest;
    public static I18n uriIsProperty;
    public static I18n warnMultiValuedProperty;
    public static I18n errorPropertyPath;

    // DefaultRequestResolver messages
    public static I18n requiredParameterMissing;
    public static I18n cannotCreateRepository;
    public static I18n cannotCreateWorkspaceInRepository;
    public static I18n cannotGetRepositorySession;

    private WebdavI18n() {
    }

    static {
        try {
            I18n.initialize(WebdavI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(err);
        }
    }
}
