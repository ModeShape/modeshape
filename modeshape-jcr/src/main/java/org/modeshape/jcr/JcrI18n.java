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
    public static I18n repositoryCannotBeStartedWithoutTransactionalSupport;
    public static I18n workspaceCacheShouldNotBeTransactional;
    public static I18n workspaceCacheShouldUseEviction;
    public static I18n workspaceCacheShouldNotUseLoaders;
    public static I18n workspaceCacheShouldBeEmbedded;
    public static I18n repositoryReferencesNonExistantSource;
    public static I18n indexRebuildingStarted;
    public static I18n indexRebuildingComplete;
    public static I18n indexRebuildingOfWorkspaceStarted;
    public static I18n indexRebuildingOfWorkspaceComplete;
    public static I18n unableToInitializeSystemWorkspace;
    public static I18n repositoryWasNeverInitializedAfterMinutes;
    public static I18n repositoryWasInitializedByOtherProcess;
    public static I18n repositoryWasNeverUpgradedAfterMinutes;
    public static I18n failureDuringUpgradeOperation;
    public static I18n errorShuttingDownIndexProvider;
    public static I18n indexProviderMissingPlanner;
    public static I18n errorNotifyingNodeTypesListener;
    public static I18n errorIndexing;
    public static I18n cannotReindexJournalNotEnabled;
    public static I18n warnIncrementalIndexingJournalNotEnabled;
    public static I18n warnIncrementalIndexingJournalNotStarted;
    public static I18n warnIncrementalIndexingNotSupported;

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
    public static I18n cannotCopySubgraphIntoRoot;
    public static I18n cannotCloneSubgraphIntoRoot;
    public static I18n cannotCopyOrCloneReferenceOutsideGraph;
    public static I18n cannotCopyOrCloneCorruptReference;
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
    public static I18n unableToInitializeConnector;
    public static I18n unableToInitializeIndexProvider;
    public static I18n requiredFieldNotSetInConnector;
    public static I18n fileConnectorCannotWriteToDirectory;
    public static I18n fileConnectorTopLevelDirectoryMissingOrCannotBeRead;
    public static I18n fileConnectorNodeIdentifierIsNotWithinScopeOfConnector;
    public static I18n fileConnectorIsReadOnly;
    public static I18n fileConnectorCannotStoreFileThatIsExcluded;
    public static I18n fileConnectorNamespaceIgnored;
    public static I18n couldNotStoreProperties;
    public static I18n couldNotStoreProperty;
    public static I18n couldNotGetMimeType;
    public static I18n connectorIsReadOnly;

    public static I18n indexProviderAlreadyExists;
    public static I18n indexProviderDoesNotExist;
    public static I18n indexAlreadyExists;
    public static I18n indexDoesNotExist;
    public static I18n indexMustHaveName;
    public static I18n indexMustHaveProviderName;
    public static I18n invalidIndexDefinitions;
    public static I18n unableToCreateUniqueIndexForColumn;
    public static I18n unableToCreateEnumeratedIndexForColumn;
    public static I18n nodeTypeIndexMustHaveOneColumn;
    public static I18n errorRefreshingIndexDefinitions;
    public static I18n errorNotifyingProviderOfInitialIndexDefinitions;
    public static I18n errorNotifyingProviderOfIndexChanges;

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
    public static I18n unableToUnregisterPrefixForNamespaceUsedByNodeType;

    public static I18n errorWhileInitializingTheNamespaceRegistry;
    public static I18n errorCleaningUpLocks;
    public static I18n errorRefreshingLocks;
    public static I18n lockNotFound;
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
    public static I18n unableToAddChildUnderParent;
    public static I18n childNameDoesNotSatisfyParentChildNodeDefinition;
    public static I18n childPrimaryTypeDoesNotSatisfyParentChildNodeDefinition;
    public static I18n parentChildNodeDefinitionDoesNotAllowSameNameSiblings;
    public static I18n parentChildNodeDefinitionIsProtected;
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
    public static I18n indexOutsidePropertyValuesBoundaries;
    public static I18n unableToRefreshBranchBecauseChangesDependOnChangesToNodesOutsideOfBranch;
    public static I18n unableToSaveBranchBecauseChangesDependOnChangesToNodesOutsideOfBranch;
    public static I18n unableToConvertPropertyValueToType;
    public static I18n unableToConvertPropertyValueAtIndexToType;
    public static I18n allPropertyValuesMustHaveSameType;
    public static I18n cannotRemoveNodeFromClone;
    public static I18n cannotRemoveNodeFromCloneDueToChangesInSession;
    public static I18n constraintViolatedOnReference;
    public static I18n unableToBindToJndi;
    public static I18n jndiReadOnly;
    public static I18n invalidOptionProvided;
    public static I18n noOptionValueProvided;
    public static I18n valueMayNotContainNull;
    public static I18n propertyNoLongerSatisfiesConstraints;
    public static I18n propertyNoLongerHasValidDefinition;
    public static I18n propertyIsProtected;
    public static I18n operationNotSupportedForUnorderedCollections;
    public static I18n invalidUnorderedCollectionType;
    

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
    public static I18n unableToMoveSourceContainExternalNodes;
    public static I18n unableToMoveTargetContainExternalNodes;
    public static I18n unableToMoveSourceTargetMismatch;
    public static I18n unableToMoveProjection;
    public static I18n unableToCopySourceTargetMismatch;
    public static I18n unableToCopySourceNotExternal;
    public static I18n unableToCloneSameWsContainsExternalNode;
    public static I18n unableToCloneExternalNodesRequireRoot;
    public static I18n aclsOnExternalNodesNotAllowed;

    public static I18n typeNotFound;
    public static I18n supertypeNotFound;
    public static I18n errorImportingNodeTypeContent;
    public static I18n errorDuringInitialImport;
    public static I18n nodeTypesNotFoundInXml;

    public static I18n invalidGarbageCollectionInitialTime;

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
    public static I18n repositoryConfigurationContainsDeprecatedField;

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
    public static I18n errorUpdatingWorkspaceNames;
    public static I18n errorUpdatingRepositoryMetadata;
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
    public static I18n multipleCallsToGetRowsOrNodesIsNotAllowed;
    public static I18n equiJoinWithOneJcrPathPseudoColumnIsInvalid;
    public static I18n equiJoinWithOneNodeIdPseudoColumnIsInvalid;
    public static I18n noSuchVariableInQuery;
    public static I18n setQueryContainsResultSetsWithDifferentColumns;
    public static I18n setQueryContainsResultSetsWithDifferentFullTextSearch;
    public static I18n setQueryContainsResultSetsWithDifferentNumberOfColumns;
    public static I18n problemsWithQuery;

    // Type registration messages
    public static I18n invalidNodeTypeName;
    public static I18n badNodeTypeName;
    public static I18n noSuchNodeType;
    public static I18n nodeTypeAlreadyExists;
    public static I18n invalidPrimaryTypeName;
    public static I18n invalidMixinSupertype;
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
    public static I18n cannotRemoveMixinNoPropertyDefinition;
    public static I18n cannotRemoveMixinNoChildNodeDefinition;


    public static I18n noChildNodeDefinitions;
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

    public static I18n referentialIntegrityException;

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
    public static I18n cannotRemoveUnorderedCollectionMixin;
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
    public static I18n invalidVersionForRestore;
    public static I18n versionNotInMergeFailed;
    public static I18n unrootedVersionsInRestore;
    public static I18n errorDuringCheckinNode;
    public static I18n noVersionHistoryForTransientVersionableNodes;
    public static I18n versionHistoryForNewlyVersionableNodesNotAvailableUntilSave;
    public static I18n versionHistoryNotEmpty;
    public static I18n nodeIsShareable;
    public static I18n cannotLocateBaseVersion;

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
    public static I18n errorRemovingISPNCache;

    public static I18n failedWhileRollingBackDestroyToRuntimeError;
    public static I18n unexpectedException;
    public static I18n errorDeterminingCurrentTransactionAssumingNone;
    public static I18n errorWhileRollingBackActiveTransactionUsingWorkspaceThatIsBeingDeleted;

    public static I18n configurationError;
    public static I18n configurationWarning;

    public static I18n errorDuringGarbageCollection;
    public static I18n errorMarkingBinaryValuesUnused;
    public static I18n errorMarkingBinaryValuesUsed;
    public static I18n errorStoringMimeType;
    public static I18n errorStoringExtractedText;
    public static I18n errorReadingExtractedText;
    public static I18n unableToCreateDirectoryForBinaryStore;

    public static I18n unableToReadTemporaryDirectory;
    public static I18n unableToWriteTemporaryDirectory;
    public static I18n unableToPersistBinaryValueToFileSystemStore;
    public static I18n unableToDeleteTemporaryFile;
    public static I18n unableToFindBinaryValue;
    public static I18n unableToFindBinaryValueInCache;
    public static I18n tempDirectorySystemPropertyMustBeSet;
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

    public static I18n unableToReadMediaTypeRegistry;
    public static I18n unableToInitializeMimeTypeDetector;
    public static I18n noMimeTypeDetectorsFound;

    public static I18n invalidInitialContentValue;
    public static I18n cannotLoadInitialContentFile;
    public static I18n errorWhileReadingInitialContentFile;
    public static I18n errorWhileParsingInitialContentFile;

    public static I18n errorDuringInitialInitialization;

    public static I18n cannotLoadCndFile;
    public static I18n errorReadingCndFile;

    public static I18n invalidUrl;
    public static I18n timeoutWhileShuttingRepositoryDown;
    public static I18n repositoryNotFound;

    public static I18n federationNodeKeyDoesNotBelongToSource;
    public static I18n invalidProjectionPath;
    public static I18n invalidProjectionExpression;
    public static I18n projectedPathPointsTowardsInternalNode;
    public static I18n errorStoringProjection;
    public static I18n errorRemovingProjection;

    public static I18n reindexMissingNoIndexesExist;
    public static I18n noReindex;
    public static I18n reindexAll;
    public static I18n noIndexesExist;

    public static I18n errorCreatingDatabaseTable;
    public static I18n warnExtractedTextTooLarge;

    public static I18n cannotLocateConnectionFactory;
    public static I18n cannotLocateQueue;
    public static I18n unexpectedJMSException;
    public static I18n incorrectJMSMessageType;
    public static I18n unknownIndexName;
    public static I18n cannotReadJMSMessage;
    public static I18n errorWhileShuttingDownListener;
    public static I18n errorWhileStartingUpListener;

    public static I18n enablingDocumentOptimization;
    public static I18n beginChildrenOptimization;
    public static I18n completeChildrenOptimization;
    public static I18n errorDuringChildrenOptimization;

    public static I18n mBeanAlreadyRegistered;
    public static I18n cannotRegisterMBean;
    public static I18n cannotUnRegisterMBean;

    public static I18n upgrade3_6_0Running;
    public static I18n upgrade3_6_0CannotUpdateNodeTypes;
    public static I18n upgrade3_6_0CannotUpdateLocks;
    public static I18n upgrade4_0_0_Alpha1_Running;
    public static I18n upgrade4_0_0_Alpha1_Failed;
    public static I18n upgrade4_0_0_Beta3_Running;
    public static I18n upgrade4_0_0_Beta3_Failed;

    public static I18n cannotStartJournal;
    public static I18n cannotStopJournal;
    public static I18n journalHasNotCompletedReconciliation;

    public static I18n indexProviderNameRequired;
    public static I18n indexProviderNameMustMatchProvider;
    public static I18n indexDefinitionIsInvalid;
    public static I18n indexMustHaveOneColumnOfSpecificType;
    public static I18n localIndexProviderMustHaveDirectory;
    public static I18n localIndexProviderDirectoryMustBeReadable;
    public static I18n localIndexProviderDirectoryMustBeWritable;
    public static I18n localIndexProviderDoesNotSupportTextIndexes;
    public static I18n localIndexProviderDoesNotSupportMultiColumnIndexes;

    private JcrI18n() {
    }

    static {
        try {
            I18n.initialize(JcrI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(err);
        }
    }
}
