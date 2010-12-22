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
package org.modeshape.jcr;

import net.jcip.annotations.Immutable;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.jcr*</code> packages.
 */
@Immutable
public final class JcrI18n {

    public static I18n engineStarting;
    public static I18n engineStarted;
    public static I18n couldNotStartEngine;
    public static I18n engineStopping;
    public static I18n engineStopped;
    public static I18n repositoryReferencesNonExistantSource;

    public static I18n cannotConvertValue;
    public static I18n credentialsMustProvideJaasMethod;
    public static I18n mustBeInPrivilegedAction;
    public static I18n loginConfigNotFound;
    public static I18n credentialsMustReturnLoginContext;
    public static I18n defaultWorkspaceName;
    public static I18n pathNotFound;
    public static I18n pathNotFoundRelativeTo;
    public static I18n pathCannotHaveSameNameSiblingIndex;
    public static I18n permissionDenied;
    public static I18n repositoryMustBeConfigured;
    public static I18n sourceInUse;
    public static I18n repositoryDoesNotExist;
    public static I18n fileDoesNotExist;
    public static I18n failedToReadPropertiesFromManifest;
    public static I18n failedToReadPropertyFromManifest;
    public static I18n errorLoadingNodeTypeDefintions;
    public static I18n unableToFindNodeTypeDefinitionsOnClasspathOrFileOrUrl;
    public static I18n unableToFindResourceOnClasspathOrFileOrUrl;
    public static I18n fileMustExistAndBeReadable;
    public static I18n invalidJcrUrl;

    public static I18n rootNodeHasNoParent;
    public static I18n rootNodeIsNotProperty;
    public static I18n childNodeAlreadyExists;

    public static I18n noNamespaceWithPrefix;
    public static I18n noNamespaceWithUri;
    public static I18n unableToChangeTheDefaultNamespace;
    public static I18n unableToRegisterReservedNamespacePrefix;
    public static I18n unableToRegisterReservedNamespaceUri;
    public static I18n unableToRegisterNamespaceUsingXmlPrefix;
    public static I18n unableToRegisterNamespaceWithInvalidPrefix;
    public static I18n errorRegisteringPersistentNamespace;
    public static I18n unableToUnregisterReservedNamespacePrefix;
    public static I18n unableToUnregisterReservedNamespaceUri;
    public static I18n unableToUnregisterPrefixForNamespaceThatIsNotRegistered;

    public static I18n errorWhileInitializingTheNamespaceRegistry;
    public static I18n errorCleaningUpLocks;
    public static I18n cleaningUpLocks;
    public static I18n cleanedUpLocks;
    public static I18n invalidRelativePath;
    public static I18n invalidAbsolutePath;
    public static I18n invalidPathParameter;
    public static I18n invalidNamePattern;
    public static I18n invalidNodeTypeNameParameter;
    public static I18n noPrimaryItemNameDefinedOnPrimaryType;
    public static I18n primaryItemNameForPrimaryTypeIsNotValid;
    public static I18n primaryItemDoesNotExist;
    public static I18n itemNotFoundWithUuid;
    public static I18n itemAlreadyExistsWithUuid;
    public static I18n itemNotFoundAtPath;
    public static I18n itemNotFoundAtPathRelativeToReferenceNode;
    public static I18n identifierPathContainedUnsupportedIdentifierFormat;
    public static I18n identifierPathNeverReferencesProperty;
    public static I18n propertyNotFoundOnNode;
    public static I18n propertyNotFoundAtPathRelativeToReferenceNode;
    public static I18n nodeNotFoundAtPathRelativeToReferenceNode;
    public static I18n childNotFoundUnderNode;
    public static I18n errorWhileFindingNodeWithUuid;
    public static I18n errorWhileFindingNodeWithPath;
    public static I18n nodeDefinitionCouldNotBeDeterminedForNode;
    public static I18n noSnsDefinitionForNode;
    public static I18n missingNodeTypeForExistingNode;
    public static I18n unableToCreateNodeWithInternalPrimaryType;
    public static I18n unableToCreateNodeWithPrimaryTypeThatDoesNotExist;
    public static I18n unableToCreateNodeWithNoDefaultPrimaryTypeOnChildNodeDefinition;
    public static I18n unableToSaveNodeThatWasCreatedSincePreviousSave;
    public static I18n unableToSetMultiValuedPropertyUsingSingleValue;
    public static I18n unableToSetSingleValuedPropertyUsingMultipleValues;
    public static I18n invalidMethodForSingleValuedProperty;
    public static I18n invalidMethodForMultiValuedProperty;
    public static I18n unableToRefreshBranchBecauseChangesDependOnChangesToNodesOutsideOfBranch;
    public static I18n unableToSaveBranchBecauseChangesDependOnChangesToNodesOutsideOfBranch;
    public static I18n allPropertyValuesMustHaveSameType;
    public static I18n cannotRemoveNodeFromClone;
    public static I18n cannotRemoveNodeFromCloneDueToChangesInSession;
    public static I18n constraintViolatedOnReference;
    public static I18n unableToBindToJndi;
    public static I18n invalidOptionProvided;
    public static I18n noOptionValueProvided;

    public static I18n cannotRemoveRootNode;
    public static I18n cannotRemoveParentNodeOfTarget;
    public static I18n invalidPropertyType;

    public static I18n unableToRemoveRootNode;
    public static I18n unableToMoveNodeToBeChildOfDecendent;
    public static I18n nodeHasAlreadyBeenRemovedFromThisSession;

    public static I18n typeNotFound;
    public static I18n supertypeNotFound;
    public static I18n errorImportingNodeTypeContent;
    public static I18n nodeTypesNotFoundInXml;

