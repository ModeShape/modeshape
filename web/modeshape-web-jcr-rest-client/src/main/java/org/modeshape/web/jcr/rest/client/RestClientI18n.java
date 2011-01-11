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

    static {
        try {
            I18n.initialize(RestClientI18n.class);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

}
