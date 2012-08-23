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

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.jcr*</code> packages.
 */
@Immutable
public final class JcrI18n {

    public static I18n initializing;

    public static I18n engineStarting;
    public static I18n engineStarted;
    public static I18n couldNotStartEngine;
    public static I18n engineStopping;
    public static I18n engineStopped;
    public static I18n repositoryCannotBeRestored;
    public static I18n repositoryCannotBeRestartedAfterRestore;
    public static I18n repositoryIsCurrentlyBeingRestored;
    public static I18n repositoryIsBeingRestoredAndCannotBeStarted;
    public static I18n repositoryReferencesNonExistantSource;
    public static I18n indexRebuildingStarted;
    public static I18n indexRebuildingComplete;
    public static I18n indexRebuildingOfWorkspaceStarted;
    public static I18n indexRebuildingOfWorkspaceComplete;
    public static I18n unableToInitializeSystemWorkspace;

    public static I18n cannotConvertValue;
    public static I18n loginFailed;
    public static I18n noPrivilegeToGetLoginContextFromCredentials;
    public static I18n credentialsMustProvideJaasMethod;
    public static I18n mustBeInPrivilegedAction;
    public static I18n loginConfigNotFound;
    public static I18n credentialsMustReturnLoginContext;
    public static I18n usingAnonymousUser;
    public static I18n unknownCredentialsImplementation;
    public static I18n defaultWorkspaceName;
    public static I18n nodeNotFound;
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
    public static I18n errorStartingRepositoryCheckConfiguration;
    public static I18n completedStartingRepository;
    public static I18n startingAllRepositoriesWasInterrupted;
    public static I18n unableToFindNodeTypeDefinitionsOnClasspathOrFileOrUrl;
    public static I18n unableToFindResourceOnClasspathOrFileOrUrl;
    public static I18n unableToImportInitialContent;
    public static I18n fileMustExistAndBeReadable;
    public static I18n existsAndMustBeWritableDirectory;
    public static I18n problemInitializingBackupArea;
    public static I18n problemsWritingDocumentToBackup;
    public static I18n problemsWritingBinaryToBackup;
    public static I18n problemsReadingBinaryFromBackup;
    public static I18n problemsGettingBinaryKeysFromBinaryStore;
    public static I18n problemsRestoringBinaryFromBackup;
    public static I18n interruptedWhilePerformingBackup;
    public static I18n problemObtainingDocumentsToBackup;
    public static I18n backupOperationWasCancelled;
    public static I18n problemsClosingBackupFiles;
    public static I18n invalidJcrUrl;
    public static I18n unableToInitializeAuthenticationProvider;
    public static I18n errorInAuthenticationProvider;
    public static I18n unableToInitializeSequencer;
    public static I18n unableToInitializeTextExtractor;

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
    public static I18n errorRefreshingLocks;
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
    public static I18n cannotSetProtectedPropertyValue;
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
    public static I18n valueMayNotContainNull;
    public static I18n propertyNoLongerSatisfiesConstraints;
    public static I18n propertyNoLongerHasValidDefinition;

    public static I18n cannotRemoveRootNode;
    public static I18n cannotRemoveParentNodeOfTarget;
    public static I18n invalidPropertyType;

    public static I18n rootNodeCannotBeDestinationOfMovedNode;
    public static I18n unableToMoveRootNode;
    public static I18n unableToRemoveRootNode;
    public static I18n unableToRemoveSystemNodes;
    public static I18n unableToModifySystemNodes;
    public static I18n unableToMoveNodeToBeChildOfDecendent;
    public static I18n nodeHasAlreadyBeenRemovedFromThisSession;
    public static I18n unableToShareNodeWithinSubgraph;
    public static I18n unableToShareNodeWithinSameParent;
    public static I18n shareAlreadyExistsWithinParent;
    public static I18n unableToMoveNodeDueToCycle;

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

    public static I18n invalidAliasForComponent;
    public static I18n unableToSetFieldOnInstance;
    public static I18n missingFieldOnInstance;
    public static I18n missingComponentType;

