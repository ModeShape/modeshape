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
 * The internationalized string constants for the <code>org.modeshape.repository*</code> packages.
 */
@Immutable
public final class RepositoryI18n {

    // Configuration
    public static I18n errorCreatingInstanceOfClass;
    public static I18n errorCreatingInstanceOfClassUsingClassLoaders;
    public static I18n errorSettingJavaBeanPropertyOnInstanceOfClass;
    public static I18n pathExpressionIsInvalidOnSequencer;
    public static I18n unableToUseGarbageCollectionIntervalValue;

    // Services and Repository
    public static I18n invalidStateString;
    public static I18n serviceShutdowAndMayNotBeStarted;
    public static I18n serviceShutdowAndMayNotBePaused;
    public static I18n serviceNotShutdowAndMayNotBeTerminated;
    public static I18n unableToFindRepositoryInJndi;
    public static I18n unableToRegisterRepositoryInJndi;
    public static I18n unableToUnregisterRepositoryInJndi;
    public static I18n unableToRemoveRepository;
    public static I18n unableToFindRepositoryWithName;
    public static I18n errorProcessingEvents;
    public static I18n errorFindingPropertyNameInPropertyAddedEvent;
    public static I18n errorFindingPropertyNameInPropertyChangedEvent;
    public static I18n errorFindingPropertyNameInPropertyRemovedEvent;

    // Repository service ...
    public static I18n repositoryServiceName;
    public static I18n errorCollectingGarbageInSource;

    // Clustering service ...
    public static I18n clusteringServiceName;
    public static I18n clusteringConfigurationRequiresClusterName;
    public static I18n unableToRegisterObserverOnUnstartedClusteringService;
    public static I18n unableToUnregisterObserverOnUnstartedClusteringService;
    public static I18n unableToNotifyObserversOnUnstartedClusteringService;

    // Sequencing
    public static I18n sequencingServiceName;
    public static I18n unableToChangeExecutionContextWhileRunning;
    public static I18n unableToStartSequencingServiceWithoutExecutionContext;
    public static I18n errorWhileSequencingNode;
    public static I18n errorWhileSequencingNodeIntoWorkspace;
    public static I18n sequencersMayNotChangeThePrimaryTypeOfTheSelectedNode;
    public static I18n problemsWhileSequencingNode;
    public static I18n errorInRepositoryWhileSequencingNode;
    public static I18n errorFindingSequencersToRunAgainstNode;
    public static I18n errorInRepositoryWhileFindingSequencersToRunAgainstNode;
    public static I18n executionContextHasBeenClosed;
    public static I18n unableToFindPropertyForSequencing;
    public static I18n atLeastOneSequencerPathExpressionMustBeSpecified;
    public static I18n shutdownWhileSequencing;

    // Properties
    public static I18n errorReadingPropertiesFromContainerNode;
    public static I18n requiredPropertyOnNodeWasExpectedToBeStringValue;
    public static I18n optionalPropertyOnNodeWasExpectedToBeStringValue;
    public static I18n requiredPropertyOnNodeWasExpectedToBeStringArrayValue;
    public static I18n optionalPropertyOnNodeWasExpectedToBeStringArrayValue;
    public static I18n requiredPropertyOnNodeCouldNotBeRead;
    public static I18n optionalPropertyOnNodeCouldNotBeRead;
    public static I18n requiredPropertyIsMissingFromNode;
    public static I18n errorGettingRequiredPropertyFromNode;
    public static I18n errorGettingOptionalPropertyFromNode;
    public static I18n errorClosingBinaryStreamForPropertyFromNode;
    public static I18n requiredNodeDoesNotExistRelativeToNode;
    public static I18n errorGettingNodeRelativeToNode;
    public static I18n unknownPropertyValueType;
    public static I18n invalidArgumentExceptionWhileSettingProperty;
    public static I18n illegalAccessExceptionWhileSettingProperty;
    public static I18n invocationTargetExceptionWhileSettingProperty;
    public static I18n securityExceptionWhileSettingProperty;

    // Path expressions
    public static I18n pathExpressionIsInvalid;
    public static I18n pathExpressionMayNotBeBlank;
    public static I18n pathExpressionHasInvalidSelect;
    public static I18n pathExpressionHasInvalidMatch;

    // Observation
    public static I18n errorUnregisteringWorkspaceListenerWhileShuttingDownObservationService;

    // General
    public static I18n invalidRepositoryNodePath;
    public static I18n unableToLoadClass;
    public static I18n unableToLoadClassUsingClasspath;
    public static I18n unableToInstantiateClassUsingClasspath;
    public static I18n unableToAccessClassUsingClasspath;

    // Repository
    public static I18n errorStartingRepositoryService;

    // Engine
    public static I18n engineIsNotRunning;
    public static I18n errorsPreventStarting;
    public static I18n warningsWhileStarting;
    public static I18n errorVerifyingConfiguration;

    // MimeTypes
    public static I18n unableToLoadMimeTypeDetector;

    // Text extraction
    public static I18n shutdownWhileExtractingText;

    private RepositoryI18n() {
    }

    static {
        try {
            I18n.initialize(RepositoryI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 2 LINES
            System.err.println(err);
        }
    }
}
