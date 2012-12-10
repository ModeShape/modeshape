/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic.internal.schema;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.infinispan.schematic.SchemaLibrary.ProblemType;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;
import org.infinispan.schematic.document.JsonSchema;
import org.infinispan.schematic.document.JsonSchema.Type;
import org.infinispan.schematic.document.Null;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.internal.document.Paths;

public class JsonSchemaValidatorFactory implements Validator.Factory {

    private CompositeValidator topLevelValidator = new CompositeValidator();
    private final Problems problems;
    private final URI uri;

    protected JsonSchemaValidatorFactory( URI uri,
                                          Problems problems ) {
        this.uri = uri;
        this.problems = problems;
    }

    @Override
    public Validator create( Document schemaDocument,
                             Path pathToDoc ) {
        CompositeValidator validators = new CompositeValidator();
        if (this.topLevelValidator == null) {
            this.topLevelValidator = validators;
        }

        // Dereference any "$ref" value, replacing this schema document with the referenced one ...
        Validator derefValidator = dereference(schemaDocument, pathToDoc, problems);
        if (derefValidator != null) {
            return derefValidator;
        }

        addValidatorsForTypes(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForProperties(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForPatternProperties(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForItems(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForRequired(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForMinimum(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForMaximum(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForMinimumItems(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForMaximumItems(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForUniqueItems(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForPattern(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForMinimumLength(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForMaximumLength(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForEnum(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForDivisibleBy(schemaDocument, pathToDoc, problems, validators);
        addValidatorsForDisallowedTypes(schemaDocument, pathToDoc, problems, validators);

        switch (validators.size()) {
            case 0:
                return null;
            case 1:
                return validators.getFirst();
            default:
                return validators;
        }
    }

    protected Validator dereference( Document schemaDocument,
                                     Path pathToDoc,
                                     Problems problems ) {
        String ref = schemaDocument.getString("$ref");
        if (ref == null) {
            return null;
        }
        if ("#".equals(ref)) {
            return topLevelValidator;
        }
        // Try to resolve the absolute or relative key ...
        // See if this is a relative URI ...
        String resolvedReference = null;
        URI refUri = null;
        try {
            refUri = new URI(ref);
            URI resolvedUri = this.uri.resolve(refUri);
            resolvedReference = resolvedUri.toString();
        } catch (URISyntaxException e) {
            problems.recordWarning(pathToDoc, "The URI of the referenced schema '" + uri + "' is not a valid URI");
        }
        if (uri.equals(resolvedReference)) {
            return topLevelValidator;
        }
        if (!ref.equals(resolvedReference)) {
            // The resolved reference is different than what we just looked up, so look it up ...
            assert resolvedReference != null;
            return new ResolvingValidator(resolvedReference);
        }
        return null;
    }

    protected void addValidatorsForTypes( Document parent,
                                          Path parentPath,
                                          Problems problems,
                                          CompositeValidator validators ) {
        Object value = parent.get("type");
        if (value instanceof String) {
            // Simple type ...
            Type type = JsonSchema.Type.byName((String)value);
            if (type == Type.ANY || type == Type.UNKNOWN) return;
            validators.add(new TypeValidator(type));
        } else if (value instanceof List<?>) {
            // Union type ...
            List<Validator> unionValidators = new ArrayList<Validator>();
            List<?> types = (List<?>)value;
            for (Object obj : types) {
                Validator validator = null;
                if (obj instanceof Document) {
                    // It's either a schema or a reference to a schema ...
                    Document schemaOrRef = (Document)obj;
                    validator = create(schemaOrRef, parentPath.with("type"));
                } else if (obj instanceof String) {
                    Type type = JsonSchema.Type.byName((String)obj);
                    if (type == Type.ANY || type == Type.UNKNOWN) continue;
                    validator = new TypeValidator(type);
                }
                if (validator != null) unionValidators.add(validator);
            }
            if (unionValidators.size() == 1) {
                // Just one validator ...
                validators.add(unionValidators.get(0));
            } else if (unionValidators.size() > 1) {
                // More than one validator, so use a union ...
                validators.add(new UnionValidator(unionValidators));
            }
        }
    }

    protected void addValidatorsForProperties( Document parent,
                                               Path parentPath,
                                               Problems problems,
                                               CompositeValidator validators ) {
        Document properties = parent.getDocument("properties");
        Set<String> propertiesWithSchemas = new HashSet<String>();
        if (properties != null && properties.size() != 0) {
            for (Field field : properties.fields()) {
                String name = field.getName();
                Object value = field.getValue();
                Path path = Paths.path(parentPath, "properties", name);
                if (!(value instanceof Document)) {
                    problems.recordError(path, "Expected a nested object");
                }
                Document propertySchema = (Document)value;
                Validator propertyValidator = create(propertySchema, path);
                if (propertyValidator != null) {
                    validators.add(new PropertyValidator(name, propertyValidator));
                }
                propertiesWithSchemas.add(name);
            }
        }

        // Check the additional properties ...
        boolean additionalPropertiesAllowed = parent.getBoolean("additionalProperties", true);
        if (!additionalPropertiesAllowed) {
            validators.add(new NoOtherAllowedPropertiesValidator(propertiesWithSchemas));
        } else {
            Document additionalSchema = parent.getDocument("additionalProperties");
            if (additionalSchema != null) {
                Path path = parentPath.with("additionalProperties");
                Validator additionalValidator = create(additionalSchema, path);
                if (additionalValidator != null) {
                    validators.add(new AllowedPropertiesValidator(propertiesWithSchemas, additionalValidator));
                }
            }
            // Otherwise, additional properties are allowed so we need to do nothing
        }

    }

    protected void addValidatorsForPatternProperties( Document parent,
                                                      Path parentPath,
                                                      Problems problems,
                                                      CompositeValidator validators ) {
        Document properties = parent.getDocument("patternProperties");
        if (properties != null && properties.size() != 0) {
            for (Field field : properties.fields()) {
                String name = field.getName();
                Object value = field.getValue();
                Path path = Paths.path(parentPath, "patternProperties", name);
                if (!(value instanceof Document)) {
                    problems.recordError(path, "Expected a nested object");
                }
                Document propertySchema = (Document)value;
                try {
                    Pattern namePattern = Pattern.compile(name);
                    Validator propertyValidator = create(propertySchema, path);
                    if (propertyValidator != null) {
                        validators.add(new PatternPropertyValidator(namePattern, propertyValidator));
                    }
                } catch (PatternSyntaxException e) {
                    problems.recordError(path, "Expected the field name to be a regular expression");
                }
            }
        }
    }

    protected void addValidatorsForItems( Document parent,
                                          Path parentPath,
                                          Problems problems,
                                          CompositeValidator validators ) {
        Object items = parent.get("items");
        if (Null.matches(items)) return;

        Path path = parentPath.with("items");
        String requiredName = parentPath.getLast();
        if (requiredName == null) return;

        // Either a schema or an array of schemas ...
        if (items instanceof Document) {
            Document schema = (Document)items;
            Validator validator = create(schema, path);
            if (validator != null) {
                validators.add(new AllItemsMatchValidator(requiredName, validator));
            }
        } else if (items instanceof List<?>) {
            // This is called "tuple typing" in the spec, and can also have 'additionalItems' ...
            List<?> array = (List<?>)items;
            List<Validator> itemValidators = new ArrayList<Validator>(array.size());
            for (Object item : array) {
                if (item instanceof Document) {
                    Validator validator = create((Document)item, path);
                    if (validator != null) {
                        itemValidators.add(validator);
                    }
                }
            }
            // Check the additional items ...
            boolean additionalItemsAllowed = parent.getBoolean("additionalItems", true);
            Validator additionalItemsValidator = null;
            if (!additionalItemsAllowed) {
                additionalItemsValidator = new NotValidValidator();
            } else {
                // additional items are allowed, but check whether there is a schema for the additional items ...
                Document additionalItems = parent.getDocument("additionalItems");
                if (additionalItems != null) {
                    Path additionalItemsPath = parentPath.with("additionalItems");
                    additionalItemsValidator = create(additionalItems, additionalItemsPath);
                }
            }

            if (!itemValidators.isEmpty()) {
                validators.add(new EachItemMatchesValidator(requiredName, itemValidators, additionalItemsValidator,
                                                            additionalItemsAllowed));
            }
        }
    }

    protected void addValidatorsForRequired( Document parent,
                                             Path parentPath,
                                             Problems problems,
                                             CompositeValidator validators ) {
        Boolean required = parent.getBoolean("required", Boolean.FALSE);
        if (required.booleanValue()) {
            String requiredName = parentPath.getLast();
            if (requiredName != null) {
                validators.add(new RequiredValidator(requiredName));
            }
        }
    }

    protected void addValidatorsForMinimum( Document parent,
                                            Path parentPath,
                                            Problems problems,
                                            CompositeValidator validators ) {
        Number minimum = parent.getNumber("minimum");
        if (minimum != null) {
            String requiredName = parentPath.getLast();
            if (requiredName != null) {
                if (parent.getBoolean("exclusiveMinimum", Boolean.FALSE)) {
                    validators.add(new ExclusiveMinimumValidator(requiredName, minimum));
                } else {
                    validators.add(new MinimumValidator(requiredName, minimum));
                }
            }
        }
    }

    protected void addValidatorsForMaximum( Document parent,
                                            Path parentPath,
                                            Problems problems,
                                            CompositeValidator validators ) {
        Double maximum = parent.getDouble("maximum");
        if (maximum != null) {
            String requiredName = parentPath.getLast();
            if (requiredName != null) {
                if (parent.getBoolean("exclusiveMinimum", Boolean.FALSE)) {
                    validators.add(new ExclusiveMaximumValidator(requiredName, maximum));
                } else {
                    validators.add(new MaximumValidator(requiredName, maximum));
                }
            }
        }
    }

    protected void addValidatorsForMinimumItems( Document parent,
                                                 Path parentPath,
                                                 Problems problems,
                                                 CompositeValidator validators ) {
        int minimum = parent.getInteger("minItems", 0);
        if (minimum > 0) {
            String requiredName = parentPath.getLast();
            if (requiredName != null) {
                validators.add(new MinimumItemsValidator(requiredName, minimum));
            }
        }
    }

    protected void addValidatorsForMaximumItems( Document parent,
                                                 Path parentPath,
                                                 Problems problems,
                                                 CompositeValidator validators ) {
        int maximum = parent.getInteger("maxItems", 0);
        if (maximum > 0) {
            String requiredName = parentPath.getLast();
            if (requiredName != null) {
                validators.add(new MaximumItemsValidator(requiredName, maximum));
            }
        }
    }

    protected void addValidatorsForUniqueItems( Document parent,
                                                Path parentPath,
                                                Problems problems,
                                                CompositeValidator validators ) {
        if (parent.getBoolean("uniqueItems", false)) {
            String requiredName = parentPath.getLast();
            if (requiredName != null) {
                validators.add(new UniqueItemsValidator(requiredName));
            }
        }
    }

    protected void addValidatorsForPattern( Document parent,
                                            Path parentPath,
                                            Problems problems,
                                            CompositeValidator validators ) {
        String regex = parent.getString("pattern");
        if (regex != null) {
            String requiredName = parentPath.getLast();
            if (requiredName != null) {
                try {
                    Pattern pattern = Pattern.compile(regex);
                    validators.add(new PatternValidator(requiredName, pattern));
                } catch (PatternSyntaxException e) {
                    problems.recordError(parentPath.with("pattern"),
                                         "The supplied value '" + regex
                                         + "' is expected to be a valid regular expression, but there was an error at position "
                                         + e.getIndex() + ": " + e.getDescription());
                }
            }
        }
    }

    protected void addValidatorsForMinimumLength( Document parent,
                                                  Path parentPath,
                                                  Problems problems,
                                                  CompositeValidator validators ) {
        int minimumLength = parent.getInteger("minimumLength", 0);
        if (minimumLength > 0) {
            String requiredName = parentPath.getLast();
            if (requiredName != null) {
                validators.add(new MinimumLengthValidator(requiredName, minimumLength));
            }
        }
    }

    protected void addValidatorsForMaximumLength( Document parent,
                                                  Path parentPath,
                                                  Problems problems,
                                                  CompositeValidator validators ) {
        int maximumLength = parent.getInteger("maximumLength", 0);
        if (maximumLength > 0) {
            String requiredName = parentPath.getLast();
            if (requiredName != null) {
                validators.add(new MaximumLengthValidator(requiredName, maximumLength));
            }
        }
    }

    protected void addValidatorsForEnum( Document parent,
                                         Path parentPath,
                                         Problems problems,
                                         CompositeValidator validators ) {
        List<?> enumValues = parent.getArray("enum");
        if (enumValues != null && !enumValues.isEmpty()) {
            String requiredName = parentPath.getLast();
            if (requiredName != null) {
                validators.add(new EnumValidator(requiredName, enumValues));
            }
        }
    }

    protected void addValidatorsForDivisibleBy( Document parent,
                                                Path parentPath,
                                                Problems problems,
                                                CompositeValidator validators ) {
        Number denominator = parent.getNumber("divisibleBy", 1);
        if (denominator != null) {
            int denominatorIntValue = denominator.intValue();
            if (denominatorIntValue != 0 && denominatorIntValue != 1) {
                String requiredName = parentPath.getLast();
                if (requiredName != null) {
                    validators.add(new DivisibleByValidator(requiredName, denominator.intValue()));
                }
            }
        }
    }

    protected void addValidatorsForDisallowedTypes( Document parent,
                                                    Path parentPath,
                                                    Problems problems,
                                                    CompositeValidator validators ) {
        Object disallowed = parent.get("disallowed");
        if (Null.matches(disallowed)) return;
        String requiredName = parentPath.getLast();
        if (requiredName != null) {
            EnumSet<Type> disallowedTypes = Type.typesWithNames(disallowed);
            validators.add(new DisallowedTypesValidator(requiredName, disallowedTypes));
        }
    }

    protected class TypeValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final Type type;

        public TypeValidator( Type type ) {
            this.type = type;
            assert this.type != null;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document document,
                              Path pathToDocument,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldValue == null) {
                if (fieldName != null) {
                    fieldValue = document.get(fieldName);
                } else {
                    // We're supposed to check the whole document is the correct type ...
                    fieldValue = document;
                }
            }
            if (fieldValue != null) {
                Type actual = Type.typeFor(fieldValue);
                if (!type.isEquivalent(actual)) {
                    // See if the value is convertable ...
                    Object converted = type.convertValueFrom(fieldValue, actual);
                    Path pathToField = fieldName != null ? pathToDocument.with(fieldName) : pathToDocument;
                    String reason = "Field value for '" + pathToField + "' expected to be of type " + type + " but was of type "
                                    + actual;
                    if (converted != null) {
                        // We could convert the value, so record this as a special error ...
                        problems.recordTypeMismatch(pathToField, reason, actual, fieldValue, type, converted);
                    } else {
                        problems.recordError(pathToField, reason);
                    }
                } else {
                    problems.recordSuccess();
                }
            }
        }

        @Override
        public String toString() {
            return "Type is '" + type + "'";
        }
    }

    protected static interface ValidatorCollection extends Iterable<Validator> {
    }

    protected class UnionValidator implements Validator, ValidatorCollection {
        private static final long serialVersionUID = 1L;
        private final List<Validator> validators;

        public UnionValidator( List<Validator> validators ) {
            this.validators = validators;
            assert this.validators != null && !this.validators.isEmpty();
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document document,
                              Path pathToDocument,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            // Try each validator with a new problems; the first one to return without any problems passes ...
            ValidationResult problemsForMostSuccesses = null;
            int mostSuccesses = -1;
            for (Validator validator : validators) {
                ValidationResult newProblems = new ValidationResult();
                validator.validate(fieldValue, fieldName, document, pathToDocument, newProblems, resolver);
                if (!newProblems.hasErrors()) {
                    problems.recordSuccess();
                    return;
                }
                if (newProblems.successCount() > mostSuccesses) {
                    mostSuccesses = newProblems.successCount();
                    problemsForMostSuccesses = newProblems;
                }
            }
            // All unioned types had problems, but record the problems with the one that had the most successful validations ...
            if (problemsForMostSuccesses != null) problemsForMostSuccesses.recordIn(problems);
        }

        @Override
        public Iterator<Validator> iterator() {
            return validators.iterator();
        }

        @Override
        public String toString() {
            return "Union of " + validators.size() + " validators";
        }
    }

    protected class ResolvingValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String schemaUri;

        public ResolvingValidator( String schemaUri ) {
            this.schemaUri = schemaUri;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document document,
                              Path pathToDocument,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            SchemaDocument resolved = resolver.get(schemaUri, problems);
            if (resolved == null) {
                problems.recordError(pathToDocument.with(fieldName), "Unable to find referenced schema '" + schemaUri + "'");
            } else {
                problems.recordSuccess();
                resolved.getValidator().validate(fieldValue, fieldName, document, pathToDocument, problems, resolver);
            }
        }

        @Override
        public String toString() {
            return "Resolves to schema '" + schemaUri + "'";
        }
    }

    protected static class RequiredValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;

        public RequiredValidator( String propertyName ) {
            this.propertyName = propertyName;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (Null.matches(fieldValue) && fieldName != null) {
                if (pathToParent.size() == 0) {
                    problems.recordError(pathToParent.with(fieldName), "The top-level '" + fieldName + "' field is required");
                } else {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' is required");
                }
            } else {
                problems.recordSuccess();
            }
        }

        @Override
        public String toString() {
            return "required '" + propertyName + "'";
        }
    }

    protected static abstract class NumericValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final Number number;
        private final double value;

        protected NumericValidator( String propertyName,
                                    Number number ) {
            this.propertyName = propertyName;
            this.number = number;
            this.value = number.doubleValue();
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldValue instanceof Number) {
                Number actualNumber = (Number)fieldValue;
                double actualValue = actualNumber.doubleValue();
                if (isValid(value, actualValue)) {
                    problems.recordSuccess();
                } else {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' is '" + actualNumber + "' but must be "
                                                                       + ruleDescription() + " '" + number + "'");
                }
            }
            // otherwise the value is not a number and the minimum doesn't apply
        }

        /**
         * Evaluate whether the actual value and expected value violate the schema rule.
         * 
         * @param expectedValue the expected value
         * @param actualValue the actual value
         * @return true if the value is valid, or false if there is an error
         */
        protected abstract boolean isValid( double expectedValue,
                                            double actualValue );

        protected abstract String ruleDescription();

        @Override
        public String toString() {
            return "'" + propertyName + "' is '" + ruleDescription() + " '" + number + "'";
        }
    }

    protected static class MinimumValidator extends NumericValidator {
        private static final long serialVersionUID = 1L;

        public MinimumValidator( String propertyName,
                                 Number minimum ) {
            super(propertyName, minimum);
        }

        @Override
        protected boolean isValid( double minimum,
                                   double actualValue ) {
            return actualValue >= minimum;
        }

        @Override
        protected String ruleDescription() {
            return "greater than or equal to";
        }
    }

    /**
     * Validation rule that states fails if the actual value is equal to or less than the minimum value.
     */
    protected static class ExclusiveMinimumValidator extends NumericValidator {
        private static final long serialVersionUID = 1L;

        public ExclusiveMinimumValidator( String propertyName,
                                          Number minimum ) {
            super(propertyName, minimum);
        }

        @Override
        protected boolean isValid( double minimum,
                                   double actualValue ) {
            return actualValue > minimum;
        }

        @Override
        protected String ruleDescription() {
            return "greater than";
        }
    }

    protected static class MaximumValidator extends NumericValidator {
        private static final long serialVersionUID = 1L;

        public MaximumValidator( String propertyName,
                                 Number maximum ) {
            super(propertyName, maximum);
        }

        @Override
        protected boolean isValid( double maximum,
                                   double actualValue ) {
            return actualValue <= maximum;
        }

        @Override
        protected String ruleDescription() {
            return "less than or equal to";
        }
    }

    protected static class ExclusiveMaximumValidator extends NumericValidator {
        private static final long serialVersionUID = 1L;

        public ExclusiveMaximumValidator( String propertyName,
                                          Number maximum ) {
            super(propertyName, maximum);
        }

        @Override
        protected boolean isValid( double maximum,
                                   double actualValue ) {
            return actualValue < maximum;
        }

        @Override
        protected String ruleDescription() {
            return "less than";
        }
    }

    protected static class MinimumLengthValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final int minimumLength;

        public MinimumLengthValidator( String propertyName,
                                       int minimumLength ) {
            this.propertyName = propertyName;
            this.minimumLength = minimumLength;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldValue instanceof String || fieldValue instanceof Symbol) {
                String value = fieldValue.toString();
                if (value.length() < minimumLength) {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' had " + value.length()
                                                                       + " characters, but was expected to have at least "
                                                                       + minimumLength);
                } else {
                    problems.recordSuccess();
                }
            }
        }

        @Override
        public String toString() {
            return "'" + propertyName + "' has a minimum length of " + minimumLength;
        }
    }

    protected static class MaximumLengthValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final int maximumLength;

        public MaximumLengthValidator( String propertyName,
                                       int maximumLength ) {
            this.propertyName = propertyName;
            this.maximumLength = maximumLength;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldValue instanceof String || fieldValue instanceof Symbol) {
                String value = fieldValue.toString();
                if (value.length() > maximumLength) {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' had " + value.length()
                                                                       + " characters, but was expected to have no more than "
                                                                       + maximumLength);
                } else {
                    problems.recordSuccess();
                }
            }
        }

        @Override
        public String toString() {
            return "'" + propertyName + "' has a maximum length of " + maximumLength;
        }
    }

    protected static class DivisibleByValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final int denominator;

        public DivisibleByValidator( String propertyName,
                                     int denominator ) {
            this.propertyName = propertyName;
            this.denominator = denominator;
            assert this.denominator != 0;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (Null.matches(fieldValue)) return;
            if (fieldValue instanceof Integer) {
                int value = ((Integer)fieldValue).intValue();
                if (value % denominator != 0) {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' had a value of " + value
                                                                       + " and was not divisible by " + denominator);
                } else {
                    problems.recordSuccess();
                }
            } else if (fieldValue instanceof Long) {
                long value = ((Long)fieldValue).longValue();
                if (value % denominator != 0L) {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' had a value of " + value
                                                                       + " and was not divisible by " + denominator);
                } else {
                    problems.recordSuccess();
                }
            } else if (fieldValue instanceof Short) {
                int value = ((Short)fieldValue).intValue();
                if (value % denominator != 0) {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' had a value of " + value
                                                                       + " and was not divisible by " + denominator);
                } else {
                    problems.recordSuccess();
                }
            } else if (fieldValue instanceof Float) {
                float value = ((Float)fieldValue).floatValue();
                if (value % denominator != 0.0f) {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' had a value of " + value
                                                                       + " and was not divisible by " + denominator);
                } else {
                    problems.recordSuccess();
                }
            } else if (fieldValue instanceof Double) {
                double value = ((Double)fieldValue).floatValue();
                if (value % denominator != 0.0d) {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' had a value of " + value
                                                                       + " and was not divisible by " + denominator);
                } else {
                    problems.recordSuccess();
                }
            }
        }

        @Override
        public String toString() {
            return "'" + propertyName + "' must be divisible by " + denominator;
        }
    }

    protected static abstract class ItemCountValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final int number;

        protected ItemCountValidator( String propertyName,
                                      int number ) {
            this.propertyName = propertyName;
            this.number = number;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldValue instanceof List) {
                List<?> array = (List<?>)fieldValue;
                if (evaluate(number, array.size())) {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' has '" + array.size() + "' values but should have "
                                                                       + ruleDescription() + " '" + number + "'");
                } else {
                    problems.recordSuccess();
                }
            }
            // otherwise the value is not a number and the minimum doesn't apply
        }

        protected abstract boolean evaluate( double value,
                                             double actualValue );

        protected abstract String ruleDescription();

        @Override
        public String toString() {
            return "'" + propertyName + "' has '" + ruleDescription() + " '" + number + "' items";
        }
    }

    protected static class MinimumItemsValidator extends ItemCountValidator {
        private static final long serialVersionUID = 1L;

        public MinimumItemsValidator( String propertyName,
                                      int minimum ) {
            super(propertyName, minimum);
        }

        @Override
        protected boolean evaluate( double minimumCount,
                                    double actualCount ) {
            return minimumCount < actualCount;
        }

        @Override
        protected String ruleDescription() {
            return "at least";
        }
    }

    protected static class MaximumItemsValidator extends ItemCountValidator {
        private static final long serialVersionUID = 1L;

        public MaximumItemsValidator( String propertyName,
                                      int maximum ) {
            super(propertyName, maximum);
        }

        @Override
        protected boolean evaluate( double maximumCount,
                                    double actualCount ) {
            return maximumCount < actualCount;
        }

        @Override
        protected String ruleDescription() {
            return "no more than";
        }
    }

    protected static class UniqueItemsValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;

        public UniqueItemsValidator( String propertyName ) {
            this.propertyName = propertyName;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            // This only applies if the value is a JSON array ...
            if (fieldValue instanceof List) {
                List<?> array = (List<?>)fieldValue;
                Set<?> uniqueValues = new HashSet<Object>(array);
                int numDups = array.size() - uniqueValues.size();
                if (numDups != 0) {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' must contain unique values, but contains " + numDups
                                                                       + " duplicate values");
                } else {
                    problems.recordSuccess();
                }
            }
        }

        @Override
        public String toString() {
            return "'" + propertyName + "' contains unique items";
        }
    }

    protected static class PatternValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final Pattern pattern;

        public PatternValidator( String propertyName,
                                 Pattern pattern ) {
            this.propertyName = propertyName;
            this.pattern = pattern;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldValue instanceof String || fieldValue instanceof Symbol) {
                String value = fieldValue.toString();
                Matcher matcher = pattern.matcher(value);
                if (!matcher.matches()) {
                    problems.recordError(pathToParent.with(fieldName),
                                         "The '" + fieldName + "' field on '" + pathToParent
                                         + "' failed match the pattern specified by '" + pattern.pattern() + "'");
                } else {
                    problems.recordSuccess();
                }
            }
        }

        @Override
        public String toString() {
            return "'" + propertyName + "' matches pattern '" + pattern.pattern() + "'";
        }
    }

    protected static class EnumValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final Set<String> values;

        public EnumValidator( String propertyName,
                              Collection<?> values ) {
            this.propertyName = propertyName;
            this.values = new HashSet<String>(values.size());
            for (Object value : values) {
                this.values.add(value.toString().toLowerCase());
            }
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (!propertyName.equals(fieldName)) return;
            // This only applies if the value is a JSON array ...
            if (fieldValue instanceof List) {
                for (Object value : (List<?>)fieldValue) {
                    if (values.contains(value.toString().toLowerCase())) {
                        problems.recordSuccess();
                    } else {
                        problems.recordError(pathToParent.with(fieldName),
                                             "The '" + fieldName + "' field on '" + pathToParent + "' contains a value '" + value
                                             + "' in the array that is not part of the enumeration: " + values);
                    }
                }
            } else if (fieldValue != null) {
                if (values.contains(fieldValue.toString().toLowerCase())) {
                    problems.recordSuccess();
                } else {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' has a value of '" + fieldValue
                                                                       + "' that is not part of the enumeration: " + values);
                }
            }
        }

        @Override
        public String toString() {
            return "'" + propertyName + "' contains values from enumeration: " + values;
        }
    }

    protected static class DisallowedTypesValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final EnumSet<Type> disallowedTypes;

        public DisallowedTypesValidator( String propertyName,
                                         EnumSet<Type> disallowedTypes ) {
            this.propertyName = propertyName;
            this.disallowedTypes = disallowedTypes;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            Type type = Type.typeFor(fieldValue);
            if (type != Type.NULL) {
                if (disallowedTypes.contains(type)) {
                    problems.recordError(pathToParent.with(fieldName), "The '" + fieldName + "' field on '" + pathToParent
                                                                       + "' contains a value '" + fieldValue + "' whose type '"
                                                                       + type + "' is disallowed.");
                } else {
                    problems.recordSuccess();
                }
            }
        }

        @Override
        public String toString() {
            return "'" + propertyName + "' may not have values with the types " + disallowedTypes;
        }
    }

    /**
     * The {@link Validator} for item values that should all match a single schema.
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    protected static class AllItemsMatchValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final Validator itemValidator;
        private final SingleProblem itemProblems = new SingleProblem();

        public AllItemsMatchValidator( String propertyName,
                                       Validator itemValidator ) {
            this.propertyName = propertyName;
            this.itemValidator = itemValidator;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldValue instanceof List) {
                // Each item in the list must match the itemValidator or additionalItemsValidator ...
                List<?> items = (List<?>)fieldValue;
                Path path = pathToParent.with(fieldName);
                int i = 1;
                boolean success = true;
                for (Object item : items) {
                    itemProblems.clear();
                    itemValidator.validate(item, fieldName, parent, pathToParent, itemProblems, resolver);
                    if (itemProblems.hasProblem()) {
                        problems.recordError(path, "The '" + fieldName + "' field on '" + pathToParent
                                                   + "' is an array, but the " + i + th(i)
                                                   + " item does not satisfy the schema for the " + i + th(i) + " item");
                        success = false;
                    }
                    ++i;
                }
                if (success) problems.recordSuccess();
            } else if (parent instanceof List) {
                //we are dealing with an optional array of items
                List<?> items = (List<?>)parent;
                int i = 1;
                boolean success = true;
                for (Object item : items) {
                    itemProblems.clear();
                    if (item instanceof Document) {
                        itemValidator.validate(null, null, (Document) item, pathToParent, itemProblems, resolver);
                        if (itemProblems.hasProblem()) {
                            success = false;
                        }
                    } else {
                        fieldName = item.toString();
                        Path path = pathToParent.with(fieldName);
                        itemValidator.validate(item, fieldName, parent, pathToParent, itemProblems, resolver);
                        if (itemProblems.hasProblem()) {
                            problems.recordError(path, "The '" + fieldName + "' field on '" + pathToParent
                                    + "' is an array, but the " + i + th(i)
                                    + " item does not satisfy the schema for the " + i + th(i) + " item");
                            success = false;
                        }
                    }
                    ++i;
                }
                if (success) problems.recordSuccess();
            }

        }

        @Override
        public String toString() {
            return "'" + propertyName + "' may be an array with items matching the schema: " + itemValidator;
        }
    }

    /**
     * The {@link Validator} for "tuple typing", when item values should each match a corresponding schema or, if applicable, an
     * additional items schema.
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    protected static class EachItemMatchesValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final List<Validator> itemValidators;
        private final Validator additionalItemsValidator;
        private final SingleProblem itemProblems = new SingleProblem();
        private final boolean additionalItemsAllowed;

        public EachItemMatchesValidator( String propertyName,
                                         List<Validator> itemValidators,
                                         Validator additionalItemsValidator,
                                         boolean additionalItemsAllowed ) {
            this.propertyName = propertyName;
            this.itemValidators = itemValidators;
            this.additionalItemsValidator = additionalItemsValidator;
            this.additionalItemsAllowed = additionalItemsAllowed;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldValue instanceof List) {
                // Each item in the list must match the itemValidator or additionalItemsValidator ...
                List<?> items = (List<?>)fieldValue;
                Path path = pathToParent.with(fieldName);
                int i = 0;
                Iterator<?> itemIterator = items.iterator();
                Iterator<Validator> itemValidatorIterator = itemValidators.iterator();
                boolean success = true;
                while (itemIterator.hasNext() && itemValidatorIterator.hasNext()) {
                    ++i;
                    Object item = itemIterator.next();
                    Validator itemValidator = itemValidatorIterator.next();
                    itemValidator.validate(item, fieldName, parent, pathToParent, itemProblems, resolver);
                }
                if (additionalItemsAllowed && additionalItemsValidator != null) {
                    while (itemIterator.hasNext()) {
                        ++i;
                        Object item = itemIterator.next();
                        itemProblems.clear();
                        additionalItemsValidator.validate(item, fieldName, parent, pathToParent, itemProblems, resolver);
                        if (itemProblems.hasProblem()) {
                            problems.recordError(path,
                                                 "The '"
                                                 + fieldName
                                                 + "' field on '"
                                                 + pathToParent
                                                 + "' is an array, but the "
                                                 + i
                                                 + th(i)
                                                 + " item does have a corresponding schema and does not satisfy the additional items schema)");
                            success = false;
                        }
                    }
                } else if (!additionalItemsAllowed) {
                    while (itemIterator.hasNext()) {
                        ++i;
                        problems.recordError(path,
                                             "The '" + fieldName + "' field on '" + pathToParent + "' is an array, but the " + i
                                             + th(i)
                                             + " item does have a corresponding schema (and no additional items were specified)");
                        success = false;
                    }
                }
                if (success) problems.recordSuccess();
            }
        }

        @Override
        public String toString() {
            return "'" + propertyName + "' may be an array with items matching the schemas: " + itemValidators
                   + (additionalItemsValidator == null ? "" : " or the additional items schema " + additionalItemsValidator);
        }
    }

    protected static class NotValidValidator implements Validator {
        private static final long serialVersionUID = 1L;

        public NotValidValidator() {
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            problems.recordError(pathToParent, "");
        }

        @Override
        public String toString() {
            return "not valid";
        }
    }

    protected static String th( int i ) {
        switch (i) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
        }
        return "th";
    }

    protected static RequiredValidator getRequiredValidator( Validator validator ) {
        if (validator instanceof RequiredValidator) return (RequiredValidator)validator;
        if (validator instanceof ValidatorCollection) {
            for (Validator val : ((ValidatorCollection)validator)) {
                if (val instanceof RequiredValidator) return (RequiredValidator)val;
            }
        }
        return null;
    }

    protected static class PropertyValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final Validator validator;
        private final RequiredValidator required;

        public PropertyValidator( String propertyName,
                                  Validator validator ) {
            this.propertyName = propertyName;
            this.validator = validator;
            this.required = getRequiredValidator(validator);
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldName == null) {
                fieldName = propertyName;
            }
            if (fieldValue == null) {
                fieldValue = parent.get(propertyName);
            }
            if (fieldValue == null) {
                if (required != null) {
                    // The field is required ...
                    required.validate(fieldValue, fieldName, parent, pathToParent, problems, resolver);
                }
                return;
            }
            if (fieldValue instanceof Document) {
                validator.validate(null, null, (Document)fieldValue, pathToParent.with(fieldName), problems, resolver);
            } else {
                validator.validate(fieldValue, fieldName, parent, pathToParent, problems, resolver);
            }
        }

        @Override
        public String toString() {
            return "property '" + propertyName + "': " + validator.toString();
        }
    }

    protected static class PatternPropertyValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final Pattern propertyNamePattern;
        private final Validator validator;

        public PatternPropertyValidator( Pattern propertyNamePattern,
                                         Validator validator ) {
            this.propertyNamePattern = propertyNamePattern;
            this.validator = validator;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldValue == null) return;
            Matcher matcher = propertyNamePattern.matcher(fieldName);
            if (matcher.matches()) {
                // Apply the validator to the field value ...
                validator.validate(fieldValue, fieldName, parent, pathToParent, problems, resolver);
            }
        }

        @Override
        public String toString() {
            return "pattern property '" + propertyNamePattern.pattern() + "': " + validator.toString();
        }
    }

    protected static class AllowedPropertiesValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final Set<String> allowedPropertyNames;
        private final Validator validator;

        public AllowedPropertiesValidator( Set<String> allowedPropertyNames,
                                           Validator validator ) {
            this.allowedPropertyNames = allowedPropertyNames;
            this.validator = validator;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldName != null && !allowedPropertyNames.contains(fieldName)) {
                // Then the field is not handled by an explicit schema, so we need to check it here
                validator.validate(fieldValue, fieldName, parent, pathToParent, problems, resolver);
            } else if (fieldName == null) {
                // we need to validate each defined additional property which has a schema
                for (Field field : parent.fields()) {
                    if (field.getValue() instanceof Document) {
                        validator.validate(null, null, (Document)field.getValue(), pathToParent.with(field.getName()), problems, resolver);
                    } else {
                        validator.validate(field.getValue(), field.getName(), parent, pathToParent, problems, resolver);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "additional properties allowed: " + validator.toString();
        }
    }

    protected static class NoOtherAllowedPropertiesValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private final Set<String> allowedPropertyNames;

        public NoOtherAllowedPropertiesValidator( Set<String> allowedPropertyNames ) {
            this.allowedPropertyNames = allowedPropertyNames;
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            if (fieldValue == null) {
                if (fieldName == null) {
                    // Go through all of the fields in the document ...
                    for (Field field : parent.fields()) {
                        validate(field.getValue(), field.getName(), parent, pathToParent, problems, resolver);
                    }
                }
            } else {
                if (!allowedPropertyNames.contains(fieldName)) {
                    // Then the field is not handled by an explicit schema, so it's not allowed ...
                    problems.recordError(pathToParent.with(fieldName),
                                         "The '" + fieldName + "' field on '" + pathToParent
                                         + "' is not defined in the schema and the schema does not allow additional properties.");
                } else {
                    problems.recordSuccess();
                }
            }
        }

        @Override
        public String toString() {
            return "additional properties not allowed";
        }
    }

    protected static class CompositeValidator implements Validator, ValidatorCollection {
        private static final long serialVersionUID = 1L;

        private final List<Validator> validators = new ArrayList<Validator>();

        public CompositeValidator() {
        }

        protected void add( Validator validator ) {
            this.validators.add(validator);
        }

        protected int size() {
            return this.validators.size();
        }

        protected Validator getFirst() {
            return this.validators.get(0);
        }

        @Override
        public void validate( Object fieldValue,
                              String fieldName,
                              Document parent,
                              Path pathToParent,
                              Problems problems,
                              SchemaDocumentResolver resolver ) {
            for (Validator validator : validators) {
                try {
                    validator.validate(fieldValue, fieldName, parent, pathToParent, problems, resolver);
                } catch (Throwable t) {
                    problems.recordError(pathToParent, t.getMessage(), t);
                }
            }
        }

        @Override
        public Iterator<Validator> iterator() {
            return validators.iterator();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Validator validator : validators) {
                sb.append(validator.toString());
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    protected static class SingleProblem implements Problems {
        private static final long serialVersionUID = 1L;
        private ProblemType type;
        private Path path;
        private String message;
        private Throwable exception;
        private Object actualValue;
        private Object convertedValue;
        private Type actualType;
        private Type requiredType;
        private boolean mismatch = false;
        private boolean success = false;

        @Override
        public void recordSuccess() {
            success = true;
        }

        @Override
        public void recordError( Path path,
                                 String message ) {
            this.type = ProblemType.ERROR;
            this.path = path;
            this.message = message;
            this.exception = null;
            this.actualValue = null;
            this.convertedValue = null;
            this.actualType = null;
            this.requiredType = null;
            this.mismatch = false;
            this.success = false;
        }

        @Override
        public void recordError( Path path,
                                 String message,
                                 Throwable exception ) {
            this.type = ProblemType.ERROR;
            this.path = path;
            this.message = message;
            this.exception = exception;
            this.actualValue = null;
            this.convertedValue = null;
            this.actualType = null;
            this.requiredType = null;
            this.mismatch = false;
            this.success = false;
        }

        @Override
        public void recordWarning( Path path,
                                   String message ) {
            this.type = ProblemType.WARNING;
            this.path = path;
            this.message = message;
            this.exception = null;
            this.actualValue = null;
            this.convertedValue = null;
            this.actualType = null;
            this.requiredType = null;
            this.mismatch = false;
            this.success = false;
        }

        @Override
        public void recordTypeMismatch( Path path,
                                        String message,
                                        Type actualType,
                                        Object actualValue,
                                        Type requiredType,
                                        Object convertedValue ) {
            this.type = ProblemType.ERROR;
            this.path = path;
            this.message = message;
            this.exception = null;
            this.actualValue = actualValue;
            this.convertedValue = convertedValue;
            this.actualType = actualType;
            this.requiredType = requiredType;
            this.mismatch = true;
            this.success = false;
        }

        public boolean hasProblem() {
            return this.type != null;
        }

        public void recordIn( Problems otherProblems ) {
            if (success) {
                otherProblems.recordSuccess();
                return;
            }
            switch (type) {
                case ERROR:
                    if (this.mismatch) {
                        otherProblems.recordTypeMismatch(this.path,
                                                         this.message,
                                                         this.actualType,
                                                         this.actualValue,
                                                         this.requiredType,
                                                         this.convertedValue);
                    } else {
                        otherProblems.recordError(this.path, this.message, this.exception);
                    }
                    break;
                case WARNING:
                    otherProblems.recordWarning(this.path, this.message);
                    break;
            }
        }

        public void clear() {
            this.type = null;
            this.success = false;
        }
    }
}