    public static I18n typeMissingWhenRegisteringEngineInJndi;
    public static I18n repositoryNameNotProvidedWhenRegisteringRepositoryInJndi;
    public static I18n invalidRepositoryNameWhenRegisteringRepositoryInJndi;
    public static I18n emptyRepositoryNameProvidedWhenRegisteringRepositoryInJndi;

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
    public static I18n multipleSelectorsAppearInQueryUnableToCallMethod;
    public static I18n equiJoinWithOneJcrPathPseudoColumnIsInvalid;
    public static I18n noSuchVariableInQuery;

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
    public static I18n residualPropertyDefinitionsCannotBeMandatory;
    public static I18n residualPropertyDefinitionsCannotBeAutoCreated;
    public static I18n residualNodeDefinitionsCannotBeMandatory;
    public static I18n residualNodeDefinitionsCannotBeAutoCreated;
    public static I18n cannotOverrideProtectedDefinition;
    public static I18n cannotMakeMandatoryDefinitionOptional;
    public static I18n constraintsChangedInSubtype;
    public static I18n cannotRedefineProperty;
    public static I18n autocreatedPropertyNeedsDefault;
    public static I18n singleValuedPropertyNeedsSingleValuedDefault;
    public static I18n couldNotFindDefinitionOfRequiredPrimaryType;
    public static I18n cannotRedefineChildNodeWithIncompatibleDefinition;
    public static I18n cannotRemoveItemWithProtectedDefinition;
    public static I18n errorCheckingNodeTypeUsage;

    public static I18n noChildNodeDefinition;
    public static I18n noPropertyDefinition;
    public static I18n noSnsDefinition;
    public static I18n missingMandatoryProperty;
    public static I18n missingMandatoryChild;
    public static I18n valueViolatesConstraintsOnDefinition;
    public static I18n valuesViolateConstraintsOnDefinition;
    public static I18n referenceValueViolatesConstraintsOnDefinition;
    public static I18n referenceValuesViolateConstraintsOnDefinition;
    public static I18n weakReferenceValueViolatesConstraintsOnDefinition;
    public static I18n weakReferenceValuesViolateConstraintsOnDefinition;

    public static I18n allNodeTypeTemplatesMustComeFromSameSession;

    public static I18n nodeNotReferenceable;
    public static I18n nodeNotReferenceableUuid;
    public static I18n noPendingChangesAllowed;
    public static I18n noPendingChangesAllowedForNode;
    public static I18n nodeNotInTheSameSession;

    public static I18n cannotUnregisterSupertype;
    public static I18n cannotUnregisterRequiredPrimaryType;
    public static I18n cannotUnregisterDefaultPrimaryType;
    public static I18n cannotUnregisterInUseType;

    public static I18n cannotAddMixin;
    public static I18n invalidMixinTypeForNode;
    public static I18n notOrderable;
    public static I18n cannotUseMixinTypeAsPrimaryType;
    public static I18n unableToChangePrimaryTypeDueToPropertyDefinition;
    public static I18n unableToChangePrimaryTypeDueToParentsChildDefinition;
    public static I18n primaryTypeCannotBeAbstract;
    public static I18n setPrimaryTypeOnRootNodeIsNotSupported;
    public static I18n suppliedNodeTypeIsNotMixinType;
    public static I18n cannotRemoveShareableMixinThatIsShared;

    public static I18n errorReadingNodeTypesFromRemote;
    public static I18n problemReadingNodeTypesFromRemote;
    public static I18n errorSynchronizingNodeTypes;

    public static I18n errorRefreshingNodeTypesFromSystem;
    public static I18n problemRefreshingNodeTypesFromSystem;
    public static I18n errorRefreshingNodeTypes;

    public static I18n errorsParsingNodeTypeDefinitions;
    public static I18n errorsParsingStreamOfNodeTypeDefinitions;
    public static I18n warningsParsingNodeTypeDefinitions;
    public static I18n warningsParsingStreamOfNodeTypeDefinitions;

    // Lock messages
    public static I18n nodeNotLockable;
    public static I18n cannotRemoveLockToken;
    public static I18n nodeIsLocked;
    public static I18n alreadyLocked;
    public static I18n parentAlreadyLocked;
    public static I18n descendantAlreadyLocked;
    public static I18n notLocked;
    public static I18n lockTokenNotHeld;
    public static I18n lockTokenAlreadyHeld;
    public static I18n invalidLockToken;
    public static I18n changedNodeCannotBeLocked;
    public static I18n changedNodeCannotBeUnlocked;
    public static I18n uuidRequiredForLock;

    // JcrObservationManager messages
    public static I18n cannotCreateUuid;
    public static I18n cannotPerformNodeTypeCheck;
    public static I18n sessionIsNotActive;

