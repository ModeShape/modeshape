package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.List;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;

/**
 * Helper class to compare various manifestions of node type definitions. Ideally, this should be converted to a Hamcrest matcher
 * at some point.
 */
public class NodeTypeAssertion {

    @SuppressWarnings( "unchecked" )
    public static void compareTemplateToNodeType( NodeTypeTemplate template,
                                                  NodeType nodeType ) {
        compareNodeTypeDefinitions(template, nodeType);

        PropertyDefinition[] propertyDefs = nodeType.getDeclaredPropertyDefinitions();
        List<PropertyDefinitionTemplate> propertyTemplates = template.getPropertyDefinitionTemplates();
        comparePropertyDefinitions(propertyDefs, propertyTemplates);

        NodeDefinition[] childNodeDefs = nodeType.getDeclaredChildNodeDefinitions();
        List<NodeDefinitionTemplate> childNodeTemplates = template.getNodeDefinitionTemplates();
        compareChildNodeDefinitions(childNodeDefs, childNodeTemplates);
    }

    private static void compareNodeTypeDefinitions( NodeTypeDefinition template,
                                                    NodeTypeDefinition nodeType ) {
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.getName(), is(template.getName()));
        assertThat(nodeType.getDeclaredSupertypeNames().length, is(template.getDeclaredSupertypeNames().length));
        for (int i = 0; i < template.getDeclaredSupertypeNames().length; i++) {
            assertThat(template.getDeclaredSupertypeNames()[i], is(nodeType.getDeclaredSupertypeNames()[i]));
        }
        assertThat(template.isMixin(), is(nodeType.isMixin()));
        assertThat(template.hasOrderableChildNodes(), is(nodeType.hasOrderableChildNodes()));
        assertThat(template.isQueryable(), is(nodeType.isQueryable()));
        assertThat(template.isAbstract(), is(nodeType.isAbstract()));
    }

    private static void comparePropertyDefinitions( PropertyDefinition[] propertyDefs,
                                                    List<PropertyDefinitionTemplate> propertyTemplates ) {
        assertThat(propertyDefs.length, is(propertyTemplates.size()));
        for (PropertyDefinitionTemplate pt : propertyTemplates) {
            JcrPropertyDefinitionTemplate propertyTemplate = (JcrPropertyDefinitionTemplate)pt;

            PropertyDefinition matchingDefinition = null;
            for (int i = 0; i < propertyDefs.length; i++) {
                PropertyDefinition pd = propertyDefs[i];

                String ptName = propertyTemplate.getName() == null ? JcrNodeType.RESIDUAL_ITEM_NAME : propertyTemplate.getName();
                if (pd.getName().equals(ptName) && pd.getRequiredType() == propertyTemplate.getRequiredType()
                    && pd.isMultiple() == propertyTemplate.isMultiple()) {
                    matchingDefinition = pd;
                    break;
                }
            }

            comparePropertyTemplateToPropertyDefinition(propertyTemplate, (JcrPropertyDefinition)matchingDefinition);
        }
    }

    private static void compareChildNodeDefinitions( NodeDefinition[] childNodeDefs,
                                                     List<NodeDefinitionTemplate> childNodeTemplates ) {
        assertThat(childNodeDefs.length, is(childNodeTemplates.size()));
        for (NodeDefinitionTemplate nt : childNodeTemplates) {
            JcrNodeDefinitionTemplate childNodeTemplate = (JcrNodeDefinitionTemplate)nt;

            NodeDefinition matchingDefinition = null;
            for (int i = 0; i < childNodeDefs.length; i++) {
                NodeDefinition nd = childNodeDefs[i];

                String ntName = childNodeTemplate.getName() == null ? JcrNodeType.RESIDUAL_ITEM_NAME : childNodeTemplate.getName();
                if (nd.getName().equals(ntName) && nd.allowsSameNameSiblings() == childNodeTemplate.allowsSameNameSiblings()) {
                    boolean onlyBaseTypeRequired = nd.getRequiredPrimaryTypes().length == 0
                                                   || (nd.getRequiredPrimaryTypes().length == 1 && nd.getRequiredPrimaryTypes()[0].getName()
                                                                                                                                  .equals("nt:base"));
                    boolean onlyHasBase = childNodeTemplate.getRequiredPrimaryTypeNames().length == 0
                                          || (childNodeTemplate.getRequiredPrimaryTypeNames().length == 1 && childNodeTemplate.getRequiredPrimaryTypeNames()[0].equals("nt:base"));

                    if (onlyBaseTypeRequired && onlyHasBase) {
                        // We have a match
                        matchingDefinition = nd;
                        break;
                    }

                    if (nd.getRequiredPrimaryTypes().length != childNodeTemplate.getRequiredPrimaryTypeNames().length) continue;

                    boolean matchesOnRequiredTypes = true;
                    for (int j = 0; j < nd.getRequiredPrimaryTypes().length; j++) {
                        String ndName = nd.getRequiredPrimaryTypes()[j].getName();
                        String tempName = childNodeTemplate.getRequiredPrimaryTypeNames()[j];
                        if (!ndName.equals(tempName)) {
                            matchesOnRequiredTypes = false;
                            break;
                        }
                    }

                    if (matchesOnRequiredTypes) {
                        matchingDefinition = nd;
                        break;
                    }
                }
            }

            if (matchingDefinition == null) {
                System.out.println(childNodeTemplate);
            }
            compareNodeTemplateToNodeDefinition(childNodeTemplate, (JcrNodeDefinition)matchingDefinition);
        }
    }

    private static void comparePropertyTemplateToPropertyDefinition( JcrPropertyDefinitionTemplate template,
                                                                     JcrPropertyDefinition definition ) {

        assertThat(definition, is(notNullValue()));
        assertThat(definition.getDeclaringNodeType(), is(notNullValue()));
        // Had to match on name to even get to the definition
        // assertThat(template.getName(), is(definition.getName()));

        assertThat(emptyIfNull(template.getValueConstraints()), is(definition.getValueConstraints()));
        assertThat(template.getOnParentVersion(), is(definition.getOnParentVersion()));
        assertThat(template.getRequiredType(), is(definition.getRequiredType()));
        assertThat(template.isAutoCreated(), is(definition.isAutoCreated()));
        assertThat(template.isMandatory(), is(definition.isMandatory()));
        assertThat(template.isMultiple(), is(definition.isMultiple()));
        assertThat(template.isProtected(), is(definition.isProtected()));
        assertThat(template.isFullTextSearchable(), is(definition.isFullTextSearchable()));
        assertThat(template.isQueryOrderable(), is(definition.isQueryOrderable()));

        String[] tempOps = template.getAvailableQueryOperators();
        if (tempOps == null) {
            tempOps = new String[] {QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN,
                QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO,
                QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN, QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO,
                QueryObjectModelConstants.JCR_OPERATOR_LIKE, QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO};
        }
        assertThat(tempOps, is(definition.getAvailableQueryOperators()));
    }

    private static void compareNodeTemplateToNodeDefinition( JcrNodeDefinitionTemplate template,
                                                             JcrNodeDefinition definition ) {
        assertThat(definition, is(notNullValue()));
        assertThat(definition.getDeclaringNodeType(), is(notNullValue()));
        // Had to match on name to even get to the definition
        // assertThat(template.getName(), is(definition.getName()));

        assertThat(template.getOnParentVersion(), is(definition.getOnParentVersion()));
        assertThat(template.isAutoCreated(), is(definition.isAutoCreated()));
        assertThat(template.isMandatory(), is(definition.isMandatory()));
        assertThat(template.isProtected(), is(definition.isProtected()));

        assertThat(template.getDefaultPrimaryType(), is(definition.getDefaultPrimaryType()));
        assertThat(template.allowsSameNameSiblings(), is(definition.allowsSameNameSiblings()));

        // assertThat(template.getRequiredPrimaryTypeNames(), is(definition.getRequiredPrimaryTypeNames()));

    }

    private static String[] emptyIfNull( String[] incoming ) {
        if (incoming != null) return incoming;
        return new String[0];
    }

}
