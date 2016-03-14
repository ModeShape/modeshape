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
package org.modeshape.shell;

import org.modeshape.common.i18n.I18n;

/**
 *
 * @author kulikov
 */
public class ShellI18n {
    public static I18n sshUserNotSpecified;
    public static I18n sshCouldNotStartServer;
    public static I18n unknownCommand;
    public static I18n connectHelp;
    public static I18n changeNodeHelp;
    public static I18n pwdHelp;
    public static I18n queryHelp;
    public static I18n nodeAddMixinHelp;
    public static I18n nodeAddNodeHelp;
    public static I18n nodeShowIndexHelp;
    public static I18n nodeShowIdentifierHelp;
    public static I18n nodeShowPrimaryTypeHelp;
    public static I18n nodeShowMixinsHelp;
    public static I18n nodeShowNodesHelp;
    public static I18n nodeShowPropertiesHelp;
    public static I18n nodeShowPropertyHelp;
    public static I18n nodeShowReferencesHelp;
    public static I18n nodeSetPrimaryTypeHelp;
    public static I18n nodeSetPropertyValueHelp;
    public static I18n nodeUploadHelp;
    public static I18n nodeDownloadHelp;
    public static I18n repositoryBackupHelp;
    public static I18n repositoryRestoreHelp;
    public static I18n scriptHelp;
    public static I18n sessionRefreshHelp;
    public static I18n sessionExitHelp;
    public static I18n sessionSaveHelp;
    public static I18n sessionStatusHelp;
    public static I18n sessionImpersonateHelp;
    public static I18n sessionMoveHelp;
    public static I18n sessionRemoveHelp;
    public static I18n sessionImportHelp;
    public static I18n sessionExportHelp;
    public static I18n sessionShowAttributesHelp;
    public static I18n sessionShowAttributeHelp;
    public static I18n sessionShowPrefixesHelp;
    public static I18n sessionShowPrefixHelp;
    public static I18n sessionShowUriHelp;
    public static I18n versionCheckinHelp;
    public static I18n versionCheckoutHelp;
    public static I18n versionMergeHelp;
    public static I18n versionRestoreHelp;
    public static I18n versionShowBaseVersion;
    public static I18n versionShowHistoryHelp;
    public static I18n versionShowStatusHelp;
    public static I18n workspaceCloneHelp;
    public static I18n workspaceCopyHelp;
    public static I18n workspaceCreateHelp;
    public static I18n workspaceDeleteHelp;
    public static I18n workspaceImport;
    public static I18n workspaceShowName;
    public static I18n workspaceShowNames;
    public static I18n typeRegisterHelp;
    public static I18n typeUnregisterHelp;
    public static I18n typeShowTypesHelp;
    public static I18n typeShowPropertyDefsHelp;
    public static I18n typeShowNodeDefsHelp;
    public static I18n aclShowHelp;
    public static I18n aclSetHelp;
    public static I18n aclRemoveHelp;
    public static I18n engineExitHelp;
    public static I18n engineShowRepositoriesHelp;
    public static I18n engineShowRepositoryStateHelp;
    public static I18n engineStartRepositoryHelp;
    public static I18n engineStopRepositoryHelp;
    public static I18n engineDeployRepositoryHelp;
    public static I18n engineUndeployRepositoryHelp;
    public static I18n engineConfigureRepositoryHelp;
    public static I18n configureSaveHelp;
    
    private ShellI18n() {
    }

    static {
        try {
            I18n.initialize(ShellI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(err);
        }
    }    
}
