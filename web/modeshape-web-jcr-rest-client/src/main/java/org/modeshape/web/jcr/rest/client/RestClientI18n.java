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
package org.modeshape.web.jcr.rest.client;

import org.modeshape.common.i18n.I18n;

/**
 * The <code>RestClientI18n</code> class provides localized messages.
 */
public final class RestClientI18n {

    public static I18n nullArgumentMsg;
    public static I18n repositoryShortDescription;
    public static I18n serverShortDescription;
    public static I18n unknownHttpRequestMethodMsg;
    public static I18n workspaceShortDescription;
    public static I18n nodeTypeShortDescription;
    public static I18n propertyDefinitionShortDescription;
    public static I18n childNodeDefinitionShortDescription;
    public static I18n unableToConvertValue;

    // JsonRestClient messages
    public static I18n connectionErrorMsg;
    public static I18n createFileFailedMsg;
    public static I18n updateFileFailedMsg;
    public static I18n createFolderFailedMsg;
    public static I18n markPublishAreaFailedMsg;
    public static I18n unmarkPublishAreaFailedMsg;
    public static I18n updateFolderFailedMsg;
    public static I18n validateFailedMsg;
    public static I18n getRepositoriesFailedMsg;
    public static I18n getWorkspacesFailedMsg;
    public static I18n publishFailedMsg;
    public static I18n unpublishFailedMsg;
    public static I18n unpublishNeverPublishedMsg;
    public static I18n publishSucceededMsg;
    public static I18n unpublishSucceededMsg;
    public static I18n invalidQueryLanguageMsg;
    public static I18n invalidQueryMsg;
    public static I18n getNodeTypesFailedMsg;
    public static I18n getNodeTypeFailedMsg;

    private RestClientI18n() {
    }

    static {
        try {
            I18n.initialize(RestClientI18n.class);
        } catch (Exception e) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(e);
        }
    }

}
