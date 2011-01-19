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
package org.modeshape.repository;

import java.util.Locale;
import java.util.Set;
import net.jcip.annotations.Immutable;
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
    public static I18n errorInRepositoryWhileSequencingNode;
    public static I18n errorFindingSequencersToRunAgainstNode;
    public static I18n errorInRepositoryWhileFindingSequencersToRunAgainstNode;
    public static I18n executionContextHasBeenClosed;
    public static I18n unableToFindPropertyForSequencing;

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

    static {
        try {
            I18n.initialize(RepositoryI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(RepositoryI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(RepositoryI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(RepositoryI18n.class, locale);
    }
}