    // Versioning messages
    public static I18n nodeIsCheckedIn;
    public static I18n cannotCreateChildOnCheckedInNodeSinceOpvOfChildDefinitionIsNotIgnore;
    public static I18n cannotRemoveChildOnCheckedInNodeSinceOpvOfChildDefinitionIsNotIgnore;
    public static I18n cannotRemoveFromProtectedNode;
    public static I18n cannotRemoveVersion;
    public static I18n pendingMergeConflicts;
    public static I18n invalidVersion;
    public static I18n invalidVersionLabel;
    public static I18n invalidVersionName;
    public static I18n versionLabelAlreadyExists;
    public static I18n labeledNodeNotFound;
    public static I18n requiresVersionable;
    public static I18n cannotRestoreRootVersion;
    public static I18n cannotCheckinNodeWithAbortProperty;
    public static I18n cannotCheckinNodeWithAbortChildNode;
    public static I18n noExistingVersionForRestore;
    public static I18n versionNotInMergeFailed;
    public static I18n unrootedVersionsInRestore;
    public static I18n errorDuringCheckinNode;
    public static I18n noVersionHistoryForTransientVersionableNodes;
    public static I18n versionHistoryForNewlyVersionableNodesNotAvailableUntilSave;

    public static I18n creatingWorkspacesIsNotAllowedInRepository;
    public static I18n workspaceHasBeenDeleted;
    public static I18n unableToDestroyPredefinedWorkspaceInRepository;
    public static I18n unableToDestroyDefaultWorkspaceInRepository;
    public static I18n unableToDestroySystemWorkspaceInRepository;
    public static I18n workspaceNotFound;
    public static I18n unableToRestoreAtAbsPathNodeAlreadyExists;

    public static I18n unableToFindRepositoryConfigurationSchema;
    public static I18n unableToLoadRepositoryConfigurationSchema;
    public static I18n errorsInRepositoryConfiguration;

    // Engine
    public static I18n engineIsNotRunning;
    public static I18n engineAtJndiLocationIsNotRunning;
    public static I18n repositoryConfigurationIsNotValid;
    public static I18n startingOfRepositoryWasCancelled;
    public static I18n startingOfRepositoryWasInterrupted;
    public static I18n failedToShutdownDeployedRepository;
    public static I18n repositoryIsAlreadyDeployed;
    public static I18n repositoryIsNotRunningOrHasBeenShutDown;
    public static I18n repositoryIsNotRunningOrHasBeenShutDownInEngineAtJndiLocation;
    public static I18n repositoryNotFoundInEngineAtJndiLocation;
    public static I18n repositoriesNotFoundInEngineAtJndiLocation;
    public static I18n potentialClasspathErrorAtJndiLocation;
    public static I18n errorStartingRepository;
    public static I18n storageRelatedConfigurationChangesWillTakeEffectAfterShutdown;
    public static I18n errorShuttingDownJcrRepositoryFactory;
    public static I18n repositoryNameDoesNotMatchConfigurationName;
    public static I18n errorWhileShuttingDownRepositoryInJndi;
    public static I18n errorWhileShuttingDownEngineInJndi;
    public static I18n nodeModifiedBySessionWasRemovedByAnotherSession;
    public static I18n nodeCreatedBySessionUsedExistingKey;

    public static I18n failedWhileRollingBackDestroyToRuntimeError;
    public static I18n unexpectedException;

    public static I18n configurationError;
    public static I18n configurationWarning;

    public static I18n errorDuringGarbageCollection;
    public static I18n errorMarkingBinaryValuesUnused;
    public static I18n errorMarkingBinaryValuesUsed;
    public static I18n errorStoringMimeType;
    public static I18n errorStoringExtractedText;
    public static I18n errorReadingExtractedText;

    public static I18n unableToReadTemporaryDirectory;
    public static I18n unableToWriteTemporaryDirectory;
    public static I18n unableToPersistBinaryValueToFileSystemStore;
    public static I18n unableToDeleteTemporaryFile;
    public static I18n unableToFindBinaryValue;
    public static I18n tempDirectorySystemPropertyMustBeSet;
    public static I18n tempDirectoryLocation;
    public static I18n errorReadingBinaryValue;
    public static I18n errorStoringBinaryValue;
    public static I18n errorLockingBinaryValue;

    public static I18n errorKillingRepository;
    public static I18n errorKillingEngine;

    // Lucene query engine ...
    public static I18n errorRetrievingExtractedTextFile;
    public static I18n errorExtractingTextFromBinary;
    public static I18n errorAddingBinaryTextToIndex;
    public static I18n missingQueryVariableValue;
    public static I18n errorClosingLuceneReaderForIndex;
    public static I18n ignoringIndexingProperty;
    public static I18n locationForIndexesIsNotDirectory;
    public static I18n locationForIndexesCannotBeRead;
    public static I18n locationForIndexesCannotBeWritten;
    public static I18n errorWhileCommittingIndexChanges;
    public static I18n errorWhileRollingBackIndexChanges;
    public static I18n missingVariableValue;

    static {
        try {
            I18n.initialize(JcrI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }
}