    public static I18n failedToQueryForDerivedContent;

    public static I18n systemSourceNameOptionValueDoesNotReferenceExistingSource;
    public static I18n systemSourceNameOptionValueDoesNotReferenceValidWorkspace;
    public static I18n systemSourceNameOptionValueIsNotFormattedCorrectly;

    public static I18n searchIndexDirectoryOptionSpecifiesFileNotDirectory;
    public static I18n searchIndexDirectoryOptionSpecifiesDirectoryThatCannotBeRead;
    public static I18n searchIndexDirectoryOptionSpecifiesDirectoryThatCannotBeWrittenTo;
    public static I18n searchIndexDirectoryOptionSpecifiesDirectoryThatCannotBeCreated;
    public static I18n errorUpdatingQueryIndexes;

    // Used in AbstractJcrNode#getAncestor
    public static I18n noNegativeDepth;
    public static I18n tooDeep;

    public static I18n SPEC_NAME_DESC;

    // New implementation
    public static I18n errorObtainingWorkspaceNames;
    public static I18n errorObtainingDefaultWorkspaceName;
    public static I18n workspaceNameIsInvalid;
    public static I18n errorVerifyingWorkspaceName;

    // Query-related messages
    public static I18n notStoredQuery;
    public static I18n invalidQueryLanguage;
    public static I18n queryCannotBeParsedUsingLanguage;
    public static I18n queryInLanguageIsNotValid;
    public static I18n queryIsDisabledInRepository;
    public static I18n queryResultsDoNotIncludeScore;
    public static I18n queryResultsDoNotIncludeColumn;
    public static I18n selectorNotUsedInQuery;
    public static I18n selectorUsedInEquiJoinCriteriaDoesNotExistInQuery;
    public static I18n multipleSelectorsAppearInQueryRequireSpecifyingSelectorName;
    public static I18n equiJoinWithOneJcrPathPseudoColumnIsInvalid;

    // Type registration messages
    public static I18n invalidNodeTypeName;
    public static I18n badNodeTypeName;
    public static I18n noSuchNodeType;
    public static I18n nodeTypeAlreadyExists;
    public static I18n invalidPrimaryTypeName;
    public static I18n invalidSupertypeName;
    public static I18n supertypesConflict;
    public static I18n ambiguousPrimaryItemName;
    public static I18n invalidPrimaryItemName;
    public static I18n autocreatedNodesNeedDefaults;
    public static I18n residualDefinitionsCannotBeMandatory;
    public static I18n cannotOverrideProtectedDefinition;
    public static I18n cannotMakeMandatoryDefinitionOptional;
    public static I18n constraintsChangedInSubtype;
    public static I18n cannotRedefineProperty;
    public static I18n autocreatedPropertyNeedsDefault;
    public static I18n singleValuedPropertyNeedsSingleValuedDefault;
    public static I18n couldNotFindDefinitionOfRequiredPrimaryType;
    public static I18n cannotRedefineChildNodeWithIncompatibleDefinition;
    public static I18n cannotRemoveItemWithProtectedDefinition;

    public static I18n noDefinition;
    public static I18n noSnsDefinition;
    public static I18n missingMandatoryItem;

    public static I18n allNodeTypeTemplatesMustComeFromSameSession;

    public static I18n nodeNotReferenceable;
    public static I18n nodeNotReferenceableUuid;
    public static I18n noPendingChangesAllowed;
    public static I18n noPendingChangesAllowedForNode;

    public static I18n cannotUnregisterSupertype;
    public static I18n cannotUnregisterRequiredPrimaryType;
    public static I18n cannotUnregisterDefaultPrimaryType;
    public static I18n cannotUnregisterInUseType;

    public static I18n cannotAddMixin;
    public static I18n invalidMixinTypeForNode;
    public static I18n notOrderable;
    public static I18n cannotUseMixinTypeAsPrimaryType;
    public static I18n primaryTypeCannotBeAbstract;
    public static I18n setPrimaryTypeNotSupported;

    public static I18n errorReadingNodeTypesFromRemote;
    public static I18n problemReadingNodeTypesFromRemote;
    public static I18n errorSynchronizingNodeTypes;

    // Lock messages
    public static I18n nodeNotLockable;
    public static I18n cannotRemoveLockToken;
    public static I18n alreadyLocked;
    public static I18n parentAlreadyLocked;
    public static I18n notLocked;
    public static I18n lockTokenNotHeld;
    public static I18n lockTokenAlreadyHeld;
    public static I18n invalidLockToken;
    public static I18n uuidRequiredForLock;

    // JcrObservationManager messages
    public static I18n cannotCreateUuid;
    public static I18n cannotPerformNodeTypeCheck;
    public static I18n sessionIsNotActive;

    // Versioning messages
    public static I18n nodeIsCheckedIn;
    public static I18n cannotRemoveFromProtectedNode;
    public static I18n cannotRemoveVersion;
    public static I18n pendingMergeConflicts;
    public static I18n invalidVersion;
    public static I18n invalidVersionLabel;
    public static I18n invalidVersionName;
    public static I18n versionLabelAlreadyExists;
    public static I18n requiresVersionable;
    public static I18n cannotRestoreRootVersion;
    public static I18n cannotCheckinNodeWithAbortProperty;
    public static I18n cannotCheckinNodeWithAbortChildNode;
    public static I18n noExistingVersionForRestore;
    public static I18n versionNotInMergeFailed;
    public static I18n unrootedVersionsInRestore;
    public static I18n repairedVersionStorage;

    static {
        try {
            I18n.initialize(JcrI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }
}
