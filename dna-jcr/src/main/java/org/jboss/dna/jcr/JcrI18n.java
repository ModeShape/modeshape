/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
* See the AUTHORS.txt file in the distribution for a full listing of 
* individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import org.jboss.dna.common.i18n.I18n;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
public final class JcrI18n {

    public static I18n cannotConvertValue;
    public static I18n credentialsMustProvideJaasMethod;
    public static I18n credentialsMustReturnAccessControlContext;
    public static I18n credentialsMustReturnLoginContext;
    public static I18n defaultWorkspaceName;
    public static I18n inputStreamConsumed;
    public static I18n nonInputStreamConsumed;
    public static I18n pathNotFound;
    public static I18n pathNotFoundRelativeTo;
    public static I18n permissionDenied;
    public static I18n repositoryMustBeConfigured;
    public static I18n sourceInUse;
    public static I18n repositoryDoesNotExist;
    public static I18n fileDoesNotExist;

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
    public static I18n unableToRemapUriNotRegisteredInNamespaceRegistry;
    public static I18n unableToRemapUriUsingPrefixUsedInNamespaceRegistry;

    public static I18n errorRegisteringNodeTypes;
    public static I18n errorWhileInitializingTheNamespaceRegistry;
    public static I18n invalidRelativePath;
    public static I18n invalidPathParameter;
    public static I18n invalidNamePattern;
    public static I18n invalidNodeTypeNameParameter;
    public static I18n noPrimaryItemNameDefinedOnPrimaryType;
    public static I18n primaryItemNameForPrimaryTypeIsNotValid;
    public static I18n primaryItemDoesNotExist;
    public static I18n itemNotFoundWithUuid;
    public static I18n itemNotFoundAtPath;
    public static I18n itemNotFoundAtPathRelativeToReferenceNode;
    public static I18n propertyNotFoundOnNode;
    public static I18n propertyNotFoundAtPathRelativeToReferenceNode;
    public static I18n nodeNotFoundAtPathRelativeToReferenceNode;
    public static I18n childNotFoundUnderNode;
    public static I18n errorWhileFindingNodeWithUuid;
    public static I18n errorWhileFindingNodeWithPath;
    public static I18n nodeDefinitionCouldNotBeDeterminedForNode;
    public static I18n noSnsDefinitionForNode;
    public static I18n missingNodeTypeForExistingNode;
    public static I18n unableToCreateNodeWithPrimaryTypeThatDoesNotExist;
    public static I18n unableToCreateNodeWithNoDefaultPrimaryTypeOnChildNodeDefinition;
    public static I18n unableToSaveNodeThatWasCreatedSincePreviousSave;
    public static I18n unableToSetMultiValuedPropertyUsingSingleValue;
    public static I18n unableToSetSingleValuedPropertyUsingMultipleValues;
    public static I18n unableToRefreshBranchSinceAtLeastOneNodeMovedToParentOutsideOfBranch;
    public static I18n allPropertyValuesMustHaveSameType;

    public static I18n unableToRemoveRootNode;
    public static I18n unableToMoveNodeToBeChildOfDecendent;
    public static I18n nodeHasAlreadyBeenRemovedFromThisSession;

    public static I18n typeNotFound;
    public static I18n supertypeNotFound;

    // Used in AbstractJcrNode#getAncestor
    public static I18n noNegativeDepth;
    public static I18n tooDeep;

    public static I18n REP_NAME_DESC;
    public static I18n REP_VENDOR_DESC;
    public static I18n SPEC_NAME_DESC;

    // New implementation
    public static I18n errorObtainingWorkspaceNames;
    public static I18n errorObtainingDefaultWorkspaceName;
    public static I18n workspaceNameIsInvalid;
    public static I18n errorVerifyingWorkspaceName;

    // Query-related messages
    public static I18n notStoredQuery;
    public static I18n invalidQueryLanguage;

    // Type registration messages
    public static I18n invalidNodeTypeName;
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

    public static I18n noDefinition;
    public static I18n noSnsDefinition;
    public static I18n missingMandatoryItem;

    public static I18n allNodeTypeTemplatesMustComeFromSameSession;

    public static I18n nodeNotReferenceable;
    public static I18n noPendingChangesAllowed;

    static {
        try {
            I18n.initialize(JcrI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }
}
